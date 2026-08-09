package com.example.dotcore;

public enum Element {
    NEUTRAL, FIRE, ICE, LIGHTNING, GRAVITY;

    public Element nextCombat(boolean gravityAllowed, SaveData save) {
        return nextCombat(gravityAllowed, save, false);
    }

    public Element nextCombat(boolean gravityAllowed, SaveData save, boolean cheats) {
        Element[] values = values();
        int start = ordinal();
        for (int i = 1; i <= values.length; i++) {
            Element e = values[(start + i) % values.length];
            if (e == NEUTRAL) return e;
            if (e == FIRE && (cheats || save.fireUnlocked)) return e;
            if (e == ICE && (cheats || save.iceUnlocked)) return e;
            if (e == LIGHTNING && (cheats || save.lightningUnlocked)) return e;
            if (e == GRAVITY && gravityAllowed && (cheats || save.gravityUnlocked)) return e;
        }
        return NEUTRAL;
    }
}
