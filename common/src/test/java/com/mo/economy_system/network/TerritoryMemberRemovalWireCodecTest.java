package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class TerritoryMemberRemovalWireCodecTest {
  @Test
  void exactGoldenOrderAndRoundTrip() {
    UUID a = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"),
        b = UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100");
    FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
    TerritoryMemberRemovalWireCodec.encode(new RemoveTerritoryMemberMessage(a, b), out);
    assertEquals(32, out.readableBytes());
    assertEquals(a, out.readUUID());
    assertEquals(b, out.readUUID());
    for (int i = 0; i < 20; i++) {
      var m = new RemoveTerritoryMemberMessage(UUID.randomUUID(), UUID.randomUUID());
      FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
      TerritoryMemberRemovalWireCodec.encode(m, buf);
      assertEquals(m, TerritoryMemberRemovalWireCodec.decode(buf));
    }
  }

  @Test
  void rejectsEveryTruncationAndTrailing() {
    for (int n = 0; n < 32; n++) {
      FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
      b.writeZero(n);
      assertThrows(RuntimeException.class, () -> TerritoryMemberRemovalWireCodec.decode(b));
    }
    FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
    TerritoryMemberRemovalWireCodec.encode(
        new RemoveTerritoryMemberMessage(UUID.randomUUID(), UUID.randomUUID()), b);
    b.writeByte(1);
    assertThrows(RuntimeException.class, () -> TerritoryMemberRemovalWireCodec.decode(b));
  }
}
