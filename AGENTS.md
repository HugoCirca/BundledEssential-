# Knapsack — Agent Guide

Spigot/Paper plugin (1.13+, compiled `1.20.4`), Gradle + ShadowJar. No tests.

## Build & Artifact

- Java 17, Gradle 9.5.1 wrapper (`gradlew`/`gradlew.bat`, `gradle.properties:1` `configuration-cache=true`)
- Real artifact is `shadowJar` — `jar { enabled = false }` (`build.gradle:26`). Don't expect `build/libs/*.jar` from `jar` task.
- Fast dev build: `./gradlew shadowJar` (~10s) / full: `./gradlew clean build` (>60s, config-cache stored)
- Output: `build/libs/Knapsack-2.0.x.jar` (`shadowJar:31` `archiveBaseName Knapsack`, relocates `net.wesjd.anvilgui` → `dev.hugocirca.knapsack.anvilgui`)
- Version is expanded into `plugin.yml:2` `${version}` via `processResources:42` → keep `build.gradle:7` `version = '...'` and `shadowJar:33` `archiveVersion` in sync. CI bumps both with `sed`.

## Config vs Features — Don't Mix

- `src/main/resources/features.yml` — on/off toggles for every module (fresh install = all `false`, `KnapsackPlugin.java:67` nags ops if none enabled). Written to `plugins/Knapsack/features.yml` on first run.
- `src/main/resources/config.yml` — tuning only (`economy.balance-cap`, `economy.scoreboard.enabled/merge`, `autosell`, `spawner`, etc.). `KnapsackPlugin.java:60` gates managers via `Features.isEnabled()`.
- `plugin.yml` — never edit `version` manually, it's replaced at build.
- Data lives in `plugins/Knapsack/*.json` + `homes.yml`; auto-migrates empty `plugins/Knapsack/` from `plugins/BundledEssential/` (`KnapsackPlugin.java:199`).

## Entrypoint & Ownership

- `src/main/java/dev/hugocirca/knapsack/KnapsackPlugin.java:60` `onEnable` wires everything; `onDisable:173` saves via `Saveable`.
- `economy/`: `BalanceManager.java:52` (bank `2B` cap, scoreboard `ebalance`), `PriceManager.java:104` `basePrices` (sell `0.6x` + `+5` per enchant, drift `0.85-1.20`), `ShopCatalog.java:15` is category truth + `SHOP_EXCLUDED:17`, `ShopManager.java:35`, `SellManager.java:23`. Sell = `getSellPriceWithEnchants`.
- `common/Saveable.java` single method `saveAll()` — all persistence via `util/JsonStorage.java` + `Gson` pretty.
- `util/Features.java:10` loads `features.yml`, `DataStorage.java:5` trivial wrapper.
- Single package `dev.hugocirca.knapsack`, 41 files, 12 subpackages.

## Scoreboard Gotcha

- `BalanceManager.java:369` `updateScoreboard` previously did `getNewScoreboard()` → overwrote other plugins. Now respects `economy.scoreboard.merge: true` (`config.yml:13`): merges `§fMoney: $X` onto existing `SIDEBAR` objective, tracks `lastBalanceLine` to clean updates. `enabled: false` never touches board. Gallery description `Balance` still says "may overwrite" — code no longer does when `merge:true`.

## Push / Release — Will Double-Bump If You Push Naively

- Every `push` to `main` triggers `.github/workflows/build.yml:4`:
  1. `grep` `build.gradle:7` `version` → bump `PATCH` → `sed` both `version` + `archiveVersion`
  2. `./gradlew clean build` → `softprops/action-gh-release@v2` → `vX.Y.Z`
  3. `Kir-Antipov/mc-publish@v3:80` → `modrinth-id: knapsack` (`LgR4avT4`) `loaders: paper/purpur` `game-versions: 1.21.11, 26.1-26.2` (`|` multiline — quoting matters, `26.1` as bare `26.1` is a YAML float and `mc-publish` drops it → `At least one game version should be specified`). Use `|` block.
  4. `git push` of the bump commit triggers **another** workflow run → another bump. Use `[skip ci]` in message or `push.bat` flow to avoid flooding.
- `push.bat:2` is the safe push: `git stash push -m "local bin outputs" -- bin/` → `git pull --rebase origin main` → `git stash pop` → `git push origin main`. `bin/` is Eclipse output (`bin/main`), gitignored but stashed to avoid rebase conflicts.
- Modrinth project `knapsack` (`LgR4avT4`, `mod` type — plugins are `mod` on Modrinth) has `changelog: ${{ github.event.head_commit.message }}` now. Needs `MODRINTH_TOKEN` secret (`Repository secrets: MODRINTH_TOKEN`).
- Icon at repo root `icon.png`/`icon.svg` (`512`, `3.1KiB`) is also `PATCH /v2/project/LgR4avT4/icon?ext=png` (`https://cdn.modrinth.com/data/LgR4avT4/...png`).

## Commands That Matter

```bash
./gradlew shadowJar          # dev jar
./gradlew clean build        # full + test (no tests, just build)
./push.bat                   # safe push with bin stash + rebase
py -c "from PIL import Image" # icon gen via `py` (not `python3`)
```

No test suite (`package.json:8` `test` fails, no `src/test`). No `opencode.json`/`.cursor` rules.

## Desktop Session Quirk

- Workspace may be `C:\Users\rosMa\Desktop` (contains `BundledEssential`/`Knapsack` as separate git repos). `BundledEssential` → `Knapsack` rename was blocked by `opencode2.exe:7972` + `node:9752` file handles on `Desktop` — close handles via `handle64.exe -c` before rename, don't force `Rename-Item`.
