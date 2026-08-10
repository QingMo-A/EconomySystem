package com.mo.economy_system.ui.text;

import java.util.Locale;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Loader-neutral number formatting shared by semantic screens. */
public final class UiNumbers {
    private static final DateTimeFormatter BALANCE_LOG_TIME =
        DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private UiNumbers() {
    }

    /** Formats an integer with locale-stable thousands separators. */
    public static String formatInteger(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** Legacy market compact formatter (implemented in the strict UI closure). */
    public static String formatLegacyMarketNumber(int value) {
        if (value >= 10_000) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0d);
        }
        return Integer.toString(value);
    }

    /** Formats balance-log timestamps exactly as the legacy table. */
    public static String formatTimestamp(long epochMillis) {
        return BALANCE_LOG_TIME.format(Instant.ofEpochMilli(epochMillis));
    }
}
