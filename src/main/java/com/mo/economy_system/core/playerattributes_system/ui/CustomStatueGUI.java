package com.mo.economy_system.core.playerattributes_system.ui;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.strength.StrengthBarRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class CustomStatueGUI {
    private static final int PLAYER_ICON_SIZE = 64;
    private static final int PLAYER_UV_X = 0;
    private static final int PLAYER_UV_Y = 0;
    private static final int PLAYER_TEXTURE_TOTAL_WIDTH = 64;
    private static final int PLAYER_TEXTURE_TOTAL_HEIGHT = 64;
    //控制小人与屏幕右侧的距离
    private static final int RIGHT_OFFSET = 5;

    //样式常量
    // 基础样式
    private static final int BACKGROUND_ALPHA = 128;
    private static final int BORDER_COLOR = 0xFFCCCCCC; // 淡灰色边框
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000;
    private static final int LOW_COLOR = (255 << 24) | 0xFF2222;

    private static final int BAR_WIDTH = 5; // 进度条宽度
    private static final int BAR_HEIGHT = 40;//进度条高度
    private static final int BAR_TO_PLAYER_SPACING = 1; //进度条与小人的间距）
    private static final int BAR_BAR_SPACING = 3; //两进度条之间的间距

    // 颜色配置
    private static final int FOOD_BAR_COLOR = (255 << 24) | 0xFFFF88;
    private static final int STRENGTH_BAR_COLOR = (255 << 24) | 0x33FF33;

    /**
     * 渲染小人图片
     */
    @SubscribeEvent
    public static void renderCustomPlayerIcon(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || mc.player.isDeadOrDying()
                || mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.CREATIVE) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        // 获取屏幕缩放后的宽高
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        //计算小人坐标：靠右、垂直中间
        int playerIconX = screenWidth - RIGHT_OFFSET - PLAYER_ICON_SIZE;
        int playerIconY = screenHeight / 2 - PLAYER_ICON_SIZE / 2;

        //获取玩家当前血量和最大血量
        float currentHealth = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        //计算血量百分比，避免最大血量为0时出现除以0异常
        float healthPercentage = (maxHealth <= 0) ? 0 : (currentHealth / maxHealth) * 100;

        //根据血量百分比返回对应的纹理文件名后缀
        String textureSuffix = getTextureSuffixByHealthPercent(healthPercentage);

        //动态构建纹理资源路径，匹配图片存放目录
        ResourceLocation playerIcon = new ResourceLocation(EconomySystem.MODID, "textures/gui/health/" + textureSuffix + ".png");

        //绘制小人图标
        guiGraphics.blit(
                playerIcon,
                playerIconX,
                playerIconY,
                PLAYER_UV_X,
                PLAYER_UV_Y,
                PLAYER_ICON_SIZE,
                PLAYER_ICON_SIZE,
                PLAYER_TEXTURE_TOTAL_WIDTH,
                PLAYER_TEXTURE_TOTAL_HEIGHT
        );

        //绘制饥饿竖向进度条（无图标、窄、贴近小人）
        int currentFood = mc.player.getFoodData().getFoodLevel();
        int maxFood = 20;

        // 饥饿条坐标：小人左侧，仅间距2，纵向居中（贴近小人）
        int foodBarX = playerIconX - BAR_TO_PLAYER_SPACING - BAR_WIDTH;
        int foodBarY = playerIconY + (PLAYER_ICON_SIZE / 2) - (BAR_HEIGHT / 2);

        //绘制饥饿进度条
        drawVerticalProgressBar(guiGraphics, foodBarX, foodBarY, currentFood, maxFood, FOOD_BAR_COLOR);

        //绘制体力竖向进度条
        int currentStrength = StrengthBarRenderer.getCurrentStrengthClient(mc.player);
        int maxStrength = StrengthBarRenderer.getMaxStrengthClient(mc.player);
        if (maxStrength <= 0) maxStrength = 100;

        // 体力条坐标：饥饿条左侧，纵向居中
        int strengthBarX = foodBarX - BAR_BAR_SPACING - BAR_WIDTH;
        int strengthBarY = playerIconY + (PLAYER_ICON_SIZE / 2) - (BAR_HEIGHT / 2);

        //绘制体力进度条
        drawVerticalProgressBar(guiGraphics, strengthBarX, strengthBarY, currentStrength, maxStrength, STRENGTH_BAR_COLOR);
    }

    /**
     * 绘制竖向进度条
     */
    private static void drawVerticalProgressBar(GuiGraphics guiGraphics, int x, int y, int currentValue, int maxValue, int normalColor) {
        //绘制背景
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BG_COLOR);

        //计算进度并绘制填充（低进度红色警告色）
        float progress = Math.max(0, Math.min(1, (float) currentValue / maxValue));
        int fillHeight = (int) (BAR_HEIGHT * progress);
        int fillStartY = y + BAR_HEIGHT - fillHeight; // 从下往上填充

        if (fillHeight > 0) {
            int finalColor = progress < 0.2f ? LOW_COLOR : normalColor;
            guiGraphics.fill(x, fillStartY, x + BAR_WIDTH, y + BAR_HEIGHT, finalColor);
        }

        // 绘制淡色边框
        drawBorder(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT);
    }

    /**
     * 绘制进度条边框
     */
    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 上边框
        guiGraphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        // 下边框
        guiGraphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        // 左边框
        guiGraphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        // 右边框
        guiGraphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);
    }

    /**
     * 根据血量百分比返回对应的纹理文件名后缀
     */
    private static String getTextureSuffixByHealthPercent(float healthPercentage) {
        if (healthPercentage >= 100.0F) {
            return "1";
        } else if (healthPercentage > 50.0F) {
            return "0.8";
        } else if (healthPercentage > 40.0F) {
            return "0.5";
        } else if (healthPercentage > 30.0F) {
            return "0.4";
        } else if (healthPercentage > 10.0F) {
            return "0.3";
        }else if (healthPercentage > 5.0F) {
            return "0.1";
        } else if (healthPercentage <= 5.0F) {
            return "0";
        } else {
            return "0";
        }
    }

    //拦截原版UI：在UI渲染前取消原版血量和饱食度的渲染事件
    @SubscribeEvent
    public static void interceptVanillaHealthAndFoodUI(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        NamedGuiOverlay currentOverlay = event.getOverlay();
        //拦截原版玩家血量UI
        if (currentOverlay.id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
            event.setCanceled(true);
        }
        //拦截原版玩家饱食度UI
        if (currentOverlay.id().equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
            event.setCanceled(true);
        }
    }
}