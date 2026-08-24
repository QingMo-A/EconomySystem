package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;

final class Forge1201CreateDemandOrderCodec {
    private Forge1201CreateDemandOrderCodec() {}

    static void encode(CreateDemandOrderMessage message, FriendlyByteBuf buffer) {
        validate(message.itemId(), message.quantity(), message.unitPrice());
        buffer.writeUtf(message.itemId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
        buffer.writeInt(message.quantity()); buffer.writeInt(message.unitPrice());
    }

    static CreateDemandOrderMessage decode(FriendlyByteBuf buffer) {
        String itemId = buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
        int quantity = buffer.readInt(); int unitPrice = buffer.readInt();
        validate(itemId, quantity, unitPrice);
        return new CreateDemandOrderMessage(itemId, quantity, unitPrice);
    }

    private static void validate(String itemId, int quantity, int unitPrice) {
        if (itemId == null || itemId.isBlank()
                || itemId.length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH
                || quantity <= 0 || unitPrice <= 0)
            throw new DecoderException("Invalid create demand order request");
    }
}
