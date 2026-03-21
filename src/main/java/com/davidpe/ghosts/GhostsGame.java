package com.davidpe.ghosts;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GhostsGame extends ApplicationAdapter {

  public static final float WORLD_WIDTH = 800;
  public static final float WORLD_HEIGHT = 600;

  // Spritesheet layout: 8 columns, 5 rows
  private static final int FRAME_COLS = 8;
  private static final int FRAME_ROWS = 5;

  // Arthur draw size on screen
  private static final float ARTHUR_DRAW_HEIGHT = 120f;
  private static final float GROUND_Y = 130f;
  private static final float MOVE_SPEED = 220f;
  private static final float JUMP_VELOCITY = 520f;
  private static final float GRAVITY = 1250f;

  private enum MovementState {
    IDLE,
    WALK,
    CROUCH,
    JUMP
  }

  private SpriteBatch batch;
  private OrthographicCamera camera;
  private Viewport viewport;

  private Texture background;
  private Texture arthurSheet;
  private TextureRegion arthurFrame;
  private MovementState movementState;
  private float arthurX;
  private float arthurY;
  private float arthurVelocityY;

  @Override
  public void create() {
    camera = new OrthographicCamera();
    viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
    camera.position.set(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0);
    camera.update();

    batch = new SpriteBatch();

    background = new Texture(Gdx.files.internal("main-backgroud-1.png"));

    // Load spritesheet, remove black background, and extract first frame
    arthurSheet = loadWithTransparentBlack("sprites_arthur.png");
    int frameWidth = arthurSheet.getWidth() / FRAME_COLS;
    int frameHeight = arthurSheet.getHeight() / FRAME_ROWS;
    arthurFrame = new TextureRegion(arthurSheet, 0, 0, frameWidth, frameHeight);

    float aspectRatio = (float) arthurFrame.getRegionWidth() / arthurFrame.getRegionHeight();
    float drawWidth = ARTHUR_DRAW_HEIGHT * aspectRatio;
    arthurX = (WORLD_WIDTH - drawWidth) / 2f;
    arthurY = GROUND_Y;
    arthurVelocityY = 0f;
    movementState = MovementState.IDLE;
  }

  @Override
  public void render() {
    ScreenUtils.clear(0, 0, 0, 1);
    updateMovementState(Gdx.graphics.getDeltaTime());

    camera.update();
    batch.setProjectionMatrix(camera.combined);

    batch.begin();

    // Draw background scaled to fill the viewport
    batch.draw(background, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

    // Draw Arthur centered horizontally, positioned on the path
    float aspectRatio = (float) arthurFrame.getRegionWidth() / arthurFrame.getRegionHeight();
    float drawWidth = ARTHUR_DRAW_HEIGHT * aspectRatio;
    batch.draw(arthurFrame, arthurX, arthurY, drawWidth, ARTHUR_DRAW_HEIGHT);

    batch.end();
  }

  /**
   * Transition priority:
   * 1) JUMP while airborne or jump triggered
   * 2) CROUCH when grounded and down key held
   * 3) WALK when grounded and horizontal input held
   * 4) IDLE otherwise
   */
  private void updateMovementState(float delta) {
    boolean leftPressed = Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A);
    boolean rightPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
    boolean downPressed = Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S);
    boolean jumpPressed =
        Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.UP);
    boolean isOnGround = arthurY <= GROUND_Y;

    if (jumpPressed && isOnGround) {
      arthurVelocityY = JUMP_VELOCITY;
      movementState = MovementState.JUMP;
    }

    if (!isOnGround || movementState == MovementState.JUMP) {
      arthurVelocityY -= GRAVITY * delta;
      arthurY += arthurVelocityY * delta;
      if (arthurY <= GROUND_Y) {
        arthurY = GROUND_Y;
        arthurVelocityY = 0f;
      } else {
        movementState = MovementState.JUMP;
      }
    }

    boolean groundedAfterPhysics = arthurY <= GROUND_Y;
    if (groundedAfterPhysics) {
      if (downPressed) {
        movementState = MovementState.CROUCH;
      } else if (leftPressed ^ rightPressed) {
        movementState = MovementState.WALK;
      } else {
        movementState = MovementState.IDLE;
      }
    }

    if (movementState == MovementState.WALK) {
      float direction = rightPressed ? 1f : -1f;
      arthurX += direction * MOVE_SPEED * delta;

      float aspectRatio = (float) arthurFrame.getRegionWidth() / arthurFrame.getRegionHeight();
      float drawWidth = ARTHUR_DRAW_HEIGHT * aspectRatio;
      arthurX = Math.max(0f, Math.min(arthurX, WORLD_WIDTH - drawWidth));
    }
  }

  @Override
  public void resize(int width, int height) {
    viewport.update(width, height);
  }

  /** Loads a texture and converts near-black pixels to transparent. */
  private Texture loadWithTransparentBlack(String path) {
    Pixmap original = new Pixmap(Gdx.files.internal(path));
    // Disable blending so drawPixel overwrites instead of blending
    original.setBlending(Pixmap.Blending.None);
    int threshold = 30;
    for (int y = 0; y < original.getHeight(); y++) {
      for (int x = 0; x < original.getWidth(); x++) {
        int pixel = original.getPixel(x, y);
        int r = (pixel >>> 24) & 0xFF;
        int g = (pixel >>> 16) & 0xFF;
        int b = (pixel >>> 8) & 0xFF;
        if (r < threshold && g < threshold && b < threshold) {
          original.drawPixel(x, y, 0x00000000);
        }
      }
    }
    Texture tex = new Texture(original);
    original.dispose();
    return tex;
  }

  @Override
  public void dispose() {
    batch.dispose();
    background.dispose();
    arthurSheet.dispose();
  }
}
