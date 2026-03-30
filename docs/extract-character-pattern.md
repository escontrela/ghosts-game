# Pattern: Extract Character Entity from Monolithic Game Class

## Purpose

Guide for extracting a player/NPC character from a LibGDX `ApplicationAdapter` subclass into a self-contained domain object. The game class becomes a slim orchestrator (camera, scene, draw order) while the character owns its state, physics, input, animation, lighting, and rendering.

## Prerequisites


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
### 2. Define the public API

Keep it minimal. The game class should interact with the character through a small surface:

```java
// Abstract base class (Character)
void update(float delta)              // calls updateBehavior() then advances stateTime
void draw(SpriteBatch batch)          // flip-aware sprite render using getCurrentFrame()
void dispose()                        // releases all ownedTextures automatically

// Concrete subclass (e.g. Arthur) — character-specific extras
Arthur(float worldWidth, AnimationUtils animationUtils)  // inject shared services via DI
void drawEffects(SpriteBatch batch)   // render light/particles (separate for z-order control)
float getWorldOffsetX()               // expose scroll state if character drives camera
```

**Key principle**: the game class controls **draw order** (effects before sprite, scene behind
character), the character controls **what** gets drawn.

**Factory principle**: never instantiate a character directly from the game class. Use a
`CharacterFactory` that receives shared services (`AnimationUtils`) at construction time:

```java
CharacterFactory factory = new CharacterFactory(AnimationUtils.getInstance());
Arthur arthur = factory.createArthur(WORLD_WIDTH);
```

### 3. Move the state machine


### 4. Move constants and fields


### 5. Move logic methods

  1. Read input (`Gdx.input` directly — no abstraction unless multiple input sources exist)
  2. Update physics (velocity, gravity, ground detection, clamping)
  3. Update state machine transitions
  4. Update scroll/camera offset
  5. Update effects (lighting, particles)
  6. Advance animation timer

### 6. Move rendering methods


### 7. Move texture loading and processing


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

If multiple entities affect scrolling, extract scroll into a separate `Camera`/`World` class.

## What stays in the game class


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
# Pattern: Extract Character Entity from Monolithic Game Class

## Purpose

Guide for extracting a player/NPC character from a LibGDX `ApplicationAdapter` subclass into a
self-contained domain object. The game class becomes a slim orchestrator (camera, scene, draw order)
while the character owns its state, physics, input, animation, lighting, and rendering.

This pattern has been **fully applied** in this project. The reference implementation is:
- Abstract base: `com.davidpe.ghosts.domain.characters.Character`
- Concrete player: `com.davidpe.ghosts.domain.characters.Arthur`
- Animation utility: `com.davidpe.ghosts.domain.utils.AnimationUtils`
- Factory: `com.davidpe.ghosts.application.factories.CharacterFactory`

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

### 2. Create an abstract base class

Before moving character logic, create an abstract `Character` base class that provides:

- **Protected fields**: `x`, `y`, `velocityX`, `velocityY`, `facingRight`, `drawWidth`,
  `stateTime`, `worldWidth`, `renderFrame`
- **`List<Texture> ownedTextures`**: all subclass textures registered here; base `dispose()`
  releases them all automatically
- **`update(float delta)`**: calls `updateBehavior(delta)` then advances `stateTime`
- **`draw(SpriteBatch batch)`**: flip-aware render using `getCurrentFrame()` and `getDrawHeight()`
- **`dispose()`**: iterates and disposes `ownedTextures`
- **`resetStateTime()`**: encapsulates `stateTime = 0f`; always call this when changing state
- **`moveTowards(current, target, maxDelta)`**: smooth acceleration/deceleration utility
- **Abstract hooks** the subclass must implement:

```java
protected abstract void updateBehavior(float delta);  // input, physics, state transitions
protected abstract TextureRegion getCurrentFrame();   // current animation frame
protected abstract float getDrawHeight();             // sprite height in world units
```

### 3. Define the public API of the concrete character

Keep it minimal:

```java
Arthur(float worldWidth, AnimationUtils animationUtils)  // DI: inject shared services
void drawEffects(SpriteBatch batch)   // render light/particles (separate for z-order control)
float getWorldOffsetX()               // expose scroll state if character drives camera
```

The base class already covers `update`, `draw`, `dispose`.

**Factory principle**: never instantiate a character directly from the game class. Wire
dependencies in a `CharacterFactory`:

```java
CharacterFactory factory = new CharacterFactory(AnimationUtils.getInstance());
Arthur arthur = factory.createArthur(WORLD_WIDTH);
```

### 4. Move the state machine

- Define the movement/state enum as a **private inner enum** inside the concrete character class
- Add as many states as needed — this project uses 6: `IDLE`, `WALK`, `CROUCH`, `CROUCH_UP`, `JUMP`, `PUNCH`
- No external code should ever read or switch on the character's internal state

### 5. Move constants and fields

- Move all character constants as `private static final` in the concrete character class
- Common state lives as `protected` fields in the **abstract base class**; subclass-specific state stays `private`
- The base constructor takes `worldWidth`; concrete constructors add injected services (e.g., `AnimationUtils`)

### 6. Move logic methods

**Override `updateBehavior(float delta)` — not `update()`**. It should:
1. Read input (`Gdx.input` directly — no abstraction unless you need replays or AI)
2. Update physics (velocity, gravity, ground detection, clamping)
3. Update state machine transitions — call `resetStateTime()` on each transition
4. Update scroll/camera offset
5. Update effects (lighting, particles)

Move generic helpers to the base class (`moveTowards`, `resetStateTime`). Keep character-specific
helpers `private` in the concrete class.

### 7. Move rendering methods

- `draw(SpriteBatch batch)`: provided by base class — no override needed unless custom layout
- `drawEffects(SpriteBatch batch)`: draw light halos, particles, etc. — concrete method, receives batch as parameter
- Both receive `SpriteBatch` as parameter — the character never owns the batch
- Save and restore batch color if the character modifies it

### 8. Move texture loading and processing

- Extract **generic animation loading** (JSON bounding-box parsing, sub-range building) into a
  dedicated `AnimationUtils` class; inject it into the character constructor
- Character-specific texture generation (e.g., procedural light halo) stays as `private` method
  in the concrete character class
- Every texture the character creates must be registered in `ownedTextures`. Use a helper:

```java
private Texture loadSheet(String path) {
    Texture sheet = new Texture(Gdx.files.internal(path));
    sheet.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    ownedTextures.add(sheet);
    return sheet;
}
```

### 9. Lifecycle integration

In the game class:

```java
// create()
CharacterFactory factory = new CharacterFactory(AnimationUtils.getInstance());
arthur = factory.createArthur(WORLD_WIDTH);

// render()
arthur.update(delta);
batch.begin();
drawScrollingBackgrounds();        // uses arthur.getWorldOffsetX()
drawBackgroundDim();               // scene overlay
arthur.drawEffects(batch);        // light behind sprite
arthur.draw(batch);               // sprite on top
batch.end();

// dispose()
arthur.dispose();                 // releases all ownedTextures via Character.dispose()
```

### 10. Scroll ownership rule

If the character is the **sole driver** of world scrolling (typical for single-player side-scrollers):
- Scroll state (`worldOffsetX`, `scrollVelocityX`) lives in the character
- Exposed via `getWorldOffsetX()` for background parallax
- Camera comfort window logic stays in the character's `updateBehavior()`

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

1. **Build passes** — `mvn compile` with no errors
2. **Tests pass** — no regression
3. **Visual parity** — all character behaviors identical: movement, animation, direction flip, jump arc, lighting, scroll
4. **Clean separation** — game class has zero character-specific constants, fields, or logic methods
5. **No shared mutable state** — character and game class communicate only through the public API
6. **Factory used** — character is never instantiated directly from `GhostsGame`
7. **ownedTextures complete** — every texture created in the character is registered; no manual `dispose()` calls needed in subclass

## Common pitfalls

| Pitfall | Fix |
|---|---|
| Leaking `SpriteBatch` ownership into character | Pass `batch` as method parameter, never store it |
| Character needs `WORLD_WIDTH` for clamping | Pass in constructor; base class stores it as `protected final worldWidth` |
| Draw order breaks after extraction | Keep `drawEffects()` and `draw()` as separate calls; game class controls order |
| Animation timer updated in wrong place | Base `update()` advances `stateTime` after `updateBehavior()` — never advance it again in the subclass |
| Setting `stateTime = 0f` directly | Always call `resetStateTime()` when transitioning states |
| Forgetting to register a texture | Add every created texture to `ownedTextures`; base `dispose()` handles the rest |
| Instantiating character directly in game class | Use `CharacterFactory` to keep dependency wiring centralized |
| Putting animation-loading code in the character | Move generic JSON/spritesheet parsing to `AnimationUtils`; keep character-specific texture generation private |
| Over-abstracting input | Let character read `Gdx.input` directly unless you need replays or AI control |
