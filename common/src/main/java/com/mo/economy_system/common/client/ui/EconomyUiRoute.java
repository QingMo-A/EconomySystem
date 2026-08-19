package com.mo.economy_system.common.client.ui;

/** Stable, loader-neutral identifiers for EconomySystem client pages. */
public enum EconomyUiRoute {
    HOME("screen.home.title"),
    SHOP("screen.shop.title"),
    MARKET("screen.market.title"),
    DELIVERY_BOX("screen.delivery_box.title"),
    MAIL_COMPOSE("screen.mailbox.compose.title"),
    TERRITORY("screen.territory.title"),
    ABOUT("screen.about.title"),
    BALANCE_LOG("screen.balance_log.title");

    private final String titleKey;

    EconomyUiRoute(String titleKey) {
        this.titleKey = titleKey;
    }

    public String titleKey() {
        return titleKey;
    }
}
