package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;

public class Packet_BalanceRequest {

    public Packet_BalanceRequest() {}

    public static void encode(Packet_BalanceRequest msg, FriendlyByteBuf buf) {
        // 无需数据
    }

    public static Packet_BalanceRequest decode(FriendlyByteBuf buf) {
        return new Packet_BalanceRequest();
    }

    public static void handle(Packet_BalanceRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                ServerLevel serverLevel = player.serverLevel();
                if (serverLevel != null) {
                    EconomySavedData data = EconomySavedData.getInstance(serverLevel);
                    List<Map.Entry<UUID, Integer>> accounts = data.getAllAccounts();
                    int balance = data.getBalance(player.getUUID());

                    // 创建一个新的列表来存储 <String, Integer> 类型的数据
                    List<Map.Entry<String, Integer>> accountNames = new ArrayList<>();

                    for (Map.Entry<UUID, Integer> entry : accounts) {
                        UUID uuid = entry.getKey();
                        Integer accountBalance = entry.getValue();

                        // 获取玩家名称
                        String playerName = Util_Player.getPlayerNameFromUUID(player.server, uuid); // 获取离线玩家名称

                        // 将玩家名称和余额加入到新的列表中
                        accountNames.add(new AbstractMap.SimpleEntry<>(playerName, accountBalance));
                    }

                    // 发送响应包到客户端
                    EconomySystem_NetworkManager.INSTANCE.send(player, new Packet_BalanceResponse(balance, accountNames));
                }
            }
        });
        context.setPacketHandled(true);
    }

}
