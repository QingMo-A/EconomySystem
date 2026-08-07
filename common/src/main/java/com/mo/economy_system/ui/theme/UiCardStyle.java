package com.mo.economy_system.ui.theme;

public record UiCardStyle(int background, int backgroundHover, int border, int borderHover,
                          int accent, int padding) {
    public UiCardStyle {
        if (padding < 0) throw new IllegalArgumentException("Card padding cannot be negative");
    }
}
