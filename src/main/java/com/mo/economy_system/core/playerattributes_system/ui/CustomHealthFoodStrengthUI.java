package com.mo.economy_system.core.playerattributes_system.ui;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.strength.StrengthBarRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 仅客户端注册事件
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class CustomHealthFoodStrengthUI {
    // ===================== 基础配置（坐标调整+吸收条配置） =====================
    private static final int UNIFIED_ICON_SIZE = 9;
    private static final int BAR_WIDTH = 55;
    private static final int BAR_HEIGHT = 7;
    private static final int ICON_BAR_SPACING = 3;
    private static final int MODULE_SPACING = 6;
    private static final int TEXT_ICON_SPACING = 4;
    private static final int ABSORB_TOP_OFFSET = 2;
    private static final int ABSORB_BAR_MAX_WIDTH = BAR_WIDTH;
    private static final int ABSORB_BAR_HEIGHT = BAR_HEIGHT;
    private static final int ABSORB_ICON_BAR_SPACING = 3;
    // 右侧偏移微调：可自定义（数值越小，越靠近“右侧一半”，默认30）
    private static final int RIGHT_HALF_OFFSET = 10;
    private static final int BOTTOM_OFFSET_RATIO = 11;

    // 基础配色
    private static final int BACKGROUND_ALPHA = 160;
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x333333;
    private static final int HEALTH_FG_COLOR = (255 << 24) | 0xFF3333;
    private static final int FOOD_FG_COLOR = (255 << 24) | 0xFFFF88;
    private static final int STRENGTH_FG_COLOR = (255 << 24) | 0x33FF33;
    private static final int LOW_COLOR = (255 << 24) | 0xFF2222;
    private static final int BORDER_COLOR = 0xA0000000;
    // 吸收条专属配色
    private static final int ABSORB_BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x555533;
    private static final int ABSORB_FG_COLOR = (255 << 24) | 0xFFFFAA;
    private static final int ABSORB_HEART_COLOR = (255 << 24) | 0xFFFFAA;
    private static final int ABSORB_TEXT_COLOR = 0xFFFFDD;
    // 状态效果配色
    private static final int WITHER_HEALTH_COLOR = (255 << 24) | 0x555555;
    private static final int POISON_HEALTH_COLOR = (255 << 24) | 0x77FF77;
    private static final int TEXT_COLOR = 0xEEEEEE;
    private static final int WITHER_TEXT_COLOR = 0x999999;
    private static final int POISON_TEXT_COLOR = 0x99FF99;

    // ===================== 新增：吸收血量客户端缓存（修复不减少问题） =====================
    private static final Map<UUID, Float> REMAIN_ABSORB_HEALTH_CACHE = new HashMap<>();
    private static final Map<UUID, Float> MAX_ABSORB_HEALTH_CACHE = new HashMap<>();

    // 供服务端同步：设置剩余吸收血量
    public static void setRemainAbsorbHealth(Player player, float remainAbsorb) {
        if (player == null) return;
        REMAIN_ABSORB_HEALTH_CACHE.put(player.getUUID(), Math.max(0, remainAbsorb));
    }

    // 供服务端同步：设置最大吸收血量
    public static void setMaxAbsorbHealth(Player player, float maxAbsorb) {
        if (player == null) return;
        MAX_ABSORB_HEALTH_CACHE.put(player.getUUID(), Math.max(1, maxAbsorb));
    }

    // 获取剩余吸收血量
    private static float getRemainAbsorbHealth(Player player) {
        if (player == null) return 0;
        return REMAIN_ABSORB_HEALTH_CACHE.getOrDefault(player.getUUID(), 0f);
    }

    // 获取最大吸收血量
    private static float getMaxAbsorbHealth(Player player) {
        if (player == null) return 0;
        return MAX_ABSORB_HEALTH_CACHE.getOrDefault(player.getUUID(), 0f);
    }

    // ===================== 图标配置（不变） =====================
    private static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final ResourceLocation MOB_EFFECTS = new ResourceLocation("textures/mob_effect/speed.png");
    private static final int SPEED_POTION_ACTUAL_WIDTH = 18;
    private static final int SPEED_POTION_ACTUAL_HEIGHT = 18;

    // 普通爱心
    private static final int BORDER_HEART_UV_X = 16;
    private static final int BORDER_HEART_UV_Y = 0;
    private static final int BORDER_HEART_SIZE = 9;
    private static final int HEART_UV_X = 53;
    private static final int HEART_UV_Y = 1;
    private static final int HEART_SIZE = 7;
    // 黄色吸收爱心（本体UV=161,1）
    private static final int BORDER_ABSORB_HEART_UV_X = 16;
    private static final int BORDER_ABSORB_HEART_UV_Y = 0;
    private static final int ABSORB_HEART_UV_X = 161;
    private static final int ABSORB_HEART_UV_Y = 1;
    private static final int ABSORB_HEART_SIZE = 7;
    // 饥饿
    private static final int BORDER_FOOD_UV_X = 16;
    private static final int BORDER_FOOD_UV_Y = 27;
    private static final int BORDER_FOOD_SIZE = 9;
    private static final int FOOD_UV_X = 53;
    private static final int FOOD_UV_Y = 28;
    private static final int FOOD_SIZE = 7;
    // 体力药水
    private static final int SPEED_POTION_UV_X = 0;
    private static final int SPEED_POTION_UV_Y = 0;
    private static final int ORIGINAL_POTION_SIZE = 16;
    private static final float POTION_SCALE_RATIO = (float) UNIFIED_ICON_SIZE / ORIGINAL_POTION_SIZE;

    // ===================== 隐藏原版UI（不变） =====================
    @SubscribeEvent
    public static void onPreRenderGui(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        NamedGuiOverlay currentOverlay = event.getOverlay();
        if (currentOverlay.id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) event.setCanceled(true);
        if (currentOverlay.id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) event.setCanceled(true);
    }

    // ===================== 核心绘制UI（坐标调整+修复吸收血量） =====================
    @SubscribeEvent
    public static void onPostRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null || player.isDeadOrDying()) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        // ========== 关键：调整baseX到屏幕右侧一半少一点点 ==========
        // 逻辑：screenWidth * 3 / 4（3/4屏幕宽度，右侧区域） - RIGHT_HALF_OFFSET（微调，靠近中间）
        // 替代原有基于RIGHT_OFFSET的固定计算，适配所有分辨率
        int baseX = screenWidth - RIGHT_HALF_OFFSET - UNIFIED_ICON_SIZE - ICON_BAR_SPACING - BAR_WIDTH;
        int totalHeight = (UNIFIED_ICON_SIZE * 3) + (MODULE_SPACING * 2);
        int baseY = screenHeight / 2 - totalHeight / 2;
        int currentY = baseY;

        // ========== 提前计算坐标（不变） ==========
        int healthBarX = baseX + UNIFIED_ICON_SIZE + ICON_BAR_SPACING;
        int healthBarY = currentY + (UNIFIED_ICON_SIZE - BAR_HEIGHT) / 2;
        int absorbBarX = healthBarX;
        int absorbBarY = healthBarY - ABSORB_BAR_HEIGHT - ABSORB_TOP_OFFSET;
        int absorbIconX = absorbBarX - UNIFIED_ICON_SIZE - ABSORB_ICON_BAR_SPACING;
        int absorbIconY = absorbBarY + (ABSORB_BAR_HEIGHT - UNIFIED_ICON_SIZE) / 2;

        // ========== 1. 绘制伤害吸收（修复血量不减少） ==========
        MobEffectInstance absorbEffect = player.getEffect(MobEffects.ABSORPTION);
        // 优先从缓存获取剩余吸收血量和最大吸收血量（服务端同步后生效）
        float remainAbsorbHealth = getRemainAbsorbHealth(player);
        float maxAbsorbHealth = getMaxAbsorbHealth(player);

        // 兼容原版效果（无服务端同步时，自动计算最大血量）
        if (absorbEffect != null && maxAbsorbHealth <= 0) {
            maxAbsorbHealth = (float) (absorbEffect.getAmplifier() + 1) * 4.0f;
            remainAbsorbHealth = maxAbsorbHealth; // 原版无剩余获取，暂用最大值（需服务端同步完善）
        }

        // 绘制吸收内容（仅当有剩余吸收血量时）
        if (remainAbsorbHealth > 0 && maxAbsorbHealth > 0) {
            // 计算动态长度（基于剩余血量，不再是固定最大值）
            float absorbRatio = Math.min(1.0f, remainAbsorbHealth / maxAbsorbHealth);
            int absorbBarWidth = (int) (ABSORB_BAR_MAX_WIDTH * absorbRatio);

            // 1. 绘制黄色爱心
            drawSingleIcon(guiGraphics, GUI_ICONS, BORDER_ABSORB_HEART_UV_X, BORDER_ABSORB_HEART_UV_Y, BORDER_HEART_SIZE, absorbIconX, absorbIconY);
            int absorbHeartOffsetX = (BORDER_HEART_SIZE - ABSORB_HEART_SIZE) / 2;
            int absorbHeartOffsetY = (BORDER_HEART_SIZE - ABSORB_HEART_SIZE) / 2;
            drawColoredIcon(guiGraphics, GUI_ICONS, ABSORB_HEART_UV_X, ABSORB_HEART_UV_Y, ABSORB_HEART_SIZE,
                    absorbIconX + absorbHeartOffsetX, absorbIconY + absorbHeartOffsetY, ABSORB_HEART_COLOR);

            // 2. 绘制黄色吸收条（动态长度）
            guiGraphics.fill(RenderType.gui(), absorbBarX, absorbBarY, absorbBarX + ABSORB_BAR_MAX_WIDTH, absorbBarY + ABSORB_BAR_HEIGHT, ABSORB_BG_COLOR);
            if (absorbBarWidth > 0) {
                guiGraphics.fill(RenderType.gui(), absorbBarX, absorbBarY, absorbBarX + absorbBarWidth, absorbBarY + ABSORB_BAR_HEIGHT, ABSORB_FG_COLOR);
            }
            drawBorder(guiGraphics, absorbBarX, absorbBarY, ABSORB_BAR_MAX_WIDTH, ABSORB_BAR_HEIGHT);

            // 3. 绘制吸收数值（剩余/最大）
            String absorbText = (int) remainAbsorbHealth + "/" + (int) maxAbsorbHealth;
            int absorbTextX = absorbBarX + (ABSORB_BAR_MAX_WIDTH / 2) - (mc.font.width(absorbText) / 2);
            int absorbTextY = absorbBarY - mc.font.lineHeight - TEXT_ICON_SPACING;
            guiGraphics.drawString(mc.font, absorbText, absorbTextX, absorbTextY, ABSORB_TEXT_COLOR);
        }

        // ========== 2. 绘制普通血量（不变） ==========
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        boolean hasWither = player.hasEffect(MobEffects.WITHER);
        boolean hasPoison = player.hasEffect(MobEffects.POISON);

        int healthFgColor = HEALTH_FG_COLOR;
        int healthTextColor = TEXT_COLOR;
        if (hasWither) {
            healthFgColor = WITHER_HEALTH_COLOR;
            healthTextColor = WITHER_TEXT_COLOR;
        } else if (hasPoison) {
            healthFgColor = POISON_HEALTH_COLOR;
            healthTextColor = POISON_TEXT_COLOR;
        }

        drawIconAndBarWithOffset(guiGraphics, GUI_ICONS,
                BORDER_HEART_UV_X, BORDER_HEART_UV_Y, BORDER_HEART_SIZE,
                HEART_UV_X, HEART_UV_Y, HEART_SIZE,
                baseX, currentY,
                currentHealth, maxHealth, healthFgColor);

        String healthText = (int) currentHealth + "/" + (int) maxHealth;
        int healthTextX = baseX - mc.font.width(healthText) - TEXT_ICON_SPACING;
        int healthTextY = currentY + (UNIFIED_ICON_SIZE - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, healthText, healthTextX, healthTextY, healthTextColor);

        currentY += UNIFIED_ICON_SIZE + MODULE_SPACING;

        // ========== 3. 饥饿绘制（不变） ==========
        float currentFood = player.getFoodData().getFoodLevel();
        float maxFood = 20.0f;
        drawIconAndBarWithOffset(guiGraphics, GUI_ICONS,
                BORDER_FOOD_UV_X, BORDER_FOOD_UV_Y, BORDER_FOOD_SIZE,
                FOOD_UV_X, FOOD_UV_Y, FOOD_SIZE,
                baseX, currentY,
                currentFood, maxFood, FOOD_FG_COLOR);

        String foodText = (int) currentFood + "/" + (int) maxFood;
        int foodTextX = baseX - mc.font.width(foodText) - TEXT_ICON_SPACING;
        int foodTextY = currentY + (UNIFIED_ICON_SIZE - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, foodText, foodTextX, foodTextY, TEXT_COLOR);

        currentY += UNIFIED_ICON_SIZE + MODULE_SPACING;

        // ========== 4. 体力绘制（不变） ==========
        int currentStrength = StrengthBarRenderer.getCurrentStrengthClient(player);
        int maxStrength = StrengthBarRenderer.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;

        drawScaledPotionIconAndBar(guiGraphics, MOB_EFFECTS,
                SPEED_POTION_UV_X, SPEED_POTION_UV_Y, ORIGINAL_POTION_SIZE,
                POTION_SCALE_RATIO, baseX, currentY,
                (float) currentStrength, (float) maxStrength, STRENGTH_FG_COLOR);

        String strengthText = currentStrength + "/" + maxStrength;
        int strengthTextX = baseX - mc.font.width(strengthText) - TEXT_ICON_SPACING;
        int strengthTextY = currentY + (UNIFIED_ICON_SIZE - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, strengthText, strengthTextX, strengthTextY, TEXT_COLOR);
    }

    // ===================== 辅助方法（不变） =====================
    private static void drawSingleIcon(
            GuiGraphics guiGraphics,
            ResourceLocation texture, int uvX, int uvY, int iconSize,
            int iconX, int iconY
    ) {
        guiGraphics.blit(texture, iconX, iconY, 0,
                (float) uvX, (float) uvY, iconSize, iconSize, 256, 256);
    }

    private static void drawColoredIcon(
            GuiGraphics guiGraphics,
            ResourceLocation texture, int uvX, int uvY, int iconSize,
            int iconX, int iconY, int color
    ) {
        guiGraphics.setColor(
                (float) (color >> 16 & 0xFF) / 255.0f,
                (float) (color >> 8 & 0xFF) / 255.0f,
                (float) (color & 0xFF) / 255.0f,
                (float) (color >> 24 & 0xFF) / 255.0f
        );
        guiGraphics.blit(texture, iconX, iconY, 0,
                (float) uvX, (float) uvY, iconSize, iconSize, 256, 256);
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void drawIconAndBarWithOffset(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int borderUvX, int borderUvY, int borderSize,
            int iconUvX, int iconUvY, int iconSize,
            int baseX, int baseY,
            float currentValue, float maxValue,
            int normalFgColor
    ) {
        drawSingleIcon(guiGraphics, texture, borderUvX, borderUvY, borderSize, baseX, baseY);
        int offsetX = (borderSize - iconSize) / 2;
        int offsetY = (borderSize - iconSize) / 2;
        drawSingleIcon(guiGraphics, texture, iconUvX, iconUvY, iconSize, baseX + offsetX, baseY + offsetY);

        int barX = baseX + UNIFIED_ICON_SIZE + ICON_BAR_SPACING;
        int barY = baseY + (UNIFIED_ICON_SIZE - BAR_HEIGHT) / 2;
        float progress = Math.max(0, Math.min(1, currentValue / maxValue));
        int filledWidth = (int) (BAR_WIDTH * progress);

        guiGraphics.fill(RenderType.gui(), barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BG_COLOR);
        int finalFg = progress < 0.2f ? LOW_COLOR : normalFgColor;
        if (filledWidth > 0) {
            guiGraphics.fill(RenderType.gui(), barX, barY, barX + filledWidth, barY + BAR_HEIGHT, finalFg);
        }
        drawBorder(guiGraphics, barX, barY, BAR_WIDTH, BAR_HEIGHT);
    }

    private static void drawScaledPotionIconAndBar(
            GuiGraphics guiGraphics,
            ResourceLocation texture,
            int uvX, int uvY, int originalPotionSize,
            float scaleRatio,
            int baseX, int baseY,
            float currentValue, float maxValue,
            int normalFgColor
    ) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(scaleRatio, scaleRatio, 1.0f);
        float scaledX = (float) baseX / scaleRatio;
        float scaledY = (float) baseY / scaleRatio;

        guiGraphics.blit(texture,
                (int) scaledX, (int) scaledY, 0,
                (float) uvX, (float) uvY,
                originalPotionSize, originalPotionSize,
                SPEED_POTION_ACTUAL_WIDTH, SPEED_POTION_ACTUAL_HEIGHT);

        poseStack.popPose();

        int barX = baseX + UNIFIED_ICON_SIZE + ICON_BAR_SPACING;
        int barY = baseY + (UNIFIED_ICON_SIZE - BAR_HEIGHT) / 2;
        float progress = Math.max(0, Math.min(1, currentValue / maxValue));
        int filledWidth = (int) (BAR_WIDTH * progress);

        guiGraphics.fill(RenderType.gui(), barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BG_COLOR);
        int finalFg = progress < 0.2f ? LOW_COLOR : normalFgColor;
        if (filledWidth > 0) {
            guiGraphics.fill(RenderType.gui(), barX, barY, barX + filledWidth, barY + BAR_HEIGHT, finalFg);
        }
        drawBorder(guiGraphics, barX, barY, BAR_WIDTH, BAR_HEIGHT);
    }

    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, BORDER_COLOR);
    }
}