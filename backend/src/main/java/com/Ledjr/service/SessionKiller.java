package com.Ledjr.service;

import java.util.Map;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

/**
 * signs a user out everywhere. spring session keeps every login in the SPRING_SESSION table with
 * the user's id as the "principal name", so we can look them all up and delete them
 */
@Service
public class SessionKiller {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public SessionKiller(FindByIndexNameSessionRepository<? extends Session> sessions){
        this.sessions = sessions;
    }

    /** every session for this user except keepSessionId (null = kill them all). returns how many went */
    public int signOutEverywhereExcept(Long userId, String keepSessionId){
        // the principal is the user id (see AuthController.startSession), spring stores it as a string
        Map<String, ? extends Session> mine = sessions.findByPrincipalName(String.valueOf(userId));
        int killed = 0;
        for (String id : mine.keySet()) {
            if (id.equals(keepSessionId)) continue;
            sessions.deleteById(id);
            killed++;
        }
        return killed;
    }
}
