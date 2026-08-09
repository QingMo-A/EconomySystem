package com.mo.economy_system.ui.theme;

public record UiCardStyle(int background, int backgroundHover, int border, int borderHover,
                          int accent, int stripeWidth, int padding) {
    public UiCardStyle {
        if (stripeWidth < 0 || padding < 0) {
            throw new IllegalArgumentException("Card dimensions cannot be negative");
        }
    }
}
