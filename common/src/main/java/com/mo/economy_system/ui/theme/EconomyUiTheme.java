package com.mo.economy_system.ui.theme;

/** Shared visual tokens copied from the NeoForge 1.21.1 baseline. */
public final class EconomyUiTheme {
    public static final int BASE_WIDTH = 640;
    public static final int BASE_HEIGHT = 360;
    public static final int PANEL_PADDING = 12;
    public static final int CARD_SPACING = 8;
    public static final int TERRITORY_ACCENT = 0xFF9B59B6;
    public static final int CARD_BACKGROUND = 0x801A1A2A;
    public static final int CARD_BACKGROUND_HOVER = 0x901A1A2A;
    public static final int CARD_BORDER = 0xFF4A5568;
    public static final int CARD_BORDER_HOVER = 0xFF6A7588;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0x90FFFFFF;
    public static final int TEXT_MUTED = 0x80FFFFFF;
    public static final int TEXT_ERROR = 0xFFFF7777;
    public static final int HOVER_ANIMATION_MILLIS = 120;
    public static final int PAGE_ANIMATION_MILLIS = 180;

    public static final UiCardStyle TERRITORY_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            TERRITORY_ACCENT, PANEL_PADDING);
    public static final UiButtonStyle TERRITORY_BUTTON = new UiButtonStyle(
            TERRITORY_ACCENT, 0xFFFFFFFF, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 10);
    public static final UiButtonStyle TERRITORY_PRIMARY_BUTTON = TERRITORY_BUTTON;
    public static final UiButtonStyle TERRITORY_WARN_BUTTON = new UiButtonStyle(
            0xFFE2A93B, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 10);
    public static final UiButtonStyle TERRITORY_NEUTRAL_BUTTON = new UiButtonStyle(
            0xFF4A8ACF, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 10);
    public static final UiButtonStyle TERRITORY_DANGER_BUTTON = new UiButtonStyle(
            0xFFE05D5D, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 10);

    private EconomyUiTheme() {
    }
}
