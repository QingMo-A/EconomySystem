package com.mo.economy_system.core.economy_system;

/**
 * Loader-neutral description of one balance mutation.
 *
 * <p>The target persistence and network layers own their respective codecs;
 * this record deliberately contains no Minecraft or loader types.</p>
 */
public record BalanceLogEntry(
        long timeMillis,
        String category,
        String reason,
        int delta,
        int beforeBalance,
        int afterBalance
) {
}
