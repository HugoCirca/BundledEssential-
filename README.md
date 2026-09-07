# BundledEssential

A lightweight, low-resource Minecraft plugin that bundles essential teleportation, location, and economy commands into one jar. No external dependencies required.

**Works with Spigot, Paper, and forks (1.13+)**

> **Docs have moved to the Wiki** — this README is a quick start. See [`wiki/Home.md`](wiki/Home.md) or browse `wiki/` for full details.

[![Wiki](https://img.shields.io/badge/docs-wiki-blue)](wiki/Home.md)

---

## Features (overview)

| Module | Highlights |
|--------|------------|
| **TPA** | `/tpa` family with `here/accept/auto` |
| **Home/Back/Waypoints** | One home, death back, 27 waypoints GUI |
| **Trade** | 54-slot anti-scam GUI |
| **Economy** | Shop 15 cats + search, `/sell`, pay 5% tax, bounty 20%, Bank 2B cap |
| **Loans** | `/loan` bedrock 200-2500 + custom, 3/7/14d or slow 20% |
| **Auto-Sell/Spawners** | $500 chest, zombie $250/skeleton $500 stack to 35x (iron+ pick) |
| **Level/Quests/Playtime** | XP levels, 15 quests, streaks, opt-vault |
| **Dynamic Light** | Fake `Light` via `sendBlockChange` |
| **Utils** | `/craft`, `/repair`, `/giveaway`, `/bundledreload` |

- **Auto-updater** + **Bank** + **Dynamic Pricing** — see [Updater](wiki/Updater.md) and [Economy](wiki/Economy.md)

---

## Wiki

Full docs are in [`wiki/`](wiki/):

- [TPA](wiki/TPA.md) · [Home](wiki/Home.md) · [Waypoints](wiki/Waypoints.md) · [Trade](wiki/Trade.md)
- [Economy](wiki/Economy.md) · [Loan](wiki/Loan.md) · [Shop](wiki/Shop.md) · [AutoSell](wiki/AutoSell.md) · [Spawners](wiki/Spawners.md)
- [Leveling](wiki/Leveling.md) · [Playtime](wiki/Playtime.md) · [Quests](wiki/Quests.md) · [Dynamic Light](wiki/DynamicLight.md)
- [Commands](wiki/Commands.md) · [Configuration](wiki/Configuration.md) · [Permissions](wiki/Permissions.md) · [Updater](wiki/Updater.md)

Each page is curated for that module (tabs, limits, files, config keys).

---

## Installation

1. Download `BundledEssential-X.X.X.jar` from [Releases](https://github.com/HugoCirca/BundledEssential-/releases)
2. Place in `plugins/` folder
3. Restart

The plugin self-updates via `/bundledupdate`.

---

## Building from Source

Requires **Java 17+** and **Gradle**.
```bash
git clone https://github.com/HugoCirca/BundledEssential-.git
cd BundledEssential-
./gradlew clean build
```
Jar in `build/libs/`.

---

## Configuration

Toggle in `features.yml`, tuning in `config.yml` (`economy.balance-cap`, `autosell.*`, etc.). Details: [Configuration](wiki/Configuration.md). Live reload: `/bundledreload` (also `bereload`).

Data files in `plugins/BundledEssential/` — see wiki.

---

## Permissions

Open by default. Restrict via `bundleessential.*` — see [Permissions](wiki/Permissions.md).

---

## License

MIT
