package com.mo.economy_system.network;

import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;

/** Protocol 20 wire: territory UUID followed by target-player UUID (32 bytes). */
public final class TerritoryInviteWireCodec {
  private TerritoryInviteWireCodec() {}

  public static void encode(InvitePlayerMessage message, WireBuffer buffer) {
    buffer.writeUuid(message.territoryId());
    buffer.writeUuid(message.targetPlayerId());
  }

  public static InvitePlayerMessage decode(WireBuffer buffer) {
    if (buffer.readableBytes() < 32) throw new WireDecodeException("truncated territory invite payload");
    InvitePlayerMessage result = new InvitePlayerMessage(buffer.readUuid(), buffer.readUuid());
    if (buffer.isReadable()) throw new WireDecodeException("trailing territory invite payload data");
    return result;
  }
}
