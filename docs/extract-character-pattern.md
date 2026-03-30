# Pattern: Extract Character Entity from Monolithic Game Class

## Purpose

Guide for extracting a player/NPC character from a LibGDX `ApplicationAdapter` subclass into a self-contained domain object. The game class becomes a slim orchestrator (camera, scene, draw order) while the character owns its state, physics, input, animation, lighting, and rendering.

## Prerequisites

- A monolithic game class that mixes scene management with character logic
- Character has: spritesheet/animations, movement physics, input handling, rendering, and optionally lighting or effects

## Step-by-step extraction

### 1. Identify character boundaries

Audit the game class and classify every field, constant, and method into one of:

| Category | Stays in Game | Moves to Character |
|---|---|---|
| **Constants** | World dimensions, scene-level visual params | Movement speeds, gravity, jump params, sprite layout, lighting params, camera comfort zone |
| **Fields** | Camera, viewport, batch, background textures, scene overlays | Sprite textures, animations, position, velocity, state machine, scroll offset, lighting state |
| **Methods** | `create()` (scene setup), `render()` (orchestration), `resize()`, `dispose()` (scene resources), background drawing, scene overlays | Movement update, physics, input reading, animation frame selection, character drawing, light drawing, sprite loading/processing |

**Rule of thumb**: if it only makes sense when the character exists, it belongs to the character.

### 2. Define the public API

Keep it minimal. The game class should interact with the character through a small surface:

```
Character(float worldWidth)    // or Character(float worldWidth, float worldHeight)
void update(float delta)       // input + physics + animation + effects
void draw(SpriteBatch batch)   // render character sprite
void drawEffects(SpriteBatch batch)  // render light/particles (separate for z-order control)
float getWorldOffsetX()        // expose scroll state if character drives camera
void dispose()                 // clean up textures and resources
```

**Key principle**: the game class controls **draw order** (effects before sprite, scene behind character), the character controls **what** gets drawn.

### 3. Move the state machine

- Move the movement/state enum (e.g., `IDLE`, `WALK`, `CROUCH`, `JUMP`) as a **private inner enum** inside the character class
- No external code should need to know the character's internal state

### 4. Move constants and fields

- Move all character constants as `private static final` in the character class
- Move all character instance fields as `private` fields
- The only constructor parameter should be values the character cannot own (e.g., world dimensions)

### 5. Move logic methods

- The main `update(float delta)` method should:
  1. Read input (`Gdx.input` directly — no abstraction unless multiple input sources exist)
  2. Update physics (velocity, gravity, ground detection, clamping)
  3. Update state machine transitions
  4. Update scroll/camera offset
  5. Update effects (lighting, particles)
  6. Advance animation timer
- All helper methods (`moveTowards`, `getFrameForState`, etc.) become **private**

### 6. Move rendering methods

- `draw(SpriteBatch batch)`: select animation frame, apply direction flip, draw sprite
- `drawEffects(SpriteBatch batch)`: draw light halos, particles, etc.
- Both receive `SpriteBatch` as parameter — the character never owns the batch
- Save and restore batch color if the character modifies it

### 7. Move texture loading and processing

- Any texture processing specific to the character (e.g., transparent background removal, spritesheet splitting, frame inset trimming) moves into the character class as private methods
- Called from the constructor
- Generic texture utilities that multiple classes might use stay in the game class or a utility class

### 8. Lifecycle integration

In the game class:

```
// create()
character = new Character(WORLD_WIDTH);

// render()
character.update(delta);
batch.begin();
drawBackgrounds(character.getWorldOffsetX());  // character drives scroll
drawSceneOverlays();
character.drawEffects(batch);   // light behind sprite
character.draw(batch);          // sprite on top
batch.end();

// dispose()
character.dispose();
```

### 9. Scroll ownership rule

If the character is the **sole driver** of world scrolling (typical for single-player side-scrollers):
- Scroll state (`worldOffsetX`, `scrollVelocityX`) lives in the character
- Exposed via `getWorldOffsetX()` for background parallax
- Camera comfort window logic stays in the character's `update()`

If multiple entities affect scrolling, extract scroll into a separate `Camera`/`World` class.

## What stays in the game class

- `SpriteBatch` creation and ownership
- `OrthographicCamera` and `Viewport` setup
- Background textures and scrolling draw logic
- Scene-level overlays (darkness, fog, UI)
- Draw order orchestration
- `resize()` delegation to viewport
- Scene-level resource disposal

## Verification checklist

1. **Build passes** — `mvn compile` / `gradle build` with no errors
2. **Tests pass** — no regression
3. **Visual parity** — all character behaviors identical: movement, animation, direction flip, jump arc, lighting, scroll
4. **Clean separation** — game class has zero character-specific constants, fields, or logic methods
5. **No shared mutable state** — character and game class communicate only through the public API

## Common pitfalls

| Pitfall | Fix |
|---|---|
| Leaking `SpriteBatch` ownership into character | Pass `batch` as method parameter, never store it |
| Character needs `WORLD_WIDTH` for clamping | Pass in constructor, store as `private final` field |
| Draw order breaks after extraction | Keep `drawEffects()` and `draw()` as separate calls, game class controls order |
| Animation timer updated in wrong place | Move `stateTime` advancement into `update()`, not `draw()` |
| Forgetting to dispose character textures | Character must have `dispose()` and game class must call it |
| Over-abstracting input | Let character read `Gdx.input` directly unless you need replays or AI control |
