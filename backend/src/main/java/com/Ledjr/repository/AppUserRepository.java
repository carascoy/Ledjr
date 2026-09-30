package com.Ledjr.repository;

import com.Ledjr.entity.AppUser;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JpaRepository<AppUser, Long> gives these for free, no code needed:
 * save(user)      — insert or update
 * findById(id)    — get one row by its id
 * findAll()       — get every row
 * deleteById(id)  — delete one row
 * count()         — how many rows exist
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /** Look up a user by their email — used for login. */
    Optional<AppUser> findByEmail(String email);

    /** Check whether an email is already registered, without loading the whole user — used for signup validation. */
    boolean existsByEmail(String email);

}
