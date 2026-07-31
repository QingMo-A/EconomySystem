package com.mo.economy_system.platform.item;

import java.util.Objects;
import java.util.Optional;

/** A success value or an explicit, stable snapshot error. */
public final class ItemStackSnapshotResult<T> {
    private final T value;
    private final ItemStackSnapshotError error;
    private final String detail;

    private ItemStackSnapshotResult(T value, ItemStackSnapshotError error, String detail) {
        this.value = value;
        this.error = error;
        this.detail = detail == null ? "" : detail;
    }

    public static <T> ItemStackSnapshotResult<T> success(T value) {
        return new ItemStackSnapshotResult<>(Objects.requireNonNull(value, "value"), null, "");
    }

    public static <T> ItemStackSnapshotResult<T> failure(ItemStackSnapshotError error, String detail) {
        return new ItemStackSnapshotResult<>(null, Objects.requireNonNull(error, "error"), detail);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public T orElseThrow() {
        if (!isSuccess()) {
            throw new IllegalStateException(error + (detail.isEmpty() ? "" : ": " + detail));
        }
        return value;
    }

    public Optional<ItemStackSnapshotError> error() {
        return Optional.ofNullable(error);
    }

    public String detail() {
        return detail;
    }
}
