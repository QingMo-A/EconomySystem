package com.mo.economy_system.common.network;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CreateSalesOrderMessageTest {
    @Test void containsOnlyServerValidatedIntent() {
        CreateSalesOrderMessage message = new CreateSalesOrderMessage(3, 12, 999);
        assertEquals(3, message.slot()); assertEquals(12, message.quantity()); assertEquals(999, message.unitPrice());
        assertEquals(3, CreateSalesOrderMessage.class.getRecordComponents().length);
        assertEquals(8, EconomyMessages.CREATE_SALES_ORDER.discriminator());
    }
}
