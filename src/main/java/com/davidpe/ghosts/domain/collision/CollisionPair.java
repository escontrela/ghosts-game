package com.davidpe.ghosts.domain.collision;

/**
 * An immutable pair of {@link Collider}s whose bounding boxes overlap during a given frame.
 *
 * <p>Produced by {@link CollisionManager#computeCollisions()} and consumed by the game controller
 * to dispatch the appropriate response (damage, push-out, pickup, etc.).
 *
 * <p>The ordering of {@code a} and {@code b} within a pair is determined by the iteration order of
 * the registered colliders, not by layer priority. Handlers must therefore be written to tolerate
 * either ordering (e.g. check both {@code a.getLayer()} and {@code b.getLayer()}).
 *
 * @param a the first collider in the overlapping pair
 * @param b the second collider in the overlapping pair
 */
public record CollisionPair(Collider a, Collider b) {}
