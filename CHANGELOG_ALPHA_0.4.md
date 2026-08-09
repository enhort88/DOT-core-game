# DOT//CORE Alpha 0.4 — Combat feel / UI pass

## Fixed
- Fixed SUPPORT-drone crash caused by nested iteration over libGDX `Array<Drone>`.
- Also hardened nearby-drone counting and AoE/lightning iteration against the same nested-iterator class of bug.
- Turret barrels now visually track their current target and pulse bullets keep predictive lead aiming.

## UI
- Shop cards now use colorful emoji-derived raster pictograms instead of developer-looking geometric placeholders.
- Element/effect assignment was removed from the shop.
- A separate bottom configuration button opens Finger / Turrets / Drones effect setup.
- Planet integrity moved to a cleaner horizontal bar at the bottom.
- Combat HUD remains intentionally sparse; wave/boss progress uses a thin side meter.
- Added a real Android launcher icon and desktop window icon.

## Combat feel
- Finger trails persist and fade rather than appearing as a short flat line.
- Fire, ice, lightning and gravity trails have different layered visuals and particles.
- Added visible expanding blast flashes/shockwaves and radial streaks to explosions.
- Two-finger ultimate logic was intentionally left unchanged.

## Bonuses
- Temporary tappable bonuses can now appear on the battlefield:
  - credits;
  - planet repair;
  - combat overdrive (temporary turret/drone attack-speed boost).

## Test build
- Infinite-money test mode remains enabled.
