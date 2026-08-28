package dev.voidpulsar.lc_claim_economy.web.auth;

import java.util.UUID;

record WebSession(String token, UUID playerId, long expiresAtMillis) {
    boolean isExpired(long nowMillis) {
        return nowMillis >= expiresAtMillis;
    }
}
