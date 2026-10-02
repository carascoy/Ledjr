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


@RestController
@CrossOrigin(origins = "http://localhost:5173") 
public class AuthController {
    
    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AppUserService appUserService, PasswordEncoder passwordEncoder){
        this.appUserService = appUserService;
        this.passwordEncoder = passwordEncoder;
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
        
        SigninResponse response = new SigninResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getCreatedAt());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<?> register(@RequestBody  SigninRequest request){

        if(appUserService.findByEmail(request.email()).isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not find user with this email");
        }

        Optional<AppUser> userInfo = appUserService.findByEmail(request.email());
        AppUser user = userInfo.get();

        if(passwordEncoder.matches(request.password(), user.getPasswordHash())){

            log.info("User {} {} has logged into their account", user.getFirstName(), user.getLastName());
            SigninResponse response = new SigninResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getCreatedAt());
            return ResponseEntity.status(HttpStatus.OK).body(response);

        }else{

            log.info("User was unable to log into an account, possible incorrect username or password entered.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not cleared to access their account");
        }


    }

}
