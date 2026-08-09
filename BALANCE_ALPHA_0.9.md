# DOT//CORE Alpha 0.9 — economy / difficulty notes

## Early economy
- Start: 120 C.
- Base passive income: 1 C/s, only during active combat.
- First turret: 300 C.
- Wave 1 target reward: 14 C before Enemy Value upgrades.
- Wave 1 lasts 30 s and spawns roughly one target every ~1.6 s before player-selected debuffs.
- Intended result: an active player can afford the first turret during the introductory wave without receiving it for free.

## Debuff costs and passive income per level

| Debuff | Base cost | Growth | Passive gain / level |
|---|---:|---:|---:|
| Enemy density | 55 C | x1.48 | +0.8 C/s |
| Spawn rate | 60 C | x1.50 | +0.9 C/s |
| Enemy value | 75 C | x1.52 | +1.2 C/s |
| Enemy speed | 65 C | x1.50 | +0.9 C/s |
| Enemy damage | 70 C | x1.52 | +1.1 C/s |
| Enemy health | 70 C | x1.52 | +1.1 C/s |

Enemy Value also increases kill rewards by 18% per level.

## Difficulty curve
- Wave 1: normal enemies only, 18 base HP, base speed 22, no hostile shooting.
- Wave 2+: fast/tank/elite enemy types and hostile shooting become available.
- Base spawn interval decreases progressively after wave 2 down to a floor.
- End-of-wave rush remains active after wave 1.
- Enemy speed receives +1.5% per wave after wave 1, +7% per killed boss, plus purchased Enemy Speed debuffs.

## Turret baseline
- Pulse turret base cooldown changed from 0.42 s to 0.50 s.
- Laser base cooldown changed from 0.72 s to 0.78 s.
- Rocket base cooldown changed from 1.35 s to 1.45 s.
- Fire-rate upgrades remain deliberately strong so turret builds can still reach late-game high-rate behavior.
