package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ui.EconomyUiBridge;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;

/** Forge 1.20.1 renderer/factory for the shared UI route model. */
public final class Forge1201UiBridge implements EconomyUiBridge<Screen> {
  public static final Forge1201UiBridge INSTANCE = new Forge1201UiBridge();

  private Forge1201UiBridge() {}

  @Override
  public Optional<Screen> create(EconomyUiRoute route) {
    return switch (route) {
      case HOME -> Optional.of(new Forge1201HomeScreen());
      case DELIVERY_BOX -> Optional.of(new Forge1201DeliveryBoxScreen());
      case MAIL_COMPOSE -> Optional.of(new Forge1201MailboxComposeScreen(null));
      case TERRITORY -> Optional.of(new Forge1201TerritoryListScreen());
      case SHOP -> Optional.of(new Forge1201ShopScreen());
      case MARKET -> Optional.of(new Forge1201MarketScreen());
      case COMMISSIONS -> Optional.of(new Forge1201CommissionCenterScreen());
      case ABOUT -> Optional.of(new Forge1201AboutScreen());
      case BALANCE_LOG -> Optional.of(new Forge1201BalanceLogScreen());
    };
  }

  @Override
  public boolean supports(EconomyUiRoute route) {
    return route == EconomyUiRoute.HOME
        || route == EconomyUiRoute.DELIVERY_BOX
        || route == EconomyUiRoute.MAIL_COMPOSE
        || route == EconomyUiRoute.TERRITORY
        || route == EconomyUiRoute.SHOP
        || route == EconomyUiRoute.MARKET
        || route == EconomyUiRoute.COMMISSIONS
        || route == EconomyUiRoute.ABOUT
        || route == EconomyUiRoute.BALANCE_LOG;
  }
}
