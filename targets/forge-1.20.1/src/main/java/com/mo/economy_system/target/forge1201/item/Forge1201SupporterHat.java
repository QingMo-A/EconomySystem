package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.common.cosmetic.CosmeticProfilePolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Forge 1.20.1 adapter for the supporter cosmetic item. */
public final class Forge1201SupporterHat extends ArmorItem {
  public Forge1201SupporterHat(ArmorMaterial material, Type type, Item.Properties properties) {
    super(material, type, properties);
  }

  public static Optional<UUID> getSupporterUuid(ItemStack stack) {
    CompoundTag tag = stack.getTag();
    if (tag == null) {
      return Optional.empty();
    }
    return CosmeticProfilePolicy.parseUuid(tag.getString(CosmeticProfilePolicy.SUPPORTER_UUID_KEY));
  }

  public static void setSupporter(ItemStack stack, UUID supporterUuid, String supporterName) {
    if (!CosmeticProfilePolicy.canBindSupporter(supporterUuid, supporterName)) {
      throw new IllegalArgumentException("supporter identity is incomplete");
    }
    CompoundTag tag = stack.getOrCreateTag();
    tag.putString(CosmeticProfilePolicy.SUPPORTER_UUID_KEY, supporterUuid.toString());
    tag.putString(CosmeticProfilePolicy.SUPPORTER_NAME_KEY,
        CosmeticProfilePolicy.nameOrFallback(supporterName, supporterUuid.toString()));
  }

  @Override
  public Component getName(ItemStack stack) {
    return Component.translatable("item.economy_system.supporter_hat");
  }

  @Override
  public void appendHoverText(
      ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
    CompoundTag tag = stack.getTag();
    if (tag != null && tag.contains(CosmeticProfilePolicy.SUPPORTER_UUID_KEY)) {
      String name = tag.getString(CosmeticProfilePolicy.SUPPORTER_NAME_KEY);
      String uuid = tag.getString(CosmeticProfilePolicy.SUPPORTER_UUID_KEY);
      tooltip.add(Component.literal("Supporter: " +
          CosmeticProfilePolicy.nameOrFallback(name, uuid)).withStyle(ChatFormatting.GOLD));
    } else {
      tooltip.add(Component.translatable("tooltip.economy_system.supporter_hat.unbound")
          .withStyle(ChatFormatting.GRAY));
    }
    super.appendHoverText(stack, level, tooltip, flag);
  }

  /** Kept as a target hook for parity with the NeoForge renderer lifecycle. */
  public static void checkAndEnableRender(Player player) {
    // Vanilla Forge renders the registered armor model; no shared state is needed.
  }

  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new IClientItemExtensions() {
      @Override
      public HumanoidModel<?> getHumanoidArmorModel(
          net.minecraft.world.entity.LivingEntity entity,
          ItemStack stack,
          net.minecraft.world.entity.EquipmentSlot slot,
          HumanoidModel<?> original) {
        original.setAllVisible(false);
        return original;
      }
    });
  }
}
