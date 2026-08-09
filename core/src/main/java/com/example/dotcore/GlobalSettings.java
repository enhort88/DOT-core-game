package com.example.dotcore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GlobalSettings {
    public String language = "ru";
    public boolean sound = true;
    public float soundVolume = 0.75f;
    public boolean vibration = true;
    public boolean highEffects = true;
    public boolean cheatsEnabled = false;
    public float musicVolume = 0.50f;

    public void load() {
        Preferences p = Gdx.app.getPreferences("dotcore.settings");
        language = p.getString("language", "ru");
        sound = p.getBoolean("sound", true);
        soundVolume = p.getFloat("soundVolume", sound ? 0.75f : 0f);
        vibration = p.getBoolean("vibration", true);
        highEffects = p.getBoolean("highEffects", true);
        cheatsEnabled = p.getBoolean("cheatsEnabled", false);
        musicVolume = p.getFloat("musicVolume", 0.50f);
    }

    public void save() {
        Gdx.app.getPreferences("dotcore.settings")
            .putString("language", language)
            .putBoolean("sound", soundVolume > 0.001f)
            .putFloat("soundVolume", soundVolume)
            .putBoolean("vibration", vibration)
            .putBoolean("highEffects", highEffects)
            .putBoolean("cheatsEnabled", cheatsEnabled)
            .putFloat("musicVolume", musicVolume)
            .flush();
    }
}
