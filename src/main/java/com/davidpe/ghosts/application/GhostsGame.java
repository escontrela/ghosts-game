package com.davidpe.ghosts.application;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.davidpe.ghosts.application.factories.CharacterFactory;
import com.davidpe.ghosts.domain.characters.Arthur;
import com.davidpe.ghosts.domain.utils.AnimationUtils;

/**
 * Main game orchestrator for "Ghosts 'n Goblins Remake". Manages the LibGDX lifecycle ({@code
 * create}, {@code render}, {@code resize}, {@code dispose}) and owns the shared rendering
 * infrastructure: camera, viewport, sprite batch, scrolling backgrounds, and the dim overlay.
 * Character creation is delegated to {@link CharacterFactory}; per-frame logic is delegated to the
 * character domain objects (currently {@link Arthur}).
 */
public class GhostsGame extends ApplicationAdapter {

  public static final float WORLD_WIDTH = 800;
  public static final float WORLD_HEIGHT = 600;

  private static final float BACKGROUND_BASE_DIM_ALPHA = 0.21f;

  private SpriteBatch batch;
  private OrthographicCamera camera;
  private Viewport viewport;

  private Texture[] backgrounds;
  private Texture blackOverlayTexture;

  private Arthur arthur;

  @Override
  public void create() {

    camera = new OrthographicCamera();
    viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
    camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
    camera.update();

    batch = new SpriteBatch();

    backgrounds =
        new Texture[] {
          new Texture(Gdx.files.internal("main-backgroud-1.png")),
          new Texture(Gdx.files.internal("main-background-2.png"))
        };
    blackOverlayTexture = createSolidTexture(1, 1, 0f, 0f, 0f, 1f);

    CharacterFactory characterFactory = new CharacterFactory(AnimationUtils.getInstance());
    arthur = characterFactory.createArthur(WORLD_WIDTH);
  }

  @Override
  public void render() {

    ScreenUtils.clear(0, 0, 0, 1);

    float delta = Gdx.graphics.getDeltaTime();
    arthur.update(delta);

    camera.update();
    batch.setProjectionMatrix(camera.combined);

    batch.begin();

    drawScrollingBackgrounds();
    drawBackgroundDim();
    arthur.drawEffects(batch);
    arthur.draw(batch);

    batch.end();
  }

  @Override
  public void resize(int width, int height) {
    viewport.update(width, height);
  }

  @Override
  public void dispose() {
    batch.dispose();
    for (Texture texture : backgrounds) {
      texture.dispose();
    }
    blackOverlayTexture.dispose();
    arthur.dispose();
  }

  private void drawScrollingBackgrounds() {

    float segmentWidth = WORLD_WIDTH;
    float cycleWidth = segmentWidth * backgrounds.length;
    float wrappedOffset = ((arthur.getWorldOffsetX() % cycleWidth) + cycleWidth) % cycleWidth;
    int startSegment = (int) (wrappedOffset / segmentWidth);
    float segmentOffset = wrappedOffset - (startSegment * segmentWidth);

    for (int i = 0; i < backgrounds.length + 1; i++) {
      int textureIndex = (startSegment + i) % backgrounds.length;
      float drawX = (i * segmentWidth) - segmentOffset;
      batch.draw(backgrounds[textureIndex], drawX, 0f, WORLD_WIDTH, WORLD_HEIGHT);
    }
  }

  private void drawBackgroundDim() {

    Color previousColor = new Color(batch.getColor());
    batch.setColor(1f, 1f, 1f, BACKGROUND_BASE_DIM_ALPHA);
    batch.draw(blackOverlayTexture, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
    batch.setColor(previousColor);
  }

  private Texture createSolidTexture(int width, int height, float r, float g, float b, float a) {

    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(r, g, b, a);
    pixmap.fill();
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }
}
