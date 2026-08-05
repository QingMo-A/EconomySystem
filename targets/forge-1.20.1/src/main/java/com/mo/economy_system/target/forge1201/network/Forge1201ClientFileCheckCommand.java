package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.ClientFileCheckManifestAuthorizationStore;
import com.mo.economy_system.common.transfer.CheckedFileTransferStore;
import com.mo.economy_system.common.transfer.CheckedFileTransferValidation;
import com.mojang.brigadier.arguments.StringArgumentType;
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
    var getTarget=Commands.argument("playerName",EntityArgument.player());
    for(ClientFileCheckType type:ClientFileCheckType.values()) getTarget.then(Commands.literal(type.id()).then(Commands.argument("fileName",StringArgumentType.string()).executes(c->get(c.getSource(),EntityArgument.getPlayer(c,"playerName"),type,StringArgumentType.getString(c,"fileName")))));
    event.getDispatcher().register(Commands.literal("get").requires(s->s.hasPermission(2)).then(getTarget));
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
      store.rollbackCreated(pending, tick);
      source.sendFailure(Component.translatable("message.check.send_failed"));
      return 0;
    }
    source.sendSuccess(
        () -> Component.translatable("message.check.sent", pending.targetPlayerName()), false);
    return 1;
  }
  private static int get(net.minecraft.commands.CommandSourceStack source,ServerPlayer target,ClientFileCheckType type,String raw){if(!(source.getEntity() instanceof ServerPlayer requester)){source.sendFailure(Component.translatable("message.check.player_only"));return 0;}String file;try{file=CheckedFileTransferValidation.fileName(raw);}catch(RuntimeException invalid){source.sendFailure(Component.translatable("message.transfer.invalid_file"));return 0;}long tick=source.getServer().overworld().getGameTime();var stores=Forge1201ClientFileCheckRuntime.transfers(source.getServer());var auth=stores.authorizations().find(new ClientFileCheckManifestAuthorizationStore.Key(target.getUUID(),requester.getUUID(),type,file),tick);if(auth.isEmpty()){source.sendFailure(Component.translatable("message.transfer.run_check_first"));return 0;}var a=auth.get();var key=new CheckedFileTransferStore.Key(target.getUUID(),requester.getUUID(),type,file);var pending=new CheckedFileTransferStore.Pending(key,target.getGameProfile().getName(),requester.getGameProfile().getName(),a.expectedByteLength(),a.expectedSha256(),tick,tick+EconomyNetworkLimits.FILE_TRANSFER_TTL_TICKS);if(stores.transfers().create(pending,tick)!=CheckedFileTransferStore.Result.CREATED){source.sendFailure(Component.translatable("message.transfer.busy"));return 0;}try{Forge1201NetworkChannel.sendToPlayer(target,new CheckedFileTransferRequestMessage(pending.targetName(),target.getUUID(),pending.requesterName(),requester.getUUID(),type,file));}catch(RuntimeException failure){stores.transfers().rollback(pending,tick);source.sendFailure(Component.translatable("message.transfer.send_failed"));return 0;}source.sendSuccess(()->Component.translatable("message.transfer.sent",target.getGameProfile().getName(),file),false);return 1;}
}
