package com.mo.economy_system.core.economy_system.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.nbt.NbtData;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

class MarketManagerFacadeTest {
  @Test
  void rebindingCannotMutateThePreviousWorldOrWriteAnOldViewBack() {
    MarketSavedData first = new MarketSavedData(), second = new MarketSavedData();
    MarketOrder firstOrder = order(), secondOrder = order();
    first.addOrder(firstOrder);
    MarketManager.bind(first);
    assertSame(first, MarketManager.boundDataForTest());
    second.addOrder(secondOrder);
    MarketManager.bind(second);
    assertSame(second, MarketManager.boundDataForTest());
    assertTrue(MarketManager.removeMarketItemById(secondOrder.tradeId()));
    assertEquals(List.of(firstOrder), first.getOrders());
    assertTrue(second.getOrders().isEmpty());
  }

  @Test
  void detachedDemandMutationDoesNotPersistButExplicitTransitionDoes() {
    MarketSavedData data = new MarketSavedData();
    MarketOrder demand =
        new MarketOrder(
            MarketOrderType.DEMAND,
            UUID.randomUUID(),
            item(),
            4,
            9,
            "buyer",
            UUID.randomUUID(),
            1,
            9,
            false);
    data.addOrder(demand);
    MarketManager.bind(data);
    DemandOrder detached =
        new DemandOrder(
            demand.tradeId(),
            demand.item().itemId(),
            ItemStack.EMPTY,
            demand.totalPrice(),
            demand.sellerName(),
            demand.sellerId(),
            demand.listingTime(),
            demand.expirationTime(),
            false);
    detached.setDelivered(true);
    assertFalse(data.getOrder(demand.tradeId()).delivered());
    assertEquals(
        DemandDeliveryTransitionStatus.UPDATED,
        MarketManager.markDemandOrderDelivered(demand.tradeId(), demand).status());
    assertTrue(data.getOrder(demand.tradeId()).delivered());
  }

  private static MarketOrder order() {
    return new MarketOrder(
        MarketOrderType.SALES,
        UUID.randomUUID(),
        item(),
        1,
        1,
        "s",
        UUID.randomUUID(),
        1,
        2,
        false);
  }

  private static ItemStackSnapshot item() {
    return ItemStackSnapshot.create(
            "minecraft:stone",
            1,
            Optional.empty(),
            List.of(),
            Map.of(),
            Map.of(),
            true,
            true,
            0,
            0,
            false,
            true,
            OptionalInt.empty(),
            true,
            OptionalInt.empty(),
            NbtData.emptyCompound())
        .orElseThrow();
  }
}
