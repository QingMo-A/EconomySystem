package com.mo.economy_system.armor.armors;

import com.mo.economy_system.EconomySystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;

@OnlyIn(Dist.CLIENT)
public class CustomHitboxRenderer {

    // 是否启用渲染
    private static boolean enabled = false;

    // 渲染范围
    private static double renderRange = 32.0;

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static void setRenderRange(double range) {
        renderRange = range;
    }

    // 事件监听器方法，签名正确
    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (!enabled || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;

        // 渲染所有在范围内的实体碰撞箱
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue; // 跳过玩家自己

            double distance = entity.distanceTo(mc.player);
            if (distance <= renderRange) {
                renderEntityHitbox(poseStack, bufferSource, entity, camX, camY, camZ);
            }
        }

        bufferSource.endBatch();
    }

    // 渲染实体碰撞箱的方法
    private static void renderEntityHitbox(PoseStack poseStack, MultiBufferSource bufferSource,
                                           Entity entity, double camX, double camY, double camZ) {
        AABB aabb = entity.getBoundingBox();

        // 根据实体类型设置不同颜色
        Color color = getColorForEntity(entity);

        // 获取顶点消费者
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        // 计算相对于相机的位置
        double minX = aabb.minX - camX;
        double minY = aabb.minY - camY;
        double minZ = aabb.minZ - camZ;
        double maxX = aabb.maxX - camX;
        double maxY = aabb.maxY - camY;
        double maxZ = aabb.maxZ - camZ;

        // 渲染碰撞箱
        LevelRenderer.renderLineBox(
                poseStack, vertexConsumer,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                color.getRed() / 255.0F,
                color.getGreen() / 255.0F,
                color.getBlue() / 255.0F,
                1.0F
        );

        // 如果是生物，渲染视线
        if (entity instanceof LivingEntity living) {
            renderEntityLookAt(poseStack, bufferSource, living, camX, camY, camZ);
        }
    }

    // 渲染实体的视线
    private static void renderEntityLookAt(PoseStack poseStack, MultiBufferSource bufferSource,
                                           LivingEntity entity, double camX, double camY, double camZ) {
        // 获取眼睛位置
        Vec3 eyePos = entity.getEyePosition();
        double eyeX = eyePos.x - camX;
        double eyeY = eyePos.y - camY;
        double eyeZ = eyePos.z - camZ;

        // 获取视线方向
        Vec3 lookVec = entity.getViewVector(1.0F).scale(5.0); // 5格距离

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());

        // 绘制视线线
        LevelRenderer.renderLineBox(
                poseStack, vertexConsumer,
                eyeX, eyeY, eyeZ,
                eyeX + lookVec.x, eyeY + lookVec.y, eyeZ + lookVec.z,
                0.0F, 1.0F, 0.0F, 1.0F // 绿色
        );
    }

    private static Color getColorForEntity(Entity entity) {
        // 根据实体类型返回不同颜色
        if (entity instanceof net.minecraft.world.entity.monster.Monster) {
            return Color.RED; // 敌对生物：红色
        } else if (entity instanceof net.minecraft.world.entity.animal.Animal) {
            return Color.GREEN; // 动物：绿色
        } else if (entity instanceof net.minecraft.world.entity.npc.Villager) {
            return Color.BLUE; // 村民：蓝色
        } else if (entity instanceof net.minecraft.world.entity.player.Player) {
            return Color.YELLOW; // 玩家：黄色
        } else {
            return Color.WHITE; // 其他：白色
        }
    }
}
