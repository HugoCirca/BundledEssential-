# Loans

- `/loan` — Chest GUI: bedrock `$200/500/1000/1500/2000/2500` + compass **custom** (anvil 50-10000). Pending loans show as **paper** (18-25) — click to pay.
- Pick repayment: **3/7/14 days lump** (max 2 weeks) or **Slow Deduct** (20% of earnings auto-pays).
- `/loan pay [id|all]` — repay lump (paper click also works); `/loan info` — list debts.
- Late lump: +5%/day compound, can go to `-1M`; on-time clear → **+$25 bonus** (slow also); total debt cap $10000.
- Data `loans.json`.
