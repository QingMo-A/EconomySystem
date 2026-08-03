package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.*;
import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.HexFormat;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TerritoryTeleportWireCodecTest {
  private static final UUID ID=UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");
  @Test void goldenUuidBytesAndRoundTrip(){FriendlyByteBuf b=new FriendlyByteBuf(Unpooled.buffer());try{
    TerritoryTeleportWireCodec.encode(new TeleportToTerritoryMessage(ID),b);
    assertEquals("00112233445566778899aabbccddeeff",HexFormat.of().formatHex(b.copy().array()));
    assertEquals(ID,TerritoryTeleportWireCodec.decode(b).territoryId());}finally{b.release();}}
  @Test void rejectsTruncatedAndTrailing(){FriendlyByteBuf shortBuf=new FriendlyByteBuf(Unpooled.buffer(15).writeZero(15));
    try{assertThrows(DecoderException.class,()->TerritoryTeleportWireCodec.decode(shortBuf));}finally{shortBuf.release();}
    FriendlyByteBuf trailing=new FriendlyByteBuf(Unpooled.buffer());trailing.writeUUID(ID).writeByte(1);
    try{assertThrows(DecoderException.class,()->TerritoryTeleportWireCodec.decode(trailing));}finally{trailing.release();}}
  @Test void canonicalManifestIsUnchanged(){assertEquals(19,EconomyMessages.TELEPORT_TO_TERRITORY.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER,EconomyMessages.TELEPORT_TO_TERRITORY.direction());
    assertEquals("economy_system:territory_system/packet_teleport_to_territory",EconomyMessages.TELEPORT_TO_TERRITORY.id());}
  @Test void rejectsNull(){assertThrows(NullPointerException.class,()->new TeleportToTerritoryMessage(null));}
}
