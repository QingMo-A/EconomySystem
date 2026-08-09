package com.mo.economy_system.target.forge1201;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritorySnapshotStore;
import java.util.stream.Collectors;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge command adapter for the baseline /info queries. */
@Mod.EventBusSubscriber(modid = EconomyConstants.MOD_ID)
public final class Forge1201InfoCommands {
  private Forge1201InfoCommands() {}

  @SubscribeEvent
  public static void register(RegisterCommandsEvent event) {
    event.getDispatcher().register(
        Commands.literal("info")
            .then(Commands.literal("player")
                .executes(c -> showPlayer(c.getSource().getPlayerOrException(),
                    c.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(c -> showPlayer(c.getSource().getPlayerOrException(),
                        EntityArgument.getPlayer(c, "player")))))
            .then(Commands.literal("territory")
                .executes(c -> showTerritory(c.getSource().getPlayerOrException())))
            .then(Commands.literal("item")
                .executes(c -> showItem(c.getSource().getPlayerOrException()))));
  }

  private static int showPlayer(ServerPlayer requester, ServerPlayer target) {
    int balance = EconomySavedData.getInstance(target.serverLevel()).getBalance(target.getUUID());
    requester.sendSystemMessage(Component.literal(
        target.getName().getString() + " 拥有 " + balance + " 枚梦鱼币"));
    return 1;
  }

  private static int showItem(ServerPlayer player) {
    CompoundTag tag = player.getMainHandItem().getTag();
    if (tag == null || tag.isEmpty()) {
      player.sendSystemMessage(Component.literal("该物品没有 NBT 数据"));
      return 1;
    }
    player.sendSystemMessage(Component.literal("NBT: " + tag));
    return 1;
  }

  private static int showTerritory(ServerPlayer player) {
    int x = (int) Math.floor(player.getX());
    int z = (int) Math.floor(player.getZ());
    String dimension = player.serverLevel().dimension().location().toString();
    Owned territory = Forge1201TerritorySnapshotStore.get(player.serverLevel())
        .at(dimension, x, z).orElse(null);
    if (territory == null) {
      player.sendSystemMessage(Component.literal("你未处于领地中"));
      return 0;
    }
    String members = territory.authorizedMembers().isEmpty()
        ? "§7无"
        : territory.authorizedMembers().stream().map(Member::playerName)
            .collect(Collectors.joining("\n"));
    Component memberLine = Component.literal("[领地成员]").withStyle(style ->
        style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            Component.literal(members))));
    var summary = territory.summary();
    player.sendSystemMessage(Component.literal(
        "-----------------------\n"
            + "领地名称: " + summary.name() + "\n"
            + "领地UUID: " + summary.territoryId() + "\n"
            + "领地所有者: " + summary.ownerName() + "\n"
            + "领地所有者UUID: " + summary.ownerId() + "\n")
        .append(memberLine));
    return 1;
  }
}
