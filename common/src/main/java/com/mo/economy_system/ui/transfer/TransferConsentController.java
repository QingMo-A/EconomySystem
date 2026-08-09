package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Objects;
import java.util.Set;

/** One-shot decision policy shared by both transfer-consent shells. */
public final class TransferConsentController
    extends AbstractEconomyScreenController<TransferConsentState, TransferConsentEvent> {
  private final TransferConsentPort port;

  public TransferConsentController(
      String requesterName,
      String checkTypeId,
      String fileName,
      long byteSize,
      String sha256,
      TransferConsentPort port) {
    super(
        new TransferConsentState(
            requesterName,
            checkTypeId,
            fileName,
            byteSize,
            sha256,
            ScreenState.READY,
            Set.of(TransferConsentAction.ALLOW, TransferConsentAction.DECLINE)));
    this.port = Objects.requireNonNull(port, "port");
  }

  @Override
  public void handle(TransferConsentEvent event) {
    if (event instanceof TransferConsentEvent.Expired) {
      if (state().actions().isEmpty()) return;
      port.expire();
      finish();
      return;
    }
    if (!(event instanceof TransferConsentEvent.ActionClicked clicked)
        || clicked.action() == null
        || !state().can(clicked.action())) {
      return;
    }
    if (clicked.action() == TransferConsentAction.ALLOW) port.allow();
    else port.decline();
    finish();
  }

  private void finish() {
    replaceState(
        new TransferConsentState(
            state().requesterName(),
            state().checkTypeId(),
            state().fileName(),
            state().byteSize(),
            state().sha256(),
            ScreenState.IDLE,
            Set.of()));
    navigate(new UiNavigation.Back());
  }
}
