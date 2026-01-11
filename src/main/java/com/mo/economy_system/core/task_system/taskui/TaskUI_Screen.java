package com.mo.economy_system.core.task_system.taskui;

import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthClientSync;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.core.task_system.TaskPlayerData;
import com.mo.economy_system.core.task_system.TaskServerData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerdata_system.Packet_RequestAllPlayerData;
import com.mo.economy_system.network.packets.task_system.Packet_SyncFullTaskData;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics; // 必须导入，解决guiGraphics解析问题
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component; // 注意：用Component而非MutableComponent
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class TaskUI_Screen extends Screen {
    //进度条ui
    // 进度条基础样式（改为横向）
    private static final int BAR_WIDTH = 50; // 横向进度条宽度
    private static final int BAR_HEIGHT = 8;//横向进度条高度
    private static final int BAR_TO_TEXT_SPACING = 8; // 类别文本与进度条间距
    private static final int BAR_BAR_SPACING = 18; // 进度条之间的纵向间距
    // 进度条颜色（深饱和鲜艳版本）
    private static final int TASK_BG_COLOR = (128 << 24) | 0x000000; // 进度条背景色
    private static final int TASK_BORDER_COLOR = 0xFFFFFFFF; // 白色边框，更明显
    private static final int TASK_LOW_COLOR = (255 << 24) | 0xFF2222; // 低进度警告色
    private static final int TASK_HEALTH_COLOR = (255 << 24) | 0xFF8888; // 血量条颜色（深鲜艳红）
    private static final int TASK_FOOD_COLOR = (255 << 24) | 0xFFCC00; // 饥饿条颜色（深金黄色）
    private static final int TASK_STRENGTH_COLOR = (255 << 24) | 0x00DD00; // 体力条颜色（深绿色）
    private static final int TASK_COURAGE_COLOR = (255 << 24) | 0xCC00FF; // 勇气条颜色（深紫色）
    private static final int TASK_INFECTION_COLOR = (255 << 24) | 0x00DD00; // 感染值条颜色（深绿色）
    // 文本样式（类别+数值）
    private static final int TASK_TEXT_COLOR = 0xFFFFFFFF; // 纯白色文本
    private static final int TASK_VALUE_OFFSET_X = 6; // 数值与进度条右侧的间距
    private static final float TASK_TEXT_SCALE = 1.0f; // 文本缩放

    //————————————————任务菜单
    // 样式参数
    private static final int BACKGROUND_ALPHA = 64; // 背景透明度（降低透明度，让现代化UI模组能添加高斯模糊）
    private static final float UI_WIDTH_PERCENT = 0.75F;
    private static final float UI_HEIGHT_PERCENT = 0.7F;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000;
    private static final int PANEL_BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x101010; // 面板背景（深灰色半透明）

    // 性能优化：缓存RGB颜色值
    private static int CACHED_DYNAMIC_BORDER_COLOR = 0xFFDDAA55;
    private static long LAST_BORDER_COLOR_UPDATE = 0;
    private static final long BORDER_COLOR_UPDATE_INTERVAL = 100; // 100ms更新一次颜色

    //按钮参数
    private static final int BTN_WIDTH = 60;
    private static final int BTN_HEIGHT = 18;
    private static final int BTN_GAP = 2;
    private static final int LOGO_BTN_WIDTH = 80;

    // 屏幕坐标
    private int screenWidth;
    private int screenHeight;
    private int screenUIWidth;
    private int screenUIHeight;
    private int uiX;
    private int uiY;

    // 布局区域定义（全屏布局）
    private float leftPanelWidthRatio = 0.18f;   // 左侧面板占屏幕宽度的18%
    private float rightPanelWidthRatio = 0.35f;  // 右侧面板占屏幕宽度的35%（增大，让进度条更长）
    private int leftPanelX;    // 左侧面板X坐标
    private int leftPanelWidth; // 左侧面板宽度
    private int rightPanelX;   // 右侧面板X坐标
    private int rightPanelWidth; // 右侧面板宽度

    // 按钮对象
    private Button serverTaskBtn;
    private Button playerTaskBtn;
    //Logo按钮
    private Button logoBtn;
    //子界面 0=无，1=服务器任务，2=个人任务
    private int showSubScreen = 0;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //————————————————————全服排行
    private static final int RANKING_WIDTH = 220; // 排行榜宽度
    private static final int RANKING_HEIGHT = 200; // 排行榜高度
    private static final int RANKING_TITLE_HEIGHT = 20; // 标题栏高度
    private static final int RANKING_LINE_HEIGHT = 18; // 每行高度
    private static final int RANKING_MAX_DISPLAY = 10; // 最多显示多少名
    // 颜色常量
    private static final int RANKING_BG_COLOR = (128 << 24) | 0x111111; // 半透黑背景
    private static final int RANKING_BORDER_COLOR = 0xFFFFD700; // 金色边框
    private static final int RANKING_TITLE_COLOR = 0xFFFFD700; // 标题金色
    private static final int RANKING_RANK_COLOR = 0xFFFFFF; // 排名默认白色
    private static final int RANKING_ONLINE_COLOR = 0x00FF00; // 在线绿色
    private static final int RANKING_OFFLINE_COLOR = 0xAAAAAA; // 离线灰色
    private static final int RANKING_LEVEL_COLOR = 0x00FFFF; // 等级青色
    private static final int RANKING_TOP3_COLOR = 0xFFFF00; // TOP3黄色
    private static final Map<UUID, PlayerRankLevelData> ALL_PLAYER_RANK_LEVEL_CACHE = new ConcurrentHashMap<>();
    // 在线玩家UUID缓存（标记在线状态）
    public static final Set<UUID> ONLINE_PLAYER_UUIDS = ConcurrentHashMap.newKeySet();

    public TaskUI_Screen() {
        super(Component.literal("任务界面"));
    }

    @Override
    protected void init() {
        super.init();

        // 计算全屏布局参数
        this.screenWidth = this.width;
        this.screenHeight = this.height;
        this.leftPanelWidth = (int) (screenWidth * leftPanelWidthRatio);
        this.rightPanelWidth = (int) (screenWidth * rightPanelWidthRatio);
        this.leftPanelX = 0;
        this.rightPanelX = screenWidth - rightPanelWidth;

        // UI中心区域（任务列表显示在中间区域）
        this.screenUIWidth = (int) (screenWidth * (1.0f - leftPanelWidthRatio - rightPanelWidthRatio));
        this.screenUIHeight = (int) (screenHeight * UI_HEIGHT_PERCENT);
        this.uiX = leftPanelWidth;
        this.uiY = (screenHeight - screenUIHeight) / 2;

        // 按钮参数
        int btnWidth = Math.min(120, leftPanelWidth - 20); // 动态宽度
        int btnHeight = 25;
        int btnGap = 8;
        int startX = leftPanelX + (leftPanelWidth - btnWidth) / 2; // 居中
        int startY = screenHeight / 3; // 从1/3处开始

        // Logo按钮（仅作显示）
        int logoBtnWidth = leftPanelWidth - 20;
        int logoBtnX = leftPanelX + 10;
        int logoBtnY = 50;

        logoBtn = Button.builder(Component.literal(""), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        showSubScreen = 0;
                    }
                })
                .pos(logoBtnX, logoBtnY)
                .size(logoBtnWidth, 40)
                .build((builder) -> {
                    return new Button(builder) {
                        @Override
                        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                            // 透明背景
                            guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                                    this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x00000000);

                            // 绘制大标题（自适应缩放，确保不超出面板）
                            String title = "§bDreaming§dFish";
                            int titleWidth = Minecraft.getInstance().font.width(title);
                            float maxScale = this.getWidth() / (float)(titleWidth + 10);
                            float titleScale = Math.min(1.5f, Math.max(0.6f, maxScale));

                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2, 0);
                            guiGraphics.pose().scale(titleScale, titleScale, 1.0f);
                            guiGraphics.drawCenteredString(Minecraft.getInstance().font,
                                    Component.literal(title), 0, -Minecraft.getInstance().font.lineHeight / 2, 0xFFFFFF);
                            guiGraphics.pose().popPose();
                        }
                    };
                });

        // 服务器任务按钮
        serverTaskBtn = Button.builder(Component.literal("故事"), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        showSubScreen = 1;
                    }
                })
                .pos(startX, startY)
                .size(btnWidth, btnHeight)
                .build((builder) -> {
                    return new Button(builder) {
                        @Override
                        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                            // 使用带颜色的透明背景
                            int bgColor = showSubScreen == 1 ? 0x600000AA : 0x40000000; // 透明度更高的紫色/黑色
                            if (this.isHoveredOrFocused()) {
                                bgColor = showSubScreen == 1 ? 0x800000AA : 0x60000000; // 悬浮时更不透明
                            }
                            guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                                    this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);

                            int borderColor = showSubScreen == 1 ? 0xFF00FFFF : getDynamicBorderColor();
                            guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                                    this.getX() + this.getWidth(), this.getY() + 2, borderColor);
                            guiGraphics.fill(RenderType.gui(), this.getX(), this.getY() + this.getHeight() - 2,
                                    this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);

                            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                                    this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2,
                                    showSubScreen == 1 ? 0xFFD700 : 0xFFFFFF);
                        }
                    };
                });

        // 个人任务按钮
        playerTaskBtn = Button.builder(Component.literal("个人任务"), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        showSubScreen = 2;
                    }
                })
                .pos(startX, startY + btnHeight + btnGap)
                .size(btnWidth, btnHeight)
                .build((builder) -> {
                    return new Button(builder) {
                        @Override
                        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                            // 使用带颜色的透明背景
                            int bgColor = showSubScreen == 2 ? 0x600000AA : 0x40000000; // 透明度更高的紫色/黑色
                            if (this.isHoveredOrFocused()) {
                                bgColor = showSubScreen == 2 ? 0x800000AA : 0x60000000; // 悬浮时更不透明
                            }
                            guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                                    this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);

                            int borderColor = showSubScreen == 2 ? 0xFF00FFFF : getDynamicBorderColor();
                            guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                                    this.getX() + this.getWidth(), this.getY() + 2, borderColor);
                            guiGraphics.fill(RenderType.gui(), this.getX(), this.getY() + this.getHeight() - 2,
                                    this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);

                            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                                    this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2,
                                    showSubScreen == 2 ? 0xFFD700 : 0xFFFFFF);
                        }
                    };
                });

        // 添加按钮
        this.addRenderableWidget(logoBtn);
        this.addRenderableWidget(serverTaskBtn);
        this.addRenderableWidget(playerTaskBtn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染左侧和右侧面板背景（半透明，让现代化UI模组添加高斯模糊）
        guiGraphics.fill(RenderType.gui(), leftPanelX, 0, leftPanelX + leftPanelWidth, screenHeight, PANEL_BG_COLOR);
        guiGraphics.fill(RenderType.gui(), rightPanelX, 0, rightPanelX + rightPanelWidth, screenHeight, PANEL_BG_COLOR);

        // 绘制面板边框（动态RGB颜色）
        int borderColor = getDynamicBorderColor();
        guiGraphics.fill(RenderType.gui(), leftPanelX + leftPanelWidth - 2, 0, leftPanelX + leftPanelWidth, screenHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), rightPanelX, 0, rightPanelX + 2, screenHeight, borderColor);

        // 渲染按钮和内容
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 根据子界面标识渲染对应内容
        switch (showSubScreen) {
            case 0:
                renderMainServerContent(guiGraphics);
                break;
            case 1:
                renderServerTaskContent(guiGraphics);
                break;
            case 2:
                renderPlayerTaskContent(guiGraphics);
                break;
            default:
                renderMainServerContent(guiGraphics);
        }
    }

    private int selectedServerTaskId = -1;
    private int selectedPlayerTaskId = -1;

    // ============================================================
    // 主界面渲染（右侧显示玩家信息）
    // ============================================================

    private void renderMainServerContent(GuiGraphics guiGraphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        int margin = 12;
        int centerX = rightPanelX + rightPanelWidth / 2;

        // 渲染玩家模型
        int modelY = renderPlayerModel(guiGraphics, player, centerX, margin);

        // 渲染玩家信息
        int currentY = renderPlayerInfo(guiGraphics, player, centerX, modelY, margin);

        // 渲染属性进度条
        renderAttributeBars(guiGraphics, player, currentY, margin);
    }

    /**
     * 渲染玩家模型
     * @return 模型底部Y坐标
     */
    private int renderPlayerModel(GuiGraphics guiGraphics, LocalPlayer player, int centerX, int margin) {
        // 缩小模型大小，确保头部不超出屏幕
        int modelSize = Math.min(rightPanelWidth - margin * 2, screenHeight / 10);
        int modelY = 50; // 下移，避免头部被挡住

        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                centerX,
                modelY + modelSize / 2,
                modelSize,
                0.0F,
                0.0F,
                player
        );

        return modelY + modelSize;
    }

    /**
     * 渲染玩家信息（等级圆圈、昵称、进度条、档案信息）
     * @return 信息区域底部Y坐标
     */
    private int renderPlayerInfo(GuiGraphics guiGraphics, LocalPlayer player, int centerX, int modelBottomY, int margin) {
        int currentY = modelBottomY + 8; // 减小间距，避免挤压底部区域

        // 绘制等级圆圈
        currentY = renderLevelCircle(guiGraphics, player, centerX, currentY);

        // 绘制昵称和状态
        currentY = renderPlayerName(guiGraphics, player, centerX, currentY);

        // 绘制等级进度条
        currentY = renderLevelProgressBar(guiGraphics, player, centerX, currentY, margin);

        // 绘制档案信息
        currentY = renderArchiveInfo(guiGraphics, player, centerX, currentY);

        return currentY;
    }

    /**
     * 绘制等级圆圈（带星星装饰）
     */
    private int renderLevelCircle(GuiGraphics guiGraphics, LocalPlayer player, int centerX, int startY) {
        String levelText = String.valueOf(PlayerLevelManager.getPlayerLevelClient(player));
        int levelWidth = this.font.width(levelText);

        // 放大等级圆圈和文字（主题元素，需要更显眼）
        float levelScale = 1.5f; // 从1.8f降低到1.5f
        int scaledLevelWidth = (int)(levelWidth * levelScale);
        int circleRadius = Math.max(scaledLevelWidth, (int)(this.font.lineHeight * levelScale)) / 2 + 6;
        int circleCenterX = centerX;
        int circleCenterY = startY + circleRadius;

        // 绘制放大的等级圆圈
        drawCircleBorder(guiGraphics, circleCenterX, circleCenterY, circleRadius, 0xFFFFFFFF);

        // 绘制放大的等级数字
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, circleCenterY, 0);
        guiGraphics.pose().scale(levelScale, levelScale, 1.0f);
        guiGraphics.pose().translate(-centerX, -circleCenterY, 0);
        guiGraphics.drawCenteredString(this.font, levelText, circleCenterX, circleCenterY - this.font.lineHeight / 2, 0xFFFFFFFF);
        guiGraphics.pose().popPose();

        // 在圆圈上方绘制星星装饰（放大）
        String starIcon = "⭐";
        float starScale = 0.7f; // 从0.8f降低
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, circleCenterY - circleRadius - 6, 0);
        guiGraphics.pose().scale(starScale, starScale, 1.0f);
        guiGraphics.pose().translate(-centerX, -(circleCenterY - circleRadius - 6), 0);
        guiGraphics.drawCenteredString(this.font, starIcon, centerX, circleCenterY - circleRadius - 6 - this.font.lineHeight / 2, 0xFFFFD700);
        guiGraphics.pose().popPose();

        return circleCenterY + circleRadius + 10; // 减小返回间距
    }

    /**
     * 绘制玩家昵称和感染状态（带状态图标）
     */
    private int renderPlayerName(GuiGraphics guiGraphics, LocalPlayer player, int centerX, int startY) {
        String nickName = player.getScoreboardName();
        int currentInfection = PlayerInfectionManager.getCurrentInfectionClient(player);

        // 根据感染状态使用不同图标
        String statusIcon = currentInfection >= 80 ? "☣ " : "💚 ";
        String infectionStatus = currentInfection >= 80 ? "§c感染者§r" : "§a幸存者§r";
        String displayName = nickName + " " + statusIcon + infectionStatus;

        // 缩小昵称（避免重叠）
        float nameScale = Math.max(0.9f, Math.min(1.2f, rightPanelWidth / 240.0f)); // 从1.4f降到1.2f
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, startY, 0);
        guiGraphics.pose().scale(nameScale, nameScale, 1.0f);
        guiGraphics.pose().translate(-centerX, -startY, 0);
        guiGraphics.drawCenteredString(this.font, displayName, centerX, startY, 0xAAAAAA);
        guiGraphics.pose().popPose();

        return startY + (int)(this.font.lineHeight * nameScale) + 6; // 减小间距
    }

    /**
     * 绘制等级进度条（带经验图标）
     */
    private int renderLevelProgressBar(GuiGraphics guiGraphics, LocalPlayer player, int centerX, int startY, int margin) {
        long currentExp = PlayerLevelManager.getPlayerExperienceClient(player);
        long nextLevelExp = PlayerLevelManager.getExperienceNeededForNextLevelClient(player);
        float expProgress = PlayerLevelManager.getExperienceProgressClient(player);

        int barWidth = rightPanelWidth - margin * 2;
        int barHeight = 5;
        int barX = centerX - barWidth / 2;
        int barY = startY;

        // 绘制进度条
        drawProgressBar(guiGraphics, barX, barY, barWidth, barHeight, expProgress, getDynamicBorderColor());

        // 经验值文本（带经验瓶图标）
        String expIcon = "✦ ";
        String expText = expIcon + currentExp + "/" + nextLevelExp;
        guiGraphics.drawCenteredString(this.font, expText, centerX, barY + barHeight + 5, getDynamicBorderColor()); // 从6改为5

        return barY + barHeight + 11; // 减小间距
    }

    /**
     * 绘制档案信息（带图标装饰）
     */
    private int renderArchiveInfo(GuiGraphics guiGraphics, LocalPlayer player, int centerX, int startY) {
        int lineHeight = 11; // 进一步减小行高
        int currentY = startY + 4; // 减小顶部间距

        // 标题（带文件夹图标）
        String titleIcon = "📁 ";
        String titleText = titleIcon + "梦鱼游戏档案";
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, centerX - titleWidth / 2, currentY, 0xFFFFD700);
        currentY += lineHeight;

        // Rank（带奖杯图标）
        String rankIcon = "🏆 ";
        String rankName = PlayerRankManager.getPlayerRankClient(player).getRankName();
        String rankText = rankIcon + "RANK: " + rankName;
        int rankWidth = this.font.width(rankText);
        // 根据Rank名称获取对应的颜色
        int rankColor = switch (rankName) {
            case "FISH" -> 0xFFFFFFFF;
            case "FISH+" -> 0xFF55FFFF;
            case "FISH++" -> 0xFFAA00;
            case "OPERATOR" -> 0xFF5555;
            default -> 0xFFFFFFFF; // NO_RANK/NULL默认白色
        };
        guiGraphics.drawString(this.font, rankText, centerX - rankWidth / 2, currentY, rankColor);
        currentY += lineHeight;

        // 称号（带星星图标）
        String titleIcon2 = "⭐ ";
        String titleText2 = titleIcon2 + "称号: " + PlayerTitleManager.getPlayerTitleClient(player).getTitleName();
        int titleWidth2 = this.font.width(titleText2);
        // 使用称号自己的颜色
        int titleColor = 0xFF000000 | PlayerTitleManager.getPlayerTitleClient(player).getColor();
        guiGraphics.drawString(this.font, titleText2, centerX - titleWidth2 / 2, currentY, titleColor);
        currentY += lineHeight + 3; // 减小底部间距

        return currentY;
    }

    /**
     * 渲染属性进度条（横向排列，带图标和数值）
     */
    private void renderAttributeBars(GuiGraphics guiGraphics, LocalPlayer player, int startY, int margin) {
        // 横向排列5个进度条
        int barCount = 5;
        int barSpacing = 12;
        int totalSpacing = barSpacing * (barCount - 1);
        int availableWidth = rightPanelWidth - margin * 2 - totalSpacing;
        int barWidth = availableWidth / barCount;
        int barHeight = 5; // 降低进度条高度

        // 计算进度条Y坐标：确保在上方文字下方，同时尽量靠近屏幕底部
        int preferredBarY = screenHeight - margin - barHeight - 45;
        int minBarY = startY + 20; // 在上方文字至少留出20像素间距
        int barY = Math.max(preferredBarY, minBarY); // 取两者中的较大值，确保不重叠

        // 获取属性值
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        int currentFood = player.getFoodData().getFoodLevel();
        int currentStrength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
        int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;
        float currentCourage = PlayerCourageManager.getCurrentCourageClient(player);
        float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
        if (maxCourage <= 0) maxCourage = 100;
        int currentInfection = PlayerInfectionManager.getCurrentInfectionClient(player);

        // 准备数值文本
        String[] valueTexts = {
            String.format("%.0f/%.0f", currentHealth, maxHealth),
            String.format("%d/20", currentFood),
            String.format("%d/%d", currentStrength, maxStrength),
            String.format("%.0f/%.0f", currentCourage, maxCourage),
            String.format("%d/100", currentInfection)
        };

        // 横向绘制5个进度条
        int barX = rightPanelX + margin;
        drawProgressBar(guiGraphics, barX, barY, barWidth, barHeight, currentHealth / maxHealth, TASK_HEALTH_COLOR & 0x00FFFFFF);
        barX += barWidth + barSpacing;
        drawProgressBar(guiGraphics, barX, barY, barWidth, barHeight, currentFood / 20.0f, TASK_FOOD_COLOR & 0x00FFFFFF);
        barX += barWidth + barSpacing;
        drawProgressBar(guiGraphics, barX, barY, barWidth, barHeight, currentStrength / (float)maxStrength, TASK_STRENGTH_COLOR & 0x00FFFFFF);
        barX += barWidth + barSpacing;
        drawProgressBar(guiGraphics, barX, barY, barWidth, barHeight, currentCourage / maxCourage, TASK_COURAGE_COLOR & 0x00FFFFFF);
        barX += barWidth + barSpacing;
        drawProgressBar(guiGraphics, barX, barY, barWidth, barHeight, currentInfection / 100.0f, TASK_INFECTION_COLOR & 0x00FFFFFF);

        // 在进度条上方显示属性图标（放大显示）
        int iconY = barY - 16; // 从-18改为-16
        barX = rightPanelX + margin + barWidth / 2;
        float iconScale = 1.0f; // 从1.2f改为1.0f，缩小图标

        // 血量图标
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(barX, iconY, 0);
        guiGraphics.pose().scale(iconScale, iconScale, 1.0f);
        guiGraphics.pose().translate(-barX, -iconY, 0);
        guiGraphics.drawCenteredString(this.font, "❤", barX, iconY, TASK_HEALTH_COLOR);
        guiGraphics.pose().popPose();

        // 饥饿图标
        barX += barWidth + barSpacing;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(barX, iconY, 0);
        guiGraphics.pose().scale(iconScale, iconScale, 1.0f);
        guiGraphics.pose().translate(-barX, -iconY, 0);
        guiGraphics.drawCenteredString(this.font, "🍖", barX, iconY, TASK_FOOD_COLOR);
        guiGraphics.pose().popPose();

        // 体力图标
        barX += barWidth + barSpacing;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(barX, iconY, 0);
        guiGraphics.pose().scale(iconScale, iconScale, 1.0f);
        guiGraphics.pose().translate(-barX, -iconY, 0);
        guiGraphics.drawCenteredString(this.font, "💪", barX, iconY, TASK_STRENGTH_COLOR);
        guiGraphics.pose().popPose();

        // 勇气图标
        barX += barWidth + barSpacing;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(barX, iconY, 0);
        guiGraphics.pose().scale(iconScale, iconScale, 1.0f);
        guiGraphics.pose().translate(-barX, -iconY, 0);
        guiGraphics.drawCenteredString(this.font, "⚡", barX, iconY, TASK_COURAGE_COLOR);
        guiGraphics.pose().popPose();

        // 感染图标
        barX += barWidth + barSpacing;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(barX, iconY, 0);
        guiGraphics.pose().scale(iconScale, iconScale, 1.0f);
        guiGraphics.pose().translate(-barX, -iconY, 0);
        guiGraphics.drawCenteredString(this.font, "☣", barX, iconY, TASK_INFECTION_COLOR);
        guiGraphics.pose().popPose();

        // 在进度条下方显示数值文本
        int textY = barY + barHeight + 4;
        barX = rightPanelX + margin + barWidth / 2;
        float textScale = 0.5f; // 从0.6f改为0.5f，进一步缩小

        for (int i = 0; i < 5; i++) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(barX, textY, 0);
            guiGraphics.pose().scale(textScale, textScale, 1.0f);
            guiGraphics.pose().translate(-barX, -textY, 0);
            guiGraphics.drawCenteredString(this.font, valueTexts[i], barX, textY, 0xFFFFFFFF);
            guiGraphics.pose().popPose();
            barX += barWidth + barSpacing;
        }
    }

    /**
     * 绘制进度条的通用方法（保留原色和发光效果，加深颜色）
     */
    private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int color) {
        // 外发光效果（彩色光晕）
        int glowColor = 0x40000000 | (color & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x - 1, y - 1, x + width + 1, y + height + 1, glowColor);

        // 背景（半透明白色）
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, 0x80FFFFFF);

        // 前景（带高光，加深颜色）
        int progressWidth = (int)(width * Math.max(0, Math.min(1, progress)));
        if (progressWidth > 2) {
            // 加深进度颜色（降低亮度，增加饱和度）
            int deepColor = 0xFF000000 | (color & 0x00FFFFFF);
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + progressWidth - 1, y + height - 1, deepColor);
            // 顶部高光
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + progressWidth - 1, y + Math.min(3, height / 2), 0x60FFFFFF);
        }

        // 边框（细边框）
        int borderColor = 0xFFFFFFFF; // 白色边框
        // 上边框
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);
        // 下边框
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, borderColor);
        // 左边框
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, borderColor);
        // 右边框
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, borderColor);
    }

    //——————————————————————————————————————————————————————————————————————————————————————————————
    private void renderPlayerTaskContent(GuiGraphics guiGraphics) {
        // 计算两栏布局参数
        int margin = 10;
        int listWidth = (int) (screenUIWidth * 0.2f);
        int detailWidth = screenUIWidth - listWidth - margin * 2;
        int listX = uiX + margin;
        int detailX = listX + listWidth + margin;
        int contentY = uiY + margin;
        int line_height = 20;
        int listContentY = contentY + line_height;

        // 获取个人任务数据
        Map<Integer, TaskPlayerData> playerTasks = Packet_SyncFullTaskData.ClientTaskCache.getClientPlayerTaskCache();

        if (playerTasks.isEmpty()) {
            guiGraphics.drawString(this.font, "懒狗腐竹还没有编写任务", listX, listContentY, 0xAAAAAA);
        } else {
            // 排序任务ID
            List<Integer> taskIds = new ArrayList<>(playerTasks.keySet());
            Collections.sort(taskIds);

            // 绘制任务列表
            for (int i = 0; i < taskIds.size(); i++) {
                int taskId = taskIds.get(i);
                TaskPlayerData task = playerTasks.get(taskId);
                int currentY = listContentY + i * line_height;

                // 绘制选中状态背景
                if (selectedPlayerTaskId == taskId) {
                    guiGraphics.fill(RenderType.gui(),
                            listX, currentY,
                            listX + listWidth, currentY + line_height - 2,
                            0x5050FFFF); // 蓝色半透背景
                }

                // 绘制序号和标题（已完成显示绿色）
                String taskText = String.format("%d. %s", taskId, task.getTaskName());
                int textColor = task.isClientPlayerFinished() ? 0x00FF00 : 0xFFFFFF;
                guiGraphics.drawString(this.font, taskText, listX + 2, currentY, textColor);
            }
        }

        //分栏竖线
        int splitLineX = listX + listWidth + (margin / 2); // 竖线X坐标（列表右侧+半个内边距）
        guiGraphics.fill(RenderType.gui(),
                splitLineX, // 竖线左边界
                contentY, // 竖线上边界（与内容区域对齐）
                splitLineX + 1, // 竖线右边界（1像素宽）
                uiY + screenUIHeight - margin, // 竖线下边界（与UI底部内边距对齐）
                BORDER_COLOR); // 使用已定义的白色边框色

        int detailContentY = contentY + line_height * 2;

        if (selectedPlayerTaskId == -1) {
            // 未选中任务时，才显示「请从左侧选择任务」放大提示
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
            guiGraphics.drawString(this.font, "请从左侧选择任务", (int)(detailX / 1.5 + 5), (int)(contentY / 1.5), 0xFFFFFF);
            guiGraphics.pose().popPose();
            // 绘制辅助提示文字
            drawStringWithWrap(guiGraphics, "个人任务是推进进度的最好方式，完成任务，获取奖励！", detailX + 5, detailContentY, screenUIWidth - splitLineX - margin,20, 0xAAAAAA);
        } else {
            TaskPlayerData selectedTask = playerTasks.get(selectedPlayerTaskId);
            if (selectedTask != null) {
                // 大字显示标题（缩放1.5倍）
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
                guiGraphics.drawString(this.font, selectedTask.getTaskName(),
                        (int) (detailX / 1.5f), (int) (detailContentY / 1.5f), 0xFFFF00);
                guiGraphics.pose().popPose();

                // 显示任务内容
                int contentStartY = detailContentY + 30;
                String[] contentLines = selectedTask.getTaskContent().split("\n");
                for (int i = 0; i < contentLines.length; i++) {
                    guiGraphics.drawString(this.font, contentLines[i],
                            detailX, contentStartY + i * line_height, 0xFFFFFF);
                }

                // 显示任务时间信息
                long startTime = selectedTask.getTaskStartTime();
                long endTime = selectedTask.getTaskEndTime();
                String timeText = String.format("有效期: %s - %s",
                        formatTime(startTime), formatTime(endTime));
                guiGraphics.drawString(this.font, timeText,
                        detailX, contentStartY + contentLines.length * line_height + 10, 0xAAAAAA);

                // 显示完成状态
                String statusText = selectedTask.isClientPlayerFinished() ? "你已完成此任务" : "你尚未完成此任务";
                guiGraphics.drawString(this.font, statusText,
                        detailX, contentStartY + (contentLines.length + 1) * line_height + 10,
                        selectedTask.isClientPlayerFinished() ? 0x00FF00 : 0xFF0000);
            } else {
                guiGraphics.drawString(this.font, "任务数据不存在", detailX, detailContentY, 0xFF0000);
            }
        }
    }
    //——————————————————————————————————————————————————————————————————————————————————————————————


    //——————————————————————————————————————————————————————————————————————————————————————————————
    //渲染服务器任务内容
    private void renderServerTaskContent(GuiGraphics guiGraphics) {
        //计算两栏布局坐标（基于主UI区域）
        int margin = 10; // 内边距
        int listWidth = (int) (screenUIWidth * 0.2f); // 左侧列表占30%宽度
        int detailWidth = screenUIWidth - listWidth - margin * 2; // 右侧详情区域宽度
        int listX = uiX + margin; // 左侧列表X坐标
        int detailX = listX + listWidth + margin; // 右侧详情X坐标
        int contentY = uiY + margin; // 内容区域起始Y坐标
        int lineHeight = 20; // 每行高度
        int listContentY = contentY + lineHeight; // 列表内容起始Y

        //遍历客户端缓存的服务器任务，渲染列表
        Map<Integer, TaskServerData> serverTasks = Packet_SyncFullTaskData.ClientTaskCache.getClientServerTaskCache();
        if (serverTasks.isEmpty()) {
            guiGraphics.drawString(this.font, "懒狗腐竹还没有编写任务", listX, listContentY, 0xAAAAAA);
        } else {
            // 按任务ID排序
            List<Integer> taskIds = new ArrayList<>(serverTasks.keySet());
            Collections.sort(taskIds);

            for (int i = 0; i < taskIds.size(); i++) {
                int taskId = taskIds.get(i);
                TaskServerData task = serverTasks.get(taskId);
                int currentY = listContentY + i * lineHeight;

                // 绘制选中状态背景
                if (selectedServerTaskId == taskId) {
                    guiGraphics.fill(RenderType.gui(),
                            listX, currentY,
                            listX + listWidth, currentY + lineHeight - 2,
                            0x5050FFFF); // 选中项蓝色半透背景
                }

                // 绘制序号和任务标题
                String taskText = String.format("%d. %s", taskId, task.getTaskName());
                guiGraphics.drawString(this.font, taskText, listX + 2, currentY,
                        task.isClientPlayerFinished() ? 0x00FF00 : 0xFFFFFF); // 已完成任务绿色
            }
        }

        //分栏竖线
        int splitLineX = listX + listWidth + (margin / 2); // 竖线X坐标（列表右侧+半个内边距）
        guiGraphics.fill(RenderType.gui(),
                splitLineX, // 竖线左边界
                contentY, // 竖线上边界（与内容区域对齐）
                splitLineX + 1, // 竖线右边界（1像素宽）
                uiY + screenUIHeight - margin, // 竖线下边界（与UI底部内边距对齐）
                BORDER_COLOR); // 使用已定义的白色边框色

        //绘制右侧详情区域
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
        if (selectedServerTaskId == -1) {
            guiGraphics.drawString(this.font, "请从左侧选择任务", (int)(detailX / 1.5 + 5), (int)(contentY / 1.5), 0xFFFFFF);
        }
        guiGraphics.pose().popPose();
        int detailContentY = contentY + lineHeight * 2; // 详情内容起始Y（预留标题空间）

        if (selectedServerTaskId == -1) {
            // 未选中任务时显示提示
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
            guiGraphics.drawString(this.font, "请从左侧选择任务", (int)(detailX / 1.5 + 5), (int)(contentY / 1.5), 0xFFFFFF);
            guiGraphics.pose().popPose();
            // 绘制辅助提示文字
            drawStringWithWrap(guiGraphics, "服务器任务需要大家共同完成，只有共同完成任务才能推进整个服务器的进度", detailX + 5, detailContentY, screenUIWidth - splitLineX - margin,20, 0xAAAAAA);
        } else {
            //显示选中任务的详情
            TaskServerData selectedTask = serverTasks.get(selectedServerTaskId);
            if (selectedTask != null) {
                //标题（大字显示，通过缩放实现）
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(1.5f, 1.5f, 1.5f); // 放大1.5倍
                guiGraphics.drawString(this.font, selectedTask.getTaskName(),
                        (int)(detailX / 1.5f), (int)(detailContentY / 1.5f), 0xFFFF00);
                guiGraphics.pose().popPose();

                // 内容
                int contentStartY = detailContentY + 30; // 标题下方留出空间
                String[] contentLines = selectedTask.getTaskContent().split("\n"); // 支持手动换行
                for (int i = 0; i < contentLines.length; i++) {
                    guiGraphics.drawString(this.font, contentLines[i],
                            detailX, contentStartY + i * lineHeight, 0xFFFFFF);
                }

                // 显示完成进度
                String progressText = String.format("完成进度: %.1f%%", selectedTask.getTaskCompletePercentage());
                guiGraphics.drawString(this.font, progressText,
                        detailX, contentStartY + contentLines.length * lineHeight + 10, 0x00FFFF);

                // 显示当前玩家完成状态
                String statusText = selectedTask.isClientPlayerFinished() ? "你已完成此任务" : "你尚未完成此任务";
                guiGraphics.drawString(this.font, statusText,
                        detailX, contentStartY + (contentLines.length + 1) * lineHeight + 10,
                        selectedTask.isClientPlayerFinished() ? 0x00FF00 : 0xFF0000);
            } else {
                guiGraphics.drawString(this.font, "任务数据不存在", detailX, detailContentY, 0xFF0000);
            }
        }
    }
    //——————————————————————————————————————————————————————————————————————————————————————————————

    private void drawRankingTitle(GuiGraphics guiGraphics, int x, int y) {
        // 绘制发光效果
        int titleGlowColor = 0x40000000 | (RANKING_BORDER_COLOR & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x - 2, y - 2, x + RANKING_WIDTH + 2, y + RANKING_TITLE_HEIGHT + 2, titleGlowColor);

        // 绘制背景
        guiGraphics.fill(RenderType.gui(), x, y, x + RANKING_WIDTH, y + RANKING_TITLE_HEIGHT, RANKING_BG_COLOR);
        // 绘制边框
        guiGraphics.fill(RenderType.gui(), x, y, x + RANKING_WIDTH, y + 1, RANKING_BORDER_COLOR); // 上边框
        guiGraphics.fill(RenderType.gui(), x, y + RANKING_TITLE_HEIGHT - 1, x + RANKING_WIDTH, y + RANKING_TITLE_HEIGHT, RANKING_BORDER_COLOR); // 下边框
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + RANKING_TITLE_HEIGHT, RANKING_BORDER_COLOR); // 左边框
        guiGraphics.fill(RenderType.gui(), x + RANKING_WIDTH - 1, y, x + RANKING_WIDTH, y + RANKING_TITLE_HEIGHT, RANKING_BORDER_COLOR); // 右边框
        // 绘制标题文字
        String title = "全服等级排行榜";
        int titleX = x + (RANKING_WIDTH - this.font.width(title)) / 2;
        int titleY = y + (RANKING_TITLE_HEIGHT - this.font.lineHeight) / 2;
        guiGraphics.drawString(this.font, title, titleX, titleY, RANKING_TITLE_COLOR);
    }

    // 绘制排行榜列表内容
    private void drawRankingList(GuiGraphics guiGraphics, int x, int y) {
        // 空数据提示
        if (ALL_PLAYER_RANK_LEVEL_CACHE.isEmpty()) {
            String emptyText = "加载全服数据中...";
            int emptyX = x + (RANKING_WIDTH - this.font.width(emptyText)) / 2;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.9f, 0.9f, 1.0f);
            guiGraphics.drawString(this.font, emptyText, (int)(emptyX/0.9f), (int)((y + 10)/0.9f), 0xAAAAAA);
            guiGraphics.pose().popPose();
            return;
        }

        //按等级降序排序
        List<Map.Entry<UUID, PlayerRankLevelData>> sortedPlayers = new ArrayList<>(ALL_PLAYER_RANK_LEVEL_CACHE.entrySet());
        sortedPlayers.sort((e1, e2) -> {
            int levelCompare = Integer.compare(e2.getValue().getLevel(), e1.getValue().getLevel());
            return levelCompare != 0 ? levelCompare :
                    e1.getValue().getPlayerName().length() - e2.getValue().getPlayerName().length();
        });

        // 渲染前20名
        int listY = y;
        int displayCount = Math.min(RANKING_MAX_DISPLAY, sortedPlayers.size());

        // 表头（
        String header = "排名 | 玩家| 总游玩时长 | 等级";
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.95f, 0.95f, 1.0f);
        guiGraphics.drawString(this.font, header, (int) ((int)(x + 2)/0.9f), (int) ((int)listY/0.9f), RANKING_TITLE_COLOR);
        guiGraphics.pose().popPose();
        listY += RANKING_LINE_HEIGHT + 1;

        for (int i = 0; i < displayCount; i++) {
            Map.Entry<UUID, PlayerRankLevelData> entry = sortedPlayers.get(i);
            PlayerRankLevelData data = entry.getValue();
            UUID playerUUID = entry.getKey();
            int currentY = listY + i * RANKING_LINE_HEIGHT;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.95f, 0.95f, 1.0f);
            float scale = 0.95f;
            int scaledX = (int)(x / scale);
            int scaledY = (int)(currentY / scale);

            // 排名
            String rankText = String.format("%d.", i + 1);
            int rankColor = i < 3 ? RANKING_TOP3_COLOR : RANKING_RANK_COLOR;
            guiGraphics.drawString(this.font, rankText, scaledX + 1, scaledY, rankColor);

            // 玩家名
            String titlePrefix = "[" + data.getTitleName() + "]";
            String fullPlayerText = titlePrefix + data.getPlayerName();
//            if (this.font.width(fullPlayerText) > 150) {
//                fullPlayerText = this.font.plainSubstrByWidth(fullPlayerText, 65) + "...";
//            }
            int nameColor = ONLINE_PLAYER_UUIDS.contains(playerUUID) ? RANKING_ONLINE_COLOR : RANKING_OFFLINE_COLOR;
            guiGraphics.drawString(this.font, fullPlayerText, scaledX + 12, scaledY, nameColor);

            //等级
            String levelText = "Lv." + data.getLevel();
            int levelX = scaledX + (int)(RANKING_WIDTH/scale) - this.font.width(levelText) - 2;
            // 总游玩时长：等级左侧
            String totalPlayTime = data.getOnlineTime();
//            if (this.font.width(totalPlayTime) > 30) {
//                totalPlayTime = this.font.plainSubstrByWidth(totalPlayTime, 25) + "...";
//            }
            // 时长坐标
            int timeX = levelX - this.font.width(totalPlayTime) - 5;
            guiGraphics.drawString(this.font, totalPlayTime, timeX, scaledY, 0xAAAAAA);

            // 绘制等级
            guiGraphics.drawString(this.font, levelText, levelX, scaledY, RANKING_LEVEL_COLOR);

            guiGraphics.pose().popPose();
        }
    }

    // 绘制完整的排行榜
    private void drawFullRanking(GuiGraphics guiGraphics) {
        // 排行榜位置：UI区域右侧，属性进度条旁边
        int rankingX = uiX + screenUIWidth - RANKING_WIDTH - 10; // 右内边距20
        int rankingY = uiY + 10; // 上内边距20

        // 绘制排行榜发光效果
        int rankingGlowColor = 0x40000000 | (RANKING_BORDER_COLOR & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), rankingX - 2, rankingY - 2, rankingX + RANKING_WIDTH + 2, rankingY + RANKING_HEIGHT + 2, rankingGlowColor);

        // 绘制排行榜背景和边框
        guiGraphics.fill(RenderType.gui(), rankingX, rankingY, rankingX + RANKING_WIDTH, rankingY + RANKING_HEIGHT, RANKING_BG_COLOR);
        // 外边框
        guiGraphics.fill(RenderType.gui(), rankingX, rankingY, rankingX + RANKING_WIDTH, rankingY + 1, RANKING_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), rankingX, rankingY + RANKING_HEIGHT - 1, rankingX + RANKING_WIDTH, rankingY + RANKING_HEIGHT, RANKING_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), rankingX, rankingY, rankingX + 1, rankingY + RANKING_HEIGHT, RANKING_BORDER_COLOR);
        guiGraphics.fill(RenderType.gui(), rankingX + RANKING_WIDTH - 1, rankingY, rankingX + RANKING_WIDTH, rankingY + RANKING_HEIGHT, RANKING_BORDER_COLOR);

        // 绘制标题栏
        drawRankingTitle(guiGraphics, rankingX, rankingY);
        // 绘制列表
        drawRankingList(guiGraphics, rankingX, rankingY + RANKING_TITLE_HEIGHT + 5);
    }

    //画圆
    private void drawCircleBorder(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        int segments = 128;
        float step = (float) (2 * Math.PI / segments);
        float angle = 0;

        int lastX = centerX + (int) (radius * Math.cos(angle));
        int lastY = centerY + (int) (radius * Math.sin(angle));
        angle += step;

        // 逐段绘制线段
        for (int i = 1; i <= segments; i++) {
            int x = centerX + (int) (Math.round(radius * Math.cos(angle))); // 四舍五入
            int y = centerY + (int) (Math.round(radius * Math.sin(angle)));

            // 用hLine/vLine补全，确保无断点
            guiGraphics.hLine(lastX, x, lastY, color);
            guiGraphics.vLine(x, lastY, y, color);

            lastX = x;
            lastY = y;
            angle += step;
        }
        // 闭合最后一段，避免圆有缺口
        int firstX = centerX + (int) (radius * Math.cos(0));
        int firstY = centerY + (int) (radius * Math.sin(0));
        guiGraphics.hLine(lastX, firstX, lastY, color);
        guiGraphics.vLine(firstX, lastY, firstY, color);
    }

    // 横向进度条绘制（int版本，支持自定义宽度）
    private void drawTaskHorizontalProgressBar(GuiGraphics guiGraphics, int x, int y, int currentValue, int maxValue, int normalColor, String category, int barWidth) {
        // 绘制左侧类别文本
        guiGraphics.drawString(this.font, category, x - this.font.width(category) - BAR_TO_TEXT_SPACING, y + (BAR_HEIGHT - this.font.lineHeight) / 2, TASK_TEXT_COLOR);

        // 绘制进度条背景
        guiGraphics.fill(x, y, x + barWidth, y + BAR_HEIGHT, TASK_BG_COLOR);
        // 计算进度（横向从左到右填充）
        float progress = Math.max(0, Math.min(1, (float) currentValue / maxValue));
        int fillWidth = (int) (barWidth * progress);
        // 绘制进度（低进度变红）
        if (fillWidth > 0) {
            int finalColor = progress < 0.2f ? TASK_LOW_COLOR : normalColor;
            guiGraphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, finalColor);
        }
        // 绘制边框（带淡色发光）
        drawTaskBorder(guiGraphics, x, y, barWidth, BAR_HEIGHT, normalColor);
        // 绘制右侧数值文本
        drawTaskProgressValueText(guiGraphics, x, y, barWidth, String.format("%d/%d", currentValue, maxValue));
    }

    // 横向进度条绘制（float版本，支持自定义宽度）
    private void drawTaskHorizontalProgressBar(GuiGraphics guiGraphics, int x, int y, float currentValue, float maxValue, int normalColor, String category, int barWidth) {
        // 绘制左侧类别文本
        guiGraphics.drawString(this.font, category, x - this.font.width(category) - BAR_TO_TEXT_SPACING, y + (BAR_HEIGHT - this.font.lineHeight) / 2, TASK_TEXT_COLOR);

        // 绘制进度条背景
        guiGraphics.fill(x, y, x + barWidth, y + BAR_HEIGHT, TASK_BG_COLOR);
        // 计算进度（横向从左到右填充）
        float progress = Math.max(0, Math.min(1, currentValue / maxValue));
        int fillWidth = (int) (barWidth * progress);
        // 绘制进度（低进度变红）
        if (fillWidth > 0) {
            int finalColor = progress < 0.2f ? TASK_LOW_COLOR : normalColor;
            guiGraphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, finalColor);
        }
        // 绘制边框（带淡色发光）
        drawTaskBorder(guiGraphics, x, y, barWidth, BAR_HEIGHT, normalColor);
        // 绘制右侧数值文本
        drawTaskProgressValueText(guiGraphics, x, y, barWidth, String.format("%.1f/%.1f", currentValue, maxValue));
    }

    // 绘制进度条边框（带鲜艳发光效果）
    private void drawTaskBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int barColor) {
        // 外发光效果（更鲜艳，使用进度条自身的颜色）
        int glowColor = 0x60000000 | (barColor & 0x00FFFFFF);
        guiGraphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, glowColor);

        // 上边框
        guiGraphics.fill(x, y, x + width, y + 1, TASK_BORDER_COLOR);
        // 下边框
        guiGraphics.fill(x, y + height - 1, x + width, y + height, TASK_BORDER_COLOR);
        // 左边框
        guiGraphics.fill(x, y, x + 1, y + height, TASK_BORDER_COLOR);
        // 右边框
        guiGraphics.fill(x + width - 1, y, x + width, y + height, TASK_BORDER_COLOR);
    }

    //绘制进度条数值文本（右侧显示）
//    private void drawTaskProgressValueText(GuiGraphics guiGraphics, int barX, int barY, String valueText) {
//        Minecraft mc = Minecraft.getInstance();
//        // 文本位置：进度条右侧 + 间距，纵向居中
//        int textX = barX + BAR_WIDTH + TASK_VALUE_OFFSET_X;
//        int textY = barY + (BAR_HEIGHT - mc.font.lineHeight) / 2;
//
//        // 绘制文本（无缩放，无阴影）
//        guiGraphics.drawString(
//                mc.font,
//                valueText,
//                textX,
//                textY,
//                TASK_TEXT_COLOR,
//                false
//        );
//    }
    //进度条上方（支持自定义宽度）
    private void drawTaskProgressValueText(GuiGraphics guiGraphics, int barX, int barY, int barWidth, String valueText) {
        Minecraft mc = Minecraft.getInstance();
        // 文本缩放比例（0.8f 表示80%大小，可按需调整为0.7f/0.9f等）
        float textScale = 0.8f;

        //计算原始文本位置
        int originalTextX = barX + barWidth / 2 - mc.font.width(valueText)/2; // 水平居中
        int originalTextY = barY - mc.font.lineHeight; // 进度条上方

        //开始缩放文本
        guiGraphics.pose().pushPose();
        // 缩放中心点设为文本中心，避免缩放后位置偏移
        guiGraphics.pose().translate(originalTextX + mc.font.width(valueText)/2 * textScale,
                originalTextY + mc.font.lineHeight/2 * textScale,
                0);
        guiGraphics.pose().scale(textScale, textScale, 1.0f); // X/Y轴缩放，Z轴不变
        // 缩放后回移，保持原始位置
        guiGraphics.pose().translate(-(originalTextX + mc.font.width(valueText)/2 * textScale),
                -(originalTextY + mc.font.lineHeight/2 * textScale),
                0);

        //绘制小号文本
        guiGraphics.drawString(
                mc.font,
                valueText,
                originalTextX,
                originalTextY,
                TASK_TEXT_COLOR,
                false
        );

        //结束缩放
        guiGraphics.pose().popPose();
    }

    private String formatTime(long timestamp) {
        try {
            // 将毫秒时间戳转换为本地时间
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    ZoneId.systemDefault()
            );
            return TIME_FORMATTER.format(dateTime);
        } catch (Exception e) {
            // 异常时返回原始时间戳
            return String.valueOf(timestamp);
        }
    }

    /**
     * 获取动态RGB变色的边框颜色（基于系统时间循环，使用缓存优化性能）
     */
    private static int getDynamicBorderColor() {
        long currentTime = System.currentTimeMillis();

        // 每100ms更新一次颜色，避免每帧计算
        if (currentTime - LAST_BORDER_COLOR_UPDATE > BORDER_COLOR_UPDATE_INTERVAL) {
            int red = (int) (Math.sin(currentTime * 0.001) * 100 + 155);
            int green = (int) (Math.sin(currentTime * 0.001 + 2) * 100 + 155);
            int blue = (int) (Math.sin(currentTime * 0.001 + 4) * 100 + 155);
            CACHED_DYNAMIC_BORDER_COLOR = 0xFF000000 | (red << 16) | (green << 8) | blue;
            LAST_BORDER_COLOR_UPDATE = currentTime;
        }

        return CACHED_DYNAMIC_BORDER_COLOR;
    }

    private void drawStringWithWrap(GuiGraphics guiGraphics, String text, int x, int y, int maxWidth, int lineHeight, int color) {
        // 分割为带格式的字符序列（自动换行）
        List<FormattedCharSequence> wrappedLines = this.font.split(Component.literal(text), maxWidth);
        // 逐行绘制（支持FormattedCharSequence）
        for (int i = 0; i < wrappedLines.size(); i++) {
            guiGraphics.drawString(this.font, wrappedLines.get(i), x, y + i * lineHeight, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 只有在显示服务器任务界面时处理点击
        if (showSubScreen == 1) {
            int margin = 10;
            int listWidth = (int) (screenUIWidth * 0.3f);
            int listX = uiX + margin;
            int listY = uiY + margin + 20; // 跳过列表标题行
            int lineHeight = 20;

            // 检查点击是否在左侧列表区域
            if (mouseX >= listX && mouseX <= listX + listWidth
                    && mouseY >= listY && mouseY <= uiY + screenUIHeight - margin) {

                // 计算点击的是第几行
                int row = (int) ((mouseY - listY) / lineHeight);
                Map<Integer, TaskServerData> serverTasks = Packet_SyncFullTaskData.ClientTaskCache.getClientServerTaskCache();
                List<Integer> taskIds = new ArrayList<>(serverTasks.keySet());
                Collections.sort(taskIds);

                // 更新选中的任务ID
                if (row >= 0 && row < taskIds.size()) {
                    selectedServerTaskId = taskIds.get(row);
                    return true;
                } else {
                    selectedServerTaskId = -1; // 点击空白区域取消选中
                }
            }
        }
        else if (showSubScreen == 2) {
            int MARGIN = 10;
            int listWidth = (int) (screenUIWidth * 0.3f);
            int listX = uiX + MARGIN;
            int listY = uiY + MARGIN + 20; // 跳过列表标题行
            int line_height = 20;

            // 检查点击是否在左侧列表区域
            if (mouseX >= listX && mouseX <= listX + listWidth
                    && mouseY >= listY && mouseY <= uiY + screenUIHeight - MARGIN) {

                // 计算点击的是第几行
                int row = (int) ((mouseY - listY) / line_height);
                Map<Integer, TaskPlayerData> playerTasks = Packet_SyncFullTaskData.ClientTaskCache.getClientPlayerTaskCache();
                List<Integer> taskIds = new ArrayList<>(playerTasks.keySet());
                Collections.sort(taskIds);

                // 更新选中的任务ID
                if (row >= 0 && row < taskIds.size()) {
                    selectedPlayerTaskId = taskIds.get(row);
                    return true;
                } else {
                    selectedPlayerTaskId = -1; // 点击空白区域取消选中
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ESC关闭界面
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC键
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null); // 关闭当前界面
        TaskUI.setShowUI(false); // 更新UI显示状态
        if (ServerInformationDisplay.isShowUI() == false) {
            ServerInformationDisplay.toggleUI();
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // 打开界面时不暂停游戏
    }

    //————————————————————————————————————————————排行榜
    @OnlyIn(Dist.CLIENT)
    private static class PlayerRankLevelData {
        private final int level;
        private final String playerName;
        private final String rankName;    // 新增：Rank名称
        private final String titleName;   // 新增：头衔/称号
        private final String onlineTime;  // 新增：在线时间（易读格式）

        public PlayerRankLevelData(int level, String playerName, String rankName, String titleName, String onlineTime) {
            this.level = level;
            this.playerName = playerName == null ? "未知玩家" : playerName;
            this.rankName = rankName == null ? "无Rank" : rankName;
            this.titleName = titleName == null ? "无头衔" : titleName;
            this.onlineTime = onlineTime == null ? "未知" : onlineTime;
        }

        // 补充getter方法
        public int getLevel() { return level; }
        public String getPlayerName() { return playerName; }
        public String getRankName() { return rankName; }
        public String getTitleName() { return titleName; }
        public String getOnlineTime() { return onlineTime; }
    }

    // 客户端缓存更新方法（供Packet_SyncPlayerData调用）
    @OnlyIn(Dist.CLIENT)
    public static void updatePlayerRankLevelCache(UUID playerUUID, String playerName, int level, String rankName, String titleName, String onlineTime) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        // 更新缓存
        ALL_PLAYER_RANK_LEVEL_CACHE.put(playerUUID, new PlayerRankLevelData(level, playerName, rankName, titleName, onlineTime));
    }

    // 清空全服缓存（用于重新请求数据）
    @OnlyIn(Dist.CLIENT)
    public static void clearAllPlayerCache() {
        ALL_PLAYER_RANK_LEVEL_CACHE.clear();
    }
}