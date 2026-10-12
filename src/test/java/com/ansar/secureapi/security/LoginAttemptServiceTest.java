package com.ansar.secureapi.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    static class MutableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        public ZoneOffset getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId z) { return this; }
        public Instant instant() { return now.get(); }
    }

    @Test
    void locksAtThresholdAndUnlocksAfterWindow() {
        MutableClock clock = new MutableClock();
        LoginAttemptService s = new LoginAttemptService(3, 60, clock);
        s.onFailure("Eve"); s.onFailure("eve");
        assertThat(s.lockedForSeconds("eve")).isZero();
        s.onFailure("EVE");
        assertThat(s.lockedForSeconds("eve")).isEqualTo(60);
        clock.now.set(clock.now.get().plusSeconds(61));
        assertThat(s.lockedForSeconds("eve")).isZero();
    }

    @Test
    void successResetsCounter() {
        LoginAttemptService s = new LoginAttemptService(3, 60, new MutableClock());
        s.onFailure("u"); s.onFailure("u");
        s.onSuccess("u");
        s.onFailure("u"); s.onFailure("u");
        assertThat(s.lockedForSeconds("u")).isZero();
    }
}
