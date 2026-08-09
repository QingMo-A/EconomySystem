package com.mo.economy_system.ui.about;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.UiNavigation;

/** Common About-page interaction; target owns clipboard and textures. */
public final class AboutController extends AbstractEconomyScreenController<AboutState, AboutEvent> {
  private final AboutPort port;

  public AboutController(AboutPort port) {
    this(port, "Economy System", "QingMo HanHanYu", "https://github.com/QingMo-A/EconomySystem");
  }

  public AboutController(AboutPort port, String modName, String author, String githubUrl) {
    super(new AboutState(modName, author, githubUrl, false));
    this.port = java.util.Objects.requireNonNull(port, "port");
  }

  @Override public void handle(AboutEvent event) {
    if (event instanceof AboutEvent.ActionClicked value) {
      if (value.action() == AboutAction.BACK) {
        navigate(new UiNavigation.Route(EconomyUiRoute.HOME));
      } else if (value.action() == AboutAction.COPY_GITHUB) {
        port.copyToClipboard(state().githubUrl());
        replaceState(new AboutState(state().modName(), state().author(), state().githubUrl(), true));
      }
    }
  }
}
