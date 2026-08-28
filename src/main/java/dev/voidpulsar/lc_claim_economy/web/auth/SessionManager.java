package dev.voidpulsar.lc_claim_economy.web.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dashboard login sessions, keyed by an opaque bearer token stored in an
 * {@code HttpOnly} cookie. Held entirely in memory - a server restart signs
 * every dashboard session out, which is an acceptable (and safer) default
 * for a lightweight companion web server rather than persisting session
 * tokens to disk.
 */
public final class SessionManager {
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, WebSession> sessionsByToken = new ConcurrentHashMap<>();

    public String create(UUID playerId, int ttlMinutes) {
        return create(playerId, ttlMinutes, System.currentTimeMillis());
    }

    String create(UUID playerId, int ttlMinutes, long nowMillis) {
        String token = generateToken();
        long expiresAt = nowMillis + ttlMinutes * 60_000L;
        sessionsByToken.put(token, new WebSession(token, playerId, expiresAt));
        return token;
    }

    public Optional<UUID> resolve(String token) {
        return resolve(token, System.currentTimeMillis());
    }

    Optional<UUID> resolve(String token, long nowMillis) {
        if (token == null) {
            return Optional.empty();
        }
        WebSession session = sessionsByToken.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.isExpired(nowMillis)) {
            sessionsByToken.remove(token);
            return Optional.empty();
        }
        return Optional.of(session.playerId());
    }

    public void invalidate(String token) {
        if (token != null) {
            sessionsByToken.remove(token);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
