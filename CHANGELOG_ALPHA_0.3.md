# DOT//CORE Alpha 0.3 — UI / Visual Test Build

- Replaced the previous raster font with a clean Noto Sans-based bitmap atlas for RU/EN.
- Cleared the top of the combat field.
- Moved HUD information to slim side meters and compact bottom controls.
- Credits are shown as `C`; test build displays `C ∞`.
- Shop redesigned into two-column visual cards with procedural pictograms.
- Enemy spheres reworked to look more like glowing energy bubbles.
- Turret bullet aiming now predicts enemy movement; rockets home toward their targets.
- Finger trails received separate fire / ice / lightning / gravity visual treatments.
- Tapping a damaged/broken turret repairs it for credits; in TEST_MODE the repair is free.
- TEST_MODE gives effectively unlimited credits and does not deduct upgrade costs.
- Late-game unlock wave gates are bypassed in TEST_MODE for easier testing.
- About screen now includes: Ponikarov Artem, enhort@gmail.com.
- Android config: AGP 9.3.1, compileSdk 36, version 0.3.0-alpha.
- Fixed SaveRepository Json import (`com.badlogic.gdx.utils.Json`).
