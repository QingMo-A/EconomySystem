package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ui.EconomyUiBridge;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.economy_system.deliver_box.Screen_DeliveryBox;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;

/** Forge 1.20.1 renderer/factory for the shared UI route model. */
public final class Forge1201UiBridge implements EconomyUiBridge<Screen> {
  public static final Forge1201UiBridge INSTANCE = new Forge1201UiBridge();

  private Forge1201UiBridge() {}

  @Override
  public Optional<Screen> create(EconomyUiRoute route) {
    return switch (route) {
      case HOME -> Optional.of(new Screen_Home());
      case DELIVERY_BOX -> Optional.of(new Screen_DeliveryBox());
      case TERRITORY -> Optional.of(new Screen_Territory());
      case SHOP, MARKET, ABOUT, BALANCE_LOG -> Optional.empty();
    };
  }

  @Override
  public boolean supports(EconomyUiRoute route) {
    return route == EconomyUiRoute.HOME
        || route == EconomyUiRoute.DELIVERY_BOX
        || route == EconomyUiRoute.TERRITORY;
  }
}
