package com.davidpe.ghosts.application;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.davidpe.ghosts.application.audio.GameAudio;
import com.davidpe.ghosts.application.factories.CharacterFactory;
import com.davidpe.ghosts.domain.characters.Arthur;
import com.davidpe.ghosts.domain.characters.Zombie;
import com.davidpe.ghosts.domain.characters.ZombieTuning;
import com.davidpe.ghosts.domain.utils.AnimationUtils;
import java.util.Random;

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
  private static final float ARTHUR_CONTACT_DRAIN_PER_SECOND = 13f;
  private static final float ENERGY_HUD_MARGIN_RIGHT = 18f;
  private static final float ENERGY_HUD_MARGIN_BOTTOM = 14f;
  private static final float SCORE_HUD_MARGIN_LEFT = 18f;
  private static final float SCORE_HUD_MARGIN_BOTTOM = 14f;
  private static final float ENERGY_HUD_BASE_R = 0.86f;
  private static final float ENERGY_HUD_BASE_G = 0.84f;
  private static final float ENERGY_HUD_BASE_B = 0.79f;
  private static final float ENERGY_HUD_BASE_A = 0.78f;
  private static final float ENERGY_HUD_CRITICAL_R = 0.93f;
  private static final float ENERGY_HUD_CRITICAL_G = 0.2f;
  private static final float ENERGY_HUD_CRITICAL_B = 0.2f;
  private static final float ENERGY_HUD_CRITICAL_A = 0.9f;
  private static final float ENERGY_HUD_BLINK_SPEED = 4f;
  private static final float ARTHUR_PUNCH_REACH = 46f;
  private static final float ARTHUR_PUNCH_VERTICAL_REACH = 22f;

  private SpriteBatch batch;
  private OrthographicCamera camera;
  private Viewport viewport;

  private Texture[] backgrounds;
  private Texture blackOverlayTexture;
  private BitmapFont hudFont;
  private GlyphLayout hudLayout;
  private GameAudio gameAudio;

  private Arthur arthur;
  private Zombie zombie;
  private Random random;
  private boolean zombieCycleActive;
  private float zombieRespawnTimer;
  private boolean zombieArthurContactActive;
  private boolean zombieDefeatByHitEventPending;
  private int defeatedZombieCount;
  private float gameTime;

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
    hudFont = new BitmapFont();
    hudLayout = new GlyphLayout();
    gameAudio = new GameAudio();
    gameAudio.loadAll();

    CharacterFactory characterFactory = new CharacterFactory(AnimationUtils.getInstance());
    arthur = characterFactory.createArthur(WORLD_WIDTH);
    zombie = characterFactory.createZombie(WORLD_WIDTH);
    random = new Random();
    zombieRespawnTimer = 0f;
    zombieArthurContactActive = false;
    zombieDefeatByHitEventPending = false;
    defeatedZombieCount = 0;
    gameTime = 0f;
    activateZombieCycle(pickSpawnSide());
  }

  @Override
  public void render() {

    ScreenUtils.clear(0, 0, 0, 1);

    float delta = Gdx.graphics.getDeltaTime();
    gameTime += delta;
    handleZombieDebugInput();
    float prevWorldOffset = arthur.getWorldOffsetX();
    arthur.update(delta);
    if (arthur.consumeJumpSoundEvent()) {
      gameAudio.play(GameAudio.Cue.ARTHUR_JUMP);
    }
    if (arthur.consumePunchSoundEvent()) {
      gameAudio.play(GameAudio.Cue.ARTHUR_LAND);
    }
    float scrollDelta = arthur.getWorldOffsetX() - prevWorldOffset;
    zombie.applyWorldScroll(scrollDelta);
    updateZombieSpawner(delta);
    zombie.setTargetX(arthur.getX());
    zombie.update(delta);
    boolean defeatedByHitEvent = zombie.consumeDefeatByHitEvent();
    if (defeatedByHitEvent) {
      defeatedZombieCount += 1;
    }
    zombieDefeatByHitEventPending = zombieDefeatByHitEventPending || defeatedByHitEvent;
    processArthurPunchHit();
    zombieArthurContactActive =
        zombie.isInContactWith(
            arthur.getX(), arthur.getY(), arthur.getDrawWidth(), arthur.getDrawHeightValue());
    arthur.applyContactEnergyDrain(
        zombieArthurContactActive, delta, ARTHUR_CONTACT_DRAIN_PER_SECOND);
    if (arthur.consumeHitSoundEvent()) {
      gameAudio.play(GameAudio.Cue.ARTHUR_HIT);
    }

    camera.update();
    batch.setProjectionMatrix(camera.combined);

    batch.begin();

    drawScrollingBackgrounds();
    drawBackgroundDim();
    zombie.draw(batch);
    arthur.drawEffects(batch);
    arthur.draw(batch);
    drawScoreHud();
    drawEnergyHud();

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
    hudFont.dispose();
    gameAudio.dispose();
    arthur.dispose();
    zombie.dispose();
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

  private void handleZombieDebugInput() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
      spawnZombieRelativeToArthur(Zombie.SpawnSide.BEHIND);
    }
    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
      spawnZombieRelativeToArthur(Zombie.SpawnSide.AHEAD);
    }
    if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
      zombie.triggerHitted();
    }
    if (Gdx.input.isKeyJustPressed(Input.Keys.G)) {
      zombie.triggerGroundHide();
    }
  }

  private void updateZombieSpawner(float delta) {
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
  }

  private Texture createSolidTexture(int width, int height, float r, float g, float b, float a) {

    Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
    pixmap.setColor(r, g, b, a);
    pixmap.fill();
    Texture texture = new Texture(pixmap);
    pixmap.dispose();
    return texture;
  }

  private void spawnZombieRelativeToArthur(Zombie.SpawnSide spawnSide) {
    float spawnDistance =
        spawnSide == Zombie.SpawnSide.AHEAD
            ? ZombieTuning.SPAWN_AHEAD_DISTANCE
            : ZombieTuning.SPAWN_BEHIND_DISTANCE;
    float spawnX = zombie.resolveSpawnX(arthur.getX(), spawnSide, spawnDistance);
    zombie.startGroundRiseAt(spawnX);
  }

  private void activateZombieCycle(Zombie.SpawnSide spawnSide) {
    spawnZombieRelativeToArthur(spawnSide);
    zombieCycleActive = true;
    zombieRespawnTimer = 0f;
  }

  private Zombie.SpawnSide pickSpawnSide() {
    return random.nextBoolean() ? Zombie.SpawnSide.AHEAD : Zombie.SpawnSide.BEHIND;
  }

  private float randomRespawnDelay() {
    return ZombieTuning.RESPAWN_DELAY_MIN_SECONDS
        + random.nextFloat()
            * (ZombieTuning.RESPAWN_DELAY_MAX_SECONDS - ZombieTuning.RESPAWN_DELAY_MIN_SECONDS);
  }

  private void drawEnergyHud() {
    float energy = arthur.getEnergy();
    String energyText = "Energy: " + Math.round(energy);
    if (energy <= 0f) {
      float blink = (float) Math.abs(Math.sin(gameTime * ENERGY_HUD_BLINK_SPEED * Math.PI));
      hudFont.setColor(
          ENERGY_HUD_CRITICAL_R,
          ENERGY_HUD_CRITICAL_G,
          ENERGY_HUD_CRITICAL_B,
          ENERGY_HUD_CRITICAL_A * blink);
    } else {
      hudFont.setColor(ENERGY_HUD_BASE_R, ENERGY_HUD_BASE_G, ENERGY_HUD_BASE_B, ENERGY_HUD_BASE_A);
    }
    hudLayout.setText(hudFont, energyText);
    float textX = WORLD_WIDTH - ENERGY_HUD_MARGIN_RIGHT - hudLayout.width;
    float textY = ENERGY_HUD_MARGIN_BOTTOM + hudLayout.height;
    hudFont.draw(batch, hudLayout, textX, textY);
  }

  private void drawScoreHud() {
    String scoreText = "Score: " + defeatedZombieCount;
    hudFont.setColor(ENERGY_HUD_BASE_R, ENERGY_HUD_BASE_G, ENERGY_HUD_BASE_B, ENERGY_HUD_BASE_A);
    hudLayout.setText(hudFont, scoreText);
    float textX = SCORE_HUD_MARGIN_LEFT;
    float textY = SCORE_HUD_MARGIN_BOTTOM + hudLayout.height;
    hudFont.draw(batch, hudLayout, textX, textY);
  }

  public boolean isZombieArthurContactActive() {
    return zombieArthurContactActive;
  }

  public boolean consumeZombieDefeatByHitEvent() {
    boolean event = zombieDefeatByHitEventPending;
    zombieDefeatByHitEventPending = false;
    return event;
  }

  public int getDefeatedZombieCount() {
    return defeatedZombieCount;
  }

  private void processArthurPunchHit() {

    if (!arthur.isPunchHitWindowPending() || !zombie.isWalking()) {
      return;
    }

    float arthurLeft = arthur.getX();
    float arthurRight = arthurLeft + arthur.getDrawWidth();
    float arthurBottom = arthur.getY();
    float arthurTop = arthurBottom + arthur.getDrawHeightValue();

    float zombieLeft = zombie.getX();
    float zombieRight = zombieLeft + zombie.getDrawWidth();
    float zombieBottom = zombie.getY();
    float zombieTop = zombieBottom + zombie.getDrawHeightValue();

    float verticalGap = distanceBetweenSegments(arthurBottom, arthurTop, zombieBottom, zombieTop);
    if (verticalGap > ARTHUR_PUNCH_VERTICAL_REACH) {
      return;
    }

    float horizontalGap = distanceBetweenSegments(arthurLeft, arthurRight, zombieLeft, zombieRight);

    if (horizontalGap <= ARTHUR_PUNCH_REACH && arthur.consumePunchHitWindow()) {
      zombie.registerValidHit();
    }
  }

  private float distanceBetweenSegments(
      float firstMin, float firstMax, float secondMin, float secondMax) {
    if (secondMin >= firstMax) {
      return secondMin - firstMax;
    }
    if (firstMin >= secondMax) {
      return firstMin - secondMax;
    }
    return 0f;
  }
}
