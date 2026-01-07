package com.mo.economy_system.item.item_renderer;

import com.mo.economy_system.item.items.Item_Blueprint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class BlueprintItemRenderer extends BlockEntityWithoutLevelRenderer {

    public BlueprintItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext transformType,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        String itemId = Item_Blueprint.getUnlockedItemId(stack);

        if (itemId != null && !itemId.isEmpty()) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            if (item != null) {
                ItemStack targetStack = new ItemStack(item);

                Minecraft.getInstance().getItemRenderer().renderStatic(
                        targetStack,
                        transformType,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        buffer,
                        Minecraft.getInstance().level,
                        0
                );
                return;
            }
        }

        // 没 NBT 或异常 → 渲染默认蓝图
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                transformType,
                packedLight,
                packedOverlay,
                poseStack,
                buffer,
                Minecraft.getInstance().level,
                0
        );
    }
}

