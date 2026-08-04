package com.mo.economy_system.commands.territory_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.territory_system.TerritoryResizeTransactionService;
import com.mo.economy_system.item.items.Item_ClaimWand;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class Command_TerritoryClaim {

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("confirm_claim")
            .then(
                Commands.argument("name", StringArgumentType.string())
                    .executes(
                        context -> {
                          ServerPlayer player = context.getSource().getPlayerOrException();
                          UUID playerUUID = player.getUUID();

                          // 检查玩家是否有两个选定点
                          if (Item_ClaimWand.getFirstPosition(playerUUID) == null
                              || Item_ClaimWand.getSecondPosition(playerUUID) == null) {
                            player.sendSystemMessage(
                                Component.translatable(Util_MessageKeys.CLAIM_WAND_SELECT_POINTS));
                            return 0;
                          }

                          // 获取选定的点
                          BlockPos firstPos = Item_ClaimWand.getFirstPosition(playerUUID);
                          BlockPos secondPos = Item_ClaimWand.getSecondPosition(playerUUID);

                          // 计算价格
                          long volume = calculateVolume(firstPos, secondPos);
                          long price = calculatePrice(volume);

                          // 检查余额
                          EconomySavedData data =
                              EconomySavedData.getInstance(player.serverLevel());
                          if (price > EconomySavedData.MAX_BALANCE
                              || !data.hasEnoughBalance(playerUUID, (int) price)) {
                            player.sendSystemMessage(
                                Component.translatable(
                                    Util_MessageKeys.CLAIM_INSUFFICIENT_BALANCE, price));
                            return 0;
                          }

                          // 先扣费再创建领地，避免扣费失败时留下免费领地。
                          if (!data.minBalance(playerUUID, (int) price, "领地", "购买领地")) {
                            player.sendSystemMessage(
                                Component.translatable(
                                    Util_MessageKeys.CLAIM_INSUFFICIENT_BALANCE, price));
                            return 0;
                          }

                          String name = StringArgumentType.getString(context, "name");
                          Territory territory =
                              new Territory(
                                  name,
                                  playerUUID,
                                  player.getName().getString(),
                                  firstPos.getX(),
                                  firstPos.getY(),
                                  firstPos.getZ(),
                                  secondPos.getX(),
                                  secondPos.getY(),
                                  secondPos.getZ(),
                                  firstPos,
                                  player.level().dimension());
                          territory.setBackpoint(firstPos);
                          TerritoryManager.addTerritory(territory);

                          player.sendSystemMessage(
                              Component.translatable(Util_MessageKeys.CLAIM_SUCCESS, name, price));
                          Item_ClaimWand.clearPositions(playerUUID); // 清除点位记录
                          return 1;
                        })));
    dispatcher.register(
        Commands.literal("confirm_modify")
            .executes(
                context -> {
                  ServerPlayer player = context.getSource().getPlayerOrException();
                  UUID playerUUID = player.getUUID();

                  // 检查玩家是否有两个选定点
                  if (!Item_ClaimWand.isResizing(playerUUID)
                      || Item_ClaimWand.getFirstModifyPosition(playerUUID) == null
                      || Item_ClaimWand.getSecondModifyPosition(playerUUID) == null
                      || Item_ClaimWand.getModifyVolume(playerUUID) == 0L) {
                    player.sendSystemMessage(
                        Component.translatable(Util_MessageKeys.CLAIM_RESIZE_FAILED));
                    return 0;
                  }

                  // 获取选定的点
                  BlockPos firstPos = Item_ClaimWand.getFirstModifyPosition(playerUUID);
                  BlockPos secondPos = Item_ClaimWand.getSecondModifyPosition(playerUUID);
                  Territory t =
                      TerritoryManager.getTerritoryByID(
                          Item_ClaimWand.getResizingTerritoryID(player));
                  if (t == null
                      || !t.isOwner(playerUUID)
                      || !player.serverLevel().dimension().equals(t.getDimension())) {
                    player.sendSystemMessage(
                        Component.translatable(Util_MessageKeys.CLAIM_RESIZE_FAILED));
                    Item_ClaimWand.clearPositions(playerUUID);
                    return 0;
                  }

                  // 检查余额
                  EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());

                  TerritoryResizeTransactionService.Outcome outcome =
                      TerritoryResizeTransactionService.execute(
                          data,
                          playerUUID,
                          t.getTerritoryID(),
                          firstPos,
                          secondPos,
                          Item_ClaimWand.getModifyVolume(playerUUID));
                  String key =
                      switch (outcome.result()) {
                        case SUCCESS -> Util_MessageKeys.CLAIM_RESIZE_SUCCESS;
                        case INSUFFICIENT_FUNDS ->
                            Util_MessageKeys.CLAIM_RESIZE_INSUFFICIENT_BALANCE;
                        case OVERLAP -> "message.claim.resize.overlap";
                        case STATE_UNKNOWN -> "message.claim.resize.state_unknown";
                        case REFUND_FAILED -> "message.claim.resize.refund_failed";
                        case PERSIST_FAILED -> "message.claim.resize.persist_failed";
                        default -> Util_MessageKeys.CLAIM_RESIZE_FAILED;
                      };
                  player.sendSystemMessage(Component.translatable(key));
                  if (outcome.result() == TerritoryResizeTransactionService.Result.SUCCESS
                      || outcome.result()
                          == TerritoryResizeTransactionService.Result.STATE_UNKNOWN) {
                    Item_ClaimWand.clearPositions(playerUUID);
                  }
                  return outcome.result() == TerritoryResizeTransactionService.Result.SUCCESS
                      ? 1
                      : 0;
                }));
  }

  private static long calculateVolume(BlockPos pos1, BlockPos pos2) {
    long xSize = Math.abs((long) pos2.getX() - pos1.getX()) + 1L;
    long zSize = Math.abs((long) pos2.getZ() - pos1.getZ()) + 1L;
    return xSize * zSize; // 计算体积
  }

  private static long calculatePrice(long volume) {
    if (volume > Long.MAX_VALUE / 20L) {
      return Long.MAX_VALUE;
    }
    return volume * 20L;
  }
}
