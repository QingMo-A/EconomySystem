package com.mo.economy_system.network;

import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;

/** The single protocol-19 wire implementation: exactly one UUID (16 bytes). */
public final class TerritoryTeleportWireCodec {
  private TerritoryTeleportWireCodec() {}

  public static void encode(TeleportToTerritoryMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.territoryId());
  }

  public static TeleportToTerritoryMessage decode(WireBuffer buffer) {
    if (buffer.readableBytes() < 16) throw new WireDecodeException("truncated territory teleport payload");
    TeleportToTerritoryMessage message = new TeleportToTerritoryMessage(buffer.readUuid());
    if (buffer.isReadable()) throw new WireDecodeException("trailing territory teleport payload data");
    return message;
  }
}
