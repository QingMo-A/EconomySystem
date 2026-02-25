package com.mo.economy_system.screen.server_screen.serverscreen;

import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerdata_system.Packet_RequestPlayerStats;
import com.mo.economy_system.network.packets.territory_system.Packet_TerritoryDataRequest;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.economy_system.shop.Screen_Shop;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mo.economy_system.network.packets.notice_system.Packet_NoticeListRequest;
import com.mo.economy_system.network.packets.notice_system.Packet_MarkNoticeReadRequest;
import com.mo.economy_system.server.notice.NoticeData;
import com.mo.economy_system.screen.server_screen.notice.Screen_NoticeDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mo.economy_system.screen.server_screen.serverscreen.ServerScreenUI_RendererUtils.*;

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

    private static final String VERSION = "§bDreaming§dFish §7v0.1(Private)";

    // ==================== 虚拟基准尺寸 ====================
    // 基准：2560×1440 全屏 + GUI缩放4 → 内部渲染尺寸 640×360
    // 所有 UI 元素按这个尺寸设计，运行时自动缩放
    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;

    // ==================== 面板比例 ====================
    private static final float LEFT_PANEL_PERCENT = 0.20f;  // 左侧面板占虚拟宽度的 20%
    private static final float RIGHT_PANEL_PERCENT = 0.45f;  // 右侧面板占虚拟宽度的 35%

    // ==================== 颜色定义（淡色系） ====================
    private static final int PANEL_BACKGROUND_COLOR = 0x801A1A2A;  // 半透明深蓝背景（alpha=128）
    private static final int PANEL_BORDER_COLOR = 0xFF4A5568;      // 淡灰边框
    // 注意：游戏化卡片配色、进度条颜色等常量已移至 ServerScreenUI_RendererUtils

    // ==================== 动画时间配置 ====================
    // 开启动画
    private long openTime = 0;                                // UI 打开的时间戳
    private static final long ANIMATION_DURATION = 400;        // 打开边框动画持续时间（毫秒）

    // 关闭动画
    private boolean isClosing = false;                         // 是否正在执行关闭动画
    private long closeTime = 0;                                // UI 开始关闭的时间戳
    private static final long CLOSE_ANIMATION_DURATION = 150;  // 关闭动画持续时间（毫秒）

    // 跳过动画标记（从子屏幕返回时使用）
    private boolean skipAnimation = false;

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

    // 卡片可点击区域（虚拟坐标）- 现在由 PageRenderer 管理
    // 保留 rankBox 变量以备将来使用
    private int rankBoxClickX1, rankBoxClickY1;
    private int rankBoxClickX2, rankBoxClickY2;

    // ==================== 左侧灵动岛按钮 ====================
    private static final String[] LEFT_BUTTON_ICONS = {"👤", "❓", "📢", "📖", "🏆", "⭐", "🛒", "🏰", "🎒", "⚙️"};
    private static final String[] LEFT_BUTTON_NAMES = {"个人档案", "新玩家帮助", "服务器公告", "故事进展", "玩家与排行", "服务器成就", "服务器商店", "领地", "背包", "设置"};
    private static final int[] LEFT_BUTTON_COLORS = {
        0xFFAAAAAA,  // 个人档案 - 灰色
        0xFF55FF55,  // 帮助 - 绿色
        0xFF4FC3F7,  // 服务器公告 - 淡蓝色
        0xFFAAFFAA,  // 故事进展 - 绿色
        0xFF4FC3F7,  // 玩家与排行 - 金色
        0xFFFFFFAA,  // 服务器成就 - 黄色
        0xFF4FC3F7,  // 服务器商店 - 橙色
        0xFF4FC3F7,  // 领地 - 紫色
        0xFFFFAAAA,  // 背包 - 粉色
        0xFF888888   // 设置 - 深灰色
    };

    // 左侧按钮可点击区域（虚拟坐标）
    private int[] leftButtonX1 = new int[LEFT_BUTTON_ICONS.length];
    private int[] leftButtonY1 = new int[LEFT_BUTTON_ICONS.length];
    private int[] leftButtonX2 = new int[LEFT_BUTTON_ICONS.length];
    private int[] leftButtonY2 = new int[LEFT_BUTTON_ICONS.length];

    // 当前选中的左侧按钮索引（0=个人档案，默认选中）
    private int selectedLeftButtonIndex = 0;
    // 箭头动画时间
    private long arrowAnimTime = 0;
    // 上次选中的按钮索引（用于动画检测）
    private int lastSelectedIndex = -1;
    // 注意：按钮配色和信息区配色常量已移至 ServerScreenUI_RendererUtils

    // ==================== 公告系统数据 ====================
    private static List<NoticeData> cachedNotices = new ArrayList<>();
    private static Set<Integer> cachedReadNoticeIds = new java.util.HashSet<>();
    private static long noticeScrollOffset = 0;  // 滚动偏移量
    private static final int NOTICE_CARD_HEIGHT = 52;  // 每个公告卡片高度（虚拟像素）
    private static final int VISIBLE_NOTICES = 5;  // 可见公告数量
    private static boolean hasUnreadNoticesGlobal = false;  // 全局未读公告标记（用于按钮感叹号）
    // 注意：公告点击区域已移至 PageRenderer.noticeClickArea

    // ==================== 任务系统数据 ====================
    private static final int TASK_CARD_HEIGHT = 60;  // 任务卡片高度
    private static final int VISIBLE_TASKS = 4;  // 可见任务数量
    private static long taskScrollOffset = 0;  // 任务滚动偏移量
    private static boolean taskShowServerTasks = true;  // true=服务器任务, false=个人任务
    private static String selectedStageId = null;  // 当前选中的阶段ID，null表示显示阶段列表
    private static long stageScrollOffset = 0;  // 阶段列表滚动偏移量
    // 注意：任务点击区域已移至 PageRenderer.taskClickArea 和 taskTabArea

    // ==================== 帮助系统数据 ====================
    private static long helpScrollOffset = 0;  // 帮助页面滚动偏移量
    private static final int HELP_LINE_HEIGHT = 18;  // 帮助页面每行高度

    private final Minecraft mc = Minecraft.getInstance();

    // ==================== 页面渲染器 ====================
    private ServerScreenUI_PageRenderer pageRenderer;

    public ServerScreenUI_Screen() {
        super(Component.literal("服务器界面"));
    }

    // ==================== Getter 方法供 PageRenderer 使用 ====================
    public float getUiScale() { return uiScale; }
    public int getVirtualWidth() { return virtualWidth; }
    public int getVirtualHeight() { return virtualHeight; }
    public int getPanelBackgroundColor() { return PANEL_BACKGROUND_COLOR; }
    public long getHelpScrollOffset() { return helpScrollOffset; }

    @Override
    protected void init() {
        super.init();
        // 初始化页面渲染器
        pageRenderer = new ServerScreenUI_PageRenderer(this, LEFT_BUTTON_ICONS.length);
        // 检查是否从子屏幕返回，保存跳过动画标记
        skipAnimation = ServerScreenUI.isReturningFromSubScreen();
        if (skipAnimation) {
            ServerScreenUI.setReturningFromSubScreen(false);
        }
        // 记录动画开始时间
        // 如果是从子屏幕返回，跳过动画（将 openTime 设为过去的时间）
        if (skipAnimation) {
            openTime = Util.getMillis() - ANIMATION_DURATION - 1;
        } else {
            openTime = Util.getMillis();
        }
        // 计算缩放比例
        calculateVirtualSize();
        // 请求领地数据
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_TerritoryDataRequest());
        // 请求统计数据（群系 + 配方）
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RequestPlayerStats());
        // 请求公告数据（用于更新感叹号状态）
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_NoticeListRequest());
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
        renderPanels(guiGraphics, mouseX, mouseY);

        // 恢复矩阵状态
        guiGraphics.pose().popPose();

        // ==================== 渲染提示框（使用屏幕坐标） ====================
        renderTooltips(guiGraphics, mouseX, mouseY);
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
    private void renderPanels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
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
        // 内容：灵动岛（按钮网格） + 版本号
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

        // ==================== 绘制左侧灵动岛（按钮容器） ====================
        renderLeftDynamicIsland(guiGraphics, mouseX, mouseY);

        // ==================== 绘制左侧标题（左下角，带版本号） ====================

        // 获取文字原始宽度（受 GUI 缩放影响）
        int titleWidth = mc.font.width(VERSION);

        // 计算缩放比例：使文字宽度适配左栏宽度的 90%
        float maxWidth = leftPanelWidth * 0.90f;
        float scale = maxWidth / titleWidth;
        // 限制最大缩放
        if (scale > 1.2f) scale = 1.2f;

        // 标题 Y 坐标（左下角）
        int titleY;
        if (!isClosing && !skipAnimation) {
            // 打开时：从下往上滑入
            float titleAnimDuration = 600f;
            float titleProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / titleAnimDuration);
            titleProgress = 1.0f - (float) Math.pow(1.0f - titleProgress, 3);

            int targetTitleY = virtualHeight - mc.font.lineHeight - 10;  // 距底部 10 像素
            int slideDistance = 60;

            titleY = targetTitleY + (int) ((1.0f - titleProgress) * slideDistance);
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
        guiGraphics.drawString(mc.font, VERSION, -titleWidth / 2, 0, 0xFFFFFF);
        guiGraphics.pose().popPose();

        // 结束左侧面板变换
        guiGraphics.pose().popPose();

        // ========================================================================
        //                           中栏 (CENTER PANEL)
        // 布局：
        // - 模型头部上方：玩家名称 + 幸存者状态
        // - 20%-60%：玩家3D模型
        // - 60%-100%：等级圆 + 经验值
        // 只在主页面(0)和公告页面(1)渲染
        // ========================================================================
        // 计算中栏内容的动画偏移（与右栏保持一致）
        int centerOffsetY;
        if (!isClosing && !skipAnimation) {
            float centerAnimDuration = 800f;
            float centerProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / centerAnimDuration);
            centerProgress = 1.0f - (float) Math.pow(1.0f - centerProgress, 3);
            centerOffsetY = (int) ((1.0f - centerProgress) * 100);
        } else {
            centerOffsetY = 0;
        }

        // ==================== 只在主页面(0)和公告页面(2)渲染中间栏内容 ====================
        if (selectedLeftButtonIndex == 0 || selectedLeftButtonIndex == 2) {
        // ==================== 绘制玩家名称 + 幸存者状态（模型头上方） ====================
        // 获取感染值并确定状态
        float infection = PlayerInfectionManager.getCurrentInfectionClient(player);
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
        pageRenderer.renderPlayerModel(guiGraphics, centerOffsetY, mouseX, mouseY, centerCenterX, MODEL_FOOT_Y, MODEL_SIZE, uiScale);

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
        }  // 结束中间栏渲染条件 (selectedLeftButtonIndex == 0 || selectedLeftButtonIndex == 2)

        // ==================== 服务器公告页面 ====================
        if (selectedLeftButtonIndex == 2) {
            // 渲染公告列表
            pageRenderer.renderNoticeList(guiGraphics, RIGHT_PANEL_START_X, rightPanelWidth,
                cachedNotices, cachedReadNoticeIds, noticeScrollOffset, NOTICE_CARD_HEIGHT, VISIBLE_NOTICES);
            guiGraphics.pose().popPose();
            return;  // 跳过后续右栏内容渲染
        }

        // ==================== 故事/任务页面 ====================
        if (selectedLeftButtonIndex == 3) {
            // 渲染任务列表（使用整个中间+右侧区域）
            pageRenderer.renderTaskPage(guiGraphics, leftPanelWidth, virtualWidth - leftPanelWidth, mouseX, mouseY,
                virtualWidth, uiScale, taskShowServerTasks,
                com.mo.economy_system.client.cache.ClientCacheManager.getStoryStages(),
                com.mo.economy_system.client.cache.ClientCacheManager.getPlayerTasks(),
                taskScrollOffset, TASK_CARD_HEIGHT, VISIBLE_TASKS,
                selectedStageId, stageScrollOffset);
            guiGraphics.pose().popPose();
            return;  // 跳过后续右栏内容渲染
        }

        // ==================== 排行榜页面 ====================
        if (selectedLeftButtonIndex == 4) {
            // 渲染排行榜页面（使用整个中间+右侧区域）
            pageRenderer.renderRankPage(guiGraphics, leftPanelWidth, virtualWidth - leftPanelWidth, mouseX, mouseY,
                virtualWidth, uiScale, mc.player);
            guiGraphics.pose().popPose();
            return;  // 跳过后续右栏内容渲染
        }

        // ==================== 成就页面 ====================
        if (selectedLeftButtonIndex == 5) {
            // 渲染成就页面（使用整个中间+右侧区域）
            pageRenderer.renderAchievementPage(guiGraphics, leftPanelWidth, virtualWidth - leftPanelWidth, mouseX, mouseY,
                virtualWidth, uiScale);
            guiGraphics.pose().popPose();
            return;  // 跳过后续右栏内容渲染
        }

        // ==================== 帮助页面 ====================
        if (selectedLeftButtonIndex == 1) {
            // 渲染帮助页面（使用整个中间+右侧区域）
            pageRenderer.renderHelpPage(guiGraphics, leftPanelWidth, virtualWidth - leftPanelWidth, mouseX, mouseY,
                virtualWidth, uiScale);
            guiGraphics.pose().popPose();
            return;  // 跳过后续右栏内容渲染
        }

        // ==================== 右侧内容滑入动画 ====================
        int rightOffsetY;
        if (!isClosing && !skipAnimation) {
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
        pageRenderer.renderAttributeBarsHorizontal(guiGraphics, player, RIGHT_PANEL_START_X, rightPanelWidth, rightOffsetY);

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
        pageRenderer.renderRankBox(guiGraphics, player, rankBoxX, twoBoxY, twoBoxWidth, mouseX, mouseY, uiScale);

        // Title 框（右侧）
        int titleBoxX = rankBoxX + twoBoxWidth + boxSpacing;
        pageRenderer.renderTitleBox(guiGraphics, player, titleBoxX, twoBoxY, twoBoxWidth, mouseX, mouseY, uiScale);

        // ==================== 绘制金币和领地框（左右排列，Rank/Title框下方） ====================
        int thirdBoxY = twoBoxY + (innerMargin * 2 + lineHeight) + boxSpacing;

        // 金币框（左侧）
        int goldBoxX = RIGHT_PANEL_START_X + boxMargin;
        pageRenderer.renderGoldBox(guiGraphics, goldBoxX, thirdBoxY, twoBoxWidth, mouseX, mouseY, uiScale);

        // 领地框（右侧）
        int territoryBoxX = goldBoxX + twoBoxWidth + boxSpacing;
        pageRenderer.renderTerritoryBox(guiGraphics, territoryBoxX, thirdBoxY, twoBoxWidth, mouseX, mouseY, uiScale);

        // ==================== 绘制群系和蓝图框（金币/领地框下方，左右排列） ====================
        int fourthBoxY = thirdBoxY + (innerMargin * 2 + lineHeight) + boxSpacing;

        // 群系框（左侧）
        int biomesBoxX = RIGHT_PANEL_START_X + boxMargin;
        pageRenderer.renderExplorationStats(guiGraphics, biomesBoxX, fourthBoxY, twoBoxWidth, mouseX, mouseY, uiScale);

        // 蓝图框（右侧）
        int blueprintBoxX = biomesBoxX + twoBoxWidth + boxSpacing;
        pageRenderer.renderBlueprintBox(guiGraphics, blueprintBoxX, fourthBoxY, twoBoxWidth, mouseX, mouseY, uiScale);

        // ==================== 绘制感染度/分裂次数信息框（最底部） ====================
        int infoBoxY = fourthBoxY + (innerMargin * 2 + lineHeight) + boxSpacing;
        int infoBoxWidth = rightPanelWidth - boxMargin * 2;
        pageRenderer.renderInfectionInfoBox(guiGraphics, player, RIGHT_PANEL_START_X + boxMargin, infoBoxY, infoBoxWidth);

        // 结束右侧面板变换
        guiGraphics.pose().popPose();
    }

    /**
     * 绘制左侧按钮区域 + 服务器信息区域
     */
    private void renderLeftDynamicIsland(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // ==================== 布局参数 ====================
        int buttonWidth = leftPanelWidth - 10;
        int buttonHeight = 26;
        int buttonSpacing = 1;
        int sideMargin = (leftPanelWidth - buttonWidth) / 2;  // 左右边距

        int totalButtons = LEFT_BUTTON_ICONS.length;

        // 按钮位置：顶部和左右边距一致
        int buttonX = sideMargin;
        int buttonStartY = sideMargin;

        // 滑入动画
        int animOffsetY = 0;
        if (!isClosing && !skipAnimation) {
            float animDuration = 500f;
            float progress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / animDuration);
            progress = 1.0f - (float) Math.pow(1.0f - progress, 3);
            animOffsetY = (int) ((1.0f - progress) * 50);
        }

        // 更新动画时间
        arrowAnimTime = Util.getMillis();

        // 转换鼠标坐标
        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        // ==================== 绘制按钮列表 ====================
        int currentButtonStartY = buttonStartY + animOffsetY;

        for (int i = 0; i < totalButtons; i++) {
            int buttonY = currentButtonStartY + i * (buttonHeight + buttonSpacing);

            // 存储点击区域
            leftButtonX1[i] = buttonX;
            leftButtonY1[i] = buttonY;
            leftButtonX2[i] = buttonX + buttonWidth;
            leftButtonY2[i] = buttonY + buttonHeight;

            boolean isSelected = (i == selectedLeftButtonIndex);
            boolean isHovered = (virtualMouseX >= buttonX && virtualMouseX <= buttonX + buttonWidth &&
                                virtualMouseY >= buttonY && virtualMouseY <= buttonY + buttonHeight);

            // 检查是否有未读/未完成内容
            boolean hasUnread = false;
            if (i == 2 && hasUnreadNoticesGlobal) {
                // 公告按钮：未读公告（使用全局标记）
                hasUnread = true;
            } else if (i == 3) {
                // 故事按钮：未完成任务
                hasUnread = com.mo.economy_system.client.cache.ClientCacheManager.hasUnfinishedTasks();
            }

            drawCleanButton(guiGraphics, mc.font, buttonX, buttonY, buttonWidth, buttonHeight,
                isSelected, isHovered, LEFT_BUTTON_ICONS[i], LEFT_BUTTON_NAMES[i], hasUnread, arrowAnimTime);
        }

        // ==================== 绘制服务器信息区域（版本号上方） ====================
        int infoHeight = 55;
        int versionBottomMargin = 8;  // 版本号和信息区之间的间距
        int versionHeight = mc.font.lineHeight + 10;
        int infoY = virtualHeight - versionHeight - infoHeight - versionBottomMargin;

        // 服务器信息区域滑入动画（从下往上，比按钮稍晚一点）
        int infoAnimOffsetY = 0;
        if (!isClosing && !skipAnimation) {
            float infoAnimDuration = 600f;
            float infoProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / infoAnimDuration);
            infoProgress = 1.0f - (float) Math.pow(1.0f - infoProgress, 3);
            infoAnimOffsetY = (int) ((1.0f - infoProgress) * 40);  // 从下往上滑入 40 像素
        }

        // 获取服务器数据
        int onlinePlayers = mc.player != null && mc.player.connection != null ?
            mc.player.connection.getOnlinePlayers().size() : 0;
        int maxPlayers = 20;
        float tps = 20.0f;

        drawServerInfoArea(guiGraphics, mc.font, buttonX, infoY, buttonWidth, infoHeight, infoAnimOffsetY, onlinePlayers, maxPlayers, tps);
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
        ServerScreenUI_RendererUtils.drawRoundedRect(guiGraphics, x, y, width, height, radius, fillColor, borderColor);
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
        ServerScreenUI_RendererUtils.drawRoundedRectOutline(guiGraphics, x, y, width, height, radius, fillColor, borderColor);
    }

    /**
     * 绘制信息框（简单的半透明边框）
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 宽度
     * @param height 高度
     */

    /**
     * 绘制游戏化卡片背景
     * @param x 左上角 X 坐标
     * @param y 左上角 Y 坐标
     * @param width 宽度
     * @param height 高度
     * @param themeColor 主题色（用于左侧装饰条和渐变）
     * @param isHovered 是否鼠标悬停
     */
    private void drawGameCard(GuiGraphics guiGraphics, int x, int y, int width, int height, int themeColor, boolean isHovered) {
        ServerScreenUI_RendererUtils.drawGameCard(guiGraphics, x, y, width, height, themeColor, isHovered);
    }

    private void drawDoubleBorderBox(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        ServerScreenUI_RendererUtils.drawDoubleBorderBox(guiGraphics, x, y, width, height);
    }

    /**
     * 渲染圆角盒子（参考 ConnectScreenMixin）
     */
    private void renderRoundedBox(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        ServerScreenUI_RendererUtils.renderRoundedBox(guiGraphics, x1, y1, x2, y2, color);
    }

    /**
     * 绘制渐变梦幻色框 (已移至 RendererUtils)
     */
    private void drawGradientBox(GuiGraphics guiGraphics, int x, int y, int width, int height, int gradientType) {
        ServerScreenUI_RendererUtils.drawGradientBox(guiGraphics, x, y, width, height, gradientType);
    }

    /**
     * 获取渐变色 (已移至 RendererUtils)
     */
    private int getGradientColor(int type, float ratio) {
        return ServerScreenUI_RendererUtils.getGradientColor(type, ratio);
    }

    /**
     * 绘制圆角进度条 (已移至 RendererUtils)
     */
    private void drawRoundedProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float pct, int color) {
        ServerScreenUI_RendererUtils.drawRoundedProgressBar(guiGraphics, x, y, width, height, pct, color);
    }

    /**
     * 根据 Rank 等级获取对应颜色 (已移至 RendererUtils)
     */
    private int getRankColor(int rankLevel) {
        return ServerScreenUI_RendererUtils.getRankColor(rankLevel);
    }

    /**
     * 根据感染值百分比计算动态颜色 (已移至 RendererUtils)
     */
    private int getInfectionColor(float infectionPercent) {
        return ServerScreenUI_RendererUtils.getInfectionColor(infectionPercent);
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

        // 任务页面：Q/E 键切换故事/个人任务
        if (selectedLeftButtonIndex == 2) {
            // Q 键 (key code 16) -> 切换到故事任务
            if (keyCode == 16) {
                taskShowServerTasks = true;
                selectedStageId = null;  // 重置阶段选择
                taskScrollOffset = 0;
                stageScrollOffset = 0;
                return true;
            }
            // E 键 (key code 18) -> 切换到个人任务
            if (keyCode == 18) {
                taskShowServerTasks = false;
                taskScrollOffset = 0;
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 将屏幕坐标转换为虚拟坐标
        double virtualMouseX = mouseX / uiScale;
        double virtualMouseY = mouseY / uiScale;

        // ==================== 检查左侧按钮点击 ====================
        for (int i = 0; i < LEFT_BUTTON_ICONS.length; i++) {
            if (virtualMouseX >= leftButtonX1[i] && virtualMouseX <= leftButtonX2[i] &&
                virtualMouseY >= leftButtonY1[i] && virtualMouseY <= leftButtonY2[i]) {
                // 点击了左侧按钮，更新选中状态
                selectedLeftButtonIndex = i;
                handleLeftButtonClick(i);
                return true;
            }
        }

        // 计算右侧面板偏移（考虑动画）
        int rightOffsetY = 0;
        if (!isClosing) {
            float rightAnimDuration = 800f;
            float rightProgress = Math.min(1.0f, (float) (Util.getMillis() - openTime) / rightAnimDuration);
            rightProgress = 1.0f - (float) Math.pow(1.0f - rightProgress, 3);
            rightOffsetY = (int) ((1.0f - rightProgress) * 100);
        }

        // 检查是否点击了金币框（仅主页面可点击）
        int[] goldBoxClick = pageRenderer.getGoldBoxClick();
        if (selectedLeftButtonIndex == 0 &&
            virtualMouseX >= goldBoxClick[0] && virtualMouseX <= goldBoxClick[2] &&
            virtualMouseY >= goldBoxClick[1] + rightOffsetY && virtualMouseY <= goldBoxClick[3] + rightOffsetY) {
            // 打开商店界面（经济系统）
            // 先将 SHOW_UI 设置为 false，防止 onClose 调用 toggleUI() 导致重新打开
            ServerScreenUI.setShowUI(false);
            mc.setScreen(new Screen_Home());
            return true;
        }

        // 检查是否点击了领地框（仅主页面可点击）
        int[] territoryBoxClick = pageRenderer.getTerritoryBoxClick();
        if (selectedLeftButtonIndex == 0 &&
            virtualMouseX >= territoryBoxClick[0] && virtualMouseX <= territoryBoxClick[2] &&
            virtualMouseY >= territoryBoxClick[1] + rightOffsetY && virtualMouseY <= territoryBoxClick[3] + rightOffsetY) {
            // 打开领地管理界面
            // 先将 SHOW_UI 设置为 false，防止 onClose 调用 toggleUI() 导致重新打开
            ServerScreenUI.setShowUI(false);
            mc.setScreen(new Screen_Territory());
            return true;
        }

        // ==================== 检查公告列表点击 ====================
        if (selectedLeftButtonIndex == 2 && !cachedNotices.isEmpty()) {  // 服务器公告页面
            int[] noticeArea = pageRenderer.getNoticeClickArea();
            if (virtualMouseX >= noticeArea[0] && virtualMouseX <= noticeArea[2] &&
                virtualMouseY >= noticeArea[1] + rightOffsetY && virtualMouseY <= noticeArea[3] + rightOffsetY) {
                // 计算点击的是哪个公告卡片
                int cardMargin = 4;
                int cardHeight = NOTICE_CARD_HEIGHT;
                int relativeY = (int) virtualMouseY - (noticeArea[1] + rightOffsetY);
                int clickedCardIndex = relativeY / (cardHeight + cardMargin);

                int totalNotices = cachedNotices.size();
                int maxCards = Math.min(VISIBLE_NOTICES, totalNotices);

                if (clickedCardIndex >= 0 && clickedCardIndex < maxCards) {
                    int noticeIndex = (int) (clickedCardIndex + noticeScrollOffset);
                    if (noticeIndex < cachedNotices.size()) {
                        NoticeData clickedNotice = cachedNotices.get(noticeIndex);
                        // 打开公告详情弹窗
                        openNoticeDetail(clickedNotice);
                        return true;
                    }
                }
            }
        }

        // ==================== 检查任务页面点击 ====================
        if (selectedLeftButtonIndex == 3) {  // 故事/任务页面
            // 检查任务分类按钮点击
            int[] taskTabArea = pageRenderer.getTaskTabArea();
            if (virtualMouseX >= taskTabArea[0] && virtualMouseX <= taskTabArea[2] &&
                virtualMouseY >= taskTabArea[1] && virtualMouseY <= taskTabArea[3]) {
                // 根据点击位置判断点击了哪个按钮
                var storyStages = com.mo.economy_system.client.cache.ClientCacheManager.getStoryStages();
                String storyText = "📋 故事";
                String personalText = "📜 个人任务";
                int storyWidth = mc.font.width(storyText) + 16;
                int buttonSpacing = 8;
                int storyBtnX2 = taskTabArea[0] + storyWidth;
                int personalBtnX1 = storyBtnX2 + buttonSpacing;

                // 判断点击的是故事按钮还是个人任务按钮
                if (virtualMouseX < storyBtnX2) {
                    // 点击了故事按钮
                    if (!taskShowServerTasks) {
                        taskShowServerTasks = true;
                        selectedStageId = null;
                        taskScrollOffset = 0;
                        stageScrollOffset = 0;
                    }
                } else if (virtualMouseX >= personalBtnX1) {
                    // 点击了个人任务按钮
                    if (taskShowServerTasks) {
                        taskShowServerTasks = false;
                        selectedStageId = null;
                        taskScrollOffset = 0;
                    }
                }
                return true;
            }

            // 检查返回按钮点击（仅在选中阶段时显示）
            if (selectedStageId != null && taskShowServerTasks) {
                int[] backButtonArea = pageRenderer.getBackButtonArea();
                if (virtualMouseX >= backButtonArea[0] && virtualMouseX <= backButtonArea[2] &&
                    virtualMouseY >= backButtonArea[1] && virtualMouseY <= backButtonArea[3]) {
                    // 返回阶段列表
                    selectedStageId = null;
                    stageScrollOffset = 0;
                    taskScrollOffset = 0;
                    return true;
                }
            }

            // 服务器任务：检查阶段列表点击
            if (taskShowServerTasks && selectedStageId == null) {
                int[] stageArea = pageRenderer.getStageClickArea();
                if (virtualMouseX >= stageArea[0] && virtualMouseX <= stageArea[2] &&
                    virtualMouseY >= stageArea[1] && virtualMouseY <= stageArea[3]) {
                    // 点击了阶段卡片，需要找出点击的是哪个阶段
                    var storyStages = com.mo.economy_system.client.cache.ClientCacheManager.getStoryStages();

                    // 按阶段ID排序
                    java.util.List<Integer> sortedStageIds = new java.util.ArrayList<>(storyStages.keySet());
                    java.util.Collections.sort(sortedStageIds);

                    int stageCardHeight = 80;
                    int cardSpacing = 10;
                    int relativeY = (int) virtualMouseY - stageArea[1];
                    int clickedStageIndex = relativeY / (stageCardHeight + cardSpacing);

                    if (clickedStageIndex >= 0 && clickedStageIndex < sortedStageIds.size()) {
                        int stageIndex = (int) (clickedStageIndex + stageScrollOffset);
                        if (stageIndex < sortedStageIds.size()) {
                            selectedStageId = String.valueOf(sortedStageIds.get(stageIndex));
                            taskScrollOffset = 0;
                            return true;
                        }
                    }
                }
            }

            // 检查任务卡片点击（完成按钮）
            int[] taskArea = pageRenderer.getTaskClickArea();
            if (virtualMouseX >= taskArea[0] && virtualMouseX <= taskArea[2] &&
                virtualMouseY >= taskArea[1] && virtualMouseY <= taskArea[3]) {
                if (taskShowServerTasks && selectedStageId != null) {
                    // 故事任务：获取选中阶段的任务列表
                    var storyStages = com.mo.economy_system.client.cache.ClientCacheManager.getStoryStages();
                    com.mo.economy_system.core.story_system.StoryStageData selectedStage = null;
                    for (com.mo.economy_system.core.story_system.StoryStageData stage : storyStages.values()) {
                        if (String.valueOf(stage.getStageId()).equals(selectedStageId)) {
                            selectedStage = stage;
                            break;
                        }
                    }

                    if (selectedStage != null) {
                        java.util.List<com.mo.economy_system.core.story_system.StoryTaskData> stageTasks = selectedStage.getTasks();
                        if (stageTasks == null) stageTasks = new java.util.ArrayList<>();

                        int cardSpacing = 8;
                        int relativeY = (int) virtualMouseY - taskArea[1];
                        int clickedCardIndex = relativeY / (TASK_CARD_HEIGHT + cardSpacing);

                        int totalTasks = stageTasks.size();
                        int maxCards = Math.min(VISIBLE_TASKS, totalTasks);

                        // 故事任务不能点击完成，由服务端控制
                        // 点击事件不处理
                    }
                } else if (!taskShowServerTasks) {
                    // 个人任务
                    var playerTasks = com.mo.economy_system.client.cache.ClientCacheManager.getPlayerTasks();

                    int cardSpacing = 8;
                    int relativeY = (int) virtualMouseY - taskArea[1];
                    int clickedCardIndex = relativeY / (TASK_CARD_HEIGHT + cardSpacing);

                    int totalTasks = playerTasks.size();
                    int maxCards = Math.min(VISIBLE_TASKS, totalTasks);

                    if (clickedCardIndex >= 0 && clickedCardIndex < maxCards) {
                        int taskIndex = (int) (clickedCardIndex + taskScrollOffset);
                        var taskEntry = playerTasks.entrySet().stream().skip(taskIndex).findFirst();
                        if (taskEntry.isPresent()) {
                            int taskId = taskEntry.get().getKey();
                            var task = taskEntry.get().getValue();

                            // 检查是否点击了完成按钮（右侧20x20区域）
                            int btnX = taskArea[2] - 28;
                            if (virtualMouseX >= btnX) {
                                if (!((com.mo.economy_system.core.task_system.TaskPlayerData) task).isClientPlayerFinished()) {
                                    // 发送完成任务请求
                                    EconomySystem_NetworkManager.INSTANCE.sendToServer(
                                        new com.mo.economy_system.network.packets.task_system.Packet_SyncCompleteTask(taskId, false)
                                    );
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 处理左侧按钮点击事件
     * @param index 按钮索引
     */
    private void handleLeftButtonClick(int index) {
        switch (index) {
            case 0: // 个人档案（主页，默认显示）
                // 当前页面，不跳转
                break;
            case 1: // 帮助
                // 帮助页面，不跳转
                break;
            case 2: // 服务器公告
                // 请求公告列表
                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_NoticeListRequest());
                break;
            case 3: // 故事进展
                // TODO: 打开故事界面
                break;
            case 4: // 玩家与排行
                // 显示排行榜页面（不打开新界面）
                break;
            case 5: // 服务器成就
                // 显示成就页面（不打开新界面）
                break;
            case 6: // 服务器商店
                ServerScreenUI.setShowUI(false);
                mc.setScreen(new Screen_Shop());
                break;
            case 7: // 领地
                ServerScreenUI.setShowUI(false);
                mc.setScreen(new Screen_Territory());
                break;
            case 8: // 背包
                // Minecraft 原版背包
                this.onClose();
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                break;
            case 9: // 设置
                // Minecraft 原版设置
                this.onClose();
                mc.setScreen(new net.minecraft.client.gui.screens.OptionsScreen(mc.screen, mc.options));
                break;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 公告列表页面滚动
        if (selectedLeftButtonIndex == 2 && !cachedNotices.isEmpty()) {
            int totalNotices = cachedNotices.size();
            int maxScrollOffset = Math.max(0, totalNotices - VISIBLE_NOTICES);

            if (maxScrollOffset > 0) {
                int newOffset = (int) (noticeScrollOffset - delta);
                noticeScrollOffset = Math.max(0, Math.min(maxScrollOffset, newOffset));
                return true;
            }
        }

        // 任务列表页面滚动
        if (selectedLeftButtonIndex == 3) {
            if (taskShowServerTasks && selectedStageId == null) {
                // 故事任务 - 阶段列表滚动
                var storyStages = com.mo.economy_system.client.cache.ClientCacheManager.getStoryStages();
                int totalStages = storyStages.size();
                int maxScrollOffset = Math.max(0, totalStages - VISIBLE_TASKS);

                if (maxScrollOffset > 0) {
                    int newOffset = (int) (stageScrollOffset - delta);
                    stageScrollOffset = Math.max(0, Math.min(maxScrollOffset, newOffset));
                    return true;
                }
            } else if (taskShowServerTasks && selectedStageId != null) {
                // 故事任务 - 选中阶段的任务列表滚动
                var storyStages = com.mo.economy_system.client.cache.ClientCacheManager.getStoryStages();
                com.mo.economy_system.core.story_system.StoryStageData selectedStage = null;
                for (com.mo.economy_system.core.story_system.StoryStageData stage : storyStages.values()) {
                    if (String.valueOf(stage.getStageId()).equals(selectedStageId)) {
                        selectedStage = stage;
                        break;
                    }
                }

                if (selectedStage != null) {
                    java.util.List<com.mo.economy_system.core.story_system.StoryTaskData> stageTasks = selectedStage.getTasks();
                    if (stageTasks == null) stageTasks = new java.util.ArrayList<>();
                    int totalTasks = stageTasks.size();
                    int maxScrollOffset = Math.max(0, totalTasks - VISIBLE_TASKS);

                    if (maxScrollOffset > 0) {
                        int newOffset = (int) (taskScrollOffset - delta);
                        taskScrollOffset = Math.max(0, Math.min(maxScrollOffset, newOffset));
                        return true;
                    }
                }
            } else {
                // 个人任务滚动
                var playerTasks = com.mo.economy_system.client.cache.ClientCacheManager.getPlayerTasks();
                int totalTasks = playerTasks.size();
                int maxScrollOffset = Math.max(0, totalTasks - VISIBLE_TASKS);

                if (maxScrollOffset > 0) {
                    int newOffset = (int) (taskScrollOffset - delta);
                    taskScrollOffset = Math.max(0, Math.min(maxScrollOffset, newOffset));
                    return true;
                }
            }
        }

        // 帮助页面滚动
        if (selectedLeftButtonIndex == 1) {
            // 获取帮助内容的总行数
            int totalHelpLines = ServerScreenUI_PageRenderer.getHelpContentLines();
            // 计算可见行数（基于虚拟高度）
            int virtualHeight = 360;  // 基础虚拟高度
            int visibleLines = virtualHeight / HELP_LINE_HEIGHT - 2;  // 减去标题和边距
            int maxScrollOffset = Math.max(0, totalHelpLines - visibleLines);

            if (maxScrollOffset > 0) {
                int newOffset = (int) (helpScrollOffset - delta);
                helpScrollOffset = Math.max(0, Math.min(maxScrollOffset, newOffset));
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * 打开公告详情子屏幕
     */
    private void openNoticeDetail(NoticeData notice) {
        // 发送标记已读数据包
        EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_MarkNoticeReadRequest(notice.getNoticeId()));
        // 更新本地已读状态
        cachedReadNoticeIds.add(notice.getNoticeId());

        // 更新全局未读标记
        hasUnreadNoticesGlobal = false;
        for (NoticeData n : cachedNotices) {
            if (!cachedReadNoticeIds.contains(n.getNoticeId())) {
                hasUnreadNoticesGlobal = true;
                break;
            }
        }

        // 使用 openSubScreen 打开子屏幕，传递当前页面索引以便关闭后返回
        ServerScreenUI.openSubScreen(new Screen_NoticeDetail(notice, selectedLeftButtonIndex));
    }

    @Override
    public void onClose() {
        // 如果正在打开子屏幕，不调用 toggleUI()
        if (ServerScreenUI.isOpeningSubScreen()) {
            super.onClose();
            return;
        }
        // 正常关闭流程
        if (ServerScreenUI.isShowUI()) {
            ServerScreenUI.toggleUI();
        }
        super.onClose();
    }

    /**
     * 设置选中的页面索引（用于从子屏幕返回时恢复页面状态）
     */
    public void setSelectedPageIndex(int index) {
        this.selectedLeftButtonIndex = index;
    }

    @Override
    public boolean isPauseScreen() {
        return false;  // 不暂停游戏
    }

    /**
     * 渲染鼠标悬浮提示框
     * @param guiGraphics 图形上下文
     * @param mouseX 鼠标 X 坐标（屏幕坐标）
     * @param mouseY 鼠标 Y 坐标（屏幕坐标）
     */
    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 将屏幕鼠标坐标转换为虚拟坐标
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

        // 检查鼠标是否悬浮在金币框上（仅主页面显示tooltip）
        int[] goldBoxClick = pageRenderer.getGoldBoxClick();
        if (selectedLeftButtonIndex == 0 &&
            virtualMouseX >= goldBoxClick[0] && virtualMouseX <= goldBoxClick[2] &&
            virtualMouseY >= goldBoxClick[1] + rightOffsetY && virtualMouseY <= goldBoxClick[3] + rightOffsetY) {
            // 获取金币余额并创建提示文本
            int goldBalance = mc.player != null ? ClientCacheManager.getPlayerBalance(mc.player.getUUID()) : 0;
            Component tooltip = Component.literal("§e点击打开经济界面")
                .append("\n")
                .append(Component.literal("§7当前余额: §6" + ServerScreenUI_RendererUtils.formatNumber(goldBalance) + " 梦鱼币"));

            // 渲染提示框（使用屏幕坐标）
            guiGraphics.renderTooltip(mc.font, tooltip, mouseX, mouseY);
        }

        // 检查鼠标是否悬浮在领地框上（仅主页面显示tooltip）
        int[] territoryBoxClick = pageRenderer.getTerritoryBoxClick();
        if (selectedLeftButtonIndex == 0 &&
            virtualMouseX >= territoryBoxClick[0] && virtualMouseX <= territoryBoxClick[2] &&
            virtualMouseY >= territoryBoxClick[1] + rightOffsetY && virtualMouseY <= territoryBoxClick[3] + rightOffsetY) {
            // 获取领地列表并创建提示文本
            java.util.List<Territory> territories = mc.player != null ? ClientCacheManager.getTerritories(mc.player.getUUID()) : new java.util.ArrayList<>();
            Component tooltip;

            if (territories.isEmpty()) {
                tooltip = Component.literal("§e点击打开领地管理界面")
                    .append("\n")
                    .append(Component.literal("§7您还没有领地"));
            } else {
                tooltip = Component.literal("§e点击打开领地管理界面")
                    .append("\n")
                    .append(Component.literal("§7您拥有 §a" + territories.size() + " §7个领地"));
            }

            // 渲染提示框（使用屏幕坐标）
            guiGraphics.renderTooltip(mc.font, tooltip, mouseX, mouseY);
        }
    }

    // ==================== 感染度/分裂次数信息框方法 ====================

    /**
     * 获取感染度信息框的高度
     */
    private int getInfectionInfoBoxHeight() {
        int innerMargin = 6;
        int lineHeight = mc.font.lineHeight;
        // 固定高度以保持一致性
        return innerMargin * 2 + lineHeight * 6 + 5 * 3;  // 6行文字
    }

    // ==================== 公告系统方法 ====================

    /**
     * 设置公告数据（从网络包调用）
     */
    public static void setNoticeData(List<NoticeData> notices, Set<Integer> readNoticeIds) {
        // 只在公告列表真正变化时重置滚动位置
        boolean dataChanged = !cachedNotices.equals(notices);

        cachedNotices = notices != null ? new ArrayList<>(notices) : new ArrayList<>();
        cachedReadNoticeIds = readNoticeIds != null ? readNoticeIds : new java.util.HashSet<>();

        // 只在数据变化时重置滚动位置
        if (dataChanged) {
            noticeScrollOffset = 0;
        }

        // 更新全局未读标记
        hasUnreadNoticesGlobal = false;
        for (NoticeData notice : cachedNotices) {
            if (!cachedReadNoticeIds.contains(notice.getNoticeId())) {
                hasUnreadNoticesGlobal = true;
                break;
            }
        }
    }

}