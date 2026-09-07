# Configuration

Toggle in `plugins/BundledEssential/features.yml` (tpa, home, back, waypoints, trade, economy, bounty, pay, shop, sell, leveling, rewards, autosell, spawner, playtime, dynamic-light, updater) — `false` + restart to disable.

Fine-tune in `config.yml`: `economy.balance-cap`, `economy.playtime-min/max-reward`, `leveling.*`, `rewards.*`, `autosell.*`, `spawner.*`, `dynamic-light.*`.

Data: `homes.yml`, `balances.json`, `serverbank.json`, `loans.json`, `levels.json`, `playtime.json` + `playtime_opt.json`, `rewards.json`, `autosell.json`, `spawner.json`, `shop.json` (price overrides). No manual config needed; `/bundledreload` applies without restart.
