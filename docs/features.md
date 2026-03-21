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

### 2026-03-21 — GHOST-0006 Corrección de bordes del sprite de Arthur

- **Recorte seguro por frame:** cada `TextureRegion` de `IDLE`, `WALK`, `CROUCH` y `JUMP` aplica inset de 1px para evitar muestreo de píxeles vecinos del spritesheet.
- **Filtro de sprite estable:** `sprites_arthur.png` usa `TextureFilter.Nearest` para mantener bordes limpios en escalado y animación.
- **Render alineado a píxel:** el dibujado de Arthur se redondea a coordenadas enteras para reducir artefactos en contorno al desplazarse y al hacer flip.
- **Sin cambios de arquitectura:** ajuste aplicado en `GhostsGame` reutilizando clases actuales.

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

### 2026-03-21 — GHOST-0010 Checklist técnico de regresión (fase 1)

Checklist manual rápido (3-5 minutos) para control, scroll, luz y sprite:

1. **Movimiento horizontal:** mantener `LEFT/RIGHT` (`A/D`) durante 5 segundos por dirección y validar desplazamiento continuo sin bloqueos.
2. **Cambio de dirección:** alternar izquierda/derecha en pulsaciones cortas y confirmar ausencia de jitter visible en scroll.
3. **Crouch estable:** en suelo, mantener `DOWN/S`; verificar que Arthur no deriva en X y que transiciona limpio con `IDLE/WALK`.
4. **Salto repetido:** ejecutar secuencia `jump -> aterrizaje -> jump` varias veces; validar arco consistente y aterrizaje en `GROUND_Y`.
5. **Cobertura de fondo:** desplazarse en ambos sentidos y confirmar que no aparecen huecos al alternar `main-backgroud-1.png` y `main-background-2.png`.
6. **Contraste visual:** validar que Arthur destaca sobre el fondo oscuro sin sobreexposición durante `IDLE/WALK/JUMP/CROUCH`.
7. **Bordes de sprite en animación:** observar contornos de Arthur mientras camina para detectar bleed/halo.
8. **Bordes de sprite en flip:** invertir dirección de forma repetida y comprobar que no aparecen artefactos en bordes ni deformación del frame.

Regla técnica de fase 1 reforzada por checklist:

- Mantener cambios en la estructura existente (`GhostsGame`) antes de crear nuevas clases.
- Evitar sobre-ingeniería y validar regresión con este checklist tras cada ajuste visual/control.

### 2026-03-21 — GHOST-0011 Ajuste fino de salto y aterrizaje de Arthur

- **Aterrizaje menos brusco:** el descenso usa suavizado al entrar en una zona cercana al suelo para reducir golpe visual al tocar `GROUND_Y`.
- **Curva de salto más predecible:** separación de gravedad de subida y bajada para mantener altura/duración consistentes en repeticiones.
- **Control de caída:** límite de velocidad vertical negativa para evitar picos de caída y mejorar lectura del aterrizaje.
- **Sin cambios de arquitectura:** ajuste aplicado sobre `GhostsGame` y máquina de estados existente (`IDLE`, `WALK`, `CROUCH`, `JUMP`).

### 2026-03-21 — GHOST-0012 Estabilidad visual de pose al agacharse

- **Huella visual estable:** Arthur usa ancho de render fijo en `IDLE`, `WALK`, `CROUCH` y `JUMP`, evitando saltos por diferencias de ancho entre frames.
- **Base consistente al agacharse:** la pose mantiene alineación de suelo durante transición entrar/salir de `CROUCH`.
- **Sin jitter en alternancia rápida:** cambios izquierda/derecha + agacharse no desplazan el sprite por recalcular ancho por frame.
- **Sin impacto en input:** se conserva la lógica de control y estados existente sin introducir latencia.

### 2026-03-21 — GHOST-0013 Scroll acoplado al avance con suavidad bidireccional

- **Acople directo al movimiento:** el objetivo de scroll usa velocidad horizontal real de Arthur para mantener seguimiento en avance y retroceso.
- **Menos latencia perceptible:** interpolación con factor dependiente de `delta` para respuesta estable entre distintos FPS.
- **Cambio de dirección más limpio:** snap corto al objetivo cuando la diferencia es mínima, eliminando tirones residuales al invertir dirección.
- **Cobertura preservada:** se mantiene la lógica existente de wrap modular y dibujo de `N+1` fondos.

### 2026-03-21 — GHOST-0014 Rebalanceo de halo de luz y atenuación del fondo

- **Mayor separación personaje-escena:** incremento moderado de atenuación global del fondo para destacar a Arthur sin apagar el escenario.
- **Halo más amplio y suave:** aumento del tamaño del halo con menor intensidad pico para evitar sobreexposición.
- **Caída de luz recalibrada:** ajuste de color y curva radial para conservar detalle de fondo y mantener foco visual en Arthur.
- **Integración directa:** cambios aplicados en el pipeline actual de `GhostsGame` sin añadir nuevas clases de iluminación.

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

---

## Iteración PO autónoma — 2026-03-21T06:02:13Z (reposición backlog fase 1 tras cierre de 0011-0014)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `features-nightly-20260321`.

### Estado actual validado

- `DONE`: `GHOST-0001`, `GHOST-0002`, `GHOST-0003`, `GHOST-0004`, `GHOST-0005`, `GHOST-0007`, `GHOST-0008`, `GHOST-0009`, `GHOST-0010`, `GHOST-0011`, `GHOST-0012`, `GHOST-0013`, `GHOST-0014`.
- `IN_PROGRESS` (`WIP=1`): `GHOST-0006`.
- `BACKLOG` (5): `GHOST-0000`, `GHOST-0015`, `GHOST-0016`, `GHOST-0017`, `GHOST-0018`.

### Tickets nuevos creados en esta iteración

- `GHOST-0015` — Ajuste de ventana de cámara para scroll más legible (`BACKLOG`).
- `GHOST-0016` — Consistencia de salto al encadenar inputs rápidos (`BACKLOG`).
- `GHOST-0017` — Calibración fina de halo según estado de movimiento (`BACKLOG`).
- `GHOST-0018` — Checklist de validación visual para scroll y luz (`BACKLOG`).

### Directriz técnica al equipo

- Reutilizar estructura y clases existentes antes de crear nuevas piezas.
- Evitar proliferación de clases pequeñas sin responsabilidad clara.
- Mantener tickets pequeños, verticales y verificables en build jugable.

### Foco activo de desarrollo (fase 1)

- Control de Arthur: izquierda, derecha, agacharse y salto.
- Scroll continuo estable con los dos fondos actuales.
- Luz de realce de Arthur y fondo moderadamente oscurecido para contraste.
- Corrección de bordes de sprite sin bleed de frames vecinos.

---

## Iteración PO autónoma — 2026-03-21T04:19:00Z (reposición de backlog fase 1)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `features-nightly-20260321`.

### Estado actual validado

- `DONE`: `GHOST-0001`, `GHOST-0002`, `GHOST-0003`, `GHOST-0004`, `GHOST-0005`, `GHOST-0007`, `GHOST-0008`, `GHOST-0009`, `GHOST-0010`.
- `IN_PROGRESS` (`WIP=1`): `GHOST-0006`.
- `BACKLOG` (5): `GHOST-0000`, `GHOST-0011`, `GHOST-0012`, `GHOST-0013`, `GHOST-0014`.

### Tickets nuevos creados en esta iteración

- `GHOST-0011` — Ajuste fino de salto y aterrizaje de Arthur (`BACKLOG`).
- `GHOST-0012` — Estabilidad visual de pose al agacharse (`BACKLOG`).
- `GHOST-0013` — Scroll acoplado al avance con suavidad bidireccional (`BACKLOG`).
- `GHOST-0014` — Rebalanceo de halo de luz y atenuación del fondo (`BACKLOG`).

### Aviso técnico al equipo de desarrollo

- Mantener foco exclusivo de fase 1: movimiento de Arthur, sprite, scroll con 2 fondos y luz de realce.
- Reutilizar estructura existente y evitar crear muchas clases pequeñas sin responsabilidad clara.
- Cada entrega debe ser concreta, pequeña y verificable en build jugable.

---

## Iteración PO autónoma — 2026-03-21T05:01:53Z (fase 1 control/sprite/scroll/luz)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `features-nightly-20260321`.

### Estado actual validado

- `DONE`: `GHOST-0001`, `GHOST-0002`, `GHOST-0003`, `GHOST-0004`, `GHOST-0005`, `GHOST-0007`, `GHOST-0008`, `GHOST-0009`, `GHOST-0010`.
- `IN_PROGRESS` (`WIP=1`): `GHOST-0006`.
- `BACKLOG` (5): `GHOST-0000`, `GHOST-0011`, `GHOST-0012`, `GHOST-0013`, `GHOST-0014`.

### Decisión de planificación

- No se crean tickets nuevos en esta corrida para evitar solapamiento con `GHOST-0006` y mantener granularidad pequeña.
- Se mantiene backlog mínimo de 5 tickets, todos dentro del alcance de fase 1.
- Se conserva `WIP=1` sin transiciones adicionales.

### Foco técnico obligatorio para desarrollo

- Priorizar control de Arthur: izquierda, derecha, agacharse y salto con respuesta precisa.
- Pulir recorte y bordes del sprite para evitar bleed de frames vecinos.
- Reforzar scroll continuo y estable con los dos fondos actuales.
- Mejorar contraste visual: luz sobre Arthur más efectiva y fondo moderadamente más oscuro.

### Directriz de arquitectura

- Reutilizar estructura y clases existentes antes de crear nuevas.
- Evitar proliferación de clases pequeñas sin responsabilidad clara.
- Mantener entregas concretas, testables y verticales por ticket.

### Verificación técnica de esta iteración

- Build local validada con `mvn -q -DskipTests compile` (OK).

---

## Iteración PO autónoma — 2026-03-21T06:06:30Z (reposicionamiento backlog fase 1)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `features-nightly-20260321`.

### Estado actual validado

- `DONE`: `GHOST-0001`, `GHOST-0002`, `GHOST-0003`, `GHOST-0004`, `GHOST-0005`, `GHOST-0007`, `GHOST-0008`, `GHOST-0009`, `GHOST-0010`, `GHOST-0011`, `GHOST-0012`, `GHOST-0013`, `GHOST-0014`.
- `IN_PROGRESS` (`WIP=1`): `GHOST-0006`.
- `BACKLOG` (5): `GHOST-0000`, `GHOST-0015`, `GHOST-0016`, `GHOST-0017`, `GHOST-0018`.

### Tickets creados para mantener backlog mínimo

- `GHOST-0015` — Ajuste de ventana de cámara para scroll más legible (`BACKLOG`).
- `GHOST-0016` — Consistencia de salto al encadenar inputs rápidos (`BACKLOG`).
- `GHOST-0017` — Calibración fina de halo según estado de movimiento (`BACKLOG`).
- `GHOST-0018` — Checklist de validación visual para scroll y luz (`BACKLOG`).

### Directriz técnica al equipo

- Reutilizar clases existentes y evitar proliferación de clases pequeñas sin responsabilidad clara.
- Mantener tickets pequeños, verticales y verificables sobre build jugable.
- Sostener foco de fase 1: control de Arthur, scroll de dos fondos, contraste luz/fondo y limpieza de bordes de sprite.
