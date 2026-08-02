package com.mo.economy_system.common.market;

import java.util.Objects;

public record InventoryInsertionResult(boolean succeeded, boolean failureRestored, InventoryRollback rollback) {
    public InventoryInsertionResult {
        if (succeeded && rollback == null) throw new IllegalArgumentException("successful insertion requires rollback");
        if (!succeeded && rollback != null) throw new IllegalArgumentException("failed insertion cannot expose rollback");
    }
    public static InventoryInsertionResult success(InventoryRollback rollback) {
        return new InventoryInsertionResult(true, true, Objects.requireNonNull(rollback, "rollback"));
    }
    public static InventoryInsertionResult failure(boolean restored) {
        return new InventoryInsertionResult(false, restored, null);
    }
}
