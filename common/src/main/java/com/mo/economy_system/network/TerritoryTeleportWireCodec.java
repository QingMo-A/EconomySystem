package com.mo.economy_system.network;

import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

/** The single protocol-19 wire implementation: exactly one UUID (16 bytes). */
public final class TerritoryTeleportWireCodec {
  private TerritoryTeleportWireCodec() {}

  public static void encode(TeleportToTerritoryMessage message, FriendlyByteBuf buffer) {
    buffer.writeUUID(message.territoryId());
  }

  public static TeleportToTerritoryMessage decode(FriendlyByteBuf buffer) {
    if (buffer.readableBytes() < 16) throw new DecoderException("truncated territory teleport payload");
    TeleportToTerritoryMessage message = new TeleportToTerritoryMessage(buffer.readUUID());
    if (buffer.isReadable()) throw new DecoderException("trailing territory teleport payload data");
    return message;
  }
}
