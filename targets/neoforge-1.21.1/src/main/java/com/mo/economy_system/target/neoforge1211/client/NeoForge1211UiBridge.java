package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ui.EconomyUiBridge;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.screen.Screen_About;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.economy_system.deliver_box.Screen_DeliveryBox;
import com.mo.economy_system.screen.economy_system.logs.Screen_BalanceLog;
import com.mo.economy_system.screen.economy_system.market.Screen_Market;
import com.mo.economy_system.screen.economy_system.shop.Screen_Shop;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
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
            case HOME -> Optional.of(new Screen_Home());
            case SHOP -> Optional.of(new Screen_Shop());
            case MARKET -> Optional.of(new Screen_Market());
            case DELIVERY_BOX -> Optional.of(new Screen_DeliveryBox());
            case TERRITORY -> Optional.of(new Screen_Territory());
            case ABOUT -> Optional.of(new Screen_About());
            case BALANCE_LOG -> Optional.of(new Screen_BalanceLog());
        };
    }

    @Override
    public boolean supports(EconomyUiRoute route) {
        return true;
    }
}
