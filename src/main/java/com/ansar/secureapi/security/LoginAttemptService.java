package com.ansar.secureapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory account lockout: after N failed logins for a username, further attempts are
 * rejected (even with the correct password) until the lock window expires.
 * Limitation: per-instance memory; use Redis or a DB table when running more than one replica.
 */
@Service
public class LoginAttemptService {
    private record State(int failures, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, State> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final long lockSeconds;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public LoginAttemptService(@Value("${app.lockout.max-failures:5}") int maxFailures,
                               @Value("${app.lockout.lock-seconds:900}") long lockSeconds) {
        this(maxFailures, lockSeconds, Clock.systemUTC());
    }

    LoginAttemptService(int maxFailures, long lockSeconds, Clock clock) {
        this.maxFailures = maxFailures;
        this.lockSeconds = lockSeconds;
        this.clock = clock;
    }

    private static String key(String username) { return username == null ? "" : username.toLowerCase(Locale.ROOT); }

    /** Seconds remaining on the lock, or 0 if not locked. */
    public long lockedForSeconds(String username) {
        State s = attempts.get(key(username));
        if (s == null || s.lockedUntil() == null) return 0;
        long remaining = s.lockedUntil().getEpochSecond() - clock.instant().getEpochSecond();
        if (remaining <= 0) {
            attempts.remove(key(username));
            return 0;
        }
        return remaining;
    }

    public void onFailure(String username) {
        attempts.compute(key(username), (k, s) -> {
            int f = (s == null ? 0 : s.failures()) + 1;
            Instant until = f >= maxFailures ? clock.instant().plusSeconds(lockSeconds) : null;
            return new State(f, until);
        });
    }

    public void onSuccess(String username) { attempts.remove(key(username)); }

    public void reset() { attempts.clear(); }
}
