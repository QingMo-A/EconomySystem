package com.mo.economy_system.commands.territory_system;

import com.mo.economy_system.item.items.Item_ClaimWand;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class Command_TerritoryClaim {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("confirm_claim")
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            UUID playerUUID = player.getUUID();

                            // 检查玩家是否有两个选定点
                            if (Item_ClaimWand.getFirstPosition(playerUUID) == null || Item_ClaimWand.getSecondPosition(playerUUID) == null) {
                                player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_WAND_SELECT_POINTS));
                                return 0;
                            }

                            // 获取选定的点
                            BlockPos firstPos = Item_ClaimWand.getFirstPosition(playerUUID);
                            BlockPos secondPos = Item_ClaimWand.getSecondPosition(playerUUID);

                            // 计算价格
                            int volume = calculateVolume(firstPos, secondPos);
                            int price = volume * 20;

                            // 检查余额
                            EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
                            if (data.getBalance(playerUUID) < price) {
                                player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_INSUFFICIENT_BALANCE, price));
                                return 0;
                            }

                            // 扣除金额并创建领地
                            String name = StringArgumentType.getString(context, "name");
                            Territory territory = new Territory(name, playerUUID, player.getName().getString(), firstPos.getX(), firstPos.getY(), firstPos.getZ(), secondPos.getX(), secondPos.getY(), secondPos.getZ(), firstPos, player.level().dimension());
                            territory.setBackpoint(firstPos);
                            TerritoryManager.addTerritory(territory);
                            data.minBalance(playerUUID, price);

                            player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_SUCCESS, name, price));
                            Item_ClaimWand.clearPositions(playerUUID); // 清除点位记录
                            return 1;
                        })));
        dispatcher.register(Commands.literal("confirm_modify")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    UUID playerUUID = player.getUUID();

                    // 检查玩家是否有两个选定点
                    if (Item_ClaimWand.isResizing(playerUUID) == false || Item_ClaimWand.getFirstModifyPosition(playerUUID) == null || Item_ClaimWand.getSecondModifyPosition(playerUUID) == null || Item_ClaimWand.getModifyVolume(playerUUID) == 0) {
                        player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_RESIZE_FAILED));
                        return 0;
                    }

                    // 获取选定的点
                    BlockPos firstPos = Item_ClaimWand.getFirstModifyPosition(playerUUID);
                    BlockPos secondPos = Item_ClaimWand.getSecondModifyPosition(playerUUID);
                    Territory t = TerritoryManager.getTerritoryByID(Item_ClaimWand.getResizingTerritoryID(player));
                    TerritoryManager.removeTerritory(t.getTerritoryID());

                    // 检查余额
                    EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());

                    int volume = Item_ClaimWand.getModifyVolume(playerUUID);
                    if (volume > 0) {
                        int price = Item_ClaimWand.getModifyVolume(playerUUID) * 20;

                        if (data.minBalance(playerUUID, price)) {
                            t.setBackpoint(firstPos);
                            t.setX1(firstPos.getX());
                            t.setY1(firstPos.getY());
                            t.setZ1(firstPos.getZ());
                            t.setX2(secondPos.getX());
                            t.setY2(secondPos.getY());
                            t.setZ2(secondPos.getZ());

                            player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_RESIZE_SUCCESS));
                            Item_ClaimWand.clearPositions(playerUUID);
                        } else {
                            player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_RESIZE_INSUFFICIENT_BALANCE));
                        }
                    } else {
                        t.setBackpoint(firstPos);
                        t.setX1(firstPos.getX());
                        t.setY1(firstPos.getY());
                        t.setZ1(firstPos.getZ());
                        t.setX2(secondPos.getX());
                        t.setY2(secondPos.getY());
                        t.setZ2(secondPos.getZ());

                        player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_RESIZE_SUCCESS));
                        Item_ClaimWand.clearPositions(playerUUID);
                    }
                    TerritoryManager.addTerritory(t);
                    return 1;
                })
        );
    }

    private static int calculateVolume(BlockPos pos1, BlockPos pos2) {
        int xSize = Math.abs(pos2.getX() - pos1.getX()) + 1;
        int zSize = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        return xSize * zSize; // 计算体积
    }
}
