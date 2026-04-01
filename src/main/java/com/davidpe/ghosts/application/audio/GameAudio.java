package com.davidpe.ghosts.application.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import java.util.EnumMap;
import java.util.Map;

/**
 * Centralized WAV audio service used by application/domain orchestrators.
 *
 * <p>It keeps a single sound registry with categories and owns the full asset lifecycle
 * (load/dispose). Domain entities should emit events; orchestration layers decide when to play
 * each cue through this component.
 */
public class GameAudio {

  public enum Category {
    GAME_GENERAL,
    ARTHUR,
    ENEMY_COMMON,
    ENEMY_ZOMBIE
  }

  public enum Cue {
    GAME_START(Category.GAME_GENERAL, "common/sound/GAMESTART.wav"),
    GAME_OVER(Category.GAME_GENERAL, "common/sound/GAMEOVER.wav"),
    ARTHUR_JUMP(Category.ARTHUR, "arthur/sounds/ARTHURJUMP.wav"),
    ARTHUR_LAND(Category.ARTHUR, "arthur/sounds/ARTHURLAND.wav"),
    ARTHUR_HIT(Category.ARTHUR, "arthur/sounds/ARTHURHIT.wav"),
    ENEMY_HIT(Category.ENEMY_COMMON, "common/sound/ENEMYHIT.wav"),
    ENEMY_DEATH(Category.ENEMY_COMMON, "common/sound/ENEMYDEATH.wav"),
    ZOMBIE_SPAWN(Category.ENEMY_ZOMBIE, "zombie/sound/ZOMBIESPAWN.wav");

    private final Category category;
    private final String path;

    Cue(Category category, String path) {
      this.category = category;
      this.path = path;
    }

    public Category getCategory() {
      return category;
    }

    public String getPath() {
      return path;
    }
  }

  private final Map<Cue, Sound> sounds;
  private boolean loaded;

  public GameAudio() {
    this.sounds = new EnumMap<>(Cue.class);
    this.loaded = false;
  }

  public void loadAll() {
    if (loaded) {
      return;
    }
    for (Cue cue : Cue.values()) {
      sounds.put(cue, Gdx.audio.newSound(Gdx.files.internal(cue.getPath())));
    }
    loaded = true;
  }

  public long play(Cue cue) {
    Sound sound = sounds.get(cue);
    if (sound == null) {
      return -1L;
    }
    return sound.play();
  }

  public boolean isLoaded() {
    return loaded;
  }

  public void dispose() {
    for (Sound sound : sounds.values()) {
      sound.dispose();
    }
    sounds.clear();
    loaded = false;
  }
}
