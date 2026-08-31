# BundledEssential

A lightweight, low-resource Minecraft plugin that bundles essential teleportation and location commands into one jar. No external dependencies required.

**Works with Spigot, Paper, and forks (1.13+)**

---

## Features

| Module | Commands |
|--------|----------|
| **TPA** | `/tpa`, `/tpaccept`, `/tpahere` |
| **Home** | `/sethome`, `/removehome`, `/home` |
| **Back** | `/back` |
| **Waypoints** | `/waypoint`, `/waypoint:new`, `/waypoint:delete` |
| **Help** | `/bundlehelp` |

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
- Setting a home when you already have one will show an error

### Back

| Command | Description |
|---------|-------------|
| `/back` | Return to your last death location |

- Death location is stored per-player and cleared after use

### Waypoints

| Command | Description |
|---------|-------------|
| `/waypoint` | Open the waypoint GUI |
| `/waypoint:new <name>` | Create a waypoint at your location |
| `/waypoint:delete <name>` | Delete a waypoint |
| `/waypoint <name>` | Teleport to a waypoint by name |

- Maximum of **27 waypoints** per player
- GUI uses colored wool for occupied slots, gray glass for empty slots
- Click a waypoint in the GUI to teleport
- GUI is click-proof and drag-proof (no item theft)

### BundleHelp

| Command | Description |
|---------|-------------|
| `/bundlehelp` | Show all available commands |

---

## Installation

1. Download `BundledEssential-1.0.0.jar` from [Releases](https://github.com/HugoCirca/BundledEssential-/releases)
2. Place the jar in your server's `plugins/` folder
3. Restart or reload the server

---

## Building from Source

Requires **Java 17+** and **Gradle**.

```bash
git clone https://github.com/HugoCirca/BundledEssential-.git
cd BundledEssential-
./gradlew clean build
```

The compiled jar will be in `build/libs/BundledEssential-1.0.0.jar`.

---

## Configuration

All player data is stored automatically in:
- `plugins/BundledEssential/homes.yml` - Home locations
- `plugins/BundledEssential/config.yml` - Waypoint locations

No manual configuration needed.

---

## Permissions

All commands default to **op-only**. To grant access to all players, add to your permissions plugin:

```yaml
bundleessential.*:
  default: true
  children:
    bundleessential.tpa: true
    bundleessential.home: true
    bundleessential.back: true
    bundleessential.waypoint: true
```

Or grant individual permissions:

```yaml
bundleessential.tpa: true
bundleessential.home: true
bundleessential.back: true
bundleessential.waypoint: true
```

---

## License

MIT
