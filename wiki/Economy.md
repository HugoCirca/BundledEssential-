# Economy

## Balance & Cap & Bank
- `/balance [player]` — check (also offline). Scoreboard shows `E-balance` without `====`.
- Per-player cap `economy.balance-cap` (default 1T, add zeros in `config.yml`, `/bundledreload`).
- Overflow from **any** earning (`/sell`, playtime $8-12/5m, mob $0.01-10, quests, autosell) → **Server Bank** (`serverbank.json`, capped 2B, `/serverbank`).
- `/resetbal <player> [amount]` — admin set/reset (supports negatives for loans, capped).
- `/serverbank [history]` — balance + history book (time/player/amount/reason). Console prints last 10.
- `/resetserverbal` (`resetbank`) — admin wipe Bank to 0, clears history. Auto-reset if old U64 exceeds 2B on load.
- Bank interest: 30% every 20 min, 0.2-0.5% of bank (max $5000) split to online non-capped players.

## Pay / Bounty / Taxes
- `/pay <player> <amount>` — 5% tax → Bank (debt → `garnish` 25% of future earnings → Bank; `/paytax` to clear)
- `/bounty <player> [amount]` — set/check; claim on PvP kill 20% tax → Bank immediately, placers can't claim own
- `/paytax` — pay accumulated taxes → Bank
- `/repair [full]` — cost scales with durability

## Sell
- `/sell` — sell main-hand
- `/sellgui` — close-to-sell GUI (enchant bonus, shift-click sell all same type)
