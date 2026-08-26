package dev.voidpulsar.lc_claim_economy.client;

import dev.voidpulsar.lc_claim_economy.client.gui.BankDashboardScreen;
import net.minecraft.client.Minecraft;

public final class BankDashboardUiRefresh {
    private BankDashboardUiRefresh() {
    }

    public static void refreshIfOpen() {
        if (Minecraft.getInstance().screen instanceof BankDashboardScreen screen) {
            screen.refresh();
        }
    }
}
