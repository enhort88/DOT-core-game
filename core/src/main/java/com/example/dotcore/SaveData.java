package com.example.dotcore;

public class SaveData {
    public int slot = 1;
    public double credits = 120;
    public int wave = 1;
    public float integrity = 100f;

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

    public long totalKills = 0;
    public long totalBossKills = 0;
    public long totalRepairs = 0;
    public float playSeconds = 0;

    public int turretCap() { return turretPlusTwo ? 7 : 5; }
    public int droneCap() { return dronePlusTwo ? 13 : 11; }
    public int droneCount() { return gunDrones + missileDrones + kamikazeDrones + supportDrones; }

    public float generalDamageMultiplier() { return 1f; }
    public float generalRateMultiplier() { return 1f; }
    public float creditMultiplier() { return 1f + enemyValueLevel * 0.20f; }

    // Risk upgrades intentionally pay noticeably more than before.
    public float passiveIncomePerSecond() {
        return 1f
            + 1.35f * densityLevel
            + 1.55f * spawnRateLevel
            + 1.80f * enemyValueLevel
            + 1.35f * enemySpeedLevel
            + 1.60f * enemyDamageLevel
            + 1.60f * enemyHealthLevel;
    }

    public float spawnMultiplier() { return 1f + spawnRateLevel * 0.11f; }
    public int densityBonus() { return densityLevel * 2; }
    public float enemyValueMultiplier() { return 1f + enemyValueLevel * 0.20f; }
    public float enemyHealthMultiplier() { return 1f + enemyHealthLevel * 0.14f; }
    public float enemyDamageMultiplier() { return 1f + enemyDamageLevel * 0.12f; }
    public float enemySpeedMultiplier() {
        return 1f + Math.max(0, wave - 1) * 0.016f + totalBossKills * 0.075f + enemySpeedLevel * 0.08f;
    }
}
