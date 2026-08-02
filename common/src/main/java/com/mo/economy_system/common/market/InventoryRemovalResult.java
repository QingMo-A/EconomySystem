package com.mo.economy_system.common.market;

import java.util.Objects;
import java.util.Optional;

public final class InventoryRemovalResult {
  private final boolean succeeded;
  private final boolean failureRestored;
  private final InventoryRemovalRollback rollback;

  private InventoryRemovalResult(
      boolean succeeded, boolean restored, InventoryRemovalRollback rollback) {
    this.succeeded = succeeded;
    this.failureRestored = restored;
    this.rollback = rollback;
  }

  public static InventoryRemovalResult success(InventoryRemovalRollback rollback) {
    return new InventoryRemovalResult(true, true, Objects.requireNonNull(rollback));
  }

  public static InventoryRemovalResult failure(boolean restored) {
    return new InventoryRemovalResult(false, restored, null);
  }

  public boolean succeeded() {
    return succeeded;
  }

  public boolean failureRestored() {
    return failureRestored;
  }

  public Optional<InventoryRemovalRollback> rollback() {
    return Optional.ofNullable(rollback);
  }
}
