package com.mo.economy_system.platform.network;

/** Protocol input was malformed, truncated, oversized, or had trailing data. */
public final class WireDecodeException extends IllegalArgumentException {
    public WireDecodeException(String message) {
        super(message);
    }

    public WireDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
