![Lightman's Currency: Claim Economy](https://raw.githubusercontent.com/voidpulsarteam/LCCE/refs/heads/main/docs/images/banner.png)

**Lightman's Currency: Claim Economy** connects [Lightman's Currency](https://www.curseforge.com/minecraft/mc-mods/lightmans-currency) with a chunk-claiming mod, turning claiming and land protection into an economy-driven system. Claiming costs money, protections require ongoing upkeep, and — on the full backend — teams can go to war (with an optional siege mode and a scheduled declaration window), place bounties, trade claimed land on a player marketplace, and race for a one-time "first claim" bonus.

The mod supports **two alternative claim backends** — pick whichever claim mod you already use:

- **FTB Chunks + FTB Teams + FTB Library** — the original, full-featured integration: land/build chunk split, per-chunk player permissions, wars, siege mode, bounties, a player marketplace, the Pioneer Bonus, and native UI buttons built into the FTB claim/team screens.
- **Open Parties and Claims (OP&C)** — a lighter integration covering claiming, upkeep, and force-load billing via a `/lcce opc_chunktype` command instead of custom UI. Wars, bounties, the marketplace, and the Pioneer Bonus are not available on this backend (OP&C has no invasion/overclaim concept for wars to map onto, and no party-created API events for bounties or listings to hook).

If both are installed, the FTB integration is used.

> **Minecraft:** 1.21.1 · **Loader:** NeoForge · **Side:** Both (required on server and client)
> Also available on [Modrinth](https://modrinth.com/mod/lcce).

---

## Requirements

| Mod | Required |
|-----|----------|
| [Lightman's Currency](https://www.curseforge.com/minecraft/mc-mods/lightmans-currency) — **exact version 1.21-2.3.0.5** | ✅ |
| **One of:** | |
| [FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-neoforge) + [FTB Teams](https://www.curseforge.com/minecraft/mc-mods/ftb-teams-neoforge) + [FTB Library](https://www.curseforge.com/minecraft/mc-mods/ftb-library-neoforge) — **2101.1.21 / 2101.1.10 / 2101.1.32 or newer** | ✅ (or OP&C) |
| [Open Parties and Claims](https://www.curseforge.com/minecraft/mc-mods/open-parties-and-claims) — **0.27.5 or newer** | ✅ (or FTB) |
| NeoForge 21.1.234+ | ✅ |

> **Lightman's Currency is pinned to an exact version**, since this mod hooks a large, sensitive internal API surface of it and a version bump is likely to carry a breaking change. **FTB Chunks and Open Parties and Claims only need to meet a minimum version** — earlier releases pinned those exactly too, which meant every routine bugfix release broke this mod's loading until manually re-pinned. If a requirement is missing or too old, the mod logs exactly what's wrong on startup and refuses to load instead of misbehaving silently.

---

## Features

### Paid Chunk Claiming
- Claiming a chunk draws funds from the player's or team's bank account. The price is server-configurable (`claimPrice`).
- Unclaiming a chunk refunds a configurable percentage of the claim price (`unclaimRefundRatio`).
- The first *N* chunks per team or player can be made free (`freeChunks`).
- **FTB Chunks:** an unaffordable claim is blocked outright before it happens.
- **OP&C:** claims can't be vetoed ahead of time by OP&C's API, so an unaffordable claim is allowed for an instant and then automatically unclaimed.

### Two Chunk Types: Build and Land
Every claimed chunk is either a **Build chunk** or a **Land chunk**.

| | Build chunk | Land chunk |
|---|---|---|
| **Purpose** | Base, builds, infrastructure | Territory, borders, open land |
| **Available protections** | Mob griefing, explosions, PvP, block interact/edit, entity interact | Block interact and block edit only |
| **Upkeep billing** | Per chunk | Per group of N chunks (cheaper for large territories) |
| **How to switch (FTB Chunks)** | Alt + click/drag in the FTB Chunks map | Same |
| **How to switch (OP&C)** | `/lcce opc_chunktype build` while standing in the chunk | `/lcce opc_chunktype land` |

On FTB Chunks, switching a chunk's type is queued to take effect at the next upkeep period (to prevent cost-dodging). On OP&C, the `opc_chunktype` command takes effect immediately.

### Per-Chunk Player Permissions *(FTB Chunks only)*
Each claimed chunk can define **player-specific access** using the same protection categories as team protections: block edit, block interact, entity interact, PvP/attack.

This system has **no extra cost** and doesn't affect upkeep pricing — a pure permission-management feature, separate from FTB Teams' own role permissions. Open it from the FTB Chunks map with **Shift + middle-click** on a claimed chunk.

Set an **All Players (Base)** profile per chunk for default permissions on non-team players, then add specific players as overrides: `effective_allow = all_players_base + specific_player_flags`. Only team owners/officers can edit these.

### Team Bank Accounts *(FTB Teams only)*
When an FTB party is created, this mod automatically creates a linked Lightman's Currency bank account for it, mirroring the team hierarchy at all times: **owner → account owner**, **officers → account admins**, **members → account members**.

The account can't be deleted while the party is alive. When the party disbands, any remaining balance and chunk refunds go to each member's personal account. Joining a party dissolves your personal claims and refunds them to your personal account.

On OP&C, party-owned claims are billed against a resolved party account the same way, but there's no equivalent auto-provisioned/mirrored account object — solo and party ownership are billed directly through OP&C's own owner/party model.

### Protection Upkeep
Protecting your land isn't free — protections must be paid for periodically (configurable; default: every hour).

**Upkeep formula:** `cost = base_protection_price × number_of_billable_chunks`. Each active protection adds its own per-chunk price to the base rate, independently configurable.

**If the account runs out of money:**
- **FTB Chunks:** protections are stripped one by one in a configurable priority order. Once the balance is restored, protections re-enable automatically in reverse order.
- **OP&C:** there's no per-protection lock to flip, so instead the owner's **force-load** setting is disabled until upkeep is paid again — claims and their protection settings are left alone.
- Changing a protection that would make upkeep unaffordable is blocked at the UI (FTB) — the toggle simply doesn't apply and an alert appears.

Changes to protections and force-loads are queued to the **next upkeep period** on FTB Chunks to prevent mid-period exploits; OP&C changes apply immediately.

### War System *(FTB Teams only, server-configurable)*
Teams with claimed chunks can declare war on each other. War raises upkeep costs:

- **Incoming wars** increase upkeep exponentially: for base upkeep `b` and `k` incoming wars, the surcharge is `b × Σ(l^n)` for `n = 0..k-1`, where `l` is `warCostMultiplier` (default `1.2`).
- **Outgoing wars** cost a flat `x × target's base upkeep` per war declared, where `x` is `warOutgoingCostMultiplier` (default `2.0`), regardless of how many wars you already have.

War declarations and endings are queued to the next upkeep period. If a team can't pay full upkeep, outgoing wars are frozen until the balance is restored. The system can be disabled entirely per server (`warEnabled`).

The **War screen** (accessible from Team Settings, built into the FTB Teams UI) shows active/pending wars, upkeep cost breakdowns, and target vulnerability indicators.

**Siege mode** *(`siegeModeEnabled`, off by default)*: a team at war longer than `siegeModeGraceHours` (default 12) has its explosion protection bypassed entirely, for packs with large-scale explosive weapons where war should mean a claim can actually be damaged. Grace period is measured from a team's first war of the current streak. PvP and block-edit protection are untouched.

**War declaration window** *(`warDeclarationWindowEnabled`)*: restricts *new* war declarations to a recurring weekly UTC window (e.g. weekends only), so players aren't attacked outside agreed hours. Ending a war, cancelling a pending declare, and the automatic suspend/restore cycle for unaffordable wars are always available — only starting a fresh attack is time-boxed.

**Participation safeguards**: `warMinClaimedChunks` keeps small/new teams out of war below a configurable chunk count, and any team can independently opt out entirely with `/lcce war peaceful on` (blocked while a war is active, so it can't be used as a mid-siege escape hatch).

### Bounty System *(FTB Teams only)*
Place a bounty on a rival player or team, payable to whoever kills them in PvP:

```
/lcce bounty player <target> <amount>
/lcce bounty team <target> <amount>
/lcce bounty list
```

A **player bounty** pays out to whoever lands the killing blow in any PvP kill. A **team bounty** only pays out if the killer's team is actually at war with the victim's team. Multiple bounties on the same target stack. You can't bounty yourself or your own team.

### Player Marketplace *(FTB Chunks only)*
List, browse, and buy claimed chunks from other players — all commands act on the chunk you're currently standing in, so there are no coordinates to get wrong:

```
/lcce market sell <price_copper>
/lcce market cancel
/lcce market buy
/lcce market browse
```

Ownership transfer is sequenced safely: the seller's chunk is unclaimed and the buyer's claim confirmed successful *before* any money moves, with an automatic rollback for the seller if the buyer-side claim fails. The buyer receives the chunk with default protection/force-load state — land/build classification and per-player permissions don't carry over.

### Pioneer Bonus *(FTB Chunks only)*
The very first chunk ever claimed on a server triggers a one-time, server-wide reward: a configurable currency deposit (`pioneerBonusAmount`, default 5 Diamond coins) straight to the claiming player's account, plus a hidden advancement usable as an FTB Quests (or any advancement-aware) task trigger. Set the amount to `0` to disable the payout while keeping the advancement.

### Claim Milestones
Hidden, toast-free advancements fire automatically at 5 / 10 / 25 / 50 / 100 / 250 / 500 total chunks claimed by a team, plus at declaring or ending a war — usable as FTB Quests task triggers **without requiring FTB Quests to be installed**.

### Force-Load Upkeep
Enabling force-load on a chunk is free, but each force-loaded chunk adds a periodic charge (`forceLoadUpkeepPrice`).

### Bank Dashboard
A standalone bank/upkeep dashboard screen, openable from anywhere with a keybind (default **B**), including a **History** tab with a running transaction ledger (upkeep, claims, refunds, pioneer bonus, market sales). Unlike most of this mod's UI, it's built without any FTB-library dependency, so it works identically on either backend.

### Optional Web Leaderboard + Player Dashboard
A small, optional built-in HTTP server (`webEnabled`, off by default) serves a live, **read-only leaderboard page** — top balances and top claimed-chunk counts — with no login and no write access. Because it exposes player names, balances, and chunk counts to anyone who can reach the port, keep it firewalled or bound to `127.0.0.1` unless it's meant to be public.

On top of that, `webDashboardEnabled` (FTB Chunks/Teams only) adds a **login-gated player dashboard** at `/dashboard`: players log in with a short one-time code from `/lcce web login` (no passwords stored or transmitted) to view/manage their team's land, protections, and wars from a browser, plus see server-wide economy activity stats. Cosmetic theming (site name, accent color, logo, custom CSS) is shared by both pages.

### Coin Mint Restriction *(optional)*
`disableCoinMint` lets a server block use of Lightman's Currency's Coin Mint block entirely, closing off a way for players to mint their own currency out of raw materials and bypass the claim economy.

### Tax Collector Placement Restriction *(FTB Teams only)*
Placing Lightman's Currency's Tax Collector block inside a claimed chunk requires the placer's team to own that chunk and hold sufficient purchase rank.

### In-Game UI Extensions
- **Claim prices** and your current balance displayed directly in the FTB Chunks map UI.
- **Protection prices** shown next to each toggle in the FTB Teams config screen.
- **Pending state indicators** for queued changes (e.g. "→ Ally pending").
- **Chunk Player Permissions screen** available from the claim map per chunk.
- **Claim cost breakdown** popup with current price/balance and a bulk-claim cost projection.

---

## Installation

1. Download **Lightman's Currency: Claim Economy** and place the `.jar` in your `mods/` folder.
2. Install Lightman's Currency (exact version above) and **either** the full FTB Chunks/Teams/Library trio **or** Open Parties and Claims (minimum versions above) into the same `mods/` folder.
3. Start the server (or single-player world). The mod generates its config file automatically on first launch. If a required companion mod is missing or too old, the mod refuses to start and logs exactly what's wrong.
4. Configure the mod to your liking (see below).

> **Important:** The mod must be installed on **both the server and every client** — it registers custom network payloads and client-side UI.

---

## Server Configuration

The config file is generated at:
```
world/serverconfig/lc_claim_economy-server.toml
```

Reload it by restarting the server (some values apply immediately).

### `general`

| Key | Default | Description |
|-----|---------|--------------|
| `claimPrice` | `10000` | Cost in copper units to claim one chunk (= 1 Diamond coin) |
| `freeChunks` | `0` | Number of free chunks each team/player gets before paying, also exempt from protection upkeep |
| `landChunkGroupSize` | `5` | Land chunks are billed once per this many chunks (rounded up) |
| `unclaimRefundRatio` | `0.8` | Fraction of claim price refunded on unclaim (0–1) |
| `forceLoadUpkeepPrice` | `100000` | Upkeep cost per force-loaded chunk per period (= 1 Netherite coin) |
| `upkeepPeriodMinutes` | `60` | How often upkeep is charged (real-time minutes, 1–10080) |
| `disableCoinMint` | `false` | Disable Lightman's Currency's Coin Mint block server-wide |

### `protectionPrices` *(added to upkeep per billable chunk)*

| Key | Default | Triggers when… |
|-----|---------|----------------|
| `mobGriefProtectionPrice` | `80` | Allow Mob Griefing = false |
| `explosionProtectionPrice` | `70` | Allow Explosion Damage = false |
| `pvpDisablePrice` | `50` | Allow PvP Combat = false |
| `blockInteractProtectionPrice` | `100` | Block Interact Mode ≠ Public |
| `blockEditProtectionPrice` | `100` | Block Edit Mode ≠ Public |
| `entityInteractProtectionPrice` | `100` | Entity Interact Mode ≠ Public |

### `war`

| Key | Default | Description |
|-----|---------|--------------|
| `warEnabled` | `true` | Enable the war system |
| `warOutgoingCostMultiplier` | `2.0` | Flat multiplier for outgoing war cost (× target's base upkeep, per war) |
| `warCostMultiplier` | `1.2` | Exponent *l* for incoming war cost scaling |
| `warDeclarationWindowEnabled` | `false` | Restrict new war declarations to a recurring weekly UTC window |
| `warDeclarationWindowStartDay` / `StartHourUtc` | `FRIDAY` / `22` | When the window opens |
| `warDeclarationWindowEndDay` / `EndHourUtc` | `SUNDAY` / `22` | When the window closes (can wrap past week's end) |
| `siegeModeEnabled` | `false` | Bypass a besieged team's explosion protection after the grace period |
| `siegeModeGraceHours` | `12` | Hours of active war before siege mode kicks in |
| `warMinClaimedChunks` | `0` | Minimum claimed chunks before a team can declare/be targeted by war (`0` = off) |

### `protectionDismantle`
Order protections are dropped in when upkeep can't be paid. Use FTB property id paths without namespace.

| Key | Default order |
|-----|---------------|
| `protectionDismantleOrderLand` | `land_block_edit_mode`, `land_block_interact_mode` |
| `protectionDismantleOrderBuild` | `entity_interact_mode`, `block_edit_mode`, `block_interact_mode`, `allow_mob_griefing`, `allow_explosions`, `allow_pvp` |

### `web`

| Key | Default | Description |
|-----|---------|--------------|
| `webEnabled` | `false` | Start the built-in read-only leaderboard web server |
| `webPort` | `8123` | Port it listens on |
| `webBindAddress` | `"0.0.0.0"` | Bind address (`127.0.0.1` for local-only) |
| `webLeaderboardSize` | `10` | Max entries shown per leaderboard |
| `webDashboardEnabled` | `false` | Adds the login-gated player dashboard at `/dashboard` (FTB only, requires `webEnabled`) |
| `webSessionMinutes` | `720` | Dashboard login session length, minutes |
| `webLoginCodeMinutes` | `5` | How long a `/lcce web login` code stays valid |

### `webTheme` *(cosmetic only)*

| Key | Default | Description |
|-----|---------|--------------|
| `webSiteName` | `"Claim Economy"` | Site name in the page title/header |
| `webAccentColor` | `"#88C0D0"` | Accent color (CSS hex) |
| `webLogoUrl` | `""` | Optional logo image URL |
| `webCustomCss` | `""` | Optional raw CSS appended after the built-in stylesheet |

### `flavor`

| Key | Default | Description |
|-----|---------|--------------|
| `pioneerBonusAmount` | `50000` | One-time reward for the first chunk ever claimed (FTB Chunks only). `0` disables the payout. |

### `debug`

| Key | Default | Description |
|-----|---------|--------------|
| `debugTestTeamCommands` | `false` | Enables `/lcce seed_test_teams` / `clear_test_teams` / `count_test_teams`. Keep off in production. |

---

## Getting Started (In-Game Tutorial — FTB Chunks path)

### Step 1 — Get some money
Players need a Lightman's Currency bank account with a positive balance. Place an **ATM** or use your wallet to deposit money. As a server operator, you can give money directly:
```
/lcbank give players <playername> <amount>
```

### Step 2 — Claim your first chunks
Open the FTB Chunks map (`M` by default). Click a chunk to claim it. The claim price is deducted from your bank account automatically. Unclaiming a chunk refunds part of the price (80% by default).

### Step 3 — Switch chunks to Land (optional)
By default all claimed chunks are **Build chunks**. Hold **Alt** and click or drag over chunks in the FTB Chunks map to mark them **Land chunks** (cheaper upkeep, restricted protections).

### Step 3.5 — Configure per-player permissions per chunk (optional)
**Shift + middle-click** a claimed chunk in the FTB Chunks map to open **Chunk Player Permissions**. Set an **All Players (Base)** default and add specific players by name or UUID.

### Step 4 — Enable protections
Open FTB Teams settings → protection properties. Each protection shows its per-chunk price next to the toggle. Protections take effect immediately; upkeep is charged at the next billing period.

### Step 5 — Set up a party (for teams)
Create a party via FTB Teams — this mod auto-creates a linked bank account for it. Only **owners and officers** can claim chunks, force-load them, or manage war for the team.

### Step 6 — Force-loading chunks
Enable force-load on any claimed chunk as usual. No upfront cost, but each adds a charge to the next upkeep bill.

### Step 7 — Monitor upkeep
```
/lcce upkeep_details
/lcce upkeep_priority
```
shows the latest upkeep breakdown and the protection dismantle order.

### Step 8 — War (optional)
Open FTB Teams settings → **War** (owners/officers). Declare war (queued to next upkeep period) or end an existing one. If a declaration window is configured, the screen shows when it's open or closed. Opt out entirely with `/lcce war peaceful on`.

### Step 9 — Bounties (optional)
```
/lcce bounty player <playername> <amount>
/lcce bounty team <teamname> <amount>
/lcce bounty list
```

### Step 10 — Marketplace (optional)
```
/lcce market sell <price_copper>
/lcce market browse
/lcce market buy
/lcce market cancel
```

### Step 11 — Web dashboard (optional)
If `webEnabled` and `webDashboardEnabled` are on, run `/lcce web login` for a one-time login code, then use it on the dashboard's login page.

---

## Getting Started (Open Parties and Claims path)

Claiming, unclaiming, and force-load upkeep work the same way through OP&C's own claim tools — this mod bills them automatically in the background. There's no dedicated claim-price UI overlay on the OP&C side, so keep an eye on your bank balance directly.

To split chunks into cheaper "land" billing versus full "build" billing, stand in the chunk and run:
```
/lcce opc_chunktype land
/lcce opc_chunktype build
/lcce opc_chunktype status
```
This takes effect immediately rather than at the next upkeep period.

If upkeep can't be paid, your force-load setting is disabled until the balance is restored — claims themselves are left alone. Wars, bounties, the marketplace, and the Pioneer Bonus are not available on this backend.

---

## Commands

All commands are under `/lcce`.

| Command | Backend | Who can use it | Description |
|---------|---------|-----------------|--------------|
| `upkeep_details` | FTB | Owners & officers (or ranked members) | Show the latest upkeep cost breakdown |
| `upkeep_priority` | FTB | Owners & officers (or ranked members) | Show the protection dismantle order and current active costs |
| `bounty player <target> <amount>` | FTB | Anyone | Place an escrowed bounty on a player |
| `bounty team <target> <amount>` | FTB | Anyone | Place an escrowed bounty on a team |
| `bounty list` | FTB | Anyone | List all active bounties |
| `market sell <price_copper>` / `cancel` / `buy` / `browse` | FTB | Anyone | List, delist, buy, or browse claimed-chunk listings (acts on the chunk you're standing in) |
| `war peaceful [on\|off]` | FTB | Owners & officers | Opt your team out of the war system entirely, or check status |
| `leaderboard` / `leaderboard land` | Either | Anyone | Rank teams by claimed-chunk count |
| `leaderboard wealth` | Either | Anyone | Rank teams by bank balance |
| `web login` | FTB | Any online player | Issue a one-time login code for the web dashboard |
| `quest_deposit <amount_copper>` | FTB | Server (op level 2) | For use as an FTB Quests Command Reward, to pay a quest reward into a player's/team's bank account |
| `clear_wars` | FTB | Server (op level 2) | Clears all active/pending war state for every tracked team |
| `opc_chunktype land / build / status` | OP&C | Claim/party owner or admin | Set or check a chunk's land/build billing type |
| `seed_test_teams`, `clear_test_teams`, `count_test_teams` | FTB | Server (op level 2), requires `debugTestTeamCommands=true` | Debug tools for seeding/removing fake test teams |

War declarations and endings are not a command — they're driven from the in-game **War screen** in FTB Teams settings, or the web dashboard's Wars tab.

---

## FAQ

**Q: The mod won't load / crashes on startup with a version error.**
A: This mod pins an exact version for Lightman's Currency and a minimum version for whichever claim backend you use (see Requirements above). The startup error names exactly which mod and version is wrong.

**Q: Can I run this with both FTB Chunks and Open Parties and Claims installed?**
A: Yes, but only the FTB integration will be active for the economy features (wars, siege mode, bounties, marketplace, Pioneer Bonus, per-chunk permissions). If you specifically want the OP&C integration, don't install the FTB Chunks/Teams/Library trio alongside it.

**Q: Why was my protection disabled?**
A: Your team's bank account (FTB) or your personal/party account (OP&C) didn't have enough balance to pay upkeep. Top up the account — protections restore automatically at the next billing cycle (FTB), or force-load restores automatically once you next have sufficient funds (OP&C).

**Q: Can members claim chunks?**
A: No. On FTB Teams, only owners and officers can claim, unclaim, or force-load chunks for the team. On OP&C, this follows OP&C's own party permission model.

**Q: Can I grant one non-member access to only one chunk?**
A: Yes, on FTB Chunks — open that chunk's player permissions screen and add the player, or set a base rule for All Players on that single chunk. Not available on OP&C.

**Q: What happens when my party disbands?**
A: All claimed chunks are unclaimed (with the configured refund), the remaining team account balance is distributed to members, and the team account is deleted. FTB Teams only.

**Q: Can I disable the war system?**
A: Yes — set `warEnabled = false` server-wide, or have a team opt out permanently with `/lcce war peaceful on`. Wars aren't available on OP&C regardless.

**Q: What's siege mode?**
A: An optional rule (`siegeModeEnabled`) that bypasses a besieged team's explosion protection once they've been at war longer than the configured grace period, so war can mean a claim is actually damageable. PvP and block-edit protection are never affected.

**Q: What's the Pioneer Bonus?**
A: A one-time reward paid to whoever claims the very first chunk ever claimed on the server. FTB Chunks only. Set `pioneerBonusAmount` to `0` to disable the payout.

**Q: Is the web leaderboard safe to leave on?**
A: It's read-only and requires no login, but it has no authentication at all — anyone who can reach the configured port sees player names, chunk counts, and balances. Leave `webEnabled` off, or bind it to `127.0.0.1` behind your own reverse proxy, unless you're fine with it being public.

**Q: Is the login-gated dashboard (`/dashboard`) safe to leave on?**
A: It's gated behind one-time login codes and session cookies, but the built-in server has no HTTPS. If it's reachable outside your LAN, put a TLS-terminating reverse proxy in front of it.

**Q: Where is the config file?**
A: `world/serverconfig/lc_claim_economy-server.toml` on a dedicated server, or `saves/<world>/serverconfig/lc_claim_economy-server.toml` in single-player.

---

More detail, always kept current, on the [GitHub wiki](https://github.com/voidpulsarteam/LCCE/wiki). Found a bug? [Open an issue](https://github.com/voidpulsarteam/LCCE/issues).
