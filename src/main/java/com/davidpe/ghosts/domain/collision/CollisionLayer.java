package com.davidpe.ghosts.domain.collision;

/**
 * Classifies a {@link Collider} by its role in the game world.
 *
 * <p>The {@link CollisionManager} uses layer pairs to decide which {@link Collider}s must be tested
 * against each other each frame, avoiding redundant or undesired checks (e.g. two obstacles, or an
 * enemy against a pickup).
 *
 * <p>Layers currently considered for collision:
 *
 * <ul>
 *   <li>{@link #PLAYER} + {@link #ENEMY} — contact-damage detection
 *   <li>{@link #PLAYER_ATTACK} + {@link #ENEMY} — melee-hit detection
 *   <li>{@link #ENEMY} + {@link #OBSTACLE} — enemy push-out / bounce
 * </ul>
 */
public enum CollisionLayer {
  /** The player character body. */
  PLAYER,

  /**
   * A transient attack hitbox owned by the player (e.g. Arthur's punch). Only registered while the
   * hit window is active.
   */
  PLAYER_ATTACK,

  /** An enemy character body. */
  ENEMY,

  /** A static or semi-static obstacle in the world (e.g. a tombstone). */
  OBSTACLE,

  /** A collectible item (reserved for future use). */
  PICKUP
}
