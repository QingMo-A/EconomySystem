package com.mo.economy_system.common.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShopBuyItemMessageTest {
    @Test
    void preservesServerValidatedPurchaseIntent() {
        ShopBuyItemMessage message = new ShopBuyItemMessage("entry-id", 64);

        assertEquals("entry-id", message.shopItemId());
        assertEquals(64, message.quantity());
    }

    @Test
    void rejectsNullCatalogId() {
        assertThrows(NullPointerException.class, () -> new ShopBuyItemMessage(null, 1));
    }
}
