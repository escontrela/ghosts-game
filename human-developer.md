# Human Developer Guide — Ghosts Game

Technical guide for developers working on the current game build in branch `feature/zombie-enemies`.

## 1. Project map and responsibilities

Main packages:

- `com.davidpe.ghosts.application`: app orchestration (`GhostsGame`) and infra (`GameAudio`, `CharacterFactory`).
- `com.davidpe.ghosts.domain.characters`: domain entities (`Character`, `Arthur`, `Zombie`, tuning constants).
- `com.davidpe.ghosts.domain.obstacles`: world obstacle domain (`Tombstone`).
- `com.davidpe.ghosts.domain.utils`: animation loading pipeline (`AnimationUtils`).

Current design rule: keep gameplay behavior mostly in `Arthur`, `Zombie`, and `GhostsGame`; avoid proliferating micro-classes.

## 2. Main game loop (`GhostsGame`)

`GhostsGame.render()` is the runtime source of truth. The order matters:

1. Read `delta` and handle pause gating.
2. Update Arthur and consume Arthur one-shot audio events.
3. Compute world scroll from Arthur (`worldOffsetX` delta).
4. Apply that scroll to Zombie and Tombstones.
5. Update tombstone segment spawning.
6. Update zombie spawner lifecycle.
7. Feed zombie target (`arthur.getX()`), then update zombie.
8. Resolve zombie/tombstone collision rules.
9. Consume zombie one-shot defeat event (score + marker + sound).
10. Resolve Arthur punch hit vs zombie.
11. Resolve zombie contact damage to Arthur.
12. Render in strict draw order.

Why this sequence exists:

- Arthur is the scroll driver; background and obstacle movement are derived from Arthur, not camera position.
- Zombie collision and damage are evaluated after all entity movement for the frame.
- One-shot events (`consume...`) are intentionally consumed once per frame to avoid duplication.

### Render order details

The draw stack is:

- scrolling backgrounds
- dark overlay
- zombie
- tombstones
- Arthur light effect
- Arthur sprite
- HUDs (enemy marker, score, energy, pause)

This guarantees obstacle readability while keeping Arthur visible on top.

## 3. Position model and world movement

The project uses a fixed virtual world (`800x600`) with a camera comfort window. Arthur typically stays inside comfort bounds and world scroll accumulates as:

```java
worldOffsetX += scrollVelocityX * delta;
```

`scrollVelocityX` is filtered (lerp + exponential response) to avoid camera jerk.

Important implication: most moving world elements (zombie + tombstones + backgrounds) are moved by applying Arthur scroll delta each frame. This keeps parallax and obstacle interactions coherent.

## 4. Arthur internals

### State machine

`Arthur` states are:

- `IDLE`
- `WALK`
- `CROUCH`
- `CROUCH_UP`
- `JUMP`
- `PUNCH`

`PUNCH` and `CROUCH_UP` are one-shot animation states; transitions use `resetStateTime()`.

### Input and movement flow

`Arthur.updateMovement(delta)` performs:

1. Input sampling.
2. State locks (`PUNCH`, `CROUCH_UP`) with early returns.
3. Jump trigger on grounded state.
4. Vertical physics (rise/fall gravities, landing soft zone).
5. Air or ground horizontal movement.
6. Tombstone collision resolution.
7. State resolution (`WALK`/`IDLE`/`CROUCH` etc).
8. Scroll update.

### Tombstone interaction

Arthur now receives tombstone colliders via `setTombstoneColliders(...)` from `GhostsGame.create()`.

Collision behavior:

- side hit while walking: Arthur keeps `WALK` animation if input persists but `x` is resolved at obstacle edge (no effective horizontal advance).
- top landing: vertical resolution places Arthur on top of tombstone and zeroes fall velocity.
- edge drop: when no longer supported by tombstone, Arthur returns to air logic and falls naturally.

The helper methods to review in `Arthur`:

- `isStandingOnTombstone()`
- `resolveTombstoneCollisions()`

### Combat event window

Punch hit registration is event-based:

- `Arthur` opens a delayed `punchHitWindowPending` inside `PUNCH`.
- `GhostsGame.processArthurPunchHit()` consumes that window only on valid range + zombie eligibility.

This avoids auto-hit on every frame of the punch animation.

### Energy and hit sounds

`Arthur.applyContactEnergyDrain(...)`:

- drains only when contact is active,
- caps unstable frame deltas (`MAX_ENERGY_DRAIN_DELTA_SECONDS`),
- emits hit sound event with cooldown (`HIT_SOUND_COOLDOWN_SECONDS`).

## 5. Zombie internals

### Lifecycle and states

`Zombie` states:

- `GROUND_RISE`
- `WALK`
- `HITTED`
- `GROUND_HIDE`
- `DEATH`

Lifecycle loops through:

`GROUND_RISE -> WALK -> (HITTED/WALK)* -> (GROUND_HIDE or DEATH) -> RESPAWN`

`GhostsGame` handles respawn timer and reactivation.

### Key combat behavior

- valid hits are accepted only during `WALK` (`registerValidHit()`).
- hits accumulate to threshold (`3`), then transition to `DEATH`.
- `consumeDefeatByHitEvent()` is one-shot and is the only score increment trigger.

### Tombstone rules for zombie

Implemented runtime guarantees:

- in `WALK`, overlapping tombstone triggers `bounceFromObstacle(...)`:
  - zombie is pushed out of obstacle,
  - movement direction is inverted by assigning avoidance target opposite to collision side.
- if collision happens again on opposite direction, same rule repeats (logical bounce).
- when not walking (spawn/hide/death), overlap is resolved by `pushOutOfObstacle(...)` to prevent finishing superposed.
- spawn X is resolved with retry steps to avoid starting `GROUND_RISE` inside visible tombstones.

## 6. Tombstone system

Tombstones are domain obstacles with sprite-derived collision bounds.

Current spawn policy in `GhostsGame`:

- evaluate once per world segment (`floor(worldOffsetX / WORLD_WIDTH)`),
- random decision of `0` or `1` tombstone,
- max one visible at a time (`MAX_VISIBLE_TOMBSTONES = 1`),
- offscreen cull margin cleanup.

This produces controlled randomness and avoids per-frame jitter decisions.

## 7. Gameplay rules currently active

- **Energy:** starts at 100, drains while zombie contact is active in `WALK`, can reach 0 without hard game-over reset.
- **Score:** increments by +1 only when zombie defeat-by-hit one-shot event is consumed.
- **Enemy marker:** HUD top-right tracks defeats by enemy type (currently zombie only).
- **Pause:** `ESC` pauses simulation; any key resumes.
- **Audio:** central `GameAudio` dispatch from domain/app events.

## 8. Code excerpts with practical explanation

### Excerpt A — Arthur punch hit processing

```java
if (!arthur.isPunchHitWindowPending() || !zombie.isWalking()) {
  return;
}
...
if (horizontalGap <= ARTHUR_PUNCH_REACH
    && arthur.consumePunchHitWindow()
    && zombie.registerValidHit()) {
  gameAudio.play(GameAudio.Cue.ENEMY_HIT);
}
```

Line-by-line intent:

- first guard prevents consuming the hit window unless both punch window and zombie state are valid.
- distance checks enforce horizontal/vertical reach constraints.
- `consumePunchHitWindow()` ensures single registration per punch window.
- `registerValidHit()` keeps zombie state rules centralized in domain.

### Excerpt B — Zombie respawn driver

```java
if (zombieCycleActive) {
  if (zombie.consumeHideCycleCompleted()) {
    zombieCycleActive = false;
    zombieRespawnTimer = randomRespawnDelay();
  }
  return;
}
zombieRespawnTimer -= delta;
if (zombieRespawnTimer <= 0f) {
  activateZombieCycle(pickSpawnSide());
}
```

Intent:

- keeps lifecycle transitions explicit and stable.
- no hidden automatic respawn inside Zombie; app layer controls timing.

## 9. Gotchas and common break points

1. **Delta-time spikes:** uncapped `delta` can destabilize energy drain and movement. Keep caps where already used.
2. **One-shot event misuse:** calling `consume...` twice in one frame silently drops behavior next frame.
3. **Update order sensitivity:** changing render/update sequence can break score/audio/contact assumptions.
4. **Scroll coupling:** if new actors do not receive `applyWorldScroll(scrollDelta)`, they will drift against the world.
5. **State resets:** on state transitions, always use `resetStateTime()`; direct `stateTime=0` can desync animation/state assumptions.
6. **Obstacle overlap loops:** if you add more obstacles, test repeated collision resolution to avoid oscillation/jitter.

## 10. Safe extension guidelines

When extending gameplay:

1. Add behavior first in domain entities (`Arthur`, `Zombie`, `Tombstone`) and keep orchestration in `GhostsGame`.
2. Reuse existing one-shot event pattern instead of polling state from multiple places.
3. Keep new constants grouped near existing tuning constants.
4. Validate with `mvn -q -DskipTests compile` and `mvn -q test` after each ticket-sized change.
5. Update `docs/features.md` per delivered feature/ticket.

## 11. Fast debugging checklist

- Validate branch: `feature/zombie-enemies`.
- Confirm no accidental status drift in Tasker before/after code changes.
- Manual run path:
  - check Arthur jump/crouch/punch states,
  - verify zombie lifecycle and score events,
  - verify tombstone spawn randomness and collision behavior,
  - verify HUD consistency (score, energy, pause, enemy marker).

