package com.mo.economy_system.ui.theme;

import com.mo.economy_system.ui.renderer.UiTextAlignment;

/**
 * Immutable semantic button style.  Renderers consume the RGB/alpha fields and own the actual
 * draw calls, keeping common UI definitions independent from either Minecraft loader.
 */
public record UiButtonStyle(
        int accent,
        int textColor,
        int backgroundRgb,
        int backgroundAlpha,
        int backgroundAlphaHover,
        int stripeWidth,
        int stripeAlpha,
        int stripeAlphaHover,
        int glowHeight,
        int glowAlphaStart,
        int glowAlphaStep,
        int borderAlpha,
        int borderAlphaHover,
        int padding,
        boolean textShadow,
        UiTextAlignment alignment) {

    public UiButtonStyle {
        if (backgroundAlpha < 0 || backgroundAlpha > 0xFF
                || backgroundAlphaHover < 0 || backgroundAlphaHover > 0xFF
                || stripeWidth < 0 || stripeAlpha < 0 || stripeAlpha > 0xFF
                || stripeAlphaHover < 0 || stripeAlphaHover > 0xFF
                || glowHeight < 0 || glowAlphaStart < 0 || glowAlphaStart > 0xFF
                || glowAlphaStep < 0 || borderAlpha < 0 || borderAlpha > 0xFF
                || borderAlphaHover < 0 || borderAlphaHover > 0xFF || padding < 0
                || alignment == null) {
            throw new IllegalArgumentException("Invalid button style dimensions or alpha");
        }
    }

    /**
     * Compatibility constructor for pre-parity styles that supplied packed ARGB colours.
     * Defaults match the old striped-button primitive while making alpha explicit.
     */
    public UiButtonStyle(int accent, int text, int background, int backgroundHover,
                         int border, int borderHover, int stripeWidth, int padding) {
        this(accent & 0x00FFFFFF, text, background & 0x00FFFFFF, (background >>> 24) & 0xFF,
                (backgroundHover >>> 24) & 0xFF, stripeWidth, 0xCC, 0xFF,
                0, 0, 0, (border >>> 24) & 0xFF, (borderHover >>> 24) & 0xFF,
                padding, false, UiTextAlignment.CENTER);
    }

    /** Packed colour aliases retained for screens outside this parity pass. */
    public int text() {
        return textColor;
    }

    public int background() {
        return withAlpha(backgroundRgb, backgroundAlpha);
    }

    public int backgroundHover() {
        return withAlpha(backgroundRgb, backgroundAlphaHover);
    }

    public int border() {
        return withAlpha(0xFFFFFF, borderAlpha);
    }

    public int borderHover() {
        return withAlpha(0xFFFFFF, borderAlphaHover);
    }

    public int accentColor(int alpha) {
        return withAlpha(accent, alpha);
    }

    /** Packed border colour used by standalone outline controls as well as striped actions. */
    public int borderColor(boolean hovered, boolean enabled) {
        if (stripeWidth == 0 && glowHeight == 0) {
            if (!enabled) return 0xFF3A3A4A;
            return hovered ? 0xFF6AB8FF : 0xFF4A8ACF;
        }
        if (stripeWidth == 0) {
            int alpha = hovered && enabled ? borderAlphaHover : borderAlpha;
            if (!enabled) alpha = Math.min(alpha, 0x40);
            return withAlpha(accent, alpha);
        }
        return hovered && enabled ? borderHover() : border();
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }
}
