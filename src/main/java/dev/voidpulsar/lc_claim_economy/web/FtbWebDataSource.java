package dev.voidpulsar.lc_claim_economy.web;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.voidpulsar.lc_claim_economy.LcClaimEconomy;
import dev.voidpulsar.lc_claim_economy.bank.BankAccountHelper;
import dev.voidpulsar.lc_claim_economy.service.WarService;
import dev.voidpulsar.lc_claim_economy.teams.FtbTeamCatalog;
import dev.voidpulsar.lc_claim_economy.util.MoneyUtil;
import io.github.lightman314.lightmanscurrency.api.money.bank.IBankAccount;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Only ever called from {@link WebDataService} behind
 * {@code ModCompat.isFtbAvailable()} - see that class's javadoc for why
 * this separation matters.
 */
final class FtbWebDataSource {
    private FtbWebDataSource() {
    }

    static List<LeaderboardEntry> collectEntries(MinecraftServer server) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Team team : FtbTeamCatalog.trackedTeams(server)) {
            try {
                entries.add(toEntry(server, team));
            } catch (Exception e) {
                LcClaimEconomy.LOGGER.warn("Web leaderboard: failed to read FTB team {}", team.getTeamId(), e);
            }
        }
        return entries;
    }

    private static LeaderboardEntry toEntry(MinecraftServer server, Team team) {
        BankAccountHelper.ensurePartyAccountExists(server, team);
        IBankAccount account = BankAccountHelper.getAccountForTeam(server, team);
        long balance = MoneyUtil.totalCopper(account);

        int claimedChunks = FTBChunksAPI.api().isManagerLoaded()
                ? FTBChunksAPI.api().getManager().getOrCreateData(team).getClaimedChunks().size()
                : 0;

        return new LeaderboardEntry(WarService.displayName(team), balance, claimedChunks);
    }

    static int trackedAccountCount(MinecraftServer server) {
        return FtbTeamCatalog.trackedTeams(server).size();
    }
}
