package com.mo.economy_system.target.neoforge1211.protocol;
import static org.junit.jupiter.api.Assertions.*;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
class NeoForge1211RemoveDemandOrderProtocolTest {
  @Test void codecRoundTripsCanonicalProtocol16(){RemoveDemandOrderMessage m=new RemoveDemandOrderMessage(UUID.randomUUID());RegistryFriendlyByteBuf b=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);var c=NeoForge1211MessageCodecs.codec(EconomyMessages.REMOVE_DEMAND_ORDER);c.encode(m,b);assertEquals(m,c.decode(b));assertEquals(16,EconomyMessages.REMOVE_DEMAND_ORDER.discriminator());assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER,EconomyMessages.REMOVE_DEMAND_ORDER.spec().direction());}
  @Test void nullAndTruncatedPayloadAreRejected(){assertThrows(NullPointerException.class,()->new RemoveDemandOrderMessage(null));RegistryFriendlyByteBuf b=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);assertThrows(RuntimeException.class,()->NeoForge1211MessageCodecs.codec(EconomyMessages.REMOVE_DEMAND_ORDER).decode(b));}
}
