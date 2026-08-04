package com.mo.economy_system.network;

import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

/** Protocol 21 wire: exactly one territory UUID (16 bytes). */
public final class TerritoryRemovalWireCodec {
  private TerritoryRemovalWireCodec() {}
  public static void encode(RemoveTerritoryMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
  }
  public static RemoveTerritoryMessage decode(FriendlyByteBuf buffer) {
    if (buffer.readableBytes() < 16) throw new DecoderException("truncated territory removal payload");
    RemoveTerritoryMessage message = new RemoveTerritoryMessage(buffer.readUUID());
    if (buffer.isReadable()) throw new DecoderException("trailing territory removal payload data");
    return message;
  }
}
