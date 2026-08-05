package com.mo.economy_system.commands.check_system;

import com.mo.economy_system.common.check.ClientFileCheckRequestStore;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.transfer.ClientFileCheckManifestAuthorizationStore;
import com.mo.economy_system.common.transfer.CheckedFileTransferStore;
import com.mo.economy_system.common.transfer.CheckedFileTransferValidation;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211ClientFileCheckRuntime;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Administrative client file checks; legacy /get remains protocol 26. */
public final class Command_Check {
  private Command_Check() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    var check = Commands.literal("check").requires(source -> source.hasPermission(2));
    var target = Commands.argument("playerName", EntityArgument.player());
    for (ClientFileCheckType type : ClientFileCheckType.values()) {
      target.then(
          Commands.literal(type.id())
              .executes(
                  context ->
                      checkPlayer(
                          context.getSource(),
                          EntityArgument.getPlayer(context, "playerName"),
                          type)));
    }
    dispatcher.register(check.then(target));

    var get = Commands.literal("get").requires(source -> source.hasPermission(2));
    var getTarget = Commands.argument("playerName", EntityArgument.player());
    var file = Commands.argument("fileName", StringArgumentType.string());
    for (ClientFileCheckType type : ClientFileCheckType.values()) {
      file.then(
          Commands.literal(type.id())
              .executes(
                  context ->
                      getPlayerFile(
                          context.getSource(),
                          EntityArgument.getPlayer(context, "playerName"),
                          type,
                          StringArgumentType.getString(context, "fileName"))));
    }
    dispatcher.register(get.then(getTarget.then(file)));
  }

  private static int checkPlayer(
      CommandSourceStack source, ServerPlayer player, ClientFileCheckType type) {
    if (!(source.getEntity() instanceof ServerPlayer requester)) {
      source.sendFailure(Component.translatable("message.check.player_only"));
      return 0;
    }
    long tick = source.getServer().overworld().getGameTime();
    ClientFileCheckRequestStore store =
        NeoForge1211ClientFileCheckRuntime.store(source.getServer());
    ClientFileCheckRequestStore.Pending pending =
        new ClientFileCheckRequestStore.Pending(
            player.getUUID(),
            player.getGameProfile().getName(),
            requester.getUUID(),
            requester.getGameProfile().getName(),
            type,
            tick,
            tick + EconomyNetworkLimits.CHECK_REQUEST_TTL_TICKS);
    ClientFileCheckRequestStore.PutResult result = store.put(pending, tick);
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
      EconomySystem_NetworkManager.sendToClient(
          player,
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

  private static int getPlayerFile(
      CommandSourceStack source, ServerPlayer player, ClientFileCheckType type, String rawFileName) {
    if (!(source.getEntity() instanceof ServerPlayer requester)) {
      source.sendFailure(Component.translatable("message.check.player_only"));
      return 0;
    }
    String fileName;try{fileName=CheckedFileTransferValidation.fileName(rawFileName);}catch(RuntimeException invalid){source.sendFailure(Component.translatable("message.transfer.invalid_file"));return 0;}
    long tick=source.getServer().overworld().getGameTime();var stores=NeoForge1211ClientFileCheckRuntime.transfers(source.getServer());var authorization=stores.authorizations().find(new ClientFileCheckManifestAuthorizationStore.Key(player.getUUID(),requester.getUUID(),type,fileName),tick);if(authorization.isEmpty()){source.sendFailure(Component.translatable("message.transfer.run_check_first"));return 0;}var a=authorization.get();var key=new CheckedFileTransferStore.Key(player.getUUID(),requester.getUUID(),type,fileName);var pending=new CheckedFileTransferStore.Pending(key,player.getGameProfile().getName(),requester.getGameProfile().getName(),a.expectedByteLength(),a.expectedSha256(),tick,tick+EconomyNetworkLimits.FILE_TRANSFER_TTL_TICKS);if(stores.transfers().create(pending,tick)!=CheckedFileTransferStore.Result.CREATED){source.sendFailure(Component.translatable("message.transfer.busy"));return 0;}try{EconomySystem_NetworkManager.sendToClient(player,new CheckedFileTransferRequestMessage(pending.targetName(),player.getUUID(),pending.requesterName(),requester.getUUID(),type,fileName));}catch(RuntimeException failure){stores.transfers().rollback(pending,tick);source.sendFailure(Component.translatable("message.transfer.send_failed"));return 0;}source.sendSuccess(()->Component.translatable("message.transfer.sent",player.getGameProfile().getName(),fileName),false);
    return 1;
  }
}
