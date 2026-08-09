package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.Objects;

/** Shared save/discard/close policy for transfer result and terminal screens. */
public final class TransferResultController
    extends AbstractEconomyScreenController<TransferResultState, TransferResultEvent> {
  private static final String FALLBACK_ERROR = "message.transfer.move_failed";
  private final TransferResultPort port;

  public TransferResultController(TransferResultState initial, TransferResultPort port) {
    super(Objects.requireNonNull(initial, "initial"));
    this.port = Objects.requireNonNull(port, "port");
  }

  @Override
  public void handle(TransferResultEvent event) {
    if (event instanceof TransferResultEvent.ArtifactStateChanged changed) {
      replaceState(state().withArtifactStateKey(changed.stateKey()));
      return;
    }
    if (event instanceof TransferResultEvent.ArtifactNoLongerCurrent) {
      if (state().actions().isEmpty()) return;
      port.close();
      finish();
      return;
    }
    if (!(event instanceof TransferResultEvent.ActionClicked clicked)
        || clicked.action() == null
        || !state().can(clicked.action())) {
      return;
    }
    switch (clicked.action()) {
      case SAVE -> apply(port.save());
      case DISCARD -> apply(port.discard());
      case CLOSE -> {
        port.close();
        finish();
      }
    }
  }

  private void apply(TransferResultPort.Outcome outcome) {
    if (outcome == null) {
      replaceState(state().withActionError(FALLBACK_ERROR));
      return;
    }
    if (outcome.close()) {
      port.close();
      finish();
    }
    else replaceState(state().withActionError(outcome.errorKey()));
  }

  private void finish() {
    replaceState(state().finished());
    navigate(new UiNavigation.Back());
  }
}
