package com.mo.economy_system.network;

import com.mo.economy_system.common.network.InvitePlayerMessage;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

/** Protocol 20 wire: territory UUID followed by target-player UUID (32 bytes). */
public final class TerritoryInviteWireCodec {
  private TerritoryInviteWireCodec() {}

  public static void encode(InvitePlayerMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
    buffer.writeUUID(message.targetPlayerId());
  }

  public static InvitePlayerMessage decode(FriendlyByteBuf buffer) {
    if (buffer.readableBytes() < 32) throw new DecoderException("truncated territory invite payload");
    InvitePlayerMessage result = new InvitePlayerMessage(buffer.readUUID(), buffer.readUUID());
    if (buffer.isReadable()) throw new DecoderException("trailing territory invite payload data");
    return result;
  }
}
