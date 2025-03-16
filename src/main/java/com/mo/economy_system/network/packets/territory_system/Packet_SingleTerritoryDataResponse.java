package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.screen.territory_system.Screen_TerritoryBuff;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_SingleTerritoryDataResponse {
    private final Territory territory;

    public Packet_SingleTerritoryDataResponse(Territory territory) {
        this.territory = territory;
    }

    public static void encode(Packet_SingleTerritoryDataResponse msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.territory.toNBT());
    }

    public static Packet_SingleTerritoryDataResponse decode(FriendlyByteBuf buf) {
        return new Packet_SingleTerritoryDataResponse(Territory.fromNBT(buf.readNbt()));
    }

    public static void handle(Packet_SingleTerritoryDataResponse msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 🔹 处理客户端数据
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof Screen_TerritoryBuff screen) {
                screen.updateTerritory(msg.territory);
            }
        });

        context.setPacketHandled(true);
    }
}
