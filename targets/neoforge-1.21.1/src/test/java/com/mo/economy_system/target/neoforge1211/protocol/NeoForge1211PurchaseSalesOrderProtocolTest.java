package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NeoForge1211PurchaseSalesOrderProtocolTest {
    @Test void uuidRoundTripsOnCanonicalDiscriminator(){PurchaseSalesOrderMessage message=new PurchaseSalesOrderMessage(UUID.randomUUID());RegistryFriendlyByteBuf buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);var codec=NeoForge1211MessageCodecs.codec(EconomyMessages.PURCHASE_SALES_ORDER);codec.encode(message,buffer);assertEquals(message,codec.decode(buffer));assertEquals(12,EconomyMessages.PURCHASE_SALES_ORDER.discriminator());}
    @Test void nullTradeIdIsRejected(){assertThrows(NullPointerException.class,()->new PurchaseSalesOrderMessage(null));}
}
