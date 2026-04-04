package com.davidpe.ghosts.domain.collision;

import com.badlogic.gdx.math.Rectangle;

/**
 * Represents a collidable entity that can be registered with the {@link CollisionManager}.
 *
 * <p>Implementations should compute their {@link Rectangle} bounds on demand inside {@link
 * #getBounds()} rather than caching them, so that the returned rectangle always reflects the
 * object's current position and size at the moment the collision query runs.
 *
 * <p>It is safe to return a reusable (mutated-in-place) {@link Rectangle} from {@link #getBounds()}
 * because the {@link CollisionManager} reads each rectangle only once per frame, immediately when
 * evaluating a pair.
 */
public interface Collider {

  /**
   * Returns the axis-aligned bounding box for this collider in world coordinates.
   *
   * <p>The returned instance may be a reused object updated in place; callers must not retain a
   * reference to it across frames.
   *
   * @return the current AABB in world units
   */
  Rectangle getBounds();

  /**
   * Returns the {@link CollisionLayer} that classifies this collider's role, used by {@link
   * CollisionManager} to determine which pairs are worth testing.
   *
   * @return the layer this collider belongs to
   */
  CollisionLayer getLayer();

  /**
   * Returns the domain object that owns this collider (e.g. the {@code Arthur} or {@code Zombie}
   * instance). Used by the game controller to dispatch the appropriate response after a collision
   * is detected.
   *
   * @return the owning domain object
   */
  Object getOwner();
}
