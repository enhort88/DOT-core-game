# DOT//CORE — Alpha 0.10

Cross-platform Java/libGDX prototype for Android + Linux/Windows desktop.

## Run on desktop

```bash
./gradlew -PskipAndroid :lwjgl3:run
```

`-PskipAndroid` excludes the Android module. The included bootstrap uses Gradle 9.5.0.

## Build/install Android

```bash
./gradlew :android:assembleDebug
./gradlew :android:installDebug
```

APK:

```text
android/build/outputs/apk/debug/android-debug.apk
```

Requires compileSdk 36 and JDK 17+.

## Controls

Desktop / Android:
- Tap/click: basic finger attack.
- Hold and release: Plasma after unlocking it.
- Drag: Trail after unlocking it.
- Two fingers on Android: Ultimate after unlocking it.
- Bottom buttons open Debuffs, Effect Setup and Shop.
- `S`: shop, `Space`: pause, `Esc`: pause/back.

## Alpha 0.10 highlights

### Economy and difficulty
- Normal economy is enabled by default; cheat mode remains a hidden easter egg.
- New save starts with `120 C` and `1 C/s` passive income.
- Passive income is earned **only while combat is running**. Shop, Debuffs, Effect Setup, Effect Shop and Pause freeze the game economy and timers.
- The old `General` shop tab was removed. Combat upgrades live only in Finger / Turrets / Drones.
- Enemy-value/risk upgrades live only in `Debuffs`.
- Debuffs are cheaper and now increase passive income much more strongly.
- Debuffs are grouped into Enemy Power, Pressure and Economy for easier reading.

### Wave curve
- Wave 1 is an introductory wave: only slow normal enemies, lower HP, slower spawn and no enemy shooting.
- From wave 2 the game introduces fast/tank/elite enemies and hostile fire.
- Spawn pressure increases progressively by wave.
- Enemy movement speed grows slightly every wave and more noticeably after bosses.
- Boss remains every fifth wave.

### Turrets
- New games start with zero turrets; the first turret is purchased.
- A single turret is placed exactly at the center of the defense line.
- Base turret fire rate is slightly slower so the fire-rate upgrade has more value.
- Turret damage/rate/shield remain independent upgrades.

### UI / UX
- Shop now has only three build tabs: Finger, Turrets, Drones.
- Effect Setup is reorganized into three clear source groups:
  - Finger: attack element.
  - Turrets: attack element + weapon.
  - Drones: attack element + aura element.
- Debuffs are organized into logical sections and each card shows how much passive `C/s` the next level adds.
- Shop / Debuffs / Effect buttons have a short energy-pulse press animation.

### Music and sound
- Added an original looping space ambient background track.
- Music volume is adjustable in Settings: 0 / 25 / 50 / 75 / 100%.
- SFX toggle remains independent from music volume.
- Existing synthesized combat SFX and vibration are retained.

## Existing core systems

- Descending enemy spheres, waves, rush periods, bosses and planet integrity.
- Finger Tap / Plasma / Trail / Ultimate / Gravity.
- Fire / Ice / Lightning status effects.
- Turrets with shields, manual repair and auto-repair.
- Gun, missile, kamikaze and support drones.
- Drone shields, respawn delay and support healing.
- Drone elemental auras.
- 5 independent save slots with deletion confirmation.
- RU/EN localization.
- Hidden cheat easter egg: tap `DOT` in `DOT//CORE` 10 times to toggle free purchases.

## Project structure

```text
DotCoreLibGDX/
├── assets/       shared game assets, localization, music and SFX
├── core/         gameplay and rendering
├── lwjgl3/       desktop launcher
└── android/      Android launcher
```

See `CHANGELOG_ALPHA_0.10.md` and `BALANCE_ALPHA_0.9.md` for this pass.
