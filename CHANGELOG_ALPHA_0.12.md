# DOT//CORE Alpha 0.12 — ENEMY ECOLOGY

- Added Invasion Catalogue / bestiary in main menu. 50 kills identifies an enemy; 100 reveals weakness. Boss requires one kill. Cheat mode exposes the entire catalogue immediately.
- Added three per-save starting difficulties: Recon, Invasion, Apocalypse, with different enemy and reward multipliers.
- Added tactical enemy archetypes: Star artillery, Guardian aura tank, Phaser, three elemental-resistance types, Element Ward and Infector.
- Guardian aura reduces direct damage to nearby allies; burn DoT and gravity can bypass it. Element Ward reduces elemental damage to nearby allies.
- Element-resistant enemies show rotating shield nodes matching Fire/Ice/Lightning.
- Infector prioritizes drones and can turn drones/turrets hostile; infected drones may attack the planet. Kamikaze drones are high-priority alien targets.
- Finger-heavy builds slightly increase Phaser/Guardian frequency without hard-countering the player.
- Stars are guaranteed to enter the spawn pool from wave 10 and act as ranged artillery.
- Natural wave scaling now increases HP, damage, speed and pressure separately; difficulty and voluntary debuffs stack on top.
- Fixed occasional stuck tap/multitouch state with an input watchdog/reset around overlays and Annihilation.
- Boss countdown is brighter and centered behind combat.
- Reasserted Android blending state after countdown rendering and replaced the ground alpha fill with an opaque dark color to avoid boss-frame rendering glitches.
