package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.target.forge1201.EconomySystemForge1201;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = EconomySystemForge1201.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Forge1201ClientFileCheckCommand {
  private Forge1201ClientFileCheckCommand() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    var root = Commands.literal("check").requires(source -> source.hasPermission(2));
    var target = Commands.argument("playerName", EntityArgument.player());
    for (ClientFileCheckType type : ClientFileCheckType.values()) {
      target.then(
          Commands.literal(type.id())
              .executes(
                  context ->
                      execute(
                          context.getSource(),
                          EntityArgument.getPlayer(context, "playerName"),
                          type)));
    }
    event.getDispatcher().register(root.then(target));
  }

  private static int execute(
      net.minecraft.commands.CommandSourceStack source,
      ServerPlayer target,
      ClientFileCheckType type) {
    if (!(source.getEntity() instanceof ServerPlayer requester)) {
      source.sendFailure(Component.translatable("message.check.player_only"));
      return 0;
    }
    long tick = source.getServer().overworld().getGameTime();
    ClientFileCheckRequestStore store = Forge1201ClientFileCheckRuntime.store(source.getServer());
    var pending =
        new ClientFileCheckRequestStore.Pending(
            target.getUUID(),
            target.getGameProfile().getName(),
            requester.getUUID(),
            requester.getGameProfile().getName(),
            type,
            tick,
            tick + EconomyNetworkLimits.CHECK_REQUEST_TTL_TICKS);
    var result = store.put(pending, tick);
    if (result != ClientFileCheckRequestStore.PutResult.CREATED) {
      String key =
          switch (result) {
            case ALREADY_PENDING -> "message.check.already_pending";
            case RATE_LIMITED -> "message.check.rate_limited";
            case FULL -> "message.check.store_full";
            default -> "message.check.send_failed";
          };
      source.sendFailure(Component.translatable(key));
      return 0;
    }
    try {
      Forge1201NetworkChannel.sendToPlayer(
          target,
          new ClientFileCheckRequestMessage(
              pending.targetPlayerName(),
              pending.targetPlayerId(),
              pending.requesterPlayerName(),
              pending.requesterPlayerId(),
              type));
    } catch (RuntimeException failure) {
      store.discard(pending.key(), tick);
      source.sendFailure(Component.translatable("message.check.send_failed"));
      return 0;
    }
    source.sendSuccess(
        () -> Component.translatable("message.check.sent", pending.targetPlayerName()), false);
    return 1;
  }
}
