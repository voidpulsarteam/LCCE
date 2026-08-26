# Lightman's Currency: Claim Economy

**Lightman's Currency: Claim Economy** connects [Lightman's Currency](https://www.curseforge.com/minecraft/mc-mods/lightmans-currency) with a chunk-claiming mod, turning claiming and land protection into an economy-driven system. Claiming costs money, protections require ongoing upkeep, and (on the full backend) teams can declare war on each other, place bounties, and race for a one-time "first claim" bonus.

The mod supports **two alternative claim backends** — pick whichever claim mod you already use:

- **FTB Chunks + FTB Teams + FTB Library** — the original, full-featured integration: land/build chunk split, per-chunk player permissions, wars, bounties, the Pioneer Bonus, and native UI buttons built into the FTB claim/team screens.
- **Open Parties and Claims (OP&C)** — a lighter integration covering claiming, upkeep, and force-load billing via a `/lc_claim_economy opc_chunktype` command instead of custom UI. Wars, bounties, and the Pioneer Bonus are not available on this backend (OP&C has no invasion/overclaim concept for wars to map onto, and no party-created API events for bounties to hook).

If both are installed, the FTB integration is used.

> **Minecraft:** 1.21.1 · **Loader:** NeoForge · **Side:** Both (required on server and client)

---

## Requirements

| Mod | Required |
|-----|----------|
| [Lightman's Currency](https://www.curseforge.com/minecraft/mc-mods/lightmans-currency) — **exact version 1.21-2.3.0.5** | ✅ |
| **One of:** | |
| [FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-neoforge) **(exact version 2101.1.20)** + [FTB Teams](https://www.curseforge.com/minecraft/mc-mods/ftb-teams-neoforge) + [FTB Library](https://www.curseforge.com/minecraft/mc-mods/ftb-library-neoforge) | ✅ (or OP&C) |
| [Open Parties and Claims](https://www.curseforge.com/minecraft/mc-mods/open-parties-and-claims) **(exact version 0.27.5)** | ✅ (or FTB) |
| NeoForge 21.1.234+ | ✅ |

> **Version pinning is strict.** This mod hooks internal APIs of Lightman's Currency and of whichever claim backend you use, and checks their versions on startup. If Lightman's Currency, FTB Chunks, or Open Parties and Claims is not installed at exactly the version listed above, **the mod refuses to load** and logs the mismatch instead of silently misbehaving. Check the changelog for which companion versions the release you're downloading was built against.

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
| **How to switch (OP&C)** | `/lc_claim_economy opc_chunktype build` while standing in the chunk | `/lc_claim_economy opc_chunktype land` |

On FTB Chunks, switching a chunk's type is queued to take effect at the next upkeep period (to prevent cost-dodging). On OP&C, the `opc_chunktype` command takes effect immediately.

### Per-Chunk Player Permissions *(FTB Chunks only)*
Each claimed chunk can define **player-specific access** using the same protection categories as team protections:

- Block edit
- Block interact
- Entity interact
- PvP/attack

This system has **no extra cost** and does not affect upkeep pricing — it's a pure permission-management feature, separate from FTB Teams' own role permissions. Open it from the FTB Chunks map with **Shift + middle-click** on a claimed chunk.

You can also set an **All Players (Base)** profile per chunk to define default permissions for non-team players, then add specific players as overrides:

`effective_allow = all_players_base + specific_player_flags`

Only team owners/officers can edit these permissions.

### Team Bank Accounts *(FTB Teams only)*
When an FTB party is created, this mod automatically creates a linked Lightman's Currency bank account for it. The account mirrors the team hierarchy at all times:

- **Team owner → Account owner**
- **Officers → Account admins**
- **Members → Account members**

The account cannot be deleted while the party is alive. When the party disbands, any remaining balance and chunk refunds are distributed to each member's personal account. Joining a party dissolves your personal claims and refunds them to your personal account.

On OP&C, party-owned claims are billed against a resolved party account the same way, but there is no equivalent auto-provisioned/mirrored-hierarchy account object — solo and party ownership are billed directly through OP&C's own owner/party model.

### Protection Upkeep
Protecting your land is not free — protections must be paid for periodically (configurable; default: every hour).

**Upkeep formula:**
```
cost = base_protection_price × number_of_billable_chunks
```

Each active protection adds a per-chunk price to the base rate. Prices per protection are independently configurable.

**If the account runs out of money:**
- **FTB Chunks:** protections are stripped one by one in a configurable priority order. Once the balance is restored, protections are automatically re-enabled in reverse order.
- **OP&C:** there's no per-protection lock to flip, so instead the owner's **force-load** setting is disabled until upkeep is paid again — claims and their protection settings themselves are left alone.
- Changing a protection setting that would make upkeep unaffordable is blocked at the UI (FTB) — the toggle simply does not apply and an alert appears.

Changes to protections and force-loads are queued to the **next upkeep period** on FTB Chunks to prevent mid-period exploits; OP&C changes apply immediately.

### War System *(FTB Teams only, server-configurable)*
Teams with claimed chunks can declare war on each other. War raises upkeep costs:

- **Incoming wars** increase your upkeep exponentially: for base upkeep `b` and `k` incoming wars, the surcharge is `b × Σ(l^n)` for `n = 0..k-1`, where `l` is `warCostMultiplier` (default `1.2`).
- **Outgoing wars** cost you a flat `x × target's base upkeep` per war declared, where `x` is `warOutgoingCostMultiplier` (default `2.0`), regardless of how many wars you already have.

War declarations and endings are queued to the next upkeep period. If a team cannot pay full upkeep, outgoing wars are frozen until the balance is restored. The war system can be disabled entirely per server (`warEnabled`).

The **War screen** (accessible from Team Settings, built into the FTB Teams UI via mixin) shows active and pending wars, upkeep cost breakdowns, and target vulnerability indicators (unprotected block edit, explosions, PvP).

### Bounty System *(FTB Teams only)*
Place a bounty on a rival player or team, payable to whoever kills them in PvP:

- `/lc_claim_economy bounty player <target> <amount>` — escrows the amount from your own account immediately and puts it on a specific player.
- `/lc_claim_economy bounty team <target> <amount>` — same, but on an entire team.
- `/lc_claim_economy bounty list` — see every active bounty and its amount.

A **player bounty** pays out to whoever lands the killing blow in any PvP kill. A **team bounty** only pays out if the killer's team is actually at war with the victim's team — it can't be collected by an unrelated or friendly kill. Multiple bounties on the same target stack. You can't bounty yourself or your own team.

### Pioneer Bonus *(FTB Chunks only)*
The very first chunk ever claimed on a server triggers a one-time, server-wide reward: a configurable currency deposit (`pioneerBonusAmount`, default 5 Diamond coins) straight to the claiming player's account, plus a hidden advancement usable as an FTB Quests (or any advancement-aware) task trigger. Set the amount to `0` to disable the payout while keeping the advancement.

### Claim Milestones
Hidden, toast-free advancements fire automatically at 5 / 10 / 25 / 50 / 100 / 250 / 500 total chunks claimed by a team, plus at declaring or ending a war. These are designed to be used as FTB Quests task triggers without requiring FTB Quests to be installed.

### Force-Load Upkeep
Enabling force-load on a chunk is free, but each force-loaded chunk adds a periodic charge (`forceLoadUpkeepPrice`).

### Bank Dashboard
A standalone bank/upkeep dashboard screen, openable from anywhere with a keybind (default **B**). Unlike most of this mod's UI, it's built without any FTB-library dependency, so it works identically whether you're running the FTB or the OP&C backend.

### Optional Web Leaderboard
A small, optional built-in HTTP server (`webEnabled`, off by default) serves a live, read-only leaderboard page — top balances and top claimed-chunk counts — over plain HTTP, refreshing client-side every ~10 seconds. No login, no write access. Because it exposes player names, balances, and chunk counts to anyone who can reach the port, keep it firewalled or bound to `127.0.0.1` unless you intend it to be public.

### Coin Mint Restriction *(optional)*
`disableCoinMint` lets a server block use of Lightman's Currency's Coin Mint block entirely, closing off a way for players to mint their own currency out of raw materials and bypass the claim economy.

### Tax Collector Placement Restriction *(FTB Teams only)*
Placing Lightman's Currency's Tax Collector block inside a claimed chunk requires the placer's team to own that chunk and hold sufficient purchase rank.

### In-Game UI Extensions
- **Claim prices** and your current balance are displayed directly in the FTB Chunks map UI.
- **Protection prices** are shown next to each toggle in the FTB Teams config screen.
- **Pending state indicators** show queued changes (e.g. "→ Ally pending").
- **Chunk Player Permissions screen** is available from the claim map per chunk.
- **Claim cost breakdown** popup shows current price/balance plus a bulk-claim cost projection.

---

## Installation

1. Download **Lightman's Currency: Claim Economy** and place the `.jar` in your `mods/` folder.
2. Install Lightman's Currency (exact version above) and **either** the full FTB Chunks/Teams/Library trio **or** Open Parties and Claims (exact versions above) into the same `mods/` folder.
3. Start the server (or single-player world). The mod generates its config file automatically on first launch. If a required companion mod is missing or at the wrong version, the mod will refuse to start and log exactly what's wrong.
4. Configure the mod to your liking (see below).

> **Important:** The mod must be installed on **both the server and every client** — it registers custom network payloads and client-side UI mixins.

---

## Server Configuration

The config file is generated at:
```
world/serverconfig/lc_claim_economy-server.toml
```

Reload it by restarting the server or using `/reload` (some values apply immediately).

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

### `flavor`

| Key | Default | Description |
|-----|---------|--------------|
| `pioneerBonusAmount` | `50000` | One-time reward for the first chunk ever claimed (FTB Chunks only). `0` disables the payout. |

### `debug`

| Key | Default | Description |
|-----|---------|--------------|
| `debugTestTeamCommands` | `false` | Enables `/lc_claim_economy seed_test_teams` / `clear_test_teams` / `count_test_teams`. Keep off in production. |

---

## Getting Started (In-Game Tutorial — FTB Chunks path)

### Step 1 — Get some money
Players need a Lightman's Currency bank account with a positive balance. Place an **ATM** or use your wallet to deposit money. As a server operator, you can give money directly:
```
/lcbank give players <playername> <amount>
```

### Step 2 — Claim your first chunks
Open the FTB Chunks map (`M` by default). Click a chunk to claim it. The claim price is deducted from your bank account automatically. If you don't have enough, the claim is rejected with a chat message.

Unclaiming a chunk refunds part of the price (80% by default).

### Step 3 — Switch chunks to Land (optional)
By default all claimed chunks are **Build chunks**. To mark some as **Land chunks** (cheaper upkeep, restricted protections), hold **Alt** and click or drag over chunks in the FTB Chunks map. A confirmation message appears in chat.

Land chunks use separate block interact and block edit protection settings, visible in the FTB Teams config under *"Land Chunk Protection"*.

### Step 3.5 — Configure per-player permissions per chunk (optional)
Open the FTB Chunks map and use **Shift + middle-click** on one of your claimed chunks to open **Chunk Player Permissions**.

In this screen you can:
- Set **All Players (Base)** permissions for that chunk.
- Add specific players by name or UUID.
- Toggle B / I / E / P permissions per player.

This applies only to that exact chunk and does not change team-wide protection settings.

### Step 4 — Enable protections
Open your FTB Teams settings and navigate to the protection properties. Each protection shows its per-chunk price next to the toggle. Enable protections you want to pay for. If your balance is too low to afford the new upkeep, the change is blocked and reverted.

Protections take effect immediately; the upkeep for them will be charged at the next billing period.

### Step 5 — Set up a party (for teams)
Create a party via FTB Teams. This mod automatically creates a linked bank account for the team. Fund the team account through any ATM (select the team account from the account list).

Only **owners and officers** can claim chunks, force-load them, or manage war for the team.

### Step 6 — Force-loading chunks
In the FTB Chunks map, enable force-load on any of your claimed chunks as usual. There is no upfront cost, but each force-loaded chunk adds a charge to the next upkeep bill.

### Step 7 — Monitor upkeep
When upkeep is charged, a chat message summarises the payment. Click **[See more]** in that message to view a full breakdown including which protections were active and what each cost.

Owners and officers can also run:
```
/lc_claim_economy upkeep_details
```
to see the most recent breakdown at any time, and:
```
/lc_claim_economy upkeep_priority
```
to see the order in which protections would be dropped if upkeep fails.

### Step 8 — War (optional)
If the war system is enabled, open your FTB Teams settings and click the **War** button (visible to owners and officers). The war screen shows:
- **Declared war on you** — incoming wars and the upkeep penalty each adds.
- **War you declared** — your active outgoing wars and their costs.
- **Declare war on** — all eligible teams you can target.

Click a team row to declare war (pending until the next upkeep period) or to queue an end to an existing war. War costs are shown in the tooltips before confirming.

> Declaring war increases your periodic upkeep. Make sure your team account can cover the new total before committing.

### Step 9 — Bounties (optional)
Put money on a rival's head, and collect on anyone who does the same to you:
```
/lc_claim_economy bounty player <playername> <amount>
/lc_claim_economy bounty team <teamname> <amount>
/lc_claim_economy bounty list
```

---

## Getting Started (Open Parties and Claims path)

Claiming, unclaiming, and force-load upkeep work the same way through OP&C's own claim tools — this mod bills them automatically in the background. There is no dedicated claim-price UI overlay on the OP&C side, so keep an eye on your bank balance directly.

To split chunks into cheaper "land" billing versus full "build" billing, stand in the chunk and run:
```
/lc_claim_economy opc_chunktype land
/lc_claim_economy opc_chunktype build
/lc_claim_economy opc_chunktype status
```
Unlike the FTB path, this takes effect immediately rather than at the next upkeep period.

If upkeep can't be paid, your force-load setting is disabled until the balance is restored — claims themselves are left alone. Wars, bounties, and the Pioneer Bonus are not available on this backend.

---

## Commands

All commands are under `/lc_claim_economy`.

| Command | Backend | Who can use it | Description |
|---------|---------|-----------------|--------------|
| `upkeep_details` | FTB | Owners & officers (or ranked members) | Show the latest upkeep cost breakdown |
| `upkeep_priority` | FTB | Owners & officers (or ranked members) | Show the protection dismantle order and current active costs |
| `bounty player <target> <amount>` | FTB | Anyone | Place an escrowed bounty on a player |
| `bounty team <target> <amount>` | FTB | Anyone | Place an escrowed bounty on a team |
| `bounty list` | FTB | Anyone | List all active bounties |
| `leaderboard` / `leaderboard land` | Either | Anyone | Rank teams by claimed-chunk count |
| `leaderboard wealth` | Either | Anyone | Rank teams by bank balance |
| `quest_deposit <amount>` | FTB | Server (op level 2) | For use as an FTB Quests command reward, to pay a quest reward into a player's/team's bank account |
| `clear_wars` | Either | Server (op level 2) | Clears all active/pending war state for every tracked team |
| `opc_chunktype land / build / status` | OP&C | Claim/party owner or admin | Set or check a chunk's land/build billing type |
| `seed_test_teams`, `clear_test_teams`, `count_test_teams` | FTB | Server (op level 2), requires `debugTestTeamCommands=true` | Debug tools for seeding/removing fake test teams |

War declarations and endings are not a command — they're driven entirely from the in-game **War screen** in FTB Teams settings.

---

## FAQ

**Q: The mod won't load / crashes on startup with a version error.**
A: This mod pins exact versions for Lightman's Currency and whichever claim backend you use (see Requirements above). The startup error message names exactly which mod and version is wrong — install the matching version, not just "latest."

**Q: Can I run this with both FTB Chunks and Open Parties and Claims installed?**
A: Yes, but only the FTB integration will be active for the economy features (wars, bounties, Pioneer Bonus, per-chunk permissions). If you specifically want the OP&C integration, don't install the FTB Chunks/Teams/Library trio alongside it.

**Q: Why was my protection disabled?**
A: Your team's bank account (FTB) or your personal/party account (OP&C) did not have enough balance to pay upkeep. Top up the account and it will be restored — protections automatically at the next billing cycle (FTB), or force-load automatically once you next have sufficient funds (OP&C).

**Q: Can members claim chunks?**
A: No. On FTB Teams, only owners and officers can claim, unclaim, or force-load chunks on behalf of the team. On OP&C, this follows OP&C's own party permission model.

**Q: Can I grant one non-member access to only one chunk?**
A: Yes, on FTB Chunks. Open that chunk's player permissions screen and add the player there. You can also set a base rule for **All Players** on that single chunk. This feature isn't available on the OP&C backend.

**Q: What happens when my party disbands?**
A: All claimed chunks are unclaimed (with the configured refund), the remaining balance in the team account is distributed to members, and the team account is deleted. (FTB Teams only.)

**Q: Can I disable the war system?**
A: Yes. Set `warEnabled = false` in the server config. The war button disappears from the client UI automatically. (Wars aren't available on OP&C regardless of this setting.)

**Q: What's the Pioneer Bonus?**
A: A one-time reward paid to whoever claims the very first chunk ever claimed on the server. FTB Chunks only. Set `pioneerBonusAmount` to `0` to disable the payout.

**Q: Is the web leaderboard safe to leave on?**
A: It's read-only and requires no login, but it has no authentication at all — anyone who can reach the configured port sees player names, chunk counts, and balances. Leave `webEnabled` off, or bind it to `127.0.0.1` and put it behind your own reverse proxy, unless you're fine with it being public.

**Q: Where is the config file?**
A: `world/serverconfig/lc_claim_economy-server.toml` on a dedicated server, or `saves/<world>/serverconfig/lc_claim_economy-server.toml` in single-player.
