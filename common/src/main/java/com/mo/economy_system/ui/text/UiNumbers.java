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

    /** Formats a long with the same locale-stable thousands separators. */
    public static String formatInteger(long value) {
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

    /** Formats a non-negative duration as a compact, locale-neutral countdown. */
    public static String formatDurationMillis(long durationMillis) {
        long totalSeconds = Math.max(0L, durationMillis) / 1_000L;
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (days > 0L) return String.format(Locale.ROOT, "%dd %02d:%02d:%02d", days, hours, minutes, seconds);
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }
}
