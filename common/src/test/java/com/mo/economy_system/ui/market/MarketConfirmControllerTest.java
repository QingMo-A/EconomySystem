package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.MarketOrderSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.nbt.NbtData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarketConfirmControllerTest {
  @Test void confirmSubmitsExactlyOnceAndCancelsWithoutSubmitting() {
    MarketRow row = new MarketRow(new MarketOrderSnapshot(MarketOrderType.SALES,
        UUID.randomUUID(), item(), 2, 40, "seller", UUID.randomUUID(), 1, 2, false));
    FakePort port = new FakePort();
    MarketConfirmController controller = new MarketConfirmController(MarketAction.BUY, row, port);
    controller.handle(new MarketConfirmEvent.ActionClicked(MarketConfirmAction.CONFIRM));
    assertEquals(1, port.calls);
    assertEquals(MarketAction.BUY, port.action);
    assertEquals(1, controller.pollNavigation().stream().count());
    controller.handle(new MarketConfirmEvent.ActionClicked(MarketConfirmAction.CONFIRM));
    controller.handle(new MarketConfirmEvent.ActionClicked(MarketConfirmAction.CANCEL));
    assertEquals(1, port.calls);
  }

  private static ItemStackSnapshot item() {
    return ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), List.of(), Map.of(), Map.of(),
        true, true, 0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), NbtData.emptyCompound())
        .orElseThrow();
  }

  private static final class FakePort implements MarketConfirmPort {
    int calls; MarketAction action;
    @Override public void submit(MarketAction action, MarketRow row) { calls++; this.action = action; }
  }
}
