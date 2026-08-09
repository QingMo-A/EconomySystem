package com.mo.economy_system.ui.text;

import java.util.Objects;

/** Pure text helpers shared by common UI layouts and views. */
public final class UiText {
    private UiText() {
    }

    /**
     * Truncates text to a pixel width while preserving as much of the leading text as possible.
     * An ellipsis is appended only when the complete text does not fit.
     */
    public static String truncate(UiTextMetrics metrics, String text, int maxWidth) {
        Objects.requireNonNull(metrics, "metrics");
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return maxWidth <= 0 ? "" : (text == null ? "" : text);
        }
        if (metrics.width(text) <= maxWidth) {
            return text;
        }
        final String suffix = "...";
        int suffixWidth = metrics.width(suffix);
        if (suffixWidth > maxWidth) {
            return "";
        }
        int prefixWidth = maxWidth - suffixWidth;
        int end = text.length();
        while (end > 0 && metrics.width(text.substring(0, end)) > prefixWidth) {
            end--;
        }
        return text.substring(0, end) + suffix;
    }
}
