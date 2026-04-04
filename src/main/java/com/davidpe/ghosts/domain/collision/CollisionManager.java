package com.davidpe.ghosts.domain.collision;

import java.util.ArrayList;
import java.util.List;

/**
 * Broad-phase collision detector that computes AABB overlaps between registered {@link Collider}s
 * each frame.
 *
 * <p>Typical per-frame usage:
 *
 * <pre>{@code
 * collisionManager.clear();
 * collisionManager.register(arthur.getBodyCollider());
 * collisionManager.register(arthur.getAttackCollider()); // null-safe (no-op when inactive)
 * collisionManager.register(zombie.getCollider());       // null-safe (no-op when inactive)
 * for (Tombstone t : tombstones) {
 *     collisionManager.register(t.getCollider());        // null-safe (no-op when invisible)
 * }
 * List<CollisionPair> pairs = collisionManager.computeCollisions();
 * for (CollisionPair pair : pairs) {
 *     handleCollision(pair);
 * }
 * }</pre>
 *
 * <p>Only layer pairs listed in {@link #shouldCheck} are ever tested for overlap, keeping the
 * number of rectangle checks proportional to the number of meaningful interaction types rather than
 * to the square of the total number of colliders.
 */
public final class CollisionManager {

  private final List<Collider> colliders = new ArrayList<>();

  /**
   * Removes all previously registered colliders. Call once at the start of each frame before
   * re-registering the active objects.
   */
  public void clear() {
    colliders.clear();
  }

  /**
   * Registers a collider for the current frame's overlap check. Passing {@code null} is a no-op,
   * which allows callers to register optional colliders (e.g. attack hitboxes) without an explicit
   * null-guard.
   *
   * @param collider the collider to register, or {@code null} to skip
   */
  public void register(Collider collider) {
    if (collider != null) {
      colliders.add(collider);
    }
  }

  /**
   * Tests all registered collider pairs that belong to interacting layer combinations and returns
   * every overlapping pair.
   *
   * <p>Each pair is reported at most once per frame. The result list is freshly allocated on every
   * call; callers should not retain it across frames.
   *
   * @return the list of overlapping {@link CollisionPair}s detected this frame, never {@code null}
   */
  public List<CollisionPair> computeCollisions() {
    List<CollisionPair> result = new ArrayList<>();
    for (int i = 0; i < colliders.size(); i++) {
      Collider a = colliders.get(i);
      for (int j = i + 1; j < colliders.size(); j++) {
        Collider b = colliders.get(j);
        if (shouldCheck(a, b) && a.getBounds().overlaps(b.getBounds())) {
          result.add(new CollisionPair(a, b));
        }
      }
    }
    return result;
  }

  /**
   * Defines which layer combinations are worth testing for overlap. Pairs not listed here are
   * silently skipped, avoiding unnecessary rectangle checks.
   *
   * <p>Supported pairs:
   *
   * <ul>
   *   <li>{@code PLAYER} ↔ {@code ENEMY} — contact-damage detection
   *   <li>{@code PLAYER_ATTACK} ↔ {@code ENEMY} — melee-hit detection
   *   <li>{@code ENEMY} ↔ {@code OBSTACLE} — enemy push-out / bounce
   * </ul>
   */
  private boolean shouldCheck(Collider a, Collider b) {
    CollisionLayer la = a.getLayer();
    CollisionLayer lb = b.getLayer();
    return isMatch(la, lb, CollisionLayer.PLAYER, CollisionLayer.ENEMY)
        || isMatch(la, lb, CollisionLayer.PLAYER_ATTACK, CollisionLayer.ENEMY)
        || isMatch(la, lb, CollisionLayer.ENEMY, CollisionLayer.OBSTACLE);
  }

  private static boolean isMatch(
      CollisionLayer la, CollisionLayer lb, CollisionLayer x, CollisionLayer y) {
    return (la == x && lb == y) || (la == y && lb == x);
  }
}
