package com.example.dotcore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GlobalSettings {
    public String language = "ru";
    public boolean sound = true;
    public boolean vibration = true;
    public boolean highEffects = true;

    public void load() {
        Preferences p = Gdx.app.getPreferences("dotcore.settings");
        language = p.getString("language", "ru");
        sound = p.getBoolean("sound", true);
        vibration = p.getBoolean("vibration", true);
        highEffects = p.getBoolean("highEffects", true);
    }

    public void save() {
        Gdx.app.getPreferences("dotcore.settings")
            .putString("language", language)
            .putBoolean("sound", sound)
            .putBoolean("vibration", vibration)
            .putBoolean("highEffects", highEffects)
            .flush();
    }
}
