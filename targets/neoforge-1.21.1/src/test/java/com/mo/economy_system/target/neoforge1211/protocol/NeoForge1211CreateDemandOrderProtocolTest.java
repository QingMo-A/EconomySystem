package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.market.CreateDemandOrderResult;
import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.common.network.EconomyMessages;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NeoForge1211CreateDemandOrderProtocolTest {
    @Test void roundTripsProtocolNine(){RegistryFriendlyByteBuf b=buffer();CreateDemandOrderMessage message=new CreateDemandOrderMessage("minecraft:stone",5,100);
        codec().encode(message,b);assertEquals(message,codec().decode(b));assertEquals(9,EconomyMessages.CREATE_DEMAND_ORDER.discriminator());}
    @Test void strictCodecRejectsInvalidFields(){assertInvalid("",1,1);assertInvalid("x".repeat(257),1,1);assertInvalid("minecraft:stone",0,1);assertInvalid("minecraft:stone",1,0);}
    @Test void feedbackNeverLeaksInternalEnumNames(){for(CreateDemandOrderResult result:CreateDemandOrderResult.values())assertFalse(NeoForge1211CreateDemandOrderHandler.messageFor(result).getString().contains(result.name()));}
    private static NeoForge1211MessageCodec<CreateDemandOrderMessage> codec(){return NeoForge1211MessageCodecs.codec(EconomyMessages.CREATE_DEMAND_ORDER);}
    private static void assertInvalid(String id,int quantity,int price){RegistryFriendlyByteBuf b=buffer();if(id.length()<=256){b.writeUtf(id,256);b.writeInt(quantity);b.writeInt(price);assertThrows(DecoderException.class,()->codec().decode(b));}else assertThrows(RuntimeException.class,()->codec().encode(new CreateDemandOrderMessage(id,quantity,price),b));}
    private static RegistryFriendlyByteBuf buffer(){return new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);}
}
