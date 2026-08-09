package com.mo.economy_system.network;

import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;

/** Protocol 22 wire: territory UUID followed by target player UUID (exactly 32 bytes). */
public final class TerritoryMemberRemovalWireCodec {
  private TerritoryMemberRemovalWireCodec() {}

  public static void encode(RemoveTerritoryMemberMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.territoryId());
    buffer.writeUuid(message.targetPlayerId());
  }

  public static RemoveTerritoryMemberMessage decode(WireBuffer buffer) {
    if (buffer.readableBytes() < 32)
      throw new WireDecodeException("truncated territory member removal payload");
    RemoveTerritoryMemberMessage message =
        new RemoveTerritoryMemberMessage(buffer.readUuid(), buffer.readUuid());
    if (buffer.isReadable())
      throw new WireDecodeException("trailing territory member removal payload data");
    return message;
  }
}
