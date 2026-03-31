package com.davidpe.ghosts.domain.characters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all game characters (player, enemies, allies). Owns the common positional
 * state (x, y, velocity, facing direction), animation timing, flip-aware rendering, and uniform
 * texture disposal. Each subclass defines its own state machine, animations, and behaviour
 * (keyboard input for the player, AI for enemies) by implementing the abstract hooks:
 *
 * <ul>
 *   <li>{@link #updateBehavior(float)} — advance physics, input/AI, state transitions
 *   <li>{@link #getCurrentFrame()} — return the texture region for the current animation frame
 *   <li>{@link #getDrawHeight()} — the on-screen height used for rendering
 * </ul>
 *
 * <p>Subclasses register their textures in {@link #ownedTextures} during construction so that
 * {@link #dispose()} releases all GPU resources uniformly.
 */
public abstract class Character {

  protected float x;
  protected float y;
  protected float velocityX;
  protected float velocityY;
  protected boolean facingRight;
  protected float drawWidth;
  protected float stateTime;
  protected final float worldWidth;
  protected final TextureRegion renderFrame;
  protected final List<Texture> ownedTextures = new ArrayList<>();

  /**
   * Initializes common character state: world bounds, a reusable render region, and zeroed
   * animation timer.
   *
   * @param worldWidth the logical width of the game world, used for boundary clamping
   */
  protected Character(float worldWidth) {
    this.worldWidth = worldWidth;
    this.renderFrame = new TextureRegion();
    this.stateTime = 0f;
  }

  /**
   * Per-frame update: delegates to the subclass-specific {@link #updateBehavior(float)} and then
   * advances the animation timer. Called once per render cycle by the game orchestrator.
   *
   * @param delta seconds elapsed since the previous frame
   */
  public void update(float delta) {
    updateBehavior(delta);
    stateTime += delta;
  }

  /**
   * Renders the character's current animation frame at its position, handling horizontal flip when
   * the character faces left. Uses {@link #getCurrentFrame()} and {@link #getDrawHeight()} provided
   * by the subclass.
   *
   * @param batch the active sprite batch (must be between {@code begin()} and {@code end()})
   */
  public void draw(SpriteBatch batch) {
    TextureRegion frameToDraw = getCurrentFrame();
    renderFrame.setRegion(frameToDraw);
    float drawX = Math.round(x);
    float dw = Math.round(drawWidth);
    if (!facingRight) {
      drawX += dw;
      dw = -dw;
    }
    batch.draw(renderFrame, drawX, Math.round(y), dw, Math.round(getDrawHeight()));
  }

  /**
   * Releases all GPU textures registered in {@link #ownedTextures}. Subclasses should add every
   * texture they create during construction so this single call frees everything.
   */
  public void dispose() {
    for (Texture texture : ownedTextures) {
      texture.dispose();
    }
  }

  /** Returns the character's current horizontal position in world coordinates. */
  public float getX() {
    return x;
  }

  /** Returns the character's current vertical position in world coordinates. */
  public float getY() {
    return y;
  }

  /** Returns the character draw width in world units. */
  public float getDrawWidth() {
    return drawWidth;
  }

  /** Returns the character draw height in world units. */
  public float getDrawHeightValue() {
    return getDrawHeight();
  }

  /**
   * Resets the animation timer to zero. Call this when the character transitions to a new state.
   */
  protected void resetStateTime() {
    stateTime = 0f;
  }

  /**
   * Moves a value towards a target by at most {@code maxDelta}, clamping when the distance is
   * smaller than the step. Useful for smooth acceleration and deceleration.
   *
   * @param current the current value
   * @param target the desired value
   * @param maxDelta the maximum allowed change this step (must be positive)
   * @return the new value, guaranteed to be no further than {@code maxDelta} from {@code current}
   */
  protected float moveTowards(float current, float target, float maxDelta) {
    if (Math.abs(target - current) <= maxDelta) {
      return target;
    }
    return current + Math.signum(target - current) * maxDelta;
  }

  /**
   * Subclass hook: advance physics, process input or AI, and update the internal state machine.
   * Called by {@link #update(float)} before the animation timer is advanced. If the character's
   * state changes, the subclass should call {@link #resetStateTime()}.
   *
   * @param delta seconds elapsed since the previous frame
   */
  protected abstract void updateBehavior(float delta);

  /**
   * Subclass hook: return the {@link TextureRegion} for the current animation frame based on the
   * character's internal state and {@link #stateTime}.
   *
   * @return the texture region to draw this frame
   */
  protected abstract TextureRegion getCurrentFrame();

  /**
   * Subclass hook: return the on-screen draw height in world units. Used by {@link #draw} to scale
   * the sprite.
   *
   * @return the height in world units
   */
  protected abstract float getDrawHeight();
}
