package com.mo.economy_system.commands.tpa_system;

import com.mojang.brigadier.CommandDispatcher;
import com.mo.economy_system.common.tpa.TpaFeedback;
import com.mo.economy_system.common.tpa.TpaRequest;
import com.mo.economy_system.common.tpa.TpaService;
import com.mo.economy_system.target.neoforge1211.tpa.NeoForge1211TpaRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** NeoForge Brigadier and chat adapter for the common TPA state machine. */
public final class Command_Tpa {
  private Command_Tpa() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("tpa")
            .then(
                Commands.argument("target", EntityArgument.player())
                    .executes(
                        context ->
                            send(
                                context.getSource(),
                                context.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(context, "target")))));
    dispatcher.register(
        Commands.literal("tpaccept")
            .executes(context -> accept(context.getSource(), context.getSource().getPlayerOrException())));
    dispatcher.register(
        Commands.literal("tpdeny")
            .executes(context -> deny(context.getSource(), context.getSource().getPlayerOrException())));
  }

  public static void expire(MinecraftServer server) {
    for (TpaRequest request : NeoForge1211TpaRuntime.expire(server)) {
      ServerPlayer sender = server.getPlayerList().getPlayer(request.senderId());
      ServerPlayer target = server.getPlayerList().getPlayer(request.targetId());
      if (target != null) {
        target.sendSystemMessage(
            Component.translatable(
                TpaFeedback.TIMEOUT_TARGET, sender == null ? "unknown" : sender.getName().getString()));
      }
      if (sender != null) {
        sender.sendSystemMessage(
            Component.translatable(
                TpaFeedback.TIMEOUT_SENDER, target == null ? "unknown" : target.getName().getString()));
      }
    }
  }

  private static int send(CommandSourceStack source, ServerPlayer sender, ServerPlayer target) {
    MinecraftServer server = sender.getServer();
    if (server == null) return 0;
    TpaService.SendResult result =
        NeoForge1211TpaRuntime.service(server)
            .send(sender.getUUID(), target.getUUID(), server.getTickCount());
    switch (result) {
      case SUCCESS -> {
        sender.sendSystemMessage(
            Component.translatable(TpaFeedback.REQUEST_SENT, target.getName().getString()));
        target.sendSystemMessage(requestMessage(sender.getName().getString()));
        return 1;
      }
      case SELF -> source.sendFailure(Component.translatable(TpaFeedback.SELF));
      case NO_POTION -> source.sendFailure(Component.translatable(TpaFeedback.NO_POTION));
      case TARGET_BUSY ->
          source.sendFailure(
              Component.translatable(TpaFeedback.TARGET_BUSY, target.getName().getString()));
      case SENDER_BUSY -> source.sendFailure(Component.translatable(TpaFeedback.SENDER_BUSY));
      case CAPACITY -> source.sendFailure(Component.translatable(TpaFeedback.CAPACITY));
      case STATE_UNKNOWN -> source.sendFailure(Component.translatable(TpaFeedback.STATE_UNKNOWN));
    }
    return 0;
  }

  private static int accept(CommandSourceStack source, ServerPlayer target) {
    MinecraftServer server = target.getServer();
    if (server == null) return 0;
    TpaService.AcceptOutcome outcome =
        NeoForge1211TpaRuntime.service(server).accept(target.getUUID(), server.getTickCount());
    TpaRequest request = outcome.request();
    ServerPlayer sender = request == null ? null : server.getPlayerList().getPlayer(request.senderId());
    switch (outcome.result()) {
      case SUCCESS -> {
        if (sender != null) {
          sender.sendSystemMessage(
              Component.translatable(TpaFeedback.TELEPORTED, target.getName().getString()));
          target.sendSystemMessage(
              Component.translatable(TpaFeedback.ACCEPTED, sender.getName().getString()));
        }
        return 1;
      }
      case NO_REQUEST -> source.sendFailure(Component.translatable(TpaFeedback.NO_REQUEST));
      case SENDER_OFFLINE -> source.sendFailure(Component.translatable(TpaFeedback.SENDER_OFFLINE));
      case SENDER_NO_POTION ->
          source.sendFailure(
              Component.translatable(
                  TpaFeedback.SENDER_NO_POTION,
                  sender == null ? "unknown" : sender.getName().getString()));
      case INVENTORY_FAILED -> notifyFailure(source, sender, TpaFeedback.INVENTORY_FAILED);
      case TELEPORT_FAILED -> notifyFailure(source, sender, TpaFeedback.TELEPORT_FAILED);
      case TELEPORT_STATE_UNKNOWN -> notifyFailure(source, sender, TpaFeedback.STATE_UNKNOWN);
      case ROLLBACK_FAILED -> notifyFailure(source, sender, TpaFeedback.ROLLBACK_FAILED);
    }
    return 0;
  }

  private static int deny(CommandSourceStack source, ServerPlayer target) {
    MinecraftServer server = target.getServer();
    if (server == null) return 0;
    TpaService.DenyOutcome outcome =
        NeoForge1211TpaRuntime.service(server).deny(target.getUUID(), server.getTickCount());
    if (outcome.result() == TpaService.DenyResult.NO_REQUEST) {
      source.sendFailure(Component.translatable(TpaFeedback.NO_REQUEST));
      return 0;
    }
    ServerPlayer sender = server.getPlayerList().getPlayer(outcome.request().senderId());
    if (sender != null) {
      sender.sendSystemMessage(
          Component.translatable(TpaFeedback.DENIED_SENDER, target.getName().getString()));
    }
    target.sendSystemMessage(Component.translatable(TpaFeedback.DENIED_TARGET));
    return 1;
  }

  private static Component requestMessage(String senderName) {
    Component accept =
        Component.translatable(TpaFeedback.ACCEPT_BUTTON)
            .withStyle(
                style ->
                    style
                        .withColor(0x55FF55)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                        .withHoverEvent(
                            new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable(TpaFeedback.ACCEPT))));
    Component deny =
        Component.translatable(TpaFeedback.DENY_BUTTON)
            .withStyle(
                style ->
                    style
                        .withColor(0xFF5555)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"))
                        .withHoverEvent(
                            new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable(TpaFeedback.DENY))));
    return Component.translatable(TpaFeedback.REQUEST_RECEIVED, senderName)
        .append(" ")
        .append(accept)
        .append(" ")
        .append(deny);
  }

  private static void notifyFailure(
      CommandSourceStack source, ServerPlayer sender, String translationKey) {
    source.sendFailure(Component.translatable(translationKey));
    if (sender != null) sender.sendSystemMessage(Component.translatable(translationKey));
  }
}
