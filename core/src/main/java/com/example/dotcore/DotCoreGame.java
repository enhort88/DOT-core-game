package com.example.dotcore;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public class DotCoreGame extends Game {
    public final GlobalSettings settings = new GlobalSettings();
    public Assets assets;
    public SaveRepository saves;

    @Override public void create() {
        settings.load();
        assets = new Assets();
        assets.load(settings.language);
        assets.startMusic(settings);
        saves = new SaveRepository();
        setScreen(new MenuScreen(this));
    }

    public void applyLanguage(String language) {
        settings.language = language;
        settings.save();
        assets.reloadI18n(language);
    }

    public void changeScreen(Screen next) {
        Screen old = getScreen();
        setScreen(next);
        if (old != null && old != next) old.dispose();
    }

    public void openMenu() { changeScreen(new MenuScreen(this)); }
    public void playSlot(int slot) { changeScreen(new GameScreen(this, saves.load(slot))); }

    @Override public void dispose() {
        super.dispose();
        assets.dispose();
    }
}
