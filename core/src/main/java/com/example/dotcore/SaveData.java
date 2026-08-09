package com.example.dotcore;

public class SaveData {
    public int slot = 1;
    public double credits = 120;
    public int wave = 1;
    public float integrity = 100f;

    // 0=Recon, 1=Invasion (default), 2=Apocalypse. Chosen when a new slot is created.
    public int difficulty = 1;

    // Legacy fields kept for save compatibility. New builds no longer expose a General tab.
    public int generalDamageLevel = 0;
    public int generalRateLevel = 0;
    public int creditYieldLevel = 0;

    // Player-selected risk / economy modifiers.
    public int densityLevel = 0;
    public int spawnRateLevel = 0;
    public int enemyValueLevel = 0;
    public int enemySpeedLevel = 0;
    public int enemyDamageLevel = 0;
    public int enemyHealthLevel = 0;

    // Finger branch.
    public int tapDamageLevel = 0;
    public int tapSpeedLevel = 0; // legacy only; tap cadence is now the player's actual tapping speed.
    public boolean plasmaUnlocked = false;
    public boolean trailUnlocked = false;      // UI name: Roscherk / Stroke
    public boolean ultimateUnlocked = false;   // UI name: Annihilation
    public boolean gravityUnlocked = false;

    // Turret branch.
    public int turretCount = 0;
    public boolean turretPlusTwo = false;
    public int turretDamageLevel = 0;
    public int turretRateLevel = 0;
    public int turretShieldLevel = 0;
    public boolean autoRepairUnlocked = false;
    public boolean turretLaserUnlocked = false;
    public boolean turretRocketsUnlocked = false;
    public int turretWeapon = 0; // 0 pulse, 1 laser, 2 rockets

    // Drone branch.
    public int gunDrones = 0;
    public int missileDrones = 0;
    public boolean missileDroneUnlocked = false;
    public int kamikazeDrones = 0;
    public boolean kamikazeUnlocked = false;
    public int supportDrones = 0;
    public boolean supportDroneUnlocked = false;
    public boolean dronePlusTwo = false;
    public int droneDamageLevel = 0;
    public int droneRateLevel = 0;
    public int droneAuraLevel = 0;
    public boolean droneAuraUnlocked = false;
    public int droneShieldLevel = 0;

    // Player elements. They are intentionally separate from hostile alien status effects.
    public boolean fireUnlocked = false;
    public boolean iceUnlocked = false;
    public boolean lightningUnlocked = false;
    public Element fingerElement = Element.NEUTRAL;
    public Element turretElement = Element.NEUTRAL;
    public Element droneElement = Element.NEUTRAL;
    public Element droneAuraElement = Element.NEUTRAL;

    // Skill-by-use progression.
    public float tapXp = 0;
    public int tapSkillLevel = 1;
    public float plasmaXp = 0;
    public int plasmaSkillLevel = 1;
    public float trailXp = 0;
    public int trailSkillLevel = 1;
    public float ultimateXp = 0;
    public int ultimateSkillLevel = 1;
    public float turretXp = 0;
    public int turretSkillLevel = 1;
    public float droneXp = 0;
    public int droneSkillLevel = 1;
    public float repairXp = 0;
    public int repairSkillLevel = 1;


    // Enemy catalogue / bestiary progress. Counts are stored per save and aggregated in the main menu.
    public long bestiaryBasic = 0;
    public long bestiaryFast = 0;
    public long bestiaryTank = 0;
    public long bestiaryElite = 0;
    public long bestiaryStar = 0;
    public long bestiaryGuardian = 0;
    public long bestiaryPhase = 0;
    public long bestiaryFireResist = 0;
    public long bestiaryIceResist = 0;
    public long bestiaryLightningResist = 0;
    public long bestiaryWard = 0;
    public long bestiaryInfector = 0;
    public long bestiaryBoss = 0;

    public long totalKills = 0;
    public long totalBossKills = 0;
    public long totalRepairs = 0;
    public float playSeconds = 0;

    public int turretCap() { return turretPlusTwo ? 7 : 5; }
    public int droneCap() { return dronePlusTwo ? 13 : 11; }
    public int droneCount() { return gunDrones + missileDrones + kamikazeDrones + supportDrones; }

    public float generalDamageMultiplier() { return 1f; }
    public float generalRateMultiplier() { return 1f; }
    public float difficultyHealthMultiplier() { return difficulty<=0 ? .82f : (difficulty>=2 ? 1.36f : 1f); }
    public float difficultyDamageMultiplier() { return difficulty<=0 ? .78f : (difficulty>=2 ? 1.34f : 1f); }
    public float difficultySpeedMultiplier() { return difficulty<=0 ? .92f : (difficulty>=2 ? 1.10f : 1f); }
    public float difficultyDensityMultiplier() { return difficulty<=0 ? .84f : (difficulty>=2 ? 1.28f : 1f); }
    public float difficultyRewardMultiplier() { return difficulty<=0 ? .90f : (difficulty>=2 ? 1.35f : 1f); }
    public float waveHealthMultiplier() { return 1f + Math.max(0,wave-1)*.052f + totalBossKills*.045f; }
    public float waveDamageMultiplier() { return 1f + Math.max(0,wave-1)*.034f + totalBossKills*.035f; }
    public float creditMultiplier() { return (1f + enemyValueLevel * 0.20f) * difficultyRewardMultiplier(); }

    // Risk upgrades intentionally pay noticeably more than before.
    public float passiveIncomePerSecond() {
        return difficultyRewardMultiplier() * (1f
            + 1.35f * densityLevel
            + 1.55f * spawnRateLevel
            + 1.80f * enemyValueLevel
            + 1.35f * enemySpeedLevel
            + 1.60f * enemyDamageLevel
            + 1.60f * enemyHealthLevel);
    }

    public float spawnMultiplier() { return 1f + spawnRateLevel * 0.11f; }
    public int densityBonus() { return densityLevel * 2; }
    public float enemyValueMultiplier() { return 1f + enemyValueLevel * 0.20f; }
    public float enemyHealthMultiplier() { return (1f + enemyHealthLevel * 0.14f) * waveHealthMultiplier() * difficultyHealthMultiplier(); }
    public float enemyDamageMultiplier() { return (1f + enemyDamageLevel * 0.12f) * waveDamageMultiplier() * difficultyDamageMultiplier(); }
    public float enemySpeedMultiplier() {
        return (1f + Math.max(0, wave - 1) * 0.013f + totalBossKills * 0.060f + enemySpeedLevel * 0.08f) * difficultySpeedMultiplier();
    }
}
