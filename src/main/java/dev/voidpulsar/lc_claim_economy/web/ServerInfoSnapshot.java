package dev.voidpulsar.lc_claim_economy.web;

/**
 * Plain snapshot of the current config/state shown in the web page's info
 * panel. Kept free of FTB/OP&C types for the same reason as
 * {@link LeaderboardEntry}.
 */
public record ServerInfoSnapshot(
        String backendName,
        long claimPrice,
        long forceLoadUpkeepPrice,
        int upkeepPeriodMinutes,
        int freeChunks,
        int landChunkGroupSize,
        boolean warEnabled,
        long mobGriefProtectionPrice,
        long explosionProtectionPrice,
        long pvpDisablePrice,
        long blockInteractProtectionPrice,
        long blockEditProtectionPrice,
        long entityInteractProtectionPrice,
        int trackedAccountCount,
        int onlinePlayerCount,
        long statUpkeepChargedCopper,
        int statUpkeepChargedCount,
        int statUpkeepMissedCount,
        long statClaimSpendCopper,
        int statClaimCount,
        long statUnclaimRefundCopper,
        int statUnclaimCount,
        long statMarketVolumeCopper,
        int statMarketSaleCount
) {
}
