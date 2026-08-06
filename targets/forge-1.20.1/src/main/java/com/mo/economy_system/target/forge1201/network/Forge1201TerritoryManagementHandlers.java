package com.mo.economy_system.target.forge1201.network;

import com.mojang.logging.LogUtils;
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
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

final class Forge1201TerritoryManagementHandlers {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201TerritoryManagementHandlers() {}

  static void modifyMode(
      ModifyTerritoryModeMessage message, Supplier<NetworkEvent.Context> supplier) {
    server(supplier, player -> {
      Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.get(player.serverLevel());
      TerritoryManagementResult result = TerritoryModifyModeService.start(
          message,
          player.getUUID(),
          player.serverLevel().dimension().location().toString(),
          territoryId -> {
            Owned value = store.findOwned(territoryId);
            return value == null ? null : value.summary();
          },
          territoryId -> Forge1201TerritoryModifySessions.start(player, territoryId));
      if (result != TerritoryManagementResult.SUCCESS) feedback(player, result);
    });
  }

  static void unlockBuff(
      UnlockTerritoryBuffMessage message, Supplier<NetworkEvent.Context> supplier) {
    buff(message.territoryId(), message.buffId(), TerritoryBuffTransactionService.Action.UNLOCK, supplier);
  }

  static void upgradeBuff(
      UpgradeTerritoryBuffMessage message, Supplier<NetworkEvent.Context> supplier) {
    buff(message.territoryId(), message.buffId(), TerritoryBuffTransactionService.Action.UPGRADE, supplier);
  }

  private static void buff(
      UUID territoryId,
      String buffId,
      TerritoryBuffTransactionService.Action action,
      Supplier<NetworkEvent.Context> supplier) {
    server(supplier, player -> {
      Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.get(player.serverLevel());
      EconomySavedData accounts = EconomySavedData.getInstance(player.serverLevel());
      TerritoryManagementResult result = TerritoryBuffTransactionService.execute(
          territoryId,
          buffId,
          action,
          new TerritoryBuffTransactionService.Context(
              player.getUUID(),
              player.serverLevel().dimension().location().toString(),
              new BuffRepository(store),
              new TerritoryAccounts(accounts, player.getUUID(), action),
              new TerritoryResources(player),
              (owner, territory, buff, stage, failure) -> LOGGER.error(
                  "Territory buff transaction failed owner={} territory={} buff={} stage={}",
                  owner, territory, buff, stage, failure)));
      feedback(player, result);
    });
  }

  static void singleRequest(
      SingleTerritoryDataRequestMessage message, Supplier<NetworkEvent.Context> supplier) {
    server(supplier, player -> {
      Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.get(player.serverLevel());
      Forge1201NetworkChannel.sendToPlayer(
          player,
          SingleTerritoryQueryService.query(message, player.getUUID(), store::findOwned));
    });
  }

  static void singleResponse(
      SingleTerritoryDataResponseMessage message, Supplier<NetworkEvent.Context> supplier) {
    NetworkEvent.Context context = supplier.get();
    DistExecutor.unsafeRunWhenOn(
        Dist.CLIENT, () -> () -> Forge1201SingleTerritoryClientState.apply(message));
    context.setPacketHandled(true);
  }

  static void permission(
      UpdateTerritoryPermissionMessage message, Supplier<NetworkEvent.Context> supplier) {
    admin(supplier, (player, context) ->
        TerritoryAdministrationService.permission(message, player.getUUID(), context));
  }

  static void transfer(
      TransferTerritoryOwnershipMessage message, Supplier<NetworkEvent.Context> supplier) {
    admin(supplier, (player, context) ->
        TerritoryAdministrationService.transfer(message, player.getUUID(), context));
  }

  static void rule(
      UpdateTerritoryRuleMessage message, Supplier<NetworkEvent.Context> supplier) {
    admin(supplier, (player, context) ->
        TerritoryAdministrationService.rule(message, player.getUUID(), context));
  }

  private static void admin(Supplier<NetworkEvent.Context> supplier, AdminAction action) {
    server(supplier, player -> {
      Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.get(player.serverLevel());
      TerritoryAdministrationService.Context service = new TerritoryAdministrationService.Context(
          new AdminRepository(store),
          playerId -> playerName(player, playerId),
          (territory, sender, stage, failure) -> LOGGER.error(
              "Territory administration failed territory={} sender={} stage={}",
              territory, sender, stage, failure));
      feedback(player, action.execute(player, service));
    });
  }

  private static void server(
      Supplier<NetworkEvent.Context> supplier, java.util.function.Consumer<ServerPlayer> work) {
    NetworkEvent.Context context = supplier.get();
    ServerPlayer player = context.getSender();
    if (player != null) context.enqueueWork(() -> work.accept(player));
    context.setPacketHandled(true);
  }

  private static Optional<String> playerName(ServerPlayer sender, UUID playerId) {
    ServerPlayer online = sender.server.getPlayerList().getPlayer(playerId);
    if (online != null) return Optional.of(online.getGameProfile().getName());
    return sender.server.getProfileCache().get(playerId).map(profile -> profile.getName());
  }

  private static void feedback(ServerPlayer player, TerritoryManagementResult result) {
    player.sendSystemMessage(Component.translatable(
        "message.territory.management." + result.name().toLowerCase(java.util.Locale.ROOT)));
  }

  @FunctionalInterface
  private interface AdminAction {
    TerritoryManagementResult execute(
        ServerPlayer player, TerritoryAdministrationService.Context context);
  }

  private record AdminRepository(Forge1201TerritorySnapshotStore store)
      implements TerritoryAdministrationService.Repository {
    public Owned find(UUID territoryId) {
      return store.findOwned(territoryId);
    }
    public TerritoryAdministrationService.RepositoryResult setPermission(
        UUID territoryId, UUID owner, UUID target, String name, boolean allowed) {
      return store.setPermission(territoryId, owner, target, name, allowed);
    }
    public TerritoryAdministrationService.RepositoryResult transfer(
        UUID territoryId, UUID owner, UUID target, String name) {
      return store.transfer(territoryId, owner, target, name);
    }
    public TerritoryAdministrationService.RepositoryResult setRule(
        UUID territoryId, UUID owner, RuleAction action, RuleLevel level) {
      return store.setRule(territoryId, owner, action, level);
    }
  }

  private record BuffRepository(Forge1201TerritorySnapshotStore store)
      implements TerritoryBuffTransactionService.Repository {
    public Owned find(UUID territoryId) {
      return store.findOwned(territoryId);
    }
    public TerritoryBuffTransactionService.RepositoryResult mutate(
        UUID territoryId,
        UUID owner,
        String buffId,
        boolean unlocked,
        int level,
        TerritoryBuffTransactionService.Action action) {
      return store.mutateBuff(territoryId, owner, buffId, unlocked, level, action);
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
      try {
        player.giveExperienceLevels(-levels);
        if (player.experienceLevel == before - levels) return true;
        player.giveExperienceLevels(before - player.experienceLevel);
        return false;
      } catch (RuntimeException failure) {
        try {
          player.giveExperienceLevels(before - player.experienceLevel);
        } catch (RuntimeException ignored) {
        }
        return false;
      }
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
