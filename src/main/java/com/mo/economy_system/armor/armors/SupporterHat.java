package com.mo.economy_system.armor.armors;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SupporterHat extends ArmorItem {
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
        if (player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof SupporterHat) {
            // 启用 CustomHitboxRenderer 渲染
            CustomHitboxRenderer.enable();
        } else {
            // 禁用 CustomHitboxRenderer 渲染
            CustomHitboxRenderer.disable();
        }
    }
}
