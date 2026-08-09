package com.example.dotcore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.Preferences;

public class SaveRepository {
    public static final int SLOT_COUNT = 5;
    private final Json json = new Json();
    private final Preferences prefs = Gdx.app.getPreferences("dotcore.saves");

    public boolean exists(int slot) { return prefs.contains("slot." + slot); }

    public SaveData load(int slot) {
        if (!exists(slot)) return fresh(slot);
        try {
            SaveData s = json.fromJson(SaveData.class, prefs.getString("slot." + slot));
            s.slot = slot;
            if (s.kamikazeDrones > 0) s.kamikazeUnlocked = true;
            if (s.missileDrones > 0) s.missileDroneUnlocked = true;
            if (s.supportDrones > 0) s.supportDroneUnlocked = true;
            if (s.droneAuraLevel > 0) s.droneAuraUnlocked = true;
            return s;
        } catch (Exception e) {
            return fresh(slot);
        }
    }

    public SaveData fresh(int slot) {
        SaveData s = new SaveData();
        s.slot = slot;
        return s;
    }

    public void save(SaveData data) {
        prefs.putString("slot." + data.slot, json.toJson(data));
        prefs.putInteger("lastSlot", data.slot);
        prefs.flush();
    }

    public void delete(int slot) {
        prefs.remove("slot." + slot);
        if (prefs.getInteger("lastSlot", 1) == slot) {
            int replacement = 1;
            for (int i=1;i<=SLOT_COUNT;i++) if (i!=slot && exists(i)) { replacement=i; break; }
            prefs.putInteger("lastSlot", replacement);
        }
        prefs.flush();
    }

    public int lastSlot() { return prefs.getInteger("lastSlot", 1); }
}
