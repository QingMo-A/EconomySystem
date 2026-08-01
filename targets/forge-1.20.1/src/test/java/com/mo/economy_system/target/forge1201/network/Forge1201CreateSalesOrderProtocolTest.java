package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Forge1201CreateSalesOrderProtocolTest {
    @Test void roundTripsProtocolEight() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        Forge1201CreateSalesOrderCodec.encode(new CreateSalesOrderMessage(4, 17, 901), buffer);
        assertEquals(new CreateSalesOrderMessage(4,17,901), Forge1201CreateSalesOrderCodec.decode(buffer));
    }
    @Test void strictDecodeRejectsInvalidFields() {
        assertInvalid(-1,1,1); assertInvalid(0,0,1); assertInvalid(0,1,0);
    }
    @Test void handlerFeedbackIsNeverAnInternalEnumName() {
        assertEquals(CreateSalesOrderFeedback.SUCCESS, ((TranslatableContents) Forge1201CreateSalesOrderHandler.messageFor(CreateSalesOrderResult.SUCCESS,10).getContents()).getKey());
        for (CreateSalesOrderResult result : CreateSalesOrderResult.values())
            assertFalse(Forge1201CreateSalesOrderHandler.messageFor(result,10).getString().contains(result.name()));
    }
    private static void assertInvalid(int slot,int quantity,int price) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer()); buffer.writeInt(slot);buffer.writeInt(quantity);buffer.writeInt(price);
        assertThrows(DecoderException.class,()->Forge1201CreateSalesOrderCodec.decode(buffer));
    }
}
