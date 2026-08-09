package com.example.dotcore;

public enum Element {
    NEUTRAL, FIRE, ICE, LIGHTNING, GRAVITY;

    public Element nextCombat(boolean gravityAllowed, SaveData save) {
        Element[] values = values();
        int start = ordinal();
        for (int i = 1; i <= values.length; i++) {
            Element e = values[(start + i) % values.length];
            if (e == NEUTRAL) return e;
            if (e == FIRE && save.fireUnlocked) return e;
            if (e == ICE && save.iceUnlocked) return e;
            if (e == LIGHTNING && save.lightningUnlocked) return e;
            if (e == GRAVITY && gravityAllowed && save.gravityUnlocked) return e;
        }
        return NEUTRAL;
    }
}
