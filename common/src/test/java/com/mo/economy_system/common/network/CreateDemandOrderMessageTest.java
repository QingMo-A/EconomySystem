package com.mo.economy_system.common.network;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CreateDemandOrderMessageTest {
    @Test void containsOnlyClientIntent() {
        CreateDemandOrderMessage message = new CreateDemandOrderMessage("minecraft:stone", 12, 999);
        assertEquals("minecraft:stone", message.itemId());
        assertEquals(12, message.quantity());
        assertEquals(999, message.unitPrice());
        assertEquals(9, EconomyMessages.CREATE_DEMAND_ORDER.discriminator());
        assertEquals(3, CreateDemandOrderMessage.class.getRecordComponents().length);
        assertEquals(Arrays.asList("itemId", "quantity", "unitPrice"), Arrays.stream(
                CreateDemandOrderMessage.class.getRecordComponents()).map(component -> component.getName()).toList());
    }
}
