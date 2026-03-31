# Ghosts 'n Goblins — Features Log

## Arquitectura del proyecto

- **Framework:** LibGDX 1.12.1 (core + lwjgl3 backend)
- **Build:** Maven con plugins `exec-maven-plugin` (exec:exec) y `maven-shade-plugin`
- **Java:** 17+
- **Resolución virtual:** 800×600 con `FitViewport` + `OrthographicCamera` (mantiene proporción al redimensionar)
- **macOS:** Requiere `-XstartOnFirstThread` (configurado en exec-maven-plugin como proceso externo)

---

## Features implementadas

### 2026-03-31 — GHOST-0000 Bootstrap de fase 1 (revalidación operativa en rama activa)

- **Rama activa validada para implementación:** `feature/zombie-enemies` (política vigente del repositorio para este ciclo).
- **Alcance base de fase ratificado:** control de Arthur (izquierda/derecha/agacharse/salto), scroll de dos fondos y luz tenue focal.
- **Regla de reutilización reafirmada:** extender clases existentes y evitar proliferación innecesaria de clases.
- **Tratamiento del ticket en Tasker:** cierre en `done` para evitar duplicidad operativa, manteniendo `GHOST-0000` como referencia documental en este log.

### 2026-03-31 — GHOST-0044 Contador de impactos de Zombie con umbral 3

- **Contador acumulado en dominio:** `Zombie` incorpora `accumulatedHits` y suma 1 por cada golpe válido aceptado en estado `WALK`.
- **Umbral de derrota explícito:** el límite queda fijado en `3` mediante constante de dominio (`DEFEAT_HIT_THRESHOLD`).
- **Reset por nuevo ciclo de spawn:** el contador vuelve a `0` al iniciar `startGroundRiseAt(...)`.
- **Integración sin nuevas capas:** `GhostsGame` registra golpes válidos de `SPACE` cuando el zombie está en rango de golpe de Arthur.

### 2026-03-31 — GHOST-0045 Reanudación de persecución tras HITTED con delay corto

- **Delay configurable tras impacto no letal:** `Zombie` añade `hittedRecoveryDelaySeconds` (base `0.18s`) antes de volver a moverse.
- **Retorno automático a persecución si impactos < 3:** tras terminar `HITTED` y consumir el delay, la transición vuelve a `WALK`.
- **Target de Arthur preservado:** al recuperar `WALK`, el zombie continúa persiguiendo la X publicada por `GhostsGame`.
- **Integración mínima:** sin clases nuevas, todo resuelto dentro del flujo actual de `Zombie`.

### 2026-03-31 — GHOST-0000 Bootstrap de fase 1 de control y scroll (validación operativa)

- **Rama de trabajo validada para el ciclo actual:** desarrollo ejecutado en `feature/zombie-enemies` (política activa del repositorio).
- **Alcance ratificado de fase 1:** izquierda/derecha, agacharse, salto, scroll de dos fondos y luz tenue sobre Arthur.
- **Regla de implementación ratificada:** priorizar extensión de clases existentes y evitar proliferación innecesaria de clases.
- **Nota de referencia histórica:** la entrada original de 2026-03-21 mantiene el contexto inicial del bootstrap.
- **Cierre operacional Tasker:** este bootstrap pasa a `done` en Tasker para evitar duplicidad de backlog, conservando su valor documental como referencia de fase.

### 2026-03-31 — GHOST-0033 Ciclo de vida temporal del Zombie (spawn -> walk -> ground hide)

- **Duración activa configurable de `WALK`:** `Zombie` incorpora `activeWalkDurationSeconds` con valor inicial de fábrica `10s`.
- **Transición automática por tiempo:** al agotarse el temporizador de `WALK`, el estado cambia a `GROUND_HIDE` sin intervención externa.
- **Fin de ciclo utilizable para respawn:** al terminar la animación `GROUND_HIDE`, el zombie se marca como inactivo y publica `hideCycleCompleted`.
- **Responsabilidad contenida en dominio:** el ciclo temporal vive en `Zombie` y evita nuevas clases para cumplir alcance del ticket.

### 2026-03-31 — GHOST-0034 Spawn relativo a Arthur (delante/detrás) con límites de mundo

- **Resolución de spawn por modo:** la aparición del zombie se calcula desde la X de Arthur con dos variantes explícitas: `AHEAD` y `BEHIND`.
- **Clamp de mundo aplicado:** la X final del spawn se limita a `0..(WORLD_WIDTH - drawWidth)` para evitar nacimientos fuera del escenario.
- **Entrada de ciclo garantizada:** cada aparición inicia en `GROUND_RISE` mediante API dedicada de Zombie (`startGroundRiseAt`).
- **Integración sin romper scroll:** Arthur mantiene el control del offset global de fondo; el spawn del zombie no altera ese contrato.

### 2026-03-31 — GHOST-0035 IA simple de aparición y reaparición aleatoria del Zombie

- **Controlador de ciclo en aplicación:** `GhostsGame` incorpora un orquestador mínimo que activa/desactiva el ciclo de spawn del zombie.
- **Reaparición con rango configurable:** al completar `GROUND_HIDE`, el siguiente spawn se agenda con delay aleatorio entre `ZOMBIE_RESPAWN_DELAY_MIN_SECONDS` y `ZOMBIE_RESPAWN_DELAY_MAX_SECONDS`.
- **Un único enemigo activo:** se mantiene un solo `Zombie` instanciado y controlado por flags de ciclo para evitar multiplicidad.
- **Separación de responsabilidades:** la orquestación temporal vive en aplicación y el character expone señales de fin de ciclo (`consumeHideCycleCompleted`).

### 2026-03-31 — GHOST-0036 Persecución hacia Arthur desacoplada del driver de scroll

- **Persecución en `WALK`:** Zombie ajusta su dirección horizontal para converger hacia la X objetivo de Arthur.
- **Objetivo inyectado desde aplicación:** `GhostsGame` publica la X de Arthur al character enemigo mediante `setTargetX(...)`.
- **Scroll global preservado:** la cámara/fondo siguen usando `arthur.getWorldOffsetX()`; el zombie no modifica el offset de mundo.
- **Ciclo estable fuera de encuadre:** el update del zombie continúa por estado aunque su sprite no esté visible en viewport.

### 2026-03-31 — GHOST-0037 Integración final spawn/IA zombie + checklist manual bloque 2

- **Flujo runtime cerrado del bloque 2:** `GROUND_RISE -> WALK -> GROUND_HIDE -> respawn` con `WALK` finalizado por timeout configurable (base `10s`) y reaparición con delay aleatorio.
- **Spawn relativo mantenido en ambos lados:** cada ciclo selecciona `AHEAD/BEHIND` relativo a Arthur y reaplica clamp de límites de mundo.
- **Sin efecto de contacto en este bloque:** cuando zombie y Arthur se cruzan, no hay daño ni cambios de energía por diseño.

Checklist manual breve bloque 2 (3-5 minutos):

1. **Inicio de ciclo:** arrancar juego y validar que el zombie aparece en `GROUND_RISE`, pasa a `WALK` y orienta su movimiento hacia Arthur.
2. **Hide por timeout:** sin usar teclas de debug, esperar fin del timeout de `WALK` y confirmar transición automática a `GROUND_HIDE`.
3. **Respawn aleatorio:** tras terminar `GROUND_HIDE`, verificar que existe una espera variable antes del siguiente spawn.
4. **Lados de spawn:** iterar varios ciclos y confirmar apariciones por delante y por detrás de Arthur (usar `1/2` para forzar lado en validación dirigida si hace falta).
5. **Cruce sin impacto:** provocar cruce zombie-Arthur y validar explícitamente que no se aplica daño ni UI de energía en este bloque.

### 2026-03-31 — PO Iteración: planificación bloque 3/4 (energía de Arthur por contacto)

- **Verificación previa completada:** bloque 2 (`GHOST-0033..GHOST-0037`) validado como implementado en código y cerrado en Tasker antes de abrir nueva temática.
- **Secuencia creada para bloque 3 (energía):**
  - `GHOST-0038` detección de cruce Zombie-Arthur (WIP activo).
  - `GHOST-0039` modelo de energía de Arthur (`100 -> 0`) con drenado por contacto.
  - `GHOST-0040` HUD sutil de energía en esquina inferior derecha.
  - `GHOST-0041` regla visual de crítico: energía 0 en rojo sin muerte.
  - `GHOST-0042` integración end-to-end + checklist manual del bloque.
- **Restricciones de alcance aplicadas:** en este bloque no entra aún reacción del zombie al golpe de Arthur ni puntuación.
- **Directriz técnica para devs:** mantener estructura DDD dominio/aplicación actual, reutilizar clases existentes y evitar proliferación innecesaria de clases pequeñas.

### 2026-03-31 — PO Iteración: validación de avance bloque 4 (combate zombie por golpes)

- **Estado Tasker validado:** `WIP=1` se mantiene en `GHOST-0043`; backlog operativo en 5 tickets (`GHOST-0000`, `GHOST-0044..GHOST-0047`).
- **Comprobación de código local:** el estado `HITTED` del zombie existe, pero el disparo desde el golpe de Arthur aún está conectado por tecla debug (`H`) y no por rango de impacto de `PUNCH`.
- **Decisión de planificación:** no se crean tickets nuevos ni se transicionan estados hasta cerrar `GHOST-0043` para respetar la secuencia del plan y evitar solapamiento de alcance.
- **Recordatorio para devs:** preservar estructura DDD actual y reutilizar clases (`GhostsGame`, `Arthur`, `Zombie`) evitando proliferación de clases pequeñas.

### 2026-03-31 — GHOST-0038 Detección de contacto Zombie-Arthur desacoplada de combate

- **Señal determinista de contacto:** `Zombie` expone `isInContactWith(...)` con chequeo AABB usando posiciones y bounds de ambos personajes.
- **Evaluación restringida a `WALK`:** el contacto solo se considera activo cuando el zombie está caminando, sin depender de cámara ni scroll.
- **Exposición para capa de aplicación:** `GhostsGame` actualiza `zombieArthurContactActive` por frame para ser consumido por tickets de energía.
- **Sin efectos de combate en este ticket:** no se alteran estados de `HITTED`, no hay daño ni knockback.

### 2026-03-31 — GHOST-0039 Energía de Arthur (100 -> 0) con drenado por contacto

- **Modelo base de energía:** `Arthur` expone energía numérica con valor inicial `100` y clamp inferior `0`.
- **Drenado configurable por segundo:** `GhostsGame` aplica drenado con constante `ARTHUR_CONTACT_DRAIN_PER_SECOND` únicamente cuando el contacto está activo.
- **Sin contacto, sin drenado:** la energía deja de bajar inmediatamente al desaparecer la señal de contacto.
- **Sin muerte en este bloque:** al llegar a `0`, Arthur mantiene su loop jugable sin estado de game over.

### 2026-03-31 — GHOST-0040 HUD sutil de energía en esquina inferior derecha

- **HUD textual mínimo:** `GhostsGame` renderiza `Energy: <valor>` con `BitmapFont` sin introducir sistema UI adicional.
- **Posición fija en viewport:** texto anclado a la esquina inferior derecha usando márgenes y medición de `GlyphLayout`.
- **Actualización en tiempo real:** el valor mostrado se calcula por frame desde `arthur.getEnergy()`.
- **Estilo sobrio para fondo oscuro:** color de baja intensidad/alfa para mantener legibilidad sin dominar la escena.

### 2026-03-31 — GHOST-0041 Estado crítico visual en energía 0 (sin muerte)

- **Regla de color crítica:** el HUD mantiene color base mientras `energy > 0` y cambia a rojo estable cuando `energy == 0`.
- **Loop de control intacto:** Arthur continúa jugable en `0` (movimiento, salto y punch) sin transición de muerte.
- **Sin consecuencias extra en este bloque:** no se introduce knockback, invulnerabilidad ni game over.

### 2026-03-31 — GHOST-0042 Integración bloque 3 (contacto -> energía -> HUD -> crítico)

- **Flujo integrado validable:** contacto activo Zombie-Arthur reduce energía, separación detiene drenado, HUD refleja el valor por frame.
- **Comportamiento crítico consolidado:** en `energy == 0` el texto cambia a rojo y Arthur mantiene control completo sin muerte.
- **Directriz de arquitectura reforzada:** mantener la implementación sobre clases existentes (`GhostsGame`, `Arthur`, `Zombie`) evitando proliferación innecesaria.

Checklist manual breve bloque 3 (3-5 minutos):

1. **Contacto inicia drenado:** acercar Arthur al zombie en `WALK` y comprobar que `Energy` empieza a bajar de forma continua.
2. **Separación detiene drenado:** alejar Arthur fuera de contacto y validar que el valor deja de decrecer inmediatamente.
3. **HUD sincronizado:** durante contacto/separación, confirmar que `Energy: <valor>` refleja siempre el valor actual sin latencia visible.
4. **Estado crítico rojo:** mantener contacto hasta `0` y verificar cambio estable del HUD a rojo.
5. **Arthur sigue vivo:** con energía `0`, validar que Arthur aún puede moverse, saltar y golpear.

### 2026-03-21 — GHOST-0000 Bootstrap de fase 1 de control y scroll

- **Rama de trabajo validada:** desarrollo ejecutado en `features-nightly-20260321`.
- **Alcance explícito de fase 1:** izquierda/derecha, agacharse, salto, scroll de dos fondos y luz tenue sobre Arthur.
- **Regla de implementación documentada:** extender clases existentes (principalmente `GhostsGame`) y evitar proliferación de clases sin responsabilidad clara.
- **Referencia de alcance:** fase sin enemigos, combate completo, UI de puntuación/vidas ni persistencia de progreso.

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
9. **Estrés de scroll con input rápido:** alternar `LEFT/RIGHT` cada 200-300ms por 10 segundos y validar continuidad sin saltos de encuadre.
10. **Contraste por estado:** verificar legibilidad Arthur/fondo en `IDLE`, `WALK`, `CROUCH` y `JUMP` de forma explícita.
11. **Transición de halo entre estados:** ejecutar `WALK -> JUMP -> CROUCH -> IDLE` y confirmar cambios suaves, sin parpadeo ni picos.
12. **Bleed en movimiento prolongado:** caminar y hacer flip repetido durante 15 segundos para confirmar ausencia de píxeles de frames vecinos.
13. **Validación contra matriz de tuning:** contrastar valores activos con la sección "Matriz de tuning fase 1" antes de cerrar tickets de control/scroll/luz.

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

### 2026-03-21 — GHOST-0015 Ventana de cámara para scroll más legible

- **Zona horizontal de confort:** Arthur puede moverse dentro de una ventana central antes de desplazar perceptiblemente el mundo.
- **Seguimiento suave:** al salir de la zona, el scroll usa velocidad objetivo derivada del exceso y se suaviza con la respuesta existente para evitar tirones.
- **Reversa estable:** al invertir dirección, el scroll converge sin saltos bruscos de encuadre.
- **Cobertura preservada:** se mantiene el pipeline actual de wrap modular y `N+1` segmentos de fondo para cubrir todo el viewport.

### 2026-03-21 — GHOST-0016 Consistencia de salto con inputs rápidos

- **Control aéreo suave:** en `JUMP`, la velocidad horizontal converge gradualmente al input activo para evitar respuestas erráticas en cambios rápidos izquierda/derecha.
- **Despegue/aterrizaje estables:** al aterrizar se aplica una ventana corta de estabilización para suavizar micro-correcciones de velocidad horizontal.
- **Trayectoria de salto conservada:** altura y timing vertical se mantienen desacoplados del ajuste horizontal.
- **Sin nueva arquitectura:** mejoras integradas en `GhostsGame` y estados actuales (`WALK`, `JUMP`, `IDLE`, `CROUCH`).

### 2026-03-21 — GHOST-0017 Calibración de halo por estado de movimiento

- **Perfiles por estado:** `IDLE`, `WALK`, `CROUCH` y `JUMP` usan objetivos específicos de alpha/tamaño de halo y oscurecimiento global.
- **Transición sin saltos:** la iluminación interpola por frame hacia su objetivo para evitar cambios bruscos al cambiar de estado.
- **Contraste sostenido:** Arthur mantiene foco visual sobre fondo oscuro sin sobreexponer escena durante desplazamiento y salto.
- **Pipeline reutilizado:** ajustes realizados sobre el render actual de `GhostsGame`, sin sistema de iluminación nuevo.

### 2026-03-21 — GHOST-0018 Checklist visual extendido para scroll y luz

- **Cobertura de scroll en ambos sentidos:** el checklist incorpora pruebas de continuidad con alternancia rápida de dirección.
- **Validación de contraste por estado:** se añaden verificaciones explícitas en `IDLE`, `WALK`, `CROUCH` y `JUMP`.
- **Detección de artefactos en bordes:** se agrega criterio específico para bleed durante animación y flip continuo.
- **Integración documental única:** la extensión se aplica al checklist existente en `docs/features.md` sin crear documentos paralelos.

### 2026-03-21 — GHOST-0020 Eliminación de bleeding residual en flip de sprite

- **Flip sin mutar frames base:** Arthur se invierte horizontalmente en draw con ancho negativo, evitando alternar `flip()` sobre `TextureRegion` de animación.
- **Muestreo más robusto del spritesheet:** `sprites_arthur.png` usa `TextureWrap.ClampToEdge` junto a `TextureFilter.Nearest` para reducir fuga visual en bordes.
- **Cobertura de estados preservada:** `IDLE`, `WALK`, `CROUCH` y `JUMP` reutilizan recorte seguro y mantienen proporciones originales de render.
- **Sin sobre-ingeniería:** ajuste integrado en `GhostsGame` sin crear clases nuevas.

### 2026-03-21 — GHOST-0021 Afinado de contraste dinámico personaje-fondo

- **Contraste por actividad real:** el halo y la atenuación global se calculan con factor continuo basado en velocidad horizontal de Arthur.
- **Salto más legible sin parpadeo:** `JUMP` aplica un boost de actividad suave y `CROUCH` reduce intensidad para conservar lectura visual.
- **Transición continua:** los objetivos de alpha/tamaño siguen interpolación exponencial por frame para evitar cambios bruscos entre reposo, movimiento y salto.
- **Pipeline reutilizado:** ajuste realizado sobre el sistema de luz/overlay existente en `GhostsGame`, sin postprocesado nuevo.

### 2026-03-21 — GHOST-0022 Microajuste de zona de confort de cámara

- **Ventana recalibrada:** los límites de confort horizontal se ajustan a una banda más estrecha para que el scroll reaccione antes al avance.
- **Respuesta estable por desborde:** la velocidad objetivo de cámara se calcula con ganancia moderada y tope máximo para evitar tirones al invertir dirección.
- **Cobertura de viewport preservada:** se mantiene el mismo pipeline de wrap modular y dibujado de `N+1` fondos para no exponer huecos.
- **Sin cambio arquitectónico:** ajuste aplicado en la lógica existente de `GhostsGame`.

### 2026-03-21 — GHOST-0023 Matriz de tuning para movimiento fase 1

Matriz corta de parámetros activos para iterar control/scroll/luz sin regresiones:

| Parámetro activo | Valor actual | Rango recomendado fase 1 | Criterio visual de validación manual |
|---|---:|---:|---|
| `GROUND_ACCELERATION` | `1700` | `1500-1900` | Arthur inicia `WALK` con respuesta firme sin salto brusco de velocidad. |
| `GROUND_DECELERATION` | `2200` | `2000-2600` | Al soltar input en suelo, frena limpio y entra en `IDLE` sin deslizamiento largo. |
| `JUMP_VELOCITY` | `520` | `480-560` | El arco de salto es legible y repetible al encadenar saltos consecutivos. |
| `JUMP_RISE_GRAVITY` | `1180` | `1050-1300` | La subida mantiene sensación de peso sin volverse flotante. |
| `JUMP_FALL_GRAVITY` | `1030` | `950-1200` | La caída regresa al suelo con continuidad, sin aterrizaje abrupto. |
| `SCROLL_RESPONSE_RATE` | `13` | `10-16` | El scroll sigue avance/reversa sin retraso excesivo ni oscilación visible. |

Uso operativo:

- Aplicar esta matriz junto al checklist técnico (ítems 1-13) para cierre de tickets de control/scroll/luz.
- Si un ajuste sale del rango recomendado, justificarlo en la entrada del ticket correspondiente en este mismo documento.

### 2026-03-21 — GHOST-0024 Ajuste de inset de recorte para eliminar borde cruzado

- **Inset uniforme por estado:** el recorte seguro de Arthur usa `SPRITE_FRAME_INSET_PX=2` en todos los frames de `IDLE`, `WALK`, `CROUCH` y `JUMP`.
- **Menos bleed entre celdas:** el margen adicional reduce muestreo de píxeles vecinos del spritesheet durante animación continua y flips repetidos.
- **Alineación preservada:** el tamaño de dibujo y la base de Arthur no se alteran porque el ancho de render se mantiene desacoplado del recorte interno.
- **Sin cambio arquitectónico:** ajuste integrado sobre `GhostsGame` reutilizando la estructura actual.

### 2026-03-21 — GHOST-0025 Oscurecimiento base del fondo para reforzar contraste

- **Parámetro único de atenuación:** el render de fondo usa `BACKGROUND_BASE_DIM_ALPHA` como único control de oscurecimiento homogéneo.
- **Contraste más claro de Arthur:** se incrementa moderadamente la atenuación base para mejorar separación personaje-escena en reposo y movimiento.
- **Sin parpadeo:** el valor fijo elimina variaciones por frame en el overlay del fondo.
- **Reutilización total:** cambio aplicado en el pipeline de render existente (`drawBackgroundDim`) sin nuevas capas ni clases.

### 2026-03-21 — GHOST-0026 Estabilidad de agachado bajo cambios rápidos de input

- **Ancla de crouch en X:** al entrar en `CROUCH`, Arthur fija una posición horizontal de referencia (`crouchAnchorX`) reutilizada mientras se mantiene `DOWN/S`.
- **Sin desplazamiento fantasma:** durante secuencias rápidas `LEFT/RIGHT + DOWN` se fuerza `arthurVelocityX=0` y se conserva la misma X anclada en estado agachado.
- **Base visual estable:** la pose mantiene alineación de suelo al entrar/salir de `CROUCH` sin saltos laterales.
- **Compatibilidad mantenida:** el cambio no altera la respuesta de salto ni el pipeline actual de movimiento fuera de crouch.

### 2026-03-21 — GHOST-0027 Reanclaje del halo de luz al torso de Arthur

- **Origen del halo por referencia de torso:** la luz toma un ancla estable basada en proporción del sprite (`LIGHT_TORSO_X/Y`) en lugar de un offset vertical único.
- **Perfil por estado:** `IDLE`, `WALK`, `CROUCH` y `JUMP` usan altura de torso específica para mantener el foco visual centrado en cada pose.
- **Sin deriva por flip:** el ancla horizontal permanece en el centro del cuerpo y no depende de la dirección (`facingRight`), evitando desplazamientos erráticos al invertir movimiento.
- **Intensidad conservada:** se mantiene el mismo pipeline y niveles de alpha/tamaño ya calibrados para no sobreexponer la escena.

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

---

## Iteración PO autónoma — 2026-03-21T07:11:00Z (fase 1 control/scroll/luz)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `features-nightly-20260321`.

### Estado actual validado

- `DONE`: `GHOST-0001` a `GHOST-0018`.
- `IN_PROGRESS` (`WIP=1`): `GHOST-0019`.
- `BACKLOG` (5): `GHOST-0000`, `GHOST-0020`, `GHOST-0021`, `GHOST-0022`, `GHOST-0023`.

### Foco activo obligatorio de fase 1

- Control de Arthur con teclado y salto más preciso (variación corto/largo).
- Scroll horizontal estable con zona de confort de cámara ajustada.
- Mayor contraste Arthur/fondo con luz tenue calibrada sin sobreexposición.
- Eliminación de bleed residual en bordes del sprite al animar y hacer flip.

### Directriz técnica al equipo (obligatoria)

- Reutilizar estructura existente antes de crear clases nuevas.
- Evitar proliferación de clases pequeñas sin responsabilidad clara.
- Mantener tickets pequeños, verticales y verificables en build jugable.

### Nuevos tickets de esta iteración

- `GHOST-0019` — Ajuste de salto corto/largo por duración de tecla (`IN_PROGRESS`).
- `GHOST-0020` — Eliminación de bleeding residual en flip de sprite (`BACKLOG`).
- `GHOST-0021` — Afinado de contraste dinámico personaje-fondo (`BACKLOG`).
- `GHOST-0022` — Microajuste de zona de confort de cámara (`BACKLOG`).
- `GHOST-0023` — Matriz de tuning para movimiento fase 1 (`BACKLOG`).

## Iteración PO autónoma — 2026-03-21T07:00:26Z (fase 1 control/scroll/luz)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `features-nightly-20260321`.

### Estado actual validado

- `DONE`: `GHOST-0001` a `GHOST-0018`.
- `IN_PROGRESS` (`WIP=1`): `GHOST-0019`.
- `BACKLOG` (5): `GHOST-0000`, `GHOST-0020`, `GHOST-0021`, `GHOST-0022`, `GHOST-0023`.

### Decisión de planificación de esta iteración

- No se crean tickets nuevos en esta ejecución para mantener foco en el cierre de `GHOST-0019` y en la secuencia ya preparada de fase 1.
- Se mantiene backlog en 5 tickets activos y alineados al alcance: control de Arthur, scroll continuo de los 2 fondos, contraste de luz y limpieza de bordes de sprite.
- Se confirma cumplimiento de la regla `WIP=1`.

### Aviso técnico al equipo de desarrollo

- Reutilizar estructura existente en `GhostsGame` antes de crear nuevas clases.
- Evitar fragmentar responsabilidades en muchas clases pequeñas sin beneficio claro.
- Cada cambio debe ser pequeño, verificable en build jugable y centrado en control, scroll, luz o bordes de sprite.

## Iteración PO autónoma — 2026-03-21T09:20:00Z (reposición backlog + guardrails fase 1)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `features-nightly-20260321`.

### Estado actual validado

- `DONE`: `GHOST-0001` a `GHOST-0018`, `GHOST-0020`, `GHOST-0021`, `GHOST-0022`, `GHOST-0023`.
- `IN_PROGRESS` (`WIP=1`): `GHOST-0019`.
- `BACKLOG` (5): `GHOST-0000`, `GHOST-0024`, `GHOST-0025`, `GHOST-0026`, `GHOST-0027`.

### Acciones ejecutadas en esta iteración

- Se crearon tickets nuevos de alcance pequeño y vertical:
  - `GHOST-0024` — Ajuste de inset de recorte para eliminar borde cruzado.
  - `GHOST-0025` — Oscurecimiento base del fondo para reforzar contraste.
  - `GHOST-0026` — Estabilidad de agachado bajo cambios rápidos de input.
  - `GHOST-0027` — Reanclaje del halo de luz al torso de Arthur.

### Foco obligatorio (fase 1)


### Aviso a desarrolladores

  - `GHOST-0027` — Reanclaje del halo de luz al torso de Arthur.
- Se corrigió desvío de estado: `GHOST-0000` fue retornado a `BACKLOG` para preservar su rol de ticket bootstrap de referencia.

### Foco obligatorio (fase 1)

- Perfeccionar control de Arthur: izquierda, derecha, agacharse y salto.
- Mejorar limpieza visual de sprite (evitar bleed entre frames).
- Afinar scroll/cámara sobre los dos fondos actuales.
- Reforzar contraste: luz tenue sobre Arthur y fondo algo más oscuro.

### Aviso a desarrolladores

- Cuidar la estructura del código: priorizar reutilización de componentes existentes.
- Evitar proliferación de clases sin responsabilidad clara; no queremos fragmentar el dominio en muchas piezas pequeñas.
- Mantener entregas pequeñas, concretas y verificables en build jugable.

---

## Refactorización arquitectónica — 2026-03-31 (post GHOST-0027)

Refactorización completa de la arquitectura de clases aprobada y en producción en rama `features-nightly-20260321`.
Build validado: `mvn compile exec:exec` (OK).

### Reestructuración de paquetes

La clase monolítica `com.davidpe.ghosts.GhostsGame` fue separada en la siguiente jerarquía de paquetes:

```
com.davidpe.ghosts
├── DesktopLauncher
├── application/
│   ├── GhostsGame
│   └── factories/
│       └── CharacterFactory
└── domain/
    ├── characters/
    │   ├── Character
    │   └── Arthur
    └── utils/
        └── AnimationUtils
```

### Clase base abstracta `Character`

- Campos protegidos comunes extraídos: `x`, `y`, `velocityX`, `velocityY`, `facingRight`,
  `drawWidth`, `stateTime`, `worldWidth`, `renderFrame`.
- `ownedTextures` (`List<Texture>`): patrón de registro centralizado; `dispose()` heredado libera
  todas las texturas registradas.
- `resetStateTime()`: sustituye el uso directo de `stateTime = 0f` al transicionar estados.
- `moveTowards(current, target, maxDelta)`: utilidad de aceleración/frenado movida a base class.
- Hooks abstractos que toda subclase implementa: `updateBehavior(float delta)`,
  `getCurrentFrame()`, `getDrawHeight()`.

### `AnimationUtils` (singleton inyectable)

- Nueva clase `com.davidpe.ghosts.domain.utils.AnimationUtils`.
- Desacopla la lógica de carga de animaciones de la clase `Arthur`.
- Obtención: `AnimationUtils.getInstance()`.
- Inyectado en constructores de personajes vía `CharacterFactory` para facilitar testing y
  reutilización futura.

### `CharacterFactory`

- Nueva clase `com.davidpe.ghosts.application.factories.CharacterFactory`.
- Recibe `AnimationUtils` en su constructor; expone `createArthur(float worldWidth)`.
- `GhostsGame` crea todos los personajes exclusivamente a través de la factory.

### Arthur — cambios y nuevos estados

- Constructor actualizado: `Arthur(float worldWidth, AnimationUtils animationUtils)`.
- Estado **`PUNCH`** añadido: golpe one-shot activado con `SPACE`, bloquea movimiento horizontal
  durante la animación.
- Estado **`CROUCH_UP`** añadido: animación de levantarse al soltar DOWN/S antes de volver a
  `IDLE`. Elimina el corte abrupto previo.
- `loadSheet(path)`: método privado que carga la textura, aplica `TextureFilter.Nearest` y la
  registra en `ownedTextures`.
- Máquina de estados completa (6 estados): `IDLE`, `WALK`, `CROUCH`, `CROUCH_UP`, `JUMP`, `PUNCH`.

### GhostsGame simplificado

- Sin constantes, campos ni métodos específicos de Arthur.
- Constantes de escena públicas: `WORLD_WIDTH = 800f`, `WORLD_HEIGHT = 600f`.
- Orden de dibujado por frame: `drawScrollingBackgrounds()` → `drawBackgroundDim()` →
  `arthur.drawEffects(batch)` → `arthur.draw(batch)`.
- Overlay negro semitransparente: `Pixmap` 1×1, alpha `0.21f`.
- Únicamente 2 fondos de scroll: `main-backgroud-1.png` y `main-background-2.png`.

## Iteración PO autónoma — 2026-03-31 (plan de zombies, bloque 1/4)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `feature/zombie-enemies`.

### Estado validado antes de planificar

- `DONE`: tickets históricos `GHOST-0001` a `GHOST-0027`.
- `IN_PROGRESS`: ninguno.
- `BACKLOG`: ninguno.

### Acciones ejecutadas

- `GHOST-0000` retornado a `BACKLOG` (ticket bootstrap debe permanecer de referencia).
- Se crearon 5 tickets nuevos de **Character Zombie** (sin lógica global de gameplay):
  - `GHOST-0028` — Esqueleto de Zombie y estados base de animación.
  - `GHOST-0029` — Carga de animaciones Zombie desde `resources/zombie`.
  - `GHOST-0030` — Máquina de estados del Zombie y transiciones cerradas.
  - `GHOST-0031` — Render y movimiento base del Zombie en escena.
  - `GHOST-0032` — Integración de fábrica y checklist de validación de Zombie Character.

### Secuencia obligatoria de rollout (sin saltos)

1. Completar `GHOST-0028`..`GHOST-0032` (character zombie listo: `WALK`, `GROUND_RISE`, `GROUND_HIDE`, `HITTED`).
2. Solo después crear 5 tickets de spawn/IA/lifetime (`10s`, hide posterior, respawn aleatorio, Arthur como driver de scroll).
3. Solo después crear 5 tickets de energía de Arthur (100→0, HUD sutil abajo derecha, rojo al llegar a 0, sin muerte aún).
4. Solo después crear 5 tickets de combate Arthur→Zombie (animación `HITTED`, 3 golpes para forzar `GROUND_HIDE`, si no llega a 3 vuelve a perseguir).
5. Al finalizar todo el bloque, crear ticket único de code review orientado a bugs/compilación, pidiendo cambios mínimos y alta reutilización estructural.

### Aviso a desarrolladores

- Mantener estructura DDD actual (`application`/`domain`) y evitar proliferación de clases.
- Reutilizar utilidades/factorías existentes antes de abrir nuevas abstracciones.
- Tickets pequeños, verticales y verificables en build jugable.

## 2026-03-31 — GHOST-0000 Bootstrap operativo vigente

- Rama de implementación validada para esta ejecución: `feature/zombie-enemies`.
- El ticket se usa como referencia de alcance y reglas de trabajo incremental.
- Foco documentado de esta iteración: bloque de Zombie Character (`GHOST-0028` a `GHOST-0032`).
- Regla de arquitectura reafirmada: priorizar extensión de estructura existente (`application`/`domain`) y evitar proliferación de clases.

## 2026-03-31 — GHOST-0028..GHOST-0032 Zombie Character base

- Nuevo character de dominio: `Zombie extends Character` con estados `WALK`, `GROUND_RISE`, `GROUND_HIDE`, `HITTED`.
- Carga de animaciones con `AnimationUtils` desde:
  - `zombie/sprite-sheet-zombie-walk.png` + `bounding-boxes-zombie-walk.json`
  - `zombie/sprite-sheet-zombie-ground.png` + `bounding-boxes-zombie-ground.json`
  - `zombie/sprite-sheet-zombie-hitted.png` + `bounding-boxes-zombie-hitted.json`
- `GROUND_HIDE` reutiliza los mismos frames de `GROUND` en orden inverso (sin assets duplicados).
- Transiciones internas cerradas del Zombie:
  - `GROUND_RISE` (one-shot) -> `WALK`
  - `WALK` (loop)
  - `HITTED` (one-shot) -> `WALK`
  - `GROUND_HIDE` (one-shot) -> `GROUND_RISE`
- Integración en `GhostsGame`:
  - `update()` + `draw()` del Zombie activos en escena.
  - Movimiento horizontal base por patrulla local (Arthur sigue siendo driver del scroll global por `worldOffsetX`).
  - Instanciación vía `CharacterFactory#createZombie(...)`.

### Checklist manual corto Zombie Character

1. Ejecutar el juego y validar que Zombie arranca con `GROUND_RISE` y pasa automáticamente a `WALK`.
2. Observar patrulla horizontal: el sprite invierte orientación en límites sin jitter visible.
3. Pulsar `H` y verificar `HITTED` one-shot con retorno a `WALK`.
4. Pulsar `G` y verificar secuencia `GROUND_HIDE` (one-shot) seguida de `GROUND_RISE` y vuelta a `WALK`.
5. Repetir `H`/`G` alternados y confirmar estabilidad de animación y ausencia de fugas de texturas al cerrar la app.

## Iteración PO autónoma — 2026-03-31 (plan de zombies, bloque 2/4)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `feature/zombie-enemies`.

### Estado validado antes de planificar

- `DONE`: `GHOST-0028`..`GHOST-0032` completados (character zombie base finalizado).
- `IN_PROGRESS`: ninguno.
- `BACKLOG`: ninguno.

### Acciones ejecutadas

- `GHOST-0000` retornado a `BACKLOG` para conservar ticket bootstrap de referencia.
- Se crearon 5 tickets nuevos del **bloque 2 (spawn/IA/lifetime)**:
  - `GHOST-0033` — Ciclo de vida temporal del Zombie (`10s` -> `GROUND_HIDE`).
  - `GHOST-0034` — Spawn relativo a Arthur (delante/detrás) con límites de mundo.
  - `GHOST-0035` — IA simple de aparición y reaparición aleatoria.
  - `GHOST-0036` — Persecución hacia Arthur desacoplada del driver de scroll.
  - `GHOST-0037` — Integración final spawn/IA + checklist manual del bloque 2.
- `GHOST-0033` pasado a `IN_PROGRESS` para restaurar `WIP=1`.

### Reglas de implementación reafirmadas para devs

- Mantener estructura DDD actual (`application`/`domain`) y reutilizar clases existentes.
- Evitar proliferación de clases: introducir solo piezas mínimas cuando sea imprescindible.
- Arthur sigue siendo el driver del scroll; el ciclo de vida del zombie no se reinicia por salir del viewport.
- En este bloque no hay daño por contacto al cruzarse con Arthur.

### Secuencia obligatoria de rollout (sin saltos)

1. Completar `GHOST-0033`..`GHOST-0037` (spawn + IA + lifecycle estables).
2. Solo después crear 5 tickets de energía de Arthur (`100 -> 0`, HUD sutil abajo derecha, rojo al llegar a 0, sin muerte).
3. Solo después crear 5 tickets de combate Arthur->Zombie (`HITTED`, 3 golpes para forzar `GROUND_HIDE`).
4. Tras cerrar esos bloques, crear ticket único de review para agentes dev orientado a bugs/compilación con cambios mínimos.

## Iteración PO autónoma — 2026-03-31 (seguimiento bloque 3/4 energía)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `feature/zombie-enemies`.

### Estado Tasker validado

- `IN_PROGRESS (WIP=1)`: `GHOST-0038`.
- `BACKLOG (5)`: `GHOST-0000`, `GHOST-0039`, `GHOST-0040`, `GHOST-0041`, `GHOST-0042`.
- `DONE`: mantiene histórico completo de bloques 1 y 2 (`GHOST-0028..GHOST-0037` cerrados).

### Validación de implementación en repositorio

- Compilación verificada: `mvn -q -DskipTests compile` (OK).
- Bloque 2 sigue operativo en código (`spawn/lifecycle/IA` de zombie ya integrado).
- Bloque 3 todavía no está implementado en código: no existe aún modelo de energía de Arthur, ni HUD inferior derecho, ni regla visual de energía 0 en rojo.

### Decisión de planificación

- No se crean tickets del bloque 4 (combate Arthur->Zombie) porque la secuencia obligatoria exige cerrar antes `GHOST-0038..GHOST-0042`.
- Se mantiene `WIP=1` y backlog en 5 tickets sin alterar scope de tickets existentes.

### Aviso a desarrolladores

- Priorizar cierre vertical de `GHOST-0038..GHOST-0042` antes de abrir nuevas líneas funcionales.
- Mantener estructura DDD actual y reutilizar clases existentes; evitar proliferación innecesaria de clases.
- Hacer cambios pequeños y compilables por ticket.

## Iteración PO autónoma — 2026-03-31 (plan de zombies, bloque 4/4)

Validación ejecutada contra Tasker (`projectId=6`, `userId=1`) y repositorio local en rama `feature/zombie-enemies`.

### Estado validado antes de planificar

- `DONE`: `GHOST-0038`..`GHOST-0042` (bloque de energía de Arthur por contacto completado en Tasker).
- Se corrigió deriva de estado: `GHOST-0000` volvió a `BACKLOG` (ticket bootstrap de referencia).
- Revisión de código confirmada en `GhostsGame`, `Arthur` y `Zombie`:
  - Contacto Zombie-Arthur activo y drenado de energía `100 -> 0` implementado.
  - HUD `Energy` visible en esquina inferior derecha, con cambio a rojo en `0`.
  - Arthur permanece jugable en energía `0` (sin muerte), como exige el alcance.

### Nuevos tickets creados (bloque 4: combate Arthur -> Zombie)

- `GHOST-0043` — Golpe de Arthur dispara estado `HITTED` del Zombie. (`IN_PROGRESS`)
- `GHOST-0044` — Contador de impactos al Zombie con umbral de 3 golpes.
- `GHOST-0045` — Reanudación de persecución tras `HITTED` con breve delay.
- `GHOST-0046` — Tercer golpe fuerza `GROUND_HIDE` y corta daño a Arthur.
- `GHOST-0047` — Integración bloque 4 combate Zombie + checklist manual.

### Reglas activas de orquestación

- `WIP=1` respetado: solo `GHOST-0043` en `IN_PROGRESS`.
- Backlog mínimo garantizado: `GHOST-0000` + `GHOST-0044`..`GHOST-0047`.
- No se abre todavía ticket de puntuación ni ticket de review final: quedan explícitamente bloqueados hasta completar `GHOST-0043`..`GHOST-0047`.

### Directriz para agentes dev

- Mantener arquitectura DDD actual (dominio/aplicación) y reutilizar clases existentes.
- Evitar proliferación de clases; priorizar extensión puntual en `GhostsGame`, `Arthur`, `Zombie` y factorías actuales.
