package com.mo.economy_system.screen.server_screen.serverscreen;

import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthClientSync;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerdata_system.Packet_RequestPlayerStats;
import com.mo.economy_system.network.packets.territory_system.Packet_TerritoryDataRequest;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.playerdata.PlayerData;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.economy_system.shop.Screen_Shop;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mo.economy_system.network.packets.notice_system.Packet_NoticeListRequest;
import com.mo.economy_system.network.packets.notice_system.Packet_MarkNoticeReadRequest;
import com.mo.economy_system.server.notice.NoticeData;
import com.mo.economy_system.screen.server_screen.notice.Screen_NoticeDetail;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private static final float LEFT_PANEL_PERCENT = 0.36f;  // 左侧平板入口区占虚拟宽度的 36%
    private static final float RIGHT_PANEL_PERCENT = 0.40f;  // 右侧内容区占虚拟宽度的 40%

    // ==================== 颜色定义（平板/终端风格） ====================
    private static final int PANEL_BACKGROUND_COLOR = 0xE61B2530;  // 柔和深蓝灰内容底
    private static final int PANEL_BORDER_COLOR = 0xFF7AA8C7;      // 降低饱和度的终端蓝
    private static final int TABLET_SHELL_COLOR = 0xDC0B1118;
    private static final int TABLET_HEADER_COLOR = 0xE6131B24;
    private static final int TABLET_CONTENT_COLOR = 0xF01B2530;
    private static final int TABLET_SHADOW_COLOR = 0x55000000;
    private static final int TABLET_CARD_COLOR = 0xFF24313E;
    private static final int TABLET_CARD_HOVER_COLOR = 0xFF2C3B4A;
    private static final int TABLET_CARD_BORDER_COLOR = 0xFF344555;
    private static final int TABLET_TEXT_COLOR = 0xFFE8EDF2;
    private static final int TABLET_MUTED_TEXT_COLOR = 0xFFA7B2BE;
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
    private static final String NOTICE_UI_NAME = "梦屿广播";
    private static final String[] LEFT_BUTTON_ICONS = {"👤", "❓", "📢", "📖", "🏆", "⭐", "🛒", "🏰", "🎒", "⚙️"};
    private static final String[] LEFT_BUTTON_NAMES = {"个人档案", "新玩家帮助", NOTICE_UI_NAME, "故事进展", "玩家与排行", "服务器成就", "服务器商店", "领地", "背包", "设置"};
    private static final int[] LEFT_BUTTON_COLORS = {
        0xFFAAAAAA,  // 个人档案 - 灰色
        0xFF55FF55,  // 帮助 - 绿色
        0xFF4FC3F7,  // 梦屿广播 - 淡蓝色
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

    // 当前选中的模块索引，-1 表示一级模块桌面
    private int selectedLeftButtonIndex = -1;
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
    private static final int TASK_CARD_HEIGHT = 48;  // 任务卡片高度
    private static final int VISIBLE_TASKS = 3;  // 可见任务数量
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
        EconomySystem_NetworkManager.sendToServer(new Packet_TerritoryDataRequest());
        // 请求统计数据（群系 + 配方）
        EconomySystem_NetworkManager.sendToServer(new Packet_RequestPlayerStats());
        // 请求公告数据（用于更新感叹号状态）
        EconomySystem_NetworkManager.sendToServer(new Packet_NoticeListRequest());
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

    private float getTerminalRevealProgress() {
        long elapsed = Util.getMillis() - openTime;
        float progress = Math.min(1.0f, (float) elapsed / 520.0f);
        return 1.0f - (float) Math.pow(1.0f - progress, 3);
    }

    private void drawTerminalRevealSweep(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        float progress = getTerminalRevealProgress();
        if (progress >= 1.0f) return;
        int sweepX = x + (int) (width * progress);
        guiGraphics.fill(RenderType.gui(), sweepX - 2, y, sweepX + 1, y + height, 0x667AA8C7);
        guiGraphics.fill(RenderType.gui(), sweepX + 1, y, sweepX + 14, y + height, 0x117AA8C7);
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

        // ==================== 平板外壳（参考新闻流界面的悬浮设备感） ====================
        int tabletMarginX = Math.max(12, virtualWidth / 28);
        int tabletMarginY = Math.max(10, virtualHeight / 14);
        int tabletX = tabletMarginX;
        int tabletY = tabletMarginY;
        int tabletWidth = virtualWidth - tabletMarginX * 2;
        int tabletHeight = virtualHeight - tabletMarginY * 2;
        int headerHeight = 34;

        guiGraphics.fill(RenderType.gui(), 0, 0, virtualWidth, virtualHeight, 0x66000000);
        drawSoftRect(guiGraphics, tabletX + 4, tabletY + 5, tabletWidth, tabletHeight, 3, TABLET_SHADOW_COLOR, 0x00000000);
        drawSoftRect(guiGraphics, tabletX, tabletY, tabletWidth, tabletHeight, 3, TABLET_SHELL_COLOR, 0x66526372);
        drawSoftRect(guiGraphics, tabletX + 6, tabletY + 6, tabletWidth - 12, headerHeight, 2, TABLET_HEADER_COLOR, 0x224A5A68);
        drawSoftRect(guiGraphics, tabletX + 6, tabletY + headerHeight, tabletWidth - 12, tabletHeight - headerHeight - 6, 2, TABLET_CONTENT_COLOR, 0x22384755);

        if (selectedLeftButtonIndex >= 0) {
            drawText(guiGraphics, "<", tabletX + 18, tabletY + 15, 0xFFE8EDF6);
        }

        if (selectedLeftButtonIndex < 0) {
            drawBrandTitle(guiGraphics, tabletX + 18, tabletY + 15);
        } else {
            String pageTitle = selectedLeftButtonIndex < LEFT_BUTTON_NAMES.length
                ? LEFT_BUTTON_NAMES[selectedLeftButtonIndex]
                : "梦屿终端";
            drawText(guiGraphics, pageTitle, tabletX + 34, tabletY + 15, TABLET_TEXT_COLOR);
        }

        int onlinePlayers = mc.player != null && mc.player.connection != null ?
            mc.player.connection.getOnlinePlayers().size() : 0;
        drawTopDateTime(guiGraphics, tabletX + 6, tabletY + 12, tabletWidth - 12, 16);
        drawTabletStatusBar(guiGraphics, tabletX + tabletWidth - 148, tabletY + 12, 130, 16, onlinePlayers, 20, 20.0f);

        boolean revealContent = !skipAnimation && !isClosing && getAnimationProgress() < 1.0f;
        if (revealContent) {
            int revealRight = tabletX + 6 + (int) ((tabletWidth - 12) * getTerminalRevealProgress());
            guiGraphics.enableScissor(
                (int) ((tabletX + 6) * uiScale),
                (int) ((tabletY + headerHeight) * uiScale),
                (int) (revealRight * uiScale),
                (int) ((tabletY + tabletHeight - 6) * uiScale)
            );
        }

        if (selectedLeftButtonIndex < 0) {
            renderModuleDashboard(guiGraphics, mouseX, mouseY, tabletX + 16, tabletY + headerHeight + 16,
                tabletWidth - 32, tabletHeight - headerHeight - 28);
            if (revealContent) {
                guiGraphics.disableScissor();
                drawTerminalRevealSweep(guiGraphics, tabletX + 6, tabletY + headerHeight, tabletWidth - 12, tabletHeight - headerHeight - 6);
            }
            return;
        }

        renderModulePage(guiGraphics, mouseX, mouseY, tabletX + 16, tabletY + headerHeight + 14,
            tabletWidth - 32, tabletHeight - headerHeight - 24);
        if (revealContent) {
            guiGraphics.disableScissor();
            drawTerminalRevealSweep(guiGraphics, tabletX + 6, tabletY + headerHeight, tabletWidth - 12, tabletHeight - headerHeight - 6);
        }
        if (selectedLeftButtonIndex >= 0) return;

        // ========================================================================
        //                           左栏 (LEFT PANEL)
        // 内容：灵动岛（按钮网格） + 版本号
        // ========================================================================
        guiGraphics.pose().pushPose();

        // 关闭时应用向左偏移
        if (isClosing) {
            guiGraphics.pose().translate(-leftOffsetX, 0, 0);
        }

        // 左侧入口背景
        guiGraphics.fill(RenderType.gui(), tabletX + 8, tabletY + headerHeight + 4,
            leftPanelWidth - 6, tabletY + tabletHeight - 8, 0x10FFFFFF);

        // ==================== 左侧右边框动画 ====================
        if (!isClosing) {
            // 打开时：边框从上到下延伸
            // 高度随动画进度从 0 增加到 virtualHeight
            int leftBorderHeight = (int) (virtualHeight * animationProgress);
            if (leftBorderHeight > 0) {
                guiGraphics.fill(RenderType.gui(), leftPanelWidth - 1, tabletY + headerHeight + 6, leftPanelWidth, Math.min(tabletY + headerHeight + 6 + leftBorderHeight, tabletY + tabletHeight - 8), PANEL_BORDER_COLOR);
            }
        } else {
            // 关闭时：保持完整边框
            guiGraphics.fill(RenderType.gui(), leftPanelWidth - 1, tabletY + headerHeight + 6, leftPanelWidth, tabletY + tabletHeight - 8, PANEL_BORDER_COLOR);
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

        // ==================== 梦屿广播页面 ====================
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
            return;  // 跳过后续右栏内容渲染
        }

        // ==================== 排行榜页面 ====================
        if (selectedLeftButtonIndex == 4) {
            // 渲染排行榜页面（使用整个中间+右侧区域）
            pageRenderer.renderRankPage(guiGraphics, leftPanelWidth, virtualWidth - leftPanelWidth, mouseX, mouseY,
                virtualWidth, uiScale, mc.player);
            return;  // 跳过后续右栏内容渲染
        }

        // ==================== 成就页面 ====================
        if (selectedLeftButtonIndex == 5) {
            // 渲染成就页面（使用整个中间+右侧区域）
            pageRenderer.renderAchievementPage(guiGraphics, leftPanelWidth, virtualWidth - leftPanelWidth, mouseX, mouseY,
                virtualWidth, uiScale);
            return;  // 跳过后续右栏内容渲染
        }

        // ==================== 帮助页面 ====================
        if (selectedLeftButtonIndex == 1) {
            // 渲染帮助页面（使用整个中间+右侧区域）
            pageRenderer.renderHelpPage(guiGraphics, leftPanelWidth, virtualWidth - leftPanelWidth, mouseX, mouseY,
                virtualWidth, uiScale);
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

    private void renderModulePage(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        switch (selectedLeftButtonIndex) {
            case 0 -> renderProfilePage(guiGraphics, mouseX, mouseY, x, y, width, height);
            case 2 -> renderNoticeFeedPage(guiGraphics, x, y, width, height);
            case 3 -> renderStoryTaskPage(guiGraphics, mouseX, mouseY, x, y, width, height);
            case 1 -> renderHelpTerminalPage(guiGraphics, x, y, width, height);
            case 4 -> renderPlaceholderPage(guiGraphics, x, y, width, height, "玩家与排行", "排行面板还在接入数据");
            case 5 -> renderPlaceholderPage(guiGraphics, x, y, width, height, "服务器成就", "成就面板还在接入数据");
            default -> renderPlaceholderPage(guiGraphics, x, y, width, height, LEFT_BUTTON_NAMES[selectedLeftButtonIndex], "该模块将打开独立界面");
        }
    }

    private void renderProfilePage(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        int heroW = Math.max(138, Math.min(176, (int) (width * 0.30f)));
        int gap = 10;
        int contentH = height;
        int rightX = x + heroW + gap;
        int rightW = width - heroW - gap;

        drawSoftRect(guiGraphics, x, y, heroW, contentH, 3, 0xFF121B24, 0xFF344555);
        drawText(guiGraphics, "PLAYER", x + 12, y + 12, TABLET_MUTED_TEXT_COLOR);

        float infection = PlayerInfectionManager.getCurrentInfectionClient(player);
        String status = infection >= 100 ? "感染者" : "幸存者";
        int statusColor = infection >= 100 ? 0xFFFF6677 : 0xFF50D890;
        String playerName = player.getScoreboardName();
        if (mc.font.width(playerName) > heroW - 24) {
            playerName = ServerScreenUI_RendererUtils.truncateText(mc.font, playerName, heroW - 24 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, playerName, x + 12, y + 28, TABLET_TEXT_COLOR);
        drawSoftRect(guiGraphics, x + 12, y + 44, mc.font.width(status) + 14, 15, 2, 0x332E3C49, 0x224A5A68);
        drawText(guiGraphics, status, x + 19, y + 48, statusColor);

        PlayerData playerData = ClientCacheManager.getPlayerData(player.getUUID());
        long registrationTime = 0L;
        long totalPlayTime = 0L;
        if (playerData != null) {
            registrationTime = playerData.getRegistrationTime() > 0 ? playerData.getRegistrationTime() : playerData.getLastLoginTime();
            totalPlayTime = playerData.getTotalPlayTime();
        }
        Rank rank = PlayerRankManager.getPlayerRankClient(player);
        Title title = PlayerTitleManager.getPlayerTitleClient(player);

        int modelSize = Math.max(38, Math.min(56, contentH / 4));
        pageRenderer.renderPlayerModel(guiGraphics, 10, mouseX, mouseY, x + heroW / 2, y + contentH - 28, modelSize, uiScale);

        int level = PlayerLevelManager.getPlayerLevelClient(player);
        long exp = PlayerLevelManager.getPlayerExperienceClient(player);
        long expNeed = PlayerLevelManager.getExperienceNeededForNextLevelClient(player);
        float progress = PlayerLevelManager.getExperienceProgressClient(player);

        int levelCardH = 44;
        drawSoftRect(guiGraphics, rightX, y, rightW, levelCardH, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), rightX, y, rightX + 4, y + levelCardH, 0xFFFFB84D);
        drawText(guiGraphics, "LEVEL " + level, rightX + 12, y + 7, TABLET_TEXT_COLOR);
        drawText(guiGraphics, "EXP " + exp + "/" + expNeed, rightX + 12, y + 23, TABLET_MUTED_TEXT_COLOR);
        drawProgressBar(guiGraphics, rightX + 110, y + 22, rightW - 124, 6, progress, 0xFFFFB84D);

        int tileY = y + levelCardH + gap;
        int tileW = (rightW - gap * 2) / 3;
        int tileH = 42;
        int goldBalance = ClientCacheManager.getPlayerBalance(player.getUUID());
        java.util.List<Territory> territories = ClientCacheManager.getTerritories(player.getUUID());
        drawStatTile(guiGraphics, rightX, tileY, tileW, tileH, "梦鱼币", ServerScreenUI_RendererUtils.formatNumber(goldBalance), 0xFFFFC857);
        drawStatTile(guiGraphics, rightX + tileW + gap, tileY, tileW, tileH, "领地", String.valueOf(territories.size()), 0xFF48C78E);
        drawStatTile(guiGraphics, rightX + (tileW + gap) * 2, tileY, tileW, tileH, "探索", String.valueOf(ClientCacheManager.getExploredBiomesCount(player.getUUID())), 0xFF4FC3F7);
        int[] goldBoxClick = pageRenderer.getGoldBoxClick();
        goldBoxClick[0] = rightX;
        goldBoxClick[1] = tileY;
        goldBoxClick[2] = rightX + tileW;
        goldBoxClick[3] = tileY + tileH;
        int[] territoryBoxClick = pageRenderer.getTerritoryBoxClick();
        territoryBoxClick[0] = rightX + tileW + gap;
        territoryBoxClick[1] = tileY;
        territoryBoxClick[2] = rightX + tileW * 2 + gap;
        territoryBoxClick[3] = tileY + tileH;

        tileY += tileH + gap;
        drawStatTile(guiGraphics, rightX, tileY, tileW, tileH, "蓝图", String.valueOf(ClientCacheManager.getUnlockedRecipesCount(player.getUUID())), 0xFF8EA7FF);
        drawStatTile(guiGraphics, rightX + tileW + gap, tileY, tileW, tileH, "Rank", rank.getRankName(), getRankColor(rank.getRankLevel()));
        drawStatTile(guiGraphics, rightX + (tileW + gap) * 2, tileY, tileW, tileH, "称号", title.getTitleName(), 0xFF000000 | title.getColor());

        int attrY = tileY + tileH + gap;
        int attrBottom = y + contentH;
        int attrH = Math.max(0, attrBottom - attrY);
        drawSoftRect(guiGraphics, rightX, attrY, rightW, attrH, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
        boolean infected = ClientCacheManager.isInfected(player.getUUID());
        float respawnPoint = ClientCacheManager.getRespawnPoint(player.getUUID());
        int deathCost = infected ? 20 : 5;
        int respawnTimes = (int) (respawnPoint / deathCost);
        String respawnWarning = respawnTimes <= 0 ? "警告: 无法复活" : (respawnTimes < 2 ? "警告: 复活不足" : "");
        drawText(guiGraphics, "身体状态", rightX + 12, attrY + 9, TABLET_TEXT_COLOR);
        if (!respawnWarning.isEmpty()) {
            drawText(guiGraphics, respawnWarning, rightX + rightW - mc.font.width(respawnWarning) - 12, attrY + 9,
                respawnTimes <= 0 ? 0xFFFF6677 : 0xFFFFC857);
        }
        int innerX = rightX + 12;
        int innerY = attrY + 28;
        int columnGap = 10;
        int rowAreaH = Math.max(0, attrY + attrH - 8 - innerY);
        int rowGap = rowAreaH < 62 ? 4 : 6;
        int rowH = Math.max(8, Math.min(18, (rowAreaH - rowGap * 2) / 3));
        int barW = (rightW - 24 - columnGap) / 2;
        int strength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
        int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;
        float courage = PlayerCourageManager.getCurrentCourageClient(player);
        float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
        if (maxCourage <= 0) maxCourage = 100;
        drawMiniBar(guiGraphics, innerX, innerY, barW, rowH, "生命", player.getHealth() / player.getMaxHealth(),
            String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth()), 0xFFFF6677);
        drawMiniBar(guiGraphics, innerX + barW + columnGap, innerY, barW, rowH, "饥饿", player.getFoodData().getFoodLevel() / 20.0f,
            player.getFoodData().getFoodLevel() + "/20", 0xFFFFC857);
        drawMiniBar(guiGraphics, innerX, innerY + rowH + rowGap, barW, rowH, "体力", (float) strength / maxStrength,
            strength + "/" + maxStrength, 0xFF50D890);
        drawMiniBar(guiGraphics, innerX + barW + columnGap, innerY + rowH + rowGap, barW, rowH, "勇气", courage / maxCourage,
            String.format("%.0f/%.0f", courage, maxCourage), 0xFFB58BFF);
        drawMiniBar(guiGraphics, innerX, innerY + (rowH + rowGap) * 2, barW, rowH, "感染", infection / 100.0f,
            String.format("%.1f/100", infection), 0xFF8B5CF6);
        drawMiniBar(guiGraphics, innerX + barW + columnGap, innerY + (rowH + rowGap) * 2, barW, rowH, "分裂", respawnPoint / 100.0f,
            String.format("%.1f/%d次", respawnPoint, respawnTimes), infected ? 0xFFFF6677 : 0xFF7AA8C7);
    }

    private void renderStoryTaskPage(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        var storyStages = ClientCacheManager.getStoryStages();
        var playerTasks = ClientCacheManager.getPlayerTasks();

        String storyText = "📋 故事";
        String personalText = "📜 个人任务";
        int tabY = y;
        int tabH = 22;
        int storyW = mc.font.width(storyText) + 16;
        int personalW = mc.font.width(personalText) + 16;
        int tabGap = 8;
        drawSegmentButton(guiGraphics, x, tabY, storyW, tabH, storyText, taskShowServerTasks,
            virtualMouseX, virtualMouseY, 0xFF7AA8C7);
        drawSegmentButton(guiGraphics, x + storyW + tabGap, tabY, personalW, tabH, personalText, !taskShowServerTasks,
            virtualMouseX, virtualMouseY, 0xFF9CB7A8);

        int[] taskTabArea = pageRenderer.getTaskTabArea();
        taskTabArea[0] = x;
        taskTabArea[1] = tabY;
        taskTabArea[2] = x + storyW + tabGap + personalW;
        taskTabArea[3] = tabY + tabH;

        int contentY = y + 34;
        int contentH = height - 34;
        if (taskShowServerTasks) {
            if (selectedStageId == null) {
                renderStoryStageList(guiGraphics, virtualMouseX, virtualMouseY, x, contentY, width, contentH, storyStages);
            } else {
                renderStoryStageTasks(guiGraphics, virtualMouseX, virtualMouseY, x, contentY, width, contentH, storyStages);
            }
        } else {
            renderPersonalTaskCards(guiGraphics, virtualMouseX, virtualMouseY, x, contentY, width, contentH, playerTasks);
        }
    }

    private void drawSegmentButton(GuiGraphics guiGraphics, int x, int y, int width, int height, String text,
                                   boolean active, float mouseX, float mouseY, int accentColor) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        int bg = active ? 0xFF2D4050 : (hovered ? TABLET_CARD_HOVER_COLOR : 0xFF202B36);
        int border = active || hovered ? accentColor : TABLET_CARD_BORDER_COLOR;
        drawSoftRect(guiGraphics, x, y, width, height, 2, bg, border);
        drawCenteredText(guiGraphics, text, x + width / 2, y + 7, active ? TABLET_TEXT_COLOR : TABLET_MUTED_TEXT_COLOR);
    }

    private void renderRespawnSummary(GuiGraphics guiGraphics, LocalPlayer player, int x, int y, int width) {
        boolean infected = ClientCacheManager.isInfected(player.getUUID());
        float respawnPoint = ClientCacheManager.getRespawnPoint(player.getUUID());
        int deathCost = infected ? 20 : 5;
        int respawnTimes = (int) (respawnPoint / deathCost);
        int color = infected ? 0xFFFF6677 : (respawnTimes <= 0 ? 0xFFFF6677 : 0xFF50D890);
        drawText(guiGraphics, infected ? "感染状态: 感染者" : "感染状态: 幸存者", x, y, color);
        String respawnText = "分裂 " + String.format("%.1f/100", respawnPoint) + "  可重生 " + respawnTimes + " 次";
        if (mc.font.width(respawnText) > width) {
            respawnText = ServerScreenUI_RendererUtils.truncateText(mc.font, respawnText, width - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, respawnText, x, y + 14, TABLET_MUTED_TEXT_COLOR);
    }

    private void renderStoryStageList(GuiGraphics guiGraphics, float mouseX, float mouseY, int x, int y, int width, int height,
                                      java.util.Map<Integer, com.mo.economy_system.core.story_system.StoryStageData> storyStages) {
        int introW = Math.max(136, width / 3);
        int listX = x + introW + 10;
        int listW = width - introW - 10;
        drawSoftRect(guiGraphics, x, y, introW, height, 2, 0xFF202B36, TABLET_CARD_BORDER_COLOR);
        drawText(guiGraphics, "STORY LOG", x + 12, y + 12, TABLET_MUTED_TEXT_COLOR);
        drawText(guiGraphics, "梦屿剧情进展", x + 12, y + 30, TABLET_TEXT_COLOR);
        drawText(guiGraphics, "阶段由全服玩家共同推进", x + 12, y + 48, TABLET_MUTED_TEXT_COLOR);
        drawText(guiGraphics, "当前阶段数", x + 12, y + height - 38, TABLET_MUTED_TEXT_COLOR);
        drawText(guiGraphics, String.valueOf(storyStages.size()), x + 12, y + height - 22, TABLET_TEXT_COLOR);

        java.util.List<Integer> sortedStageIds = new java.util.ArrayList<>(storyStages.keySet());
        java.util.Collections.sort(sortedStageIds);
        int cardH = 58;
        int gap = 10;
        int visible = Math.min(VISIBLE_TASKS, Math.max(0, sortedStageIds.size() - (int) stageScrollOffset));
        int firstY = y;
        int lastY = y;

        for (int i = 0; i < visible; i++) {
            int stageIndex = i + (int) stageScrollOffset;
            if (stageIndex >= sortedStageIds.size()) break;
            var stage = storyStages.get(sortedStageIds.get(stageIndex));
            int cardY = y + i * (cardH + gap);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= cardY && mouseY <= cardY + cardH;
            renderStoryStageCard(guiGraphics, listX, cardY, listW, cardH, stage, hovered);
            lastY = cardY + cardH;
        }

        int[] stageArea = pageRenderer.getStageClickArea();
        stageArea[0] = listX;
        stageArea[1] = firstY;
        stageArea[2] = listX + listW;
        stageArea[3] = lastY;

        if (storyStages.isEmpty()) {
            drawSoftRect(guiGraphics, listX, y, listW, 86, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
            drawCenteredText(guiGraphics, "暂无故事阶段", listX + listW / 2, y + 36, TABLET_MUTED_TEXT_COLOR);
        }
    }

    private void renderStoryStageCard(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                      com.mo.economy_system.core.story_system.StoryStageData stage, boolean hovered) {
        int bg = hovered ? TABLET_CARD_HOVER_COLOR : TABLET_CARD_COLOR;
        drawSoftRect(guiGraphics, x, y, width, height, 2, bg, hovered ? PANEL_BORDER_COLOR : TABLET_CARD_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + 4, y + height, PANEL_BORDER_COLOR);

        String name = stage.getStageName();
        if (mc.font.width(name) > width - 24) {
            name = ServerScreenUI_RendererUtils.truncateText(mc.font, name, width - 24 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, name, x + 12, y + 8, TABLET_TEXT_COLOR);

        String desc = stage.getStageDescription();
        if (mc.font.width(desc) > width - 24) {
            desc = ServerScreenUI_RendererUtils.truncateText(mc.font, desc, width - 24 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, desc, x + 12, y + 24, TABLET_MUTED_TEXT_COLOR);

        int finished = 0;
        java.util.List<com.mo.economy_system.core.story_system.StoryTaskData> tasks = stage.getTasks();
        if (tasks != null) {
            for (var task : tasks) {
                if (task.isClientPlayerFinished()) finished++;
            }
        }
        int total = Math.max(1, stage.getTotalTaskCount());
        float progress = stage.getProgressPercentage();
        drawText(guiGraphics, finished + "/" + total, x + 12, y + 42, TABLET_MUTED_TEXT_COLOR);
        drawProgressBar(guiGraphics, x + 48, y + 45, width - 72, 5, progress, PANEL_BORDER_COLOR);
    }

    private void renderStoryStageTasks(GuiGraphics guiGraphics, float mouseX, float mouseY, int x, int y, int width, int height,
                                       java.util.Map<Integer, com.mo.economy_system.core.story_system.StoryStageData> storyStages) {
        var selectedStage = storyStages.values().stream()
            .filter(stage -> String.valueOf(stage.getStageId()).equals(selectedStageId))
            .findFirst()
            .orElse(null);
        if (selectedStage == null) {
            renderPlaceholderPage(guiGraphics, x, y, width, height, "阶段不存在", "返回故事列表后重新选择");
            return;
        }

        int backW = 34;
        drawSegmentButton(guiGraphics, x, y, backW, 22, "<", false, mouseX, mouseY, PANEL_BORDER_COLOR);
        int[] backArea = pageRenderer.getBackButtonArea();
        backArea[0] = x;
        backArea[1] = y;
        backArea[2] = x + backW;
        backArea[3] = y + 22;

        drawText(guiGraphics, selectedStage.getStageName(), x + 44, y + 7, TABLET_TEXT_COLOR);
        java.util.List<com.mo.economy_system.core.story_system.StoryTaskData> tasks = selectedStage.getTasks();
        if (tasks == null) tasks = new java.util.ArrayList<>();

        int cardY = y + 34;
        int gap = 8;
        int visible = Math.min(VISIBLE_TASKS, Math.max(0, tasks.size() - (int) taskScrollOffset));
        int firstY = cardY;
        int lastY = cardY;
        for (int i = 0; i < visible; i++) {
            int taskIndex = i + (int) taskScrollOffset;
            var task = tasks.get(taskIndex);
            int currentY = cardY + i * (TASK_CARD_HEIGHT + gap);
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + TASK_CARD_HEIGHT;
            renderTaskTerminalCard(guiGraphics, x, currentY, width, TASK_CARD_HEIGHT, task.getTaskName(), task.getTaskContent(),
                task.isClientPlayerFinished(), 0xFF7AA8C7, hovered, task.getFinishedPlayerCount());
            lastY = currentY + TASK_CARD_HEIGHT;
        }

        int[] taskArea = pageRenderer.getTaskClickArea();
        taskArea[0] = x;
        taskArea[1] = firstY;
        taskArea[2] = x + width;
        taskArea[3] = lastY;
    }

    private void renderPersonalTaskCards(GuiGraphics guiGraphics, float mouseX, float mouseY, int x, int y, int width, int height,
                                         java.util.Map<Integer, com.mo.economy_system.core.task_system.TaskPlayerData> playerTasks) {
        drawText(guiGraphics, "个人任务队列", x + 4, y, TABLET_TEXT_COLOR);
        int cardY = y + 20;
        int gap = 8;
        int visible = Math.min(VISIBLE_TASKS, Math.max(0, playerTasks.size() - (int) taskScrollOffset));
        int firstY = cardY;
        int lastY = cardY;
        java.util.List<com.mo.economy_system.core.task_system.TaskPlayerData> tasks = new java.util.ArrayList<>(playerTasks.values());
        for (int i = 0; i < visible; i++) {
            int taskIndex = i + (int) taskScrollOffset;
            var task = tasks.get(taskIndex);
            int currentY = cardY + i * (TASK_CARD_HEIGHT + gap);
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= currentY && mouseY <= currentY + TASK_CARD_HEIGHT;
            renderTaskTerminalCard(guiGraphics, x, currentY, width, TASK_CARD_HEIGHT, task.getTaskName(), task.getTaskContent(),
                task.isClientPlayerFinished(), 0xFF9CB7A8, hovered, 0);
            lastY = currentY + TASK_CARD_HEIGHT;
        }

        int[] taskArea = pageRenderer.getTaskClickArea();
        taskArea[0] = x;
        taskArea[1] = firstY;
        taskArea[2] = x + width;
        taskArea[3] = lastY;

        if (playerTasks.isEmpty()) {
            drawSoftRect(guiGraphics, x, cardY, width, 86, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
            drawCenteredText(guiGraphics, "暂无个人任务", x + width / 2, cardY + 36, TABLET_MUTED_TEXT_COLOR);
        }
    }

    private void renderTaskTerminalCard(GuiGraphics guiGraphics, int x, int y, int width, int height, String title, String content,
                                        boolean finished, int accent, boolean hovered, int finishedPlayers) {
        drawSoftRect(guiGraphics, x, y, width, height, 2, hovered ? TABLET_CARD_HOVER_COLOR : TABLET_CARD_COLOR,
            hovered ? accent : TABLET_CARD_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + 4, y + height, finished ? 0xFF50D890 : accent);
        String state = finished ? "DONE" : "OPEN";
        drawText(guiGraphics, state, x + 12, y + 8, finished ? 0xFF50D890 : accent);
        String displayTitle = title;
        if (mc.font.width(displayTitle) > width - 96) {
            displayTitle = ServerScreenUI_RendererUtils.truncateText(mc.font, displayTitle, width - 96 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, displayTitle, x + 54, y + 8, TABLET_TEXT_COLOR);
        String displayContent = content;
        if (mc.font.width(displayContent) > width - 68) {
            displayContent = ServerScreenUI_RendererUtils.truncateText(mc.font, displayContent, width - 68 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, displayContent, x + 12, y + 29, TABLET_MUTED_TEXT_COLOR);
        if (finishedPlayers > 0) {
            String doneText = finishedPlayers + "人完成";
            drawText(guiGraphics, doneText, x + width - mc.font.width(doneText) - 12, y + height - 12, TABLET_MUTED_TEXT_COLOR);
        }
    }

    private void renderNoticeFeedPage(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        drawText(guiGraphics, "梦屿广播", x + 4, y, TABLET_TEXT_COLOR);
        drawText(guiGraphics, "服务器公告与事件消息", x + 4, y + 14, TABLET_MUTED_TEXT_COLOR);

        int columns = width > 430 ? 2 : 1;
        int gap = 8;
        int cardW = (width - gap * (columns - 1)) / columns;
        int cardH = 58;
        int startY = y + 34;

        if (cachedNotices.isEmpty()) {
            drawSoftRect(guiGraphics, x, startY, width, 84, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
            drawCenteredText(guiGraphics, "暂无公告", x + width / 2, startY + 36, TABLET_MUTED_TEXT_COLOR);
            int[] noticeClickArea = pageRenderer.getNoticeClickArea();
            noticeClickArea[0] = noticeClickArea[1] = noticeClickArea[2] = noticeClickArea[3] = 0;
            return;
        }

        int maxRows = Math.max(1, (height - 34 + gap) / (cardH + gap));
        int maxCards = Math.max(1, maxRows * columns);
        int visible = Math.min(cachedNotices.size() - (int) noticeScrollOffset, maxCards);
        int firstY = startY;
        int lastY = startY;
        for (int i = 0; i < visible; i++) {
            int index = i + (int) noticeScrollOffset;
            NoticeData notice = cachedNotices.get(index);
            boolean isRead = cachedReadNoticeIds.contains(notice.getNoticeId());
            int col = i % columns;
            int row = i / columns;
            int cardX = x + col * (cardW + gap);
            int cardY = startY + row * (cardH + gap);
            renderNoticeFeedCard(guiGraphics, cardX, cardY, cardW, cardH, notice, isRead);
            lastY = cardY + cardH;
        }

        int[] noticeClickArea = pageRenderer.getNoticeClickArea();
        noticeClickArea[0] = x;
        noticeClickArea[1] = firstY;
        noticeClickArea[2] = x + width;
        noticeClickArea[3] = lastY;
    }

    private void renderNoticeFeedCard(GuiGraphics guiGraphics, int x, int y, int width, int height, NoticeData notice, boolean isRead) {
        int accent = isRead ? 0xFF9AA3B2 : 0xFF4FC3F7;
        drawSoftRect(guiGraphics, x, y, width, height, 2, isRead ? 0xFF202B36 : TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + 4, y + height, accent);
        drawText(guiGraphics, isRead ? "已读" : "新消息", x + 12, y + 7, accent);
        drawText(guiGraphics, ServerScreenUI_RendererUtils.formatDateTime(notice.getPublishTime()), x + width - 88, y + 7, TABLET_MUTED_TEXT_COLOR);

        String title = notice.getNoticeTitle();
        if (mc.font.width(title) > width - 24) {
            title = ServerScreenUI_RendererUtils.truncateText(mc.font, title, width - 24 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, title, x + 12, y + 22, TABLET_TEXT_COLOR);

        String content = notice.getNoticeContent();
        if (mc.font.width(content) > width - 24) {
            content = ServerScreenUI_RendererUtils.truncateText(mc.font, content, width - 24 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, content, x + 12, y + 39, TABLET_MUTED_TEXT_COLOR);
    }

    private void renderHelpTerminalPage(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int leftW = Math.max(146, width / 3);
        int gap = 10;
        int rightX = x + leftW + gap;
        int rightW = width - leftW - gap;

        drawSoftRect(guiGraphics, x, y, leftW, height, 2, 0xFF202B36, TABLET_CARD_BORDER_COLOR);
        drawText(guiGraphics, "NEW USER GUIDE", x + 12, y + 12, TABLET_MUTED_TEXT_COLOR);
        drawText(guiGraphics, "新玩家教程", x + 12, y + 30, TABLET_TEXT_COLOR);
        drawText(guiGraphics, "先看机制，再看目标", x + 12, y + 48, TABLET_MUTED_TEXT_COLOR);

        int chipY = y + 76;
        drawHelpChip(guiGraphics, x + 12, chipY, leftW - 24, "生存", "血量 / 饥饿 / 体力");
        drawHelpChip(guiGraphics, x + 12, chipY + 38, leftW - 24, "心理", "勇气过低会削弱行动");
        drawHelpChip(guiGraphics, x + 12, chipY + 76, leftW - 24, "感染", "感染值满会改变状态");
        drawHelpChip(guiGraphics, x + 12, chipY + 114, leftW - 24, "故事", "全服共同推进阶段");

        int cardH = 56;
        int rowGap = 8;
        int colGap = 8;
        int colW = (rightW - colGap) / 2;
        String[][] cards = {
            {"体力与血量", "等级会提升身体上限；绿色进度代表体力，受行动消耗影响。"},
            {"勇气机制", "黑暗、怪物和附近死亡会压低勇气；光亮和同伴能帮助恢复。"},
            {"等级成长", "探索群系、击杀怪物、完成原版挑战和隐藏成就可提升等级。"},
            {"故事阶段", "剧情不是单人按钮，阶段推进取决于全服玩家的调查与选择。"},
            {"感染与重生", "受到伤害或靠近感染者会增加感染；感染者死亡代价更高。"},
            {"分裂次数", "分裂不足时无法正常重生，需要其他玩家救助你。"}
        };
        int[] colors = {0xFF50D890, 0xFFB58BFF, 0xFFFFB84D, 0xFF7AA8C7, 0xFF8B5CF6, 0xFFFF6677};
        for (int i = 0; i < cards.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int cardX = rightX + col * (colW + colGap);
            int cardY = y + row * (cardH + rowGap);
            drawHelpCard(guiGraphics, cardX, cardY, colW, cardH, cards[i][0], cards[i][1], colors[i]);
        }
    }

    private void drawHelpChip(GuiGraphics guiGraphics, int x, int y, int width, String title, String hint) {
        drawSoftRect(guiGraphics, x, y, width, 30, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
        drawText(guiGraphics, title, x + 8, y + 6, TABLET_TEXT_COLOR);
        String text = hint;
        if (mc.font.width(text) > width - 16) {
            text = ServerScreenUI_RendererUtils.truncateText(mc.font, text, width - 16 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, text, x + 8, y + 18, TABLET_MUTED_TEXT_COLOR);
    }

    private void drawHelpCard(GuiGraphics guiGraphics, int x, int y, int width, int height, String title, String body, int accent) {
        drawSoftRect(guiGraphics, x, y, width, height, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + 4, y + height, accent);
        drawText(guiGraphics, title, x + 12, y + 8, TABLET_TEXT_COLOR);
        String line = body;
        if (mc.font.width(line) > width - 24) {
            line = ServerScreenUI_RendererUtils.truncateText(mc.font, line, width - 24 - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, line, x + 12, y + 28, TABLET_MUTED_TEXT_COLOR);
    }

    private void renderPlaceholderPage(GuiGraphics guiGraphics, int x, int y, int width, int height, String title, String hint) {
        drawSoftRect(guiGraphics, x, y, width, height, 3, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
        drawCenteredText(guiGraphics, title, x + width / 2, y + height / 2 - 12, TABLET_TEXT_COLOR);
        drawCenteredText(guiGraphics, hint, x + width / 2, y + height / 2 + 6, TABLET_MUTED_TEXT_COLOR);
    }

    private void drawProfileTimeRow(GuiGraphics guiGraphics, int x, int y, int width, String label, String value, int accentColor) {
        drawSoftRect(guiGraphics, x, y, width, 17, 2, 0x6624313E, 0x22344555);
        guiGraphics.fill(RenderType.gui(), x, y, x + 3, y + 17, accentColor);
        drawText(guiGraphics, label, x + 8, y + 5, TABLET_MUTED_TEXT_COLOR);
        int valueX = x + 38;
        int valueMaxWidth = Math.max(16, width - 44);
        String displayValue = value;
        if (mc.font.width(displayValue) > valueMaxWidth) {
            displayValue = ServerScreenUI_RendererUtils.truncateText(mc.font, displayValue, valueMaxWidth - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, displayValue, valueX, y + 5, TABLET_TEXT_COLOR);
    }

    private void drawIdentityLine(GuiGraphics guiGraphics, int x, int y, int width, String label, String value, int accentColor) {
        drawSoftRect(guiGraphics, x, y, width, 16, 2, 0x3324313E, 0x22344555);
        guiGraphics.fill(RenderType.gui(), x, y, x + 3, y + 16, accentColor);
        drawText(guiGraphics, label, x + 8, y + 5, TABLET_MUTED_TEXT_COLOR);
        int valueX = x + 44;
        int valueMaxWidth = Math.max(16, width - 50);
        String displayValue = value;
        if (mc.font.width(displayValue) > valueMaxWidth) {
            displayValue = ServerScreenUI_RendererUtils.truncateText(mc.font, displayValue, valueMaxWidth - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, displayValue, valueX, y + 5, accentColor);
    }

    private String formatProfileDate(long epochMs) {
        if (epochMs <= 0) {
            return "--";
        }
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        return time.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    private String formatProfileDateTime(long epochMs) {
        if (epochMs <= 0) {
            return "--";
        }
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        return time.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
    }

    private String formatPlayDuration(long millis) {
        if (millis <= 0) {
            return "0分";
        }
        long totalMinutes = millis / 60000L;
        long days = totalMinutes / 1440L;
        long hours = (totalMinutes % 1440L) / 60L;
        long minutes = totalMinutes % 60L;
        if (days > 0) {
            return days + "天" + hours + "时";
        }
        if (hours > 0) {
            return hours + "时" + minutes + "分";
        }
        return Math.max(1, minutes) + "分";
    }

    private void drawStatTile(GuiGraphics guiGraphics, int x, int y, int width, int height, String label, String value, int accentColor) {
        drawSoftRect(guiGraphics, x, y, width, height, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), x, y, x + 4, y + height, accentColor);
        drawText(guiGraphics, label, x + 12, y + 9, TABLET_MUTED_TEXT_COLOR);
        int valueMaxWidth = Math.max(18, width - 24);
        String displayValue = value;
        if (mc.font.width(displayValue) > valueMaxWidth) {
            displayValue = ServerScreenUI_RendererUtils.truncateText(mc.font, displayValue, valueMaxWidth - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, displayValue, x + 12, y + 27, TABLET_TEXT_COLOR);
    }

    private void drawMiniBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                             String label, float progress, String value, int color) {
        int valueMaxWidth = Math.max(18, width - mc.font.width(label) - 10);
        String displayValue = value;
        if (mc.font.width(displayValue) > valueMaxWidth) {
            displayValue = ServerScreenUI_RendererUtils.truncateText(mc.font, displayValue, valueMaxWidth - mc.font.width("...")) + "...";
        }
        int valueWidth = mc.font.width(displayValue);
        drawText(guiGraphics, label, x, y, TABLET_MUTED_TEXT_COLOR);
        drawText(guiGraphics, displayValue, x + width - valueWidth, y, TABLET_TEXT_COLOR);
        drawProgressBar(guiGraphics, x, y + height - 6, width, 5, progress, color);
    }

    private void drawSoftRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        if (width <= 0 || height <= 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(width / 2, height / 2)));
        int right = x + width;
        int bottom = y + height;

        if (r <= 0) {
            guiGraphics.fill(RenderType.gui(), x, y, right, bottom, fillColor);
        } else {
            guiGraphics.fill(RenderType.gui(), x + r, y, right - r, bottom, fillColor);
            guiGraphics.fill(RenderType.gui(), x, y + r, right, bottom - r, fillColor);

            if (r >= 1) {
                guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + r, y + r, fillColor);
                guiGraphics.fill(RenderType.gui(), right - r, y + 1, right - 1, y + r, fillColor);
                guiGraphics.fill(RenderType.gui(), x + 1, bottom - r, x + r, bottom - 1, fillColor);
                guiGraphics.fill(RenderType.gui(), right - r, bottom - r, right - 1, bottom - 1, fillColor);
            }
        }

        if ((borderColor >>> 24) == 0) return;
        if (r <= 0) {
            guiGraphics.fill(RenderType.gui(), x, y, right, y + 1, borderColor);
            guiGraphics.fill(RenderType.gui(), x, bottom - 1, right, bottom, borderColor);
            guiGraphics.fill(RenderType.gui(), x, y, x + 1, bottom, borderColor);
            guiGraphics.fill(RenderType.gui(), right - 1, y, right, bottom, borderColor);
            return;
        }

        guiGraphics.fill(RenderType.gui(), x + r, y, right - r, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x + r, bottom - 1, right - r, bottom, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + r, x + 1, bottom - r, borderColor);
        guiGraphics.fill(RenderType.gui(), right - 1, y + r, right, bottom - r, borderColor);

        guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + 2, y + 2, borderColor);
        guiGraphics.fill(RenderType.gui(), right - 2, y + 1, right - 1, y + 2, borderColor);
        guiGraphics.fill(RenderType.gui(), x + 1, bottom - 2, x + 2, bottom - 1, borderColor);
        guiGraphics.fill(RenderType.gui(), right - 2, bottom - 2, right - 1, bottom - 1, borderColor);
    }

    private void drawText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.drawString(mc.font, text, x, y, color, false);
    }

    private void drawBrandTitle(GuiGraphics guiGraphics, int x, int y) {
        drawText(guiGraphics, "Dreaming", x, y, 0xFFB58BFF);
        int fishX = x + mc.font.width("Dreaming");
        drawText(guiGraphics, "Fish", fishX, y, 0xFF4FC3F7);
        drawText(guiGraphics, " Terminal", fishX + mc.font.width("Fish"), y, 0xFFFFC857);
    }

    private void drawTopDateTime(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
        int textWidth = mc.font.width(dateTime);
        int centerX = x + width / 2;
        int boxW = textWidth + 22;
        drawSoftRect(guiGraphics, centerX - boxW / 2, y - 1, boxW, height + 2, 2, 0x221B2530, 0x22344555);
        drawText(guiGraphics, dateTime, centerX - textWidth / 2, y + 4, TABLET_TEXT_COLOR);
    }

    private void drawCenteredText(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        drawText(guiGraphics, text, centerX - mc.font.width(text) / 2, y, color);
    }

    /**
     * 绘制左侧按钮区域 + 服务器信息区域
     */
    private void renderModuleDashboard(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        int bottomBarHeight = 34;
        int bottomGap = 10;
        int gridHeight = Math.max(120, height - bottomBarHeight - bottomGap);
        int gridY = y;
        int gap = 8;
        int unitW = (width - gap * 5) / 6;
        int unitH = Math.max(38, (gridHeight - gap * 3) / 4);
        int[][] tileLayout = {
            {0, 0, 2, 2}, {4, 0, 2, 1}, {2, 0, 2, 1},
            {2, 1, 2, 2}, {4, 1, 2, 1}, {4, 2, 2, 1},
            {0, 2, 2, 1}, {0, 3, 2, 1}, {2, 3, 2, 1}, {4, 3, 2, 1}
        };

        for (int i = 0; i < LEFT_BUTTON_ICONS.length; i++) {
            int[] tile = tileLayout[i];
            int cardX = x + tile[0] * (unitW + gap);
            int cardY = gridY + tile[1] * (unitH + gap);
            int cardWidth = unitW * tile[2] + gap * (tile[2] - 1);
            int cardHeight = unitH * tile[3] + gap * (tile[3] - 1);

            leftButtonX1[i] = cardX;
            leftButtonY1[i] = cardY;
            leftButtonX2[i] = cardX + cardWidth;
            leftButtonY2[i] = cardY + cardHeight;

            boolean isHovered = virtualMouseX >= cardX && virtualMouseX <= cardX + cardWidth &&
                virtualMouseY >= cardY && virtualMouseY <= cardY + cardHeight;
            boolean hasUnread = (i == 2 && hasUnreadNoticesGlobal) ||
                (i == 3 && com.mo.economy_system.client.cache.ClientCacheManager.hasUnfinishedTasks());

            float reveal = getDashboardTileReveal(cardX, x, width);
            if (reveal <= 0.0f) continue;
            if (reveal < 1.0f) {
                guiGraphics.enableScissor(
                    (int) (cardX * uiScale),
                    (int) (cardY * uiScale),
                    (int) ((cardX + cardWidth * reveal) * uiScale),
                    (int) ((cardY + cardHeight) * uiScale)
                );
            }
            drawDashboardModuleCard(guiGraphics, cardX, cardY, cardWidth, cardHeight,
                LEFT_BUTTON_ICONS[i], LEFT_BUTTON_NAMES[i], LEFT_BUTTON_COLORS[i], isHovered, hasUnread);
            if (reveal < 1.0f) {
                guiGraphics.disableScissor();
            }
        }

        drawDashboardPlayerBar(guiGraphics, x, y + height - bottomBarHeight, width, bottomBarHeight);
    }

    private float getDashboardTileReveal(int tileX, int dashboardX, int dashboardWidth) {
        if (skipAnimation || isClosing) return 1.0f;
        long elapsed = Util.getMillis() - openTime;
        float positionDelay = (float) (tileX - dashboardX) / Math.max(1, dashboardWidth) * 180.0f;
        float progress = (elapsed - positionDelay) / 360.0f;
        progress = Math.max(0.0f, Math.min(1.0f, progress));
        return 1.0f - (float) Math.pow(1.0f - progress, 3);
    }

    private void drawDashboardModuleCard(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                         String icon, String label, int accentColor, boolean isHovered, boolean hasUnread) {
        int bgColor = isHovered ? TABLET_CARD_HOVER_COLOR : TABLET_CARD_COLOR;
        int borderColor = isHovered ? (0xFF000000 | (accentColor & 0x00FFFFFF)) : TABLET_CARD_BORDER_COLOR;
        drawSoftRect(guiGraphics, x, y, width, height, 2, bgColor, borderColor);

        int accent = 0xFF000000 | (accentColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x, y, x + 4, y + height, accent);
        drawText(guiGraphics, icon, x + 14, y + 12, accent);

        String displayLabel = label;
        int maxLabelWidth = width - 28;
        if (mc.font.width(displayLabel) > maxLabelWidth) {
            displayLabel = ServerScreenUI_RendererUtils.truncateText(mc.font, displayLabel, maxLabelWidth - mc.font.width("...")) + "...";
        }
        drawText(guiGraphics, displayLabel, x + 14, y + height - 20, TABLET_TEXT_COLOR);

        if (hasUnread) {
            drawSoftRect(guiGraphics, x + width - 14, y + 10, 7, 7, 2, 0xFFFF7A88, 0xFFFFB8C0);
        }
    }

    private void drawDashboardPlayerBar(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        LocalPlayer player = mc.player;
        drawSoftRect(guiGraphics, x, y, width, height, 2, 0xFF202B36, TABLET_CARD_BORDER_COLOR);
        if (player == null) return;

        int avatarSize = Math.max(18, height - 12);
        int avatarX = x + 10;
        int avatarY = y + (height - avatarSize) / 2;
        PlayerInfo playerInfo = player.connection != null ? player.connection.getPlayerInfo(player.getUUID()) : null;
        if (playerInfo != null) {
            PlayerFaceRenderer.draw(guiGraphics, playerInfo.getSkin(), avatarX, avatarY, avatarSize);
        } else {
            drawSoftRect(guiGraphics, avatarX, avatarY, avatarSize, avatarSize, 2, TABLET_CARD_COLOR, TABLET_CARD_BORDER_COLOR);
        }

        PlayerData playerData = ClientCacheManager.getPlayerData(player.getUUID());
        long registrationTime = 0L;
        long totalPlayTime = 0L;
        if (playerData != null) {
            registrationTime = playerData.getRegistrationTime() > 0 ? playerData.getRegistrationTime() : playerData.getLastLoginTime();
            totalPlayTime = playerData.getTotalPlayTime();
        }

        String playerText = player.getScoreboardName();
        int textY = y + (height - mc.font.lineHeight) / 2;
        int cursorX = avatarX + avatarSize + 10;
        drawText(guiGraphics, playerText, cursorX, textY, TABLET_TEXT_COLOR);

        int registerX = x + Math.max(width / 3, 170);
        drawText(guiGraphics, "注册", registerX, textY, TABLET_MUTED_TEXT_COLOR);
        drawText(guiGraphics, formatProfileDateTime(registrationTime), registerX + 32, textY, TABLET_TEXT_COLOR);

        int playX = x + Math.max(width * 2 / 3, registerX + 128);
        drawText(guiGraphics, "游玩时长", playX, textY, TABLET_MUTED_TEXT_COLOR);
        drawText(guiGraphics, formatPlayDuration(totalPlayTime), playX + 56, textY, TABLET_TEXT_COLOR);
    }

    /**
     * 绘制左侧按钮区域 + 服务器信息区域
     */
    private void renderLeftDynamicIsland(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // ==================== 布局参数 ====================
        int sideMargin = 14;
        int cardSpacing = 6;
        int columnCount = 2;
        int cardWidth = (leftPanelWidth - sideMargin * 2 - cardSpacing) / columnCount;
        int cardHeight = 42;

        int totalButtons = LEFT_BUTTON_ICONS.length;

        // 按钮位置：位于平板内容区内，参考新闻流卡片排布
        int headerY = Math.max(44, virtualHeight / 10);
        int buttonStartY = headerY + 30;

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

        String terminalTitle = "DreamingFish Terminal";
        guiGraphics.drawString(mc.font, terminalTitle, sideMargin, headerY, 0xFF20242C);
        guiGraphics.drawString(mc.font, "选择一个服务器模块", sideMargin, headerY + mc.font.lineHeight + 4, 0xFF687080);

        for (int i = 0; i < totalButtons; i++) {
            int row = i / columnCount;
            int column = i % columnCount;
            int buttonX = sideMargin + column * (cardWidth + cardSpacing);
            int buttonY = currentButtonStartY + row * (cardHeight + cardSpacing);

            // 存储点击区域
            leftButtonX1[i] = buttonX;
            leftButtonY1[i] = buttonY;
            leftButtonX2[i] = buttonX + cardWidth;
            leftButtonY2[i] = buttonY + cardHeight;

            boolean isSelected = (i == selectedLeftButtonIndex);
            boolean isHovered = (virtualMouseX >= buttonX && virtualMouseX <= buttonX + cardWidth &&
                                virtualMouseY >= buttonY && virtualMouseY <= buttonY + cardHeight);

            // 检查是否有未读/未完成内容
            boolean hasUnread = false;
            if (i == 2 && hasUnreadNoticesGlobal) {
                // 公告按钮：未读公告（使用全局标记）
                hasUnread = true;
            } else if (i == 3) {
                // 故事按钮：未完成任务
                hasUnread = com.mo.economy_system.client.cache.ClientCacheManager.hasUnfinishedTasks();
            }

            drawTabletLauncherCard(guiGraphics, buttonX, buttonY, cardWidth, cardHeight,
                isSelected, isHovered, LEFT_BUTTON_ICONS[i], LEFT_BUTTON_NAMES[i], LEFT_BUTTON_COLORS[i], hasUnread);
        }

        // ==================== 绘制服务器信息区域（版本号上方） ====================
        int infoHeight = 34;
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

        drawTabletServerInfo(guiGraphics, sideMargin, infoY, leftPanelWidth - sideMargin * 2, infoHeight,
            infoAnimOffsetY, onlinePlayers, maxPlayers, tps);
    }

    /**
     * 绘制平板首页功能卡片。
     */
    private void drawTabletLauncherCard(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                        boolean isSelected, boolean isHovered, String icon, String label,
                                        int accentColor, boolean hasUnread) {
        int bgColor = isSelected ? 0xFFFFFFFF : (isHovered ? 0xFFF8FAFF : 0xFFF1F3F8);
        int borderColor = isSelected ? (0xFF000000 | (accentColor & 0x00FFFFFF)) : (isHovered ? 0xFFB8C3D6 : 0xFFE0E4EC);

        drawSoftRect(guiGraphics, x, y, width, height, 2, bgColor, borderColor);
        guiGraphics.fill(RenderType.gui(), x + 1, y + height - 3, x + width - 1, y + height - 1,
            0xFF000000 | (accentColor & 0x00FFFFFF));

        int iconColor = 0xFF000000 | (accentColor & 0x00FFFFFF);
        guiGraphics.drawString(mc.font, icon, x + 8, y + 8, iconColor);

        String displayLabel = label;
        int maxLabelWidth = width - 14;
        if (mc.font.width(displayLabel) > maxLabelWidth) {
            displayLabel = ServerScreenUI_RendererUtils.truncateText(mc.font, displayLabel, maxLabelWidth - mc.font.width("...")) + "...";
        }
        guiGraphics.drawString(mc.font, displayLabel, x + 8, y + 24, isSelected ? 0xFF161A22 : 0xFF343946);

        if (hasUnread) {
            drawSoftRect(guiGraphics, x + width - 12, y + 6, 6, 6, 2, 0xFFFF7A88, 0xFFFFB8C0);
        }
    }

    /**
     * 绘制平板底部状态条。
     */
    private void drawTabletServerInfo(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                      int offsetY, int onlinePlayers, int maxPlayers, float tps) {
        y += offsetY;
        drawSoftRect(guiGraphics, x, y, width, height, 2, 0xFF202B36, TABLET_CARD_BORDER_COLOR);
        drawText(guiGraphics, "ONLINE", x + 8, y + 6, TABLET_MUTED_TEXT_COLOR);
        drawText(guiGraphics, onlinePlayers + "/" + maxPlayers, x + 8, y + 18, TABLET_TEXT_COLOR);

        String tpsText = String.format("TPS %.1f", tps);
        int tpsWidth = mc.font.width(tpsText);
        drawText(guiGraphics, tpsText, x + width - tpsWidth - 8, y + 18, TABLET_TEXT_COLOR);
    }

    private void drawTabletStatusBar(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                     int onlinePlayers, int maxPlayers, float tps) {
        drawSoftRect(guiGraphics, x, y, width, height, 2, 0x332E3C49, 0x224A5A68);
        drawText(guiGraphics, "在线", x + 8, y + 4, TABLET_MUTED_TEXT_COLOR);
        String onlineText = onlinePlayers + "/" + maxPlayers;
        drawText(guiGraphics, onlineText, x + 38, y + 4, TABLET_TEXT_COLOR);
        String tpsText = String.format("TPS %.1f", tps);
        drawText(guiGraphics, tpsText, x + width - mc.font.width(tpsText) - 8, y + 4, TABLET_TEXT_COLOR);
    }

    private String formatWorldTime() {
        if (mc.level == null) {
            return "--:--";
        }
        long dayTime = mc.level.getDayTime() % 24000L;
        int hour = (int) ((dayTime / 1000L + 6L) % 24L);
        int minute = (int) ((dayTime % 1000L) * 60L / 1000L);
        return String.format("%02d:%02d", hour, minute);
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
        // ESC 键：二级界面返回终端主页，主页关闭 UI
        if (keyCode == 256) {
            if (selectedLeftButtonIndex >= 0) {
                selectedLeftButtonIndex = -1;
                selectedStageId = null;
                taskScrollOffset = 0;
                stageScrollOffset = 0;
                return true;
            }
            if (isClosing) return true;  // 如果已经在关闭中，不再响应
            isClosing = true;
            closeTime = Util.getMillis();
            return true;
        }

        // 任务页面：Q/E 键切换故事/个人任务
        if (selectedLeftButtonIndex == 3) {
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

        int tabletMarginX = Math.max(12, virtualWidth / 28);
        int tabletMarginY = Math.max(10, virtualHeight / 14);
        if (selectedLeftButtonIndex >= 0 &&
            virtualMouseX >= tabletMarginX + 12 && virtualMouseX <= tabletMarginX + 30 &&
            virtualMouseY >= tabletMarginY + 10 && virtualMouseY <= tabletMarginY + 30) {
            selectedLeftButtonIndex = -1;
            selectedStageId = null;
            taskScrollOffset = 0;
            stageScrollOffset = 0;
            return true;
        }

        // ==================== 检查一级模块按钮点击 ====================
        if (selectedLeftButtonIndex < 0) {
            for (int i = 0; i < LEFT_BUTTON_ICONS.length; i++) {
                if (virtualMouseX >= leftButtonX1[i] && virtualMouseX <= leftButtonX2[i] &&
                    virtualMouseY >= leftButtonY1[i] && virtualMouseY <= leftButtonY2[i]) {
                    selectedLeftButtonIndex = i;
                    handleLeftButtonClick(i);
                    return true;
                }
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
        if (selectedLeftButtonIndex == 2 && !cachedNotices.isEmpty()) {  // 梦屿广播页面
            int[] noticeArea = pageRenderer.getNoticeClickArea();
            if (virtualMouseX >= noticeArea[0] && virtualMouseX <= noticeArea[2] &&
                virtualMouseY >= noticeArea[1] + rightOffsetY && virtualMouseY <= noticeArea[3] + rightOffsetY) {
                // 计算点击的是哪个公告卡片
                int cardMargin = 8;
                int cardHeight = 58;
                int areaWidth = noticeArea[2] - noticeArea[0];
                int columns = areaWidth > 430 ? 2 : 1;
                int cardWidth = (areaWidth - cardMargin * (columns - 1)) / columns;
                int relativeX = (int) virtualMouseX - noticeArea[0];
                int relativeY = (int) virtualMouseY - (noticeArea[1] + rightOffsetY);
                int clickedColumn = relativeX / (cardWidth + cardMargin);
                int clickedRow = relativeY / (cardHeight + cardMargin);
                int clickedCardIndex = clickedRow * columns + clickedColumn;

                int totalNotices = cachedNotices.size();
                int maxCards = Math.min((noticeArea[3] - noticeArea[1] + cardMargin) / (cardHeight + cardMargin) * columns, totalNotices);

                if (clickedColumn >= 0 && clickedColumn < columns && clickedCardIndex >= 0 && clickedCardIndex < maxCards) {
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

                    int stageCardHeight = 58;
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
                                    EconomySystem_NetworkManager.sendToServer(
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
            case 2: // 梦屿广播
                // 请求公告列表
                EconomySystem_NetworkManager.sendToServer(new Packet_NoticeListRequest());
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
                mc.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(mc.screen, mc.options));
                break;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 公告列表页面滚动
        if (selectedLeftButtonIndex == 2 && !cachedNotices.isEmpty()) {
            int totalNotices = cachedNotices.size();
            int maxScrollOffset = Math.max(0, totalNotices - VISIBLE_NOTICES);

            if (maxScrollOffset > 0) {
                int newOffset = (int) (noticeScrollOffset - scrollY);
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
                    int newOffset = (int) (stageScrollOffset - scrollY);
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
                        int newOffset = (int) (taskScrollOffset - scrollY);
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
                    int newOffset = (int) (taskScrollOffset - scrollY);
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
                int newOffset = (int) (helpScrollOffset - scrollY);
                helpScrollOffset = Math.max(0, Math.min(maxScrollOffset, newOffset));
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * 打开公告详情子屏幕
     */
    private void openNoticeDetail(NoticeData notice) {
        // 发送标记已读数据包
        EconomySystem_NetworkManager.sendToServer(new Packet_MarkNoticeReadRequest(notice.getNoticeId()));
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
