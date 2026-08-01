package com.mo.economy_system.target.forge1201.market;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Forge1201MarketSavedDataTest {
    @Test void roundTripsStableOrderAndPreservesDemandAlongsideNewSale() {
        CompoundTag root = new CompoundTag(); ListTag list = new ListTag(); list.add(MarketOrderCodec.encode(demand())); root.put("marketItems", list);
        MarketSavedData data = MarketSavedData.load(root); assertTrue(data.addOrder(sale()));
        CompoundTag saved = data.save(new CompoundTag());
        assertEquals(2, saved.getList("marketItems", Tag.TAG_COMPOUND).size());
        assertEquals(Set.of(MarketOrderType.SALES, MarketOrderType.DEMAND),
                new HashSet<>(MarketSavedData.load(saved).getOrders().stream().map(MarketOrder::type).toList()));
    }
    @Test void unresolvedLegacyRecordRemainsPendingAndBlocksSave() {
        CompoundTag legacy = new CompoundTag(); legacy.putString("type", "com.mo.economy_system.core.economy_system.market.SalesOrder");
        legacy.put("itemStack", new CompoundTag()); CompoundTag root = root(legacy);
        MarketSavedData data = MarketSavedData.load(root);
        assertTrue(data.getOrders().isEmpty());
        assertThrows(IllegalStateException.class, () -> data.save(new CompoundTag()));
        assertEquals(root, root(legacy));
    }
    @Test void duplicateTradeIdsFailClosedWithoutProducingReplacementData() {
        MarketOrder order = sale(); CompoundTag encoded = MarketOrderCodec.encode(order); CompoundTag root = new CompoundTag();
        ListTag list = new ListTag(); list.add(encoded); list.add(encoded.copy()); root.put("marketItems", list);
        assertThrows(IllegalArgumentException.class, () -> MarketSavedData.load(root));
        assertEquals(2, root.getList("marketItems", Tag.TAG_COMPOUND).size());
    }
    @Test void tooManyOrdersFailClosed() {
        CompoundTag root = new CompoundTag(); ListTag list = new ListTag();
        for (int i=0;i<=MarketLedger.MAX_ORDERS;i++) list.add(MarketOrderCodec.encode(sale()));
        root.put("marketItems", list); assertThrows(IllegalArgumentException.class, () -> MarketSavedData.load(root));
    }
    private static CompoundTag root(CompoundTag order) { CompoundTag root=new CompoundTag();ListTag list=new ListTag();list.add(order.copy());root.put("marketItems",list);return root; }
    private static MarketOrder sale() { return order(MarketOrderType.SALES, false); }
    private static MarketOrder demand() { return order(MarketOrderType.DEMAND, true); }
    private static MarketOrder order(MarketOrderType type, boolean delivered) { return new MarketOrder(type, UUID.randomUUID(), item(), 2, 10, "seller", UUID.randomUUID(), 10, 99, delivered); }
    private static ItemStackSnapshot item() { return ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), List.of(), Map.of(), Map.of(), true, true, 0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag()).orElseThrow(); }
}
