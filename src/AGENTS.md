🎮 Prompt: Pantalla inicial tipo Ghosts ’n Goblins con scroll lateral en LibGDX

🧠 Contexto

Estoy desarrollando un juego 2D en Java usando LibGDX con Maven. Quiero crear una pantalla inicial jugable donde el personaje pueda moverse horizontalmente (izquierda/derecha) mientras el fondo hace scroll continuo.

El estilo visual es tipo Ghosts ’n Goblins:
	•	ambientación oscura
	•	cementerio / bosque nocturno
	•	scroll lateral clásico

⸻

🎯 Objetivo

Implementar una escena básica con:
	1.	Un personaje animado (spritesheet)
	2.	Movimiento izquierda/derecha con teclado
	3.	Scroll lateral del fondo
	4.	Uso de 3 imágenes de fondo consecutivas
	5.	Loop continuo del escenario

⸻

🖼️ Recursos disponibles

Fondos

Tengo 3 imágenes:
	•	background1.png
	•	background2.png
	•	background3.png

Todas tienen el mismo tamaño y están diseñadas para encajar horizontalmente.

Personaje

Spritesheet con:
	•	animación de caminar
	•	frames en una fila
	•	tamaño uniforme

⸻

⚙️ Requisitos técnicos

Framework
	•	LibGDX
	•	Java (17+)
	•	Maven

Renderizado
	•	Usar SpriteBatch
	•	Dibujar fondos y personaje

⸻

🎮 Comportamiento esperado

Movimiento del personaje
	•	Flecha derecha → mueve personaje a la derecha
	•	Flecha izquierda → mueve personaje a la izquierda
	•	Animación de caminar activa al moverse
	•	Personaje centrado (opcional estilo cámara)

⸻

Scroll del fondo
	•	El fondo se mueve en dirección opuesta al movimiento del jugador
	•	Se usan las 3 imágenes en secuencia horizontal
	•	Cuando una imagen sale completamente de pantalla → se recicla al final

Ejemplo:

[BG1][BG2][BG3] → scroll → [BG2][BG3][BG1]


⸻

Sistema de coordenadas
	•	Usar un offset global (worldOffsetX)
	•	El personaje puede mantenerse fijo en pantalla mientras el mundo se mueve

⸻

🧱 Estructura sugerida

Variables principales

float worldOffsetX;
float playerX;

Texture bg1, bg2, bg3;
float bgWidth;

Animation<TextureRegion> walkAnimation;
float stateTime;


⸻

Lógica de update

if (RIGHT) worldOffsetX += speed * delta;
if (LEFT) worldOffsetX -= speed * delta;

stateTime += delta;


⸻

Render de fondos (loop)

for (int i = 0; i < 3; i++) {
    batch.draw(backgrounds[i], i * bgWidth - worldOffsetX, 0);
}

Recolocar fondos cuando salen de pantalla.

⸻

🎨 Detalles visuales
	•	Fondo oscuro (cementerio, árboles, niebla)
	•	Personaje estilo caballero medieval
	•	Movimiento fluido
	•	Escenario continuo sin cortes visibles

⸻

🚫 No incluir (por ahora)
	•	Saltos
	•	Colisiones
	•	Enemigos
	•	Física
	•	UI

⸻

✅ Resultado esperado
	•	El personaje puede moverse izquierda/derecha
	•	El fondo hace scroll continuo
	•	Las 3 imágenes se reutilizan en bucle
	•	Animación de caminar funcionando

⸻

🚀 Bonus (opcional)
	•	Parallax (varias capas de fondo con distinta velocidad)
	•	Flip automático del personaje según dirección
	•	Cámara centrada en el jugador

⸻

📌 Instrucción final

Genera el código completo en Java usando LibGDX (ApplicationAdapter o Screen), listo para integrarse en un proyecto Maven existente.