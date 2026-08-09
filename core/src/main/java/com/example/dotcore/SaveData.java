package com.example.dotcore;

public class SaveData {
    public int slot = 1;
    public double credits = 150;
    public int wave = 1;
    public float integrity = 100f;

    public int generalDamageLevel = 0;
    public int generalRateLevel = 0;
    public int creditYieldLevel = 0;
    public int densityLevel = 0;
    public int spawnRateLevel = 0;
    public int enemyValueLevel = 0;
    public int enemySpeedLevel = 0;
    public int enemyDamageLevel = 0;
    public int enemyHealthLevel = 0;

    public int tapDamageLevel = 0;
    public int tapSpeedLevel = 0;
    public boolean plasmaUnlocked = false;
    public boolean trailUnlocked = false;
    public boolean ultimateUnlocked = false;
    public boolean gravityUnlocked = false;

    public int turretCount = 0;
    public boolean turretPlusTwo = false;
    public int turretDamageLevel = 0;
    public int turretRateLevel = 0;
    public int turretShieldLevel = 0;
    public boolean autoRepairUnlocked = false;
    public boolean turretLaserUnlocked = false;
    public boolean turretRocketsUnlocked = false;
    public int turretWeapon = 0; // 0 pulse, 1 laser, 2 rockets

    public int gunDrones = 0;
    public int missileDrones = 0;
    public int kamikazeDrones = 0;
    public boolean kamikazeUnlocked = false;
    public int supportDrones = 0;
    public boolean dronePlusTwo = false;
    public int droneDamageLevel = 0;
    public int droneRateLevel = 0;
    public int droneAuraLevel = 0;
    public int droneShieldLevel = 0;

    public boolean fireUnlocked = false;
    public boolean iceUnlocked = false;
    public boolean lightningUnlocked = false;

    public Element fingerElement = Element.NEUTRAL;
    public Element turretElement = Element.NEUTRAL;
    public Element droneElement = Element.NEUTRAL;
    public Element droneAuraElement = Element.NEUTRAL;


    public float tapXp = 0;
    public int tapSkillLevel = 1;
    public float plasmaXp = 0;
    public int plasmaSkillLevel = 1;
    public float trailXp = 0;
    public int trailSkillLevel = 1;
    public float turretXp = 0;
    public int turretSkillLevel = 1;
    public float droneXp = 0;
    public int droneSkillLevel = 1;

    public long totalKills = 0;
    public long totalBossKills = 0;
    public float playSeconds = 0;

    public int turretCap() { return turretPlusTwo ? 7 : 5; }
    public int droneCap() { return dronePlusTwo ? 13 : 11; }
    public int droneCount() { return gunDrones + missileDrones + kamikazeDrones + supportDrones; }
    public float generalDamageMultiplier() { return 1f + generalDamageLevel * 0.12f; }
    public float generalRateMultiplier() { return 1f + generalRateLevel * 0.10f; }
    public float creditMultiplier() { return 1f + creditYieldLevel * 0.15f + enemyValueLevel * 0.20f; }
    public float passiveIncomePerSecond() {
        return 1f + 0.45f * (densityLevel + spawnRateLevel + enemyValueLevel)
            + 0.60f * enemySpeedLevel + 0.55f * enemyDamageLevel + 0.55f * enemyHealthLevel;
    }
    public float spawnMultiplier() { return 1f + spawnRateLevel * 0.14f; }
    public int densityBonus() { return densityLevel * 3; }
    public float enemyValueMultiplier() { return 1f + enemyValueLevel * 0.22f; }
    public float enemyHealthMultiplier() { return 1f + enemyHealthLevel * 0.16f; }
    public float enemyDamageMultiplier() { return 1f + enemyDamageLevel * 0.15f; }
    public float enemySpeedMultiplier() { return 1f + totalBossKills * 0.08f + enemySpeedLevel * 0.10f; }
}
