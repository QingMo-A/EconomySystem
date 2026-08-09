package com.mo.economy_system.item.items;

import com.mo.economy_system.client.render.PlayerDollHatItemRenderer;
import com.mo.economy_system.utils.ItemStackDataHelper;
import com.mo.economy_system.common.cosmetic.CosmeticProfilePolicy;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerDollHatItem extends ArmorItem {
    public static final String SKIN_UUID_KEY = CosmeticProfilePolicy.SKIN_UUID_KEY;
    public static final String SKIN_NAME_KEY = CosmeticProfilePolicy.SKIN_NAME_KEY;
    public static final String SKIN_SLIM_KEY = CosmeticProfilePolicy.SKIN_SLIM_KEY;

    private final UUID defaultSkinPlayerUuid;
    private final String defaultSkinPlayerName;
    private final boolean defaultSlimModel;

    public PlayerDollHatItem(ArmorMaterial material, Type type, Item.Properties properties, UUID defaultSkinPlayerUuid, String defaultSkinPlayerName, boolean defaultSlimModel) {
        super(Holder.direct(material), type, properties);
        this.defaultSkinPlayerUuid = defaultSkinPlayerUuid;
        this.defaultSkinPlayerName = defaultSkinPlayerName;
        this.defaultSlimModel = defaultSlimModel;
    }

    public UUID getSkinPlayerUuid(ItemStack stack) {
        return getStoredString(stack, SKIN_UUID_KEY)
                .flatMap(uuid -> {
                    return CosmeticProfilePolicy.parseUuid(uuid);
                })
                .orElse(defaultSkinPlayerUuid);
    }

    public String getSkinPlayerName(ItemStack stack) {
        return getStoredString(stack, SKIN_NAME_KEY).orElse(defaultSkinPlayerName);
    }

    public boolean isSlimModel(ItemStack stack) {
        CompoundTag tag = ItemStackDataHelper.getTag(stack);
        if (tag != null && !tag.isEmpty()) {
            if (tag.contains(SKIN_SLIM_KEY)) {
                return tag.getBoolean(SKIN_SLIM_KEY);
            }
        }
        return defaultSlimModel;
    }

    public static void setSkin(ItemStack stack, UUID skinPlayerUuid, String skinPlayerName, boolean slimModel) {
        if (skinPlayerUuid == null) {
            throw new IllegalArgumentException("skin player UUID is required");
        }
        CompoundTag tag = ItemStackDataHelper.getTag(stack);
        if (tag == null) {
            tag = new CompoundTag();
        }
        tag.putString(SKIN_UUID_KEY, skinPlayerUuid.toString());
        tag.putString(SKIN_NAME_KEY,
                CosmeticProfilePolicy.nameOrFallback(skinPlayerName, skinPlayerUuid.toString()));
        tag.putBoolean(SKIN_SLIM_KEY, slimModel);
        ItemStackDataHelper.setTag(stack, tag);
    }

    private static Optional<String> getStoredString(ItemStack stack, String key) {
        CompoundTag tag = ItemStackDataHelper.getTag(stack);
        if (tag == null || tag.isEmpty()) {
            return Optional.empty();
        }
        return tag.contains(key) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    public String getDefaultSkinPlayerName() {
        return defaultSkinPlayerName;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(getSkinPlayerName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("玩偶玩家: " + getSkinPlayerName(stack)).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("玩家UUID: " + getSkinPlayerUuid(stack)).withStyle(ChatFormatting.GRAY));
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
