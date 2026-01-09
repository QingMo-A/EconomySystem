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
    //样式参数
    private static final int BACKGROUND_ALPHA = 128; //背景透明度
    private static final float UI_WIDTH_PERCENT = 0.75F; //宽度百分比
    private static final float UI_HEIGHT_PERCENT = 0.7F; //高度百分比
    private static final int BORDER_COLOR = 0xFFFFFFFF; // 白色边框（用于分隔线等）
    private static final int BG_COLOR = (BACKGROUND_ALPHA << 24) | 0x000000; //半透明黑背景

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
        // 计算黑框坐标
        this.screenWidth = this.width;
        this.screenHeight = this.height;
        this.screenUIWidth = (int) (screenWidth * UI_WIDTH_PERCENT);
        this.screenUIHeight = (int) (screenHeight * UI_HEIGHT_PERCENT);
        this.uiX = (screenWidth - screenUIWidth) / 2;
        this.uiY = (screenHeight - screenUIHeight) / 2;

        // 按钮坐标（黑框上方10像素，水平居中）
        int totalBtnWidth = BTN_WIDTH * 2 + BTN_GAP;
        int btnStartX = LOGO_BTN_WIDTH + 2 + uiX;
        int btnY = uiY - BTN_HEIGHT - 2;

        //Logo按钮————————————————————————————————————————————————————————————
        int logoBtnX = uiX;
        Button.Builder logoBtnBuilder = Button.builder(Component.literal("§bDreaming§dFish"), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        showSubScreen = 0; // 切换到服务器任务子界面（不跳转Screen）
                    }
                })
                .pos(logoBtnX, btnY) //Logo按钮位置）
                .size(LOGO_BTN_WIDTH, BTN_HEIGHT); // Logo按钮尺寸

        //创建LOGO按钮
        logoBtn = logoBtnBuilder.build((builder) -> {
            return new Button(builder) {
                // 标记按钮不可交互
//                @Override
//                public boolean isActive() {
//                    return false;
//                }

                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    //背景色固定为半透黑，无悬浮高亮
                    int bgColor = (128 << 24) | 0x000000;
                    //绘制Logo按钮背景
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);

                    //绘制动态RGB边框
                    int borderColor = getDynamicBorderColor();
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + 1, borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY() + this.getHeight() - 1,
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + 1, this.getY() + this.getHeight(), borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX() + this.getWidth() - 1, this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);

                    int textColor = 0xFFFFFF; // 默认白色
                    if (showSubScreen == 0) { // 服务器任务按钮被选中
                        textColor = 0xFFD700;
                    }
                    //绘制Logo文字
                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                            this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFF);
                }
            };
        });
        //————————————————————————————————————————————————————————————————————————————————————————————————————————

        //服务器按钮————————————————————————————————————————————————————————————————————————————————————————————————
        Button.Builder serverBtnBuilder = Button.builder(Component.literal("故事"), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        showSubScreen = 1; // 切换到服务器任务子界面（不跳转Screen）
                    }
                }).pos(btnStartX, btnY)
                .size(BTN_WIDTH, BTN_HEIGHT);

        //创建自定义按钮（重写renderWidget）
        serverTaskBtn = serverBtnBuilder.build((builder) -> {
            // 创建Button子类，重写渲染方法
            return new Button(builder) {
                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    // 背景色（半透黑，悬浮高亮）
                    int bgColor = (128 << 24) | 0x000000;
                    if (this.isHoveredOrFocused()) {
                        bgColor = (180 << 24) | 0x111111;
                    }
                    // 绘制按钮背景
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);
                    // 绘制动态RGB边框
                    int borderColor = getDynamicBorderColor();
                    // 上边框
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + 1, borderColor);
                    // 下边框
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY() + this.getHeight() - 1,
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    // 左边框
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + 1, this.getY() + this.getHeight(), borderColor);
                    // 右边框
                    guiGraphics.fill(RenderType.gui(), this.getX() + this.getWidth() - 1, this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    // 绘制居中文字（选中时高亮）
                    int textColor = 0xFFFFFF; // 默认白色
                    if (showSubScreen == 1) { // 服务器任务按钮被选中
                        textColor = 0xFFD700;
                    }

                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                            this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, textColor);
                }
            };
        });
        //————————————————————————————————————————————————————————————————————————————————————————————————————

        //个人任务——————————————————————————————————————————————————————————————————————————————————————————————————
        Button.Builder playerBtnBuilder = Button.builder(Component.literal("个人任务"), new Button.OnPress() {
                    @Override
                    public void onPress(Button btn) {
                        showSubScreen = 2; // 切换到个人任务子界面（不跳转Screen）
                    }
                })
                .pos(btnStartX + BTN_WIDTH + BTN_GAP, btnY)
                .size(BTN_WIDTH, BTN_HEIGHT);


        playerTaskBtn = playerBtnBuilder.build((builder) -> {
            return new Button(builder) {
                @Override
                public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    int bgColor = (128 << 24) | 0x000000;
                    if (this.isHoveredOrFocused()) {
                        bgColor = (180 << 24) | 0x111111;
                    }
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), bgColor);

                    // 绘制动态RGB边框
                    int borderColor = getDynamicBorderColor();
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + this.getWidth(), this.getY() + 1, borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY() + this.getHeight() - 1,
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX(), this.getY(),
                            this.getX() + 1, this.getY() + this.getHeight(), borderColor);
                    guiGraphics.fill(RenderType.gui(), this.getX() + this.getWidth() - 1, this.getY(),
                            this.getX() + this.getWidth(), this.getY() + this.getHeight(), borderColor);

                    int textColor = 0xFFFFFF;
                    if (showSubScreen == 2) { // 个人任务按钮被选中
                        textColor = 0xFFD700;
                    }

                    guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                            this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, textColor);
                }
            };
        });
        //————————————————————————————————————————————————————————————————————————————————————————————————————————————————

        // 添加按钮到屏幕
        this.addRenderableWidget(logoBtn);
        this.addRenderableWidget(serverTaskBtn);
        this.addRenderableWidget(playerTaskBtn);

        //排行榜——————————————————
        //请求全服玩家的数据
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player != null) {
//            TaskUI_Screen.clearAllPlayerCache(); // 清空旧缓存
//            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_RequestAllPlayerData());
//        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 获取动态RGB边框颜色
        int dynamicBorderColor = getDynamicBorderColor();

        // 渲染主界面背景发光效果
        int glowColor = 0x30000000 | (dynamicBorderColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), uiX - 2, uiY - 2, uiX + screenUIWidth + 2, uiY + screenUIHeight + 2, glowColor);

        // 渲染主界面背景和边框
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + screenUIWidth, uiY + screenUIHeight, BG_COLOR);
        // 上边框
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + screenUIWidth, uiY + 1, dynamicBorderColor);
        // 下边框
        guiGraphics.fill(RenderType.gui(), uiX, uiY + screenUIHeight - 1, uiX + screenUIWidth, uiY + screenUIHeight, dynamicBorderColor);
        // 左边框
        guiGraphics.fill(RenderType.gui(), uiX, uiY, uiX + 1, uiY + screenUIHeight, dynamicBorderColor);
        // 右边框
        guiGraphics.fill(RenderType.gui(), uiX + screenUIWidth - 1, uiY, uiX + screenUIWidth, uiY + screenUIHeight, dynamicBorderColor);

        //渲染按钮，父类方法会渲染已添加的所有按钮
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        //根据子界面标识渲染对应内容
        switch (showSubScreen) {
            case 0:
                renderMainServerContent(guiGraphics);
                break;
            case 1:
                renderServerTaskContent(guiGraphics); // 渲染服务器任务列表及详情
                break;
            case 2:
                renderPlayerTaskContent(guiGraphics); // 渲染个人任务列表及详情
                break;
            default:
                // 初始状态提示文字
                guiGraphics.drawString(this.font, "请选择任务类型", uiX + 20, uiY + 20, 0xFFFFFF);
        }
    }

    private int selectedServerTaskId = -1; // 选中的服务器任务ID，-1表示未选中
    private int selectedPlayerTaskId = -1;
    //——————————————————————————————————————————————————————————————————————————————
    private void renderMainServerContent(GuiGraphics guiGraphics) {
        int margin = 10;
        //计算模型尺寸（稍微大一点，但避免与进度条重叠）
        int modelSize = screenUIHeight / 5;

        LocalPlayer player = Minecraft.getInstance().player;
        float modelRealHeight = 0; // 模型精确视觉高度
        if (player != null) {
            //获取玩家碰撞箱高度（基础高度）
            float bbHeight = player.getBbHeight();
            //修正渲染偏移
            // 缩放系数：modelSize是渲染缩放，对应UI像素的比例
            modelRealHeight = bbHeight * modelSize;
        }

        //模型位置（稍微往下一点，但避免与进度条重叠）
        int modelCenterX = uiX + margin * 4 + modelSize / 2; //模型中心X
        int modelCenterY = uiY + screenUIHeight - (int)((screenUIHeight - modelRealHeight) / 2) - margin * 5; //脚部的y坐标

        if (player != null) {
            //调用原版背包的渲染方法
            // guiGraphics - 渲染上下文
            // modelCenterX/modelCenterY - 模型中心坐标
            // modelSize - 模型尺寸
            // 0/0 - 鼠标偏移（设为0则模型固定朝向，不跟随鼠标）
            // player - 要渲染的玩家
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics,
                    modelCenterX,
                    modelCenterY,
                    modelSize,
                    0.0F, // 鼠标X偏移（固定为0，模型不旋转）
                    0.0F, // 鼠标Y偏移（固定为0，模型不旋转）
                    (LivingEntity) player
            );
        }

        //分割线————————————————————
        int splitLineX = uiX + screenUIWidth / 4; // UI区域1/3宽度位置
        // 绘制竖直线：X坐标固定，Y从UI顶部到UI底部，宽度1像素
        guiGraphics.fill(RenderType.gui(),
                splitLineX,          // 分割线X坐标（UI区域1/3处）
                uiY,                 // 分割线顶部Y坐标（UI顶部）
                splitLineX + 1,      // 分割线宽度（1像素，细线条）
                uiY + screenUIHeight,// 分割线底部Y坐标（UI底部）
                0x80FFFFFF);         // 分割线颜色（半透明白色，可自行调整）

        //文字区域起始坐标
        int textX = uiX + screenUIWidth / 3;
        int textY = uiY + screenUIHeight / 8;

        // 大标题
        if (player != null) {
            String levelText = String.valueOf(PlayerLevelManager.getPlayerLevelClient(player));
            String nickName = player.getScoreboardName();
            int currentInfection = PlayerInfectionManager.getCurrentInfectionClient(player);
            String infectionStatus = currentInfection >= 80 ? "[§c感染者§r]" : "[§a幸存者§r]";
            String displayName = nickName + " " + infectionStatus;
            int levelWidth = this.font.width(levelText);
            int levelHeight = this.font.lineHeight;

            // 圆形参数（保持放大后的视觉大小）
            int circleRadius = Math.max(levelWidth, levelHeight) / 2 + 3;
            //原始坐标（和无缩放时一致的位置）
            int circleCenterX = textX;
            int circleCenterY = textY;
            // 计算实际渲染后的圆圈半径（考虑1.8倍缩放）
            int scaledCircleRadius = (int)(circleRadius * 1.8f);

            //调整缩放锚点，放大后坐标不偏移
            guiGraphics.pose().pushPose();
            // 把坐标系原点移到圆形中心（缩放中心）
            guiGraphics.pose().translate(circleCenterX, circleCenterY, 0);
            // 放大1.8倍（文字变大）
            guiGraphics.pose().scale(1.8f, 1.8f, 1.8f);
            //把坐标系移回原位（这样缩放后的内容仍在原始坐标）
            guiGraphics.pose().translate(-circleCenterX, -circleCenterY, 0);

            // 绘制白色空心圆边框（坐标还是原始的circleCenterX/Y，位置不变）
            drawCircleBorder(guiGraphics, circleCenterX, circleCenterY, circleRadius, 0xFFFFFFFF);

            // 等级数字精准居中（坐标不变）
            int textDrawX = circleCenterX - levelWidth / 2 + margin / 8;
            int textDrawY = circleCenterY - levelHeight / 2;
            guiGraphics.drawString(this.font, levelText, textDrawX, textDrawY, 0xFFFFFFFF);

            guiGraphics.pose().popPose();

            // 昵称显示在圆圈右侧（单独放大，作为标题）
            int nickDrawX = circleCenterX + scaledCircleRadius + 15; // 圆圈右边 + 15px间距
            int nickDrawY = circleCenterY - this.font.lineHeight / 2; // 垂直居中对齐圆圈

            // 单独缩放昵称（1.3倍，保持标题感）
            guiGraphics.pose().pushPose();
            float nameScale = 1.3f;
            // 以文字左下角为缩放中心，避免位置偏移
            guiGraphics.pose().translate(nickDrawX, nickDrawY, 0);
            guiGraphics.pose().scale(nameScale, nameScale, 1.0f);
            guiGraphics.pose().translate(-nickDrawX, -nickDrawY, 0);
            guiGraphics.drawString(this.font, displayName, nickDrawX, nickDrawY, 0xAAAAAA);
            guiGraphics.pose().popPose();

            //————————————————————————————————————————————————————————————
            // 左对齐基准点：使用实际的圆圈半径（考虑缩放）
            int alignBaseX = circleCenterX - scaledCircleRadius;
            // 垂直起始点：等级圆形图标正下方
            int baseY = circleCenterY + scaledCircleRadius + 10;

            //档案标题
            String archiveTitle = "您的梦鱼游戏档案：";
            guiGraphics.drawString(this.font, archiveTitle, alignBaseX, baseY + 10, 0xFFFFD700); // 淡金色

            int lineStartY = baseY + 30;
            int lineHalfWidth = 80;
            guiGraphics.fill(RenderType.gui(),
                    alignBaseX, lineStartY,
                    alignBaseX + lineHalfWidth, lineStartY + 1,
                    0x80FFFFFF); // 半透明白色细横线

            //Rank：左对齐，浅灰色
            String rankText = "RANK:" + PlayerRankManager.getPlayerRankClient(player).getRankName();
            guiGraphics.drawString(this.font, rankText, alignBaseX, baseY + 40, 0xFFCCCCCC); // 浅灰色

            //称号：左对齐
            String titleText = "称号:" + PlayerTitleManager.getPlayerTitleClient(player).getTitleName();
            guiGraphics.drawString(this.font, titleText, alignBaseX, baseY + 55, 0xFF87CEFA); // 淡蓝色
            //————————————————————————————————————————————————————————————————
        }

        //绘制横向属性进度条
        if (player != null) {
            // 进度条起始位置
            int barStartY = uiY + screenUIHeight - BAR_HEIGHT * 6 - BAR_BAR_SPACING * 3;
            int categoryTextX = uiX + margin;
            int barX = categoryTextX + this.font.width("感染值") + BAR_TO_TEXT_SPACING; // 基于最长类别文本宽度

            // 获取玩家属性值
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();
            int currentFood = player.getFoodData().getFoodLevel();
            int maxFood = 20;
            int currentStrength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
            int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
            if (maxStrength <= 0) maxStrength = 100;
            float currentCourage = PlayerCourageManager.getCurrentCourageClient(player);
            float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
            if (maxCourage <= 0) maxCourage = 100;
            int currentInfection = PlayerInfectionManager.getCurrentInfectionClient(player);
            int maxInfection = 100;

            // 绘制血量进度条
            drawTaskHorizontalProgressBar(guiGraphics, barX, barStartY, currentHealth, maxHealth, TASK_HEALTH_COLOR, "血量");
            // 绘制饥饿值进度条
            drawTaskHorizontalProgressBar(guiGraphics, barX, barStartY + BAR_BAR_SPACING, currentFood, maxFood, TASK_FOOD_COLOR, "饥饿");
            // 绘制体力值进度条
            drawTaskHorizontalProgressBar(guiGraphics, barX, barStartY + BAR_BAR_SPACING * 2, currentStrength, maxStrength, TASK_STRENGTH_COLOR, "体力");
            // 绘制勇气值进度条
            drawTaskHorizontalProgressBar(guiGraphics, barX, barStartY + BAR_BAR_SPACING * 3, currentCourage, maxCourage, TASK_COURAGE_COLOR, "勇气");
            // 绘制感染值进度条
            drawTaskHorizontalProgressBar(guiGraphics, barX, barStartY + BAR_BAR_SPACING * 4, currentInfection, maxInfection, TASK_INFECTION_COLOR, "感染");
        }

        //排行榜
//        drawFullRanking(guiGraphics);
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

    // 横向进度条绘制（int版本）
    private void drawTaskHorizontalProgressBar(GuiGraphics guiGraphics, int x, int y, int currentValue, int maxValue, int normalColor, String category) {
        // 绘制左侧类别文本
        guiGraphics.drawString(this.font, category, x - this.font.width(category) - BAR_TO_TEXT_SPACING, y + (BAR_HEIGHT - this.font.lineHeight) / 2, TASK_TEXT_COLOR);

        // 绘制进度条背景
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TASK_BG_COLOR);
        // 计算进度（横向从左到右填充）
        float progress = Math.max(0, Math.min(1, (float) currentValue / maxValue));
        int fillWidth = (int) (BAR_WIDTH * progress);
        // 绘制进度（低进度变红）
        if (fillWidth > 0) {
            int finalColor = progress < 0.2f ? TASK_LOW_COLOR : normalColor;
            guiGraphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, finalColor);
        }
        // 绘制边框（带淡色发光）
        drawTaskBorder(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, normalColor);
        // 绘制右侧数值文本
        drawTaskProgressValueText(guiGraphics, x, y, String.format("%d/%d", currentValue, maxValue));
    }

    // 横向进度条绘制（float版本） ==========
    private void drawTaskHorizontalProgressBar(GuiGraphics guiGraphics, int x, int y, float currentValue, float maxValue, int normalColor, String category) {
        // 绘制左侧类别文本
        guiGraphics.drawString(this.font, category, x - this.font.width(category) - BAR_TO_TEXT_SPACING, y + (BAR_HEIGHT - this.font.lineHeight) / 2, TASK_TEXT_COLOR);

        // 绘制进度条背景
        guiGraphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, TASK_BG_COLOR);
        // 计算进度（横向从左到右填充）
        float progress = Math.max(0, Math.min(1, currentValue / maxValue));
        int fillWidth = (int) (BAR_WIDTH * progress);
        // 绘制进度（低进度变红）
        if (fillWidth > 0) {
            int finalColor = progress < 0.2f ? TASK_LOW_COLOR : normalColor;
            guiGraphics.fill(x, y, x + fillWidth, y + BAR_HEIGHT, finalColor);
        }
        // 绘制边框（带淡色发光）
        drawTaskBorder(guiGraphics, x, y, BAR_WIDTH, BAR_HEIGHT, normalColor);
        // 绘制右侧数值文本
        drawTaskProgressValueText(guiGraphics, x, y, String.format("%.1f/%.1f", currentValue, maxValue));
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
    //进度条上方
    private void drawTaskProgressValueText(GuiGraphics guiGraphics, int barX, int barY, String valueText) {
        Minecraft mc = Minecraft.getInstance();
        // 文本缩放比例（0.8f 表示80%大小，可按需调整为0.7f/0.9f等）
        float textScale = 0.8f;

        //计算原始文本位置
        int originalTextX = barX + BAR_WIDTH / 2 - mc.font.width(valueText)/2; // 水平居中
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