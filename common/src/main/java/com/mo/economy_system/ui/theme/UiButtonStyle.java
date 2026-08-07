package com.mo.economy_system.ui.theme;

public record UiButtonStyle(int accent, int text, int background, int backgroundHover,
                            int border, int borderHover, int stripeWidth, int padding) {
    public UiButtonStyle {
        if (stripeWidth < 0 || padding < 0) {
            throw new IllegalArgumentException("Button dimensions cannot be negative");
        }
    }
}
