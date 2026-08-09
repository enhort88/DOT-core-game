# DOT//CORE — Alpha 0.11.1 // PORTRAIT PASS

Cross-platform Java/libGDX game prototype for Android + Linux/Windows desktop.

## Run on desktop

```bash
./gradlew -PskipAndroid :lwjgl3:run
```

## Build/install Android

```bash
./gradlew :android:assembleDebug
./gradlew :android:installDebug
```

APK:

```text
android/build/outputs/apk/debug/android-debug.apk
```

Requires JDK 17+ and Android compileSdk 36.

## First launch

On the first launch the game asks for the commander's name, stores it globally, then shows a short four-page military briefing. The intro can be replayed from Settings and the name can be changed there.

## Alpha 0.11 / 0.11.1 highlights

### Progressive technology discovery
- Normal mode no longer exposes the whole tech tree at once.
- Finger begins with Tap Damage only. The next technology is shown as `???` until its requirements are met and it is purchased.
- Finger chain: Tap -> Plasma -> Roscherk -> Annihilation -> Gravity.
- Turrets begin with Buy Turret / Damage / Fire Rate / Shield / Repair skill; Laser, Rockets, Auto Repair and +2 slots are discovered later.
- Drones begin with Gun Drone / Damage / Fire Rate; Missile, Support, Kamikaze, Aura and +2 slots are discovered later.
- The Effect Shop itself appears only after defeating the fifth boss; Fire, Ice and Lightning are then discovered in sequence.
- Cheat mode is a real test mode: all technologies, elements, weapons and late-game systems are treated as unlocked immediately and purchases are free.

### Annihilation
- Two-finger Ultimate is renamed **Annihilation**.
- Maximum active use is 3 seconds.
- It then enters a long cooldown, displayed as a thin status bar below planet HP.
- Skill-by-use levels slightly reduce cooldown.

### Repair gameplay
- A broken/damaged turret is not repaired by one tap.
- A basic turret takes about five repair taps; shield upgrades increase required work.
- Every repair tap costs credits in normal mode, restores a portion of shield, plays welding sparks and awards Repair XP.
- Repair skill improves manual repair efficiency and later gates Auto Repair.
- Parasite infection is also cleansed by manual taps.

### Geometric invasion
- Wave 1 uses simple circles and is deliberately introductory.
- From wave 5, polygons enter the invasion.
- Around wave 10, aggressive triangular craft become common.
- Shapes are paired with existing fast/tank/elite/boss roles so the battlefield becomes visually richer as the war develops.

### Alien technology
Hostile special attacks are deliberately separate from the player's Fire / Ice / Lightning elements:
- **Corrosion** — damage-over-time against turret/drone shields.
- **Parasites** — infect a turret or drone; the infected unit attacks allies until cleansed manually.
- **Disruption** — temporarily jams firing.
- **Touch shields** — elite/boss craft can briefly deploy a transparent field that rejects direct finger taps.

Enemies prefer nearby drones as targets, otherwise they fire at turrets. Hostile attacks are visible projectiles rather than unexplained beams.

### Element visual pass
- Fire attacks have hot cores, sparks, lingering flames and visibly burning enemies.
- Ice uses crystalline projectiles/shards, frost shells and visible freeze states.
- Lightning uses jagged white-blue arcs with branches and visible chain jumps.
- Gravity remains a Finger-only late technology; heavy enemies resist pull and bosses move only slightly unless gravity is strongly charged.

### First-run story / profile
- The player enters a name on first launch.
- Main menu greets the player by name and save slots also show it.
- Short offline briefing introduces the general, a military officer and the geometry-obsessed alien invasion.
- Settings includes **Replay introduction** and allows changing the player name during the replay.

### Existing systems retained
- 5 save slots with deletion confirmation.
- Normal economy + risk/reward Debuffs.
- Passive income advances only while combat itself is running.
- Turret predictive aiming and homing rockets.
- Drone movement, support healing, kamikaze contact attacks and elemental auras.
- Bonus pickups that must be tapped.
- RU/EN localization.
- Separate SFX and music volume sliders, looping space music and Android vibration.
- Hidden cheat toggle: tap the `DOT` part of `DOT//CORE` ten times in the main menu; ten more taps turn it off.

## Project structure

```text
DotCoreLibGDX/
├── assets/       shared game assets, localization, music and SFX
├── core/         gameplay, progression, UI and rendering
├── lwjgl3/       desktop launcher
└── android/      Android launcher
```

See `CHANGELOG_ALPHA_0.11.md` and `PROGRESSION_ALPHA_0.11.md`.
