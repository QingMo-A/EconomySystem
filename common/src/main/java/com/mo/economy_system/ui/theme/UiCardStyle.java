package com.mo.economy_system.ui.theme;

/**
 * Immutable semantic card style.  Alpha for the accent stripe is kept separate from the RGB
 * colour so renderers can apply hover state without duplicating Territory-specific constants.
 */
public record UiCardStyle(int background, int backgroundHover, int border, int borderHover,
                          int accent, int accentWidth, int accentAlpha, int accentAlphaHover) {
    public UiCardStyle {
        if (accentWidth < 0 || accentAlpha < 0 || accentAlpha > 0xFF
                || accentAlphaHover < 0 || accentAlphaHover > 0xFF) {
            throw new IllegalArgumentException("Invalid card style dimensions or alpha");
        }
    }

    /** Compatibility constructor used by older shared screens. */
    public UiCardStyle(int background, int backgroundHover, int border, int borderHover,
                       int accent, int stripeWidth, int padding) {
        this(background, backgroundHover, border, borderHover, accent, stripeWidth, 0xFF, 0xFF);
    }

    /** Legacy name retained for non-Territory common screens during the migration. */
    public int stripeWidth() {
        return accentWidth;
    }

    /** Card padding is a layout concern; retained as a harmless compatibility value. */
    public int padding() {
        return 0;
    }
}
