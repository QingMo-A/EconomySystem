package com.mo.economy_system.ui.about;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.core.UiNavigation;
import org.junit.jupiter.api.Test;

class AboutControllerTest {
  @Test
  void copiesGithubAndNavigatesHome() {
    FakePort port = new FakePort();
    AboutController controller = new AboutController(port, "Mod", "Author", "https://example.test");
    controller.handle(new AboutEvent.ActionClicked(AboutAction.COPY_GITHUB));
    assertEquals("https://example.test", port.copied);
    assertTrue(controller.state().copied());
    controller.handle(new AboutEvent.ActionClicked(AboutAction.BACK));
    assertEquals(EconomyUiRoute.HOME,
        ((UiNavigation.Route) controller.pollNavigation().orElseThrow()).route());
  }

  private static final class FakePort implements AboutPort {
    private String copied;
    @Override public void copyToClipboard(String value) { copied = value; }
  }
}
