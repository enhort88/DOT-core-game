# DOT//CORE — Alpha 0.6

Cross-platform Java/libGDX prototype for Android + Linux/Windows desktop.

## Run on desktop

```bash
./gradlew -PskipAndroid :lwjgl3:run
```

`-PskipAndroid` deliberately excludes the Android module, so desktop launch does not require an Android SDK.
The included `gradlew` bootstraps Gradle 9.5.0 into `~/.gradle/dotcore-bootstrap` on the first run.

## Build/install Android

Open the root folder in Android Studio, or from terminal:

```bash
./gradlew :android:assembleDebug
./gradlew :android:installDebug
```

APK:

```text
android/build/outputs/apk/debug/android-debug.apk
```

Requires Android SDK with compileSdk 36 and JDK 17+ (JDK 21 is fine).

## Controls

Desktop:
- Mouse click: basic finger attack.
- Hold and release: Plasma, after unlocking it.
- Drag: Trail, after unlocking it.
- Two simultaneous touches on Android: Ultimate. Desktop multi-touch is naturally intended for Android testing.
- `S`: shop.
- `Space`: pause.
- `F`: cycle finger element.
- `T`: cycle turret element.
- `D`: cycle drone element.
- `Esc`: pause/back.

Android:
- Tap / hold / drag work directly with touch.
- Two-finger drag activates the ultimate after it is purchased.
- Short vibration feedback is used for taps, purchases, explosions, bosses and gravity skills when enabled.

## Implemented in Alpha 0.6

### Game loop
- Enemy spheres descend toward the planet.
- Normal, fast, tank, elite and boss spheres.
- Increasing waves and rush periods.
- Boss every fifth wave with a kill timer.
- Planet integrity and defeat.
- Overrun damage when too many spheres accumulate.
- Passive credits per second.

### Risk/economy
- Enemy density upgrade.
- Spawn-rate upgrade.
- Enemy-value upgrade.
- These risk upgrades increase passive income.
- Global damage, attack-speed and enemy-credit-yield upgrades.

### Finger build
- Basic tap from the start.
- Purchasable Plasma.
- Purchasable Trail.
- Purchasable two-finger Ultimate.
- Late Gravity unlock.
- Gravity tap = micro black hole.
- Gravity Plasma = large black hole.
- Gravity Trail/Ultimate = spatial rift/pull.
- Tap speed and damage upgrades.

### Elements/status effects
Fire, Ice and Lightning can be selected independently for:
- finger attacks;
- turrets;
- drones.

Effects:
- Fire applies damage-over-time burning.
- Ice applies slow and accumulated freeze.
- Lightning chains into nearby enemies.
- The same status system is used regardless of whether damage came from finger, turret or drone.

### Turret build
- Starts with one turret.
- Buy up to 5 turret slots.
- Late-game +2 slots => 7 total.
- Shields can be damaged and turrets can be disabled.
- Manual paid repair.
- Mid-game auto-repair unlock that consumes credits.
- Damage, shield and very high fire-rate scaling.
- Pulse gun, unlockable pulse laser and rockets.
- Element effects apply to turret attacks.

### Drone build
- Base cap: 9 drones.
- Late-game +2 slots => 11 total.
- Gun drone.
- Missile drone.
- Kamikaze drone.
- Support drone.
- Drones have shields but do not self-repair.
- Destroyed drones respawn after a delay.
- Support drones heal living damaged drones.
- Drone aura level increases aura radius/power.
- Fire aura damages/burns enemies and boosts nearby drone damage.
- Ice aura slows enemies and helps nearby drone tempo.
- Lightning aura periodically chains damage and boosts nearby drone fire rate.

### Progression
- 5 independent save slots.
- Builds can be developed independently.
- Shop upgrades and usage-based skill XP coexist.
- Separate use-levels for tap, Plasma, Trail, turrets and drones.

### UI / audiovisual
- New sci-fi main menu.
- Play, Continue, Settings, About, Exit.
- RU/EN switch in settings.
- Sound toggle, vibration toggle, effects-quality toggle.
- Procedural/neon sphere and turret rendering retained as the visual direction.
- 11 original synthesized SFX included: shots, pops, explosions, laser, rockets, buying, waves, boss warning, Plasma, electricity and ice.
- No third-party audio assets.

## Project structure

```text
DotCoreLibGDX/
├── assets/       shared game assets / localization / SFX
├── core/         all gameplay and rendering
├── lwjgl3/       Linux/Windows/macOS desktop launcher
└── android/      Android launcher
```

## Notes

This is still an Alpha vertical slice, not final balance. Numbers/prices are intentionally centralized in gameplay/shop code so we can tune the economy after playing it on a real device.


## Test build notes

Alpha 0.6 has `BuildFlags.TEST_MODE = true`: shop purchases and manual turret repairs do not spend credits, and the HUD shows unlimited test credits. Set it to `false` before economy balancing.


See `CHANGELOG_ALPHA_0.4.md` for the latest UI/combat-feel changes.
