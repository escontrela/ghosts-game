package com.davidpe.ghosts.domain.characters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.davidpe.ghosts.domain.utils.AnimationUtils;

public class Zombie extends Character {

  private static final float DRAW_HEIGHT = 145f;
  private static final float GROUND_Y = 130f;
  private static final float WALK_SPEED = 70f;
  private static final float TARGET_REACH_EPSILON = 2f;

  private static final float WALK_FRAME_DURATION = 0.06f;
  private static final float GROUND_FRAME_DURATION = 0.07f;
  private static final float HITTED_FRAME_DURATION = 0.05f;

  public enum SpawnSide {
    AHEAD,
    BEHIND
  }

  private enum MovementState {
    WALK,
    GROUND_RISE,
    GROUND_HIDE,
    HITTED
  }

  private final Animation<TextureRegion> walkAnimation;
  private final Animation<TextureRegion> groundRiseAnimation;
  private final Animation<TextureRegion> groundHideAnimation;
  private final Animation<TextureRegion> hittedAnimation;

  private MovementState movementState;
  private float targetX;
  private boolean active;
  private boolean hideCycleCompleted;

  public Zombie(float worldWidth, AnimationUtils animationUtils) {
    super(worldWidth);

    Texture walkSheet = loadSheet("zombie/sprite-sheet-zombie-walk.png");
    Texture groundSheet = loadSheet("zombie/sprite-sheet-zombie-ground.png");
    Texture hittedSheet = loadSheet("zombie/sprite-sheet-zombie-hitted.png");

    walkAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            walkSheet, "zombie/bounding-boxes-zombie-walk.json", WALK_FRAME_DURATION);
    groundRiseAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            groundSheet, "zombie/bounding-boxes-zombie-ground.json", GROUND_FRAME_DURATION);
    hittedAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            hittedSheet, "zombie/bounding-boxes-zombie-hitted.json", HITTED_FRAME_DURATION);

    TextureRegion[] groundFrames =
        animationUtils.loadFramesFromBoundingBoxes(
            groundSheet, "zombie/bounding-boxes-zombie-ground.json");
    TextureRegion[] reversedGroundFrames = reverseFrames(groundFrames);
    groundHideAnimation = new Animation<>(GROUND_FRAME_DURATION, reversedGroundFrames);

    TextureRegion firstFrame = walkAnimation.getKeyFrame(0f);
    float aspectRatio = (float) firstFrame.getRegionWidth() / firstFrame.getRegionHeight();
    drawWidth = DRAW_HEIGHT * aspectRatio;
    x = Math.max(0f, worldWidth - drawWidth - 120f);
    y = GROUND_Y;
    velocityX = 0f;
    velocityY = 0f;
    facingRight = false;
    movementState = MovementState.GROUND_RISE;
    targetX = x;
    active = true;
    hideCycleCompleted = false;
  }

  @Override
  protected void updateBehavior(float delta) {
    if (!active) {
      velocityX = 0f;
      return;
    }
    switch (movementState) {
      case GROUND_RISE -> {
        if (groundRiseAnimation.isAnimationFinished(stateTime)) {
          transitionTo(MovementState.WALK);
        }
      }
      case WALK -> {
        float distanceToTarget = targetX - x;
        if (Math.abs(distanceToTarget) <= TARGET_REACH_EPSILON) {
          velocityX = 0f;
        } else {
          velocityX = Math.signum(distanceToTarget) * WALK_SPEED;
          facingRight = velocityX > 0f;
        }
        x += velocityX * delta;
        x = clampX(x);
      }
      case HITTED -> {
        velocityX = 0f;
        if (hittedAnimation.isAnimationFinished(stateTime)) {
          transitionTo(MovementState.WALK);
        }
      }
      case GROUND_HIDE -> {
        velocityX = 0f;
        if (groundHideAnimation.isAnimationFinished(stateTime)) {
          active = false;
          hideCycleCompleted = true;
        }
      }
    }
  }

  @Override
  protected TextureRegion getCurrentFrame() {
    Animation<TextureRegion> animation =
        switch (movementState) {
          case WALK -> walkAnimation;
          case GROUND_RISE -> groundRiseAnimation;
          case GROUND_HIDE -> groundHideAnimation;
          case HITTED -> hittedAnimation;
        };
    return animation.getKeyFrame(stateTime, movementState == MovementState.WALK);
  }

  @Override
  protected float getDrawHeight() {
    return DRAW_HEIGHT;
  }

  private Texture loadSheet(String path) {
    Texture sheet = new Texture(Gdx.files.internal(path));
    sheet.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    ownedTextures.add(sheet);
    return sheet;
  }

  private TextureRegion[] reverseFrames(TextureRegion[] frames) {
    TextureRegion[] reversed = new TextureRegion[frames.length];
    for (int i = 0; i < frames.length; i++) {
      reversed[i] = frames[frames.length - 1 - i];
    }
    return reversed;
  }

  public void triggerHitted() {
    if (movementState == MovementState.WALK) {
      transitionTo(MovementState.HITTED);
    }
  }

  public void triggerGroundHide() {
    if (movementState == MovementState.WALK || movementState == MovementState.HITTED) {
      transitionTo(MovementState.GROUND_HIDE);
    }
  }

  private void transitionTo(MovementState targetState) {
    if (movementState == targetState) {
      return;
    }
    movementState = targetState;
    if (targetState == MovementState.WALK && velocityX == 0f) {
      facingRight = !facingRight;
    }
    resetStateTime();
  }

  public float resolveSpawnX(float arthurX, SpawnSide spawnSide, float spawnDistance) {
    float rawSpawnX =
        spawnSide == SpawnSide.AHEAD ? arthurX + spawnDistance : arthurX - spawnDistance;
    return clampX(rawSpawnX);
  }

  public void startGroundRiseAt(float spawnX) {
    x = clampX(spawnX);
    y = GROUND_Y;
    velocityX = 0f;
    velocityY = 0f;
    active = true;
    hideCycleCompleted = false;
    transitionTo(MovementState.GROUND_RISE);
  }

  public void setTargetX(float targetX) {
    this.targetX = clampX(targetX);
  }

  public boolean consumeHideCycleCompleted() {
    boolean completed = hideCycleCompleted;
    hideCycleCompleted = false;
    return completed;
  }

  private float clampX(float candidateX) {
    return Math.max(0f, Math.min(candidateX, worldWidth - drawWidth));
  }
}
