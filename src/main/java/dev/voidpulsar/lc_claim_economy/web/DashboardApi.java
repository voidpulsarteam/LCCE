package dev.voidpulsar.lc_claim_economy.web;

import dev.voidpulsar.lc_claim_economy.compat.ModCompat;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * Backend-agnostic entry point for the authenticated dashboard - mirrors
 * {@link WebDataService}'s split: this class's own method signatures must
 * never reference any {@code dev.ftb.mods.*} type (see that class's
 * javadoc for why), so all real work is delegated to {@link FtbDashboardService}
 * behind a {@code ModCompat.isFtbAvailable()} check. There is currently no
 * Open Parties and Claims dashboard backend - see {@link #isAvailable()}.
 */
public final class DashboardApi {
    private DashboardApi() {
    }

    /** Whether the authenticated dashboard has a working backend at all (FTB Chunks/Teams only, for now). */
    public static boolean isAvailable() {
        return ModCompat.isFtbAvailable();
    }

    public static String playerName(MinecraftServer server, UUID playerId) {
        if (!ModCompat.isFtbAvailable()) {
            return playerId.toString().substring(0, 8);
        }
        return FtbDashboardService.playerName(server, playerId);
    }

    /** Null if the player has no team / dashboard isn't available. */
    public static String buildDashboardJson(MinecraftServer server, UUID playerId) {
        if (!ModCompat.isFtbAvailable()) {
            return null;
        }
        return FtbDashboardService.buildDashboardJson(server, playerId);
    }

    public static ActionResult applyProtection(MinecraftServer server, UUID playerId, String propertyKey, boolean active) {
        if (!ModCompat.isFtbAvailable()) {
            return ActionResult.failure("Dashboard actions require FTB Chunks/Teams.");
        }
        return FtbDashboardService.applyProtection(server, playerId, propertyKey, active);
    }

    public static ActionResult setPeaceful(MinecraftServer server, UUID playerId, boolean peaceful) {
        if (!ModCompat.isFtbAvailable()) {
            return ActionResult.failure("Dashboard actions require FTB Chunks/Teams.");
        }
        return FtbDashboardService.setPeaceful(server, playerId, peaceful);
    }

    public static ActionResult toggleForceLoad(MinecraftServer server, UUID playerId, String chunkKey, boolean load) {
        if (!ModCompat.isFtbAvailable()) {
            return ActionResult.failure("Dashboard actions require FTB Chunks/Teams.");
        }
        return FtbDashboardService.toggleForceLoad(server, playerId, chunkKey, load);
    }

    public static ActionResult unclaimChunk(MinecraftServer server, UUID playerId, String chunkKey) {
        if (!ModCompat.isFtbAvailable()) {
            return ActionResult.failure("Dashboard actions require FTB Chunks/Teams.");
        }
        return FtbDashboardService.unclaimChunk(server, playerId, chunkKey);
    }

    public static ActionResult toggleWar(MinecraftServer server, UUID playerId, UUID targetTeamId) {
        if (!ModCompat.isFtbAvailable()) {
            return ActionResult.failure("Dashboard actions require FTB Chunks/Teams.");
        }
        return FtbDashboardService.toggleWar(server, playerId, targetTeamId);
    }
}
