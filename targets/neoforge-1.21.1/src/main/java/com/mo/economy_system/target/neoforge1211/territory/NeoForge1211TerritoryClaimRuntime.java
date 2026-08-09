package com.mo.economy_system.target.neoforge1211.territory;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.territory.TerritoryClaimService;
import com.mo.economy_system.common.territory.TerritoryClaimCreationPolicy;
import com.mo.economy_system.common.territory.TerritoryGeometry;
import com.mo.economy_system.common.territory.TerritorySelectionService;
import com.mo.economy_system.common.territory.TerritorySnapshots;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryBuffManager;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.territory_system.TerritoryNetworkSnapshots;
import com.mo.economy_system.utils.Util_MessageKeys;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** NeoForge adapter for the common initial-territory claim transaction. */
public final class NeoForge1211TerritoryClaimRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();

  private NeoForge1211TerritoryClaimRuntime() {}

  public static int confirm(ServerPlayer player, String name) {
    UUID playerId = player.getUUID();
    TerritorySelectionService.Session selection =
        NeoForge1211TerritorySelectionRuntime.claimSession(player).orElse(null);
    if (selection == null || selection.first().isEmpty() || selection.second().isEmpty()) {
      player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_WAND_SELECT_POINTS));
      return 0;
    }

    final TerritoryClaimService.Request request;
    try {
      request = new TerritoryClaimService.Request(
          playerId,
          player.getGameProfile().getName(),
          name,
          player.serverLevel().dimension().location().toString(),
          position(selection.first().orElseThrow()),
          position(selection.second().orElseThrow()));
    } catch (IllegalArgumentException invalidInput) {
      player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_RESIZE_FAILED));
      return 0;
    }

    EconomySavedData economy = EconomySavedData.getInstance(player.serverLevel());
    TerritoryClaimService.Outcome outcome = TerritoryClaimService.execute(
        request,
        new TerritoryClaimService.Balance() {
          @Override
          public com.mo.economy_system.core.economy_system.BalanceMutationResult debitExact(
              UUID ownerId, int amount) {
            return economy.debitExact(ownerId, amount, "领地", "购买领地");
          }

          @Override
          public com.mo.economy_system.core.economy_system.BalanceMutationResult creditExact(
              UUID ownerId, int amount) {
            return economy.creditExact(ownerId, amount, "领地", "领地创建失败退款");
          }
        },
        new TerritoryClaimService.Repository() {
          @Override
          public boolean overlaps(TerritoryClaimService.Request value) {
            TerritoryGeometry.Rectangle candidate = TerritoryGeometry.rectangle(
                value.first(), value.second());
            return TerritoryManager.getAllTerritories().stream().anyMatch(existing ->
                existing.getDimension().location().toString().equals(value.dimensionId())
                    && candidate.intersects(TerritoryGeometry.rectangle(
                        position(existing.getPos1()), position(existing.getPos2()))));
          }

          @Override
          public TerritoryClaimService.RepositoryResult create(
              TerritoryClaimService.Request value, long area, int price) {
            try {
              var snapshot = TerritoryClaimCreationPolicy.create(
                  value, TerritoryBuffManager.catalog());
              Territory territory = TerritoryNetworkSnapshots.restoreOwned(snapshot);
              TerritoryManager.addTerritory(territory);
              return TerritoryManager.getTerritoryByID(territory.getTerritoryID()) == territory
                  ? TerritoryClaimService.RepositoryResult.CREATED
                  : TerritoryClaimService.RepositoryResult.STATE_UNKNOWN;
            } catch (RuntimeException failure) {
              LOGGER.warn("NeoForge territory claim persistence failed player={}",
                  value.ownerId(), failure);
              return failure.getSuppressed().length == 0
                  ? TerritoryClaimService.RepositoryResult.PERSIST_FAILED
                  : TerritoryClaimService.RepositoryResult.STATE_UNKNOWN;
            }
          }
        },
        (stage, owner, failure) -> LOGGER.warn(
            "NeoForge territory claim stage={} player={}", stage, owner, failure));

    switch (outcome.result()) {
      case SUCCESS -> {
        player.sendSystemMessage(Component.translatable(
            Util_MessageKeys.CLAIM_SUCCESS, request.territoryName(), outcome.price()));
        NeoForge1211TerritorySelectionRuntime.clear(player.getServer(), playerId);
        return 1;
      }
      case OVERLAP -> player.sendSystemMessage(
          Component.translatable(Util_MessageKeys.CLAIM_WAND_OVERLAP_ERROR));
      case INSUFFICIENT_FUNDS -> player.sendSystemMessage(Component.translatable(
          Util_MessageKeys.CLAIM_INSUFFICIENT_BALANCE, outcome.price()));
      case PERSIST_FAILED -> player.sendSystemMessage(
          Component.translatable("message.claim.resize.persist_failed"));
      case STATE_UNKNOWN, REFUND_FAILED -> player.sendSystemMessage(
          Component.translatable("message.claim.resize.state_unknown"));
      case INVALID_INPUT, PRICE_OVERFLOW, PAYMENT_FAILED -> player.sendSystemMessage(
          Component.translatable(Util_MessageKeys.CLAIM_RESIZE_FAILED));
    }
    if (outcome.result() == TerritoryClaimService.Result.STATE_UNKNOWN
        || outcome.result() == TerritoryClaimService.Result.REFUND_FAILED) {
      NeoForge1211TerritorySelectionRuntime.clear(player.getServer(), playerId);
    }
    return 0;
  }

  private static TerritorySnapshots.Position position(TerritorySelectionService.Point value) {
    return new TerritorySnapshots.Position(value.x(), value.y(), value.z());
  }

  private static TerritorySnapshots.Position position(BlockPos value) {
    return new TerritorySnapshots.Position(value.getX(), value.getY(), value.getZ());
  }
}
