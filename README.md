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
| **Leveling** | `/level` |
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
| `/sell` | Sell the item in your main hand |
| `/sellgui` | Open sell GUI — put items in, close to sell |
| `/balance` | Check your balance |
| `/balance <player>` | Check another player's balance |
| `/pay <player> <amount>` | Pay a player |
| `/bounty <player> [amount]` | Set or check a bounty |

#### Money Sources
- **Kill mobs** — $0.01 to $10.00 (random)
- **Playtime** — $1.00 to $3.00 every 5 minutes, scaled up by your level (+10% per level by default)
- **Bounty claims** — Kill a player with a bounty to claim it (20% tax)

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
- **Search** — Compass button in `/shop` opens an anvil: type a name, land on a results page (chat fallback included)

#### Dynamic Pricing
- Prices drift ±5-10% every 5 minutes
- Inflation grows slowly over server uptime
- Enchanted items sell for bonus money
- Sell price is 60% of current buy price
- High-end items (Elytra, Totem, Netherite, Dragon Egg...) keep premium prices

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

### Dynamic Light

No command — just hold anything with a light property in either hand and it glows around you: torches, lanterns, lava buckets, glowstone, shroomlight, sea lanterns, froglights, end rods, jack o'lanterns, campfires, beacons, conduits, crying obsidian, amethyst buds and more.

- Places a real invisible Light block at your feet (only ever replaces air)
- Light follows you, updates its level, and is removed on logout/shutdown
- No extra dynamic-lights plugin needed
- Configurable in `config.yml` under `dynamic-light:` (enabled, interval-ticks)

### Help

| Command | Description |
|---------|-------------|
| `/bundledhelp` | Show all available commands |
| `/bundleversion` | Show the installed plugin version |

---

## Auto-Updater

The plugin automatically checks for new versions on startup via GitHub Releases. If an update is found:

1. The new JAR is downloaded to `plugins/update/`
2. On next server restart, the old JAR is replaced with the new one

No forced restarts — updates apply naturally.

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

All player data is stored automatically in:
- `plugins/BundledEssential/homes.yml` - Home locations
- `plugins/BundledEssential/config.yml` - Waypoint locations
- `plugins/BundledEssential/balances.json` - Player balances
- `plugins/BundledEssential/levels.json` - Player levels and XP
- `plugins/BundledEssential/playtime.json` - Player online time
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
