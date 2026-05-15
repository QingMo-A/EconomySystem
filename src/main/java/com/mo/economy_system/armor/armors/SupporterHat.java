package com.mo.economy_system.armor.armors;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public class SupporterHat extends ArmorItem {
    public static final String SUPPORTER_UUID_KEY = "supporter_uuid";
    public static final String SUPPORTER_NAME_KEY = "supporter_name";

    public SupporterHat(ArmorMaterial material, Type slot, Item.Properties properties) {
        super(Holder.direct(material), slot, properties);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        // 检查玩家是否穿戴了此头盔
        checkAndEnableRender(player);
    }

    // 玩家每次tick时检查头盔是否穿戴
    public static void checkAndEnableRender(Player player) {
        // 旧版这里曾经控制碰撞箱调试渲染。赞助者帽子的正式视觉效果现在由客户端渲染层负责。
        CustomHitboxRenderer.disable();
    }

    public static Optional<UUID> getSupporterUuid(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.isEmpty()) {
            return Optional.empty();
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(SUPPORTER_UUID_KEY)) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(tag.getString(SUPPORTER_UUID_KEY)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static void setSupporter(ItemStack stack, UUID supporterUuid, String supporterName) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(SUPPORTER_UUID_KEY, supporterUuid.toString());
            if (supporterName != null && !supporterName.isBlank()) {
                tag.putString(SUPPORTER_NAME_KEY, supporterName);
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        if (tag.contains(SUPPORTER_UUID_KEY)) {
            String name = tag.getString(SUPPORTER_NAME_KEY);
            tooltipComponents.add(Component.literal("赞助者: " + (name.isBlank() ? tag.getString(SUPPORTER_UUID_KEY) : name)).withStyle(ChatFormatting.GOLD));
        } else {
            tooltipComponents.add(Component.literal("未绑定赞助者 UUID，佩戴时显示佩戴者皮肤").withStyle(ChatFormatting.GRAY));
        }
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
        });
    }
}
