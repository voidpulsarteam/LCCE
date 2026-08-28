package dev.voidpulsar.lc_claim_economy.web.auth;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoginCodeServiceTest {

    @Test
    void redeem_validCode_returnsPlayerId() {
        LoginCodeService service = new LoginCodeService();
        UUID playerId = UUID.randomUUID();
        String code = service.issue(playerId, 5, 1000L);

        assertEquals(playerId, service.redeem(code, 1500L).orElseThrow());
    }

    @Test
    void redeem_isSingleUse() {
        LoginCodeService service = new LoginCodeService();
        UUID playerId = UUID.randomUUID();
        String code = service.issue(playerId, 5, 1000L);

        assertTrue(service.redeem(code, 1500L).isPresent());
        assertTrue(service.redeem(code, 1500L).isEmpty());
    }

    @Test
    void redeem_expiredCode_returnsEmpty() {
        LoginCodeService service = new LoginCodeService();
        UUID playerId = UUID.randomUUID();
        long ttlMillis = 5 * 60_000L;
        String code = service.issue(playerId, 5, 1000L);

        assertTrue(service.redeem(code, 1000L + ttlMillis).isEmpty());
    }

    @Test
    void redeem_unknownCode_returnsEmpty() {
        LoginCodeService service = new LoginCodeService();
        assertTrue(service.redeem("NOTREAL1", 1000L).isEmpty());
    }

    @Test
    void redeem_nullCode_returnsEmpty() {
        LoginCodeService service = new LoginCodeService();
        assertTrue(service.redeem(null, 1000L).isEmpty());
    }

    @Test
    void redeem_isCaseInsensitiveAndTrimmed() {
        LoginCodeService service = new LoginCodeService();
        UUID playerId = UUID.randomUUID();
        String code = service.issue(playerId, 5, 1000L);

        assertEquals(playerId, service.redeem("  " + code.toLowerCase(java.util.Locale.ROOT) + "  ", 1500L).orElseThrow());
    }

    @Test
    void issue_generatesUniqueCodes() {
        LoginCodeService service = new LoginCodeService();
        UUID playerId = UUID.randomUUID();
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            codes.add(service.issue(playerId, 5, 1000L));
        }
        assertEquals(200, codes.size());
    }
}
