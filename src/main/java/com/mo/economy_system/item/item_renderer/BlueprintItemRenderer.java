package com.mo.economy_system.item.item_renderer;

import com.mo.economy_system.item.items.BlueprintItem;
import com.mojang.blaze3d.vertex.PoseStack;
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

@OnlyIn(Dist.CLIENT)
public class BlueprintItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final ItemRenderer itemRenderer;

    public BlueprintItemRenderer() {
        super(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels()
        );
        this.itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        // ① 渲染蓝图本体（模型 + layer0）
        itemRenderer.renderStatic(
                stack,
                context,
                light,
                overlay,
                poseStack,
                buffer,
                Minecraft.getInstance().level,
                0
        );

        // ② 读取 NBT
        String itemId = BlueprintItem.getUnlockedItemId(stack);
        if (itemId == null || itemId.isEmpty()) return;

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item == null) return;

        ItemStack overlayStack = new ItemStack(item);

        // ③ 叠加渲染
        poseStack.pushPose();

        // ⭐ 关键：Z 轴前移，否则一定被遮挡
        poseStack.translate(0.0F, 0.0F, 0.01F);

        // 缩放到蓝图中间
        poseStack.scale(0.5F, 0.5F, 0.5F);

        itemRenderer.renderStatic(
                overlayStack,
                ItemDisplayContext.GUI,
                light,
                overlay,
                poseStack,
                buffer,
                Minecraft.getInstance().level,
                1
        );

        poseStack.popPose();
    }
}
