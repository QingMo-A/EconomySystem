package com.mo.economy_system.target.forge1201.redpacket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.redpacket.RedPacket;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class Forge1201RedPacketSavedDataTest {
  @Test
  void schemaOneRoundTripsCommonSnapshot() {
    RedPacket packet = packet();
    Forge1201RedPacketSavedData data = new Forge1201RedPacketSavedData();
    data.replacePackets(List.of(packet));

    CompoundTag encoded = data.save(new CompoundTag());
    assertEquals(1, encoded.getInt("schemaVersion"));
    assertEquals(List.of(packet), Forge1201RedPacketSavedData.load(encoded).packets());
  }

  @Test
  void unknownSchemaIsReadOnly() {
    CompoundTag encoded = new CompoundTag();
    encoded.putInt("schemaVersion", 2);
    Forge1201RedPacketSavedData data = Forge1201RedPacketSavedData.load(encoded);
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
