package com.Ledjr.controller;

import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;

import com.Ledjr.dto.RegisterRequest;
import com.Ledjr.dto.SigninRequest;
import com.Ledjr.dto.SigninResponse;
import com.Ledjr.entity.AppUser;
import com.Ledjr.service.AppUserService;
import com.Ledjr.service.CategoryService;


@RestController
@CrossOrigin(origins = "http://localhost:5173") 
public class AuthController {
    
    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;
    private final CategoryService categoryService;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AppUserService appUserService, PasswordEncoder passwordEncoder, CategoryService categoryService){
        this.appUserService = appUserService;
        this.passwordEncoder = passwordEncoder;
        this.categoryService = categoryService;
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request){

        if(appUserService.emailTaken(request.email())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already registered.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        appUserService.register(request.email(), passwordHash, request.firstName(), request.lastName());

        Optional<AppUser> userInfo = appUserService.findByEmail(request.email());
        AppUser user = userInfo.get();

        // Every new account starts with a handful of categories so quick-add works right away.
        categoryService.seedDefaults(user);

        SigninResponse response = new SigninResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getCreatedAt());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<?> register(@RequestBody  SigninRequest request){

        // Same status + same body for "no such email" and "wrong password",
        // so the response never reveals which emails are registered.
        Optional<AppUser> userInfo = appUserService.findByEmail(request.email());

        if(userInfo.isEmpty() || !passwordEncoder.matches(request.password(), userInfo.get().getPasswordHash())){
            log.info("Failed sign-in attempt.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect email or password.");
        }

        AppUser user = userInfo.get();

        log.info("User {} {} has logged into their account", user.getFirstName(), user.getLastName());
        SigninResponse response = new SigninResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getCreatedAt());
        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

}
