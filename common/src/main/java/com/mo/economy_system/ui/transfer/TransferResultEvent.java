package com.mo.economy_system.ui.transfer;

/** Target lifecycle and pointer events handled by the common transfer-result controller. */
public sealed interface TransferResultEvent
    permits TransferResultEvent.ActionClicked,
        TransferResultEvent.ArtifactNoLongerCurrent,
        TransferResultEvent.ArtifactStateChanged {
  record ActionClicked(TransferResultAction action) implements TransferResultEvent {}

  record ArtifactNoLongerCurrent() implements TransferResultEvent {}

  record ArtifactStateChanged(String stateKey) implements TransferResultEvent {}
}
