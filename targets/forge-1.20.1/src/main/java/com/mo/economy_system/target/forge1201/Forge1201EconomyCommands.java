package com.mo.economy_system.target.forge1201;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.settings.EconomySettings;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.target.forge1201.item.Forge1201SupporterHat;
import com.mo.economy_system.target.forge1201.recycle.Forge1201RecyclerCommands;
import com.mo.economy_system.target.forge1201.commission.Forge1201CommissionRuntime;
import com.mo.economy_system.platform.EconomyServices;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Map;
import java.util.Collection;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge command adapter for the baseline economy and settings commands. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201EconomyCommands {
  private Forge1201EconomyCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    Forge1201RecyclerCommands.register(event.getDispatcher());
    event.getDispatcher().register(
        Commands.literal("economy_system")
            .then(Commands.literal("shop")
                .then(Commands.literal("addhand").requires(s -> s.hasPermission(2))
                    .then(Commands.argument("basePrice", IntegerArgumentType.integer(1))
                        .then(Commands.argument("description", StringArgumentType.greedyString())
                            .executes(c -> addHand(
                                c.getSource(),
                                c.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(c, "basePrice"),
                                StringArgumentType.getString(c, "description"))))))));
    event.getDispatcher().register(
        Commands.literal("economy_system")
            .then(Commands.literal("supporter_hat")
                .then(Commands.literal("bind").requires(s -> s.hasPermission(2))
                    .then(Commands.argument("supporter", EntityArgument.player())
                        .executes(c -> bindSupporterHat(
                            c.getSource(), c.getSource().getPlayerOrException(),
                            EntityArgument.getPlayer(c, "supporter").getUUID(),
                            EntityArgument.getPlayer(c, "supporter").getGameProfile().getName())))
                    .then(Commands.argument("uuid", UuidArgument.uuid())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                            .executes(c -> bindSupporterHat(
                                c.getSource(), c.getSource().getPlayerOrException(),
                                UuidArgument.getUuid(c, "uuid"),
                                StringArgumentType.getString(c, "name"))))))));
    var coin = Commands.literal("coin");
    coin.then(Commands.literal("balance")
        .executes(c -> balance(c.getSource().getPlayerOrException())));

    var addTarget = Commands.argument("target", EntityArgument.player())
        .executes(c -> add(EntityArgument.getPlayer(c, "target"),
            IntegerArgumentType.getInteger(c, "amount"), c.getSource()));
    coin.then(Commands.literal("add").requires(s -> s.hasPermission(2))
        .then(Commands.argument("amount", IntegerArgumentType.integer(1)).then(addTarget)));

    var minTarget = Commands.argument("target", EntityArgument.player())
        .executes(c -> min(EntityArgument.getPlayer(c, "target"),
            IntegerArgumentType.getInteger(c, "amount"), c.getSource()));
    coin.then(Commands.literal("min").requires(s -> s.hasPermission(2))
        .then(Commands.argument("amount", IntegerArgumentType.integer(1)).then(minTarget)));

    var setTarget = Commands.argument("target", EntityArgument.players())
        .executes(c -> setMany(EntityArgument.getPlayers(c, "target"),
            IntegerArgumentType.getInteger(c, "amount"), c.getSource()));
    coin.then(Commands.literal("set").requires(s -> s.hasPermission(2))
        .then(Commands.argument("amount", IntegerArgumentType.integer(0)).then(setTarget)));

    coin.then(Commands.literal("setall").requires(s -> s.hasPermission(2))
        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
            .executes(c -> setMany(c.getSource().getServer().getPlayerList().getPlayers(),
                IntegerArgumentType.getInteger(c, "amount"), c.getSource()))));

    var transferAmount = Commands.argument("amount", IntegerArgumentType.integer(1))
        .executes(c -> {
          ServerPlayer sender = c.getSource().getPlayerOrException();
          ServerPlayer target = EntityArgument.getPlayer(c, "target");
          return Forge1201TransferAdapter.execute(sender,
              new TransferMessage(target.getUUID(),
                  IntegerArgumentType.getInteger(c, "amount")))
              == com.mo.economy_system.core.economy_system.BalanceTransferResult.SUCCESS
              ? 1 : 0;
        });
    coin.then(Commands.literal("transfer")
        .then(Commands.argument("target", EntityArgument.player()).then(transferAmount)));
    event.getDispatcher().register(coin);

    var commission = Commands.literal("commission");
    commission.then(Commands.literal("refresh")
        .executes(c -> Forge1201CommissionRuntime.refreshCommand(c.getSource()))
        .then(Commands.literal("player").requires(s -> s.hasPermission(2))
            .then(Commands.argument("target", EntityArgument.player()).executes(c -> {
              ServerPlayer target = EntityArgument.getPlayer(c, "target");
              var view = Forge1201CommissionRuntime.forceRefresh(target);
              c.getSource().sendSuccess(() -> Component.literal("已刷新 " + target.getName().getString()
                  + "，新增 " + view.generation().added().size() + " 条。"), true);
              return 1;
            }))));
    commission.then(Commands.literal("reload").requires(s -> s.hasPermission(2))
        .executes(c -> Forge1201CommissionRuntime.reloadCommand(c.getSource())));
    commission.then(Commands.literal("template")
        .then(Commands.literal("list").requires(s -> s.hasPermission(2)).executes(c -> {
          for (String id : Forge1201CommissionRuntime.templateIds()) {
            c.getSource().sendSuccess(() -> Component.literal(id), false);
          }
          return Forge1201CommissionRuntime.templateIds().size();
        })));
    commission.then(Commands.literal("list")
        .executes(c -> Forge1201CommissionRuntime.listCommand(c.getSource())));
    commission.then(Commands.literal("submit")
        .then(Commands.argument("commissionId", UuidArgument.uuid())
            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(c -> submitCommission(c.getSource(),
                    UuidArgument.getUuid(c, "commissionId"),
                    IntegerArgumentType.getInteger(c, "amount"))))));
    commission.then(Commands.literal("public")
        .then(Commands.literal("list").executes(c -> listPublic(c.getSource())))
        .then(Commands.literal("info")
            .then(Commands.argument("id", UuidArgument.uuid())
                .executes(c -> infoPublic(c.getSource(), UuidArgument.getUuid(c, "id")))))
        .then(Commands.literal("create").requires(s -> s.hasPermission(2))
            .then(Commands.argument("name", StringArgumentType.string())
                .then(Commands.argument("requesterId", StringArgumentType.word())
                    .then(Commands.argument("requesterName", StringArgumentType.string())
                        .then(Commands.argument("target", StringArgumentType.word())
                            .then(Commands.argument("targetAmount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("unitReward", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("durationSeconds", LongArgumentType.longArg(1))
                                        .then(Commands.argument("description", StringArgumentType.greedyString())
                                            .executes(c -> createPublic(c)))))))))))
        .then(Commands.literal("end").requires(s -> s.hasPermission(2))
            .then(Commands.argument("id", UuidArgument.uuid())
                .executes(c -> endPublic(c.getSource(), UuidArgument.getUuid(c, "id")))))
        .then(Commands.literal("remove").requires(s -> s.hasPermission(2))
            .then(Commands.argument("id", UuidArgument.uuid())
                .executes(c -> removePublic(c.getSource(), UuidArgument.getUuid(c, "id")))))
        .then(Commands.literal("submit")
            .then(Commands.argument("id", UuidArgument.uuid())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(c -> submitPublic(c.getSource(), UuidArgument.getUuid(c, "id"),
                        IntegerArgumentType.getInteger(c, "amount")))))));
    event.getDispatcher().register(Commands.literal("economy_system").then(commission));

    event.getDispatcher().register(
        Commands.literal("economy_system")
            .then(Commands.literal("settings").requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(c -> listSettings(c.getSource())))
                .then(Commands.literal("get")
                    .then(Commands.argument("key", StringArgumentType.word())
                        .executes(c -> getSetting(c.getSource(),
                            StringArgumentType.getString(c, "key")))))
                .then(Commands.literal("set")
                    .then(Commands.argument("key", StringArgumentType.word())
                        .then(Commands.argument("value", StringArgumentType.word())
                            .executes(c -> setSetting(c.getSource(),
                                StringArgumentType.getString(c, "key"),
                                StringArgumentType.getString(c, "value"))))))
                .then(Commands.literal("reload").executes(c -> {
                  EconomySettings.reload();
                  c.getSource().sendSuccess(() -> Component.literal("已重载 EconomySystem 设置"), false);
                  return 1;
                }))));
  }

  private static int submitCommission(
      net.minecraft.commands.CommandSourceStack source, java.util.UUID commissionId, int amount) {
    ServerPlayer player;
    try {
      player = source.getPlayerOrException();
    } catch (Exception failure) {
      source.sendFailure(Component.literal("个人委托命令只能由玩家执行。"));
      return 0;
    }
    Forge1201CommissionRuntime.SubmitFeedback feedback =
        Forge1201CommissionRuntime.submitItem(player, commissionId, amount);
    if (feedback.accepted()) {
      source.sendSuccess(() -> Component.literal(feedback.message()), false);
      return 1;
    }
    source.sendFailure(Component.literal(feedback.message()));
    return 0;
  }

  private static int createPublic(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> c) {
    try {
      long now = Math.max(1L, System.currentTimeMillis());
      long duration = LongArgumentType.getLong(c, "durationSeconds");
      long expires = Math.addExact(now, Math.multiplyExact(duration, 1000L));
      PublicCommission value = Forge1201CommissionRuntime.createPublic(c.getSource().getServer(),
          StringArgumentType.getString(c, "name"), StringArgumentType.getString(c, "requesterId"),
          StringArgumentType.getString(c, "requesterName"), StringArgumentType.getString(c, "target"),
          IntegerArgumentType.getInteger(c, "targetAmount"), IntegerArgumentType.getInteger(c, "unitReward"),
          expires, StringArgumentType.getString(c, "description"));
      c.getSource().sendSuccess(() -> Component.literal("已创建公共委托 " + value.commissionId()
          + "，总预算 " + value.remainingBudget()), true);
      return 1;
    } catch (RuntimeException failure) {
      c.getSource().sendFailure(Component.literal("创建公共委托失败：" + failure.getMessage())); return 0;
    }
  }

  private static int listPublic(net.minecraft.commands.CommandSourceStack source) {
    var entries = Forge1201CommissionRuntime.listPublic(source.getServer());
    if (entries.isEmpty()) { source.sendSuccess(() -> Component.literal("当前暂无公共大型委托。"), false); return 1; }
    for (PublicCommission value : entries) source.sendSuccess(() -> Component.literal(formatPublic(value)), false);
    return entries.size();
  }

  private static int infoPublic(net.minecraft.commands.CommandSourceStack source, java.util.UUID id) {
    var entry = Forge1201CommissionRuntime.findPublic(source.getServer(), id);
    if (entry.isEmpty()) { source.sendFailure(Component.literal("找不到公共委托。")); return 0; }
    source.sendSuccess(() -> Component.literal(formatPublic(entry.get())), false); return 1;
  }

  private static int endPublic(net.minecraft.commands.CommandSourceStack source, java.util.UUID id) {
    if (!Forge1201CommissionRuntime.cancelPublic(source.getServer(), id)) { source.sendFailure(Component.literal("找不到公共委托。")); return 0; }
    source.sendSuccess(() -> Component.literal("公共委托已结束。"), true); return 1;
  }

  private static int removePublic(net.minecraft.commands.CommandSourceStack source, java.util.UUID id) {
    if (Forge1201CommissionRuntime.findPublic(source.getServer(), id).isEmpty()) { source.sendFailure(Component.literal("找不到公共委托。")); return 0; }
    Forge1201CommissionRuntime.removePublic(source.getServer(), id);
    source.sendSuccess(() -> Component.literal("公共委托已删除。"), true); return 1;
  }

  private static int submitPublic(net.minecraft.commands.CommandSourceStack source, java.util.UUID id, int amount) {
    try {
      ServerPlayer player = source.getPlayerOrException();
      var result = Forge1201CommissionRuntime.submitPublicItem(player, id, amount);
      source.sendSuccess(() -> Component.literal("公共委托提交：" + result.outcome()
          + "，接受 " + result.acceptedAmount() + "，奖励 " + result.payout() + "（邮件领取）"), false);
      return result.acceptedAmount() > 0 ? 1 : 0;
    } catch (Exception failure) {
      source.sendFailure(Component.literal("提交公共委托失败：" + failure.getMessage())); return 0;
    }
  }

  private static String formatPublic(PublicCommission value) {
    return value.commissionId() + " | " + value.name() + " | " + value.requesterName() + " | "
        + value.targetSnapshot() + " | " + (value.targetAmount() - value.remainingAmount()) + "/"
        + value.targetAmount() + " | 单价 " + value.unitReward() + " | 剩余预算 "
        + value.remainingBudget() + " | " + value.status();
  }

  private static int addHand(
      net.minecraft.commands.CommandSourceStack source,
      ServerPlayer player,
      int basePrice,
      String description) {
    ItemStack held = player.getMainHandItem();
    if (held.isEmpty()) {
      source.sendFailure(Component.literal("Please hold the item to add in your main hand."));
      return 0;
    }
    try {
      var added = Forge1201Platform.nativeShopCatalog().addItemFromStack(
          held, basePrice, description, player.serverLevel().registryAccess());
      source.sendSuccess(() -> Component.literal(
          "Added " + held.getHoverName().getString() + " to the shop ("
              + added.basePrice() + ")"), false);
      return 1;
    } catch (RuntimeException failure) {
      source.sendFailure(Component.literal("Shop catalog could not be updated."));
      return 0;
    }
  }

  private static int bindSupporterHat(
      net.minecraft.commands.CommandSourceStack source,
      ServerPlayer executor,
      java.util.UUID supporterUuid,
      String supporterName) {
    ItemStack stack = executor.getMainHandItem();
    if (!(stack.getItem() instanceof Forge1201SupporterHat)) {
      source.sendFailure(Component.translatable("message.supporter_hat.hold_hat"));
      return 0;
    }
    try {
      Forge1201SupporterHat.setSupporter(stack, supporterUuid, supporterName);
    } catch (IllegalArgumentException failure) {
      source.sendFailure(Component.translatable("message.supporter_hat.invalid_identity"));
      return 0;
    }
    source.sendSuccess(() -> Component.translatable(
        "message.supporter_hat.bound", supporterName, supporterUuid), false);
    return 1;
  }

  private static int balance(ServerPlayer player) {
    int value = EconomySavedData.getInstance(player.serverLevel()).getBalance(player.getUUID());
    player.sendSystemMessage(Component.translatable("message.coin_command_balance", value));
    return 1;
  }

  private static int add(ServerPlayer player, int amount, net.minecraft.commands.CommandSourceStack source) {
    EconomySavedData.getInstance(player.serverLevel()).addBalance(
        player.getUUID(), amount, "指令", "管理员增加余额");
    source.sendSuccess(() -> Component.translatable("message.coin_command_add", amount), false);
    return 1;
  }

  private static int min(ServerPlayer player, int amount, net.minecraft.commands.CommandSourceStack source) {
    if (!EconomySavedData.getInstance(player.serverLevel()).minBalance(
        player.getUUID(), amount, "指令", "管理员减少余额")) {
      source.sendFailure(Component.translatable("message.coin_command_insufficient_balance"));
      return 0;
    }
    source.sendSuccess(() -> Component.translatable("message.coin_command_min", amount), false);
    return 1;
  }

  private static int set(ServerPlayer player, int amount, net.minecraft.commands.CommandSourceStack source) {
    EconomySavedData.getInstance(player.serverLevel()).setBalance(
        player.getUUID(), amount, "指令", "管理员设置余额");
    source.sendSuccess(() -> Component.translatable("message.coin_command_set", amount), false);
    return 1;
  }

  private static int setMany(Collection<ServerPlayer> players, int amount,
      net.minecraft.commands.CommandSourceStack source) {
    if (players == null || players.isEmpty()) {
      source.sendFailure(Component.translatable("message.coin_command_no_targets"));
      return 0;
    }
    int updated = 0;
    for (ServerPlayer player : players) {
      if (player == null) continue;
      EconomySavedData.getInstance(player.serverLevel()).setBalance(
          player.getUUID(), amount, "指令", "管理员设置余额");
      updated++;
    }
    final int count = updated;
    if (count == 1) {
      source.sendSuccess(() -> Component.translatable("message.coin_command_set", amount), false);
    } else {
      source.sendSuccess(() -> Component.translatable(
          "message.coin_command_set_many", amount, count), false);
    }
    return count;
  }

  private static int listSettings(net.minecraft.commands.CommandSourceStack source) {
    source.sendSuccess(() -> Component.literal("EconomySystem 设置项:"), false);
    for (Map.Entry<String, String> entry : EconomySettings.all().entrySet()) {
      String description = EconomySettings.description(entry.getKey());
      source.sendSuccess(() -> Component.literal(entry.getKey() + " = " + entry.getValue()
          + (description.isBlank() ? "" : " | " + description)), false);
    }
    return 1;
  }

  private static int getSetting(net.minecraft.commands.CommandSourceStack source, String key) {
    String value = EconomySettings.get(key);
    if (value == null) {
      source.sendFailure(Component.literal("未知设置项: " + key));
      return 0;
    }
    source.sendSuccess(() -> Component.literal(key + " = " + value), false);
    return 1;
  }

  private static int setSetting(net.minecraft.commands.CommandSourceStack source, String key, String value) {
    try {
      if (!EconomySettings.set(key, value)) {
        source.sendFailure(Component.literal("未知设置项: " + key));
        return 0;
      }
      source.sendSuccess(() -> Component.literal("已设置 " + key + " = " + EconomySettings.get(key)), false);
      return 1;
    } catch (IllegalArgumentException failure) {
      source.sendFailure(Component.literal("设置失败: " + failure.getMessage()));
      return 0;
    }
  }
}
