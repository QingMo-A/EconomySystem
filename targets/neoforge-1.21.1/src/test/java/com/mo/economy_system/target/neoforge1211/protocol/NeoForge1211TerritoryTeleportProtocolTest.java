package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.*;
import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.network.TerritoryTeleportWireCodec;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

class NeoForge1211TerritoryTeleportProtocolTest {
  @Test void targetCodecUsesSharedBytes() {
    var message=new TeleportToTerritoryMessage(UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"));
    FriendlyByteBuf shared=new FriendlyByteBuf(Unpooled.buffer());
    RegistryFriendlyByteBuf target=new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
    try {
      TerritoryTeleportWireCodec.encode(message,shared);
      NeoForge1211MessageCodecs.codec(EconomyMessages.TELEPORT_TO_TERRITORY).encode(message,target);
      assertEquals(shared,target);
      assertEquals(message,NeoForge1211MessageCodecs.codec(EconomyMessages.TELEPORT_TO_TERRITORY).decode(target));
    } finally { shared.release(); target.release(); }
  }
}
