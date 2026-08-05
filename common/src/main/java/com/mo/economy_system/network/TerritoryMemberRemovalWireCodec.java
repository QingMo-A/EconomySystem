package com.mo.economy_system.network;

import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

/** Protocol 22 wire: territory UUID followed by target player UUID (exactly 32 bytes). */
public final class TerritoryMemberRemovalWireCodec {
  private TerritoryMemberRemovalWireCodec() {}

  public static void encode(RemoveTerritoryMemberMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
    buffer.writeUUID(message.targetPlayerId());
  }

  public static RemoveTerritoryMemberMessage decode(FriendlyByteBuf buffer) {
    if (buffer.readableBytes() < 32)
      throw new DecoderException("truncated territory member removal payload");
    RemoveTerritoryMemberMessage message =
        new RemoveTerritoryMemberMessage(buffer.readUUID(), buffer.readUUID());
    if (buffer.isReadable())
      throw new DecoderException("trailing territory member removal payload data");
    return message;
  }
}
