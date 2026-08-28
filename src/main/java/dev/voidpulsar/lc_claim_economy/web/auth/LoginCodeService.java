package dev.voidpulsar.lc_claim_economy.web.auth;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived, single-use codes pairing a web dashboard login to a player's
 * UUID. A player generates one in-game (see the {@code /lcce web login}
 * command) and enters it on the web login page - no password is ever
 * stored or transmitted, since there isn't one to steal.
 * <p>
 * Held entirely in memory: codes don't need to survive a server restart, and
 * doing so would only extend how long a leaked/overheard code stays live.
 */
public final class LoginCodeService {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, LoginCode> codesByValue = new ConcurrentHashMap<>();

    public String issue(UUID playerId, int ttlMinutes) {
        return issue(playerId, ttlMinutes, System.currentTimeMillis());
    }

    String issue(UUID playerId, int ttlMinutes, long nowMillis) {
        sweepExpired(nowMillis);
        String code = generateCode();
        long expiresAt = nowMillis + ttlMinutes * 60_000L;
        codesByValue.put(code, new LoginCode(code, playerId, expiresAt));
        return code;
    }

    /** Single-use: a valid code is consumed on the first successful redemption. */
    public Optional<UUID> redeem(String code) {
        return redeem(code, System.currentTimeMillis());
    }

    Optional<UUID> redeem(String code, long nowMillis) {
        if (code == null) {
            return Optional.empty();
        }
        LoginCode entry = codesByValue.remove(code.trim().toUpperCase(java.util.Locale.ROOT));
        if (entry == null || entry.isExpired(nowMillis)) {
            return Optional.empty();
        }
        return Optional.of(entry.playerId());
    }

    private void sweepExpired(long nowMillis) {
        codesByValue.values().removeIf(entry -> entry.isExpired(nowMillis));
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
