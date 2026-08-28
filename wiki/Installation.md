# Installation

## Requirements

| Mod | Version |
|---|---|
| [Lightman's Currency](https://www.curseforge.com/minecraft/mc-mods/lightmans-currency) | **exactly** `1.21-2.3.0.5` |
| **One of:** | |
| [FTB Chunks](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-neoforge) + [FTB Teams](https://www.curseforge.com/minecraft/mc-mods/ftb-teams-neoforge) + [FTB Library](https://www.curseforge.com/minecraft/mc-mods/ftb-library-neoforge) | `2101.1.21` / `2101.1.10` / `2101.1.32` **or newer** |
| [Open Parties and Claims](https://www.curseforge.com/minecraft/mc-mods/open-parties-and-claims) | `0.27.5` **or newer** |
| NeoForge | `21.1.234` or newer |
| Minecraft | `1.21.1` |

If both FTB Chunks and Open Parties and Claims are installed, only the FTB integration is active — economy features that depend on FTB Teams (wars, bounties, marketplace, Pioneer Bonus, per-chunk permissions) won't be available through OP&C in that setup.

## Version pinning

This mod hooks internal implementation details of Lightman's Currency and of whichever claim backend you use — not just their public APIs — and checks versions on startup:

- **Lightman's Currency is checked as an exact match.** It's a required dependency this mod is compiled against a large API surface of, where even a small version bump is more likely to carry a breaking change.
- **FTB Chunks and Open Parties and Claims are checked as a minimum version.** Earlier releases pinned these to an exact point version too, which meant every routine bugfix release of either mod broke this mod's loading until it was manually re-pinned. Since 4.4.0, any version at or above the minimum listed works.

If a requirement is missing or doesn't satisfy its check, **the mod refuses to start** and logs exactly which mod and version is wrong — check your log rather than guessing.

## Steps

1. Download the mod jar and place it in your `mods/` folder.
2. Install Lightman's Currency (exact version above) and **either** the full FTB Chunks/Teams/Library trio **or** Open Parties and Claims (minimum versions above) into the same `mods/` folder.
3. Start the server (or single-player world). The config file is generated automatically on first launch at:
   ```
   world/serverconfig/lc_claim_economy-server.toml
   ```
   If a required companion mod is missing or too old, startup fails with a clear error naming the problem.
4. Tune the config to your liking — see [Configuration](Configuration).
5. In-game, follow [Getting Started](Getting-Started) to walk through claiming, upkeep, and (if using FTB) wars and bounties.

> **Both sides required.** Install the mod on **every client as well as the server** — it registers custom network payloads and client-side UI.
