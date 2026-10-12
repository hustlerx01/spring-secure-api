package com.ansar.secureapi.error;

public class AccountLockedException extends RuntimeException {
    private final long retryAfterSeconds;
    public AccountLockedException(long retryAfterSeconds) {
        super("Too many failed login attempts. Try again later.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
