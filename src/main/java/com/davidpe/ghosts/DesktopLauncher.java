package com.davidpe.ghosts;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.davidpe.ghosts.application.GhostsGame;

/**
 * Launches the desktop (LWJGL3) application. The main method serves as the entry point of the
 * application, where it sets up the configuration for the LWJGL3 application and starts it with an
 * instance of GhostsGame.
 */
public class DesktopLauncher {

  public static void main(String[] args) {

    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

    config.setTitle("Ghosts 'n Goblins Remake");
    config.setWindowedMode((int) GhostsGame.WORLD_WIDTH, (int) GhostsGame.WORLD_HEIGHT);
    config.setResizable(true);
    config.useVsync(true);
    config.setForegroundFPS(60);

    new Lwjgl3Application(new GhostsGame(), config);
  }
}
