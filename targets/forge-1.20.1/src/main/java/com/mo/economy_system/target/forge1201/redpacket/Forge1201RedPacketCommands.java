package com.mo.economy_system.target.forge1201.redpacket;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.redpacket.RedPacket;
import com.mo.economy_system.common.redpacket.RedPacketFeedback;
import com.mo.economy_system.common.redpacket.RedPacketService;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Forge Brigadier adapter for the common red-packet use cases. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201RedPacketCommands {
  private static final Logger LOGGER = LogUtils.getLogger();

  private Forge1201RedPacketCommands() {}

  @SubscribeEvent
  public static void onRegisterCommands(RegisterCommandsEvent event) {
    register(event.getDispatcher());
  }

  static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    var duration =
        Commands.argument("duration", IntegerArgumentType.integer(1))
            .then(
                Commands.literal("lucky")
                    .executes(
                        context ->
                            create(
                                context.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(context, "amount"),
                                IntegerArgumentType.getInteger(context, "duration"),
                                RedPacket.Mode.LUCKY)))
            .then(
                Commands.literal("even")
                    .executes(
                        context ->
                            create(
                                context.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(context, "amount"),
                                IntegerArgumentType.getInteger(context, "duration"),
                                RedPacket.Mode.EVEN)));
    var createCommand =
        Commands.literal("create")
            .then(Commands.argument("amount", IntegerArgumentType.integer(1)).then(duration));
    var claimCommand =
        Commands.literal("claim")
            .executes(context -> claim(context.getSource().getPlayerOrException(), null))
            .then(
                Commands.argument("player", EntityArgument.player())
                    .executes(
                        context ->
                            claim(
                                context.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(context, "player").getUUID())));
    var cancelCommand =
        Commands.literal("cancel")
            .executes(context -> cancel(context.getSource().getPlayerOrException()));
    dispatcher.register(
        Commands.literal("redpacket").then(createCommand).then(claimCommand).then(cancelCommand));
  }

  private static int create(
      ServerPlayer sender, int amount, int durationMinutes, RedPacket.Mode mode) {
    MinecraftServer server = Objects.requireNonNull(sender.getServer(), "server");
    try {
      RedPacketService.CreateOutcome outcome =
          Forge1201RedPacketRuntime.service(server)
              .create(
                  sender.getUUID(),
                  sender.getGameProfile().getName(),
                  amount,
                  durationMinutes,
                  mode,
                  server.getPlayerCount());
      if (outcome.result() != RedPacketService.CreateResult.SUCCESS) {
        sender.sendSystemMessage(
            Component.translatable(RedPacketFeedback.createFailureKey(outcome.result())));
        return 0;
      }

      Component claimButton =
          Component.translatable(RedPacketFeedback.CLAIM_BUTTON)
              .withStyle(
                  style ->
                      style
                          .withColor(0x55FF55)
                          .withClickEvent(
                              new ClickEvent(
                                  ClickEvent.Action.RUN_COMMAND,
                                  "/redpacket claim " + sender.getGameProfile().getName()))
                          .withHoverEvent(
                              new HoverEvent(
                                  HoverEvent.Action.SHOW_TEXT,
                                  Component.translatable(RedPacketFeedback.CLAIM_HOVER))));
      server
          .getPlayerList()
          .broadcastSystemMessage(
              Component.translatable(
                      RedPacketFeedback.BROADCAST, sender.getGameProfile().getName())
                  .append(claimButton),
              false);
      sender.sendSystemMessage(Component.translatable(RedPacketFeedback.CREATED));
      return 1;
    } catch (RuntimeException error) {
      LOGGER.error("Red-packet create adapter failed player={}", sender.getUUID(), error);
      sender.sendSystemMessage(Component.translatable(RedPacketFeedback.STATE_UNKNOWN));
      return 0;
    }
  }

  private static int claim(ServerPlayer player, UUID senderId) {
    MinecraftServer server = Objects.requireNonNull(player.getServer(), "server");
    try {
      RedPacketService.ClaimOutcome outcome =
          Forge1201RedPacketRuntime.service(server).claim(player.getUUID(), senderId);
      if (outcome.result() != RedPacketService.ClaimResult.SUCCESS) {
        player.sendSystemMessage(
            Component.translatable(RedPacketFeedback.claimFailureKey(outcome.result())));
        return 0;
      }

      RedPacket packet = outcome.packet();
      player.sendSystemMessage(
          Component.translatable(
              RedPacketFeedback.CLAIM_SUCCESS, packet.senderName(), outcome.amount()));
      Component broadcast =
          Component.translatable(
              RedPacketFeedback.CLAIM_BROADCAST,
              player.getGameProfile().getName(),
              packet.senderName(),
              outcome.amount());
      for (ServerPlayer online : server.getPlayerList().getPlayers()) {
        if (!online.getUUID().equals(player.getUUID())) online.sendSystemMessage(broadcast);
      }
      if (outcome.completed()) {
        server
            .getPlayerList()
            .broadcastSystemMessage(
                Component.translatable(RedPacketFeedback.FULLY_CLAIMED, packet.senderName()),
                false);
      }
      return 1;
    } catch (RuntimeException error) {
      LOGGER.error("Red-packet claim adapter failed player={} sender={}", player.getUUID(), senderId, error);
      player.sendSystemMessage(Component.translatable(RedPacketFeedback.STATE_UNKNOWN));
      return 0;
    }
  }

  private static int cancel(ServerPlayer sender) {
    MinecraftServer server = Objects.requireNonNull(sender.getServer(), "server");
    try {
      RedPacketService.CancelOutcome outcome =
          Forge1201RedPacketRuntime.service(server).cancel(sender.getUUID());
      if (outcome.result() != RedPacketService.CancelResult.SUCCESS) {
        sender.sendSystemMessage(
            Component.translatable(RedPacketFeedback.cancelFailureKey(outcome.result())));
        return 0;
      }
      sender.sendSystemMessage(
          Component.translatable(RedPacketFeedback.CANCELLED, outcome.refundedAmount()));
      return 1;
    } catch (RuntimeException error) {
      LOGGER.error("Red-packet cancel adapter failed player={}", sender.getUUID(), error);
      sender.sendSystemMessage(Component.translatable(RedPacketFeedback.STATE_UNKNOWN));
      return 0;
    }
  }
}
