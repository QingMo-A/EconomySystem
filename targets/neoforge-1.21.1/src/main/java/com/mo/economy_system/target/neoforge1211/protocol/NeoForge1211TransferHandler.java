package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.target.neoforge1211.NeoForge1211TransferAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** NeoForge context adapter for the shared transfer behavior. */
public final class NeoForge1211TransferHandler {
    private NeoForge1211TransferHandler() {
    }

    public static void handle(TransferMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) {
                NeoForge1211TransferAdapter.execute(sender, message);
            }
        });
    }
}
