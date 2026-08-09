package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.Objects;
import java.util.Set;

/** Immutable transfer-request details and available consent actions. */
public record TransferConsentState(
    String requesterName,
    String checkTypeId,
    String fileName,
    long byteSize,
    String sha256,
    ScreenState screenState,
    Set<TransferConsentAction> actions) {
  public TransferConsentState {
    requesterName = Objects.requireNonNullElse(requesterName, "");
    checkTypeId = Objects.requireNonNullElse(checkTypeId, "");
    fileName = Objects.requireNonNullElse(fileName, "");
    if (byteSize < 0) throw new IllegalArgumentException("byte size");
    sha256 = Objects.requireNonNullElse(sha256, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public boolean can(TransferConsentAction action) {
    return actions.contains(action);
  }
}
