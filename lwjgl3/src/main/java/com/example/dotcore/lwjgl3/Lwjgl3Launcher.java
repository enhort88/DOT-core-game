package com.example.dotcore.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.example.dotcore.DotCoreGame;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("DOT//CORE Alpha 0.4");
        config.setWindowIcon("icon-128.png");
        config.setWindowedMode(540, 960);
        config.setForegroundFPS(120);
        config.useVsync(true);
        config.setResizable(true);
        new Lwjgl3Application(new DotCoreGame(), config);
    }
}
