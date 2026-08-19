package com.mo.economy_system.ui.theme;

/** Shared visual tokens copied from the NeoForge 1.21.1 baseline. */
public final class EconomyUiTheme {
    public static final int BASE_WIDTH = 640;
    public static final int BASE_HEIGHT = 360;
    public static final int PANEL_PADDING = 12;
    public static final int CARD_SPACING = 8;
    public static final int TERRITORY_ACCENT = 0xFF9B59B6;
    public static final int VERSION_ACCENT = 0xFF4FC3F7;
    public static final int SHOP_ACCENT = 0xFFFF8C00;
    public static final int MARKET_ACCENT = 0xFF4FC3F7;
    public static final int DELIVERY_ACCENT = 0xFF27AE60;
    public static final int ABOUT_ACCENT = 0xFF888888;
    public static final int BALANCE_ACCENT = 0xFFFFD700;
    public static final int LEADERBOARD_ACCENT = 0xFF1ABC9C;
    /** Home reference accents; kept separate from tokens consumed by other screens. */
    public static final int HOME_NAV_SHOP_ACCENT = 0xFFFF8C00;
    public static final int HOME_NAV_MARKET_ACCENT = 0xFF4FC3F7;
    public static final int HOME_NAV_DELIVERY_ACCENT = 0xFF27AE60;
    public static final int HOME_NAV_TERRITORY_ACCENT = 0xFF9B59B6;
    public static final int HOME_NAV_ABOUT_ACCENT = 0xFF888888;
    public static final int HOME_BALANCE_ACCENT = 0xFFFFD700;
    public static final int HOME_TRADE_ACCENT = 0xFF4FC3F7;
    public static final int HOME_LEADERBOARD_ACCENT = 0xFF1ABC9C;
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
    /** Global native-input policy; page-specific frame accents remain in the existing frame tokens. */
    public static final UiInputStyle INPUT_STYLE = new UiInputStyle(
            TEXT_PRIMARY, TEXT_LOCKED, TEXT_MUTED, false, true);
    /** Legacy shop tooltip price delta tones (ChatFormatting.RED/GREEN/GRAY). */
    public static final int TOOLTIP_PRICE_UP = 0xFFFF5555;
    public static final int TOOLTIP_PRICE_DOWN = 0xFF55FF55;
    public static final int TOOLTIP_PRICE_SAME = 0xFFAAAAAA;
    public static final int HOVER_ANIMATION_MILLIS = 120;
    public static final int PAGE_ANIMATION_MILLIS = 180;

    public static final UiCardStyle TERRITORY_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            TERRITORY_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle VERSION_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            VERSION_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle TERRITORY_LOCKED_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            DISABLED_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle HOME_BALANCE_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            HOME_BALANCE_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle HOME_TRADE_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            HOME_TRADE_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle HOME_LEADERBOARD_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            HOME_LEADERBOARD_ACCENT, 3, 0xCC, 0xFF);
    /** Compatibility alias retained for screens compiled against the initial common Home pass. */
    public static final UiCardStyle HOME_CARD = HOME_TRADE_CARD;
    public static final UiCardStyle SHOP_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            SHOP_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle MARKET_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            MARKET_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle DELIVERY_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            DELIVERY_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle BALANCE_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            BALANCE_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle ABOUT_CARD = new UiCardStyle(
            CARD_BACKGROUND, CARD_BACKGROUND_HOVER, CARD_BORDER, CARD_BORDER_HOVER,
            ABOUT_ACCENT, 3, 0xCC, 0xFF);
    public static final UiCardStyle SHOP_PURCHASE_PANEL = new UiCardStyle(
            0xB01A2A3A, 0xB01A2A3A, 0xFF4A8ACF, 0xFF4A8ACF,
            0xFF4A8ACF, 3, 0xFF, 0xFF);
    public static final UiButtonStyle TERRITORY_BUTTON = actionButton(TERRITORY_ACCENT);
    public static final UiButtonStyle TERRITORY_PRIMARY_BUTTON = TERRITORY_BUTTON;
    public static final UiButtonStyle TERRITORY_WARN_BUTTON = actionButton(0xFFE2A93B);
    public static final UiButtonStyle TERRITORY_NEUTRAL_BUTTON = actionButton(0xFF4A8ACF);
    public static final UiButtonStyle TERRITORY_DANGER_BUTTON = actionButton(0xFFE05D5D);
    public static final UiButtonStyle HOME_NAV_SHOP_STYLE = homeNavButton(HOME_NAV_SHOP_ACCENT);
    public static final UiButtonStyle HOME_NAV_MARKET_STYLE = homeNavButton(HOME_NAV_MARKET_ACCENT);
    public static final UiButtonStyle HOME_NAV_DELIVERY_STYLE = homeNavButton(HOME_NAV_DELIVERY_ACCENT);
    public static final UiButtonStyle HOME_NAV_TERRITORY_STYLE = homeNavButton(HOME_NAV_TERRITORY_ACCENT);
    public static final UiButtonStyle HOME_NAV_ABOUT_STYLE = homeNavButton(HOME_NAV_ABOUT_ACCENT);
    /** Compatibility names retained for target code outside this parity pass. */
    public static final UiButtonStyle HOME_SHOP_BUTTON = HOME_NAV_SHOP_STYLE;
    public static final UiButtonStyle HOME_MARKET_BUTTON = HOME_NAV_MARKET_STYLE;
    public static final UiButtonStyle HOME_DELIVERY_BUTTON = HOME_NAV_DELIVERY_STYLE;
    public static final UiButtonStyle HOME_TERRITORY_BUTTON = HOME_NAV_TERRITORY_STYLE;
    public static final UiButtonStyle HOME_ABOUT_BUTTON = HOME_NAV_ABOUT_STYLE;
    public static final UiButtonStyle SHOP_BUTTON = actionButton(SHOP_ACCENT, 8);
    public static final UiButtonStyle MARKET_BUTTON = compactActionButton(MARKET_ACCENT, 8);
    public static final UiButtonStyle MARKET_TOP_SALES_BUTTON = topButton(DELIVERY_ACCENT);
    public static final UiButtonStyle MARKET_TOP_DEMAND_BUTTON = topButton(SHOP_ACCENT);
    public static final UiButtonStyle MARKET_ACTION_BUY = compactActionButton(MARKET_ACCENT, 6);
    public static final UiButtonStyle MARKET_ACTION_REMOVE = compactActionButton(0xFFE05D5D, 6);
    public static final UiButtonStyle MARKET_ACTION_DELIVER = compactActionButton(SHOP_ACCENT, 6);
    public static final UiButtonStyle MARKET_ACTION_CONFIRM = compactActionButton(DELIVERY_ACCENT, 6);
    public static final UiButtonStyle MARKET_ACTION_CANCEL = compactActionButton(0xFFE05D5D, 6);
    public static final UiButtonStyle MARKET_ACTION_DISABLED = disabledActionButton();
    public static final UiButtonStyle DELIVERY_CLAIM_BUTTON = compactActionButton(DELIVERY_ACCENT, 6);
    public static final UiButtonStyle BALANCE_BUTTON = actionButton(BALANCE_ACCENT, 6);
    public static final UiButtonStyle TERRITORY_BUFF_UNLOCK_BUTTON = actionButton(SHOP_ACCENT, 6);
    public static final UiButtonStyle TERRITORY_BUFF_UPGRADE_BUTTON = actionButton(TERRITORY_ACCENT, 6);
    public static final UiButtonStyle DISABLED_BUTTON = new UiButtonStyle(
            DISABLED_ACCENT & 0x00FFFFFF, TEXT_LOCKED, 0xFFFFFF, 0x30, 0x30,
            3, 0, 0, 0, 0, 0, 0x20, 0x20, 6, false,
            com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);

    /** Standalone primitive used by pagination; it intentionally has no action stripe/glow. */
    public static final UiButtonStyle PAGE_BUTTON = new UiButtonStyle(
            0x4A8ACF, TEXT_PRIMARY, 0x3A7ABF, 0xB0, 0xD0,
            0, 0, 0, 0, 0, 0, 0x4A, 0x6A, 2, false,
            com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);
    public static final UiButtonStyle PAGE_BUTTON_DISABLED = new UiButtonStyle(
            0x3A3A4A, 0x60808080, 0x2A2A3A, 0x60, 0x60,
            0, 0, 0, 0, 0, 0, 0x3A, 0x3A, 2, false,
            com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);

    /** Legacy Shop page controls use the market-blue accent and real arrow textures. */
    public static final UiButtonStyle SHOP_PAGE_BUTTON = pageButton(0x4FC3F7);
    public static final UiButtonStyle SHOP_PAGE_BUTTON_DISABLED = disabledPageButton();
    /** Legacy Market page controls use textual angle labels and market-blue chrome. */
    public static final UiButtonStyle MARKET_PAGE_BUTTON = pageButton(MARKET_ACCENT);
    public static final UiButtonStyle MARKET_PAGE_BUTTON_DISABLED = disabledPageButton();
    /** Legacy Delivery page controls use the shop-orange accent. */
    public static final UiButtonStyle DELIVERY_PAGE_BUTTON = pageButton(SHOP_ACCENT);
    public static final UiButtonStyle DELIVERY_PAGE_BUTTON_DISABLED = disabledPageButton();
    /** Balance log tabs and paging use the gold balance theme. */
    public static final UiButtonStyle BALANCE_BUTTON_DISABLED = disabledPageButton();

    /** Transparent inputs use a quiet underline that changes to the page accent on focus/hover. */
    public static final UiInputFrameStyle SHOP_SEARCH_FRAME = UiInputFrameStyle.underline(
            CARD_BORDER, MARKET_ACCENT);
    public static final UiInputFrameStyle MARKET_SEARCH_FRAME = SHOP_SEARCH_FRAME;
    public static final UiInputFrameStyle INPUT_ERROR_FRAME = UiInputFrameStyle.underline(
            TEXT_ERROR, TEXT_ERROR);
    public static final UiInputFrameStyle DELIVERY_SEARCH_FRAME = UiInputFrameStyle.underline(
            CARD_BORDER, 0xFFFFB74D);
    /** Territory inputs use the same transparent treatment with a purple focused underline. */
    public static final UiInputFrameStyle TERRITORY_SEARCH_FRAME = UiInputFrameStyle.underline(
            CARD_BORDER, TERRITORY_ACCENT);
    /** Territory list/detail/buff/invite paging uses the legacy purple action chrome. */
    public static final UiButtonStyle TERRITORY_PAGE_BUTTON = pageButton(TERRITORY_ACCENT);
    public static final UiButtonStyle TERRITORY_PAGE_BUTTON_DISABLED = disabledPageButton();

    private static UiButtonStyle actionButton(int accent) {
        return actionButton(accent, 8);
    }

    private static UiButtonStyle actionButton(int accent, int padding) {
        return new UiButtonStyle(accent & 0x00FFFFFF, TEXT_PRIMARY, 0x000000,
                0x55, 0x70, 4, 0xCC, 0xFF, 6, 36, 4,
                0x25, 0x40, padding, false,
                com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);
    }

    /** Compact market/delivery actions use the three-pixel stripe and four glow rows. */
    private static UiButtonStyle compactActionButton(int accent, int padding) {
        return new UiButtonStyle(accent & 0x00FFFFFF, TEXT_PRIMARY, 0x000000,
                0x55, 0x70, 3, 0xCC, 0xFF, 4, 36, 4,
                0x25, 0x40, padding, false,
                com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);
    }

    private static UiButtonStyle topButton(int accent) {
        return new UiButtonStyle(accent & 0x00FFFFFF, TEXT_PRIMARY, 0x000000,
                0x55, 0x70, 4, 0xCC, 0xFF, 7, 36, 4,
                0x25, 0x40, 10, false,
                com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);
    }

    private static UiButtonStyle disabledActionButton() {
        return new UiButtonStyle(DISABLED_ACCENT & 0x00FFFFFF, TEXT_LOCKED, 0x000000,
                0x30, 0x30, 3, 0x50, 0x50, 0, 0, 0, 0x20, 0x20, 6, false,
                com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);
    }

    private static UiButtonStyle pageButton(int accent) {
        return new UiButtonStyle(accent & 0x00FFFFFF, TEXT_PRIMARY, 0x000000,
                0x55, 0x70, 3, 0xCC, 0xFF, 4, 36, 4,
                0x25, 0x40, 6, false,
                com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);
    }

    private static UiButtonStyle disabledPageButton() {
        return new UiButtonStyle(DISABLED_ACCENT & 0x00FFFFFF, TEXT_LOCKED, 0x000000,
                0x30, 0x30, 3, 0x50, 0x50, 0, 0, 0, 0x20, 0x20, 6, false,
                com.mo.economy_system.ui.renderer.UiTextAlignment.CENTER);
    }

    /** Home navigation style copied from the NeoForge 1.21.1 reference renderer. */
    public static UiButtonStyle homeNavButton(int accent) {
        return new UiButtonStyle(accent & 0x00FFFFFF, TEXT_PRIMARY, 0x000000,
                0x99, 0xAA, 4, 0xCC, 0xFF, 6, 36, 4,
                0x20, 0x40, 10, true,
                com.mo.economy_system.ui.renderer.UiTextAlignment.LEFT);
    }

    private EconomyUiTheme() {
    }
}
