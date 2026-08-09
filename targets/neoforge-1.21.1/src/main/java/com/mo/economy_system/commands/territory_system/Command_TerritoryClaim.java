package com.mo.economy_system.commands.territory_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.territory.TerritorySelectionService;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.territory_system.TerritoryResizeTransactionService;
import com.mo.economy_system.target.neoforge1211.territory.NeoForge1211TerritoryClaimRuntime;
import com.mo.economy_system.target.neoforge1211.territory.NeoForge1211TerritorySelectionRuntime;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** NeoForge command shell for common claim and resize transactions. */
public final class Command_TerritoryClaim {
  private Command_TerritoryClaim() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("confirm_claim")
            .then(
                Commands.argument("name", StringArgumentType.string())
                    .executes(
                        context -> {
                          ServerPlayer player = context.getSource().getPlayerOrException();
                          return NeoForge1211TerritoryClaimRuntime.confirm(
                              player, StringArgumentType.getString(context, "name"));
                        })));
    dispatcher.register(
        Commands.literal("confirm_modify")
            .executes(
                context -> confirmModify(context.getSource().getPlayerOrException())));
  }

  private static int confirmModify(ServerPlayer player) {
    UUID playerId = player.getUUID();
    TerritorySelectionService.Session selection =
        NeoForge1211TerritorySelectionRuntime.resizeSession(player).orElse(null);
    if (selection == null || selection.first().isEmpty() || selection.second().isEmpty()) {
      player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_RESIZE_FAILED));
      return 0;
    }

    Territory territory = TerritoryManager.getTerritoryByID(selection.territoryId());
    if (territory == null
        || !territory.isOwner(playerId)
        || !player.serverLevel().dimension().equals(territory.getDimension())) {
      player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_RESIZE_FAILED));
      NeoForge1211TerritorySelectionRuntime.clear(player.getServer(), playerId);
      return 0;
    }

    TerritoryResizeTransactionService.Outcome outcome =
        TerritoryResizeTransactionService.execute(
            EconomySavedData.getInstance(player.serverLevel()),
            playerId,
            territory.getTerritoryID(),
            blockPos(selection.first().orElseThrow()),
            blockPos(selection.second().orElseThrow()),
            (stage, owner, territoryId, failure) ->
                EconomySystem.LOGGER.warn(
                    "territory resize stage={} player={} territory={}",
                    stage,
                    owner,
                    territoryId,
                    failure));
    player.sendSystemMessage(Component.translatable(resultKey(outcome.result())));
    if (outcome.result() == TerritoryResizeTransactionService.Result.SUCCESS
        || outcome.result() == TerritoryResizeTransactionService.Result.UNCHANGED
        || outcome.result() == TerritoryResizeTransactionService.Result.STATE_UNKNOWN) {
      NeoForge1211TerritorySelectionRuntime.clear(player.getServer(), playerId);
    }
    return outcome.result() == TerritoryResizeTransactionService.Result.SUCCESS
            || outcome.result() == TerritoryResizeTransactionService.Result.UNCHANGED
        ? 1
        : 0;
  }

  private static String resultKey(TerritoryResizeTransactionService.Result result) {
    return switch (result) {
      case SUCCESS -> Util_MessageKeys.CLAIM_RESIZE_SUCCESS;
      case UNCHANGED -> "message.claim.resize.unchanged";
      case INSUFFICIENT_FUNDS -> Util_MessageKeys.CLAIM_RESIZE_INSUFFICIENT_BALANCE;
      case OVERLAP -> "message.claim.resize.overlap";
      case STATE_UNKNOWN -> "message.claim.resize.state_unknown";
      case REFUND_FAILED -> "message.claim.resize.refund_failed";
      case PERSIST_FAILED -> "message.claim.resize.persist_failed";
      default -> Util_MessageKeys.CLAIM_RESIZE_FAILED;
    };
  }

  private static BlockPos blockPos(TerritorySelectionService.Point value) {
    return new BlockPos(value.x(), value.y(), value.z());
  }
}
