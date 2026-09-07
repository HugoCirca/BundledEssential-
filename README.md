# BundledEssential

A lightweight, low-resource Minecraft plugin that bundles essential teleportation, location, and economy commands into one jar. No external dependencies required.

**Works with Spigot, Paper, and forks (1.13+)**

---

## Features

| Module | Commands |
|--------|----------|
| **TPA** | `/tpa`, `/tpaccept`, `/tpahere`, `/tpaauto` (aliases `/tpa here/accept/auto`) |
| **Home** | `/home`, `/sethome`, `/removehome` (`/home set/remove`) |
| **Back** | `/back` |
| **Waypoints** | `/waypoint` |
| **Trade** | `/trade`, `/tradeaccept`, `/tradecancel` (`/trade accept/cancel`) |
| **Economy** | `/shop`, `/sell`, `/sellgui`, `/balance`, `/resetbal`, `/serverbank`, `/pay`, `/paytax`, `/bounty`, `/repair` |
| **Loan** | `/loan` (bedrock GUI 200-2500 + custom anvil, 3/7/14/30d or slow 20%) |
| **Auto-Sell** | `/autosell` (chest in `/shop` Custom tab) |
| **Spawner** | _(stackable zombie $250 + skeleton $500 in `/shop` Custom tab, iron+ pick to keep)_ |
| **Leveling** | `/level` |
| **Quests & Daily** | `/quest`, `/daily` |
| **Playtime** | `/playtime` (leaderboard, `optin/optout/vault` silent farm) |
| **Dynamic Light** | _(automatic — client-side fake light, no block place)_ |
| **Help** | `/bundledhelp`, `/be`, `/bundledreload`, `/bundleversion` |

- **Auto-updater** — Checks for updates on startup, downloads and applies on next restart (console can use `/bundledupdate` too)
- **Dynamic Pricing** — Shop prices drift based on market simulation and inflation
- **Server Bank** — Taxes + overflow beyond `economy.balance-cap` feed the bank (capped at 2B, reset via `/resetserverbal`)

---

## Commands

### TPA (Teleport Request)

| Command | Description |
|---------|-------------|
| `/tpa <player>` | Send a teleport request to a player |
| `/tpa here <player>` | Request a player to teleport to you |
| `/tpa accept` | Accept a pending teleport request |
| `/tpa auto [on|off]` | Toggle instant auto-accept (saved) |
| `/tpahere <player>` / `/tpaccept` | Legacy aliases (still work) |
| `/tpaauto [on|off]` | Legacy alias for `/tpa auto` |

- Requests expire after **30 seconds**
- You cannot TPA to yourself

### Home

| Command | Description |
|---------|-------------|
| `/home` | Teleport to your home |
| `/home set` | Set your home at your current location |
| `/home remove` | Remove your home |
| `/sethome` / `/removehome` | Legacy aliases |

- Each player can only have **one home**

### Back

| Command | Description |
|---------|-------------|
| `/back` | Return to your last death location |

- Death location is stored per-player and cleared after use

### Waypoints

| Command | Description |
|---------|-------------|
| `/waypoint` | Open the waypoint GUI |
| `/waypoint new <name>` | Create a waypoint at your location |
| `/waypoint delete <name>` | Delete a waypoint |
| `/waypoint <name>` | Teleport to a waypoint by name |

- Maximum of **27 waypoints** per player

### Trade

| Command | Description |
|---------|-------------|
| `/trade <player>` | Send a trade request to a player |
| `/trade accept` | Accept a pending trade request (opens GUI) |
| `/trade cancel` | Cancel a pending request or an open trade |
| `/tradeaccept` / `/tradecancel` | Legacy aliases |

- Requests expire after **30 seconds**
- Both sides accept via green pane; changing offer resets both accepts (anti-scam)
- Closing, `/trade cancel` or quit returns items

### Economy

| Command | Description |
|---------|-------------|
| `/shop` | Open the shop with categories |
| `/shop search <name>` | Jump straight to matching items (Bedrock-friendly) |
| `/sell` | Sell the item in your main hand |
| `/sellgui` | Open sell GUI — put items in, close to sell |
| `/balance` | Check your balance |
| `/balance <player>` | Check another player's balance (also offline) |
| `/resetbal <player> [amount]` | **Admin** reset/set balance (clamped to cap, negatives allowed for loans) |
| `/serverbank` | Show Server Bank |
| `/serverbank history` | History book (time/player/amount/reason, console prints last 10) |
| `/resetserverbal` | **Admin** wipe Bank to $0 (`resetbank` alias) |
| `/loan` | Open loan GUI (see Loans) |
| `/pay <player> <amount>` | Pay a player (5% tax → Bank, garnished if overdue) |
| `/bounty <player> [amount]` | Set or check a bounty (20% tax → Bank on claim) |
| `/paytax` | Pay accumulated taxes → Bank |
| `/repair [full]` | Repair held item (cost scales with durability) |
| `/bundledreload` / `bereload` | Reload `config.yml` + `features.yml` live (console + admins) |

#### Balance Cap & Server Bank
- Per-player cap is `economy.balance-cap` in `config.yml` (default `1,000,000,000,000` = 1T — add zeros to taste, reload via `/bundledreload`)
- `balance` is clamped on save/load; overflow from any earning (`/sell`, playtime, kills, quests, autosell) → Bank
- All taxes: `/pay` 5% and bounty 20% (immediate to Bank), `garnish` 25% of playtime/kills when in tax debt → Bank, `/paytax` → Bank
- Bank is in `serverbank.json`, capped at **2B** (`/resetserverbal` wipes, `/serverbank history` book)
- E at cap sees "Capped!" and earnings feed Bank

#### Loans (`LoanManager`)
| Command | Description |
|---------|-------------|
| `/loan` | Chest GUI — bedrock `200/500/1000/1500/2000/2500` + compass custom (anvil 50-10000) → pick repayment (max 14d). Pending loans show as **paper** click-to-pay |
| `/loan pay [id|all]` | Repay lump loan early (on-time +$25 bonus, overdue no bonus, can go negative) |
| `/loan info` | List active loans (id, debt, mode, due) |

- Repay modes: **3/7/14 days lump** (max 2 weeks, must `/loan pay` or paper click before due) or **Slow Deduct** (20% of each earning auto-pays, no due date)
- Late lump: +5%/day compound, adds to `debt`; `/loan pay` can go negative as punishment
- Slow fully paid → +$25 bonus; cap total debt $10000 per player
- Data: `loans.json`

#### Money Sources
- **Kill mobs** — $0.01 to $10.00 (random, split among damagers)
- **Playtime** — ~$10.00 every 5 minutes ($8-12 base, scaled by level, tunable under `economy:`; `/playtime optout` silent farms to personal vault, `/playtime optin` claims vault)
- **Bounty claims** — Kill a player with a bounty to claim it (20% → Bank, placers can't claim own)
- **Sell** — 60% of buy price + enchant bonus; autosell chests tick at interval

#### Shop Categories
- **Logs, Stone, Ores, Crops, Mob Drops, Food, Tools, Armor, Building, Decoration, Redstone, Nether, End, New 1.21-26.2, Misc, Custom**
- **Custom**: Auto-Sell Chest ($500) + Zombie Spawner $250 + Skeleton Spawner $500 (bottom row)
- **Search**: compass anvil or `/shop search <name>` (Bedrock-friendly)
- **Bulk**: click stackable → panes for 1-64 / 10/25/50; unstackables instant 1

#### Dynamic Pricing
- Prices drift ±5-10% every 5 minutes, inflation over uptime, enchant bonus, 60% sell

### Auto-Sell Chest

| Command | Description |
|---------|-------------|
| `/autosell` | How it works + price |
| `/autosell give <player> [amount]` | Admin handout (op only) |

- Right-click air to set interval (30s/1m/5m/10m) + recipients (equal split, offline too); sneak+right-click placed to reconfigure; hoppers feed it

### Spawners

- Zombie $250, Skeleton $500 — **iron pickaxe or better** required to keep on break (else cancelled). Right-click same-type to stack to 35x, keep rate on break, hologram label.

### Leveling

| Command | Description |
|---------|-------------|
| `/level` | Check your level, XP progress and playtime bonus |
| `/level <player>` | Check another player's level |

- Need `base-xp x multiplier^(level-1)` (default `100 x 1.5`), higher level boosts playtime prize

### Playtime

| Command | Description |
|---------|-------------|
| `/playtime` | Check your total online time |
| `/playtime <player>` | Check another player's time (offline too) |
| `/playtime leaderboard` / `top` | Top 10 |
| `/playtime optout` | Silent farm earnings to personal vault (no messages) |
| `/playtime optin` | Claim vault + resume messages |
| `/playtime vault` | Show vault |

### Quests & Daily

| Command | Description |
|---------|-------------|
| `/quest` | Check current quest progress |
| `/quest claim` | Claim finished quest — new one instantly |
| `/quest skip` | Ditch current quest for fresh roll |
| `/daily` | Claim daily streak (aliases `/login`, `/claim`, `/streak`) |

- 15 tasks $20-90; login streak `base+(N-1)*per-day+random` + weekly $50 + monthly $200; placed shop blocks don't count

### Dynamic Light

No command — hold light in either hand → client-side `Light` via `sendBlockChange` at feet/eye, no real block placed so crouch/buckets/liquids never break. Freezes near spawners (radius `dynamic-light.spawner-freeze-radius`). Tune `interval-ticks`.

### Help

| Command | Description |
|---------|-------------|
| `/bundledhelp` | Get the guide book |
| `/be help/version/update` | Consolidated hub (`/bundle` alias) |
| `/bundledreload` | Reload config + features live |
| `/bundleversion` / `/bundledupdate` | Version / updater |

---

## Auto-Updater

Checks GitHub Releases on startup. If new: download to `plugins/update/` → next restart replaces jar. Checksum-verified, backup kept in `plugins/BundledEssential-backups/` (last 3), auto-restore on fail. `/bundledupdate` shows pending.

---

## Installation

1. Download latest `BundledEssential-X.X.X.jar` from [Releases](https://github.com/HugoCirca/BundledEssential-/releases)
2. Place in `plugins/` folder
3. Restart

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

## CI/CD

GitHub Actions on push to `main`: bump patch, build, commit, release.

---

## Configuration

Toggle every feature in `plugins/BundledEssential/features.yml` (tpa, home, back, waypoints, trade, economy, bounty, pay, shop, sell, leveling, rewards, autosell, spawner, playtime, dynamic-light, updater) — set `false` and restart to disable. Fine-tuning stays in `config.yml` (e.g., `economy.balance-cap`, `economy.playtime-min/max-reward`, `leveling.*`, `rewards.*`, `autosell.*`, `spawner.*`, `dynamic-light.*`).

All player data is stored automatically in:
- `homes.yml`, `balances.json` + `serverbank.json`, `loans.json`, `levels.json`, `playtime.json` + `playtime_opt.json`, `rewards.json`, `autosell.json`, `spawner.json`, `shop.json` (price overrides)

No manual configuration needed; `/bundledreload` applies `config.yml`/`features.yml` without restart.

---

## Permissions

All commands are **open by default**. To restrict, add:

```yaml
bundleessential.tpa: true
bundleessential.home: true
bundleessential.back: true
bundleessential.waypoint: true
bundleessential.trade: true
bundleessential.economy: true
bundleessential.update: true
bundleessential.admin: true   # resetbal, serverbank details, breload
bundleessential.reload: true
bundleessential.resetbal: true
bundleessential.giveaway: true
```

---

## License

MIT
