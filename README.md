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
| **Waypoints** | `/waypoint` |
| **Trade** | `/trade`, `/tradeaccept`, `/tradecancel` |

- **Auto-updater** — Checks for updates on startup, downloads and restarts automatically

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
| `/waypoint new <name>` | Create a waypoint at your location |
| `/waypoint delete <name>` | Delete a waypoint |
| `/waypoint <name>` | Teleport to a waypoint by name |

- Maximum of **27 waypoints** per player
- GUI uses colored wool for occupied slots, gray glass for empty slots
- Click a waypoint in the GUI to teleport
- GUI is click-proof and drag-proof (no item theft)

### Trade

| Command | Description |
|---------|-------------|
| `/trade <player>` | Send a trade request to a player |
| `/tradeaccept` | Accept a pending trade request |
| `/tradecancel` | Cancel your pending trade requests |

- Requests expire after **30 seconds**
- You cannot trade with yourself

---

## Auto-Updater

The plugin automatically checks for new versions on startup via GitHub Releases. If an update is found:

1. The new JAR is downloaded to `plugins/update/`
2. A broadcast message warns players of an upcoming restart
3. The server restarts after 10 seconds
4. On restart, the old JAR is replaced with the new one

---

## Installation

1. Download the latest `BundledEssential-X.X.X.jar` from [Releases](https://github.com/HugoCirca/BundledEssential-/releases)
2. Place the jar in your server's `plugins/` folder
3. Restart or reload the server

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
```

---

## License

MIT
