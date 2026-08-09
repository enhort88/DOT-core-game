package com.example.dotcore;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.I18NBundle;
import com.badlogic.gdx.utils.ObjectMap;
import java.util.Locale;

public class Assets {
    public GlyphFont font;
    public I18NBundle i18n;
    public Sound shot, pop, explosion, laser, rocket, buy, wave, boss, plasma, electric, ice;
    private final ObjectMap<String, Texture> icons = new ObjectMap<>();

    private static final String[] ICON_NAMES = {
        "tab_general","tab_finger","tab_turrets","tab_drones","tab_elements",
        "gDamage","gRate","yield","density","spawn","value","enemySpeed","enemyDamage","enemyHealth","tab_debuffs","debuff_button","delete_save",
        "tapDmg","tapRate","plasma","trail","ultimate","gravity",
        "buyTurret","turretDmg","turretRate","turretShield","repairTurrets","autoRepair","laser","rockets","cycleWeapon","turretPlusTwo",
        "gunDrone","missileDrone","kamikaze","support","droneDmg","droneRate","droneAura","droneShield","dronePlusTwo",
        "fire","ice","lightning","config","shop_button","neutral","bonus_credit","bonus_heal","bonus_overdrive"
    };

    public void load(String language) {
        font = new GlyphFont();
        reloadI18n(language);
        shot = snd("sfx/shot.wav");
        pop = snd("sfx/pop.wav");
        explosion = snd("sfx/explosion.wav");
        laser = snd("sfx/laser.wav");
        rocket = snd("sfx/rocket.wav");
        buy = snd("sfx/buy.wav");
        wave = snd("sfx/wave.wav");
        boss = snd("sfx/boss.wav");
        plasma = snd("sfx/plasma.wav");
        electric = snd("sfx/electric.wav");
        ice = snd("sfx/ice.wav");
        loadIcons();
    }

    private void loadIcons() {
        for (String name : ICON_NAMES) {
            try {
                Texture t = new Texture(Gdx.files.internal("icons/" + name + ".png"));
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                icons.put(name, t);
            } catch (Exception ignored) { }
        }
    }

    public Texture icon(String name) { return icons.get(name); }

    private Sound snd(String path) {
        try { return Gdx.audio.newSound(Gdx.files.internal(path)); }
        catch (Exception e) { return null; }
    }

    public void reloadI18n(String language) {
        Locale locale = "en".equalsIgnoreCase(language) ? Locale.ENGLISH : new Locale("ru");
        i18n = I18NBundle.createBundle(Gdx.files.internal("i18n/messages"), locale, "UTF-8");
    }

    public String t(String key) { return i18n.get(key); }

    public void play(Sound s, GlobalSettings settings, float volume) {
        if (settings.sound && s != null) s.play(Math.max(0f, Math.min(1f, volume)));
    }

    public void dispose() {
        if (font != null) font.dispose();
        Sound[] ss = {shot,pop,explosion,laser,rocket,buy,wave,boss,plasma,electric,ice};
        for (Sound s:ss) if (s!=null) s.dispose();
        for (Texture t : icons.values()) if (t != null) t.dispose();
        icons.clear();
    }
}
