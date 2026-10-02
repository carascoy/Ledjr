package com.Ledjr.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.Ledjr.dto.RegisterRequest;
import com.Ledjr.service.AppUserService;

@RestController
@CrossOrigin(origins = "http://localhost:5173") 
public class DashController {

    private final AppUserService appUserService;

    public DashController(AppUserService appUserService){
        this.appUserService = appUserService;
    }
    
}
