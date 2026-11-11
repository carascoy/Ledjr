package com.Ledjr.controller;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Ledjr.entity.AppUser;
import com.Ledjr.service.AdminAccess;
import com.Ledjr.service.AdminStatsService;
import com.Ledjr.service.AppUserService;

@RestController
public class AdminController {

    private final AppUserService appUserService;
    private final AdminAccess adminAccess;
    private final AdminStatsService statsService;

    public AdminController(AppUserService appUserService, AdminAccess adminAccess, AdminStatsService statsService){
        this.appUserService = appUserService;
        this.adminAccess = adminAccess;
        this.statsService = statsService;
    }

    /** 404 instead of 403 for everyone else, so the page doesn't even look like it exists */
    @GetMapping("/admin/stats")
    public ResponseEntity<?> stats(@AuthenticationPrincipal Long userId){
        Optional<AppUser> user = appUserService.findById(userId);
        if (user.isEmpty() || !adminAccess.isAdmin(user.get())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(statsService.stats(LocalDate.now()));
    }
}
