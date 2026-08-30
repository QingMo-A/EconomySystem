package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ui.EconomyUiBridge;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;

/** NeoForge 1.21.1 page factory; this target is the UI behavior baseline. */
public final class NeoForge1211UiBridge implements EconomyUiBridge<Screen> {
    public static final NeoForge1211UiBridge INSTANCE = new NeoForge1211UiBridge();

    private NeoForge1211UiBridge() {
    }

    @Override
    public Optional<Screen> create(EconomyUiRoute route) {
        return switch (route) {
            case HOME -> Optional.of(new NeoForge1211HomeScreen());
            case SHOP -> Optional.of(new NeoForge1211ShopScreen());
            case MARKET -> Optional.of(new NeoForge1211MarketScreen());
            case COMMISSIONS -> Optional.of(new NeoForge1211CommissionCenterScreen());
            case DELIVERY_BOX -> Optional.of(new NeoForge1211DeliveryBoxScreen());
            case MAIL_COMPOSE -> Optional.of(new NeoForge1211MailboxComposeScreen(null));
            case TERRITORY -> Optional.of(new NeoForge1211TerritoryListScreen());
            case ABOUT -> Optional.of(new NeoForge1211AboutScreen());
            case BALANCE_LOG -> Optional.of(new NeoForge1211BalanceLogScreen());
        };
    }

    @Override
    public boolean supports(EconomyUiRoute route) {
        return true;
    }
}
