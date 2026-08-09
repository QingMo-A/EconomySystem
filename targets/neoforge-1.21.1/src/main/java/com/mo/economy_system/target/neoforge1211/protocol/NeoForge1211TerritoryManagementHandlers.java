package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseMessage;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UnlockTerritoryBuffMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.network.UpgradeTerritoryBuffMessage;
import com.mo.economy_system.common.territory.SingleTerritoryQueryService;
import com.mo.economy_system.common.territory.TerritoryAdministrationService;
import com.mo.economy_system.common.territory.TerritoryBuffTransactionService;
import com.mo.economy_system.common.territory.TerritoryManagementResult;
import com.mo.economy_system.common.territory.TerritoryModifyModeService;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.territory_system.TerritoryNetworkSnapshots;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211SingleTerritoryClientState;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211TerritoryDetailScreen;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211TerritoryManageScreen;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211BuffManageScreen;
import com.mo.economy_system.target.neoforge1211.territory.NeoForge1211TerritorySelectionRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NeoForge1211TerritoryManagementHandlers {
  private NeoForge1211TerritoryManagementHandlers() {}

  public static void modifyMode(ModifyTerritoryModeMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      TerritoryManagementResult result = TerritoryModifyModeService.start(
          message,
          player.getUUID(),
          player.serverLevel().dimension().location().toString(),
          territoryId -> {
            Territory territory = TerritoryManager.getTerritoryByID(territoryId);
            return territory == null ? null : TerritoryNetworkSnapshots.summary(territory);
          },
          territoryId -> {
            NeoForge1211TerritorySelectionRuntime.startResize(player, territoryId);
            return NeoForge1211TerritorySelectionRuntime.hasResize(player);
          });
      if (result != TerritoryManagementResult.SUCCESS) feedback(player, result);
    });
  }

  public static void unlockBuff(UnlockTerritoryBuffMessage message, IPayloadContext context) {
    buff(message.territoryId(), message.buffId(), TerritoryBuffTransactionService.Action.UNLOCK, context);
  }

  public static void upgradeBuff(UpgradeTerritoryBuffMessage message, IPayloadContext context) {
    buff(message.territoryId(), message.buffId(), TerritoryBuffTransactionService.Action.UPGRADE, context);
  }

  private static void buff(
      UUID territoryId,
      String buffId,
      TerritoryBuffTransactionService.Action action,
      IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
      TerritoryManagementResult result = TerritoryBuffTransactionService.execute(
          territoryId,
          buffId,
          action,
          new TerritoryBuffTransactionService.Context(
              player.getUUID(),
              player.serverLevel().dimension().location().toString(),
              new TerritoryBuffRepository(),
              new TerritoryAccounts(accounts, player.getUUID(), action),
              new TerritoryResources(player),
              (owner, territory, buff, stage, failure) -> EconomySystem.LOGGER.error(
                  "Territory buff transaction failed owner={} territory={} buff={} stage={}",
                  owner, territory, buff, stage, failure)));
      feedback(player, result);
    });
  }

  public static void singleRequest(
      SingleTerritoryDataRequestMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      SingleTerritoryDataResponseMessage response = SingleTerritoryQueryService.query(
          message,
          player.getUUID(),
          territoryId -> {
            Territory territory = TerritoryManager.getTerritoryByID(territoryId);
            return territory == null ? null : TerritoryNetworkSnapshots.owned(territory);
          });
      EconomySystem_NetworkManager.sendToClient(player, response);
    });
  }

  public static void singleResponse(
      SingleTerritoryDataResponseMessage message, IPayloadContext context) {
    context.enqueueWork(() -> {
      Minecraft minecraft = Minecraft.getInstance();
      NeoForge1211SingleTerritoryClientState.apply(message);
      if (minecraft.screen instanceof NeoForge1211TerritoryManageScreen screen) {
        screen.applyResponse(message.requestId(), message);
        return;
      }
      if (minecraft.screen instanceof NeoForge1211BuffManageScreen) {
        return;
      }
      if (minecraft.screen instanceof NeoForge1211TerritoryDetailScreen screen) {
        screen.applyResponse(message.requestId(), message);
        return;
      }
    });
  }

  public static void permission(
      UpdateTerritoryPermissionMessage message, IPayloadContext context) {
    admin(context, (player, service) ->
        TerritoryAdministrationService.permission(message, player.getUUID(), service));
  }

  public static void transfer(
      TransferTerritoryOwnershipMessage message, IPayloadContext context) {
    admin(context, (player, service) ->
        TerritoryAdministrationService.transfer(message, player.getUUID(), service));
  }

  public static void rule(UpdateTerritoryRuleMessage message, IPayloadContext context) {
    admin(context, (player, service) ->
        TerritoryAdministrationService.rule(message, player.getUUID(), service));
  }

  private static void admin(IPayloadContext context, AdminAction action) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      TerritoryAdministrationService.Context service = new TerritoryAdministrationService.Context(
          new TerritoryAdminRepository(),
          playerId -> playerName(player, playerId),
          (territory, sender, stage, failure) -> EconomySystem.LOGGER.error(
              "Territory administration failed territory={} sender={} stage={}",
              territory, sender, stage, failure));
      feedback(player, action.execute(player, service));
    });
  }

  private static Optional<String> playerName(ServerPlayer sender, UUID playerId) {
    ServerPlayer online = sender.server.getPlayerList().getPlayer(playerId);
    if (online != null) return Optional.of(online.getGameProfile().getName());
    return sender.server.getProfileCache().get(playerId).map(profile -> profile.getName());
  }

  private static void feedback(ServerPlayer player, TerritoryManagementResult result) {
    player.sendSystemMessage(Component.translatable("message.territory.management." + result.name().toLowerCase(java.util.Locale.ROOT)));
  }

  @FunctionalInterface
  private interface AdminAction {
    TerritoryManagementResult execute(
        ServerPlayer player, TerritoryAdministrationService.Context context);
  }

  private static final class TerritoryAdminRepository
      implements TerritoryAdministrationService.Repository {
    public Owned find(UUID territoryId) {
      Territory territory = TerritoryManager.getTerritoryByID(territoryId);
      return territory == null ? null : TerritoryNetworkSnapshots.owned(territory);
    }

    public TerritoryAdministrationService.RepositoryResult apply(Owned expected, Owned replacement) {
      return TerritoryManager.applyTerritoryAdministrationAuthoritatively(expected, replacement);
    }
  }

  private static final class TerritoryBuffRepository
      implements TerritoryBuffTransactionService.Repository {
    public Owned find(UUID territoryId) {
      Territory territory = TerritoryManager.getTerritoryByID(territoryId);
      return territory == null ? null : TerritoryNetworkSnapshots.owned(territory);
    }

    public TerritoryBuffTransactionService.RepositoryResult mutate(
        UUID territoryId,
        UUID owner,
        String buffId,
        boolean unlocked,
        int level,
        boolean newUnlocked,
        int newLevel) {
      return TerritoryManager.mutateTerritoryBuffAuthoritatively(
          territoryId, owner, buffId, unlocked, level, newUnlocked, newLevel);
    }
  }

  private record TerritoryAccounts(
      EconomySavedData data, UUID playerId, TerritoryBuffTransactionService.Action action)
      implements TerritoryBuffTransactionService.Accounts {
    public BalanceMutationResult preview(int amount) {
      return amount > 0 && data.hasEnoughBalance(playerId, amount)
          ? BalanceMutationResult.SUCCESS
          : BalanceMutationResult.INSUFFICIENT_FUNDS;
    }

    public BalanceMutationResult debit(int amount) {
      return data.debitExact(
          playerId, amount, "领地",
          action == TerritoryBuffTransactionService.Action.UNLOCK ? "解锁领地增益" : "升级领地增益");
    }

    public BalanceMutationResult refund(int amount) {
      return data.creditExact(playerId, amount, "领地", "领地增益事务退款");
    }
  }

  private static final class TerritoryResources implements TerritoryBuffTransactionService.Resources {
    private final ServerPlayer player;

    private TerritoryResources(ServerPlayer player) {
      this.player = player;
    }

    public int experienceLevel() {
      return player.experienceLevel;
    }

    public boolean canRemove(Map<String, Integer> items) {
      for (Map.Entry<String, Integer> requirement : items.entrySet()) {
        Item item = item(requirement.getKey());
        if (item == null) return false;
        long found = 0;
        for (ItemStack stack : player.getInventory().items) {
          if (stack.getItem() == item) found += stack.getCount();
        }
        if (found < requirement.getValue()) return false;
      }
      return true;
    }

    public boolean debitExperience(int levels) {
      int before = player.experienceLevel;
      if (levels < 0 || before < levels) return false;
      RuntimeException mutationFailure = null;
      try {
        player.giveExperienceLevels(-levels);
        if (player.experienceLevel == before - levels) return true;
      } catch (RuntimeException failure) {
        mutationFailure = failure;
      }
      try {
        if (player.experienceLevel != before) {
          player.giveExperienceLevels(before - player.experienceLevel);
        }
      } catch (RuntimeException rollbackFailure) {
        if (mutationFailure != null) rollbackFailure.addSuppressed(mutationFailure);
        throw new IllegalStateException("experience rollback failed", rollbackFailure);
      }
      if (player.experienceLevel != before) {
        IllegalStateException unknown = new IllegalStateException("experience rollback not proven");
        if (mutationFailure != null) unknown.addSuppressed(mutationFailure);
        throw unknown;
      }
      return false;
    }

    public boolean refundExperience(int levels) {
      int before = player.experienceLevel;
      player.giveExperienceLevels(levels);
      return player.experienceLevel == before + levels;
    }

    public TerritoryBuffTransactionService.ItemRemoval remove(Map<String, Integer> items) {
      List<ItemStack> before = player.getInventory().items.stream().map(ItemStack::copy).toList();
      try {
        for (Map.Entry<String, Integer> requirement : items.entrySet()) {
          Item item = item(requirement.getKey());
          if (item == null) return TerritoryBuffTransactionService.ItemRemoval.failure(restore(before));
          int remaining = requirement.getValue();
          for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item && remaining > 0) {
              int removed = Math.min(stack.getCount(), remaining);
              stack.shrink(removed);
              remaining -= removed;
            }
          }
          if (remaining != 0) return TerritoryBuffTransactionService.ItemRemoval.failure(restore(before));
        }
        player.getInventory().setChanged();
        return TerritoryBuffTransactionService.ItemRemoval.success(() -> restore(before));
      } catch (RuntimeException failure) {
        return TerritoryBuffTransactionService.ItemRemoval.failure(restore(before));
      }
    }

    private boolean restore(List<ItemStack> before) {
      boolean restored = true;
      for (int index = 0; index < before.size(); index++) {
        try {
          player.getInventory().setItem(index, before.get(index).copy());
        } catch (RuntimeException failure) {
          restored = false;
        }
      }
      try {
        player.getInventory().setChanged();
      } catch (RuntimeException failure) {
        restored = false;
      }
      return restored;
    }

    private static Item item(String id) {
      ResourceLocation location = ResourceLocation.tryParse(id);
      return location != null && BuiltInRegistries.ITEM.containsKey(location)
          ? BuiltInRegistries.ITEM.get(location)
          : null;
    }
  }
}
