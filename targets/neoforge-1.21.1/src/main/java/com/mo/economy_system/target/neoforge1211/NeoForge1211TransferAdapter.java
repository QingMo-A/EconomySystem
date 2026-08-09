package com.mo.economy_system.target.neoforge1211;

import com.mo.economy_system.common.economy.TransferService;
import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** NeoForge API adapter for the common transfer policy. */
public final class NeoForge1211TransferAdapter {
  private NeoForge1211TransferAdapter() {}

  public static BalanceTransferResult execute(ServerPlayer sender, TransferMessage message) {
    Objects.requireNonNull(sender, "sender");
    MinecraftServer server = Objects.requireNonNull(sender.getServer(), "server");
    return TransferService.execute(identity(sender), message, new Port(server, sender));
  }

  private static TransferService.PlayerIdentity identity(ServerPlayer player) {
    return new TransferService.PlayerIdentity(
        player.getUUID(), player.getGameProfile().getName());
  }

  private record Port(MinecraftServer server, ServerPlayer sender)
      implements TransferService.TransferPort {
    @Override
    public Optional<TransferService.PlayerIdentity> onlinePlayer(UUID id) {
      ServerPlayer player = server.getPlayerList().getPlayer(id);
      return player == null ? Optional.empty() : Optional.of(identity(player));
    }

    @Override
    public BalanceTransferResult transfer(
        UUID senderId,
        UUID recipientId,
        int amount,
        String category,
        String senderReason,
        String recipientReason) {
      return EconomySavedData.getInstance(sender.serverLevel())
          .transferExact(senderId, recipientId, amount, category, senderReason, recipientReason);
    }

    @Override
    public void send(UUID playerId, String translationKey, Object... arguments) {
      ServerPlayer player = server.getPlayerList().getPlayer(playerId);
      if (player != null) {
        player.sendSystemMessage(Component.translatable(translationKey, arguments));
      }
    }
  }
}
