package com.mo.economy_system.ui.text;

import java.util.Locale;

/** Loader-neutral number formatting shared by semantic screens. */
public final class UiNumbers {
    private UiNumbers() {
    }

    /** Formats an integer with locale-stable thousands separators. */
    public static String formatInteger(int value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
