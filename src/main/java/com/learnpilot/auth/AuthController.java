package com.learnpilot.auth;

import com.learnpilot.security.JwtService;
import com.learnpilot.user.AppUser;
import com.learnpilot.user.AppUserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(AppUserRepository users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public record RegisterRequest(@NotBlank @Email @Size(max = 320) String email,
                                  @Size(min = 8, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) {}
    public record AuthResponse(String token, String tokenType, String email, String role) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        AppUser user = users.save(new AppUser(email, encoder.encode(request.password())));
        return new AuthResponse(jwtService.createToken(user.getEmail()), "Bearer", user.getEmail(), user.getRole());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email())
            .filter(found -> encoder.matches(request.password(), found.getPasswordHash()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        return new AuthResponse(jwtService.createToken(user.getEmail()), "Bearer", user.getEmail(), user.getRole());
    }
}
