package dev.voidpulsar.lc_claim_economy.integration.quest;

import dev.voidpulsar.lc_claim_economy.LcClaimEconomy;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * This mod has no dependency, hard or soft, on FTB Quests. Instead it
 * silently grants a handful of hidden, toast-free internal advancements
 * (defined under {@code data/lc_claim_economy/advancement/}) at claim
 * economy milestones - total chunks claimed, the very first claim ever
 * made on the server, declaring a war, a war ending. Any advancement-aware
 * mod (FTB Quests included) can use these as task targets without this mod
 * needing to know FTB Quests exists.
 * <p>
 * All of these advancements sit under an invisible {@code root} (no
 * display block) and are individually marked {@code hidden}, so none of
 * them ever produce a toast, a tab, or an entry in the vanilla advancements
 * screen - they exist purely to be granted and observed.
 */
public final class QuestAdvancements {
    private static final String NS = "lc_claim_economy";
    private static final String CRITERION = "trigger";

    public static final int[] CLAIM_MILESTONES = {5, 10, 25, 50, 100, 250, 500};

    private QuestAdvancements() {
    }

    public static ResourceLocation claimedChunks(int milestone) {
        return ResourceLocation.fromNamespaceAndPath(NS, "claims/claimed_" + milestone);
    }

    public static ResourceLocation pioneer() {
        return ResourceLocation.fromNamespaceAndPath(NS, "claims/pioneer");
    }

    public static ResourceLocation warDeclared() {
        return ResourceLocation.fromNamespaceAndPath(NS, "war/declared_war");
    }

    public static ResourceLocation warEnded() {
        return ResourceLocation.fromNamespaceAndPath(NS, "war/war_ended");
    }

    public static void grant(ServerPlayer player, ResourceLocation advancementId) {
        MinecraftServer server = player.server;
        if (server == null) {
            return;
        }
        AdvancementHolder holder = server.getAdvancements().get(advancementId);
        if (holder == null) {
            LcClaimEconomy.LOGGER.warn("Missing internal quest-hook advancement {} - was it removed from mod resources?", advancementId);
            return;
        }
        player.getAdvancements().award(holder, CRITERION);
    }
}
