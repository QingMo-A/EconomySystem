package com.mo.economy_system.target.neoforge1211;

import com.mo.economy_system.common.economy.ShopPurchaseService;
import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211TransactionalInventoryAdapter;
import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/** NeoForge API adapter for the common shop purchase transaction. */
public final class NeoForge1211ShopPurchaseAdapter {
  private static final Logger LOGGER = LogUtils.getLogger();

  private NeoForge1211ShopPurchaseAdapter() {}

  public static void execute(ServerPlayer player, ShopBuyItemMessage message) {
    Objects.requireNonNull(player, "player");
    UUID playerId = player.getUUID();
    NeoForge1211TransactionalInventoryAdapter inventory =
        new NeoForge1211TransactionalInventoryAdapter(player);
    ShopPurchaseService.execute(
        message,
        new ShopPurchaseService.Context(
            playerId,
            new Catalog(player),
            inventory,
            new Accounts(player),
            new Feedback(player)));
  }

  private record Catalog(ServerPlayer player) implements ShopPurchaseService.Catalog {
    @Override
    public ShopItemSnapshot find(String shopItemId) {
      return EconomyServices.platform().shopCatalog().findByShopItemId(shopItemId);
    }

    @Override
    public ShopPurchaseService.MaterializedItem materialize(ShopItemSnapshot item) {
      ItemStack stack = NeoForge1211Platform.nativeShopCatalog().createItemStack(
          item, player.registryAccess());
      if (stack == null || stack.isEmpty()) return null;
      return new ShopPurchaseService.MaterializedItem(stack, stack.getHoverName().getString());
    }

    @Override
    public boolean recordPurchase(String shopItemId, int quantity) {
      return EconomyServices.platform().shopCatalog().recordPurchase(shopItemId, quantity);
    }
  }

  private record Accounts(ServerPlayer player) implements ShopPurchaseService.Accounts {
    @Override
    public BalanceMutationResult debit(UUID playerId, int amount, String category, String reason) {
      return EconomySavedData.getInstance(player.serverLevel())
          .debitExact(playerId, amount, category, reason);
    }

    @Override
    public BalanceMutationResult credit(UUID playerId, int amount, String category, String reason) {
      return EconomySavedData.getInstance(player.serverLevel())
          .creditExact(playerId, amount, category, reason);
    }
  }

  private record Feedback(ServerPlayer player) implements ShopPurchaseService.Feedback {
    @Override
    public void send(UUID playerId, String translationKey, Object... arguments) {
      if (player.getUUID().equals(playerId)) {
        player.sendSystemMessage(Component.translatable(translationKey, arguments));
      }
    }

    @Override
    public void report(String stage, RuntimeException failure) {
      if (failure == null) {
        LOGGER.warn("Shop purchase compensation warning at {}", stage);
      } else {
        LOGGER.error("Shop purchase failed at {} for {}", stage, player.getUUID(), failure);
      }
    }
  }
}
