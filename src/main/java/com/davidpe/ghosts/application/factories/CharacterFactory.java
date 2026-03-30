package com.davidpe.ghosts.application.factories;

import com.davidpe.ghosts.domain.characters.Arthur;
import com.davidpe.ghosts.domain.utils.AnimationUtils;

/**
 * Factory responsible for creating game character instances with their required dependencies
 * already wired. Receives shared services (e.g. {@link AnimationUtils}) once at construction time
 * and hides that wiring from callers, which only need to supply game-context parameters such as
 * world dimensions.
 */
public class CharacterFactory {

  private final AnimationUtils animationUtils;

  public CharacterFactory(AnimationUtils animationUtils) {
    this.animationUtils = animationUtils;
  }

  public Arthur createArthur(float worldWidth) {
    return new Arthur(worldWidth, animationUtils);
  }
}
