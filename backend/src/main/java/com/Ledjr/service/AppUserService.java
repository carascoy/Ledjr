package com.Ledjr.service;

import java.util.Optional;

import com.Ledjr.entity.AppUser;
import com.Ledjr.repository.AppUserRepository;

public class AppUserService {
    
    private final AppUserRepository userRepository;

    public AppUserService(AppUserRepository userRepository){
        this.userRepository = userRepository;
    }

    public void register(String email, String passwordHash){
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPassword_hash(passwordHash);
        userRepository.save(user);
    }

    public Optional<AppUser> findByEmail(String email){
        return userRepository.findByEmail(email);
    }

    public boolean emailTaken(){
        return userRepository.existsByEmail(null);
    }

}
