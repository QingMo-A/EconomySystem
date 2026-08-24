package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.DeliverDemandOrderMessage;
import net.minecraft.network.FriendlyByteBuf;

final class Forge1201DeliverDemandOrderCodec {
  static void encode(DeliverDemandOrderMessage m, FriendlyByteBuf b) {
    b.writeUUID(m.tradeId());
    b.writeVarInt(m.quantity());
  }

  static DeliverDemandOrderMessage decode(FriendlyByteBuf b) {
    return new DeliverDemandOrderMessage(b.readUUID(), b.readVarInt());
  }
}
