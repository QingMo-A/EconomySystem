package com.mo.economy_system.commands.economy_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class Command_StarterKit {

    private static final String TAG_KEY = "ReceivedStarterKit";

    // 注册指令
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("starterkit") // 指令名称
                        .requires(commandSource -> commandSource.hasPermission(0)) // 任何玩家可用
                        .executes(Command_StarterKit::execute) // 指令逻辑
        );
    }

    // 指令逻辑
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (source.getEntity() instanceof ServerPlayer player) {
            CompoundTag persistentData = player.getPersistentData(); // 获取玩家的持久化数据
            broadcastClaimMessage(player);

            // 检查是否已经领取过
            if (persistentData.getBoolean(TAG_KEY)) {
                player.displayClientMessage(Component.literal("§c你已经领取过新手礼包，无法再次领取！"), false);
                broadcastClaimResultMessage(player, false);
            } else {
                EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
                data.addBalance(player.getUUID(), 10000);

                // 成功领取，记录状态
                persistentData.putBoolean(TAG_KEY, true);
                player.displayClientMessage(Component.literal("§a成功领取新手礼包！你获得了 10000 枚梦鱼币！"), false);
                broadcastClaimResultMessage(player, true);
            }
        } else {
            source.sendFailure(Component.literal("§c该指令只能由玩家使用！"));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static void broadcastClaimMessage(ServerPlayer claimer) {
        String claimerName = claimer.getName().getString();
        CompoundTag persistentData = claimer.getPersistentData(); // 获取玩家的持久化数据

        // 构建消息内容
        Component message = Component.literal("[Debug] 玩家 \" " + claimerName + " \" 尝试领取新手礼包 玩家数据: " +  persistentData.getBoolean(TAG_KEY));

        // 向所有其他玩家发送消息
        claimer.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (player.getUUID().equals(claimer.getUUID())) { // 排除抢红包的玩家
                player.sendSystemMessage(message);
            }
        });
    }

    private static void broadcastClaimResultMessage(ServerPlayer claimer, boolean is) {
        String claimerName = claimer.getName().getString();
        CompoundTag persistentData = claimer.getPersistentData(); // 获取玩家的持久化数据

        Component message;
        if (is) {
            // 构建消息内容
            message = Component.literal("[Debug] 玩家 \" " + claimerName + " \" 成功领取新手礼包 玩家数据: " +  persistentData.getBoolean(TAG_KEY));
        } else {
            // 构建消息内容
            message = Component.literal("[Debug] 玩家 \" " + claimerName + " \" 领取新手礼包失败 玩家数据: " +  persistentData.getBoolean(TAG_KEY));
        }



        // 向所有其他玩家发送消息
        claimer.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (player.getUUID().equals(claimer.getUUID())) { // 排除抢红包的玩家
                player.sendSystemMessage(message);
            }
        });
    }
}
