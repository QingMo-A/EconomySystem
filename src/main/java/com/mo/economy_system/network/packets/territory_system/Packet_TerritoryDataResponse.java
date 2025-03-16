package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import com.mo.economy_system.screen.territory_system.Screen_TerritoryBuff;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Packet_TerritoryDataResponse {
    private final List<Territory> owned;
    private final List<Territory> authorized;

    public Packet_TerritoryDataResponse(List<Territory> owned, List<Territory> authorized) {
        this.owned = owned;
        this.authorized = authorized;
    }

    public static void encode(Packet_TerritoryDataResponse msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.owned.size());
        msg.owned.forEach(territory -> buf.writeNbt(territory.toNBT()));

        buf.writeInt(msg.authorized.size());
        msg.authorized.forEach(territory -> buf.writeNbt(territory.toNBT()));
    }

    public static Packet_TerritoryDataResponse decode(FriendlyByteBuf buf) {
        int ownedSize = buf.readInt();
        List<Territory> owned = new ArrayList<>();
        for (int i = 0; i < ownedSize; i++) {
            owned.add(Territory.fromNBT(buf.readNbt()));
        }

        int authorizedSize = buf.readInt();
        List<Territory> authorized = new ArrayList<>();
        for (int i = 0; i < authorizedSize; i++) {
            authorized.add(Territory.fromNBT(buf.readNbt()));
        }

        return new Packet_TerritoryDataResponse(owned, authorized);
    }

    public static void handle(Packet_TerritoryDataResponse msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 检查当前屏幕是否是 TerritoryScreen，避免不必要的处理
            if (Minecraft.getInstance().screen instanceof Screen_Territory screen) {
                // 更新领地数据
                screen.updateTerritoryData(msg.owned, msg.authorized);
            }
        });
        context.setPacketHandled(true);
    }

}
