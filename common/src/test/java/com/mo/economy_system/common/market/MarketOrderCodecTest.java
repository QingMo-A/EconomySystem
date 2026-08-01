package com.mo.economy_system.common.market;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MarketOrderCodecTest {
    @Test void goldenSchemaUsesStableTypeAndExactExpiration() {
        MarketOrder order = order(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        CompoundTag tag = MarketOrderCodec.encode(order);
        assertEquals(Set.of("type", "tradeID", "itemStack", "listedCount", "basePrice", "sellerName", "sellerID", "listingTime", "expirationTime"), tag.getAllKeys());
        assertEquals("sales_order", tag.getString("type")); assertEquals(37, tag.getInt("listedCount"));
        assertEquals(1234, tag.getInt("basePrice")); assertEquals(999999L, tag.getLong("expirationTime"));
        assertEquals(order, MarketOrderCodec.decodeCurrent(tag).orElseThrow());
    }

    @Test void readsLegacyClassTypeWhenSnapshotIsAlreadyVersioned() {
        CompoundTag tag = MarketOrderCodec.encode(order(UUID.randomUUID()));
        tag.putString("type", "com.mo.economy_system.core.economy_system.market.SalesOrder");
        assertEquals(MarketOrderType.SALES, MarketOrderCodec.decodeCurrent(tag).orElseThrow().type());
    }

    @Test void failsClosedOnUnknownOrderField() {
        CompoundTag tag = MarketOrderCodec.encode(order(UUID.randomUUID())); tag.putString("futureField", "x");
        assertFalse(MarketOrderCodec.decodeCurrent(tag).isSuccess());
    }

    private static MarketOrder order(UUID id) {
        return new MarketOrder(MarketOrderType.SALES, id, item(), 37, 1234, "Alice",
                UUID.fromString("00000000-0000-0000-0000-000000000002"), 100L, 999999L, false);
    }
    static ItemStackSnapshot item() {
        return ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), List.of(), Map.of(), Map.of(),
                true, true, 0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag()).orElseThrow();
    }
}
