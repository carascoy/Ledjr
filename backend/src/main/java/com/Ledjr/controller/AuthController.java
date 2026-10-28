package com.Ledjr.controller;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.Ledjr.dto.RegisterRequest;
import com.Ledjr.dto.SigninRequest;
import com.Ledjr.dto.SigninResponse;
import com.Ledjr.entity.AppUser;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.CategoryService;
import com.Ledjr.service.LoginAttemptLimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@RestController
public class AuthController {

    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;
    private final CategoryService categoryService;
    private final LoginAttemptLimiter limiter;
    // where the "you're logged in" info gets saved: the session (which Spring Session keeps in Postgres)
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    // same regexes as AuthPage.tsx: any digit, and anything that isn't a letter or digit
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9]");

    public AuthController(AppUserService appUserService, PasswordEncoder passwordEncoder, CategoryService categoryService,
                          LoginAttemptLimiter limiter){
        this.appUserService = appUserService;
        this.passwordEncoder = passwordEncoder;
        this.categoryService = categoryService;
        this.limiter = limiter;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse){

        // same 3 rules as the sign up form in AuthPage.tsx. the form is just for showing hints,
        // THIS is the real check, anyone can skip the form and hit /register directly
        String problem = passwordProblem(request.password());
        if (problem != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
        }

        if(appUserService.emailTaken(request.email())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already registered.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        appUserService.register(request.email(), passwordHash, request.firstName(), request.lastName());

        Optional<AppUser> userInfo = appUserService.findByEmail(request.email());
        AppUser user = userInfo.get();

        // new accounts get some starter categories so quick-add isn't empty
        categoryService.seedDefaults(user);

        // signing up logs you straight in
        startSession(user, httpRequest, httpResponse);
        return ResponseEntity.status(HttpStatus.OK).body(toResponse(user));
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody SigninRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse){

        // too many wrong passwords lately (for this email or from this IP) -> don't even check
        String ip = httpRequest.getRemoteAddr();
        long wait = limiter.retryAfterSeconds(request.email(), ip);
        if (wait > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(wait))
                .body(LoginAttemptLimiter.tooManyMessage(wait));
        }

        // same error for wrong email AND wrong password, otherwise you could
        // figure out which emails have accounts
        Optional<AppUser> userInfo = appUserService.findByEmail(request.email());

        if(userInfo.isEmpty() || !passwordEncoder.matches(request.password(), userInfo.get().getPasswordHash())){
            limiter.recordFailure(request.email(), ip);
            log.info("Failed sign-in attempt.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect email or password.");
        }

        limiter.recordSuccess(request.email());
        AppUser user = userInfo.get();
        startSession(user, httpRequest, httpResponse);

        log.info("User {} {} has logged into their account", user.getFirstName(), user.getLastName());
        return ResponseEntity.status(HttpStatus.OK).body(toResponse(user));
    }

    /** GET /me - who's logged in. the frontend calls this on page load so a refresh doesn't log you out */
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Long userId){
        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(toResponse(user.get()));
    }

    /** POST /me/setup-done - finished or skipped the setup, don't offer it again */
    @PostMapping("/me/setup-done")
    public ResponseEntity<?> setupDone(@AuthenticationPrincipal Long userId){
        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        appUserService.markSetupDone(user.get());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /csrf - hands the frontend the CSRF token for this session.
     * it sends it back in a header (headerName) on every POST/PUT/DELETE, so another
     * website can't make requests using your login cookie (it can't read this token)
     */
    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token){
        return token;
    }

    /**
     * marks this browser as logged in as `user`.
     * throws away any old session first and makes a new one, so a session id someone
     * else planted before login is useless after it (session fixation)
     */
    private void startSession(AppUser user, HttpServletRequest request, HttpServletResponse response){
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        request.getSession(true);

        // the "principal" is just the user's id. controllers get it back with @AuthenticationPrincipal Long userId
        UsernamePasswordAuthenticationToken auth = UsernamePasswordAuthenticationToken.authenticated(user.getId(), null, List.of());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /** what's wrong with this password, or null if it's fine. AccountController uses it too for changing it */
    static String passwordProblem(String password){
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters.";
        }
        if (!DIGIT.matcher(password).find()) {
            return "Password needs at least 1 number.";
        }
        if (!SPECIAL.matcher(password).find()) {
            return "Password needs at least 1 special character.";
        }
        return null;
    }

    static SigninResponse toResponse(AppUser user){
        return new SigninResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getCreatedAt(), user.isSetupDone());
    }
}
