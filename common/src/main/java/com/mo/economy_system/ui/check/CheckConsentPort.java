package com.mo.economy_system.ui.check;

/** Target-owned side effects for a file-check decision. */
public interface CheckConsentPort {
  void allow();

  void decline();
}
