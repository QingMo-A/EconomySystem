package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.common.cosmetic.CosmeticProfilePolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import com.mo.economy_system.target.forge1201.client.render.Forge1201PlayerDollHatItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Forge 1.20.1 adapter for a player-skin doll hat. */
public final class Forge1201PlayerDollHatItem extends ArmorItem {
  private final CosmeticProfilePolicy.DollProfile defaultProfile;

  public Forge1201PlayerDollHatItem(
      ArmorMaterial material,
      Type type,
      Item.Properties properties,
      UUID defaultSkinPlayerUuid,
      String defaultSkinPlayerName,
      boolean defaultSlimModel) {
    super(material, type, properties);
    defaultProfile = new CosmeticProfilePolicy.DollProfile(
        defaultSkinPlayerUuid, defaultSkinPlayerName, defaultSlimModel);
  }

  public UUID getSkinPlayerUuid(ItemStack stack) {
    return stored(stack, CosmeticProfilePolicy.SKIN_UUID_KEY)
        .flatMap(CosmeticProfilePolicy::parseUuid)
        .orElse(defaultProfile.playerUuid());
  }

  public String getSkinPlayerName(ItemStack stack) {
    return stored(stack, CosmeticProfilePolicy.SKIN_NAME_KEY)
        .map(value -> CosmeticProfilePolicy.nameOrFallback(value, defaultProfile.playerName()))
        .orElse(defaultProfile.playerName());
  }

  public boolean isSlimModel(ItemStack stack) {
    CompoundTag tag = stack.getTag();
    return tag != null && tag.contains(CosmeticProfilePolicy.SKIN_SLIM_KEY)
        ? tag.getBoolean(CosmeticProfilePolicy.SKIN_SLIM_KEY)
        : defaultProfile.slimModel();
  }

  public static void setSkin(
      ItemStack stack, UUID skinPlayerUuid, String skinPlayerName, boolean slimModel) {
    if (skinPlayerUuid == null) {
      throw new IllegalArgumentException("skin player UUID is required");
    }
    CompoundTag tag = stack.getOrCreateTag();
    tag.putString(CosmeticProfilePolicy.SKIN_UUID_KEY, skinPlayerUuid.toString());
    tag.putString(CosmeticProfilePolicy.SKIN_NAME_KEY,
        CosmeticProfilePolicy.nameOrFallback(skinPlayerName, skinPlayerUuid.toString()));
    tag.putBoolean(CosmeticProfilePolicy.SKIN_SLIM_KEY, slimModel);
  }

  @Override
  public Component getName(ItemStack stack) {
    return Component.literal(getSkinPlayerName(stack));
  }

  @Override
  public void appendHoverText(
      ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
    tooltip.add(Component.literal("Player doll: " + getSkinPlayerName(stack))
        .withStyle(ChatFormatting.GOLD));
    tooltip.add(Component.literal("UUID: " + getSkinPlayerUuid(stack))
        .withStyle(ChatFormatting.GRAY));
    super.appendHoverText(stack, level, tooltip, flag);
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

      @Override
      public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
        return Forge1201PlayerDollHatItemRenderer.getInstance();
      }
    });
  }

  private static Optional<String> stored(ItemStack stack, String key) {
    CompoundTag tag = stack.getTag();
    if (tag == null || !tag.contains(key)) {
      return Optional.empty();
    }
    return Optional.ofNullable(tag.getString(key));
  }
}
