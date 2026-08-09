package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.nbt.NbtData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarketOrderCodecTest {
    @Test
    void goldenSchemaUsesStableTypeAndExactExpiration() {
        MarketOrder order = order(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        NbtData.Compound tag = MarketOrderCodec.encode(order);
        assertEquals(Set.of(
                "type", "tradeID", "itemStack", "listedCount", "basePrice",
                "sellerName", "sellerID", "listingTime", "expirationTime"), tag.keys());
        assertEquals("sales_order", stringValue(tag, "type"));
        assertEquals(37, intValue(tag, "listedCount"));
        assertEquals(1234, intValue(tag, "basePrice"));
        assertEquals(999999L, longValue(tag, "expirationTime"));
        assertEquals(order, MarketOrderCodec.decodeCurrent(tag).orElseThrow());
    }

    @Test
    void readsLegacyClassTypeWhenSnapshotIsAlreadyVersioned() {
        NbtData.Compound tag = MarketOrderCodec.encode(order(UUID.randomUUID())).with(
                "type", NbtData.string("com.mo.economy_system.core.economy_system.market.SalesOrder"));
        assertEquals(MarketOrderType.SALES, MarketOrderCodec.decodeCurrent(tag).orElseThrow().type());
    }

    @Test
    void failsClosedOnUnknownOrderField() {
        NbtData.Compound tag = MarketOrderCodec.encode(order(UUID.randomUUID()))
                .with("futureField", NbtData.string("x"));
        assertFalse(MarketOrderCodec.decodeCurrent(tag).isSuccess());
    }

    private static MarketOrder order(UUID id) {
        return new MarketOrder(
                MarketOrderType.SALES, id, item(), 37, 1234, "Alice",
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                100L, 999999L, false);
    }

    static ItemStackSnapshot item() {
        return ItemStackSnapshot.create(
                "minecraft:stone", 1, Optional.empty(), List.of(), Map.of(), Map.of(),
                true, true, 0, 0, false, true, OptionalInt.empty(), true,
                OptionalInt.empty(), NbtData.emptyCompound()).orElseThrow();
    }

    private static int intValue(NbtData.Compound tag, String key) {
        return ((NbtData.IntValue) tag.get(key)).value();
    }

    private static long longValue(NbtData.Compound tag, String key) {
        return ((NbtData.LongValue) tag.get(key)).value();
    }

    private static String stringValue(NbtData.Compound tag, String key) {
        return ((NbtData.StringValue) tag.get(key)).value();
    }
}
