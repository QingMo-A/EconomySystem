package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.CreateSalesOrderMessage;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

final class Forge1201CreateSalesOrderCodec {
    private Forge1201CreateSalesOrderCodec() {}
    static void encode(CreateSalesOrderMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.slot()); buffer.writeInt(message.quantity()); buffer.writeInt(message.totalPrice());
    }
    static CreateSalesOrderMessage decode(FriendlyByteBuf buffer) {
        int slot=buffer.readInt(), quantity=buffer.readInt(), totalPrice=buffer.readInt();
        if(slot<0||quantity<=0||totalPrice<=0) throw new DecoderException("Invalid create sales order request");
        return new CreateSalesOrderMessage(slot,quantity,totalPrice);
    }
}
