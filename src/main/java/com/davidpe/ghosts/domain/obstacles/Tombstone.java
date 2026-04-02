package com.davidpe.ghosts.domain.obstacles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.davidpe.ghosts.domain.utils.AnimationUtils;

public class Tombstone {

  private static final float DRAW_HEIGHT = 92f;

  private final float worldWidth;
  private final Texture texture;
  private final TextureRegion baseFrame;
  private final float drawWidth;
  private final float drawHeight;
  private final float collisionWidth;
  private final float collisionHeight;
  private float x;
  private float y;
  private boolean visible;

  public Tombstone(float worldWidth, AnimationUtils animationUtils) {
    this.worldWidth = worldWidth;
    this.texture = new Texture(Gdx.files.internal("tombstone/sprite-sheet-tombstone.png"));
    this.texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    TextureRegion[] frames =
        animationUtils.loadFramesFromBoundingBoxes(
            texture, "tombstone/bounding-boxes-tombstone.json");
    if (frames.length == 0) {
      throw new IllegalStateException("Tombstone requires at least one frame");
    }
    this.baseFrame = frames[0];
    this.drawHeight = DRAW_HEIGHT;
    this.drawWidth = drawHeight * ((float) baseFrame.getRegionWidth() / baseFrame.getRegionHeight());
    this.collisionWidth = drawWidth;
    this.collisionHeight = drawHeight;
    this.x = 0f;
    this.y = 0f;
    this.visible = false;
  }

  public void draw(SpriteBatch batch) {
    if (!visible) {
      return;
    }
    batch.draw(baseFrame, x, y, drawWidth, drawHeight);
  }

  public void dispose() {
    texture.dispose();
  }

  public void setPosition(float candidateX, float candidateY) {
    x = clampX(candidateX);
    y = candidateY;
  }

  public void applyWorldScroll(float scrollDelta) {
    x -= scrollDelta;
  }

  public boolean overlaps(float otherX, float otherY, float otherWidth, float otherHeight) {
    if (!visible) {
      return false;
    }
    float left = getCollisionX();
    float right = left + collisionWidth;
    float bottom = getCollisionY();
    float top = bottom + collisionHeight;
    return right > otherX
        && left < otherX + otherWidth
        && top > otherY
        && bottom < otherY + otherHeight;
  }

  public TextureRegion getBaseFrame() {
    return baseFrame;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public float getDrawWidth() {
    return drawWidth;
  }

  public float getDrawHeight() {
    return drawHeight;
  }

  public float getCollisionX() {
    return x;
  }

  public float getCollisionY() {
    return y;
  }

  public float getCollisionWidth() {
    return collisionWidth;
  }

  public float getCollisionHeight() {
    return collisionHeight;
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  private float clampX(float candidateX) {
    return Math.max(0f, Math.min(candidateX, worldWidth - drawWidth));
  }
}
