package com.Ledjr.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.Ledjr.dto.RegisterRequest;
import com.Ledjr.dto.SigninRequest;
import com.Ledjr.entity.AppUser;
import com.Ledjr.service.AppUserService;

@RestController
@CrossOrigin(origins = "http://localhost:5173") 
public class AuthController {
    
    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserService appUserService, PasswordEncoder passwordEncoder){
        this.appUserService = appUserService;
        this.passwordEncoder = passwordEncoder;
    }


    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request){

        if(appUserService.emailTaken(request.email())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already registered.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        appUserService.register(request.email(), passwordHash, request.firstName(), request.lastName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body("Account Created");
    }

    @PostMapping("/signin")
    public ResponseEntity<String> register(@RequestBody  SigninRequest request){

        if(appUserService.findByEmail(request.email()).isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Could not find user with this email");
        }

        Optional<AppUser> userInfo = appUserService.findByEmail(request.email());
        AppUser user = userInfo.get();

        if(passwordEncoder.matches(request.password(), user.getPasswordHash())){
            return ResponseEntity.status(HttpStatus.OK).body("User is cleared to access their account");
        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User is not cleared to access their account");
        }


    }

}
