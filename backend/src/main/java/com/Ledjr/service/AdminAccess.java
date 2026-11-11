package com.Ledjr.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.Ledjr.entity.AppUser;

/**
 * who can see /admin: account ids from ADMIN_USER_IDS (e.g. "1" or "1,4").
 * ids instead of emails because emails aren't verified, an id can't be taken over
 */
@Component
public class AdminAccess {

    private final Set<Long> adminIds;

    public AdminAccess(@Value("${app.admin-user-ids}") String ids){
        this.adminIds = Arrays.stream(ids.split(","))
            .map(String::trim)
            .filter(s -> s.matches("\\d+"))
            .map(Long::valueOf)
            .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(AppUser user){
        return user != null && !user.isDemo() && adminIds.contains(user.getId());
    }
}
