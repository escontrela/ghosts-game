package com.davidpe.ghosts.domain.characters;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Immutable render payload used by the game controller to draw a domain object.
 *
 * @param region texture region to draw for the current frame
 * @param x world-space X position where the sprite should be rendered
 * @param y world-space Y position where the sprite should be rendered
 * @param width draw width in world units (before applying optional flip)
 * @param height draw height in world units
 * @param flipX whether the sprite should be mirrored horizontally
 */
public record RenderData(
    TextureRegion region, float x, float y, float width, float height, boolean flipX) {}
