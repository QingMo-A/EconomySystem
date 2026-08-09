package com.mo.economy_system.ui.transfer;

/** Target lifecycle and pointer events translated into the common consent controller. */
public sealed interface TransferConsentEvent
    permits TransferConsentEvent.ActionClicked, TransferConsentEvent.Expired {
  record ActionClicked(TransferConsentAction action) implements TransferConsentEvent {}

  record Expired() implements TransferConsentEvent {}
}
