package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.economy.TransferService;
import com.mo.economy_system.common.network.TransferMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Forge context adapter for the shared transfer behavior. */
final class Forge1201TransferHandler {
    private Forge1201TransferHandler() {
    }

    static void handle(
            TransferMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer sender = context.getSender();
        if (sender != null) {
            TransferService.execute(sender, message);
        }
        context.setPacketHandled(true);
    }
}
