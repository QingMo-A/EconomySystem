package com.mo.economy_system.screen.playerattribute_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import com.mo.economy_system.core.playerattributes_system.limb_health_system.LimbClientInjurySync;
import com.mo.economy_system.core.playerattributes_system.limb_health_system.LimbType;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthClientSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class CustomStatueGUI {
    // 玩家图标尺寸和UV坐标
    private static final int PLAYER_ICON_WIDTH = 27;
    private static final int PLAYER_ICON_HEIGHT = 59;
    private static final int PLAYER_UV_X_NORMAL = 0;      // 正常状态 UV X (左边)
    private static final int PLAYER_UV_X_INJURED = 27;    // 受伤状态 UV X (右边，白色边框)
    private static final int PLAYER_UV_Y = 0;
    private static final int PLAYER_TEXTURE_TOTAL_WIDTH = 256;
    private static final int PLAYER_TEXTURE_TOTAL_HEIGHT = 256;

    // 受伤闪烁相关
    private static long lastDamageTime = 0;              // 上次受伤时间（毫秒）
    private static final long DAMAGE_FLASH_DURATION = 400; // 受伤闪烁持续时间（毫秒）
    private static final int FLASH_CYCLE = 100;          // 闪烁周期（毫秒）- 每100ms切换一次
    private static float lastHealth = 0;                 // 上次记录的血量
    //控制小人与屏幕右侧的距离
//    private static final int RIGHT_OFFSET = 5;
    private static final int LEFT_OFFSET = 12;
    private static final int PLAYER_ICON_Y_OFFSET = 5;
    //样式常量
    // 基础样式
    private static final int BACKGROUND_ALPHA = 72;
    private static final int BORDER_COLOR = 0x408A8A8A;
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000;
    private static final int LOW_COLOR = (255 << 24) | 0xFF2222;

    private static final int BAR_WIDTH = 4; // 进度条宽度
    private static final int BAR_HEIGHT = 36;//进度条高度
    private static final int BAR_TO_PLAYER_SPACING = 6; //进度条与小人的间距
    private static final int BAR_BAR_SPACING = 2; //两进度条之间的间距

    // 颜色配置（深饱和鲜艳版本）
    private static final int FOOD_BAR_COLOR = (255 << 24) | 0xFFCC00;      // 深金黄色
    private static final int STRENGTH_BAR_COLOR = (255 << 24) | 0x00DD00;   // 深绿色
    // 勇气值进度条为深紫色
    private static final int COURAGE_BAR_COLOR = (255 << 24) | 0xCC00FF;    // 深紫色

    // 玩家健康图标纹理（单一文件，包含正常和受伤两个状态）
    private static final ResourceLocation PLAYER_HEALTH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "textures/gui/health/health.png");

    // 肢体受伤标记配置（脉冲圆点）
    private static final int INJURY_DOT_COLOR = 0x80FF0000;    // 半透明红色
    private static final int INJURY_DOT_BASE_SIZE = 3;         // 圆点基础大小（缩小）
    private static final int INJURY_DOT_PULSE_SIZE = 2;        // 脉冲放大幅度（缩小）
    private static final long PULSE_CYCLE = 500;               // 脉冲周期（毫秒）

    // 肢体部位在小人图标上的相对位置（相对于playerIconX, playerIconY）
    // 坐标基于小人图标尺寸 27x59
    private static final int HEAD_OFFSET_X = 13;   // 头部中心X偏移
    private static final int HEAD_OFFSET_Y = 5;    // 头部中心Y偏移
    private static final int CHEST_OFFSET_X = 13;  // 胸部中心X偏移
    private static final int CHEST_OFFSET_Y = 22;  // 胸部中心Y偏移
    private static final int LEGS_OFFSET_X = 13;   // 腿部中心X偏移
    private static final int LEGS_OFFSET_Y = 38;   // 腿部中心Y偏移
    private static final int FEET_OFFSET_X = 13;   // 脚部中心X偏移
    private static final int FEET_OFFSET_Y = 52;   // 脚部中心Y偏移

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

        // 计算小人坐标（往左移一点）
        CACHED_PLAYER_ICON_X = LEFT_OFFSET;
        CACHED_PLAYER_ICON_Y = screenHeight - PLAYER_ICON_Y_OFFSET - PLAYER_ICON_HEIGHT;

        //同步计算并缓存进度条坐标

        CACHED_FOOD_BAR_X = CACHED_PLAYER_ICON_X + PLAYER_ICON_WIDTH + BAR_TO_PLAYER_SPACING;
        CACHED_FOOD_BAR_Y = CACHED_PLAYER_ICON_Y + (PLAYER_ICON_HEIGHT / 2) - (BAR_HEIGHT / 2);

        CACHED_STRENGTH_BAR_X = CACHED_FOOD_BAR_X + BAR_BAR_SPACING + BAR_WIDTH;
        CACHED_STRENGTH_BAR_Y = CACHED_PLAYER_ICON_Y + (PLAYER_ICON_HEIGHT / 2) - (BAR_HEIGHT / 2);
        // 勇气值进度条坐标（最左侧）：体力条左侧
        CACHED_COURAGE_BAR_X = CACHED_STRENGTH_BAR_X + BAR_BAR_SPACING + BAR_WIDTH;
        CACHED_COURAGE_BAR_Y = CACHED_PLAYER_ICON_Y + (PLAYER_ICON_HEIGHT / 2) - (BAR_HEIGHT / 2);
        // 感染值进度条坐标：勇气值条左侧
        CACHED_INFECTION_BAR_X = CACHED_COURAGE_BAR_X + BAR_BAR_SPACING + BAR_WIDTH;
        CACHED_INFECTION_BAR_Y = CACHED_PLAYER_ICON_Y + (PLAYER_ICON_HEIGHT / 2) - (BAR_HEIGHT / 2);

        //更新参数缓存
        CACHED_SCREEN_WIDTH = screenWidth;
        CACHED_SCREEN_HEIGHT = screenHeight;
        CACHED_GUI_SCALE = guiScale;
    }

    /**
     * 渲染小人图片
     */
    @SubscribeEvent
    public static void renderCustomPlayerIcon(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        } //只处理快捷栏事件
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null || player.isDeadOrDying()
                || mc.options.hideGui || mc.getDebugOverlay().showDebugScreen()
                || mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.CREATIVE) {
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        // 获取屏幕缩放后的宽高
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double currentGuiScale = mc.getWindow().getGuiScale();
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
        float healthPercent = maxHealth > 0 ? currentHealth / maxHealth : 0;

        int currentFood = player.getFoodData().getFoodLevel();
        int maxFood = 20;

        // 检查是否在受伤闪烁时间内（闪烁两下）
        long currentTime = System.currentTimeMillis();
        long timeSinceDamage = currentTime - lastDamageTime;
        boolean isFlashing = false;
        if (timeSinceDamage < DAMAGE_FLASH_DURATION) {
            // 闪烁两下：0-100ms 显示，100-200ms 隐藏，200-300ms 显示，300-400ms 隐藏
            int cycle = (int) (timeSinceDamage / FLASH_CYCLE);
            isFlashing = (cycle == 0 || cycle == 2);  // 第1和第3个周期显示白色边框
        }

        int uvX = isFlashing ? PLAYER_UV_X_INJURED : PLAYER_UV_X_NORMAL;

        // 绘制小人图标（带血量颜色）
        int healthColor = getHealthColor(healthPercent);
        guiGraphics.setColor(
            (healthColor >> 16 & 0xFF) / 255.0f,
            (healthColor >> 8 & 0xFF) / 255.0f,
            (healthColor & 0xFF) / 255.0f,
            1.0F
        );
        guiGraphics.blit(
                PLAYER_HEALTH_TEXTURE,
                playerIconX,
                playerIconY,
                uvX,
                PLAYER_UV_Y,
                PLAYER_ICON_WIDTH,
                PLAYER_ICON_HEIGHT,
                PLAYER_TEXTURE_TOTAL_WIDTH,
                PLAYER_TEXTURE_TOTAL_HEIGHT
        );
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 绘制肢体受伤标记
        cleanupAndDrawLimbInjuryIcons(guiGraphics, player, playerIconX, playerIconY);

        //绘制饥饿竖向进度条
        drawVerticalProgressBar(guiGraphics, foodBarX, foodBarY, currentFood, maxFood, FOOD_BAR_COLOR);
        drawIcon(guiGraphics, "🍖", foodBarX + BAR_WIDTH / 2, foodBarY - 8, FOOD_BAR_COLOR);

        //绘制体力竖向进度条
        int currentStrength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
        int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;
        drawVerticalProgressBar(guiGraphics, strengthBarX, strengthBarY, currentStrength, maxStrength, STRENGTH_BAR_COLOR);
        drawIcon(guiGraphics, "💪", strengthBarX + BAR_WIDTH / 2, strengthBarY - 8, STRENGTH_BAR_COLOR);

        // 绘制勇气值竖向进度条
        float currentCourage = PlayerCourageManager.getCurrentCourageClient(player);
        float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
        if (maxCourage <= 0) maxCourage = 100; // 避免除以0异常
        drawVerticalProgressBar(guiGraphics, courageBarX, courageBarY, currentCourage, maxCourage, COURAGE_BAR_COLOR);
        drawIcon(guiGraphics, "⚡", courageBarX + BAR_WIDTH / 2, courageBarY - 8, COURAGE_BAR_COLOR);

        // 绘制感染值竖向进度条
        float currentInfection = PlayerInfectionManager.getCurrentInfectionClient(player);
        int maxInfection = 100;
        int infectionColor = getInfectionColor((int) currentInfection, maxInfection);
        drawVerticalProgressBar(guiGraphics, infectionBarX, infectionBarY, (int) currentInfection, maxInfection, infectionColor);
        drawIcon(guiGraphics, "☣", infectionBarX + BAR_WIDTH / 2, infectionBarY - 8, infectionColor);
    }

    /**
     * 根据血量百分比计算颜色（分段线性插值）
     * 确保关键血量点的颜色准确
     *
     * 100% → 纯绿色 (0, 255, 0)
     * 75% → 黄绿色 (128, 255, 0)
     * 50% → 纯黄色 (255, 255, 0)
     * 25% → 橙色 (255, 128, 0)
     * 0% → 纯红色 (255, 0, 0)
     *
     * @param percent 血量百分比 (0.0 ~ 1.0)
     * @return ARGB 颜色值
     */
    private static int getHealthColor(float percent) {
        percent = Math.max(0.0f, Math.min(1.0f, percent));

        int r, g;
        float value = 1.0f;

        // 低血量时降低明度使其偏黑
        if (percent < 0.25f) {
            value = 0.6f + (percent / 0.25f) * 0.4f;
        }

        if (percent > 0.5f) {
            // 50% ~ 100%：黄色 → 绿色
            // 黄色(255, 255, 0) → 绿色(0, 255, 0)
            float t = (percent - 0.5f) * 2.0f;  // 0 ~ 1
            r = (int) ((1.0f - t) * 255 * value);  // 255 → 0
            g = (int) (255 * value);               // 始终255
        } else {
            // 0% ~ 50%：红色 → 黄色（包含50%）
            // 红色(255, 0, 0) → 黄色(255, 255, 0)
            float t = percent * 2.0f;  // 0 ~ 1
            r = (int) (255 * value);   // 始终255
            g = (int) (t * 255 * value);  // 0 → 255
        }

        int ri = Math.max(0, Math.min(255, r));
        int gi = Math.max(0, Math.min(255, g));

        return (255 << 24) | (ri << 16) | (gi << 8);
    }

    /**
     * 根据感染值计算动态颜色
     * 感染值越高，颜色越深
     *
     * 颜色渐变：
     * - 0：浅绿色 (0xBBFFBB)
     * - 50：中等绿色
     * - 100：深绿色 (0x003300)
     *
     * @param currentInfection 当前感染值
     * @param maxInfection 最大感染值
     * @return ARGB 颜色值
     */
    private static int getInfectionColor(int currentInfection, int maxInfection) {
        // 计算感染值百分比（0.0 ~ 1.0）
        float t = (maxInfection <= 0) ? 0 : (float) currentInfection / maxInfection;
        t = Math.max(0.0f, Math.min(1.0f, t));

        // RGB 渐变计算
        // R: 187 (0xBB) → 0
        // G: 255 (0xFF) → 221 → 51 (0x33)
        // B: 187 (0xBB) → 0

        // 使用二次函数让颜色变化更明显（感染值高时颜色加深更快）
        float factor = t * t;  // 二次缓动

        int r = (int) (187 * (1.0f - factor));           // 187 → 0
        int g = (int) (255 - (255 - 51) * factor);        // 255 → 51
        int b = (int) (187 * (1.0f - factor));           // 187 → 0

        return (255 << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * 绘制竖向进度条（带左侧高光效果）
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
            // 左侧高光效果（白色半透明细线）
        }

        // 绘制边框（带淡色发光）
        drawBorder(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, normalColor);
    }
    //重载 float（带左侧高光效果）
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
            // 左侧高光效果（白色半透明细线）
        }

        // 绘制边框（带淡色发光）
        drawBorder(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, normalColor);
    }

    /**
     * 绘制进度条边框（带鲜艳发光效果）
     */
    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int barColor) {
        // 外发光效果（更鲜艳，使用进度条自身的颜色）

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
     * 绘制属性图标（在进度条上方）
     */
    private static void drawIcon(GuiGraphics guiGraphics, String icon, int x, int y, int color) {
        Minecraft mc = Minecraft.getInstance();
        float iconScale = 0.7f; // 图标缩放比例

        // 绘制缩放后的图标
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(iconScale, iconScale, 1.0f);
        guiGraphics.pose().translate(-x, -y, 0);

        // 绘制图标（居中对齐）
        guiGraphics.drawCenteredString(mc.font, icon, x, y - mc.font.lineHeight / 2, color);

        guiGraphics.pose().popPose();
    }

    //拦截原版UI：在UI渲染前取消原版血量和饱食度的渲染事件
    @SubscribeEvent
    public static void interceptVanillaHealthAndFoodUI(RenderGuiLayerEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        //拦截原版玩家血量UI
        if (event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);
        }
        //拦截原版玩家饱食度UI
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)) {
            event.setCanceled(true);
        }
    }

    /**
     * 客户端tick事件 - 检测血量变化，触发受伤闪烁
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        Player player = mc.player;
        float currentHealth = player.getHealth();

        // 检测血量下降（受伤）
        if (currentHealth < lastHealth) {
            lastDamageTime = System.currentTimeMillis();
        }

        lastHealth = currentHealth;
    }

    /**
     * 清理过期的受伤记录并绘制肢体受伤标记
     */
    private static void cleanupAndDrawLimbInjuryIcons(GuiGraphics guiGraphics, Player player, int playerIconX, int playerIconY) {
        // 清理过期记录
        LimbClientInjurySync.cleanupExpiredInjuries(player);

        // 检查并绘制各部位的受伤感叹号
        for (LimbType limbType : LimbType.values()) {
            if (LimbClientInjurySync.isInjuryVisible(player, limbType)) {
                drawLimbInjuryIcon(guiGraphics, playerIconX, playerIconY, limbType);
            }
        }
    }

    /**
     * 绘制单个部位的受伤脉冲圆点
     */
    private static void drawLimbInjuryIcon(GuiGraphics guiGraphics, int playerIconX, int playerIconY, LimbType limbType) {
        // 根据部位获取偏移位置
        int offsetX, offsetY;
        switch (limbType) {
            case HEAD:
                offsetX = HEAD_OFFSET_X;
                offsetY = HEAD_OFFSET_Y;
                break;
            case CHEST:
                offsetX = CHEST_OFFSET_X;
                offsetY = CHEST_OFFSET_Y;
                break;
            case LEGS:
                offsetX = LEGS_OFFSET_X;
                offsetY = LEGS_OFFSET_Y;
                break;
            case FEET:
                offsetX = FEET_OFFSET_X;
                offsetY = FEET_OFFSET_Y;
                break;
            default:
                return;
        }

        // 计算中心位置
        int centerX = playerIconX + offsetX;
        int centerY = playerIconY + offsetY;

        // 计算脉冲大小（呼吸效果：0~1~0 的正弦波）
        long time = System.currentTimeMillis();
        float pulsePhase = (time % PULSE_CYCLE) / (float) PULSE_CYCLE;  // 0 ~ 1
        float pulseFactor = (float) Math.sin(pulsePhase * Math.PI);      // 0 → 1 → 0
        int currentSize = INJURY_DOT_BASE_SIZE + (int) (pulseFactor * INJURY_DOT_PULSE_SIZE);

        // 绘制半透明红色圆点（用小矩形模拟圆形）
        int halfSize = currentSize / 2;
        guiGraphics.fill(
            centerX - halfSize,
            centerY - halfSize,
            centerX + halfSize,
            centerY + halfSize,
            INJURY_DOT_COLOR
        );
    }
}
