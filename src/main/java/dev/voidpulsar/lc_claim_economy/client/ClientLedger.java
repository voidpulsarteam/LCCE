package dev.voidpulsar.lc_claim_economy.client;

import dev.voidpulsar.lc_claim_economy.client.gui.TransactionHistoryScreen;
import dev.voidpulsar.lc_claim_economy.network.LedgerLine;

import java.util.List;

/** Client-side cache of the viewing player's own ledger, refreshed each time {@link TransactionHistoryScreen} opens. */
public final class ClientLedger {
    private static List<LedgerLine> entries = List.of();

    private ClientLedger() {
    }

    public static void update(List<LedgerLine> newEntries) {
        entries = List.copyOf(newEntries);
        TransactionHistoryScreen.refreshIfOpen();
    }

    public static List<LedgerLine> entries() {
        return entries;
    }
}
