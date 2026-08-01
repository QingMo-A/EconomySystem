package com.mo.economy_system.platform.item;

/** Stable failure categories for snapshot parsing and target conversion. */
public enum ItemStackSnapshotError {
    INVALID_SCHEMA,
    UNSUPPORTED_SCHEMA_VERSION,
    UNKNOWN_ITEM_ID,
    INVALID_COUNT,
    UNSUPPORTED_COMPONENT,
    LOSSY_COMPONENT_CONVERSION,
    DATA_LIMIT_EXCEEDED,
    DATA_PARSE_FAILED
}
