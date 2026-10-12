package com.ansar.secureapi.web;

import com.ansar.secureapi.service.AuthService;
import com.ansar.secureapi.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary register(@Valid @RequestBody RegisterRequest req) { return auth.register(req); }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) { return auth.login(req); }
}
