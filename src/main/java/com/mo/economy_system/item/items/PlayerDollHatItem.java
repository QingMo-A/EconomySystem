package com.mo.economy_system.item.items;

import com.mo.economy_system.client.render.PlayerDollHatItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PlayerDollHatItem extends ArmorItem {
    public static final String SKIN_NAME_KEY = "player_doll_skin_name";
    public static final String SKIN_TEXTURE_KEY = "player_doll_skin_texture";
    public static final String SKIN_SLIM_KEY = "player_doll_skin_slim";

    private final String defaultSkinPlayerName;
    private final ResourceLocation defaultSkinTexture;
    private final boolean defaultSlimModel;

    public PlayerDollHatItem(ArmorMaterial material, Type type, Item.Properties properties, String defaultSkinPlayerName, ResourceLocation defaultSkinTexture, boolean defaultSlimModel) {
        super(Holder.direct(material), type, properties);
        this.defaultSkinPlayerName = defaultSkinPlayerName;
        this.defaultSkinTexture = defaultSkinTexture;
        this.defaultSlimModel = defaultSlimModel;
    }

    public String getSkinPlayerName(ItemStack stack) {
        return getStoredString(stack, SKIN_NAME_KEY).orElse(defaultSkinPlayerName);
    }

    public ResourceLocation getSkinTexture(ItemStack stack) {
        return getStoredString(stack, SKIN_TEXTURE_KEY)
                .map(texture -> {
                    try {
                        return ResourceLocation.parse(texture);
                    } catch (Exception ignored) {
                        return defaultSkinTexture;
                    }
                })
                .orElse(defaultSkinTexture);
    }

    public boolean isSlimModel(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.isEmpty()) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(SKIN_SLIM_KEY)) {
                return tag.getBoolean(SKIN_SLIM_KEY);
            }
        }
        return defaultSlimModel;
    }

    public static void setSkin(ItemStack stack, String skinPlayerName, ResourceLocation skinTexture, boolean slimModel) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (skinPlayerName != null && !skinPlayerName.isBlank()) {
                tag.putString(SKIN_NAME_KEY, skinPlayerName);
            }
            tag.putString(SKIN_TEXTURE_KEY, skinTexture.toString());
            tag.putBoolean(SKIN_SLIM_KEY, slimModel);
        });
    }

    private static Optional<String> getStoredString(ItemStack stack, String key) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag tag = customData.copyTag();
        return tag.contains(key) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    public String getDefaultSkinPlayerName() {
        return defaultSkinPlayerName;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("玩偶玩家: " + getSkinPlayerName(stack)).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("皮肤材质: " + getSkinTexture(stack)).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(net.minecraft.world.entity.LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, net.minecraft.client.model.HumanoidModel<?> original) {
                original.setAllVisible(false);
                return original;
            }

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return PlayerDollHatItemRenderer.getInstance();
            }
        });
    }
}
