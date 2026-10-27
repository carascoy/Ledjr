package com.Ledjr.repository;

import com.Ledjr.entity.AppUser;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    /** is this email taken? doesn't load the whole user. used on signup */
    boolean existsByEmail(String email);

}
