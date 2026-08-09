package com.mo.economy_system.ui.transfer;

import java.util.Objects;

/** Target-owned persistence and close operations requested by the common result controller. */
public interface TransferResultPort {
  Outcome save();

  Outcome discard();

  void close();

  record Outcome(boolean close, String errorKey) {
    public Outcome {
      if (!close) {
        errorKey = Objects.requireNonNull(errorKey, "errorKey");
        if (errorKey.isBlank()) throw new IllegalArgumentException("errorKey");
      } else if (errorKey != null) {
        throw new IllegalArgumentException("closed outcome cannot carry an error");
      }
    }

    public static Outcome closed() {
      return new Outcome(true, null);
    }

    public static Outcome failed(String errorKey) {
      return new Outcome(false, errorKey);
    }
  }
}
