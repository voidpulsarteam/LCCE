package dev.voidpulsar.lc_claim_economy.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.voidpulsar.lc_claim_economy.client.gui.BankDashboardScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Registers a single keybind that opens {@link BankDashboardScreen}. This
 * is the one entry point to that screen that works no matter which claim
 * mod is active (or neither) - unlike the FTB-side {@code ClaimBreakdownScreen}
 * button, which only exists inside FTB Teams' own "My Team" screen.
 */
public final class ClientDashboardKeybind {
    public static final KeyMapping OPEN_DASHBOARD = new KeyMapping(
            "key.lc_claim_economy.open_dashboard",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.lc_claim_economy"
    );

    public static void registerMapping(RegisterKeyMappingsEvent event) {
        event.register(OPEN_DASHBOARD);
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_DASHBOARD.consumeClick()) {
            if (Minecraft.getInstance().screen == null) {
                Minecraft.getInstance().setScreen(new BankDashboardScreen());
            }
        }
    }
}
