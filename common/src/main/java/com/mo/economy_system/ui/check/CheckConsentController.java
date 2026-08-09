package com.mo.economy_system.ui.check;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Objects;
import java.util.Set;

/** Common decision controller; scanning and network dispatch remain target-owned. */
public final class CheckConsentController
    extends AbstractEconomyScreenController<CheckConsentState, CheckConsentEvent> {
  private final CheckConsentPort port;

  public CheckConsentController(String requesterName, String checkTypeId, CheckConsentPort port) {
    super(new CheckConsentState(
        requesterName,
        checkTypeId,
        ScreenState.READY,
        Set.of(CheckConsentAction.ALLOW, CheckConsentAction.DECLINE)));
    this.port = Objects.requireNonNull(port, "port");
  }

  @Override
  public void handle(CheckConsentEvent event) {
    if (!(event instanceof CheckConsentEvent.ActionClicked clicked)
        || clicked.action() == null
        || !state().can(clicked.action())) {
      return;
    }
    if (clicked.action() == CheckConsentAction.ALLOW) {
      port.allow();
    } else {
      port.decline();
    }
    replaceState(new CheckConsentState(
        state().requesterName(), state().checkTypeId(), ScreenState.IDLE, Set.of()));
    navigate(new UiNavigation.Back());
  }
}
