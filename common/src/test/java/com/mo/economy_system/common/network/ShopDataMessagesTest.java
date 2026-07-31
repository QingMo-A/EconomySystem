package com.mo.economy_system.common.network;

import com.mo.economy_system.common.client.ClientShopState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShopDataMessagesTest {
    @Test
    void responseDefensivelyCopiesItsCatalog() {
        List<ShopItemSnapshot> source = new ArrayList<>();
        source.add(item("first"));

        ShopDataResponseMessage message = new ShopDataResponseMessage(source);
        source.add(item("second"));

        assertEquals(1, message.items().size());
        assertThrows(UnsupportedOperationException.class, () -> message.items().clear());
    }

    @Test
    void clientStatePublishesAnImmutableSnapshot() {
        ClientShopState.update(new ShopDataResponseMessage(List.of(item("entry"))));

        assertEquals("entry", ClientShopState.snapshot().items().get(0).shopItemId());
        assertThrows(
                UnsupportedOperationException.class,
                () -> ClientShopState.snapshot().items().clear()
        );
    }

    @Test
    void optionalItemPayloadStringsAreNormalized() {
        ShopItemSnapshot item = new ShopItemSnapshot(
                "entry", "minecraft:stone", 5, 5, 5, null,
                0.0D, null, null, 0, 0, 0
        );

        assertEquals("", item.description());
        assertEquals("", item.nbt());
        assertEquals("", item.itemData());
    }

    private static ShopItemSnapshot item(String id) {
        return new ShopItemSnapshot(
                id, "minecraft:stone", 5, 5, 5, "Stone",
                0.0D, "", "", 0, 512, 512
        );
    }
}
