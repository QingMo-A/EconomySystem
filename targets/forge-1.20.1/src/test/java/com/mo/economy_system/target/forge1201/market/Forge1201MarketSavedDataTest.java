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
    private static MarketOrder sale() { return order(MarketOrderType.SALES, false); }
    private static MarketOrder demand() { return order(MarketOrderType.DEMAND, true); }
    private static MarketOrder order(MarketOrderType type, boolean delivered) { return new MarketOrder(type, UUID.randomUUID(), item(), 2, 10, "seller", UUID.randomUUID(), 10, 99, delivered); }
    private static ItemStackSnapshot item() { return ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), List.of(), Map.of(), Map.of(), true, true, 0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag()).orElseThrow(); }
}
