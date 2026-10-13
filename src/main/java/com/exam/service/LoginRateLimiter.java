package com.exam.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 15;
    private static final long LOCKOUT_DURATION_SECONDS = 15; // 15 seconds

    private static class AttemptTracker {
        int failedAttempts = 0;
        Instant lockoutUntil = Instant.MIN;
    }

    private final Map<String, AttemptTracker> cache = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptTracker tracker = cache.get(key);
        if (tracker == null) {
            return false;
        }
        if (Instant.now().isBefore(tracker.lockoutUntil)) {
            return true;
        }
        if (Instant.now().isAfter(tracker.lockoutUntil) && tracker.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            // Expired lockout
            cache.remove(key);
            return false;
        }
        return false;
    }

    public void recordFailedAttempt(String key) {
        AttemptTracker tracker = cache.computeIfAbsent(key, k -> new AttemptTracker());
        tracker.failedAttempts++;
        if (tracker.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            tracker.lockoutUntil = Instant.now().plusSeconds(LOCKOUT_DURATION_SECONDS);
        }
    }

    public void recordSuccessfulAttempt(String key) {
        cache.remove(key);
    }
}
