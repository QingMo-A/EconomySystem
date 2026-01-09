package com.mo.economy_system.core.playerattributes_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthClientSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class CustomStatueGUI {
    private static final int PLAYER_ICON_SIZE = 64;
    private static final int PLAYER_UV_X = 0;
    private static final int PLAYER_UV_Y = 0;
    private static final int PLAYER_TEXTURE_TOTAL_WIDTH = 64;
    private static final int PLAYER_TEXTURE_TOTAL_HEIGHT = 64;
    //控制小人与屏幕右侧的距离
//    private static final int RIGHT_OFFSET = 5;
    private static final int LEFT_OFFSET = 20;
    private static final int PLAYER_ICON_Y_OFFSET = 5;
    //样式常量
    // 基础样式
    private static final int BACKGROUND_ALPHA = 128;
    private static final int BORDER_COLOR = 0xFFFFFFFF; // 白色边框，更明显
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000;
    private static final int LOW_COLOR = (255 << 24) | 0xFF2222;

    private static final int BAR_WIDTH = 5; // 进度条宽度
    private static final int BAR_HEIGHT = 40;//进度条高度
    private static final int BAR_TO_PLAYER_SPACING = 1; //进度条与小人的间距）
    private static final int BAR_BAR_SPACING = 3; //两进度条之间的间距

    // 颜色配置（深饱和鲜艳版本）
    private static final int FOOD_BAR_COLOR = (255 << 24) | 0xFFCC00;      // 深金黄色
    private static final int STRENGTH_BAR_COLOR = (255 << 24) | 0x00DD00;   // 深绿色
    // 勇气值进度条为深紫色
    private static final int COURAGE_BAR_COLOR = (255 << 24) | 0xCC00FF;    // 深紫色
    // 感染值进度条为深绿色
    private static final int INFECTION_BAR_COLOR = (255 << 24) | 0x00DD00;  // 深绿色

    //缓存路径，优化
    private static final Map<String, ResourceLocation> PLAYER_HEALTH_TEXTURES = new HashMap<>();
    // 静态代码块：初始化时缓存所有纹理
    static {
        String[] suffixes = {"1", "0.8", "0.5", "0.4", "0.3", "0.1", "0"};
        for (String suffix : suffixes) {
            PLAYER_HEALTH_TEXTURES.put(suffix, new ResourceLocation(EconomySystem.MODID, "textures/gui/health/" + suffix + ".png"));
        }
    }

    //缓存坐标，优化
    // 缓存小人坐标
    private static int CACHED_PLAYER_ICON_X = 0;
    private static int CACHED_PLAYER_ICON_Y = 0;
    // 缓存上一次的屏幕宽高和GUI缩放（用于判断是否需要重新计算）
    private static int CACHED_SCREEN_WIDTH = 0;
    private static int CACHED_SCREEN_HEIGHT = 0;
    private static double CACHED_GUI_SCALE = 0.0D;
    //进度条坐标缓存
    private static int CACHED_FOOD_BAR_X = 0;
    private static int CACHED_FOOD_BAR_Y = 0;
    private static int CACHED_STRENGTH_BAR_X = 0;
    private static int CACHED_STRENGTH_BAR_Y = 0;
    // 勇气值进度条坐标缓存（最左侧）
    private static int CACHED_COURAGE_BAR_X = 0;
    private static int CACHED_COURAGE_BAR_Y = 0;
    // 感染值进度条坐标缓存
    private static int CACHED_INFECTION_BAR_X = 0;
    private static int CACHED_INFECTION_BAR_Y = 0;

    /**
     * 计算并缓存小人坐标 + 进度条坐标
     */
    private static void calculateAndCachePlayerCoords() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double guiScale = mc.getWindow().getGuiScale();

        // 计算小人坐标
        CACHED_PLAYER_ICON_X = screenWidth / 2 - PLAYER_ICON_SIZE - 90 - 2;
        CACHED_PLAYER_ICON_Y = screenHeight - PLAYER_ICON_Y_OFFSET - PLAYER_ICON_SIZE;

        //同步计算并缓存进度条坐标

        CACHED_FOOD_BAR_X = CACHED_PLAYER_ICON_X - BAR_TO_PLAYER_SPACING - BAR_WIDTH;
        CACHED_FOOD_BAR_Y = CACHED_PLAYER_ICON_Y + (PLAYER_ICON_SIZE / 2) - (BAR_HEIGHT / 2);

        CACHED_STRENGTH_BAR_X = CACHED_FOOD_BAR_X - BAR_BAR_SPACING - BAR_WIDTH;
        CACHED_STRENGTH_BAR_Y = CACHED_PLAYER_ICON_Y + (PLAYER_ICON_SIZE / 2) - (BAR_HEIGHT / 2);
        // 勇气值进度条坐标（最左侧）：体力条左侧
        CACHED_COURAGE_BAR_X = CACHED_STRENGTH_BAR_X - BAR_BAR_SPACING - BAR_WIDTH;
        CACHED_COURAGE_BAR_Y = CACHED_PLAYER_ICON_Y + (PLAYER_ICON_SIZE / 2) - (BAR_HEIGHT / 2);
        // 感染值进度条坐标：勇气值条左侧
        CACHED_INFECTION_BAR_X = CACHED_COURAGE_BAR_X - BAR_BAR_SPACING - BAR_WIDTH;
        CACHED_INFECTION_BAR_Y = CACHED_PLAYER_ICON_Y + (PLAYER_ICON_SIZE / 2) - (BAR_HEIGHT / 2);

        //更新参数缓存
        CACHED_SCREEN_WIDTH = screenWidth;
        CACHED_SCREEN_HEIGHT = screenHeight;
        CACHED_GUI_SCALE = guiScale;
    }

    /**
     * 渲染小人图片
     */
    @SubscribeEvent
    public static void renderCustomPlayerIcon(RenderGuiOverlayEvent.Post event) {
        NamedGuiOverlay overlay = event.getOverlay();
        if (!VanillaGuiOverlay.HOTBAR.id().equals(overlay.id())) {
            return;
        } //只处理快捷栏事件
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null || player.isDeadOrDying()
                || mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.CREATIVE) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        // 获取屏幕缩放后的宽高
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        double currentGuiScale = event.getWindow().getGuiScale();
        //计算小人坐标：靠右、垂直中间
//        int playerIconX = screenWidth - RIGHT_OFFSET - PLAYER_ICON_SIZE;

        //判断是否需要重新计算坐标
        // 首次渲染 或 屏幕宽高/GUI缩放变化时，重新计算坐标
        if (CACHED_SCREEN_WIDTH != screenWidth
                || CACHED_SCREEN_HEIGHT != screenHeight
                || CACHED_GUI_SCALE != currentGuiScale) {
            calculateAndCachePlayerCoords();
        }
        // 直接取用缓存坐标，无需重复计算
        int playerIconX = CACHED_PLAYER_ICON_X;
        int playerIconY = CACHED_PLAYER_ICON_Y;
        // 进度条缓存坐标
        int foodBarX = CACHED_FOOD_BAR_X;
        int foodBarY = CACHED_FOOD_BAR_Y;
        int strengthBarX = CACHED_STRENGTH_BAR_X;
        int strengthBarY = CACHED_STRENGTH_BAR_Y;
        // 勇气值进度条缓存坐标
        int courageBarX = CACHED_COURAGE_BAR_X;
        int courageBarY = CACHED_COURAGE_BAR_Y;
        // 感染值进度条缓存坐标
        int infectionBarX = CACHED_INFECTION_BAR_X;
        int infectionBarY = CACHED_INFECTION_BAR_Y;

        //获取玩家当前血量和最大血量
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        //计算血量百分比，避免最大血量为0时出现除以0异常
        float healthPercentage = (maxHealth <= 0) ? 0 : (currentHealth / maxHealth) * 100;

        //根据血量百分比返回对应的纹理文件名后缀
        String textureSuffix = getTextureSuffixByHealthPercent(healthPercentage);
        ResourceLocation playerIcon = PLAYER_HEALTH_TEXTURES.getOrDefault(textureSuffix, PLAYER_HEALTH_TEXTURES.get("0"));

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

        //绘制饥饿竖向进度条
        int currentFood = player.getFoodData().getFoodLevel();
        int maxFood = 20;
        // 饥饿条坐标：小人左侧
        //绘制饥饿进度条
        drawVerticalProgressBar(guiGraphics, foodBarX, foodBarY, currentFood, maxFood, FOOD_BAR_COLOR);

        //绘制体力竖向进度条
        int currentStrength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
        int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;
        // 体力条坐标：饥饿条左侧，纵向居中
        //绘制体力进度条
        drawVerticalProgressBar(guiGraphics, strengthBarX, strengthBarY, currentStrength, maxStrength, STRENGTH_BAR_COLOR);

        // 绘制勇气值竖向进度条
        float currentCourage = PlayerCourageManager.getCurrentCourageClient(player);
        float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
        if (maxCourage <= 0) maxCourage = 100; // 避免除以0异常
        // 绘制勇气值进度条（最左侧，样式与其他进度条统一）
        drawVerticalProgressBar(guiGraphics, courageBarX, courageBarY, currentCourage, maxCourage, COURAGE_BAR_COLOR);

        // 绘制感染值竖向进度条
        int currentInfection = PlayerInfectionManager.getCurrentInfectionClient(player);
        int maxInfection = 100;
        // 绘制感染值进度条（在勇气值条左侧）
        drawVerticalProgressBar(guiGraphics, infectionBarX, infectionBarY, currentInfection, maxInfection, INFECTION_BAR_COLOR);
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

        // 绘制边框（带淡色发光）
        drawBorder(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, normalColor);
    }
    //重载 float
    private static void drawVerticalProgressBar(GuiGraphics guiGraphics, int x, int y, float currentValue, float maxValue, int normalColor) {
        //绘制背景
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BG_COLOR);

        //计算进度并绘制填充（低进度红色警告色）
        float progress = Math.max(0, Math.min(1, currentValue / maxValue)); // 直接用float计算，无强转
        int fillHeight = (int) (BAR_HEIGHT * progress);
        int fillStartY = y + BAR_HEIGHT - fillHeight; // 从下往上填充

        if (fillHeight > 0) {
            int finalColor = progress < 0.2f ? LOW_COLOR : normalColor;
            guiGraphics.fill(x, fillStartY, x + BAR_WIDTH, y + BAR_HEIGHT, finalColor);
        }

        // 绘制边框（带淡色发光）
        drawBorder(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, normalColor);
    }

    /**
     * 绘制进度条边框（带鲜艳发光效果）
     */
    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int barColor) {
        // 外发光效果（更鲜艳，使用进度条自身的颜色）
        int glowColor = 0x60000000 | (barColor & 0x00FFFFFF);
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, glowColor);

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