package com.mo.economy_system.target.forge1201.reward;

import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.reward.RewardEnchantmentIds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Forge 1.20.1 registrations matching the data-driven 1.21.1 enchantments. */
public final class Forge1201RewardEnchantments {
  private static final DeferredRegister<Enchantment> ENCHANTMENTS =
      DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, EconomyConstants.MOD_ID);

  public static final RegistryObject<Enchantment> CAREFULLY =
      ENCHANTMENTS.register(RewardEnchantmentIds.CAREFULLY, RewardEnchantment::new);
  public static final RegistryObject<Enchantment> BOUNTY_HUNTER =
      ENCHANTMENTS.register(RewardEnchantmentIds.BOUNTY_HUNTER, RewardEnchantment::new);

  private Forge1201RewardEnchantments() {}

  public static void register(IEventBus bus) {
    ENCHANTMENTS.register(bus);
  }

  private static final class RewardEnchantment extends Enchantment {
    private RewardEnchantment() {
      super(Rarity.UNCOMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[] {EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinCost(int level) {
      return 10 + Math.max(0, level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
      return 35 + Math.max(0, level - 1) * 10;
    }

    @Override
    public int getMaxLevel() {
      return RewardEnchantmentIds.MAX_LEVEL;
    }
  }
}
