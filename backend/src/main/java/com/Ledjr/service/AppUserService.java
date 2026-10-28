package com.Ledjr.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.Ledjr.entity.AppUser;
import com.Ledjr.repository.AppUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AppUserService {

    private final AppUserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(AppUserService.class);

    public AppUserService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void register(String email, String passwordHash, String firstName, String lastName) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);
        log.info("New User Created-> UserID: {} | Email: {}", user.getId(), user.getEmail());
    }

    public Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<AppUser> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findById(id);
    }

    public AppUser updateProfile(AppUser user, String firstName, String lastName, String email) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        return userRepository.save(user);
    }

    /** takes the already hashed password, hashing stays in the controller with the encoder */
    public void changePassword(AppUser user, String passwordHash) {
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
        log.info("User id# {} changed their password", user.getId());
    }

    /** they finished or skipped the setup after sign-up */
    public void markSetupDone(AppUser user) {
        user.setSetupDone(true);
        userRepository.save(user);
    }

}
