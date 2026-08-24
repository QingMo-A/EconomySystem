package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.nbt.NbtData;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NeoForge1211MarketDataProtocolTest {
    @Test
    void roundTripsSummaryAndPageRequests() {
        assertRequestRoundTrip(MarketDataRequestMessage.summary(10));
        assertRequestRoundTrip(new MarketDataRequestMessage(11, MarketDataRequestPurpose.PAGE, 9, 9,
                MarketOrderFilter.MINE, "stone"));
        UUID focused = UUID.randomUUID();
        assertRequestRoundTrip(new MarketDataRequestMessage(12, MarketDataRequestPurpose.PAGE, 0, 9,
                MarketOrderFilter.ALL, MarketOrderSort.NEWEST, "diamond", focused));
    }

    @Test
    void roundTripsAllResponseKindsWithRevisionAndSnapshot() {
        assertResponseRoundTrip(new MarketDataResponseMessage(MarketDataResponseKind.SUMMARY, 1, 7,
                0, 0, 0, 1, 0, List.of()));
        assertResponseRoundTrip(MarketDataResponseMessage.invalidated(8, 1, 0));
        MarketOrderSnapshot order = order();
        MarketDataResponseMessage page = new MarketDataResponseMessage(MarketDataResponseKind.PAGE, 2, 9,
                0, 9, 1, 1, 0, List.of(order));
        assertEquals(page, assertResponseRoundTrip(page));
    }

    @Test
    void rejectsInvalidEnumsAndOrderCountsBeforeAllocation() {
        RegistryFriendlyByteBuf request = buffer();
        request.writeLong(1);
        request.writeVarInt(Integer.MAX_VALUE);
        assertThrows(DecoderException.class, () -> NeoForge1211MarketDataCodec.REQUEST.decode(request));

        RegistryFriendlyByteBuf response = buffer();
        response.writeEnum(MarketDataResponseKind.PAGE);
        response.writeLong(1);
        response.writeLong(0);
        response.writeInt(0);
        response.writeInt(9);
        response.writeInt(0);
        response.writeInt(0);
        response.writeInt(0);
        response.writeInt(10);
        assertThrows(DecoderException.class, () -> NeoForge1211MarketDataCodec.RESPONSE.decode(response));
    }

    @Test void rejectsOversizedWirePayloadBeforeParsingSnapshot() {
        RegistryFriendlyByteBuf buffer=buffer();buffer.writeZero(EconomyNetworkLimits.MAX_MARKET_RESPONSE_WIRE_BYTES+1);
        assertThrows(DecoderException.class,()->NeoForge1211MarketDataCodec.RESPONSE.decode(buffer));
    }

    private static void assertRequestRoundTrip(MarketDataRequestMessage message) {
        RegistryFriendlyByteBuf buffer = buffer();
        NeoForge1211MarketDataCodec.REQUEST.encode(message, buffer);
        assertEquals(message, NeoForge1211MarketDataCodec.REQUEST.decode(buffer));
    }

    private static MarketDataResponseMessage assertResponseRoundTrip(MarketDataResponseMessage message) {
        RegistryFriendlyByteBuf buffer = buffer();
        NeoForge1211MarketDataCodec.RESPONSE.encode(message, buffer);
        MarketDataResponseMessage decoded = NeoForge1211MarketDataCodec.RESPONSE.decode(buffer);
        assertEquals(message, decoded);
        return decoded;
    }

    private static MarketOrderSnapshot order() {
        NbtData.Compound customData = NbtData.compoundBuilder()
                .putString("fixture", "market-page")
                .build();
        ItemStackSnapshot snapshot = ItemStackSnapshot.create("minecraft:stone", 1,
                Optional.of("{\"text\":\"Stone\"}"), List.of("{\"text\":\"Lore\"}"),
                Map.of(), Map.of(), true, true, 0, 0, false, true,
                OptionalInt.empty(), true, OptionalInt.empty(), customData).orElseThrow();
        return new MarketOrderSnapshot(MarketOrderType.SALES, UUID.randomUUID(), snapshot, 3, 30,
                "seller", UUID.randomUUID(), 100, 200, false);
    }

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
    }
}
