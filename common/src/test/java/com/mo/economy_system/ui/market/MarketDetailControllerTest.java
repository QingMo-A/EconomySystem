package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.MarketOrderSnapshot;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarketDetailControllerTest {
  @Test
  void salesPartialPurchaseRecalculatesAmountAndUsesClientPreflight() {
    CapturingPort port = new CapturingPort();
    MarketRow row = new MarketRow(MarketTestFixtures.order(
        2, MarketOrderType.SALES, new UUID(9, 1), false)); // qty 3, total 60
    MarketDetailController controller = new MarketDetailController(
        row, MarketTestFixtures.VIEWER, false, port);

    controller.handle(new MarketDetailEvent.FactsChanged(100, 2, 0, 100));
    controller.handle(new MarketDetailEvent.QuantityChanged(2));

    assertEquals(20, controller.state().exactUnitPrice());
    assertEquals(40, controller.state().amount());
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertTrue(controller.state().can(MarketDetailAction.SUBMIT_PRIMARY));

    controller.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.SUBMIT_PRIMARY));
    assertEquals(MarketAction.BUY, port.action);
    assertEquals(2, port.quantity);

    controller.handle(new MarketDetailEvent.FactsChanged(30, 2, 0, 30));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.market.detail.insufficient_balance", controller.state().errorKey());
    assertFalse(controller.state().can(MarketDetailAction.SUBMIT_PRIMARY));
  }

  @Test
  void selectAllSalesUsesMinimumOfRemainingCapacityAndAffordableQuantity() {
    MarketRow row = new MarketRow(MarketTestFixtures.order(
        4, MarketOrderType.SALES, new UUID(9, 2), false)); // qty 5, total 100, unit 20
    MarketDetailController controller = new MarketDetailController(
        row, MarketTestFixtures.VIEWER, false, (action, selected, quantity) -> { });
    controller.handle(new MarketDetailEvent.FactsChanged(70, 4, 0, 70));

    controller.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.SELECT_ALL));

    assertEquals(3, controller.state().quantity());
    assertEquals(60, controller.state().amount());
  }

  @Test
  void demandPartialDeliveryUsesMatchingInventoryAndReceivableHeadroom() {
    CapturingPort port = new CapturingPort();
    MarketRow row = new MarketRow(MarketTestFixtures.order(
        4, MarketOrderType.DEMAND, new UUID(9, 3), false)); // qty 5, total 100
    MarketDetailController controller = new MarketDetailController(
        row, MarketTestFixtures.VIEWER, false, port);
    controller.handle(new MarketDetailEvent.FactsChanged(0, 0, 4, 55));

    controller.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.SELECT_ALL));

    assertEquals(2, controller.state().quantity());
    assertEquals(40, controller.state().amount());
    assertTrue(controller.state().can(MarketDetailAction.SUBMIT_PRIMARY));
    controller.handle(new MarketDetailEvent.ActionClicked(MarketDetailAction.SUBMIT_PRIMARY));
    assertEquals(MarketAction.DELIVER_DEMAND, port.action);
    assertEquals(2, port.quantity);
  }

  @Test
  void legacyNonDivisibleOrderIsWholeOnly() {
    MarketOrderSnapshot base = MarketTestFixtures.order(
        2, MarketOrderType.SALES, new UUID(9, 4), false);
    MarketOrderSnapshot legacy = new MarketOrderSnapshot(
        base.type(), base.tradeId(), base.item(), 3, 10, base.ownerName(), base.ownerId(),
        base.listingTime(), base.expirationTime(), base.delivered());
    MarketDetailController controller = new MarketDetailController(
        new MarketRow(legacy), MarketTestFixtures.VIEWER, false,
        (action, selected, quantity) -> { });
    controller.handle(new MarketDetailEvent.FactsChanged(100, 64, 0, 100));

    assertFalse(controller.state().partialSupported());
    assertEquals(3, controller.state().quantity());
    assertEquals(10, controller.state().amount());
    assertFalse(controller.state().can(MarketDetailAction.DECREMENT));
    assertTrue(controller.state().can(MarketDetailAction.SUBMIT_PRIMARY));
  }

  @Test
  void refreshedRowKeepsQuantityButClampsToNewRemainingAmount() {
    MarketRow first = new MarketRow(MarketTestFixtures.order(
        4, MarketOrderType.SALES, new UUID(9, 5), false)); // 5 x 20
    MarketDetailController controller = new MarketDetailController(
        first, MarketTestFixtures.VIEWER, false, (action, selected, quantity) -> { });
    controller.handle(new MarketDetailEvent.FactsChanged(500, 64, 0, 500));
    controller.handle(new MarketDetailEvent.QuantityChanged(4));

    MarketOrderSnapshot source = first.order();
    MarketOrderSnapshot refreshed = new MarketOrderSnapshot(
        source.type(), source.tradeId(), source.item(), 2, 40, source.ownerName(), source.ownerId(),
        source.listingTime(), source.expirationTime(), source.delivered());
    controller.handle(new MarketDetailEvent.RowRefreshed(new MarketRow(refreshed)));

    assertEquals(2, controller.state().quantity());
    assertEquals(40, controller.state().amount());
  }

  @Test
  void invalidatedSelectionDisablesActionsAndCanRecoverOnAuthoritativeRefresh() {
    MarketRow first = new MarketRow(MarketTestFixtures.order(
        4, MarketOrderType.SALES, new UUID(9, 6), false));
    MarketDetailController controller = new MarketDetailController(
        first, MarketTestFixtures.VIEWER, false, (action, selected, quantity) -> { });
    controller.handle(new MarketDetailEvent.FactsChanged(500, 64, 0, 500));
    controller.handle(new MarketDetailEvent.QuantityChanged(3));

    controller.handle(new MarketDetailEvent.OrderInvalidated());

    assertEquals(ScreenState.ERROR, controller.state().screenState());
    assertEquals("screen.market.detail.order_invalidated", controller.state().errorKey());
    assertTrue(controller.state().actions().isEmpty());
    assertFalse(controller.state().quantityMode());

    MarketOrderSnapshot source = first.order();
    MarketOrderSnapshot refreshed = new MarketOrderSnapshot(
        source.type(), source.tradeId(), source.item(), 2, 40, source.ownerName(), source.ownerId(),
        source.listingTime(), source.expirationTime(), source.delivered());
    controller.handle(new MarketDetailEvent.RowRefreshed(new MarketRow(refreshed)));

    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(2, controller.state().quantity());
    assertTrue(controller.state().can(MarketDetailAction.SUBMIT_PRIMARY));
  }

  private static final class CapturingPort implements MarketDetailPort {
    MarketAction action;
    int quantity;

    @Override public void submit(MarketAction action, MarketRow row, int quantity) {
      this.action = action;
      this.quantity = quantity;
    }
  }
}
