package com.mo.economy_system.target.neoforge1211.market;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NeoForge1211MarketSavedDataTest {
    @Test void roundTripsStableOrderWithExactExpiration() {
        MarketOrder original = new MarketOrder(MarketOrderType.SALES, UUID.randomUUID(), item(), 7, 101, "seller", UUID.randomUUID(), 10, 123456, false);
        CompoundTag root = new CompoundTag(); ListTag list = new ListTag(); list.add(MarketOrderCodec.encode(original)); root.put("marketItems", list);
        MarketSavedData data = MarketSavedData.load(root, null);
        CompoundTag saved = data.save(new CompoundTag(), null);
        MarketOrder decoded = MarketOrderCodec.decodeCurrent(saved.getList("marketItems", Tag.TAG_COMPOUND).getCompound(0)).orElseThrow();
        assertEquals(original, decoded); assertEquals(123456, decoded.expirationTime());
    }
    private static ItemStackSnapshot item() { return ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), List.of(), Map.of(), Map.of(), true, true, 0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag()).orElseThrow(); }
}
