package com.davidpe.ghosts.domain.characters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.davidpe.ghosts.domain.utils.AnimationUtils;

public class Zombie extends Character {

  private static final float DRAW_HEIGHT = 120f;
  private static final float GROUND_Y = 130f;
  private static final float WALK_SPEED = 70f;
  private static final float TARGET_REACH_EPSILON = 2f;
  private static final float HITTED_KNOCKBACK_SPEED = 180f;
  private static final float HITTED_KNOCKBACK_DURATION = 0.25f;
  private static final float OBSTACLE_BOUNCE_TARGET_DISTANCE = 150f;
  private static final float OBSTACLE_RESOLVE_MARGIN = 0.1f;

  private static final float WALK_FRAME_DURATION = 0.06f;
  private static final float GROUND_FRAME_DURATION = 0.16f;
  private static final float HITTED_FRAME_DURATION = 0.05f;
  private static final float DEATH_FRAME_DURATION = 0.12f;
  private static final float DEATH_BLINK_DURATION = 2.0f;
  private static final float DEATH_BLINK_INTERVAL = 0.12f;
  private static final float DEFAULT_ACTIVE_WALK_DURATION_SECONDS =
      ZombieTuning.ACTIVE_WALK_DURATION_SECONDS;
  private static final float DEFAULT_HITTED_RECOVERY_DELAY_SECONDS =
      ZombieTuning.HITTED_RECOVERY_DELAY_SECONDS;
  private static final int DEFEAT_HIT_THRESHOLD = 3;

  public enum SpawnSide {
    AHEAD,
    BEHIND
  }

  private enum MovementState {
    WALK,
    GROUND_RISE,
    GROUND_HIDE,
    HITTED,
    DEATH
  }

  private final Animation<TextureRegion> walkAnimation;
  private final Animation<TextureRegion> groundRiseAnimation;
  private final Animation<TextureRegion> groundHideAnimation;
  private final Animation<TextureRegion> hittedAnimation;
  private final Animation<TextureRegion> deathAnimation;

  private MovementState movementState;
  private final float activeWalkDurationSeconds;
  private final float hittedRecoveryDelaySeconds;
  private float activeWalkTimer;
  private float hittedRecoveryTimer;
  private float targetX;
  private boolean active;
  private boolean hideCycleCompleted;
  private boolean defeatedByHit;
  private boolean defeatByHitEventPending;
  private int accumulatedHits;
  private boolean deathBlinking;
  private float deathBlinkTimer;
  private boolean obstacleAvoidanceActive;
  private float obstacleAvoidanceTargetX;

  public Zombie(float worldWidth, AnimationUtils animationUtils) {
    this(
        worldWidth,
        animationUtils,
        DEFAULT_ACTIVE_WALK_DURATION_SECONDS,
        DEFAULT_HITTED_RECOVERY_DELAY_SECONDS);
  }

  public Zombie(float worldWidth, AnimationUtils animationUtils, float activeWalkDurationSeconds) {
    this(
        worldWidth,
        animationUtils,
        activeWalkDurationSeconds,
        DEFAULT_HITTED_RECOVERY_DELAY_SECONDS);
  }

  public Zombie(
      float worldWidth,
      AnimationUtils animationUtils,
      float activeWalkDurationSeconds,
      float hittedRecoveryDelaySeconds) {
    super(worldWidth);

    Texture walkSheet = loadSheet("zombie/sprite-sheet-zombie-walk.png");
    Texture groundSheet = loadSheet("zombie/sprite-sheet-zombie-ground.png");
    Texture hittedSheet = loadSheet("zombie/sprite-sheet-zombie-hitted.png");
    Texture deathSheet = loadSheet("zombie/sprite-sheet-zombie-death.png");

    walkAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            walkSheet, "zombie/bounding-boxes-zombie-walk.json", WALK_FRAME_DURATION);
    groundRiseAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            groundSheet, "zombie/bounding-boxes-zombie-ground.json", GROUND_FRAME_DURATION);
    hittedAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            hittedSheet, "zombie/bounding-boxes-zombie-hitted.json", HITTED_FRAME_DURATION);
    deathAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            deathSheet, "zombie/bounding-boxes-zombie-death.json", DEATH_FRAME_DURATION);

    groundHideAnimation =
        animationUtils.buildReversedAnimationFromBoundingBoxes(
            groundSheet, "zombie/bounding-boxes-zombie-ground.json", GROUND_FRAME_DURATION);

    TextureRegion firstFrame = walkAnimation.getKeyFrame(0f);
    float aspectRatio = (float) firstFrame.getRegionWidth() / firstFrame.getRegionHeight();
    drawWidth = DRAW_HEIGHT * aspectRatio;
    x = Math.max(0f, worldWidth - drawWidth - 120f);
    y = GROUND_Y;
    velocityX = 0f;
    velocityY = 0f;
    facingRight = false;
    movementState = MovementState.GROUND_RISE;
    this.activeWalkDurationSeconds = Math.max(0.1f, activeWalkDurationSeconds);
    this.hittedRecoveryDelaySeconds = Math.max(0f, hittedRecoveryDelaySeconds);
    activeWalkTimer = this.activeWalkDurationSeconds;
    hittedRecoveryTimer = 0f;
    targetX = x;
    active = true;
    hideCycleCompleted = false;
    defeatedByHit = false;
    defeatByHitEventPending = false;
    accumulatedHits = 0;
    deathBlinking = false;
    deathBlinkTimer = 0f;
    obstacleAvoidanceActive = false;
    obstacleAvoidanceTargetX = x;
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
        activeWalkTimer -= delta;
        if (obstacleAvoidanceActive && Math.abs(obstacleAvoidanceTargetX - x) <= TARGET_REACH_EPSILON) {
          obstacleAvoidanceActive = false;
        }
        float activeTargetX = obstacleAvoidanceActive ? obstacleAvoidanceTargetX : targetX;
        float distanceToTarget = activeTargetX - x;
        if (Math.abs(distanceToTarget) <= TARGET_REACH_EPSILON) {
          velocityX = 0f;
        } else {
          velocityX = Math.signum(distanceToTarget) * WALK_SPEED;
          facingRight = velocityX > 0f;
        }
        x = clampX(x + velocityX * delta);
        if (activeWalkTimer <= 0f) {
          defeatedByHit = false;
          transitionTo(MovementState.GROUND_HIDE);
        }
      }
      case HITTED -> {
        if (stateTime < HITTED_KNOCKBACK_DURATION) {
          float knockbackDir = facingRight ? -1f : 1f;
          x += knockbackDir * HITTED_KNOCKBACK_SPEED * delta;
        }
        if (hittedAnimation.isAnimationFinished(stateTime)) {
          hittedRecoveryTimer -= delta;
          if (hittedRecoveryTimer <= 0f && accumulatedHits < DEFEAT_HIT_THRESHOLD) {
            transitionTo(MovementState.WALK);
          }
        }
      }
      case GROUND_HIDE -> {
        velocityX = 0f;
        if (groundHideAnimation.isAnimationFinished(stateTime)) {
          active = false;
          hideCycleCompleted = true;
        }
      }
      case DEATH -> {
        velocityX = 0f;
        if (deathBlinking) {
          deathBlinkTimer -= delta;
          if (deathBlinkTimer <= 0f) {
            active = false;
            hideCycleCompleted = true;
            deathBlinking = false;
          }
        } else if (deathAnimation.isAnimationFinished(stateTime)) {
          deathBlinking = true;
          deathBlinkTimer = DEATH_BLINK_DURATION;
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
          case DEATH -> deathAnimation;
        };
    return animation.getKeyFrame(stateTime, movementState == MovementState.WALK);
  }

  @Override
  protected float getDrawHeight() {
    return DRAW_HEIGHT;
  }

  @Override
  protected float getReferenceFramePixelHeight() {
    return 360f;
  }

  @Override
  public void draw(SpriteBatch batch) {
    if (!active) {
      return;
    }
    if (deathBlinking) {
      int blinkIndex = (int) (deathBlinkTimer / DEATH_BLINK_INTERVAL);
      if (blinkIndex % 2 != 0) {
        return;
      }
    }
    super.draw(batch);
  }

  private Texture loadSheet(String path) {
    Texture sheet = new Texture(Gdx.files.internal(path));
    sheet.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    ownedTextures.add(sheet);
    return sheet;
  }

  public boolean triggerHitted() {
    return registerValidHit();
  }

  public boolean registerValidHit() {
    if (movementState == MovementState.WALK) {
      accumulatedHits = Math.min(DEFEAT_HIT_THRESHOLD, accumulatedHits + 1);
      if (accumulatedHits >= DEFEAT_HIT_THRESHOLD) {
        defeatedByHit = true;
        defeatByHitEventPending = true;
        transitionTo(MovementState.DEATH);
      } else {
        transitionTo(MovementState.HITTED);
      }
      return true;
    }
    return false;
  }

  public void triggerGroundHide() {
    if (movementState == MovementState.WALK || movementState == MovementState.HITTED) {
      defeatedByHit = false;
      transitionTo(MovementState.GROUND_HIDE);
    }
  }

  private void transitionTo(MovementState targetState) {
    if (movementState == targetState) {
      return;
    }
    movementState = targetState;
    if (targetState == MovementState.WALK) {
      activeWalkTimer = activeWalkDurationSeconds;
    }
    if (targetState == MovementState.HITTED) {
      hittedRecoveryTimer = hittedRecoveryDelaySeconds;
    }
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
    defeatedByHit = false;
    defeatByHitEventPending = false;
    accumulatedHits = 0;
    deathBlinking = false;
    deathBlinkTimer = 0f;
    obstacleAvoidanceActive = false;
    obstacleAvoidanceTargetX = x;
    transitionTo(MovementState.GROUND_RISE);
  }

  public int getAccumulatedHits() {
    return accumulatedHits;
  }

  public int getDefeatHitThreshold() {
    return DEFEAT_HIT_THRESHOLD;
  }

  public boolean isDefeatedByHit() {
    return defeatedByHit;
  }

  public boolean consumeDefeatByHitEvent() {
    boolean event = defeatByHitEventPending;
    defeatByHitEventPending = false;
    return event;
  }

  public void setTargetX(float targetX) {
    this.targetX = clampX(targetX);
  }

  public void bounceFromObstacle(float obstacleLeft, float obstacleRight) {
    if (!isWalking()) {
      return;
    }
    boolean movingRight = velocityX > 0f || (velocityX == 0f && facingRight);
    if (movingRight) {
      x = clampX(Math.min(x, obstacleLeft - drawWidth - OBSTACLE_RESOLVE_MARGIN));
      obstacleAvoidanceTargetX = clampX(x - OBSTACLE_BOUNCE_TARGET_DISTANCE);
    } else {
      x = clampX(Math.max(x, obstacleRight + OBSTACLE_RESOLVE_MARGIN));
      obstacleAvoidanceTargetX = clampX(x + OBSTACLE_BOUNCE_TARGET_DISTANCE);
    }
    obstacleAvoidanceActive = true;
    velocityX = 0f;
    facingRight = obstacleAvoidanceTargetX > x;
  }

  public void pushOutOfObstacle(float obstacleLeft, float obstacleRight) {
    float leftResolveDistance = Math.abs((x + drawWidth) - obstacleLeft);
    float rightResolveDistance = Math.abs(obstacleRight - x);
    if (leftResolveDistance <= rightResolveDistance) {
      x = clampX(obstacleLeft - drawWidth - OBSTACLE_RESOLVE_MARGIN);
    } else {
      x = clampX(obstacleRight + OBSTACLE_RESOLVE_MARGIN);
    }
    velocityX = 0f;
  }

  public void applyWorldScroll(float scrollDelta) {
    x -= scrollDelta;
  }

  public boolean consumeHideCycleCompleted() {
    boolean completed = hideCycleCompleted;
    hideCycleCompleted = false;
    return completed;
  }

  public boolean isWalking() {
    return active && movementState == MovementState.WALK;
  }

  public TextureRegion getWalkMarkerFrame() {
    return walkAnimation.getKeyFrame(0f);
  }

  @Override
  public boolean isInContactWith(float otherX, float otherY, float otherWidth, float otherHeight) {
    if (!isWalking()) {
      return false;
    }
    return super.isInContactWith(otherX, otherY, otherWidth, otherHeight);
  }

  private float clampX(float candidateX) {
    return Math.max(0f, Math.min(candidateX, worldWidth - drawWidth));
  }
}
