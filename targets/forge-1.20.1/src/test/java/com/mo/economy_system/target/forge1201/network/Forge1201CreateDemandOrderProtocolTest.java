package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.common.network.EconomyMessages;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Forge1201CreateDemandOrderProtocolTest {
    @Test void roundTripsProtocolNine() {
        FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());CreateDemandOrderMessage message=new CreateDemandOrderMessage("minecraft:stone",5,100);
        Forge1201CreateDemandOrderCodec.encode(message,buffer);assertEquals(message,Forge1201CreateDemandOrderCodec.decode(buffer));assertEquals(9,EconomyMessages.CREATE_DEMAND_ORDER.discriminator());
    }
    @Test void strictCodecRejectsInvalidFields(){assertInvalid("",1,1);assertInvalid("x".repeat(257),1,1);assertInvalid("minecraft:stone",0,1);assertInvalid("minecraft:stone",1,0);}
    @Test void feedbackNeverLeaksInternalEnumNames(){for(CreateDemandOrderResult result:CreateDemandOrderResult.values())assertFalse(Forge1201CreateDemandOrderHandler.messageFor(result).getString().contains(result.name()));}
    @Test void bridgeRoutesDemandCreationToProtocolNineAndRejectsUnknownMessages() {
        assertEquals(9, Forge1201NetworkBridge.serverDiscriminator(CreateDemandOrderMessage.class));
        assertThrows(UnsupportedOperationException.class, () -> Forge1201NetworkBridge.serverDiscriminator(UnknownMessage.class));
    }
    private record UnknownMessage() implements com.mo.economy_system.platform.network.EconomyNetworkMessage {}
    private static void assertInvalid(String id,int quantity,int price){FriendlyByteBuf b=new FriendlyByteBuf(Unpooled.buffer());
        if(id.length()<=256){b.writeUtf(id,256);b.writeInt(quantity);b.writeInt(price);assertThrows(DecoderException.class,()->Forge1201CreateDemandOrderCodec.decode(b));}
        else assertThrows(RuntimeException.class,()->Forge1201CreateDemandOrderCodec.encode(new CreateDemandOrderMessage(id,quantity,price),b));}
}
