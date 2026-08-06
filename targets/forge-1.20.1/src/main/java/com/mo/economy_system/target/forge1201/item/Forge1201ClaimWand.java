package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.target.forge1201.network.Forge1201TerritoryModifySessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Forge 1.20.1 selection adapter for an active protocol-36 resize session. */
final class Forge1201ClaimWand extends Item {
  Forge1201ClaimWand(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    if (!(context.getPlayer() instanceof ServerPlayer player)) {
      return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }
    return Forge1201TerritoryModifySessions.select(player, context.getClickedPos())
        ? InteractionResult.SUCCESS
        : InteractionResult.FAIL;
  }
}
