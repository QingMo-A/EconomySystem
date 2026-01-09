package com.mo.economy_system.item.item_renderer;

import com.mo.economy_system.item.items.Item_Blueprint;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintItemRenderer.class);

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

        LOGGER.info("Blueprint rendering - itemId: {}", itemId);

        if (itemId != null && !itemId.isEmpty()) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
            LOGGER.info("Found item: {} for itemId: {}", item, itemId);

            if (item != null) {
                // 渲染目标物品（正常大小）
                LOGGER.info("Rendering target item...");
                poseStack.pushPose();

                ItemStack targetStack = new ItemStack(item);

                // 使用 renderStatic 渲染目标物品
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        targetStack,
                        transformType,
                        packedLight,
                        packedOverlay,
                        poseStack,
                        buffer,
                        null,
                        0
                );

                poseStack.popPose();
                return;
            }
        }

        // 没 NBT 或异常 → 不渲染任何东西
        LOGGER.info("No NBT or invalid item, showing nothing");
    }
}
