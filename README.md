# BundledEssential

A lightweight, low-resource Minecraft plugin that bundles essential teleportation, location, and economy commands into one jar. No external dependencies required.

**Works with Spigot, Paper, and forks (1.13+)**

---

## Features

| Module | Commands |
|--------|----------|
| **TPA** | `/tpa`, `/tpaccept`, `/tpahere` |
| **Home** | `/sethome`, `/removehome`, `/home` |
| **Back** | `/back` |
| **Waypoints** | `/waypoint` |
| **Trade** | `/trade`, `/tradeaccept`, `/tradecancel` |
| **Economy** | `/shop`, `/sell`, `/sellgui`, `/balance`, `/pay`, `/paytax`, `/bounty`, `/repair` |
| **Auto-Sell** | `/autosell` (chest in `/shop` Custom tab) |
| **Spawner** | _(stackable zombie spawner in `/shop` Custom tab)_ |
| **Leveling** | `/level` |
| **Quests & Daily** | `/quest`, `/daily` |
| **Playtime** | `/playtime` |
| **Dynamic Light** | _(automatic — hold a light)_ |
| **Help** | `/bundledhelp` |

- **Auto-updater** — Checks for updates on startup, downloads and applies on next restart (console can use `/bundledupdate` too)
- **Dynamic Pricing** — Shop prices drift based on market simulation and inflation

---

## Commands

### TPA (Teleport Request)

| Command | Description |
|---------|-------------|
| `/tpa <player>` | Send a teleport request to a player |
| `/tpahere <player>` | Request a player to teleport to you |
| `/tpaccept` | Accept a pending teleport request |

- Requests expire after **30 seconds**
- You cannot TPA to yourself

### Home

| Command | Description |
|---------|-------------|
| `/sethome` | Set your home at your current location |
| `/removehome` | Remove your home |
| `/home` | Teleport to your home |

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
- Click a waypoint in the GUI to teleport

### Trade

| Command | Description |
|---------|-------------|
| `/trade <player>` | Send a trade request to a player |
| `/tradeaccept` | Accept a pending trade request (opens the trade GUI) |
| `/tradecancel` | Cancel a pending request or an open trade |

- Requests expire after **30 seconds**
- Accepting opens a shared **trade GUI**: your offer on your side, glass divider in the middle, both players' heads at the bottom
- Put items on your side, click the **green pane** to accept — changing any offer resets both accepts (anti-scam)
- When **both** accept, items swap. Closing, `/tradecancel` or logging out returns everyone's items

### Economy

| Command | Description |
|---------|-------------|
| `/shop` | Open the shop with categories |
| `/shop search <name>` | Jump straight to matching items (Bedrock-friendly, no anvil needed) |
| `/sell` | Sell the item in your main hand |
| `/sellgui` | Open sell GUI — put items in, close to sell |
| `/balance` | Check your balance |
| `/balance <player>` | Check another player's balance |
| `/pay <player> <amount>` | Pay a player |
| `/bounty <player> [amount]` | Set or check a bounty |

#### Money Sources
- **Kill mobs** — $0.01 to $10.00 (random)
- **Playtime** — $2.00 to $5.00 every 5 minutes, scaled up by your level (+10% per level by default, tunable in `config.yml` under `economy:`)
- **Bounty claims** — Kill a player with a bounty to claim it (20% tax, placers can't claim their own)

#### Shop Categories
- **Logs** — All logs, woods, planks, saplings, leaves (incl. Cherry, Pale Oak)
- **Stone** — Cobblestone, Stone, Deepslate, Granite, Sandstone, Tuff, dirt, sand, etc.
- **Ores** — Coal, Iron, Copper, Gold, Redstone, Lapis, Diamond, Emerald, Quartz, Amethyst, Netherite, Resin
- **Crops** — Wheat, Carrot, Potato, Melon, Pumpkin, berries, mushrooms, all flowers, Torchflower, Eyeblossom, etc.
- **Mob Drops** — Bone, String, Gunpowder, Ender Pearl, Blaze/Breeze Rods, heads, Totem, Nether Star, Elytra, etc.
- **Food** — Raw + cooked meat and fish, bread, cake, stews, golden foods
- **Tools** — All tiers incl. Mace, bows, buckets, boats, bundles, compasses, minecarts
- **Armor** — All tiers, horse armor, Wolf Armor, Harnesses, armor trims
- **Building** — All wool, concrete, terracotta, glass, stairs/slabs/walls, quartz, prismarine, copper, sulfur/cinnabar
- **Decoration** — Furniture, lights, candles, beds, banners, shulker boxes, music discs, shelves, copper chests
- **Redstone** — Pistons, rails, Crafter, Copper Bulbs, sculk, TNT
- **Nether** — Full Nether set incl. Blackstone, Basalt, Nylium, Netherite
- **End** — End Stone, Purpur, Chorus, all Shulker Boxes, Dragon Egg, Elytra
- **New 1.21-26.2** — Copper/Tuff variants, Pale Garden, Resin, Happy Ghast gear, Sulfur & Cinnabar sets, new discs
- **Custom** — Auto-Sell Chest + Zombie Spawner (barrier icon, bottom-right), see below
- **Search** — Compass button in `/shop` opens an anvil: type a name, land on a results page. No anvil? Use `/shop search <name>` instead (works everywhere, incl. Bedrock)
- **Bulk buying** — Click any stackable item, then the red/green panes to pick 1-64 (shift-click = 10 at a time), confirm to buy the stack. Unstackables (tools/weapons) still buy instantly

#### Dynamic Pricing
- Prices drift ±5-10% every 5 minutes
- Inflation grows slowly over server uptime
- Enchanted items sell for bonus money
- Sell price is 60% of current buy price
- High-end items (Elytra, Totem, Netherite, Dragon Egg...) keep premium prices

### Auto-Sell Chest

| Command | Description |
|---------|-------------|
| `/autosell` | How it works + price |
| `/autosell give <player> [amount]` | Admin handout (op only) |

- Buy the chest in `/shop` (Custom tab, $500 by default). **Right-click air** holding it to set the sell interval (30s/1m/5m/10m) and pick recipients via player heads (online + offline, equal split). **Sneak + right-click** a placed one to reconfigure it
- Place it, drop items in by hand or feed it with **hoppers** — contents sell automatically every interval at normal `/sell` prices, split equally between recipients (works for offline players too)
- Breaking it returns the chest (config kept); explosions destroy it like a normal chest
- Tune in `config.yml` under `autosell:` (price, default interval, interval options, max recipients)

### Zombie Spawner

- Buy the spawner in `/shop` (Custom tab, $500 by default). Place it for a normal zombie spawner labeled **Zombie 1x**
- **Right-click** a placed spawner holding another spawner item to consume it and raise the rate (2x, 3x... up to **35x**). Each extra level spawns bonus zombies every spawner cycle
- Breaking it returns the spawner **with its rate kept**, so relocating loses nothing. Explosions destroy it like normal
- Tune in `config.yml` under `spawner:` (price, max-multiplier)

### Leveling

| Command | Description |
|---------|-------------|
| `/level` | Check your level, XP progress and playtime bonus |
| `/level <player>` | Check another player's level |

- Collect **XP orbs** to earn server XP and level up
- Each level needs more XP than the last: `base-xp x multiplier^(level-1)` (default `100 x 1.5`)
- Higher level boosts your playtime money prize
- Configurable in `config.yml` under `leveling:` (enabled, base-xp, multiplier, playtime-bonus-per-level)

### Playtime

| Command | Description |
|---------|-------------|
| `/playtime` | Check your total online time |
| `/playtime <player>` | Check another player's time (works offline) |
| `/playtime leaderboard` | Top 10 players by playtime |

### Quests & Daily

| Command | Description |
|---------|-------------|
| `/quest` | Check current quest progress |
| `/quest claim` | Claim a finished quest — a new one starts instantly, no waiting |
| `/quest skip` | Ditch the current quest for a fresh roll (no reward, no penalty) |
| `/daily` | Claim daily streak reward, auto-granted on join (aliases `/login`, `/claim`, `/streak`) |

- **Quests** — 15 repeatable tasks: mine stone/ores, harvest crops, hunt hostiles, catch real fish, breed/tame/shear animals, enchant, smelt, brew, eat, sleep, visit dimensions, gain XP levels. Rewards $20-90 (ores, hunts and enchants pay most). Claiming instantly rolls your next quest
- **Login streak** — claim every day to grow the streak: Day N pays `base + (N-1) x per-day + random`, plus a bonus every 7th day ($50) and 30th day ($200) by default (day 1 ~$25, day 365 ~$1850)
- Player-placed blocks (shop-bought ores, etc.) never count toward quests — no buy-and-break farming
- Tune everything in `config.yml` under `rewards:` (login base/per-day/random/bonuses, daily reward multiplier)

### Dynamic Light

No command — just hold anything with a light property in either hand and it glows around you: torches, lanterns, lava buckets, glowstone, shroomlight, sea lanterns, froglights, end rods, jack o'lanterns, campfires, beacons, conduits, crying obsidian, amethyst buds and more.

- Places a real invisible Light block at your feet (only ever replaces air)
- Light follows you, updates its level, and is removed on logout/shutdown
- No extra dynamic-lights plugin needed
- Configurable in `config.yml` under `dynamic-light:` (enabled, interval-ticks)

### Help

| Command | Description |
|---------|-------------|
| `/bundledhelp` | Get the guide book with all commands |
| `/bundleversion` | Show the installed plugin version |

---

## Auto-Updater

The plugin automatically checks for new versions on startup via GitHub Releases. If an update is found:

1. The new JAR is downloaded to `plugins/update/`
2. On next server restart, the old JAR is replaced with the new one

No forced restarts — updates apply naturally.

- Downloads are checksum-verified against the release's published SHA-256 (size check as fallback); bad files are deleted, never applied
- The replaced jar is kept as a versioned backup in `plugins/BundledEssential-backups/` (last 3)
- A failed apply restores the backup automatically
- `/bundledupdate` tells you if an update is already pending on disk

Players with `bundleessential.update` permission (and console) can run `/bundledupdate` to check/download manually.

---

## Installation

1. Download the latest `BundledEssential-X.X.X.jar` from [Releases](https://github.com/HugoCirca/BundledEssential-/releases)
2. Place the jar in your server's `plugins/` folder
3. Restart the server

The plugin will keep itself updated automatically.

---

## Building from Source

Requires **Java 17+** and **Gradle**.

```bash
git clone https://github.com/HugoCirca/BundledEssential-.git
cd BundledEssential-
./gradlew clean build
```

The compiled jar will be in `build/libs/`.

---

## CI/CD

The GitHub Actions workflow automatically:

1. Bumps the patch version in `build.gradle`
2. Builds the plugin
3. Commits the version bump
4. Creates a GitHub release with the new tag

Just push to `main` and a new release is created.

---

## Configuration

Toggle every feature in `plugins/BundledEssential/features.yml` (tpa, home, back,
waypoints, trade, economy, bounty, pay, shop, sell, leveling, rewards, autosell,
spawner, playtime, dynamic-light, updater) — set `false` and restart to disable.
Fine-tuning values stay in `config.yml` (leveling rates, light interval,
`rewards.login` streak payouts, `rewards.daily` multiplier, `autosell` chest price,
`spawner` price, playtime base rewards under `economy:`).

All player data is stored automatically in:
- `plugins/BundledEssential/homes.yml` - Home locations
- `plugins/BundledEssential/config.yml` - Waypoint locations
- `plugins/BundledEssential/balances.json` - Player balances
- `plugins/BundledEssential/levels.json` - Player levels and XP
- `plugins/BundledEssential/playtime.json` - Player online time
- `plugins/BundledEssential/rewards.json` - Quest progress, login streaks, anti-farm block list
- `plugins/BundledEssential/autosell.json` - Auto-sell chest locations + configs
- `plugins/BundledEssential/spawner.json` - Boosted spawner locations + rates
- `plugins/BundledEssential/shop.json` - Shop price overrides (delete to reset to defaults)

No manual configuration needed.

---

## Permissions

All commands are **open to everyone by default**. No permissions plugin needed.

If you want to restrict access, add these permission nodes:

```yaml
bundleessential.tpa: true
bundleessential.home: true
bundleessential.back: true
bundleessential.waypoint: true
bundleessential.trade: true
bundleessential.economy: true
bundleessential.update: true
```

---

## License

MIT
