package com.mo.economy_system.server.serverui.serverscreen;

import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthClientSync;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.playerdata.PlayerDataManager;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
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
    private static final float RIGHT_PANEL_PERCENT = 0.35f;  // 右侧面板占虚拟宽度的 35%

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

    // 玩家模型位置（虚拟坐标）
    private int MODEL_HEIGHT;        // 模型总高度（虚拟像素）
    private int MODEL_SIZE;          // 模型缩放大小（虚拟像素）
    private int MODEL_FOOT_Y;        // 模型脚部 Y 坐标（虚拟像素）

    // 关闭动画滑动距离（虚拟坐标）
    private int LEFT_PANEL_SLIDE_DISTANCE;   // 左面板向左滑动的最大距离
    private int RIGHT_PANEL_SLIDE_DISTANCE;  // 右面板向右滑动的最大距离

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

        // ==================== 计算玩家模型位置（虚拟坐标） ====================
        // 模型高度：虚拟高度的 1/4
        // 例如：virtualHeight = 360 → MODEL_HEIGHT = 90
        MODEL_HEIGHT = virtualHeight / 4;

        // 模型大小：模型高度除以 1.8（renderEntityInInventory 的模型高度系数）
        MODEL_SIZE = (int) (MODEL_HEIGHT / 1.8);

        // 模型脚部 Y 坐标：顶部微小偏移 + 模型高度
        // 例如：virtualHeight = 360 → MODEL_FOOT_Y = 7.2 + 90 ≈ 97
        MODEL_FOOT_Y = virtualHeight / 50 + MODEL_HEIGHT;

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
     * │  DreamingFish│                     │  玩家3D模型      │
     * │              │                     │  ⭐             │
     * │  (边框动画)  │                     │  等级圆(进度)   │
     * │              │                     │  经验值         │
     * │              │                     │  玩家名称+状态   │
     * │              │                     │  Rank & Title   │
     * │              │                     │  属性进度条     │
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

        // ==================== 绘制左侧面板 ====================
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

        // ==================== 绘制左侧标题（带滑入动画） ====================
        String serverTitle = "§bDreaming§dFish";

        // 获取文字原始宽度（受 GUI 缩放影响）
        int titleWidth = mc.font.width(serverTitle);

        // 计算缩放比例：使文字宽度适配左栏宽度的 85%
        float maxWidth = leftPanelWidth * 0.85f;
        float scale = maxWidth / titleWidth;

        // 标题 Y 坐标（带滑入动画）
        int titleY;
        if (!isClosing) {
            // 打开时：从上往下滑入
            float titleAnimDuration = 600f;  // 动画持续 600ms
            float titleProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / titleAnimDuration);
            // 缓动函数
            titleProgress = 1.0f - (float) Math.pow(1.0f - titleProgress, 3);

            // 目标位置：虚拟高度的 20%
            int targetTitleY = (int) (virtualHeight * 0.2);

            // 滑动距离：80 虚拟像素
            int slideDistance = 80;

            // 计算当前 Y：目标位置 - (未完成进度 × 滑动距离)
            // 动画开始时：targetY - 80（在屏幕上方）
            // 动画结束时：targetY
            titleY = targetTitleY - (int) ((1.0f - titleProgress) * slideDistance);
        } else {
            // 关闭时：保持固定位置
            titleY = (int) (virtualHeight * 0.2);
        }

        // 标题 X 坐标：左栏中心
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

        // ==================== 绘制右侧面板 ====================
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

        // ==================== 绘制玩家模型 ====================
        renderPlayerModel(guiGraphics, rightOffsetY);

        // ==================== 绘制星星（模型上方） ====================
        String starEmoji = "⭐";
        int starWidth = mc.font.width(starEmoji);
        // X：rightCenterX - starWidth/2（水平居中）
        // Y：MODEL_FOOT_Y + 行高*0.7 + 动画偏移
        guiGraphics.drawString(mc.font, starEmoji, rightCenterX - starWidth / 2, (int) (MODEL_FOOT_Y + mc.font.lineHeight * 0.7) + rightOffsetY, 0xFFFF00);

        // ==================== 绘制等级圆（带进度弧） ====================
        int level = PlayerLevelManager.getPlayerLevelClient(player);
        String levelText = String.valueOf(level);

        // 计算等级文字缩放
        int levelWidthRaw = mc.font.width(levelText);
        float maxLevelWidth = rightPanelWidth * 0.15f;  // 最大宽度为右栏的 15%
        float levelScale = maxLevelWidth / levelWidthRaw;

        // 计算圆半径
        int circleRadius = (int) (levelWidthRaw * levelScale * 0.7);

        // 个位数时特殊处理（防止圆太小）
        if (level < 10) {
            levelScale = 2.0f;
            circleRadius = (int) (levelWidthRaw * 3.0f);
        }

        // 圆心位置
        int circleX = rightCenterX;  // X：右栏中心
        // Y：模型下方 + 半径间距 + 动画偏移
        int circleY = (int) (MODEL_FOOT_Y + circleRadius * 1.8) + rightOffsetY;

        // 获取经验进度
        float progress = PlayerLevelManager.getExperienceProgressClient(player);

        // 绘制进度圆（背景圆 + 进度弧）
        drawProgressCircle(guiGraphics, circleX, circleY, circleRadius, progress);

        // 绘制等级文字（圆心）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(circleX, circleY, 0);  // 移动到圆心
        guiGraphics.pose().scale(levelScale, levelScale, 1.0f);  // 缩放等级文字
        // 向左偏移一半宽度、向上偏移半行高实现居中
        guiGraphics.drawString(mc.font, levelText, -levelWidthRaw / 2, -mc.font.lineHeight / 2, 0xFFFFFF);
        guiGraphics.pose().popPose();

        // ==================== 绘制经验值 ====================
        String expText = PlayerLevelManager.getPlayerExperienceClient(player) + "/" + PlayerLevelManager.getExperienceNeededForNextLevelClient(player);
        int expWidthRaw = mc.font.width(expText);
        int expMaxWidth = (int) (rightPanelWidth * 0.25);
        float expScale = (float) expMaxWidth / expWidthRaw;

        // Y 坐标：圆下方 + 半径 + 行高缩放后的间距
        int expY = circleY + circleRadius + (int) (mc.font.lineHeight * expScale * 0.8f);

        // 绘制经验值（居中）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(circleX, expY, 0);
        guiGraphics.pose().scale(expScale, expScale, 1.0f);
        guiGraphics.drawString(mc.font, expText, -expWidthRaw / 2, 0, 0xFFFFAA);  // 金色
        guiGraphics.pose().popPose();

        // ==================== 绘制玩家名称 + 感染状态 ====================
        String playerName = "§e" + player.getScoreboardName();

        // 获取感染值并确定状态
        int infection = PlayerInfectionManager.getCurrentInfectionClient(player);
        String status = infection >= 100 ? "§c感染者" : "§a幸存者";

        // 组合名称：颜色 + 玩家名 + 爱心 + 状态
        playerName = "§7" + playerName + " §c❤" + " §7[" + status + "§7]";

        // 计算名称文字缩放
        int nameWidthRaw = mc.font.width(playerName);
        int nameX = rightCenterX;  // X：右栏中心（水平居中）
        // Y：经验值下方 + 文字高度 + 间距
        int nameY = expY + (int) (mc.font.lineHeight * expScale) + 10;

        float maxNameWidth = rightPanelWidth * 0.55f;
        float nameScale = maxNameWidth / nameWidthRaw;

        // 限制最大缩放（防止文字太大）
        if (nameScale > 1.5f) {
            nameScale = 1.2f;
        }

        // 绘制玩家名称（居中）
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(nameX, nameY, 0);
        guiGraphics.pose().scale(nameScale, nameScale, 1.0f);
        guiGraphics.drawString(mc.font, playerName, -nameWidthRaw / 2, 0, 0xFFFFFF);
        guiGraphics.pose().popPose();

        // ==================== 绘制分割线 ====================
        int underlineWidth = (int) (rightPanelWidth * 0.8);  // 分割线宽度为右栏的 80%
        // 起点：从右边缘向左偏移右栏的 90%
        int underlineX = (int) (virtualWidth - rightPanelWidth * 0.9f);
        int underlineY = (int) (nameY + mc.font.lineHeight + 7);  // Y：名称下方 + 行高 + 间距
        guiGraphics.fill(RenderType.gui(), underlineX, underlineY, underlineX + underlineWidth, underlineY + 2, 0xFFFFFFFF);

        // ==================== 绘制 Rank & Title ====================
        int rankTitleY = underlineY + 7;  // 分割线下方 7 像素

        Rank rank = PlayerRankManager.getPlayerRankClient(player);
        Title title = PlayerTitleManager.getPlayerTitleClient(player);

        int rankColor = getRankColor(rank.getRankLevel());

        // Rank（左侧对齐）
        String rankText = "🏆 " + rank.getRankName();
        guiGraphics.drawString(mc.font, rankText, underlineX, rankTitleY, rankColor);

        // Title（右侧对齐）
        String titleText = "⭐ " + title.getTitleName();
        // 靠右对齐：起点 + 宽度 - 文字宽度
        int titleX = underlineX + underlineWidth - mc.font.width(titleText);
        guiGraphics.drawString(mc.font, titleText, titleX, rankTitleY, 0xFF000000 | title.getColor());

        // ==================== 绘制属性进度条 ====================
        renderAttributeBars(guiGraphics, player, RIGHT_PANEL_START_X, rightPanelWidth, virtualHeight, rightOffsetY);

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
        InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, rightCenterX, MODEL_FOOT_Y, MODEL_SIZE, 0, 0, player);

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
     * 渲染五个属性进度条（两行排列在右侧面板底部）
     *
     * 布局示意：
     * ┌─────────────────────────────────────────────────────────────┐
     * │  第一行：❤血量 — 🍖饥饿 — 💪力量                           │
     * │  第二行：     ⚡勇气 — ☣感染  （居中显示）                    │
     * └─────────────────────────────────────────────────────────────┘
     *
     * @param guiGraphics 图形上下文
     * @param player 当前玩家
     * @param rightPanelX 右侧面板起点 X 坐标（虚拟像素）
     * @param rightPanelWidth 右侧面板宽度（虚拟像素）
     * @param screenHeight 虚拟画布高度
     * @param offsetY Y 轴动画偏移量（虚拟像素）
     */
    private void renderAttributeBars(GuiGraphics guiGraphics, LocalPlayer player, int rightPanelX, int rightPanelWidth, int screenHeight, int offsetY) {
        // ==================== 尺寸参数（虚拟像素） ====================
        int margin = 12;           // 进度条距离面板边缘的边距
        int barsPerRow = 3;        // 第一行进度条数量
        int barsSecondRow = 2;     // 第二行进度条数量
        int barSpacing = 12;       // 进度条之间的间距
        int rowSpacing = 35;       // 两行之间的间距
        int barHeight = 7;         // 进度条高度
        int bottomMargin = 10;     // 进度条与底边的间距

        // ==================== 计算第一行进度条宽度 ====================
        int totalSpacingFirstRow = barSpacing * (barsPerRow - 1);  // 总间距：12 * 2 = 24
        // 可用宽度：右栏宽度 - 左右边距 - 总间距
        int availableWidthFirstRow = rightPanelWidth - margin * 2 - totalSpacingFirstRow;
        int barWidthFirstRow = availableWidthFirstRow / barsPerRow;  // 单个进度条宽度

        // ==================== 计算第二行进度条宽度 ====================
        int totalSpacingSecondRow = barSpacing * (barsSecondRow - 1);  // 总间距：12 * 1 = 12
        int availableWidthSecondRow = rightPanelWidth - margin * 2 - totalSpacingSecondRow;
        int barWidthSecondRow = barWidthFirstRow;  // 与第一行保持相同宽度

        // ==================== 计算第一行进度条 Y 坐标 ====================
        // 从底部向上：高度 - 边距 - 高度 - 行间距 - 底边距 + 动画偏移
        int barY = screenHeight - margin - barHeight - rowSpacing - bottomMargin + offsetY;

        // ==================== 获取玩家属性值 ====================
        float healthPercent = player.getHealth() / player.getMaxHealth();  // 血量百分比
        float foodPercent = (float) player.getFoodData().getFoodLevel() / 20.0f;  // 饥饿值百分比

        int strength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
        int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;  // 防止除零
        float strengthPercent = (float) strength / maxStrength;  // 力量值百分比

        float courage = PlayerCourageManager.getCurrentCourageClient(player);
        float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
        if (maxCourage <= 0) maxCourage = 100;
        float couragePercent = courage / maxCourage;  // 勇气值百分比

        float infectionPercent = (float) PlayerInfectionManager.getCurrentInfectionClient(player) / 100.0f;  // 感染值百分比

        // ==================== 准备进度条数据 ====================
        String[] icons = {"❤", "🍖", "💪", "⚡", "☣"};  // 进度条上方显示的 emoji 图标
        int[] colors = {BAR_HEALTH_COLOR, BAR_FOOD_COLOR, BAR_STRENGTH_COLOR, BAR_COURAGE_COLOR, BAR_INFECTION_COLOR};
        float[] percents = {healthPercent, foodPercent, strengthPercent, couragePercent, infectionPercent};
        String[] values = {
                String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth()),
                String.format("%d/20", player.getFoodData().getFoodLevel()),
                String.format("%d/%d", strength, maxStrength),
                String.format("%.0f/%.0f", courage, maxCourage),
                String.format("%d/100", PlayerInfectionManager.getCurrentInfectionClient(player))
        };

        // ==================== 绘制第一行：3个进度条 ====================
        int barX = rightPanelX + margin;  // 起始 X 坐标
        int iconY = barY - 16;  // 图标 Y 坐标（进度条上方 16 像素）

        for (int i = 0; i < barsPerRow; i++) {
            // 绘制进度条
            drawProgressBar(guiGraphics, barX, barY, barWidthFirstRow, barHeight, percents[i], colors[i] & 0x00FFFFFF);

            // 绘制图标（进度条上方，水平居中）
            guiGraphics.drawCenteredString(mc.font, icons[i], barX + barWidthFirstRow / 2, iconY, colors[i]);

            // 绘制数值文本（进度条下方，缩放到 50%）
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);  // 缩小到一半
            // 坐标 × 2 抵消缩放，使文字绘制在正确位置
            guiGraphics.drawCenteredString(mc.font, values[i], (barX + barWidthFirstRow / 2) * 2, (barY + barHeight + 4) * 2, 0xFFFFFFFF);
            guiGraphics.pose().popPose();

            // 移动到下一个进度条位置
            barX += barWidthFirstRow + barSpacing;
        }

        // ==================== 绘制第二行：2个进度条（居中显示） ====================
        int barY2 = barY + rowSpacing;  // 第二行 Y 坐标
        int iconY2 = barY2 - 16;

        // 计算第二行的起始 X 坐标，使 2 个进度条在右侧面板中水平居中
        int secondRowTotalWidth = barWidthSecondRow * barsSecondRow + barSpacing * (barsSecondRow - 1);
        barX = rightPanelX + (rightPanelWidth - secondRowTotalWidth) / 2;

        for (int i = barsPerRow; i < 5; i++) {
            // 绘制进度条
            drawProgressBar(guiGraphics, barX, barY2, barWidthSecondRow, barHeight, percents[i], colors[i] & 0x00FFFFFF);

            // 绘制图标
            guiGraphics.drawCenteredString(mc.font, icons[i], barX + barWidthSecondRow / 2, iconY2, colors[i]);

            // 绘制数值文本
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
            guiGraphics.drawCenteredString(mc.font, values[i], (barX + barWidthSecondRow / 2) * 2, (barY2 + barHeight + 4) * 2, 0xFFFFFFFF);
            guiGraphics.pose().popPose();

            // 移动到下一个进度条位置
            barX += barWidthSecondRow + barSpacing;
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
