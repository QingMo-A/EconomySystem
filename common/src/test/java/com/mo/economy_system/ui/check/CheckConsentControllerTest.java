package com.mo.economy_system.ui.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import org.junit.jupiter.api.Test;

class CheckConsentControllerTest {
  @Test
  void dispatchesExactlyOneDecisionAndNavigatesBack() {
    FakePort port = new FakePort();
    CheckConsentController controller = new CheckConsentController("requester", "mods", port);

    assertEquals(ScreenState.READY, controller.state().screenState());
    assertTrue(controller.state().can(CheckConsentAction.ALLOW));
    controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.ALLOW));
    controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.DECLINE));

    assertEquals(1, port.allowed);
    assertEquals(0, port.declined);
    assertFalse(controller.state().can(CheckConsentAction.ALLOW));
    assertTrue(controller.pollNavigation().orElseThrow() instanceof UiNavigation.Back);
  }

  @Test
  void declineUsesTheSameOneShotTransition() {
    FakePort port = new FakePort();
    CheckConsentController controller = new CheckConsentController("requester", "mods", port);

    controller.handle(new CheckConsentEvent.ActionClicked(CheckConsentAction.DECLINE));

    assertEquals(0, port.allowed);
    assertEquals(1, port.declined);
    assertEquals(ScreenState.IDLE, controller.state().screenState());
  }

  private static final class FakePort implements CheckConsentPort {
    private int allowed;
    private int declined;

    @Override
    public void allow() {
      allowed++;
    }

    @Override
    public void decline() {
      declined++;
    }
  }
}
