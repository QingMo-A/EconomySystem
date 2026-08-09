package com.mo.economy_system.target.neoforge1211.redpacket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.redpacket.RedPacket;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class NeoForge1211RedPacketSavedDataTest {
  @Test
  void schemaOneRoundTripsCommonSnapshot() {
    RedPacket packet = packet();
    NeoForge1211RedPacketSavedData data = new NeoForge1211RedPacketSavedData();
    data.replacePackets(List.of(packet));

    CompoundTag encoded = data.save(new CompoundTag(), null);
    assertEquals(1, encoded.getInt("schemaVersion"));
    assertEquals(List.of(packet), NeoForge1211RedPacketSavedData.load(encoded, null).packets());
  }

  @Test
  void unknownSchemaIsReadOnly() {
    CompoundTag encoded = new CompoundTag();
    encoded.putInt("schemaVersion", 2);
    NeoForge1211RedPacketSavedData data = NeoForge1211RedPacketSavedData.load(encoded, null);
    assertThrows(IllegalStateException.class, () -> data.replacePackets(List.of(packet())));
  }

  private static RedPacket packet() {
    return new RedPacket(
        UUID.fromString("00000000-0000-0000-0000-000000000001"),
        "sender",
        20,
        3,
        7,
        RedPacket.Mode.LUCKY,
        10,
        70_000,
        Set.of(UUID.fromString("00000000-0000-0000-0000-000000000002")));
  }
}
