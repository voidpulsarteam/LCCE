package dev.voidpulsar.lc_claim_economy.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.voidpulsar.lc_claim_economy.LcClaimEconomy;
import dev.voidpulsar.lc_claim_economy.compat.ModCompat;
import dev.voidpulsar.lc_claim_economy.config.LcClaimEconomyConfig;
import dev.voidpulsar.lc_claim_economy.web.DashboardSessions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /lcce web login} - issues a short-lived, single-use code (see
 * {@link dev.voidpulsar.lc_claim_economy.web.auth.LoginCodeService}) that
 * the player enters on the web dashboard's login page to start a session,
 * without ever having a password to set or steal.
 */
public final class WebLoginCommand {
    private WebLoginCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal(LcClaimEconomy.COMMAND_ROOT)
                .then(Commands.literal("web")
                        .then(Commands.literal("login")
                                .executes(WebLoginCommand::issueCode))));
    }

    private static int issueCode(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        if (!ModCompat.isFtbAvailable()) {
            player.displayClientMessage(Component.translatable("message.lc_claim_economy.web_login.unavailable"), false);
            return 0;
        }
        if (!LcClaimEconomyConfig.SERVER.webEnabled.get() || !LcClaimEconomyConfig.SERVER.webDashboardEnabled.get()) {
            player.displayClientMessage(Component.translatable("message.lc_claim_economy.web_login.disabled"), false);
            return 0;
        }

        int ttlMinutes = LcClaimEconomyConfig.SERVER.webLoginCodeMinutes.get();
        String code = DashboardSessions.LOGIN_CODES.issue(player.getUUID(), ttlMinutes);

        player.displayClientMessage(
                Component.translatable("message.lc_claim_economy.web_login.code", Component.literal(code).withStyle(ChatFormatting.BOLD, ChatFormatting.AQUA), ttlMinutes),
                false
        );
        return 1;
    }
}
