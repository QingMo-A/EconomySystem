package com.mo.economy_system.ui.transfer;

/** Target-owned transfer side effects requested by the common consent state machine. */
public interface TransferConsentPort {
  void allow();

  void decline();

  void expire();
}
