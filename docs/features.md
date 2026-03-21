# Ghosts 'n Goblins — Features Log

## Arquitectura del proyecto

- **Framework:** LibGDX 1.12.1 (core + lwjgl3 backend)
- **Build:** Maven con plugins `exec-maven-plugin` (exec:exec) y `maven-shade-plugin`
- **Java:** 17+
- **Resolución virtual:** 800×600 con `FitViewport` + `OrthographicCamera` (mantiene proporción al redimensionar)
- **macOS:** Requiere `-XstartOnFirstThread` (configurado en exec-maven-plugin como proceso externo)

---

## Features implementadas

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
