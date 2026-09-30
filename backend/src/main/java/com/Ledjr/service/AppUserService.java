package com.Ledjr.service;

import java.util.Optional;

import com.Ledjr.entity.AppUser;
import com.Ledjr.repository.AppUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppUserService {

    private final AppUserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

    public AppUserService(AppUserRepository userRepository){
        this.userRepository = userRepository;
    }

    public void register(String email, String passwordHash){
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword_hash(passwordHash);
        userRepository.save(user);
        log.info("New User Created-> UserID: {} | Email: {}", user.getId(), user.getEmail());
    }

    public Optional<AppUser> findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public boolean emailTaken(String email){
        return userRepository.existsByEmail(email);
    }

}
