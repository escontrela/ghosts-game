# Ghosts 'n Goblins — Features Log

## Arquitectura del proyecto

- **Framework:** LibGDX 1.12.1 (core + lwjgl3 backend)
- **Build:** Maven con plugins `exec-maven-plugin` (exec:exec) y `maven-shade-plugin`
- **Java:** 17+
- **Resolución virtual:** 800×600 con `FitViewport` + `OrthographicCamera` (mantiene proporción al redimensionar)
- **macOS:** Requiere `-XstartOnFirstThread` (configurado en exec-maven-plugin como proceso externo)

---

## Features implementadas

### 2026-03-21 — GHOST-0001 Control de Arthur (izquierda/derecha/agacharse/salto)

- **Movimiento horizontal:** flechas izquierda/derecha y teclas `A/D`.
- **Agacharse:** `abajo` o `S`; bloquea desplazamiento horizontal mientras está activo.
- **Salto:** `SPACE`, `UP` o `W` disparan arco de salto con gravedad y retorno al suelo.
- **Integración:** control implementado dentro de la estructura existente (`GhostsGame`) sin clases adicionales.

### 2026-03-21 — GHOST-0002 Máquina de estados mínima de movimiento

- **Estados definidos:** `IDLE`, `WALK`, `CROUCH`, `JUMP` en `GhostsGame`.
- **Integración en update loop:** La resolución de estado se ejecuta por frame con `delta`.
- **Prioridad de input:** `JUMP` (cuando está en aire o se dispara salto) > `CROUCH` (si está en suelo y se mantiene abajo) > `WALK` (input horizontal exclusivo) > `IDLE`.
- **Transiciones válidas:** desde suelo se puede pasar a `WALK`/`CROUCH`/`JUMP`; en aire se mantiene `JUMP` hasta aterrizar; al aterrizar vuelve a estados de suelo según input activo.
- **Implementación:** reutilización de clase existente sin crear nuevas clases auxiliares.

### 2026-03-21 — GHOST-0003 Animaciones + flip de Arthur

- **Animación por estado:** selección de frame basada en `IDLE` / `WALK` / `CROUCH` / `JUMP`.
- **Walk anim:** `Animation<TextureRegion>` activa únicamente cuando hay desplazamiento horizontal real.
- **Flip horizontal:** actualización de dirección (`facingRight`) y `flipX` consistente por frame, sin escalar en negativo ni distorsionar dimensiones.
- **Reutilización:** se mantiene el pipeline actual de carga/render y se amplía la lógica dentro de `GhostsGame`.

### 2026-03-21 — GHOST-0004 Scroll continuo con dos fondos

- **Fondos usados en secuencia:** `main-backgroud-1.png` + `main-background-2.png`.
- **Respuesta al avance de Arthur:** `worldOffsetX` se actualiza con el desplazamiento horizontal real de Arthur (`deltaX`).
- **Reciclado continuo:** wrap modular por ciclo de dos segmentos y redibujado de `N+1` fondos para cubrir viewport sin cortes al recolocar.
- **Estructura:** implementado en la clase existente con método dedicado `drawScrollingBackgrounds()`.

### 2026-03-21 — GHOST-0005 Luz tenue sobre Arthur

- **Efecto de legibilidad:** máscara radial suave (`Texture` generada con `Pixmap`) dibujada sobre el entorno y centrada en Arthur.
- **Seguimiento en tiempo real:** la luz usa la posición actual de Arthur en cada frame.
- **Intensidad controlada:** alpha moderada (`0.23`) para mejorar contraste sin sobreexponer fondo.
- **Complejidad mínima:** integración directa en pipeline de render actual sin sistema global de iluminación.

### 2026-03-21 — GHOST-0007 Balance de luz focal y fondo más oscuro

- **Luz focal más visible:** incremento suave de alpha de luz centrada en Arthur (`0.28`) para mejorar lectura del personaje.
- **Oscurecimiento moderado del entorno:** overlay negro global con alpha bajo (`0.16`) para separar personaje y fondo nocturno.
- **Sin sobreexposición:** ajuste conservador en un único paso de render para mantener detalle del escenario.
- **Reutilización de pipeline:** integración en `GhostsGame` con texturas simples generadas por `Pixmap`, sin nueva arquitectura de iluminación.

### 2026-03-21 — GHOST-0008 Afinado de scroll para continuidad visual

- **Continuidad de alternancia:** se mantiene el wrap modular de dos fondos y el dibujado de `N+1` segmentos para cobertura total del viewport.
- **Cambio de dirección sin jitter:** el scroll usa velocidad interpolada (`lerp`) hacia la dirección objetivo en lugar de saltos instantáneos.
- **Estabilidad al frenar:** cuando no hay desplazamiento, la velocidad converge suavemente a cero para evitar microvibraciones.
- **Implementación incremental:** cambios aplicados sobre la lógica existente de `GhostsGame`, sin nuevas capas ni sistemas de streaming.

### 2026-03-21 — GHOST-0009 Pulido fino de movimiento e input

- **Respuesta de movimiento ajustada:** actualización de velocidad horizontal con aceleración/frenado en suelo para mejorar sensación de control.
- **Consistencia de `CROUCH`:** al agacharse en suelo se anula velocidad horizontal para evitar artefactos de transición con `WALK`/`IDLE`.
- **Transiciones más limpias:** el estado `WALK` depende de velocidad real, reduciendo parpadeos por cambios rápidos de input.
- **Salto estable con repetición de input:** se conserva disparo de salto solo en suelo, manteniendo arco y aterrizaje consistentes.

### 2026-03-21 — Ventana principal con fondo y Arthur

- **Ventana de juego:** 800×600, título "Ghosts 'n Goblins", VSync 60 FPS.
- **Fondo de cementerio:** `main-backgroud-1.png` (1536×1024) escalado al viewport completo.
- **Arthur (sprite):** Primer frame extraído del spritesheet `sprites-arthur.png` (1536×1024, grid 8×5) con `TextureRegion`.
- **Transparencia del sprite:** El spritesheet tiene fondo negro opaco. Se procesa al cargar con `Pixmap` (blending desactivado) para convertir píxeles casi-negros (RGB < 30) a transparentes.
- **Posición:** Arthur centrado horizontalmente, posicionado sobre el camino del escenario (Y=130).
- **Escalado:** Arthur dibujado a 120px de alto manteniendo aspect ratio.

---

## Recursos

| Archivo | Tipo | Tamaño |
|---|---|---|
| `main-backgroud-1.png` | Fondo cementerio | 1536×1024 |
| `main-background-2.png` | Fondo cementerio (2) | 1536×1024 |
| `sprites-arthur.png` | Spritesheet Arthur | 1536×1024 (8×5 frames) | 

---

## Pendiente (según AGENTS.md)

- [ ] Movimiento izquierda/derecha con teclado
- [ ] Animación de caminar (recorrer frames del spritesheet)
- [ ] Scroll lateral del fondo con las 2 imágenes de background
- [ ] Loop continuo del escenario
- [ ] Flip automático del personaje según dirección
- [ ] Parallax (opcional)

---

## Backlog Tasker fase 1 (2026-03-21)

En esta fase se prioriza exclusivamente: control de Arthur (izquierda/derecha/agacharse/salto), scroll continuo con los dos fondos actuales y luz tenue de realce sobre Arthur.

Regla técnica de implementación para el equipo:

- Reutilizar la estructura de código existente antes de crear clases nuevas.
- Evitar fragmentación en múltiples clases pequeñas sin responsabilidad clara.
- Mantener tickets pequeños, verticales y verificables en build jugable.

Tickets creados en Tasker (projectId=6):

- `GHOST-0000` (BACKLOG): Bootstrap de fase 1 de control y scroll.
- `GHOST-0001` (IN_PROGRESS): Control de Arthur (izquierda/derecha/agacharse/salto).
- `GHOST-0002` (BACKLOG): Máquina de estados mínima de movimiento.
- `GHOST-0003` (BACKLOG): Animaciones y flip de sprite de Arthur.
- `GHOST-0004` (BACKLOG): Scroll continuo con dos fondos del escenario.
- `GHOST-0005` (BACKLOG): Luz tenue sobre Arthur para legibilidad.

---

## Iteración de planificación PO — 2026-03-21 (fase 1 enfocada)

Contexto de foco activo para las siguientes entregas:

- Control de Arthur con teclado: izquierda, derecha, agacharse y salto.
- Scroll continuo con los 2 fondos disponibles.
- Mejora de legibilidad visual: más contraste personaje/fondo.
- Pulido de sprite para eliminar bordes con bleed de frames vecinos.

Directriz técnica para desarrollo (obligatoria en esta fase):

- Priorizar reutilización de la estructura actual.
- Evitar crear muchas clases pequeñas sin responsabilidad clara.
- Mantener cambios verticales, verificables y de alcance reducido por ticket.

### Estado Tasker validado (projectId=6, userId=1)

- `DONE`: `GHOST-0001`, `GHOST-0002`, `GHOST-0003`, `GHOST-0004`, `GHOST-0005`.
- `IN_PROGRESS` (WIP=1): `GHOST-0006`.
- `BACKLOG` (5 tickets): `GHOST-0000`, `GHOST-0007`, `GHOST-0008`, `GHOST-0009`, `GHOST-0010`.

### Nuevos tickets creados para la fase 1

- `GHOST-0006` — Corrección de bordes del sprite de Arthur (`IN_PROGRESS`).
- `GHOST-0007` — Balance de luz focal y fondo más oscuro (`BACKLOG`).
- `GHOST-0008` — Afinado de scroll para continuidad visual (`BACKLOG`).
- `GHOST-0009` — Pulido fino de movimiento y respuesta de input (`BACKLOG`).
- `GHOST-0010` — Checklist técnico de regresión para fase 1 (`BACKLOG`).

---

## Iteración PO autónoma — 2026-03-21T03:01:54Z (fase 1 control/scroll/luz)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `features-nightly-20260321`.

### Estado actual validado

- `DONE`: `GHOST-0001`, `GHOST-0002`, `GHOST-0003`, `GHOST-0004`, `GHOST-0005`.
- `IN_PROGRESS` (`WIP=1`): `GHOST-0006`.
- `BACKLOG` (5): `GHOST-0000`, `GHOST-0007`, `GHOST-0008`, `GHOST-0009`, `GHOST-0010`.

### Foco activo obligatorio de fase 1

- Perfeccionar control de Arthur: izquierda, derecha, agacharse y salto.
- Pulir continuidad del scroll usando los dos fondos actuales.
- Reforzar legibilidad: más presencia de luz sobre Arthur y fondo algo más oscuro.
- Corregir bleed leve en bordes del sprite para evitar ver parte de frames vecinos.

### Directriz técnica al equipo

- Reutilizar estructura actual y ampliar sobre `GhostsGame` antes de crear nuevas clases.
- Evitar proliferación de clases pequeñas sin responsabilidad clara.
- Mantener tickets pequeños, verticales, verificables y centrados en mejora concreta.

### Verificación técnica de esta iteración

- Build local validada con `mvn -q -DskipTests compile` (OK).
