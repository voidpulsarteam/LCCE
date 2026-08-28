![Lightman's Currency: Claim Economy](https://raw.githubusercontent.com/voidpulsarteam/LCCE/refs/heads/main/docs/images/banner.png)

**Lightman's Currency: Claim Economy** connects [Lightman's Currency](https://modrinth.com/mod/lightmans-currency) with a chunk-claiming mod, turning claiming and land protection into an economy-driven system. Claiming costs money, protections require ongoing upkeep, and — on the full backend — teams can go to war (with an optional siege mode and a scheduled declaration window), place bounties, trade claimed land on a player marketplace, and race for a one-time "first claim" bonus.

The mod supports **two alternative claim backends** — pick whichever claim mod you already use:

- **FTB Chunks + FTB Teams + FTB Library** — the original, full-featured integration: land/build chunk split, per-chunk player permissions, wars, siege mode, bounties, a player marketplace, the Pioneer Bonus, and native UI buttons built into the FTB claim/team screens.
- **Open Parties and Claims (OP&C)** — a lighter integration covering claiming, upkeep, and force-load billing via a `/lcce opc_chunktype` command instead of custom UI. Wars, bounties, the marketplace, and the Pioneer Bonus are not available on this backend (OP&C has no invasion/overclaim concept for wars to map onto, and no party-created API events for bounties or listings to hook).

If both are installed, the FTB integration is used.

**Environment:** required on both client and server. **Minecraft:** 1.21.1 · **Loader:** NeoForge

Also available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/lcce).

---

## Requirements

| Mod | Required |
|-----|----------|
| [Lightman's Currency](https://modrinth.com/mod/lightmans-currency) — **exact version 1.21-2.3.0.5** | ✅ |
| **One of:** | |
| [FTB Chunks](https://modrinth.com/mod/ftb-chunks) + [FTB Teams](https://modrinth.com/mod/ftb-teams) + [FTB Library](https://modrinth.com/mod/ftb-library) — **2101.1.21 / 2101.1.10 / 2101.1.32 or newer** | ✅ (or OP&C) |
| [Open Parties and Claims](https://modrinth.com/mod/open-parties-and-claims) — **0.27.5 or newer** | ✅ (or FTB) |
| NeoForge 21.1.234+ | ✅ |

Lightman's Currency is pinned to an **exact** version, since this mod hooks a large, sensitive internal API surface of it and a version bump is likely to carry a breaking change. FTB Chunks and Open Parties and Claims only need to meet a **minimum** version. If a requirement is missing or too old, the mod logs exactly what's wrong on startup and refuses to load instead of misbehaving silently.

---

## Features

### Paid chunk claiming
Claiming a chunk draws funds from the player's or team's bank account (`claimPrice`, server-configurable). Unclaiming refunds a configurable percentage (`unclaimRefundRatio`). The first *N* chunks per team/player can be free (`freeChunks`).

- **FTB Chunks:** an unaffordable claim is blocked outright.
- **OP&C:** claims can't be vetoed ahead of time, so an unaffordable claim is allowed for an instant and then automatically unclaimed.

### Build and Land chunks
Every claimed chunk is a **Build chunk** (base/infrastructure, all protections, billed per chunk) or a **Land chunk** (territory, block interact/edit only, billed once per group of chunks — cheaper for large territories). Switch via Alt + click/drag on the FTB Chunks map, or `/lcce opc_chunktype land|build` on OP&C.

### Per-chunk player permissions *(FTB Chunks only)*
Block edit / interact, entity interact, and PvP access set per player, per chunk — free, and separate from FTB Teams' own roles. Open with **Shift + middle-click** on a claimed chunk in the FTB Chunks map. Owners/officers only.

### Team bank accounts *(FTB Teams only)*
Creating a party auto-creates a linked bank account mirroring the team hierarchy (owner → account owner, officers → admins, members → members). Disbanding distributes the balance and unclaims chunks with refunds.

### Protection upkeep
Protections (PvP, explosions, mob griefing, block/entity interact) are paid periodically: `cost = base_price × billable_chunks`. Can't pay? FTB strips protections in a configurable order and restores them automatically once funded again; OP&C disables force-load instead and leaves claims/protections alone.

### War system *(FTB Teams only)*
Declare war to raise a rival's upkeep — incoming wars scale exponentially, outgoing wars cost a flat multiple of the target's base upkeep. Includes:
- **Siege mode** — bypasses a long-besieged team's explosion protection after a grace period, for packs with real siege weapons.
- **Declaration window** — restrict new war declarations to a recurring weekly time window.
- **Participation safeguards** — a minimum-chunk-count gate and a per-team `/lcce war peaceful` opt-out protect small/unwilling teams.

Managed from FTB Teams' own War screen, or the web dashboard.

### Bounties *(FTB Teams only)*
`/lcce bounty player|team <target> <amount>` escrows a reward collectible in PvP; team bounties only pay out to an actual war opponent's kill. `/lcce bounty list` to browse.

### Player marketplace *(FTB Chunks only)*
`/lcce market sell|cancel|buy|browse` — list, delist, buy, and browse claimed chunks, position-based (acts on the chunk you're standing in). Transfers are sequenced so a failed purchase never charges the buyer or strands the chunk.

### Pioneer Bonus & claim milestones *(FTB Chunks only)*
A one-time reward for the server's first-ever claim, plus hidden advancement milestones at 5/10/25/50/100/250/500 claimed chunks and at war events — usable as FTB Quests hooks without an FTB Quests dependency.

### Force-load upkeep
Force-loading a chunk is free to enable, but adds a periodic charge (`forceLoadUpkeepPrice`).

### Bank Dashboard
An in-game balance/upkeep screen with transaction history, opened anywhere with **B**. FTB-library-free, so it works identically on either backend.

### Optional web leaderboard + player dashboard
A built-in HTTP server (`webEnabled`, off by default) serves a public read-only leaderboard. On top of that, `webDashboardEnabled` (FTB only) adds a passwordless-login player dashboard (`/lcce web login`) for managing land, protections, and wars from a browser, plus server-wide economy stats — with cosmetic theming (site name, accent color, logo, custom CSS).

### Everything else
Coin Mint restriction (`disableCoinMint`), Tax Collector placement rules on claimed FTB chunks, and in-game UI extensions — claim prices in the FTB Chunks map, protection prices in FTB Teams config, pending-state indicators, and a claim cost breakdown popup.

---

## Installation

1. Place the mod jar, Lightman's Currency, and your chosen claim backend into `mods/` — **on both server and every client**.
2. Launch once to generate `world/serverconfig/lc_claim_economy-server.toml`.
3. Configure claim price, upkeep period, protection prices, and (optionally) the web server — full reference below.
4. In-game: `/lcce upkeep_details` and `/lcce leaderboard` confirm it's running.

---

## Server configuration

Config file: `world/serverconfig/lc_claim_economy-server.toml`. Full key-by-key reference:

**`general`** — `claimPrice` (10000), `freeChunks` (0), `landChunkGroupSize` (5), `unclaimRefundRatio` (0.8), `forceLoadUpkeepPrice` (100000), `upkeepPeriodMinutes` (60), `disableCoinMint` (false)

**`protectionPrices`** — `mobGriefProtectionPrice` (80), `explosionProtectionPrice` (70), `pvpDisablePrice` (50), `blockInteractProtectionPrice` (100), `blockEditProtectionPrice` (100), `entityInteractProtectionPrice` (100)

**`war`** — `warEnabled` (true), `warOutgoingCostMultiplier` (2.0), `warCostMultiplier` (1.2), `warDeclarationWindowEnabled` (false) with `warDeclarationWindowStartDay`/`StartHourUtc` (FRIDAY/22) and `EndDay`/`EndHourUtc` (SUNDAY/22), `siegeModeEnabled` (false), `siegeModeGraceHours` (12), `warMinClaimedChunks` (0)

**`protectionDismantle`** — `protectionDismantleOrderLand` (land_block_edit_mode, land_block_interact_mode), `protectionDismantleOrderBuild` (entity_interact_mode, block_edit_mode, block_interact_mode, allow_mob_griefing, allow_explosions, allow_pvp)

**`web`** — `webEnabled` (false), `webPort` (8123), `webBindAddress` ("0.0.0.0"), `webLeaderboardSize` (10), `webDashboardEnabled` (false), `webSessionMinutes` (720), `webLoginCodeMinutes` (5)

**`webTheme`** *(cosmetic)* — `webSiteName` ("Claim Economy"), `webAccentColor` ("#88C0D0"), `webLogoUrl` (""), `webCustomCss` ("")

**`flavor`** — `pioneerBonusAmount` (50000, FTB Chunks only, `0` disables)

**`debug`** — `debugTestTeamCommands` (false)

The [GitHub wiki](https://github.com/voidpulsarteam/LCCE/wiki) has the full description and defaults for every key.

---

## Getting started

**FTB Chunks:** deposit money into your Lightman's Currency account → claim chunks from the FTB Chunks map (`M`) → optionally mark Land chunks with Alt+click → enable protections in FTB Teams settings → create a party for a shared team account → `/lcce upkeep_details` to check billing → optionally declare war, place bounties, or list a chunk on the marketplace.

**Open Parties and Claims:** claim/unclaim/force-load through OP&C's own tools as usual — this mod bills them in the background. Use `/lcce opc_chunktype land|build|status` to set a chunk's billing type (takes effect immediately). If upkeep can't be paid, force-load is disabled until you're funded again; claims are untouched.

Full step-by-step walkthroughs for both paths are on the [wiki](https://github.com/voidpulsarteam/LCCE/wiki/Getting-Started).

---

## Commands

All commands are under `/lcce`.

| Command | Backend | Notes |
|---|---|---|
| `upkeep_details`, `upkeep_priority` | FTB | Owners/officers — upkeep breakdown and dismantle order |
| `bounty player\|team\|list <...>` | FTB | Anyone — place or list bounties |
| `market sell\|cancel\|buy\|browse` | FTB | Anyone — position-based marketplace |
| `war peaceful [on\|off]` | FTB | Owners/officers — opt out of war entirely |
| `leaderboard [land\|wealth]` | Either | Anyone |
| `web login` | FTB | Any online player — one-time dashboard login code |
| `quest_deposit <amount>` | FTB | Op level 2 — FTB Quests command reward |
| `clear_wars` | FTB | Op level 2 |
| `opc_chunktype land\|build\|status` | OP&C | Claim/party owner or admin |

War declarations/endings happen in the FTB Teams War screen or the web dashboard, not via command. Full reference on the [wiki](https://github.com/voidpulsarteam/LCCE/wiki/Commands).

---

## FAQ

**Won't load / version error on startup?** The error names exactly which companion mod and version is wrong — Lightman's Currency needs an exact match, FTB Chunks/OP&C just a minimum version.

**Both FTB Chunks and OP&C installed?** Only the FTB integration is active for wars/bounties/marketplace/Pioneer Bonus/per-chunk permissions in that case.

**Protection got disabled?** The relevant bank account ran out of funds for upkeep — top up and it restores automatically (next cycle on FTB, immediately on OP&C once funded).

**Is the web leaderboard safe to leave on?** It's read-only but has no authentication — anyone reaching the port sees player names, balances, and chunk counts. Keep it firewalled or local-only unless you want it public. The login-gated `/dashboard` is separate and requires a one-time code, but still has no built-in HTTPS.

More in the [FAQ wiki page](https://github.com/voidpulsarteam/LCCE/wiki/FAQ).
