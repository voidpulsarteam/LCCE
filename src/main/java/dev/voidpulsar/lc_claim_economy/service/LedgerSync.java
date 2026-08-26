package dev.voidpulsar.lc_claim_economy.service;

import dev.voidpulsar.lc_claim_economy.bank.BankAccountHelper;
import dev.voidpulsar.lc_claim_economy.compat.ModCompat;
import dev.voidpulsar.lc_claim_economy.data.LcClaimEconomySavedData;
import dev.voidpulsar.lc_claim_economy.network.LedgerLine;
import dev.voidpulsar.lc_claim_economy.network.SyncLedgerPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * Sends a player their own account's transaction history - the FTB Chunks
 * backend keys ledger entries by team ID (see {@link
 * BankAccountHelper#ledgerKeyForPlayer}), while the OP&C backend keys them
 * by the acting player's own UUID (see {@code OpcClaimEconomyListener}/
 * {@code OpcUpkeepService}) since OP&C claims aren't necessarily tied to an
 * FTB Teams party.
 */
public final class LedgerSync {
    private LedgerSync() {
    }

    public static void syncToPlayer(ServerPlayer player) {
        UUID ledgerKey = ModCompat.isFtbAvailable()
                ? BankAccountHelper.ledgerKeyForPlayer(player)
                : player.getUUID();

        List<LedgerLine> lines = LcClaimEconomySavedData.get(player.server).getLedger(ledgerKey).stream()
                .map(LedgerLine::from)
                .toList();
        PacketDistributor.sendToPlayer(player, new SyncLedgerPayload(lines));
    }
}
