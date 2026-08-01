package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.*;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NeoForge1211CreateSalesOrderProtocolTest {
    @Test void roundTripsProtocolEight() {
        RegistryFriendlyByteBuf buffer = buffer(); NeoForge1211MessageCodec<CreateSalesOrderMessage> codec = NeoForge1211MessageCodecs.codec(EconomyMessages.CREATE_SALES_ORDER);
        codec.encode(new CreateSalesOrderMessage(4,17,901),buffer);
        assertEquals(new CreateSalesOrderMessage(4,17,901),codec.decode(buffer));
    }
    @Test void strictDecodeRejectsInvalidFields() { assertInvalid(-1,1,1);assertInvalid(0,0,1);assertInvalid(0,1,0); }
    @Test void handlerFeedbackIsStableAndSafe() {
        assertEquals(CreateSalesOrderFeedback.SUCCESS, CreateSalesOrderFeedback.messageKey(CreateSalesOrderResult.SUCCESS));
        for(CreateSalesOrderResult result:CreateSalesOrderResult.values())
            assertFalse(NeoForge1211CreateSalesOrderHandler.messageFor(result,10).getString().contains(result.name()));
    }
    private static void assertInvalid(int slot,int quantity,int price){RegistryFriendlyByteBuf b=buffer();b.writeInt(slot);b.writeInt(quantity);b.writeInt(price);
        assertThrows(DecoderException.class,()->NeoForge1211MessageCodecs.codec(EconomyMessages.CREATE_SALES_ORDER).decode(b));}
    private static RegistryFriendlyByteBuf buffer(){return new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);}
}
