package com.mo.economy_system.target.forge1201.reward;

import com.mo.economy_system.common.reward.RewardFeedback;
import com.mo.economy_system.common.reward.RewardService;
import com.mo.economy_system.target.forge1201.EconomySystemForge1201;
import com.mo.economy_system.target.forge1201.commission.Forge1201CommissionRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Forge event and registry translation for the common mob-reward service. */
@Mod.EventBusSubscriber(
    modid = EconomySystemForge1201.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Forge1201RewardEvents {
  private Forge1201RewardEvents() {}

  @SubscribeEvent
  public static void onMobDeath(LivingDeathEvent event) {
    if (!(event.getEntity() instanceof Mob mob)) return;
    if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

    ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
    if (entityId == null || player.getServer() == null) return;
    Forge1201CommissionRuntime.handleEntityKill(player, entityId.toString());
    ItemStack weapon = player.getMainHandItem();
    int carefully =
        EnchantmentHelper.getItemEnchantmentLevel(
            Forge1201RewardEnchantments.CAREFULLY.get(), weapon);
    int bountyHunter =
        EnchantmentHelper.getItemEnchantmentLevel(
            Forge1201RewardEnchantments.BOUNTY_HUNTER.get(), weapon);

    RewardService.Outcome outcome =
        Forge1201RewardRuntime.award(
            player.getServer(),
            player.getUUID(),
            entityId.toString(),
            mob.getName().getString(),
            bountyHunter,
            carefully);
    publish(player, mob, outcome);
  }

  private static void publish(ServerPlayer player, Mob mob, RewardService.Outcome outcome) {
    switch (outcome.result()) {
      case SUCCESS ->
          player.sendSystemMessage(
              Component.translatable(
                  RewardFeedback.SUCCESS, mob.getName().getString(), outcome.amount()));
      case BALANCE_LIMIT ->
          player.sendSystemMessage(Component.translatable(RewardFeedback.BALANCE_LIMIT));
      case PERSIST_FAILED ->
          player.sendSystemMessage(Component.translatable(RewardFeedback.TRANSACTION_FAILED));
      case STATE_UNKNOWN ->
          player.sendSystemMessage(Component.translatable(RewardFeedback.STATE_UNKNOWN));
      case UNCONFIGURED, NO_DROP -> {
        // No user-visible reward event occurred.
      }
    }
  }
}
