# GHOSTS GAME — Arquitectura y Referencia para Agentes

Referencia técnica para agentes de IA que implementen tickets en este proyecto.
Lee este documento antes de tocar cualquier clase existente.

---

## Stack técnico

| Ítem | Valor |
|---|---|
| Lenguaje | Java 17 |
| Framework | LibGDX 1.12.1 |
| Backend desktop | LWJGL3 (`gdx-backend-lwjgl3`) |
| Build | Maven 3.x |
| Resolución virtual | 800 × 600 (`FitViewport` + `OrthographicCamera`) |
| macOS | Requiere `-XstartOnFirstThread` (configurado en `exec-maven-plugin`) |
| Comando de build | `mvn compile` |
| Comando de ejecución | `mvn compile exec:exec` |

---

## Estructura de paquetes

```
com.davidpe.ghosts
├── DesktopLauncher                    ← main(), configura Lwjgl3Application
├── application/
│   ├── GhostsGame                    ← ApplicationAdapter (orquestador de escena)
│   └── factories/
│       └── CharacterFactory          ← Crea personajes con dependencias inyectadas
└── domain/
    ├── characters/
    │   ├── Character                 ← Clase base abstracta para todos los personajes
    │   └── Arthur                    ← Personaje jugador (extiende Character)
    └── utils/
        └── AnimationUtils            ← Singleton: carga animaciones desde JSON de bounding boxes
```

---

## Regla de responsabilidades

| Clase | Responsabilidad |
|---|---|
| `DesktopLauncher` | Punto de entrada, configura ventana y FPS, instancia `GhostsGame` |
| `GhostsGame` | Ciclo LibGDX (`create/render/resize/dispose`), camara, viewport, batch, fondos, overlay, orden de dibujado |
| `CharacterFactory` | Conecta personajes con sus dependencias (inyecta `AnimationUtils`) |
| `Character` | Estado común: posición, velocidad, dirección, timer de animación, dibujado con flip, disposal de texturas |
| `Arthur` | Máquina de estados de 6 estados, física de salto/caída, input de teclado, scroll de cámara, luz focal dinámica |
| `AnimationUtils` | Parsea JSON de bounding boxes, crea `Animation<TextureRegion>` y sub-rangos |

---

## Clase base `Character`

Todos los personajes extienden `Character`. La clase base provee:

- Campos protegidos: `x`, `y`, `velocityX`, `velocityY`, `facingRight`, `drawWidth`, `stateTime`, `worldWidth`, `renderFrame`
- Lista `ownedTextures` (tipo `List<Texture>`): registrar aquí todas las texturas creadas para que `dispose()` las libere automáticamente
- `update(float delta)`: llama a `updateBehavior(delta)` y luego avanza `stateTime`
- `draw(SpriteBatch batch)`: dibuja `getCurrentFrame()` con flip horizontal según `facingRight`
- `dispose()`: itera `ownedTextures` y libera todas
- Método utilitario protegido `moveTowards(current, target, maxDelta)`: acelera/frena sin sobrepasar objetivo
- Método protegido `resetStateTime()`: resetea `stateTime` a 0 al cambiar de estado

**Hooks abstractos que toda subclase debe implementar:**

```java
protected abstract void updateBehavior(float delta);   // input / física / máquina de estados
protected abstract TextureRegion getCurrentFrame();     // frame actual de animación
protected abstract float getDrawHeight();               // altura en unidades de mundo
```

---

## Clase `Arthur`

Personaje jugador. Constructor: `Arthur(float worldWidth, AnimationUtils animationUtils)`.

### Máquina de estados (`MovementState` — enum privado interno)

| Estado | Descripción | Loop |
|---|---|---|
| `IDLE` | Quieto en suelo | sí |
| `WALK` | Caminando (velocityX > 8) | sí |
| `CROUCH` | Agachado (manteniendo DOWN/S) | no |
| `CROUCH_UP` | Animación de levantarse | no |
| `JUMP` | En el aire | sí |
| `PUNCH` | Golpe (one-shot, bloquea otros inputs) | no |

### Constantes clave de física

| Constante | Valor | Uso |
|---|---|---|
| `GROUND_Y` | 130f | Y mínima (suelo) |
| `MOVE_SPEED` | 235f | Velocidad máxima horizontal |
| `JUMP_VELOCITY` | 520f | Impulso vertical inicial del salto |
| `JUMP_RISE_GRAVITY` | 1180f | Gravedad mientras sube |
| `JUMP_FALL_GRAVITY` | 1030f | Gravedad mientras baja |
| `LANDING_SOFT_ZONE` | 42f | Zona cerca del suelo donde se reduce la gravedad |
| `CAMERA_COMFORT_LEFT` | 320f | Borde izquierdo de la zona de confort de cámara |
| `CAMERA_COMFORT_RIGHT` | 480f | Borde derecho de la zona de confort de cámara |

### API pública adicional de Arthur

```java
void drawEffects(SpriteBatch batch)   // dibuja halo de luz focal alrededor del torso
float getWorldOffsetX()               // offset acumulado del mundo (lo usa GhostsGame para scroll de fondos)
```

### Carga de animaciones

Cada estado tiene su propio sprite sheet. Los frames se definen en archivos JSON de bounding boxes bajo `src/main/resources/arthur/`.

| Recurso | Archivo |
|---|---|
| Sprite idle | `arthur/sprite-sheet-arthur-idle.png` |
| Sprite walk | `arthur/sprite-sheet-arthur-walk.png` |
| Sprite jump | `arthur/sprite-sheet-arthur-jump.png` |
| Sprite punch | `arthur/sprite-sheet-arthur-punch.png` |
| Sprite crouch | `arthur/sprite-sheet-arthur-crouching.png` |
| JSON idle | `arthur/bounding-boxes-arthur-idle.json` |
| JSON walk | `arthur/bouding-boxes-arthur-walk.json` ← typo en nombre de archivo, no corregir |
| JSON jump | `arthur/bounding-boxes-arthur-jump.json` |
| JSON punch | `arthur/bounding-boxes-arthur-punch.json` |
| JSON crouch | `arthur/bounding-boxes-arthur-crouching.json` |

El sprite de crouch es un solo sheet partido en dos animaciones:
- frames 0..19 → `crouchDownAnimation` (agacharse)
- frames 16..fin → `crouchUpAnimation` (levantarse, con 4 frames de overlap)

Todos los sheets usan `TextureFilter.Nearest` para bordes nítidos en pixel art.

---

## `AnimationUtils` (singleton)

Obtener la instancia: `AnimationUtils.getInstance()`.

```java
Animation<TextureRegion> buildAnimationFromBoundingBoxes(Texture sheet, String jsonPath, float frameDuration)
TextureRegion[] loadFramesFromBoundingBoxes(Texture sheet, String jsonPath)
Animation<TextureRegion> buildAnimationFromRange(TextureRegion[] allFrames, int from, int to, float frameDuration)
```

Formato del JSON de bounding boxes (array de objetos):

```json
[
  { "x": 0, "y": 0, "width": 48, "height": 64 },
  { "x": 48, "y": 0, "width": 48, "height": 64 }
]
```

---

## `CharacterFactory`

Crear un Arthur con sus dependencias ya conectadas:

```java
CharacterFactory factory = new CharacterFactory(AnimationUtils.getInstance());
Arthur arthur = factory.createArthur(WORLD_WIDTH);
```

Toda creación de personajes debe pasar por `CharacterFactory`, no instanciar directamente desde `GhostsGame`.

---

## `GhostsGame` — ciclo de render

```java
// create()
CharacterFactory factory = new CharacterFactory(AnimationUtils.getInstance());
arthur = factory.createArthur(WORLD_WIDTH);

// render() — orden de dibujado obligatorio
arthur.update(delta);
batch.begin();
  drawScrollingBackgrounds();   // usa arthur.getWorldOffsetX()
  drawBackgroundDim();          // overlay negro semitransparente
  arthur.drawEffects(batch);    // luz focal (detrás del sprite)
  arthur.draw(batch);           // sprite del personaje (encima)
batch.end();

// dispose()
arthur.dispose();               // libera ownedTextures
```

Resolución virtual constante: `WORLD_WIDTH = 800`, `WORLD_HEIGHT = 600` (campos `public static final float`).

---

## Convenciones de código

- Todas las constantes de clase son `private static final`.
- Las texturas creadas dentro de `Arthur` (o cualquier subclase de `Character`) se registran en `ownedTextures` para disposal automático.
- Al cambiar de estado en `Arthur`, siempre llamar `resetStateTime()` (no `stateTime = 0f` directamente).
- El `SpriteBatch` jamás se almacena como campo de un personaje; siempre se recibe como parámetro.
- Si un método modifica el color del batch (`batch.setColor(...)`), debe restaurarlo antes de retornar.
- Nuevas clases de personaje: extender `Character`, implementar los tres hooks abstractos, registrar texturas en `ownedTextures`, crearse vía `CharacterFactory`.

---

## Cómo añadir un nuevo personaje (enemy, NPC…)

1. Crear `MiPersonaje extends Character` en `com.davidpe.ghosts.domain.characters`.
2. Implementar `updateBehavior(float delta)`, `getCurrentFrame()`, `getDrawHeight()`.
3. Registrar todas las texturas en `ownedTextures` durante el constructor.
4. Añadir `createMiPersonaje(...)` en `CharacterFactory`.
5. Instanciar desde `GhostsGame.create()` y llamar `update/draw/dispose` en el ciclo.

---

## Fondos de scroll

Hay exactamente 2 fondos definidos en `GhostsGame`:

- `main-backgroud-1.png` ← typo en nombre de archivo, no corregir
- `main-background-2.png`

El scroll usa wrap modular: el offset acumulado (`arthur.getWorldOffsetX()`) se convierte en un índice de segmento y un suboffset. Se dibujan `N+1` repeticiones para garantizar cobertura completa del viewport sin huecos.