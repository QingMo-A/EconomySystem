package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class Forge1201PurchaseSalesOrderProtocolTest {
    @Test void uuidRoundTripsOnCanonicalDiscriminator(){PurchaseSalesOrderMessage message=new PurchaseSalesOrderMessage(UUID.randomUUID());FriendlyByteBuf buffer=new FriendlyByteBuf(Unpooled.buffer());Forge1201PurchaseSalesOrderCodec.encode(message,buffer);assertEquals(message,Forge1201PurchaseSalesOrderCodec.decode(buffer));assertEquals(12,EconomyMessages.PURCHASE_SALES_ORDER.discriminator());}
    @Test void nullTradeIdIsRejected(){assertThrows(NullPointerException.class,()->new PurchaseSalesOrderMessage(null));}
}
