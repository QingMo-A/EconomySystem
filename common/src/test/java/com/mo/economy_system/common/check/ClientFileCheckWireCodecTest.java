package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.network.ClientFileCheckWireCodec;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class ClientFileCheckWireCodecTest {
  private static final UUID TARGET = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID REQUESTER = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Test
  void requestAndResultRoundTripInLegacyOrder() {
    var request =
        new ClientFileCheckRequestMessage(
            "Target", TARGET, "Admin", REQUESTER, ClientFileCheckType.MODS);
    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    ClientFileCheckWireCodec.encodeRequest(request, buffer);
    FriendlyByteBuf fields = new FriendlyByteBuf(buffer.copy());
    assertEquals("Target", fields.readUtf(16));
    assertEquals(TARGET.toString(), fields.readUtf(36));
    assertEquals("Admin", fields.readUtf(16));
    assertEquals(REQUESTER.toString(), fields.readUtf(36));
    assertEquals("mods", fields.readUtf(16));
    assertEquals(request, ClientFileCheckWireCodec.decodeRequest(buffer));

    String json =
        ClientFileCheckResultJsonCodec.encode(
            ClientFileCheckResult.declined(ClientFileCheckType.MODS));
    var result =
        new ClientFileCheckResultRequestMessage(
            "Target", TARGET, "Admin", REQUESTER, ClientFileCheckType.MODS, json);
    FriendlyByteBuf resultBuffer = new FriendlyByteBuf(Unpooled.buffer());
    ClientFileCheckWireCodec.encodeResultRequest(result, resultBuffer);
    assertEquals(result, ClientFileCheckWireCodec.decodeResultRequest(resultBuffer));
  }

  @Test
  void rejectsTrailingAndNonCanonicalUuid() {
    FriendlyByteBuf trailing = new FriendlyByteBuf(Unpooled.buffer());
    ClientFileCheckWireCodec.encodeRequest(
        new ClientFileCheckRequestMessage(
            "Target", TARGET, "Admin", REQUESTER, ClientFileCheckType.MODS),
        trailing);
    trailing.writeByte(1);
    assertThrows(RuntimeException.class, () -> ClientFileCheckWireCodec.decodeRequest(trailing));
    FriendlyByteBuf bad = new FriendlyByteBuf(Unpooled.buffer());
    bad.writeUtf("Target", 16);
    bad.writeUtf(TARGET.toString().toUpperCase(), 36);
    bad.writeUtf("Admin", 16);
    bad.writeUtf(REQUESTER.toString(), 36);
    bad.writeUtf("mods", 16);
    assertThrows(RuntimeException.class, () -> ClientFileCheckWireCodec.decodeRequest(bad));
  }
}
