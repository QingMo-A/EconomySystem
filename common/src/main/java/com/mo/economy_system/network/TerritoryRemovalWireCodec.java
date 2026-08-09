package com.mo.economy_system.network;

import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;

/** Protocol 21 wire: exactly one territory UUID (16 bytes). */
public final class TerritoryRemovalWireCodec {
  private TerritoryRemovalWireCodec() {}
  public static void encode(RemoveTerritoryMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.territoryId());
  }
  public static RemoveTerritoryMessage decode(WireBuffer buffer) {
    if (buffer.readableBytes() < 16) throw new WireDecodeException("truncated territory removal payload");
    RemoveTerritoryMessage message = new RemoveTerritoryMessage(buffer.readUuid());
    if (buffer.isReadable()) throw new WireDecodeException("trailing territory removal payload data");
    return message;
  }
}
