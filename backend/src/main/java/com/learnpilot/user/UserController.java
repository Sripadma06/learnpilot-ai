package com.learnpilot.user;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final AppUserRepository users;
    public UserController(AppUserRepository users) { this.users = users; }

    public record MeResponse(UUID id, String email, String role, Instant createdAt) {}

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        AppUser user = users.findByEmailIgnoreCase(authentication.getName()).orElseThrow();
        return new MeResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
