package com.ansar.secureapi.service;

import com.ansar.secureapi.domain.AppUser;
import com.ansar.secureapi.domain.Role;
import com.ansar.secureapi.error.AccountLockedException;
import com.ansar.secureapi.error.ConflictException;
import com.ansar.secureapi.repo.UserRepository;
import com.ansar.secureapi.security.JwtService;
import com.ansar.secureapi.security.LoginAttemptService;
import com.ansar.secureapi.web.Dtos.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService attempts;
    /** Pre-computed hash so unknown usernames still pay the BCrypt cost (reduces user-enumeration timing signal). */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.attempts = attempts;
        this.dummyHash = encoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public UserSummary register(RegisterRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken");
        }
        // Self-registration always yields USER; role is never taken from the request body (mass-assignment guard).
        AppUser u = users.save(new AppUser(req.username(), encoder.encode(req.password()), Role.USER));
        return new UserSummary(u.getId(), u.getUsername(), u.getRole().name());
    }

    public TokenResponse login(LoginRequest req) {
        long locked = attempts.lockedForSeconds(req.username());
        if (locked > 0) throw new AccountLockedException(locked);

        var user = users.findByUsername(req.username());
        boolean ok;
        if (user.isPresent()) {
            ok = encoder.matches(req.password(), user.get().getPasswordHash());
        } else {
            encoder.matches(req.password(), dummyHash); // burn equivalent CPU time, result ignored
            ok = false;
        }
        if (!ok) {
            attempts.onFailure(req.username());
            throw new BadCredentialsException("bad credentials");
        }
        attempts.onSuccess(req.username());
        AppUser u = user.get();
        return new TokenResponse(jwt.issue(u.getUsername(), u.getRole().name()), "Bearer", jwt.getTtlSeconds());
    }
}
