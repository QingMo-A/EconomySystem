package com.mo.economy_system.common.network;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransferMessageTest {
    @Test
    void preservesTheRequestedTargetAndAmount() {
        UUID target = UUID.randomUUID();
        TransferMessage message = new TransferMessage(target, 25);

        assertEquals(target, message.targetPlayerId());
        assertEquals(25, message.amount());
    }

    @Test
    void rejectsANullTarget() {
        assertThrows(NullPointerException.class, () -> new TransferMessage(null, 1));
    }
}
