package com.davidpe.ghosts.domain.characters;

/**
 * Contract for domain objects that can be rendered by the game orchestrator.
 *
 * <p>Implementations provide all data required to draw themselves without depending on {@code
 * SpriteBatch} directly.
 */
public interface Drawable {
  /**
   * Returns the render payload for the current frame.
   *
   * @return the current {@link RenderData}, or {@code null} when the object should not be drawn
   */
  RenderData getRenderData();
}
