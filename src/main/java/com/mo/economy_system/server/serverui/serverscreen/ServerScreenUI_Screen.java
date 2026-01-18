package com.mo.economy_system.server.serverui.serverscreen;

import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthClientSync;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerdata_system.Packet_RequestPlayerStats;
import com.mo.economy_system.network.packets.territory_system.Packet_TerritoryDataRequest;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.playerdata.PlayerDataManager;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import com.mo.economy_system.core.territory_system.Territory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 服务器UI - 虚拟坐标系统
 *
 * 设计原理：
 * 1. 虚拟基准尺寸 640×360（基于 2560×1440 全屏 + GUI缩放4 的内部渲染尺寸）
 * 2. 所有元素按虚拟尺寸设计，运行时自动等比缩放到实际屏幕
 * 3. 保证不同分辨率、不同 GUI 缩放下 UI 显示一致
 *
 * 坐标系统：
 * - 虚拟坐标：设计时使用的 640×360 坐标系
 * - 屏幕坐标：实际渲染到屏幕的像素坐标
 * - uiScale：虚拟坐标到屏幕坐标的缩放比例
 *
 * 保留所有原有特性：动画、美术样式、布局等
 */
@OnlyIn(Dist.CLIENT)
public class ServerScreenUI_Screen extends Screen {

    // ==================== 虚拟基准尺寸 ====================
    // 基准：2560×1440 全屏 + GUI缩放4 → 内部渲染尺寸 640×360
    // 所有 UI 元素按这个尺寸设计，运行时自动缩放
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;

    // ==================== 面板比例 ====================
    private static final float LEFT_PANEL_PERCENT = 0.20f;  // 左侧面板占虚拟宽度的 20%
    private static final float RIGHT_PANEL_PERCENT = 0.45f;  // 右侧面板占虚拟宽度的 35%

    // ==================== 颜色定义 ====================
    private static final int PANEL_BACKGROUND_COLOR = 0x80000000;  // 半透明黑色背景（alpha=128）
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;      // 白色边框

    private static final int BAR_HEALTH_COLOR = 0xFFFF8888;        // 血量条颜色（浅红）
    private static final int BAR_FOOD_COLOR = 0xFFFFCC00;          // 饥饿值颜色（金黄）
    private static final int BAR_STRENGTH_COLOR = 0xFF00DD00;       // 力量值颜色（绿色）
    private static final int BAR_COURAGE_COLOR = 0xFFCC00FF;        // 勇气值颜色（紫色）
    private static final int BAR_INFECTION_COLOR = 0xFF00DD00;      // 感染值颜色（绿色）

    // ==================== 动画时间配置 ====================
    // 开启动画
    private long openTime = 0;                                // UI 打开的时间戳
    private static final long ANIMATION_DURATION = 400;        // 打开边框动画持续时间（毫秒）

    // 关闭动画
    private boolean isClosing = false;                         // 是否正在执行关闭动画
    private long closeTime = 0;                                // UI 开始关闭的时间戳
    private static final long CLOSE_ANIMATION_DURATION = 150;  // 关闭动画持续时间（毫秒）

    // ==================== 虚拟坐标系统变量 ====================
    // 这些变量在 calculateVirtualSize() 中每帧重新计算
    private float uiScale;           // 虚拟坐标到屏幕坐标的缩放比例
    private int virtualWidth;        // 虚拟画布宽度
    private int virtualHeight;       // 虚拟画布高度

    // 面板位置（虚拟坐标）
    private int leftPanelWidth;      // 左栏宽度（虚拟像素）
    private int rightPanelWidth;     // 右栏宽度（虚拟像素）
    private int rightCenterX;        // 右栏中心 X 坐标（虚拟像素）
    private int RIGHT_PANEL_START_X; // 右侧面板起点 X 坐标（虚拟像素）
    private int centerCenterX;       // 中间区域中心 X 坐标（虚拟像素）

    // 玩家模型位置（虚拟坐标）
    private int MODEL_HEIGHT;        // 模型总高度（虚拟像素）
    private int MODEL_SIZE;          // 模型缩放大小（虚拟像素）
    private int MODEL_FOOT_Y;        // 模型脚部 Y 坐标（虚拟像素）
    private int MODEL_HEAD_Y;        // 模型头部 Y 坐标（虚拟像素）

    // 关闭动画滑动距离（虚拟坐标）
    private int LEFT_PANEL_SLIDE_DISTANCE;   // 左面板向左滑动的最大距离
    private int RIGHT_PANEL_SLIDE_DISTANCE;  // 右面板向右滑动的最大距离

    // 金币框可点击区域（虚拟坐标）
    private int goldBoxClickX1, goldBoxClickY1;
    private int goldBoxClickX2, goldBoxClickY2;

    // 领地管理按钮可点击区域（虚拟坐标）
    private int territoryButtonClickX1, territoryButtonClickY1;
    private int territoryButtonClickX2, territoryButtonClickY2;

    private final Minecraft mc = Minecraft.getInstance();

    public ServerScreenUI_Screen() {
        super(Component.literal("服务器界面"));
    }

    @Override
    protected void init() {
        super.init();
        // 记录动画开始时间
        openTime = Util.getMillis();
        // 计算缩放比例
        calculateVirtualSize();
        // 请求领地数据
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TerritoryDataRequest());
        // 请求统计数据（群系 + 配方）
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RequestPlayerStats());
    }

    /**
     * 计算虚拟尺寸和缩放比例
     *
     * 工作流程：
     * 1. 计算屏幕尺寸到虚拟基准尺寸的缩放比例
     * 2. 使用较小的缩放比例保持宽高比（避免拉伸变形）
     * 3. 计算虚拟画布尺寸（实际屏幕 / 缩放比例）
     * 4. 根据虚拟画布计算各元素位置
     *
     * 示例（2560×1440 全屏 + GUI缩放4）：
     *   this.width = 640, this.height = 360
     *   uiScale = 1.0
     *   virtualWidth = 640, virtualHeight = 360
     *
     * 示例（1920×1080 全屏 + GUI缩放2）：
     *   this.width = 960, this.height = 540
     *   uiScale = 1.5
     *   virtualWidth = 640, virtualHeight = 360
     */
    private void calculateVirtualSize() {
        // 计算屏幕尺寸到基准尺寸的缩放比例
        float scaleX = (float) this.width / BASE_WIDTH;    // 宽度缩放比
        float scaleY = (float) this.height / BASE_HEIGHT;  // 高度缩放比

        // 取较小值，确保内容完整显示（可能有黑边，但不会裁剪）
        uiScale = Math.min(scaleX, scaleY);

        // 反向计算虚拟画布尺寸
        // 如果屏幕比基准大，虚拟画布 = 基准尺寸
        // 如果屏幕比基准小，虚拟画布 > 基准尺寸（反向缩放）
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);

        // ==================== 计算面板位置（虚拟坐标） ====================
        // 左栏宽度：虚拟宽度的 20%
        // 例如：virtualWidth = 640 → leftPanelWidth = 128
        leftPanelWidth = (int) (virtualWidth * LEFT_PANEL_PERCENT);

        // 右栏宽度：虚拟宽度的 35%
        // 例如：virtualWidth = 640 → rightPanelWidth = 224
        rightPanelWidth = (int) (virtualWidth * RIGHT_PANEL_PERCENT);

        // 右栏中心 X 坐标：从右边缘向左偏移右栏宽度的一半
        // 例如：virtualWidth=640, rightPanelWidth=224 → rightCenterX = 640 * (1 - 0.175) = 528
        rightCenterX = (int) (virtualWidth * (1.0f - RIGHT_PANEL_PERCENT / 2.0f));

        // 右栏起点 X 坐标：从右边缘向左偏移右栏宽度
        // 例如：virtualWidth=640, rightPanelWidth=224 → RIGHT_PANEL_START_X = 640 * 0.65 = 416
        RIGHT_PANEL_START_X = (int) (virtualWidth * (1.0f - RIGHT_PANEL_PERCENT));

        // 中间区域中心 X 坐标：左栏和右栏之间的区域中心
        // 例如：virtualWidth=640, leftPanelWidth=128, RIGHT_PANEL_START_X=416 → centerCenterX = 272
        centerCenterX = (leftPanelWidth + RIGHT_PANEL_START_X) / 2;

        // ==================== 计算玩家模型位置（虚拟坐标） ====================
        // 模型占据屏幕中间 20% 到 70% 的高度
        // 模型脚部 Y 坐标：虚拟高度的 70%
        MODEL_FOOT_Y = (int) (virtualHeight * 0.75f);
        // 模型头部 Y 坐标：虚拟高度的 20%
        MODEL_HEAD_Y = (int) (virtualHeight * 0.2f);
        // 模型总高度：头到脚的距离
        MODEL_HEIGHT = MODEL_FOOT_Y - MODEL_HEAD_Y;
        // 模型缩放大小：模型高度除以 1.8（renderEntityInInventory 的模型高度系数）
        MODEL_SIZE = (int) (MODEL_HEIGHT / 1.8);

        // ==================== 计算关闭动画滑动距离（虚拟坐标） ====================
        LEFT_PANEL_SLIDE_DISTANCE = (int) (virtualWidth * LEFT_PANEL_PERCENT);    // 左栏宽度
        RIGHT_PANEL_SLIDE_DISTANCE = (int) (virtualWidth * RIGHT_PANEL_PERCENT);  // 右栏宽度
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 检查关闭动画是否完成
        if (isCloseAnimationComplete()) {
            this.onClose();
            return;
        }

        // 每帧重新计算虚拟尺寸（支持窗口大小变化）
        calculateVirtualSize();

        // ==================== 应用全局缩放 ====================
        // 所有后续绘制命令都会被这个缩放影响
        // 虚拟坐标 × uiScale = 屏幕坐标
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        // 绘制所有内容（使用虚拟坐标）
        renderPanels(guiGraphics);

        // 恢复矩阵状态
        guiGraphics.pose().popPose();
    }

    /**
     * 获得动画播放进度（0.0 ~ 1.0）
     *
     * @return 打开时从 0 渐变到 1，关闭时从 1 渐变到 0
     */
    private float getAnimationProgress() {
        if (isClosing) {
            // 关闭动画：从 1 递减到 0
            long elapsed = Util.getMillis() - closeTime;
            float progress = 1.0f - Math.min(1.0f, (float) elapsed / CLOSE_ANIMATION_DURATION);
            return Math.max(0.0f, progress);
        } else {
            // 打开动画：从 0 递增到 1
            long elapsed = Util.getMillis() - openTime;
            return Math.min(1.0f, (float) elapsed / ANIMATION_DURATION);
        }
    }

    /**
     * 绘制左右两栏，全部基于虚拟坐标计算
     *
     * 布局结构：
     * ┌──────────────┬─────────────────────┬─────────────────┐
     * │   左栏 20%   │      中间区域        │    右栏 35%     │
     * │              │                     │                 │
     * │  DreamingFish│  玩家名[幸存者]     │ Rank & Title   │
     * │              │  (模型头上方)        │                 │
     * │  (边框动画)  │   玩家3D模型         │   属性进度条    │
     * │              │   (20%-60%)          │                 │
     * │              │   等级圆+经验值      │                 │
     * │              │   (60%-100%)         │                 │
     * └──────────────┴─────────────────────┴─────────────────┘
     */
    private void renderPanels(GuiGraphics guiGraphics) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 动画进度
        float animationProgress = getAnimationProgress();

        // ==================== 计算关闭动画的横向偏移量（虚拟坐标） ====================
        int leftOffsetX = 0;   // 左栏向左偏移量
        int rightOffsetX = 0;  // 右栏向右偏移量

        if (isClosing) {
            // 关闭时：左栏向左滑出，右栏向右滑出
            float closeProgress = 1.0f - animationProgress;  // 0 → 1
            // 缓动函数：1 - (1 - t)^3，让动画更自然
            closeProgress = 1.0f - (float) Math.pow(1.0f - closeProgress, 3);

            // 计算偏移量（随动画进度增加）
            leftOffsetX = (int) (closeProgress * LEFT_PANEL_SLIDE_DISTANCE);    // 向左
            rightOffsetX = (int) (closeProgress * RIGHT_PANEL_SLIDE_DISTANCE);  // 向右
        }

        // ========================================================================
        //                           左栏 (LEFT PANEL)
        // 内容：DreamingFish 标题 + 边框动画
        // ========================================================================
        guiGraphics.pose().pushPose();

        // 关闭时应用向左偏移
        if (isClosing) {
            guiGraphics.pose().translate(-leftOffsetX, 0, 0);
        }

        // 左侧背景（虚拟坐标：从原点到左栏宽度）
        guiGraphics.fill(RenderType.gui(), 0, 0, leftPanelWidth, virtualHeight, PANEL_BACKGROUND_COLOR);

        // ==================== 左侧右边框动画 ====================
        if (!isClosing) {
            // 打开时：边框从上到下延伸
            // 高度随动画进度从 0 增加到 virtualHeight
            int leftBorderHeight = (int) (virtualHeight * animationProgress);
            if (leftBorderHeight > 0) {
                guiGraphics.fill(RenderType.gui(), leftPanelWidth - 1, 0, leftPanelWidth, leftBorderHeight, PANEL_BORDER_COLOR);
            }
        } else {
            // 关闭时：保持完整边框
            guiGraphics.fill(RenderType.gui(), leftPanelWidth - 1, 0, leftPanelWidth, virtualHeight, PANEL_BORDER_COLOR);
        }

        // ==================== 绘制左侧标题（左下角，带版本号） ====================
        String serverTitle = "§bDreaming§dFish §7v内部0.1";

        // 获取文字原始宽度（受 GUI 缩放影响）
        int titleWidth = mc.font.width(serverTitle);

        // 计算缩放比例：使文字宽度适配左栏宽度的 90%
        float maxWidth = leftPanelWidth * 0.90f;
        float scale = maxWidth / titleWidth;
        // 限制最大缩放
        if (scale > 1.2f) scale = 1.2f;

        // 标题 Y 坐标（左下角）
        int titleY;
        if (!isClosing) {
            // 打开时：从左往右滑入
            float titleAnimDuration = 600f;
            float titleProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / titleAnimDuration);
            titleProgress = 1.0f - (float) Math.pow(1.0f - titleProgress, 3);

            int targetTitleY = virtualHeight - mc.font.lineHeight - 10;  // 距底部 10 像素
            int slideDistance = 60;

            titleY = targetTitleY - (int) ((1.0f - titleProgress) * slideDistance);
        } else {
            titleY = virtualHeight - mc.font.lineHeight - 10;
        }

        // 标题 X 坐标（左栏中心）
        int serverTitleX = leftPanelWidth / 2;

        // 绘制标题（居中）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(serverTitleX, titleY, 0);  // 移动到标题位置
        guiGraphics.pose().scale(scale, scale, 1.0f);           // 缩放文字
        // 向左偏移一半宽度居中
        guiGraphics.drawString(mc.font, serverTitle, -titleWidth / 2, 0, 0xFFFFFF);
        guiGraphics.pose().popPose();

        // 结束左侧面板变换
        guiGraphics.pose().popPose();

        // ========================================================================
        //                           中栏 (CENTER PANEL)
        // 布局：
        // - 模型头部上方：玩家名称 + 幸存者状态
        // - 20%-60%：玩家3D模型
        // - 60%-100%：等级圆 + 经验值
        // ========================================================================
        // 计算中栏内容的动画偏移（与右栏保持一致）
        int centerOffsetY;
        if (!isClosing) {
            float centerAnimDuration = 800f;
            float centerProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / centerAnimDuration);
            centerProgress = 1.0f - (float) Math.pow(1.0f - centerProgress, 3);
            centerOffsetY = (int) ((1.0f - centerProgress) * 100);
        } else {
            centerOffsetY = 0;
        }

        // ==================== 绘制玩家名称 + 幸存者状态（模型头上方） ====================
        // 获取感染值并确定状态
        int infection = PlayerInfectionManager.getCurrentInfectionClient(player);
        String status = infection >= 100 ? "§c感染者" : "§a幸存者";
        String playerName = "§e" + player.getScoreboardName() + " §7[" + status + "§7]";

        // 计算名称文字缩放
        int nameWidthRaw = mc.font.width(playerName);
        // 中间区域宽度 = RIGHT_PANEL_START_X - leftPanelWidth（虚拟宽度的 45%）
        float maxNameWidth = (RIGHT_PANEL_START_X - leftPanelWidth) * 0.30f;
        float nameScale = maxNameWidth / nameWidthRaw;
        // 限制最大缩放
        if (nameScale > 1.8f) nameScale = 1.8f;

        // 名称位置：模型头部上方
        int nameY = MODEL_HEAD_Y - 20 + centerOffsetY;

        // 绘制玩家名称（中间栏居中）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerCenterX, nameY, 0);
        guiGraphics.pose().scale(nameScale, nameScale, 1.0f);
        guiGraphics.drawString(mc.font, playerName, -nameWidthRaw / 2, -mc.font.lineHeight, 0xFFFFFF);
        guiGraphics.pose().popPose();

        // ==================== 绘制玩家模型（20%-60%） ====================
        renderPlayerModel(guiGraphics, centerOffsetY);

        // ==================== 绘制等级圆和经验值（60%-100%区域） ====================
        // 布局：等级圆（左） + 经验值（右），水平排列，垂直居中

        int level = PlayerLevelManager.getPlayerLevelClient(player);
        String levelText = String.valueOf(level);

        // 等级文字缩放
        float levelScale = 2.0f;
        int levelWidthRaw = mc.font.width(levelText);
        int levelWidthScaled = (int) (levelWidthRaw * levelScale);  // 缩放后的宽度
        int levelHeightScaled = (int) (mc.font.lineHeight * levelScale);

        // 圆的半径：根据缩放后的文字大小计算，确保圆完全包裹文字
        int circleRadius = Math.max(levelWidthScaled, levelHeightScaled) / 2 + 8;

        // 整体内容中心 Y 坐标（虚拟高度的 88%）
        int contentCenterY = (int) (virtualHeight * 0.88f) + centerOffsetY;

        // 圆心位置（整体中心的左侧）
        int circleX = centerCenterX - 40;
        int circleY = contentCenterY;

        // 获取经验进度
        float progress = PlayerLevelManager.getExperienceProgressClient(player);

        // 绘制进度圆（背景圆 + 进度弧）
        drawProgressCircle(guiGraphics, circleX, circleY, circleRadius, progress);

        // 绘制等级文字（圆心）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(circleX, circleY, 0);
        guiGraphics.pose().scale(levelScale, levelScale, 1.0f);
        guiGraphics.drawString(mc.font, levelText, -levelWidthRaw / 2, -mc.font.lineHeight / 2, 0xFFFFFF);
        guiGraphics.pose().popPose();

        // ==================== 绘制经验值（圆的右边，垂直居中对齐） ====================
        String expText = "EXP " + PlayerLevelManager.getPlayerExperienceClient(player) + "/" + PlayerLevelManager.getExperienceNeededForNextLevelClient(player);
        int expWidthRaw = mc.font.width(expText);

        // 经验值位置：圆的右边，垂直居中
        int expX = circleX + circleRadius + 12;
        int expY = contentCenterY;

        // 绘制经验值标签（左对齐，垂直居中）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(expX, expY, 0);
        // 文字左对齐，向上偏移半个行高实现垂直居中
        guiGraphics.drawString(mc.font, expText, 0, -mc.font.lineHeight / 2, 0xFFFFAA);
        guiGraphics.pose().popPose();

        // ==================== 绘制经验进度条（经验值下方） ====================
        int barWidth = 80;  // 进度条宽度（虚拟像素）
        int barHeight = 6;  // 进度条高度
        int barX = expX;
        int barY = expY + mc.font.lineHeight / 2 + 8;

        // 绘制经验进度条
        drawProgressBar(guiGraphics, barX, barY, barWidth, barHeight, progress, 0xFFFFAA00);

        // 保存中间栏底部 Y 坐标，供右栏使用
        int centerPanelBottomY = barY + barHeight;

        // ========================================================================
        //                           右栏 (RIGHT PANEL)
        // 内容：圆角矩形框包裹五个横向排列的属性条
        // ========================================================================
        guiGraphics.pose().pushPose();

        // 关闭时应用向右偏移
        if (isClosing) {
            guiGraphics.pose().translate(rightOffsetX, 0, 0);
        }

        // 右侧背景（虚拟坐标：从右栏起点到虚拟宽度）
        guiGraphics.fill(RenderType.gui(), RIGHT_PANEL_START_X, 0, virtualWidth, virtualHeight, PANEL_BACKGROUND_COLOR);

        // ==================== 右侧左边框动画 ====================
        if (!isClosing) {
            // 打开时：边框从下到上延伸
            int rightBorderHeight = (int) (virtualHeight * animationProgress);
            if (rightBorderHeight > 0) {
                // 计算边框起点 Y：从底部向上
                int rightBorderY = virtualHeight - rightBorderHeight;
                guiGraphics.fill(RenderType.gui(), RIGHT_PANEL_START_X, rightBorderY, RIGHT_PANEL_START_X + 1, virtualHeight, PANEL_BORDER_COLOR);
            }
        } else {
            // 关闭时：保持完整边框
            guiGraphics.fill(RenderType.gui(), RIGHT_PANEL_START_X, 0, RIGHT_PANEL_START_X + 1, virtualHeight, PANEL_BORDER_COLOR);
        }

        // ==================== 右侧内容滑入动画 ====================
        int rightOffsetY;
        if (!isClosing) {
            // 打开时：从下往上滑入
            float rightAnimDuration = 800f;
            float rightProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / rightAnimDuration);
            rightProgress = 1.0f - (float) Math.pow(1.0f - rightProgress, 3);
            // 初始偏移 100 虚拟像素，随动画进度减少到 0
            rightOffsetY = (int) ((1.0f - rightProgress) * 100);
        } else {
            // 关闭时：无偏移
            rightOffsetY = 0;
        }

        // ==================== 绘制属性进度条（横向排列） ====================
        renderAttributeBarsHorizontal(guiGraphics, player, RIGHT_PANEL_START_X, rightPanelWidth, rightOffsetY);

        // ==================== 绘制 Rank 和 Title 框（左右排列，属性框下方） ====================
        int boxMargin = 5;
        int innerMargin = 8;
        int lineHeight = mc.font.lineHeight;
        int attrBoxHeight = innerMargin * 2 + lineHeight + 2 + 6 + 3 + lineHeight + 8 + lineHeight;
        int boxSpacing = 8;              // 框之间的间距

        int twoBoxY = boxMargin + attrBoxHeight + boxSpacing + rightOffsetY;
        int twoBoxWidth = (rightPanelWidth - boxMargin * 2 - boxSpacing) / 2;  // 两个框平分宽度

        // Rank 框（左侧）
        int rankBoxX = RIGHT_PANEL_START_X + boxMargin;
        renderRankBox(guiGraphics, player, rankBoxX, twoBoxY, twoBoxWidth);

        // Title 框（右侧）
        int titleBoxX = rankBoxX + twoBoxWidth + boxSpacing;
        renderTitleBox(guiGraphics, player, titleBoxX, twoBoxY, twoBoxWidth);

        // ==================== 绘制金币和领地框（左右排列，Rank/Title框下方） ====================
        int thirdBoxY = twoBoxY + (innerMargin * 2 + lineHeight) + boxSpacing;

        // 金币框（左侧）
        int goldBoxX = RIGHT_PANEL_START_X + boxMargin;
        renderGoldBox(guiGraphics, goldBoxX, thirdBoxY, twoBoxWidth);

        // 领地框（右侧）
        int territoryBoxX = goldBoxX + twoBoxWidth + boxSpacing;
        renderTerritoryBox(guiGraphics, territoryBoxX, thirdBoxY, twoBoxWidth);

        // ==================== 绘制群系和蓝图框（金币/领地框下方，左右排列） ====================
        int fourthBoxY = thirdBoxY + (innerMargin * 2 + lineHeight) + boxSpacing;

        // 群系框（左侧）
        int biomesBoxX = RIGHT_PANEL_START_X + boxMargin;
        renderExplorationStats(guiGraphics, biomesBoxX, fourthBoxY, twoBoxWidth);

        // 蓝图框（右侧）
        int blueprintBoxX = biomesBoxX + twoBoxWidth + boxSpacing;
        renderBlueprintBox(guiGraphics, blueprintBoxX, fourthBoxY, twoBoxWidth);

        // 结束右侧面板变换
        guiGraphics.pose().popPose();
    }

    /**
     * 绘制玩家模型
     * @param offsetY Y 轴动画偏移量（虚拟像素）
     */
    private void renderPlayerModel(GuiGraphics guiGraphics, int offsetY) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, offsetY, 0);  // 应用 Y 轴偏移

        // renderEntityInInventoryFollowsMouse 参数说明：
        // - guiGraphics: 图形上下文
        // - x: 模型中心 X 坐标（虚拟）
        // - y: 模型脚部 Y 坐标（虚拟）
        // - size: 模型缩放大小（虚拟）
        // - mouseX, mouseY: 鼠标跟随（设为 0 表示不跟随）
        // - player: 玩家实体
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, centerCenterX, MODEL_FOOT_Y, MODEL_SIZE, 0, 0, player);

        guiGraphics.pose().popPose();
    }

    /**
     * 绘制进度环（带进度弧）
     * @param centerX 圆心 X 坐标（虚拟像素）
     * @param centerY 圆心 Y 坐标（虚拟像素）
     * @param radius 半径（虚拟像素）
     * @param progress 进度 0.0 ~ 1.0
     */
    private void drawProgressCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, float progress) {
        int segments = 256;  // 圆的分段数（越多越圆滑）
        float step = (float) (2 * Math.PI / segments);
        int borderColor = 0xFFFFFFFF;  // 白色边框
        int progressColor = 0xFFFFAA00;  // 金色进度

        // ==================== 绘制进度弧 ====================
        if (progress > 0) {
            float startAngle = (float) (-Math.PI / 2);  // -90度，12点钟方向
            float endAngle = startAngle + (float) (2 * Math.PI * progress);  // 根据进度计算结束角度
            int arcRadius = radius - 3;  // 进度弧比边框稍小

            // 计算需要绘制的段数
            int segmentsToDraw = (int) (segments * progress);
            if (segmentsToDraw < 1) segmentsToDraw = 1;

            // 起始点
            int lastX = centerX + (int) (arcRadius * Math.cos(startAngle));
            int lastY = centerY + (int) (arcRadius * Math.sin(startAngle));

            // 逐段绘制进度弧
            for (int i = 1; i <= segmentsToDraw; i++) {
                float angle = startAngle + (endAngle - startAngle) * i / segmentsToDraw;
                int x = centerX + (int) Math.round(arcRadius * Math.cos(angle));
                int y = centerY + (int) Math.round(arcRadius * Math.sin(angle));

                guiGraphics.hLine(lastX, x, lastY, progressColor);
                guiGraphics.vLine(x, lastY, y, progressColor);

                lastX = x;
                lastY = y;
            }
        }

        // ==================== 绘制白色外圈边框 ====================
        int lastX = centerX + (int) (radius * Math.cos(0));
        int lastY = centerY + (int) (radius * Math.sin(0));

        for (int i = 1; i <= segments; i++) {
            float angle = i * step;
            int x = centerX + (int) Math.round(radius * Math.cos(angle));
            int y = centerY + (int) Math.round(radius * Math.sin(angle));

            guiGraphics.hLine(lastX, x, lastY, borderColor);
            guiGraphics.vLine(x, lastY, y, borderColor);

            lastX = x;
            lastY = y;
        }
    }

    /**
     * 绘制横向进度条（带发光效果）
     * @param x 左上角 X 坐标（虚拟像素）
     * @param y 左上角 Y 坐标（虚拟像素）
     * @param width 宽度（虚拟像素）
     * @param height 高度（虚拟像素）
     * @param pct 进度 0.0 ~ 1.0
     * @param color 进度条颜色
     */
    private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float pct, int color) {
        // 外发光效果（半透明阴影）
        int glowColor = 0x40000000 | (color & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x - 1, y - 1, x + width + 1, y + height + 1, glowColor);

        // 背景（半透明白色）
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, 0x80FFFFFF);

        // ==================== 绘制前景（进度） ====================
        int progressWidth = (int) (width * Math.max(0, Math.min(1, pct)));
        if (progressWidth > 2) {
            // 深色进度
            int deepColor = 0xFF000000 | (color & 0x00FFFFFF);
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + progressWidth - 1, y + height - 1, deepColor);

            // 顶部高光（增加立体感）
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + progressWidth - 1, y + Math.min(3, height / 2), 0x60FFFFFF);
        }

        // ==================== 绘制白色边框 ====================
        int borderColor = 0xFFFFFFFF;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);              // 上边框
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, borderColor);  // 下边框
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, borderColor);              // 左边框
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, borderColor);  // 右边框
    }

    /**
     * 渲染五个属性进度条（圆角矩形框包裹，五个竖排）
     *
     * 布局示意：
     * ┌─────────────────────────────────────┐
     * │  ┌─────────────────────────────┐   │
     * │  │ ❤ 20/20  ▓▓▓▓▓▓▓▓▓           │   │
     * │  │ 🍖 20/20  ▓▓▓▓▓▓▓▓▓           │   │
     * │  │ 💪 100/100 ▓▓▓▓▓▓▓▓▓          │   │
     * │  │ ⚡ 50/100  ▓▓▓▓▓              │   │
     * │  │ ☣ 0/100   ▓▓▓▓▓▓▓▓▓           │   │
     * │  └─────────────────────────────┘   │
     * └─────────────────────────────────────┘
     *
     * @param guiGraphics 图形上下文
     * @param player 当前玩家
     * @param rightPanelX 右侧面板起点 X 坐标（虚拟像素）
     * @param rightPanelWidth 右侧面板宽度（虚拟像素）
     * @param offsetY Y 轴动画偏移量（虚拟像素）
     */
    private void renderAttributeBars(GuiGraphics guiGraphics, LocalPlayer player, int rightPanelX, int rightPanelWidth, int offsetY) {
        // ==================== 获取玩家属性值 ====================
        float healthPercent = player.getHealth() / player.getMaxHealth();
        float foodPercent = (float) player.getFoodData().getFoodLevel() / 20.0f;

        int strength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
        int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;
        float strengthPercent = (float) strength / maxStrength;

        float courage = PlayerCourageManager.getCurrentCourageClient(player);
        float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
        if (maxCourage <= 0) maxCourage = 100;
        float couragePercent = courage / maxCourage;

        float infectionPercent = (float) PlayerInfectionManager.getCurrentInfectionClient(player) / 100.0f;

        // ==================== 准备进度条数据 ====================
        String[] icons = {"❤", "🍖", "💪", "⚡", "☣"};
        int[] colors = {BAR_HEALTH_COLOR, BAR_FOOD_COLOR, BAR_STRENGTH_COLOR, BAR_COURAGE_COLOR, BAR_INFECTION_COLOR};
        float[] percents = {healthPercent, foodPercent, strengthPercent, couragePercent, infectionPercent};
        String[] values = {
                String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth()),
                String.format("%d/20", player.getFoodData().getFoodLevel()),
                String.format("%d/%d", strength, maxStrength),
                String.format("%.0f/%.0f", courage, maxCourage),
                String.format("%d/100", PlayerInfectionManager.getCurrentInfectionClient(player))
        };

        // ==================== 布局参数（虚拟像素） ====================
        int boxMargin = 5;              // 框距离面板边缘的边距
        int boxWidth = rightPanelWidth - boxMargin * 2;  // 框宽度
        int innerMargin = 10;           // 框内边距
        int itemSpacing = 13;           // 每个属性条之间的垂直间距
        int itemCount = 5;              // 属性条数量
        int extraPadding = 4;           // 额外的顶部/底部内边距
        int cornerRadius = 12;           // 圆角半径
        int barHeight = 7;               // 进度条高度
        int valueWidth = 45;             // 数值文本宽度
        int iconWidth = 12;              // 图标宽度
        int barWidth = boxWidth - innerMargin * 2 - 10;  // 进度条宽度（图标 + 进度条 + 数值）
        int progressWidth = barWidth - iconWidth - valueWidth - 8 - 10;  // 单进度条宽度

        int boxX = rightPanelX + boxMargin;  // 框 X 坐标
        int startY = boxMargin + innerMargin + extraPadding + offsetY;  // 起始 Y 坐标

        // ==================== 绘制五个竖向排列的属性条 ====================
        for (int i = 0; i < itemCount; i++) {
            int itemY = startY + i * itemSpacing;

            // 感染值条使用动态颜色
            int barColor;
            if (i == 4) {
                barColor = getInfectionColor(infectionPercent);
            } else {
                barColor = colors[i];
            }

            // 绘制图标
            int iconX = boxX + innerMargin;
            int iconY = itemY - mc.font.lineHeight / 2;
            guiGraphics.drawString(mc.font, icons[i], iconX, iconY, barColor);

            // 绘制进度条
            int barX = iconX + iconWidth + 4;
            int barY = itemY - barHeight / 2;
            drawProgressBar(guiGraphics, barX, barY, progressWidth, barHeight, percents[i], barColor);

            // 绘制数值（右对齐）
            int valueX = boxX + innerMargin + barWidth - valueWidth;
            int valueY = itemY - mc.font.lineHeight / 2;
            guiGraphics.drawString(mc.font, values[i], valueX, valueY, 0xFFFFFFFF);
        }

        // ==================== 绘制圆角矩形框（包裹内容，最后绘制） ====================
        int boxHeight = (itemCount - 1) * itemSpacing + innerMargin * 2 + extraPadding * 2;
        int boxY = boxMargin + offsetY;  // 框 Y 坐标
        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, cornerRadius, 0x40FFAAAA, 0xFFFFFFFF);
    }

    /**
     * 渲染五个属性进度条（横向排列）
     *
     * 布局示意：
     * ┌─────────────────────────────────────────────────────┐
     * │ ❤ ▓▓▓▓  🍖 ▓▓▓▓  💪 ▓▓▓▓  ⚡ ▓▓▓▓  ☣ ▓▓▓▓        │
     * │ 20/20    20/20   100/100  50/100   0/100           │
     * └─────────────────────────────────────────────────────┘
     *
     * @param guiGraphics 图形上下文
     * @param player 当前玩家
     * @param rightPanelX 右侧面板起点 X 坐标（虚拟像素）
     * @param rightPanelWidth 右侧面板宽度（虚拟像素）
     * @param offsetY Y 轴动画偏移量（虚拟像素）
     */
    private void renderAttributeBarsHorizontal(GuiGraphics guiGraphics, LocalPlayer player, int rightPanelX, int rightPanelWidth, int offsetY) {
        // ==================== 获取玩家属性值 ====================
        float healthPercent = player.getHealth() / player.getMaxHealth();
        float foodPercent = (float) player.getFoodData().getFoodLevel() / 20.0f;

        int strength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
        int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;
        float strengthPercent = (float) strength / maxStrength;

        float courage = PlayerCourageManager.getCurrentCourageClient(player);
        float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
        if (maxCourage <= 0) maxCourage = 100;
        float couragePercent = courage / maxCourage;

        float infectionPercent = (float) PlayerInfectionManager.getCurrentInfectionClient(player) / 100.0f;

        // ==================== 准备进度条数据 ====================
        String[] icons = {"❤", "🍖", "💪", "⚡", "☣"};
        int[] colors = {BAR_HEALTH_COLOR, BAR_FOOD_COLOR, BAR_STRENGTH_COLOR, BAR_COURAGE_COLOR, BAR_INFECTION_COLOR};
        float[] percents = {healthPercent, foodPercent, strengthPercent, couragePercent, infectionPercent};
        String[] values = {
                String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth()),
                String.format("%d/20", player.getFoodData().getFoodLevel()),
                String.format("%d/%d", strength, maxStrength),
                String.format("%.0f/%.0f", courage, maxCourage),
                String.format("%d/100", PlayerInfectionManager.getCurrentInfectionClient(player))
        };

        // ==================== 布局参数（虚拟像素） ====================
        int boxMargin = 5;               // 框距离面板边缘的边距
        int boxWidth = rightPanelWidth - boxMargin * 2;  // 框宽度
        int innerMargin = 8;             // 框内边距
        int itemCount = 5;               // 属性条数量
        int itemSpacing = 8;             // 每个属性条之间的水平间距
        int extraPadding = 4;            // 额外的左右内边距
        int cornerRadius = 12;           // 圆角半径
        int barHeight = 6;               // 进度条高度
        int lineHeight = mc.font.lineHeight;  // 文本行高度
        // 框高度 = 图标 + 间距 + 进度条 + 间距 + 文本 + 提示文字间距 + 提示文字(1行) + 内边距
        int boxHeight = innerMargin * 2 + lineHeight + 2 + barHeight + 3 + lineHeight + 8 + lineHeight;

        int boxX = rightPanelX + boxMargin;  // 框 X 坐标
        int boxY = boxMargin + offsetY;      // 框 Y 坐标

        // 计算每个属性条的宽度
        int totalSpacing = (itemCount - 1) * itemSpacing;
        int itemWidth = (boxWidth - innerMargin * 2 - extraPadding * 2 - totalSpacing) / itemCount;

        int startX = boxX + innerMargin + extraPadding;  // 起始 X 坐标
        int iconY = boxY + innerMargin;                   // 图标 Y 坐标（在框内顶部）
        int barY = iconY + mc.font.lineHeight + 2;        // 进度条 Y 坐标（图标下方 + 间距）
        int textY = barY + barHeight + 3;                 // 文本 Y 坐标（进度条下方 + 间距）

        // ==================== 绘制五个横向排列的属性条 ====================
        for (int i = 0; i < itemCount; i++) {
            int itemX = startX + i * (itemWidth + itemSpacing);

            // 感染值条使用动态颜色
            int barColor;
            if (i == 4) {
                barColor = getInfectionColor(infectionPercent);
            } else {
                barColor = colors[i];
            }

            // 绘制图标（居中）
            int iconX = itemX + itemWidth / 2 - mc.font.width(icons[i]) / 2;
            guiGraphics.drawString(mc.font, icons[i], iconX, iconY, barColor);

            // 绘制进度条
            int barX = itemX;
            drawProgressBar(guiGraphics, barX, barY, itemWidth, barHeight, percents[i], barColor);

            // 绘制数值（居中）
            int valueX = itemX + itemWidth / 2 - mc.font.width(values[i]) / 2;
            guiGraphics.drawString(mc.font, values[i], valueX, textY, 0xFFFFFFFF);
        }

        // ==================== 绘制提示文字（属性条下方） ====================
        int tipY = textY + lineHeight + 8;  // 提示文字 Y 坐标
        String tipText = "属性与您的等级密切相关，提升等级可以提高您的属性";

        // 绘制提示文字（居中，灰色）
        int tipX = boxX + boxWidth / 2 - mc.font.width(tipText) / 2;
        guiGraphics.drawString(mc.font, tipText, tipX, tipY, 0xFFAAAAAA);

        // ==================== 绘制圆角矩形框（包裹内容，最后绘制） ====================
        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, cornerRadius, 0x40FFAAAA, 0xFFFFFFFF);
    }

    /**
     * 渲染 Rank 框（属性框下方，左侧）
     *
     * 布局示意：
     * ┌──────────────────────┐
     * │  🏆 Rank: FISH       │
     * │  Rank是服务器玩家  │
     * │  的一种特殊身份... │
     * └──────────────────────┘
     *
     * @param guiGraphics 图形上下文
     * @param player 当前玩家
     * @param boxX 框 X 坐标（虚拟像素）
     * @param boxY 框 Y 坐标（虚拟像素）
     * @param boxWidth 框宽度（虚拟像素）
     */
    private void renderRankBox(GuiGraphics guiGraphics, LocalPlayer player, int boxX, int boxY, int boxWidth) {
        // ==================== 获取 Rank ====================
        Rank rank = PlayerRankManager.getPlayerRankClient(player);
        int rankColor = getRankColor(rank.getRankLevel());

        // ==================== 布局参数（虚拟像素） ====================
        int innerMargin = 8;             // 框内边距
        int cornerRadius = 12;           // 圆角半径
        int lineHeight = mc.font.lineHeight;  // 文本行高度

        // ==================== 计算内容高度 ====================
        // 内容高度 = 单行高度 + 内边距 * 2
        int boxHeight = innerMargin * 2 + lineHeight;

        // ==================== 绘制框（白色边框，半透明填充） ====================
        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0, 0x40AAAAFF, 0xFFFFFFFF);

        // ==================== 绘制内容 ====================
        // 左侧：Rank 名称
        String rankText = "🏆 " + rank.getRankName();
        guiGraphics.drawString(mc.font, rankText, boxX + innerMargin, boxY + innerMargin, rankColor);

        // 右侧：您的RANK（右对齐）
        String yourRankText = "- 您的RANK";
        int yourRankWidth = mc.font.width(yourRankText);
        guiGraphics.drawString(mc.font, yourRankText, boxX + boxWidth - innerMargin - yourRankWidth, boxY + innerMargin, 0xFFAAAAFF);
    }

    /**
     * 渲染 Title 框（属性框下方，右侧）
     *
     * 布局示意：
     * ┌──────────────────────┐
     * │  ⭐ 称号名称         │
     * │  称号可以让玩家拥有│
     * │  聊天前缀...        │
     * └──────────────────────┘
     *
     * @param guiGraphics 图形上下文
     * @param player 当前玩家
     * @param boxX 框 X 坐标（虚拟像素）
     * @param boxY 框 Y 坐标（虚拟像素）
     * @param boxWidth 框宽度（虚拟像素）
     */
    private void renderTitleBox(GuiGraphics guiGraphics, LocalPlayer player, int boxX, int boxY, int boxWidth) {
        // ==================== 获取 Title ====================
        Title title = PlayerTitleManager.getPlayerTitleClient(player);

        // ==================== 布局参数（虚拟像素） ====================
        int innerMargin = 8;             // 框内边距
        int cornerRadius = 12;           // 圆角半径
        int lineHeight = mc.font.lineHeight;  // 文本行高度
        int titleColor = 0xFF000000 | title.getColor();

        // ==================== 计算内容高度 ====================
        // 内容高度 = 单行高度 + 内边距 * 2
        int boxHeight = innerMargin * 2 + lineHeight;

        // ==================== 绘制框（白色边框，半透明填充） ====================
        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0, 0x40FFAAFF, 0xFFFFFFFF);

        // ==================== 绘制内容 ====================
        // 左侧：Title 名称
        String titleText = "⭐ " + title.getTitleName();
        guiGraphics.drawString(mc.font, titleText, boxX + innerMargin, boxY + innerMargin, titleColor);

        // 右侧：您的称号（右对齐）
        String yourTitleText = "- 您的称号";
        int yourTitleWidth = mc.font.width(yourTitleText);
        guiGraphics.drawString(mc.font, yourTitleText, boxX + boxWidth - innerMargin - yourTitleWidth, boxY + innerMargin, 0xFFFFAAFF);
    }

    /**
     * 渲染金币框
     *
     * @param guiGraphics 图形上下文
     * @param boxX 框 X 坐标（虚拟像素）
     * @param boxY 框 Y 坐标（虚拟像素）
     * @param boxWidth 框宽度（虚拟像素）
     */
    private void renderGoldBox(GuiGraphics guiGraphics, int boxX, int boxY, int boxWidth) {
        int innerMargin = 8;
        int cornerRadius = 12;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        // 获取金币余额
        int goldBalance = ServerInformationDisplay.PLAYER_BALANCE;

        // 绘制框（白色边框，半透明填充）
        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0, 0x40FFD700, 0xFFFFFFFF);

        // 左侧：金币图标和数量
        String goldText = "💰 " + formatNumber(goldBalance);
        guiGraphics.drawString(mc.font, goldText, boxX + innerMargin, boxY + innerMargin, 0xFFFFD700);

        // 右侧：梦鱼币（右对齐）
        String yourGoldText = "- 梦鱼币";
        int goldLabelWidth = mc.font.width(yourGoldText);
        guiGraphics.drawString(mc.font, yourGoldText, boxX + boxWidth - innerMargin - goldLabelWidth, boxY + innerMargin, 0xFFFFD700);

        // 存储整个框的可点击区域
        goldBoxClickX1 = boxX;
        goldBoxClickY1 = boxY;
        goldBoxClickX2 = boxX + boxWidth;
        goldBoxClickY2 = boxY + boxHeight;
    }

    /**
     * 渲染领地框
     *
     * @param guiGraphics 图形上下文
     * @param boxX 框 X 坐标（虚拟像素）
     * @param boxY 框 Y 坐标（虚拟像素）
     * @param boxWidth 框宽度（虚拟像素）
     */
    private void renderTerritoryBox(GuiGraphics guiGraphics, int boxX, int boxY, int boxWidth) {
        int innerMargin = 8;
        int cornerRadius = 12;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        // 获取领地列表
        java.util.List<Territory> territories = ServerInformationDisplay.PLAYER_TERRITORIES;

        // 绘制框（白色边框，半透明填充）
        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0, 0x40FF8C00, 0xFFFFFFFF);

        if (territories.isEmpty()) {
            // 无领地
            guiGraphics.drawString(mc.font, "🏰 0 个领地", boxX + innerMargin, boxY + innerMargin, 0xFFAAAAAA);

            String manageText = "- 管理您的领地";
            int manageWidth = mc.font.width(manageText);
            int manageX = boxX + boxWidth - innerMargin - manageWidth;
            guiGraphics.drawString(mc.font, manageText, manageX, boxY + innerMargin, 0xFFFF8C00);
        } else {
            // 显示领地数量
            String territoryText = "🏰 " + territories.size() + " 个领地";
            guiGraphics.drawString(mc.font, territoryText, boxX + innerMargin, boxY + innerMargin, 0xFFFF8C00);

            String manageText = "- 管理您的领地";
            int manageWidth = mc.font.width(manageText);
            int manageX = boxX + boxWidth - innerMargin - manageWidth;
            guiGraphics.drawString(mc.font, manageText, manageX, boxY + innerMargin, 0xFFFF8C00);
        }

        // 存储整个框的可点击区域
        territoryButtonClickX1 = boxX;
        territoryButtonClickY1 = boxY;
        territoryButtonClickX2 = boxX + boxWidth;
        territoryButtonClickY2 = boxY + boxHeight;
    }

    /**
     * 格式化数字（添加千分位分隔符）
     */
    private String formatNumber(int num) {
        return String.format("%,d", num);
    }

    /**
     * 渲染群系框
     */
    private void renderExplorationStats(GuiGraphics guiGraphics, int boxX, int boxY, int boxWidth) {
        int innerMargin = 8;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        int biomesCount = ServerInformationDisplay.EXPLORED_BIOMES_COUNT;

        // 绘制框（白色边框，青色半透明填充）
        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0, 0x4000DDFF, 0xFFFFFFFF);

        // 左侧：群系数量
        String biomesText = "🗺️ " + biomesCount;
        guiGraphics.drawString(mc.font, biomesText, boxX + innerMargin, boxY + innerMargin, 0xFF00DDFF);

        // 右侧：已探索群系（右对齐）
        String biomesDesc = "- 已探索群系";
        int biomesDescWidth = mc.font.width(biomesDesc);
        guiGraphics.drawString(mc.font, biomesDesc, boxX + boxWidth - innerMargin - biomesDescWidth, boxY + innerMargin, 0xFF00DDFF);
    }

    /**
     * 渲染蓝图框
     */
    private void renderBlueprintBox(GuiGraphics guiGraphics, int boxX, int boxY, int boxWidth) {
        int innerMargin = 8;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        int blueprintCount = ServerInformationDisplay.UNLOCKED_RECIPES_COUNT;

        // 绘制框（白色边框，淡紫色半透明填充）
        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, 0, 0x40DDAAFF, 0xFFFFFFFF);

        // 左侧：蓝图数量
        String blueprintText = "📜 " + blueprintCount;
        guiGraphics.drawString(mc.font, blueprintText, boxX + innerMargin, boxY + innerMargin, 0xFFDDAAFF);

        // 右侧：已解锁蓝图（右对齐）
        String blueprintDesc = "- 已解锁蓝图";
        int blueprintDescWidth = mc.font.width(blueprintDesc);
        guiGraphics.drawString(mc.font, blueprintDesc, boxX + boxWidth - innerMargin - blueprintDescWidth, boxY + innerMargin, 0xFFDDAAFF);
    }

    /**
     * 将文字按指定宽度换行
     * @param text 原始文字
     * @param maxWidth 最大宽度
     * @return 换行后的文字数组
     */
    private String[] wrapText(String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String remaining = text;

        while (!remaining.isEmpty()) {
            // 如果剩余文字能在一行显示，直接添加
            if (mc.font.width(remaining) <= maxWidth) {
                lines.add(remaining);
                break;
            }

            // 找到能放入当前行的最大字符数
            int maxChars = 0;
            for (int i = 1; i <= remaining.length(); i++) {
                if (mc.font.width(remaining.substring(0, i)) > maxWidth) {
                    maxChars = i - 1;
                    break;
                }
            }
            if (maxChars == 0) maxChars = 1;

            // 尝试在空格处换行
            String line = remaining.substring(0, maxChars);
            int lastSpace = line.lastIndexOf(' ');
            if (lastSpace > 0) {
                line = line.substring(0, lastSpace);
                maxChars = lastSpace + 1;  // 跳过空格
            }

            lines.add(line);
            remaining = remaining.substring(maxChars);
        }

        return lines.toArray(new String[0]);
    }

    /**
     * 绘制圆角矩形
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @param radius 圆角半径
     * @param fillColor 填充颜色（ARGB）
     * @param borderColor 边框颜色（ARGB）
     */
    private void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        // 绘制填充
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + height, fillColor);
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + width, y + height - radius, fillColor);
        // 四个圆角
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + radius, y + radius + radius, fillColor);
        guiGraphics.fill(RenderType.gui(), x + width - radius, y + radius, x + width, y + radius + radius, fillColor);
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + radius, fillColor);
        guiGraphics.fill(RenderType.gui(), x + radius, y + height - radius, x + width - radius, y + height, fillColor);

        // 绘制边框（使用四个线条）
        int borderWidth = 1;
        // 上边
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + borderWidth, borderColor);
        // 下边
        guiGraphics.fill(RenderType.gui(), x + radius, y + height - borderWidth, x + width - radius, y + height, borderColor);
        // 左边
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + borderWidth, y + height - radius, borderColor);
        // 右边
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y + radius, x + width, y + height - radius, borderColor);
        // 四个圆角边框（简化为小方块）
        guiGraphics.fill(RenderType.gui(), x, y, x + radius, y + borderWidth, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + borderWidth, y + radius, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - radius, y, x + width, y + borderWidth, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y, x + width, y + radius, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - borderWidth, x + radius, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - radius, x + borderWidth, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - radius, y + height - borderWidth, x + width, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y + height - radius, x + width, y + height, borderColor);
    }

    /**
     * 绘制带直角边框的矩形
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @param radius 圆角半径（未使用，保留参数兼容性）
     * @param fillColor 填充颜色（ARGB）
     * @param borderColor 边框颜色（ARGB）
     */
    private void drawRoundedRectOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        // 绘制填充（整个矩形）
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, fillColor);

        // 绘制直角边框
        int borderWidth = 1;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + borderWidth, borderColor);                    // 上边
        guiGraphics.fill(RenderType.gui(), x, y + height - borderWidth, x + width, y + height, borderColor);  // 下边
        guiGraphics.fill(RenderType.gui(), x, y, x + borderWidth, y + height, borderColor);                  // 左边
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y, x + width, y + height, borderColor);  // 右边
    }

    /**
     * 绘制渐变梦幻色框
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @param gradientType 渐变类型：0=粉紫蓝, 1=蓝青绿, 2=金橙, 3=橙红
     */
    private void drawGradientBox(GuiGraphics guiGraphics, int x, int y, int width, int height, int gradientType) {
        // 逐像素绘制渐变
        for (int i = 0; i < width; i++) {
            float ratio = (float) i / width;
            int color = getGradientColor(gradientType, ratio);

            // 每次画1像素宽的竖线
            guiGraphics.fill(RenderType.gui(), x + i, y, x + i + 1, y + height, color);
        }

        // 绘制白色边框
        int borderWidth = 1;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + borderWidth, 0xFFFFFFFF);
        guiGraphics.fill(RenderType.gui(), x, y + height - borderWidth, x + width, y + height, 0xFFFFFFFF);
        guiGraphics.fill(RenderType.gui(), x, y, x + borderWidth, y + height, 0xFFFFFFFF);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y, x + width, y + height, 0xFFFFFFFF);
    }

    /**
     * 获取渐变色
     * @param type 渐变类型
     * @param ratio 0.0 ~ 1.0
     * @return ARGB 颜色值
     */
    private int getGradientColor(int type, float ratio) {
        int r, g, b;

        switch (type) {
            case 0: // 粉紫蓝渐变 (Rank框)
                // 粉色(255, 182, 193) -> 紫色(186, 85, 211) -> 蓝色(138, 43, 226)
                if (ratio < 0.5f) {
                    float t = ratio * 2;
                    r = (int) (255 + (186 - 255) * t);
                    g = (int) (182 + (85 - 182) * t);
                    b = (int) (193 + (211 - 193) * t);
                } else {
                    float t = (ratio - 0.5f) * 2;
                    r = (int) (186 + (138 - 186) * t);
                    g = (int) (85 + (43 - 85) * t);
                    b = (int) (211 + (226 - 211) * t);
                }
                break;
            case 1: // 粉紫渐变 (Title框)
                // 粉色(255, 182, 193) -> 紫色(186, 85, 211)
                r = (int) (255 + (186 - 255) * ratio);
                g = (int) (182 + (85 - 182) * ratio);
                b = (int) (193 + (211 - 193) * ratio);
                break;
            case 2: // 金橙渐变 (金币框)
                // 金色(255, 215, 0) -> 橙色(255, 140, 0)
                r = 255;
                g = (int) (215 + (140 - 215) * ratio);
                b = 0;
                break;
            case 3: // 橙红渐变 (领地框)
                // 橙色(255, 140, 0) -> 红色(255, 69, 0)
                r = 255;
                g = (int) (140 + (69 - 140) * ratio);
                b = 0;
                break;
            default:
                r = g = b = 255;
        }

        return 0x80000000 | (r << 16) | (g << 8) | b; // 半透明
    }

    /**
     * 绘制圆角进度条
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @param pct 进度 0.0 ~ 1.0
     * @param color 进度条颜色
     */
    private void drawRoundedProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float pct, int color) {
        int radius = Math.min(height / 2, 4);  // 圆角半径

        // 背景（半透明白色）
        int bgColor = 0x60FFFFFF;
        drawRoundedRect(guiGraphics, x, y, width, height, radius, bgColor, bgColor);

        // 进度
        int progressWidth = (int) (width * Math.max(0, Math.min(1, pct)));
        if (progressWidth > radius * 2) {
            int progressColor = 0xFF000000 | (color & 0x00FFFFFF);
            drawRoundedRect(guiGraphics, x, y, progressWidth, height, radius, progressColor, progressColor);
        }
    }

    /**
     * 根据 Rank 等级获取对应颜色
     * @param rankLevel Rank 等级（0-4）
     * @return ARGB 颜色值
     */
    private int getRankColor(int rankLevel) {
        return switch (rankLevel) {
            case 0 -> 0xFF888888;  // NO_RANK - 灰色
            case 1 -> 0xFF00AAFF;  // FISH - 蓝色
            case 2 -> 0xFF00FFFF;  // FISH+ - 青色
            case 3 -> 0xFFFFD700;  // FISH++ - 金色
            case 4 -> 0xFFFF0000;  // OPERATOR - 红色
            default -> 0xFF888888;
        };
    }

    /**
     * 根据感染值百分比计算动态颜色
     * 感染值越高，颜色越深
     *
     * 颜色渐变：
     * - 0%：浅绿色 (0xBBFFBB)
     * - 50%：中等绿色 (0x00DD00)
     * - 100%：深绿色 (0x003300)
     *
     * @param infectionPercent 感染值百分比（0.0 ~ 1.0）
     * @return ARGB 颜色值
     */
    private int getInfectionColor(float infectionPercent) {
        // 限制范围在 0.0 ~ 1.0
        float t = Math.max(0.0f, Math.min(1.0f, infectionPercent));

        // RGB 渐变计算
        // R: 187 (0xBB) → 0
        // G: 255 (0xFF) → 221 → 51 (0x33)
        // B: 187 (0xBB) → 0

        // 使用二次函数让颜色变化更明显（感染值高时颜色加深更快）
        float factor = t * t;  // 二次缓动

        int r = (int) (187 * (1.0f - factor));           // 187 → 0
        int g = (int) (255 - (255 - 51) * factor);        // 255 → 51
        int b = (int) (187 * (1.0f - factor));           // 187 → 0

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * 检查关闭动画是否完成
     * @return true 如果关闭动画已完成
     */
    private boolean isCloseAnimationComplete() {
        if (!isClosing) return false;
        long elapsed = Util.getMillis() - closeTime;
        return elapsed >= CLOSE_ANIMATION_DURATION;
    }

    // ==================== 键盘事件处理 ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC 键（key code 256）关闭 UI
        if (keyCode == 256) {
            if (isClosing) return true;  // 如果已经在关闭中，不再响应
            isClosing = true;
            closeTime = Util.getMillis();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 将屏幕坐标转换为虚拟坐标
        double virtualMouseX = mouseX / uiScale;
        double virtualMouseY = mouseY / uiScale;

        // 计算右侧面板偏移（考虑动画）
        int rightOffsetY = 0;
        if (!isClosing) {
            float rightAnimDuration = 800f;
            float rightProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / rightAnimDuration);
            rightProgress = 1.0f - (float) Math.pow(1.0f - rightProgress, 3);
            rightOffsetY = (int) ((1.0f - rightProgress) * 100);
        }

        // 检查是否点击了金币框
        if (virtualMouseX >= goldBoxClickX1 && virtualMouseX <= goldBoxClickX2 &&
            virtualMouseY >= goldBoxClickY1 + rightOffsetY && virtualMouseY <= goldBoxClickY2 + rightOffsetY) {
            // 打开商店界面（经济系统）
            mc.setScreen(new com.mo.economy_system.screen.economy_system.shop.Screen_Shop());
            return true;
        }

        // 检查是否点击了领地框
        if (virtualMouseX >= territoryButtonClickX1 && virtualMouseX <= territoryButtonClickX2 &&
            virtualMouseY >= territoryButtonClickY1 + rightOffsetY && virtualMouseY <= territoryButtonClickY2 + rightOffsetY) {
            // 打开领地管理界面
            mc.setScreen(new com.mo.economy_system.screen.territory_system.Screen_Territory());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        // 使用 toggleUI 来正确处理关闭逻辑
        if (ServerScreenUI.isShowUI()) {
            ServerScreenUI.toggleUI();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;  // 不暂停游戏
    }
}
