package com.mo.economy_system.core.playerattributes_system.courage;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.courage_system.Packet_SyncCourageData;
import net.minecraft.server.level.ServerPlayer;

public class PlayerCourageClientSync {
    public static void sendCourageDataToClient(ServerPlayer player, float currentCourage, float maxCourage) {
        // 构建勇气值同步数据包
        Packet_SyncCourageData packet = new Packet_SyncCourageData(currentCourage, maxCourage);
        // 发送数据包到指定玩家
        EconomySystem_NetworkManager.sendToClient(packet, player);
    }
}
