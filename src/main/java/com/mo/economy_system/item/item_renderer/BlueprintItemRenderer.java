package com.mo.economy_system.item.item_renderer;

import net.minecraft.core.registries.BuiltInRegistries;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mo.economy_system.item.items.Item_Blueprint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;


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

        // 如果不在GUI中(物品栏)
        if (transformType != ItemDisplayContext.GUI) {
            // 只渲染底层贴图
            renderBase(stack, poseStack, buffer, packedLight, packedOverlay);

            return;
        }

        String itemId = Item_Blueprint.getUnlockedItemId(stack);

        // 第一步：渲染蓝图自己的材质（底层）
        renderBase(stack, poseStack, buffer, packedLight, packedOverlay);

        // 第二步：如果有目标物品，在蓝图上方叠加渲染目标物品图标
        if (itemId != null && !itemId.isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (item != null) {
                ItemStack targetStack = new ItemStack(item);

                poseStack.pushPose();

                // Z 轴偏移必须在最前面，确保目标物品在蓝图上方
                poseStack.translate(0.3F, 0.3F, 0.7F);

                // 缩小到 60%（因为物品原点在中心，缩放后自动居中）
                poseStack.scale(0.6F, 0.6F, 0.6F);

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
                poseStack.popPose();
            }
        }
    }

    private void renderBase(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel blueprintModel = modelManager.getModel(
                new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath("economy_system", "blueprint"), "inventory")
        );

        poseStack.pushPose();
        for (var renderType : blueprintModel.getRenderTypes(stack, false)) {
            VertexConsumer vertexConsumer = buffer.getBuffer(renderType);
            Minecraft.getInstance().getItemRenderer().renderModelLists(
                    blueprintModel,
                    stack,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    vertexConsumer
            );
        }
        poseStack.popPose();
    }
}
