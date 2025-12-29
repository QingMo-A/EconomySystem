//package com.mo.economy_system.core.playerattributes_system.ui;
//
//import com.mo.economy_system.core.playerattributes_system.strength.StrengthBarRenderer;
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.gui.screens.Screen;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.network.chat.Component;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.effect.MobEffectInstance;
//import net.minecraft.world.effect.MobEffects;
//import net.minecraft.world.entity.player.Player;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.api.distmarker.OnlyIn;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//
//@OnlyIn(Dist.CLIENT)
//public class CustomPlayerStatusScreen extends Screen {
//    // ========== 性能优化1：静态常量缓存（避免每次实例化重复创建） ==========
//    // 基础配置
//    public static boolean isEnabled = true;
//    private static final int BACKGROUND_ALPHA = 128;
//    private static final int UI_WIDTH = 120;
//    private static final int UI_HEIGHT = 80;
//    private static final int BORDER_COLOR = 0xFFFFFFFF;
//    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000;
//
//    // 绘制参数
//    private static final int UNIFIED_ICON_SIZE = 9;
//    private static final int BAR_WIDTH = 55;
//    private static final int BAR_HEIGHT = 7;
//    private static final int ICON_BAR_SPACING = 3;
//    private static final int MODULE_SPACING = 6;
//    private static final int TEXT_ICON_SPACING = 4;
//    private static final int ABSORB_TOP_OFFSET = 2;
//    private static final int ABSORB_BAR_MAX_WIDTH = BAR_WIDTH;
//    private static final int ABSORB_BAR_HEIGHT = BAR_HEIGHT;
//    private static final int ABSORB_ICON_BAR_SPACING = 3;
//
//    // 配色参数
//    private static final int HEALTH_FG_COLOR = (255 << 24) | 0xFF3333;
//    private static final int FOOD_FG_COLOR = (255 << 24) | 0xFFFF88;
//    private static final int STRENGTH_FG_COLOR = (255 << 24) | 0x33FF33;
//    private static final int LOW_COLOR = (255 << 24) | 0xFF2222;
//    private static final int ABSORB_BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x555533;
//    private static final int ABSORB_FG_COLOR = (255 << 24) | 0xFFFFAA;
//    private static final int ABSORB_HEART_COLOR = (255 << 24) | 0xFFFFAA;
//    private static final int ABSORB_TEXT_COLOR = 0xFFFFDD;
//    private static final int WITHER_HEALTH_COLOR = (255 << 24) | 0x555555;
//    private static final int POISON_HEALTH_COLOR = (255 << 24) | 0x77FF77;
//    private static final int TEXT_COLOR = 0xEEEEEE;
//    private static final int WITHER_TEXT_COLOR = 0x999999;
//    private static final int POISON_TEXT_COLOR = 0x99FF99;
//
//    // 图标配置
//    private static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");
//    private static final ResourceLocation MOB_EFFECTS = new ResourceLocation("textures/mob_effect/speed.png");
//    private static final int SPEED_POTION_ACTUAL_WIDTH = 18;
//    private static final int SPEED_POTION_ACTUAL_HEIGHT = 18;
//    private static final int BORDER_HEART_UV_X = 16;
//    private static final int BORDER_HEART_UV_Y = 0;
//    private static final int BORDER_HEART_SIZE = 9;
//    private static final int HEART_UV_X = 53;
//    private static final int HEART_UV_Y = 1;
//    private static final int HEART_SIZE = 7;
//    private static final int BORDER_ABSORB_HEART_UV_X = 16;
//    private static final int BORDER_ABSORB_HEART_UV_Y = 0;
//    private static final int ABSORB_HEART_UV_X = 161;
//    private static final int ABSORB_HEART_UV_Y = 1;
//    private static final int ABSORB_HEART_SIZE = 7;
//    private static final int BORDER_FOOD_UV_X = 16;
//    private static final int BORDER_FOOD_UV_Y = 27;
//    private static final int BORDER_FOOD_SIZE = 9;
//    private static final int FOOD_UV_X = 53;
//    private static final int FOOD_UV_Y = 28;
//    private static final int FOOD_SIZE = 7;
//    private static final int SPEED_POTION_UV_X = 0;
//    private static final int SPEED_POTION_UV_Y = 0;
//    private static final int ORIGINAL_POTION_SIZE = 16;
//    private static final float POTION_SCALE_RATIO = (float) UNIFIED_ICON_SIZE / ORIGINAL_POTION_SIZE;
//
//    // ========== 实例变量（仅存储动态变化的坐标，避免重复计算） ==========
//    private int uiX;
//    private int uiY;
//    private static final Map<UUID, Float> REMAIN_ABSORB_HEALTH_CACHE = new HashMap<>();
//    private static final Map<UUID, Float> MAX_ABSORB_HEALTH_CACHE = new HashMap<>();
//
//    // 构造方法
//    public CustomPlayerStatusScreen() {
//        super(Component.literal("玩家状态界面"));
//    }
//
//    // ========== 性能优化2：init方法只计算一次坐标（无需每帧重复计算） ==========
//    @Override
//    protected void init() {
//        super.init();
//        // 仅在初始化时计算UI坐标，后续渲染直接复用
//        this.uiX = (this.width - UI_WIDTH) / 2;
//        this.uiY = (this.height - UI_HEIGHT) / 2;
//    }
//
//    // ========== 核心渲染：减少冗余判断，只做必要渲染 ==========
//    @Override
//    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
//        // 性能优化3：玩家无效/UI禁用时，只绘制背景，快速返回
//        if (!isEnabled) {
//            super.render(guiGraphics, mouseX, mouseY, partialTick);
//            return;
//        }
//
//        Minecraft mc = Minecraft.getInstance();
//        Player player = mc.player;
//        if (player == null || player.isDeadOrDying()) {
//            // 绘制基础背景，避免空白闪烁
//            guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + UI_WIDTH, uiY + UI_HEIGHT, BG_COLOR);
//            drawBorder(guiGraphics, uiX, uiY, UI_WIDTH, UI_HEIGHT);
//            guiGraphics.drawString(this.font, "玩家无效", uiX + 10, uiY + 10, 0xFF0000);
//            super.render(guiGraphics, mouseX, mouseY, partialTick);
//            return;
//        }
//
//        // 1. 绘制背景和边框（仅一次绘制，无重复操作）
//        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + UI_WIDTH, uiY + UI_HEIGHT, BG_COLOR);
//        drawBorder(guiGraphics, uiX, uiY, UI_WIDTH, UI_HEIGHT);
//
//        // 2. 父类渲染（无按钮，性能损耗可忽略）
//        super.render(guiGraphics, mouseX, mouseY, partialTick);
//
//        // 3. 预缓存动态参数，减少重复调用
//        int baseX = uiX + 10;
//        int baseY = uiY + 10;
//        int currentY = baseY;
//        int healthBarX = baseX + UNIFIED_ICON_SIZE + ICON_BAR_SPACING;
//        int healthBarY = currentY + (UNIFIED_ICON_SIZE - BAR_HEIGHT) / 2;
//        int absorbBarX = healthBarX;
//        int absorbBarY = healthBarY - ABSORB_TOP_OFFSET - BAR_HEIGHT;
//        int absorbIconX = absorbBarX - UNIFIED_ICON_SIZE - ABSORB_ICON_BAR_SPACING;
//        int absorbIconY = absorbBarY + (ABSORB_BAR_HEIGHT - UNIFIED_ICON_SIZE) / 2;
//        UUID playerUUID = player.getUUID();
//
//        // ========== 绘制伤害吸收条 ==========
//        MobEffectInstance absorbEffect = player.getEffect(MobEffects.ABSORPTION);
//        float remainAbsorbHealth = REMAIN_ABSORB_HEALTH_CACHE.getOrDefault(playerUUID, 0f);
//        float maxAbsorbHealth = MAX_ABSORB_HEALTH_CACHE.getOrDefault(playerUUID, 0f);
//
//        if (absorbEffect != null && maxAbsorbHealth <= 0) {
//            maxAbsorbHealth = (float) (absorbEffect.getAmplifier() + 1) * 4.0f;
//            remainAbsorbHealth = maxAbsorbHealth;
//        }
//
//        if (remainAbsorbHealth > 0 && maxAbsorbHealth > 0) {
//            float absorbRatio = Math.min(1.0f, remainAbsorbHealth / maxAbsorbHealth);
//            int absorbBarWidth = (int) (ABSORB_BAR_MAX_WIDTH * absorbRatio);
//
//            // 绘制吸收爱心
//            drawSingleIcon(guiGraphics, GUI_ICONS, BORDER_ABSORB_HEART_UV_X, BORDER_ABSORB_HEART_UV_Y, BORDER_HEART_SIZE, absorbIconX, absorbIconY);
//            int absorbHeartOffsetX = (BORDER_HEART_SIZE - ABSORB_HEART_SIZE) / 2;
//            int absorbHeartOffsetY = (BORDER_HEART_SIZE - ABSORB_HEART_SIZE) / 2;
//            drawColoredIcon(guiGraphics, GUI_ICONS, ABSORB_HEART_UV_X, ABSORB_HEART_UV_Y, ABSORB_HEART_SIZE,
//                    absorbIconX + absorbHeartOffsetX, absorbIconY + absorbHeartOffsetY, ABSORB_HEART_COLOR);
//
//            // 绘制吸收条
//            guiGraphics.fill(RenderType.gui(), absorbBarX, absorbBarY, absorbBarX + ABSORB_BAR_MAX_WIDTH, absorbBarY + ABSORB_BAR_HEIGHT, ABSORB_BG_COLOR);
//            if (absorbBarWidth > 0) {
//                guiGraphics.fill(RenderType.gui(), absorbBarX, absorbBarY, absorbBarX + absorbBarWidth, absorbBarY + ABSORB_BAR_HEIGHT, ABSORB_FG_COLOR);
//            }
//            drawBorder(guiGraphics, absorbBarX, absorbBarY, ABSORB_BAR_MAX_WIDTH, ABSORB_BAR_HEIGHT);
//
//            // 绘制吸收数值
//            String absorbText = (int) remainAbsorbHealth + "/" + (int) maxAbsorbHealth;
//            int absorbTextX = absorbBarX + (ABSORB_BAR_MAX_WIDTH / 2) - (mc.font.width(absorbText) / 2);
//            int absorbTextY = absorbBarY - mc.font.lineHeight - TEXT_ICON_SPACING;
//            guiGraphics.drawString(mc.font, absorbText, absorbTextX, absorbTextY, ABSORB_TEXT_COLOR);
//        }
//
//        // ========== 绘制血量条 ==========
//        float currentHealth = player.getHealth();
//        float maxHealth = player.getMaxHealth();
//        boolean hasWither = player.hasEffect(MobEffects.WITHER);
//        boolean hasPoison = player.hasEffect(MobEffects.POISON);
//
//        int healthFgColor = HEALTH_FG_COLOR;
//        int healthTextColor = TEXT_COLOR;
//        if (hasWither) {
//            healthFgColor = WITHER_HEALTH_COLOR;
//            healthTextColor = WITHER_TEXT_COLOR;
//        } else if (hasPoison) {
//            healthFgColor = POISON_HEALTH_COLOR;
//            healthTextColor = POISON_TEXT_COLOR;
//        }
//
//        drawIconAndBarWithOffset(guiGraphics, GUI_ICONS,
//                BORDER_HEART_UV_X, BORDER_HEART_UV_Y, BORDER_HEART_SIZE,
//                HEART_UV_X, HEART_UV_Y, HEART_SIZE,
//                baseX, currentY,
//                currentHealth, maxHealth, healthFgColor);
//
//        String healthText = (int) currentHealth + "/" + (int) maxHealth;
//        int healthTextX = healthBarX + BAR_WIDTH + 5;
//        int healthTextY = currentY + (UNIFIED_ICON_SIZE - mc.font.lineHeight) / 2;
//        guiGraphics.drawString(mc.font, healthText, healthTextX, healthTextY, healthTextColor);
//
//        currentY += UNIFIED_ICON_SIZE + MODULE_SPACING;
//
//        // ========== 绘制饥饿条 ==========
//        float currentFood = player.getFoodData().getFoodLevel();
//        drawIconAndBarWithOffset(guiGraphics, GUI_ICONS,
//                BORDER_FOOD_UV_X, BORDER_FOOD_UV_Y, BORDER_FOOD_SIZE,
//                FOOD_UV_X, FOOD_UV_Y, FOOD_SIZE,
//                baseX, currentY,
//                currentFood, 20.0f, FOOD_FG_COLOR);
//
//        String foodText = (int) currentFood + "/20";
//        int foodTextX = healthBarX + BAR_WIDTH + 5;
//        int foodTextY = currentY + (UNIFIED_ICON_SIZE - mc.font.lineHeight) / 2;
//        guiGraphics.drawString(mc.font, foodText, foodTextX, foodTextY, TEXT_COLOR);
//
//        currentY += UNIFIED_ICON_SIZE + MODULE_SPACING;
//
//        // ========== 绘制体力条 ==========
//        int currentStrength = StrengthBarRenderer.getCurrentStrengthClient(player);
//        int maxStrength = StrengthBarRenderer.getMaxStrengthClient(player);
//        if (maxStrength <= 0) maxStrength = 100;
//
//        drawScaledPotionIconAndBar(guiGraphics, MOB_EFFECTS,
//                SPEED_POTION_UV_X, SPEED_POTION_UV_Y, ORIGINAL_POTION_SIZE,
//                POTION_SCALE_RATIO, baseX, currentY,
//                (float) currentStrength, (float) maxStrength, STRENGTH_FG_COLOR);
//
//        String strengthText = currentStrength + "/" + maxStrength;
//        int strengthTextX = healthBarX + BAR_WIDTH + 5;
//        int strengthTextY = currentY + (UNIFIED_ICON_SIZE - mc.font.lineHeight) / 2;
//        guiGraphics.drawString(mc.font, strengthText, strengthTextX, strengthTextY, TEXT_COLOR);
//    }
//
//    // ========== 辅助方法（无冗余逻辑，性能最优） ==========
//    private void drawSingleIcon(GuiGraphics guiGraphics, ResourceLocation texture, int uvX, int uvY, int iconSize, int iconX, int iconY) {
//        guiGraphics.blit(texture, iconX, iconY, 0,
//                (float) uvX, (float) uvY, iconSize, iconSize, 256, 256);
//    }
//
//    private void drawColoredIcon(GuiGraphics guiGraphics, ResourceLocation texture, int uvX, int uvY, int iconSize, int iconX, int iconY, int color) {
//        guiGraphics.setColor(
//                (float) (color >> 16 & 0xFF) / 255.0f,
//                (float) (color >> 8 & 0xFF) / 255.0f,
//                (float) (color & 0xFF) / 255.0f,
//                (float) (color >> 24 & 0xFF) / 255.0f
//        );
//        guiGraphics.blit(texture, iconX, iconY, 0,
//                (float) uvX, (float) uvY, iconSize, iconSize, 256, 256);
//        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
//    }
//
//    private void drawIconAndBarWithOffset(GuiGraphics guiGraphics, ResourceLocation texture, int borderUvX, int borderUvY, int borderSize,
//                                          int iconUvX, int iconUvY, int iconSize, int baseX, int baseY,
//                                          float currentValue, float maxValue, int normalFgColor) {
//        drawSingleIcon(guiGraphics, texture, borderUvX, borderUvY, borderSize, baseX, baseY);
//        int offsetX = (borderSize - iconSize) / 2;
//        int offsetY = (borderSize - iconSize) / 2;
//        drawSingleIcon(guiGraphics, texture, iconUvX, iconUvY, iconSize, baseX + offsetX, baseY + offsetY);
//
//        float progress = Math.max(0, Math.min(1, currentValue / maxValue));
//        int filledWidth = (int) (BAR_WIDTH * progress);
//        int barX = baseX + UNIFIED_ICON_SIZE + ICON_BAR_SPACING;
//        int barY = baseY + (UNIFIED_ICON_SIZE - BAR_HEIGHT) / 2;
//
//        guiGraphics.fill(RenderType.gui(), barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BG_COLOR);
//        if (filledWidth > 0) {
//            int finalFg = progress < 0.2f ? LOW_COLOR : normalFgColor;
//            guiGraphics.fill(RenderType.gui(), barX, barY, barX + filledWidth, barY + BAR_HEIGHT, finalFg);
//        }
//        drawBorder(guiGraphics, barX, barY, BAR_WIDTH, BAR_HEIGHT);
//    }
//
//    private void drawScaledPotionIconAndBar(GuiGraphics guiGraphics, ResourceLocation texture, int uvX, int uvY, int originalPotionSize,
//                                            float scaleRatio, int baseX, int baseY, float currentValue, float maxValue, int normalFgColor) {
//        PoseStack poseStack = guiGraphics.pose();
//        poseStack.pushPose();
//        poseStack.scale(scaleRatio, scaleRatio, 1.0f);
//        float scaledX = (float) baseX / scaleRatio;
//        float scaledY = (float) baseY / scaleRatio;
//
//        guiGraphics.blit(texture,
//                (int) scaledX, (int) scaledY, 0,
//                (float) uvX, (float) uvY,
//                originalPotionSize, originalPotionSize,
//                SPEED_POTION_ACTUAL_WIDTH, SPEED_POTION_ACTUAL_HEIGHT);
//
//        poseStack.popPose();
//
//        float progress = Math.max(0, Math.min(1, currentValue / maxValue));
//        int filledWidth = (int) (BAR_WIDTH * progress);
//        int barX = baseX + UNIFIED_ICON_SIZE + ICON_BAR_SPACING;
//        int barY = baseY + (UNIFIED_ICON_SIZE - BAR_HEIGHT) / 2;
//
//        guiGraphics.fill(RenderType.gui(), barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, BG_COLOR);
//        if (filledWidth > 0) {
//            int finalFg = progress < 0.2f ? LOW_COLOR : normalFgColor;
//            guiGraphics.fill(RenderType.gui(), barX, barY, barX + filledWidth, barY + BAR_HEIGHT, finalFg);
//        }
//        drawBorder(guiGraphics, barX, barY, BAR_WIDTH, BAR_HEIGHT);
//    }
//
//    private void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
//        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, BORDER_COLOR);
//        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, BORDER_COLOR);
//        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, BORDER_COLOR);
//        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, BORDER_COLOR);
//    }
//
//    // ========== 静态方法（缓存操作） ==========
//    public static void setRemainAbsorbHealth(Player player, float remainAbsorb) {
//        if (player == null) return;
//        REMAIN_ABSORB_HEALTH_CACHE.put(player.getUUID(), Math.max(0, remainAbsorb));
//    }
//
//    public static void setMaxAbsorbHealth(Player player, float maxAbsorb) {
//        if (player == null) return;
//        MAX_ABSORB_HEALTH_CACHE.put(player.getUUID(), Math.max(1, maxAbsorb));
//    }
//
//    // ========== 按键与关闭逻辑（不变） ==========
//    @Override
//    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//        if (keyCode == 256) {
//            this.onClose();
//            return true;
//        }
//        return super.keyPressed(keyCode, scanCode, modifiers);
//    }
//
//    @Override
//    public void onClose() {
//        Minecraft.getInstance().setScreen(null);
//        super.onClose();
//    }
//
//    @Override
//    public boolean isPauseScreen() {
//        return false;
//    }
//}