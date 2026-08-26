# Changelog

All notable changes to this mod are documented here.

## [4.2.0]

### Added

- **Player-to-player chunk marketplace (FTB Chunks only).** `MarketService`
  and its command frontend, `/lc_claim_economy market sell|cancel|buy|browse`:
  - All commands are position-based - `sell <price_copper>` lists whatever
    chunk you're currently standing in, `cancel` delists it, `buy` purchases
    the listing on the chunk you're standing in, `browse` prints all active
    listings (position, dimension, price, seller) sorted cheapest first.
  - Ownership transfer is sequenced safely: the seller's chunk is unclaimed
    and the buyer's claim is confirmed successful *before* any money moves,
    with an automatic rollback (reclaiming for the seller) if the buyer-side
    claim fails, so a failed transfer never charges the buyer or strands the
    chunk unclaimed.
  - Transfers run through a new `ClaimBatchContext.runAsInternalTransfer`
    suppression so `ChunkClaimHandler` doesn't also charge the buyer the
    normal claim price or pay the seller an unclaim refund on top of the
    agreed sale price.
  - Scope note: the buyer receives a freshly-claimed chunk with default
    protection/force-load state - land/build classification and per-player
    chunk permissions are not carried over from the seller.
- **Transaction ledger and economy statistics**, tracking upkeep charges,
  claim purchases, unclaim refunds, the pioneer bonus, and market sales/
  purchases:
  - Every player/team account now keeps a capped (50-entry), newest-first
    history (`LcClaimEconomySavedData.LedgerEntry`), viewable in-game via a
    new **History** button on the Bank Dashboard, opening
    `TransactionHistoryScreen` - a scrollable, ftblibrary-free list showing
    each entry's description, timestamp, and signed amount.
  - Server-wide aggregate counters (total upkeep charged/missed, claim spend,
    unclaim refunds, market volume) are now shown on the web dashboard under
    a new **Economy Activity** panel - answers "is offline upkeep actually
    being charged, how often, and how much" at a glance, without exposing
    any individual account's history over the unauthenticated web server.
  - Wired into both backends: `ChunkClaimHandler`/`ClaimBatchContext` (FTB
    single and bulk claims/unclaims), `OpcClaimEconomyListener` (OP&C claims/
    unclaims), and `UpkeepSettlementService`/`OpcUpkeepService` (periodic
    upkeep, on both a successful charge and a missed/frozen payment).

### Fixed

- **Free-allowance chunks paid out an unclaim refund they were never charged
  for**, on both backends:
  - OP&C (`OpcClaimEconomyListener.handleUnclaim`) refunded on *every*
    unclaim unconditionally - it never checked the free-chunk allowance at
    all, unlike the claim side right above it which does. A chunk claimed
    for free within `freeChunks` still paid out a refund when unclaimed,
    manufacturing money from nothing.
  - FTB Chunks (`ChunkClaimHandler`) had a subtler version of the same bug:
    `ChunkTeamData.getClaimedChunks()` caches its result, and FTB Chunks'
    own `unclaim()` only invalidates that cache *after* firing the
    `AFTER_UNCLAIM` event this mod listens on - confirmed by decompiling the
    installed FTB Chunks jar. Reading the team's claimed-chunk count from
    inside that listener could return a stale, pre-removal count, tipping
    the free-allowance comparison the wrong way. Fixed by counting from the
    claim manager's own live (uncached) chunk collection instead of the
    per-team cached one.

## [4.1.0]

### Added

- **Merged in the bounty/leaderboard/quest-integration branch** (previously
  built and released separately as 4.0.0, without OP&C or dashboard/web
  support). All FTB Chunks-only, consistent with the rest of that branch:
  - `/lc_claim_economy bounty player|team|list` - place a bounty (in
    copper) on a player or their team, escrowed immediately on placement.
    Player bounties pay out to whoever lands the kill; team bounties only
    pay out if the killer's team is actually at war with the target's, via
    `BountyKillHandler`.
  - `/lc_claim_economy leaderboard [land|wealth]` - a chat-based top-10
    ranking by claimed chunks or bank balance. Complementary to (not a
    replacement for) the web server's live leaderboard added in 3.9.0 -
    same underlying data, different access path.
  - `/lc_claim_economy quest_deposit <amount>` (permission level 2) - lets
    an FTB Quests Command Reward pay directly into a player's bank account.
  - **Pioneer Bonus** - a one-time, server-wide reward (`pioneerBonusAmount`
    config, default 5 diamond coins) for whoever claims the very first
    chunk ever claimed on the server.
  - **FTB Quests hooks with zero dependency** - hidden, toast-free internal
    advancements (`QuestAdvancements`) granted at claim-count milestones
    (5/10/25/50/100/250/500), on the first-ever claim, on declaring a war,
    and on a war ending. Any advancement-aware mod, FTB Quests included,
    can target these as task criteria without this mod needing FTB Quests
    as a dependency at all.

## [3.9.0]

### Added

- **OP&C protection upkeep.** Open Parties and Claims claims now pay periodic
  upkeep like FTB Chunks claims do, via a new `OpcUpkeepService`:
  - Force-load upkeep: `forceLoadUpkeepPrice` per force-loaded chunk per
    period, same as FTB.
  - Non-payment now disables the owner's OP&C `FORCELOAD` config option
    (the closest OP&C equivalent to FTB's "protection locked" state) until
    they can pay again, and restores it automatically once they can.
- **OP&C land/build chunk split.** Claimed chunks can now be marked as
  "land" (cheaper, billed once per group of `landChunkGroupSize` chunks) or
  "build" (billed individually), the same distinction FTB Chunks claims
  already had:
  - New command: `/lc_claim_economy opc_chunktype land|build|status`,
    applied to whichever claimed chunk the player is standing in. OP&C has
    no claims screen to hook a UI toggle into the way FTB Chunks does, so
    this is command-driven and applies immediately rather than being
    queued to the next upkeep period.
  - Protection pricing (`OpcProtectionPricing`) maps OP&C's per-owner
    protection exceptions (mob/explosion/PvP/block-edit/block-interact/
    entity-interact) onto the same price config FTB claims use: exempting
    everyone is treated as free, anything more restrictive is billable.
  - The `freeChunks` allowance now applies to OP&C claims the same way it
    does for FTB.
- **Bank/Upkeep Dashboard.** A new in-game screen (`BankDashboardScreen`),
  opened with a keybind (default **B**, rebindable in Controls), showing
  account balance, next claim price, claimed/free chunks, upkeep period and
  force-load price, and all protection prices. Built entirely on vanilla
  Minecraft GUI widgets rather than ftblibrary, so it works identically
  whether the server is running FTB Chunks or OP&C (or neither yet).
- **Optional built-in web server** for a live leaderboard/info page,
  entirely opt-in and off by default:
  - New config section (`web`): `webEnabled`, `webPort` (default 8123),
    `webBindAddress` (default `0.0.0.0`), `webLeaderboardSize` (default 10).
  - Serves a single dashboard page with a richest-accounts leaderboard,
    most-claimed-chunks leaderboard, and a general server info panel
    (claim price, upkeep, all protection prices, backend in use, tracked
    account count, players online). Auto-refreshes every 10 seconds.
  - Works against either backend (FTB Chunks or OP&C), same as the rest of
    the mod's dual-backend design.
  - Built on the JDK's own `com.sun.net.httpserver` and a small hand-rolled
    JSON writer - no new external dependencies added to the mod.
  - Read-only, unauthenticated by design: it exposes no way to modify
    anything, but does expose player/team names, chunk counts, and account
    balances to anyone who can reach the configured port. Documented
    clearly in the config comment; keep it firewalled/off if that's
    sensitive on your server.

### Fixed

- Fixed a crash: `SyncClaimPricesPayload`'s client handler unconditionally
  touched ftblibrary/FTB Teams UI classes on every balance sync. Since
  ftblibrary is an optional dependency, a client connected to an OP&C-only
  server without ftblibrary installed would throw `NoClassDefFoundError`
  and disconnect the moment any claim-price data synced. Now gated behind
  the same FTB-availability check used everywhere else in the mod.
- `ClaimPriceSync` (balance/claim-price sync to clients) now also populates
  correctly for OP&C players; previously it only ever populated data when
  FTB Teams was the active backend, so OP&C clients always saw "not synced"
  placeholders.

### Changed

- Wars remain FTB Chunks-only by design (not a gap to close) - OP&C has no
  invasion/overclaim concept for a war system to attach to, and this is
  documented explicitly in the OP&C integration's own code comments rather
  than left as an open question.

