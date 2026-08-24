package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.PurchaseSalesOrderMessage;
import net.minecraft.network.FriendlyByteBuf;

final class Forge1201PurchaseSalesOrderCodec {
    static void encode(PurchaseSalesOrderMessage message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.tradeId());
        buffer.writeVarInt(message.quantity());
    }
    static PurchaseSalesOrderMessage decode(FriendlyByteBuf buffer) {
        return new PurchaseSalesOrderMessage(buffer.readUUID(), buffer.readVarInt());
    }
}
