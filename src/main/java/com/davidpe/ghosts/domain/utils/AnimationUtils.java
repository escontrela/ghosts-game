package com.davidpe.ghosts.domain.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Singleton utility for building LibGDX animations from sprite sheets paired with bounding-box JSON
 * files. Each JSON file describes per-frame regions (x, y, width, height) within a sprite sheet,
 * allowing frames of varying sizes. Provides helpers to load all frames, build a full animation, or
 * build a sub-range animation (useful for splitting a single sheet into multiple logical states,
 * e.g. crouch-down / crouch-up).
 *
 * <p>Intended to be injected into domain character objects via their constructors so that
 * animation-loading logic stays decoupled from character behaviour.
 */
public class AnimationUtils {

  private static AnimationUtils instance;

  private AnimationUtils() {}

  public static AnimationUtils getInstance() {
    if (instance == null) {
      instance = new AnimationUtils();
    }
    return instance;
  }

  public Animation<TextureRegion> buildAnimationFromBoundingBoxes(
      Texture sheet, String jsonPath, float frameDuration) {
    TextureRegion[] frames = loadFramesFromBoundingBoxes(sheet, jsonPath);
    return new Animation<>(frameDuration, frames);
  }

  public TextureRegion[] loadFramesFromBoundingBoxes(Texture sheet, String jsonPath) {
    JsonReader reader = new JsonReader();
    JsonValue root = reader.parse(Gdx.files.internal(jsonPath));
    TextureRegion[] frames = new TextureRegion[root.size];
    int i = 0;
    for (JsonValue entry = root.child; entry != null; entry = entry.next) {
      int fx = entry.getInt("x");
      int fy = entry.getInt("y");
      int fw = entry.getInt("width");
      int fh = entry.getInt("height");
      frames[i++] = new TextureRegion(sheet, fx, fy, fw, fh);
    }
    return frames;
  }

  public Animation<TextureRegion> buildAnimationFromRange(
      TextureRegion[] allFrames, int from, int to, float frameDuration) {
    TextureRegion[] subset = new TextureRegion[to - from];
    System.arraycopy(allFrames, from, subset, 0, subset.length);
    return new Animation<>(frameDuration, subset);
  }

  public TextureRegion[] reverseFrames(TextureRegion[] frames) {
    TextureRegion[] reversed = new TextureRegion[frames.length];
    for (int i = 0; i < frames.length; i++) {
      reversed[i] = frames[frames.length - 1 - i];
    }
    return reversed;
  }

  public Animation<TextureRegion> buildReversedAnimationFromBoundingBoxes(
      Texture sheet, String jsonPath, float frameDuration) {
    TextureRegion[] frames = loadFramesFromBoundingBoxes(sheet, jsonPath);
    return new Animation<>(frameDuration, reverseFrames(frames));
  }
}
