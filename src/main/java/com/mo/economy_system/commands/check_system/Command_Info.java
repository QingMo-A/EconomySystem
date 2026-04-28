package com.mo.economy_system.commands.check_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.PlayerInfo;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.stream.Collectors;

/**
 * Command_Info 类用于注册和处理与信息查询相关的命令。
 */
public class Command_Info {

    /**
     * 注册信息查询命令，包括玩家余额、领地信息和物品信息子命令。
     *
     * @param dispatcher 命令分发器，用于注册命令
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("info")
                        // balance 子指令
                        .then(Commands.literal("player")
                                // 添加 player 参数
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> showPlayerInfo(context.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(context, "player"))))
                                .executes(context -> showPlayerInfo(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException())))
                        // territory 子指令
                        .then(Commands.literal("territory")
                                .executes(context -> showTerritory(context.getSource().getPlayerOrException())))
                        // item 子指令
                        .then(Commands.literal("item")
                                .executes(context -> showItemInfo(context.getSource().getPlayerOrException(), context.getSource().getPlayerOrException())))
        );
    }

    /**
     * 显示指定玩家的余额信息。
     *
     * @param requester 请求者玩家，用于发送消息
     * @param target    目标玩家，用于查询余额
     * @return 返回值为 1 表示成功执行
     */
    private static int showPlayerInfo(ServerPlayer requester, ServerPlayer target) {
        ServerLevel serverLevel = target.serverLevel(); // 获取服务器世界实例
        EconomySavedData data = EconomySavedData.getInstance(serverLevel);
        int balance = data.getBalance(target.getUUID());

        requester.sendSystemMessage(Component.literal(target.getName().getString() + " 拥有 " + balance + " 枚梦鱼币"));

        return 1;
    }

    /**
     * 显示当前手持物品的 NBT 数据信息。
     *
     * @param requester 请求者玩家，用于发送消息
     * @param target    目标玩家，用于获取手持物品
     * @return 返回值为 1 表示成功执行
     */
    private static int showItemInfo(ServerPlayer requester, ServerPlayer target) {
        ItemStack itemStack = requester.getItemInHand(InteractionHand.MAIN_HAND);

        // 获取物品的 NBT 数据
        CompoundTag nbt = com.mo.economy_system.utils.ItemStackCompat.getTag(itemStack);

        if (nbt != null) {
            // 遍历 NBT 中的所有键并打印不同类型的数据
            for (String key : nbt.getAllKeys()) {
                System.out.println("键名: " + key);

                // 打印不同类型的值
                if (nbt.contains(key, 8)) { // 8 表示字符串类型
                    System.out.println("值 (String): " + nbt.getString(key));
                } else if (nbt.contains(key, 3)) { // 3 表示整数类型
                    System.out.println("值 (Integer): " + nbt.getInt(key));
                } else if (nbt.contains(key, 10)) { // 10 表示 CompoundTag 类型
                    System.out.println("值 (Compound): " + nbt.getCompound(key));
                } else if (nbt.contains(key, 9)) { // 9 表示 ListTag 类型
                    System.out.println("值 (List): " + nbt.getList(key, 10));  // 假设 List 是由 CompoundTag 组成
                }
            }
        } else {
            System.out.println("该物品没有 NBT 数据");
        }

        return 1;
    }

    /**
     * 显示玩家所在领地的信息。
     *
     * @param sender 发送命令的玩家
     * @return 返回值为 1 表示成功执行，返回值为 0 表示未处于领地中
     */
    private static int showTerritory(ServerPlayer sender) {
        Vec3 senderPos = sender.position();
        int x = (int) Math.floor(senderPos.x);
        int z = (int) Math.floor(senderPos.z);

        Territory territory = TerritoryManager.getTerritoryAtIgnoreY(x, z);
        if (territory == null) {
            sender.sendSystemMessage(Component.literal("你未处于领地中"));
            return 0;
        } else {
            // 检查是否有授权玩家并生成悬浮提示文字
            String hoverTextContent = territory.getAuthorizedPlayers().isEmpty() ?
                    "§7无" :
                    territory.getAuthorizedPlayers().stream()
                            .map(PlayerInfo::getName)
                            .collect(Collectors.joining("\n"));

            Component hoverText = Component.literal(hoverTextContent);
            Component messageWithHover = Component.literal("[领地成员]").withStyle(style ->
                    style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText))
            );

            // 发送领地信息给玩家
            sender.sendSystemMessage(Component.literal(
                            "-----------------------" + "\n" +
                                    "领地名称: " + territory.getName() + "\n" +
                                    "领地UUID: " + territory.getTerritoryID() + "\n" +
                                    "领地所有者: " + territory.getOwnerName() + "\n" +
                                    "领地所有者UUID: " + territory.getOwnerUUID() + "\n"
                    ).append(messageWithHover)
            );
        }
        return 1;
    }
}
