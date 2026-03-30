package com.davidpe.ghosts.domain.characters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.davidpe.ghosts.domain.utils.AnimationUtils;

/**
 * Player character Arthur. Extends {@link Character} with keyboard-driven input, a six-state
 * animation machine (IDLE, WALK, CROUCH, CROUCH_UP, JUMP, PUNCH), camera-tracking scroll, and a
 * dynamic focal light effect. Each state loads its own sprite sheet via bounding-box JSON through
 * an injected {@link AnimationUtils} instance.
 *
 * <p>Arthur-specific public methods beyond the inherited API:
 *
 * <ul>
 *   <li>{@code drawEffects(batch)} — renders the focal light around Arthur
 *   <li>{@code getWorldOffsetX()} — exposes the horizontal scroll offset for backgrounds
 * </ul>
 */
public class Arthur extends Character {

  // --- Sprite / draw constants ---
  private static final float DRAW_HEIGHT = 120f;
  private static final float GROUND_Y = 130f;

  // --- Movement & physics ---
  private static final float MOVE_SPEED = 235f;
  private static final float GROUND_ACCELERATION = 1700f;
  private static final float GROUND_DECELERATION = 2200f;
  private static final float AIR_CONTROL_ACCELERATION = 820f;
  private static final float JUMP_VELOCITY = 520f;
  private static final float JUMP_RISE_GRAVITY = 1180f;
  private static final float JUMP_FALL_GRAVITY = 1030f;
  private static final float MAX_FALL_SPEED = 620f;
  private static final float LANDING_SOFT_ZONE = 42f;
  private static final float LANDING_GRAVITY_SCALE = 0.58f;
  private static final float LANDING_STABILIZE_DURATION = 0.08f;
  private static final float LANDING_STABILIZE_ACCELERATION = 900f;

  // --- Camera / scroll ---
  private static final float CAMERA_COMFORT_LEFT = 320f;
  private static final float CAMERA_COMFORT_RIGHT = 480f;
  private static final float CAMERA_SCROLL_OVERFLOW_GAIN = 1.12f;
  private static final float CAMERA_MAX_SCROLL_SPEED = 270f;
  private static final float SCROLL_RESPONSE_RATE = 13f;

  // --- Lighting ---
  private static final float LIGHT_RESPONSE_RATE = 7f;
  private static final float LIGHT_ALPHA_IDLE = 0.22f;
  private static final float LIGHT_ALPHA_ACTIVE = 0.27f;
  private static final float LIGHT_SIZE_IDLE = 254f;
  private static final float LIGHT_SIZE_ACTIVE = 300f;
  private static final float LIGHT_TORSO_X = 0.5f;
  private static final float LIGHT_TORSO_Y_IDLE = 0.52f;
  private static final float LIGHT_TORSO_Y_WALK = 0.51f;
  private static final float LIGHT_TORSO_Y_CROUCH = 0.4f;
  private static final float LIGHT_TORSO_Y_CROUCH_UP = 0.45f;
  private static final float LIGHT_TORSO_Y_JUMP = 0.56f;
  private static final float LIGHT_TORSO_Y_PUNCH = 0.52f;

  // --- Crouch split: rows 0-4 = crouch down (20 frames), rows 4-end = stand up (13 frames) ---
  private static final int CROUCH_DOWN_END_FRAME = 20;

  // --- Animation frame durations per state ---
  private static final float IDLE_FRAME_DURATION = 0.07f;
  private static final float WALK_FRAME_DURATION = 0.035f;
  private static final float JUMP_FRAME_DURATION = 0.04f;
  private static final float PUNCH_FRAME_DURATION = 0.035f;
  private static final float CROUCH_FRAME_DURATION = 0.035f;

  // --- State machine ---
  private enum MovementState {
    IDLE,
    WALK,
    CROUCH,
    CROUCH_UP,
    JUMP,
    PUNCH
  }

  // --- Animations ---
  private Animation<TextureRegion> idleAnimation;
  private Animation<TextureRegion> walkAnimation;
  private Animation<TextureRegion> crouchDownAnimation;
  private Animation<TextureRegion> crouchUpAnimation;
  private Animation<TextureRegion> jumpAnimation;
  private Animation<TextureRegion> punchAnimation;

  // --- Lighting texture (needed by drawEffects) ---
  private Texture lightTexture;

  // --- Arthur-specific state ---
  private MovementState movementState;
  private boolean movingHorizontally;
  private float worldOffsetX;
  private float scrollVelocityX;
  private float landingStabilizeTimer;
  private float crouchAnchorX;
  private float currentLightAlpha;
  private float currentLightSize;

  public Arthur(float worldWidth, AnimationUtils animationUtils) {
    super(worldWidth);

    // --- Load individual sprite sheets (registered for automatic disposal) ---
    Texture idleSheet = loadSheet("arthur/sprite-sheet-arthur-idle.png");
    Texture walkSheet = loadSheet("arthur/sprite-sheet-arthur-walk.png");
    Texture jumpSheet = loadSheet("arthur/sprite-sheet-arthur-jump.png");
    Texture punchSheet = loadSheet("arthur/sprite-sheet-arthur-punch.png");
    Texture crouchSheet = loadSheet("arthur/sprite-sheet-arthur-crouching.png");

    // --- Build animations from bounding-box JSON ---
    idleAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            idleSheet, "arthur/bounding-boxes-arthur-idle.json", IDLE_FRAME_DURATION);
    walkAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            walkSheet, "arthur/bouding-boxes-arthur-walk.json", WALK_FRAME_DURATION);
    jumpAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            jumpSheet, "arthur/bounding-boxes-arthur-jump.json", JUMP_FRAME_DURATION);
    punchAnimation =
        animationUtils.buildAnimationFromBoundingBoxes(
            punchSheet, "arthur/bounding-boxes-arthur-punch.json", PUNCH_FRAME_DURATION);

    // --- Crouch: split into crouch-down (frames 0..19) and stand-up (frames 16..end) ---
    TextureRegion[] allCrouchFrames =
        animationUtils.loadFramesFromBoundingBoxes(
            crouchSheet, "arthur/bounding-boxes-arthur-crouching.json");
    crouchDownAnimation =
        animationUtils.buildAnimationFromRange(
            allCrouchFrames, 0, CROUCH_DOWN_END_FRAME, CROUCH_FRAME_DURATION);
    crouchUpAnimation =
        animationUtils.buildAnimationFromRange(
            allCrouchFrames,
            CROUCH_DOWN_END_FRAME - 4,
            allCrouchFrames.length,
            CROUCH_FRAME_DURATION);

    // --- Lighting ---
    lightTexture = createLightTexture(256);
    ownedTextures.add(lightTexture);

    // --- Initial state ---
    TextureRegion firstFrame = idleAnimation.getKeyFrame(0f);
    float aspectRatio = (float) firstFrame.getRegionWidth() / firstFrame.getRegionHeight();
    drawWidth = DRAW_HEIGHT * aspectRatio;
    x = (worldWidth - drawWidth) / 2f;
    y = GROUND_Y;
    velocityX = 0f;
    velocityY = 0f;
    movementState = MovementState.IDLE;
    facingRight = true;
    movingHorizontally = false;
    worldOffsetX = 0f;
    scrollVelocityX = 0f;
    landingStabilizeTimer = 0f;
    crouchAnchorX = x;
    currentLightAlpha = LIGHT_ALPHA_IDLE;
    currentLightSize = LIGHT_SIZE_IDLE;
  }

  // ---------------------------------------------------------------------------
  // Arthur-specific public API
  // ---------------------------------------------------------------------------

  public void drawEffects(SpriteBatch batch) {
    float torsoAnchorX = x + (drawWidth * LIGHT_TORSO_X);
    float torsoAnchorY = y + (DRAW_HEIGHT * getTorsoAnchorY());
    float lightX = torsoAnchorX - (currentLightSize * 0.5f);
    float lightY = torsoAnchorY - (currentLightSize * 0.5f);
    Color previousColor = new Color(batch.getColor());
    batch.setColor(1f, 1f, 1f, currentLightAlpha);
    batch.draw(lightTexture, lightX, lightY, currentLightSize, currentLightSize);
    batch.setColor(previousColor);
  }

  public float getWorldOffsetX() {
    return worldOffsetX;
  }

  // ---------------------------------------------------------------------------
  // Character abstract hooks
  // ---------------------------------------------------------------------------

  @Override
  protected void updateBehavior(float delta) {
    MovementState previousState = movementState;
    updateMovement(delta);
    updateLighting(delta);
    if (movementState != previousState) {
      resetStateTime();
    }
  }

  @Override
  protected TextureRegion getCurrentFrame() {
    Animation<TextureRegion> anim =
        switch (movementState) {
          case IDLE -> idleAnimation;
          case WALK -> walkAnimation;
          case CROUCH -> crouchDownAnimation;
          case CROUCH_UP -> crouchUpAnimation;
          case JUMP -> jumpAnimation;
          case PUNCH -> punchAnimation;
        };
    boolean looping =
        movementState != MovementState.PUNCH
            && movementState != MovementState.CROUCH
            && movementState != MovementState.CROUCH_UP;
    return anim.getKeyFrame(stateTime, looping);
  }

  @Override
  protected float getDrawHeight() {
    return DRAW_HEIGHT;
  }

  // ---------------------------------------------------------------------------
  // Input, physics, state machine, scroll
  // ---------------------------------------------------------------------------

  private void updateMovement(float delta) {
    boolean leftPressed =
        Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
    boolean rightPressed =
        Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
    boolean downPressed =
        Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
    boolean jumpPressed =
        Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W);
    boolean punchPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    boolean isOnGround = y <= GROUND_Y;

    int horizontalInput = 0;
    if (leftPressed ^ rightPressed) {
      horizontalInput = rightPressed ? 1 : -1;
    }
    movingHorizontally = horizontalInput != 0 && !downPressed;
    if (movingHorizontally) {
      facingRight = horizontalInput > 0;
    }

    // --- Punch: one-shot animation, blocks other grounded actions ---
    if (movementState == MovementState.PUNCH) {
      if (punchAnimation.isAnimationFinished(stateTime)) {
        movementState = MovementState.IDLE;
      } else {
        velocityX = 0f;
        updateScroll(delta);
        return;
      }
    }
    if (punchPressed && isOnGround) {
      movementState = MovementState.PUNCH;
      resetStateTime();
      velocityX = 0f;
      updateScroll(delta);
      return;
    }

    // --- Crouch up: stand-up animation after releasing crouch key ---
    if (movementState == MovementState.CROUCH_UP) {
      if (crouchUpAnimation.isAnimationFinished(stateTime)) {
        movementState = MovementState.IDLE;
      } else {
        velocityX = 0f;
        x = crouchAnchorX;
        updateScroll(delta);
        return;
      }
    }

    boolean wasAirborne = !isOnGround || movementState == MovementState.JUMP;
    if (jumpPressed && isOnGround) {
      velocityY = JUMP_VELOCITY;
      movementState = MovementState.JUMP;
    }

    if (!isOnGround || movementState == MovementState.JUMP) {
      float gravity = velocityY > 0f ? JUMP_RISE_GRAVITY : JUMP_FALL_GRAVITY;
      if (velocityY < 0f && y - GROUND_Y < LANDING_SOFT_ZONE) {
        gravity *= LANDING_GRAVITY_SCALE;
      }
      velocityY = Math.max(velocityY - gravity * delta, -MAX_FALL_SPEED);
      y += velocityY * delta;
      if (y <= GROUND_Y) {
        y = GROUND_Y;
        velocityY = 0f;
      } else {
        movementState = MovementState.JUMP;
      }
    }

    boolean groundedAfterPhysics = y <= GROUND_Y;
    if (wasAirborne && groundedAfterPhysics) {
      landingStabilizeTimer = LANDING_STABILIZE_DURATION;
    }

    if (!groundedAfterPhysics) {
      if (horizontalInput != 0) {
        float airTargetVelocityX = horizontalInput * MOVE_SPEED;
        velocityX = moveTowards(velocityX, airTargetVelocityX, AIR_CONTROL_ACCELERATION * delta);
      }
      x += velocityX * delta;
    }

    boolean crouchRequested = downPressed && groundedAfterPhysics;
    if (groundedAfterPhysics) {
      float groundResponseAcceleration =
          landingStabilizeTimer > 0f ? LANDING_STABILIZE_ACCELERATION : GROUND_ACCELERATION;
      if (crouchRequested) {
        if (movementState != MovementState.CROUCH) {
          crouchAnchorX = x;
        }
        velocityX = 0f;
        x = crouchAnchorX;
      } else {
        float targetVelocityX = horizontalInput * MOVE_SPEED;
        float maxDelta =
            (targetVelocityX == 0f ? GROUND_DECELERATION : groundResponseAcceleration) * delta;
        velocityX = moveTowards(velocityX, targetVelocityX, maxDelta);
      }
      x += velocityX * delta;
    }
    landingStabilizeTimer = Math.max(0f, landingStabilizeTimer - delta);

    if (groundedAfterPhysics) {
      if (downPressed) {
        movementState = MovementState.CROUCH;
      } else if (movementState == MovementState.CROUCH) {
        movementState = MovementState.CROUCH_UP;
        resetStateTime();
      } else if (Math.abs(velocityX) > 8f) {
        movementState = MovementState.WALK;
      } else {
        movementState = MovementState.IDLE;
      }
    }

    x = Math.max(0f, Math.min(x, worldWidth - drawWidth));
    if (crouchRequested) {
      crouchAnchorX = x;
    }

    updateScroll(delta);
  }

  private void updateScroll(float delta) {
    float targetScrollVelocity = 0f;
    float comfortMin = CAMERA_COMFORT_LEFT - (drawWidth * 0.5f);
    float comfortMax = CAMERA_COMFORT_RIGHT - (drawWidth * 0.5f);
    if (x < comfortMin) {
      float overflow = x - comfortMin;
      x = comfortMin;
      targetScrollVelocity =
          MathUtils.clamp(
              (overflow / Math.max(delta, 0.0001f)) * CAMERA_SCROLL_OVERFLOW_GAIN,
              -CAMERA_MAX_SCROLL_SPEED,
              CAMERA_MAX_SCROLL_SPEED);
    } else if (x > comfortMax) {
      float overflow = x - comfortMax;
      x = comfortMax;
      targetScrollVelocity =
          MathUtils.clamp(
              (overflow / Math.max(delta, 0.0001f)) * CAMERA_SCROLL_OVERFLOW_GAIN,
              -CAMERA_MAX_SCROLL_SPEED,
              CAMERA_MAX_SCROLL_SPEED);
    }

    float scrollBlend = 1f - (float) Math.exp(-SCROLL_RESPONSE_RATE * delta);
    scrollVelocityX = MathUtils.lerp(scrollVelocityX, targetScrollVelocity, scrollBlend);
    if (Math.abs(targetScrollVelocity - scrollVelocityX) < 3f) {
      scrollVelocityX = targetScrollVelocity;
    }
    if (Math.abs(scrollVelocityX) < 0.5f) {
      scrollVelocityX = 0f;
    }
    worldOffsetX += scrollVelocityX * delta;
  }

  // ---------------------------------------------------------------------------
  // Lighting
  // ---------------------------------------------------------------------------

  private float getTorsoAnchorY() {
    return switch (movementState) {
      case IDLE -> LIGHT_TORSO_Y_IDLE;
      case WALK -> LIGHT_TORSO_Y_WALK;
      case CROUCH -> LIGHT_TORSO_Y_CROUCH;
      case CROUCH_UP -> LIGHT_TORSO_Y_CROUCH_UP;
      case JUMP -> LIGHT_TORSO_Y_JUMP;
      case PUNCH -> LIGHT_TORSO_Y_PUNCH;
    };
  }

  private void updateLighting(float delta) {
    float speedFactor = MathUtils.clamp(Math.abs(velocityX) / MOVE_SPEED, 0f, 1f);
    float jumpBoost = movementState == MovementState.JUMP ? 0.25f : 0f;
    float activityFactor = MathUtils.clamp(Math.max(speedFactor, jumpBoost), 0f, 1f);
    if (movementState == MovementState.CROUCH || movementState == MovementState.CROUCH_UP) {
      activityFactor *= 0.4f;
    }
    float targetLightAlpha = MathUtils.lerp(LIGHT_ALPHA_IDLE, LIGHT_ALPHA_ACTIVE, activityFactor);
    float targetLightSize = MathUtils.lerp(LIGHT_SIZE_IDLE, LIGHT_SIZE_ACTIVE, activityFactor);
    float blend = 1f - (float) Math.exp(-LIGHT_RESPONSE_RATE * delta);
    currentLightAlpha = MathUtils.lerp(currentLightAlpha, targetLightAlpha, blend);
    currentLightSize = MathUtils.lerp(currentLightSize, targetLightSize, blend);
  }

  // ---------------------------------------------------------------------------
  // Texture utilities
  // ---------------------------------------------------------------------------

  private Texture loadSheet(String path) {
    Texture sheet = new Texture(Gdx.files.internal(path));
    sheet.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    ownedTextures.add(sheet);
    return sheet;
  }

  private Texture createLightTexture(int size) {
    Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
    float center = size / 2f;
    float radius = size / 2f;
    for (int py = 0; py < size; py++) {
      for (int px = 0; px < size; px++) {
        float dx = px - center;
        float dy = py - center;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        float normalized = Math.min(1f, distance / radius);
        float alpha = 1f - normalized;
        alpha = alpha * alpha * 0.85f;
        pixmap.setColor(1f, 0.93f, 0.76f, alpha);
        pixmap.drawPixel(px, py);
      }
    }
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }
}
