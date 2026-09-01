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
| **Economy** | `/shop`, `/sell`, `/sellgui`, `/balance`, `/pay`, `/bounty` |
| **Help** | `/bundledhelp` |

- **Auto-updater** — Checks for updates on startup, downloads and applies on next restart
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
| `/tradeaccept` | Accept a pending trade request |
| `/tradecancel` | Cancel your pending trade requests |

- Requests expire after **30 seconds**

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
- **Playtime** — $15.00 every 5 minutes
- **Bounty claims** — Kill a player with a bounty to claim it

#### Shop Categories
- **Logs** — Oak, Birch, Spruce, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Crimson, Warped
- **Stone** — Cobblestone, Stone, Deepslate, Andesite, Diorite, Granite, Tuff, etc.
- **Ores** — Coal, Iron, Copper, Gold, Redstone, Lapis, Diamond, Emerald, Netherite
- **Crops** — Wheat, Carrot, Potato, Beetroot, Melon, Pumpkin, Sugar Cane, etc.
- **Mob Drops** — Bone, String, Gunpowder, Ender Pearl, Blaze Rod, etc.
- **Building** — Planks, Fences, Stairs, Slabs, Doors, Glass, Bricks, etc.
- **Decoration** — Crafting Table, Furnace, Anvil, Chest, Torch, Lantern, etc.

#### Dynamic Pricing
- Prices drift ±5-10% every 5 minutes
- Inflation grows slowly over server uptime
- Enchanted items sell for bonus money
- Sell price is 60% of current buy price

### Help

| Command | Description |
|---------|-------------|
| `/bundledhelp` | Show all available commands |

---

## Auto-Updater

The plugin automatically checks for new versions on startup via GitHub Releases. If an update is found:

1. The new JAR is downloaded to `plugins/update/`
2. On next server restart, the old JAR is replaced with the new one

No forced restarts — updates apply naturally.

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
```

---

## License

MIT
