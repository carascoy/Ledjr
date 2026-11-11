package com.Ledjr.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.Ledjr.entity.AppUser;

class AdminAccessTest {

    private static AppUser user(long id, boolean demo){
        AppUser u = new AppUser();
        u.setId(id);
        u.setDemo(demo);
        return u;
    }

    @Test
    @DisplayName("listed ids are admins, everyone else isn't")
    void listedIds(){
        AdminAccess access = new AdminAccess(" 1, 4 ");
        assertTrue(access.isAdmin(user(1, false)));
        assertTrue(access.isAdmin(user(4, false)));
        assertFalse(access.isAdmin(user(2, false)));
    }

    @Test
    @DisplayName("not set means nobody is an admin")
    void emptyMeansNobody(){
        assertFalse(new AdminAccess("").isAdmin(user(1, false)));
    }

    @Test
    @DisplayName("junk in the list is ignored, not an error")
    void junkIgnored(){
        AdminAccess access = new AdminAccess("abc, 7, ,1x");
        assertTrue(access.isAdmin(user(7, false)));
        assertFalse(access.isAdmin(user(1, false)));
    }

    @Test
    @DisplayName("a demo account is never an admin")
    void demoNever(){
        assertFalse(new AdminAccess("3").isAdmin(user(3, true)));
    }
}
