package com.mo.economy_system.target.forge1201.redpacket;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.redpacket.RedPacket;
import com.mo.economy_system.common.redpacket.RedPacketAccountPort;
import com.mo.economy_system.common.redpacket.RedPacketFeedback;
import com.mo.economy_system.common.redpacket.RedPacketRepository;
import com.mo.economy_system.common.redpacket.RedPacketService;
import com.mo.economy_system.common.redpacket.RedPacketServiceRegistry;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.List;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Forge API adapter for the common red-packet state machine. */
public final class Forge1201RedPacketRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final RedPacketServiceRegistry<MinecraftServer> SERVICES =
      new RedPacketServiceRegistry<>();

  private Forge1201RedPacketRuntime() {}

  public static RedPacketService service(MinecraftServer server) {
    return SERVICES.get(server, Forge1201RedPacketRuntime::createService);
  }

  public static void expire(MinecraftServer server) {
    publishExpiry(server, service(server).expire());
  }

  public static void shutdown(MinecraftServer server) {
    publishExpiry(server, service(server).refundAll());
    SERVICES.remove(server);
  }

  private static RedPacketService createService(MinecraftServer server) {
    EconomySavedData economy = EconomySavedData.getInstance(server.overworld());
    Forge1201RedPacketSavedData data = Forge1201RedPacketSavedData.getInstance(server.overworld());
    RedPacketAccountPort accounts = new RedPacketAccountPort() {
      @Override
      public com.mo.economy_system.core.economy_system.BalanceMutationResult debit(
          java.util.UUID playerId, int amount, String category, String reason) {
        return economy.debitExact(playerId, amount, category, reason);
      }

      @Override
      public com.mo.economy_system.core.economy_system.BalanceMutationResult credit(
          java.util.UUID playerId, int amount, String category, String reason) {
        return economy.creditExact(playerId, amount, category, reason);
      }
    };
    RedPacketRepository repository = new RedPacketRepository() {
      @Override
      public List<RedPacket> load() {
        return data.packets();
      }

      @Override
      public void save(List<RedPacket> packets) {
        data.replacePackets(packets);
      }
    };
    Random random = new Random();
    return new RedPacketService(
        accounts,
        repository,
        System::currentTimeMillis,
        random::nextInt,
        (operation, senderId, playerId, error) ->
            LOGGER.error("red packet operation={} sender={} player={}", operation, senderId, playerId, error));
  }

  private static void publishExpiry(
      MinecraftServer server, List<RedPacketService.ExpireOutcome> outcomes) {
    for (RedPacketService.ExpireOutcome outcome : outcomes) {
      RedPacket packet = outcome.packet();
      if (outcome.result() == RedPacketService.ExpireResult.REFUNDED
          || outcome.result() == RedPacketService.ExpireResult.NO_REFUND) {
        ServerPlayer sender = server.getPlayerList().getPlayer(packet.senderId());
        if (sender != null && outcome.refundedAmount() > 0) {
          sender.sendSystemMessage(
              Component.translatable(RedPacketFeedback.EXPIRED_REFUNDED, outcome.refundedAmount()));
        }
        server.getPlayerList().broadcastSystemMessage(
            Component.translatable(RedPacketFeedback.EXPIRED_BROADCAST, packet.senderName()), false);
      } else {
        LOGGER.error(
            "Unable to refund red packet sender={} result={} remaining={}",
            packet.senderId(), outcome.result(), packet.remainingAmount());
        ServerPlayer sender = server.getPlayerList().getPlayer(packet.senderId());
        if (sender != null) {
          sender.sendSystemMessage(Component.translatable(failureKey(outcome.result())));
        }
      }
    }
  }

  private static String failureKey(RedPacketService.ExpireResult result) {
    return switch (result) {
      case BALANCE_LIMIT -> RedPacketFeedback.BALANCE_LIMIT;
      case PERSIST_FAILED -> RedPacketFeedback.TRANSACTION_FAILED;
      case STATE_UNKNOWN -> RedPacketFeedback.STATE_UNKNOWN;
      case REFUNDED, NO_REFUND -> throw new IllegalArgumentException("not a failure");
    };
  }
}
