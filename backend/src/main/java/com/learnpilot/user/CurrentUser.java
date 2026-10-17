package com.learnpilot.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/** Resolves the signed-in user's id from the JWT-authenticated principal (their email). */
@Component
public class CurrentUser {
    private final AppUserRepository users;
    public CurrentUser(AppUserRepository users) { this.users = users; }

    public UUID id(Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in to continue.");
        return users.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again."))
            .getId();
    }
}
