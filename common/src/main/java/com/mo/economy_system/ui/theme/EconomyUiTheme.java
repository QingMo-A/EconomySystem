package com.mo.economy_system.ui.theme;

/** Shared visual tokens copied from the NeoForge 1.21.1 baseline. */
public final class EconomyUiTheme {
    public static final int BASE_WIDTH = 640;
    public static final int BASE_HEIGHT = 360;
    public static final int PANEL_PADDING = 12;
    public static final int CARD_SPACING = 8;
    public static final int TERRITORY_ACCENT = 0xFF9B59B6;
    public static final int SHOP_ACCENT = 0xFFFF8C00;
    public static final int MARKET_ACCENT = 0xFF3D9BE9;
    public static final int DELIVERY_ACCENT = 0xFF43B5A0;
    public static final int ABOUT_ACCENT = 0xFFB779E8;
    public static final int LEADERBOARD_ACCENT = 0xFFE0B34C;
    public static final int DISABLED_ACCENT = 0xFF6F7F8C;
    public static final int CARD_BACKGROUND = 0x801A1A2A;
    public static final int CARD_BACKGROUND_HOVER = 0x901A1A2A;
    public static final int CARD_BORDER = 0xFF4A5568;
    public static final int CARD_BORDER_HOVER = 0xFF6A7588;
    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0x90FFFFFF;
    public static final int TEXT_MUTED = 0x80FFFFFF;
    public static final int TEXT_ERROR = 0xFFFF7777;
    public static final int TEXT_SUCCESS = 0xFF9BE7A7;
    public static final int TEXT_LOCKED = 0xFFB0BBC6;
    public static final int HOVER_ANIMATION_MILLIS = 120;
    public static final int PAGE_ANIMATION_MILLIS = 180;

    public static final UiCardStyle TERRITORY_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            TERRITORY_ACCENT, 3, PANEL_PADDING);
    public static final UiCardStyle TERRITORY_LOCKED_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            DISABLED_ACCENT, 3, PANEL_PADDING);
    public static final UiCardStyle HOME_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            MARKET_ACCENT, 3, PANEL_PADDING);
    public static final UiCardStyle HOME_LEADERBOARD_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            LEADERBOARD_ACCENT, 3, PANEL_PADDING);
    public static final UiCardStyle SHOP_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            SHOP_ACCENT, 3, PANEL_PADDING);
    public static final UiCardStyle MARKET_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            MARKET_ACCENT, 3, PANEL_PADDING);
    public static final UiCardStyle DELIVERY_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            DELIVERY_ACCENT, 3, PANEL_PADDING);
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
    public static final UiButtonStyle HOME_SHOP_BUTTON = new UiButtonStyle(
            SHOP_ACCENT, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 10);
    public static final UiButtonStyle HOME_MARKET_BUTTON = new UiButtonStyle(
            MARKET_ACCENT, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 10);
    public static final UiButtonStyle HOME_DELIVERY_BUTTON = new UiButtonStyle(
            DELIVERY_ACCENT, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 10);
    public static final UiButtonStyle HOME_TERRITORY_BUTTON = TERRITORY_BUTTON;
    public static final UiButtonStyle HOME_ABOUT_BUTTON = new UiButtonStyle(
            ABOUT_ACCENT, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 10);
    public static final UiButtonStyle SHOP_BUTTON = new UiButtonStyle(
            SHOP_ACCENT, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 8);
    public static final UiButtonStyle MARKET_BUTTON = new UiButtonStyle(
            MARKET_ACCENT, TEXT_PRIMARY, 0x20FFFFFF, 0x35FFFFFF,
            0x40FFFFFF, 0x60FFFFFF, 4, 8);
    public static final UiButtonStyle TERRITORY_BUFF_UNLOCK_BUTTON = new UiButtonStyle(
            SHOP_ACCENT, TEXT_PRIMARY, 0x55FFFFFF, 0x70FFFFFF,
            0x25FFFFFF, 0x40FFFFFF, 3, 6);
    public static final UiButtonStyle TERRITORY_BUFF_UPGRADE_BUTTON = new UiButtonStyle(
            TERRITORY_ACCENT, TEXT_PRIMARY, 0x55FFFFFF, 0x70FFFFFF,
            0x25FFFFFF, 0x40FFFFFF, 3, 6);
    public static final UiButtonStyle DISABLED_BUTTON = new UiButtonStyle(
            DISABLED_ACCENT, TEXT_LOCKED, 0x30FFFFFF, 0x30FFFFFF,
            0x20FFFFFF, 0x20FFFFFF, 3, 6);

    private EconomyUiTheme() {
    }
}
