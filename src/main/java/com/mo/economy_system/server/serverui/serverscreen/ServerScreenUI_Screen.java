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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ServerScreenUI_Screen extends Screen {

    //左右侧面板
    private final float LEFT_PANEL_PERCENT = 0.20f;  //左侧面板的比例
    private final float RIGHT_PANEL_PERCENT = 0.35f;  //右侧面板的比例
    private static final int PANEL_BACKGROUND_COLOR = 0x80000000;  //半透明黑（左右面板背景色）
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;  //面板边缘白色边框

    private static final int BAR_HEALTH_COLOR = 0xFFFF8888;    // 血量
    private static final int BAR_FOOD_COLOR = 0xFFFFCC00;      // 饥饿
    private static final int BAR_STRENGTH_COLOR = 0xFF00DD00;   // 力量
    private static final int BAR_COURAGE_COLOR = 0xFFCC00FF;    // 勇气
    private static final int BAR_INFECTION_COLOR = 0xFF00DD00;  // 感染


    //构造方法
    public ServerScreenUI_Screen() {
        super(Component.literal("综合界面"));
    }

    //————————玩家模型的坐标
    //模型高度 = MODEL_SIZE * 1.8
    //MODEL_Y 模型脚部的坐标
    private int MODEL_HEIGHT;
    private int MODEL_SIZE;
    private int MODEL_FOOT_Y;


    //重写的init方法
    @Override
    protected void init() {
        super.init();

        //玩家模型
        MODEL_HEIGHT = this.height / 4;
        MODEL_SIZE = (int) (MODEL_HEIGHT / 1.8);
        MODEL_FOOT_Y = this.height / 50 + MODEL_HEIGHT;
    }

    //渲染方法
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        //绘制左右侧面板
        renderPanels(guiGraphics);
    }

    //绘制左右侧面板
    private void renderPanels(GuiGraphics guiGraphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        int leftWidth = (int) (this.width * LEFT_PANEL_PERCENT);
        int rightWidth = (int) (this.width * RIGHT_PANEL_PERCENT);

        int rightCenterX = this.width - rightWidth / 2;

        int RIGHT_PANEL_START_X = this.width - rightWidth; //右侧面板绘制起点x

        //左侧透明黑背景
        guiGraphics.fill(RenderType.gui(), 0, 0, leftWidth, this.height, PANEL_BACKGROUND_COLOR);
        //右侧透明黑背景
        guiGraphics.fill(RenderType.gui(), RIGHT_PANEL_START_X, 0, this.width, this.height, PANEL_BACKGROUND_COLOR);

        // 左侧右边框（1像素宽）
        guiGraphics.fill(RenderType.gui(), leftWidth - 1, 0, leftWidth, this.height, PANEL_BORDER_COLOR);
        // 右侧左边框
        guiGraphics.fill(RenderType.gui(), RIGHT_PANEL_START_X, 0, RIGHT_PANEL_START_X + 1, this.height, PANEL_BORDER_COLOR);

        //————————————左侧标题————————————
        //绘制标题
        String serverTitle = "§bDreaming§dFish";

        //设置标题渲染坐标
        int serverTitleY = (int) (this.height * 0.2);  //屏幕纵像素1/5
        int serverTitleX = leftWidth / 2;  //渲染中心

        // 获取文字原始宽度
        int titleWidth = this.font.width(serverTitle);

        // 计算缩放比例（留一点边距，比如左右各留10像素）
        float maxWidth = (float) (leftWidth * 0.85);  // 文字允许的最长长度为左面板的85%长度
        float scale = maxWidth / titleWidth;

        guiGraphics.pose().pushPose();  //保存当前坐标系状态（位置、缩放、旋转等）

        //移动坐标原点到titlex，titiley，保证坐标不被缩放
        guiGraphics.pose().translate(serverTitleX, serverTitleY, 0);
        //进行缩放
        guiGraphics.pose().scale(scale, scale, 1.0f);   //三个参数为x，y，z分别缩放多少
        //进行渲染，颜色随便填都可以，已经用特殊符号§显示颜色
        //原点坐标是translate后的坐标，因此要减去自身长度的一半
        guiGraphics.drawString(this.font, serverTitle, -titleWidth / 2, 0, 0xFFFFFF);

        guiGraphics.pose().popPose();  // 恢复之前的状态

        //————————————右侧——————————————————————————————————————————————————————————————————————————
        //玩家模型
        renderPlayerModel(guiGraphics);

        String starEmoji = "⭐";
        int starWidth = this.font.width(starEmoji);

        guiGraphics.drawString(this.font, starEmoji, rightCenterX - starWidth / 2, (int) (MODEL_FOOT_Y + this.font.lineHeight * 0.7), 0xFFFF00);
        //——————————等级圆渲染————————————————————————————————————————————————————————————————————
        int level = PlayerLevelManager.getPlayerLevelClient(player);
        String levelText = String.valueOf(level);
        int levelWidth = this.font.width(levelText);
        //等级文本缩放计算
        float maxLevelWidth = (float) (rightWidth * 0.15);
        float levelScale = maxLevelWidth / levelWidth;

        //圆的半径,x,y
        //半径为等级长度的1.5倍
        int circleRadius = (int) (levelWidth * levelScale * 0.7);
        //等级为个位数时，防止异常大小
        if (level < 10) {
            levelScale = 2.0f;
            circleRadius = (int) (levelWidth * 3.0f);
        }
        int circleX = rightCenterX;
        //根据半径自适应y坐标
        int circleY = (int) (MODEL_FOOT_Y + circleRadius * 1.8);
        //画圆进度条
        float progress = PlayerLevelManager.getExperienceProgressClient(player);
        drawProgressCircle(guiGraphics, circleX, circleY, circleRadius, progress);

        guiGraphics.pose().pushPose();
        //移动原点到圆心
        guiGraphics.pose().translate(circleX, circleY, 0);
        guiGraphics.pose().scale(levelScale, levelScale, 1.0f);  // 缩放
        //显示等级文本
        guiGraphics.drawString(this.font, levelText, -levelWidth / 2, -this.font.lineHeight / 2, 0xFFFFFF);
        guiGraphics.pose().popPose();

        //——————————等级经验显示————————————————————————————————————————————————————————————————————————————————
        String expText = PlayerLevelManager.getPlayerExperienceClient(player) + "/" + PlayerLevelManager.getExperienceNeededForNextLevelClient(player);
        int expWidth = this.font.width(expText);
        int expMaxWidth = (int) (rightWidth * 0.25);
        float expScale = (float) expMaxWidth / expWidth;
        int expY = circleY + circleRadius + (int) (this.font.lineHeight * expScale * 0.8f);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(circleX, expY, 0);
        guiGraphics.pose().scale(expScale, expScale, 1.0f);
        guiGraphics.drawString(this.font, expText, -expWidth / 2, 0, 0xFFFFAA);  // 金色
        guiGraphics.pose().popPose();

        //————————玩家id与感染渲染————————————————————————————————————————————————————————————————————————————————————
        //玩家名称
        String playerName = "§e" + player.getScoreboardName();
        //感染状态
        int infection = PlayerInfectionManager.getCurrentInfectionClient(player);
        String status = infection >= 100 ? "§c感染者" : "§a幸存者";
        playerName = "§7" + playerName + " §c❤" + " §7[" + status + "§7]";
        //名字长度
        int nameWidth = this.font.width(playerName);
        //名字坐标
        int nameX = this.width - rightWidth / 2;
        int nameY = (int) (expY + circleRadius * 0.8);
        //玩家昵称缩放计算
        float maxNameWidth = (float) (rightWidth * 0.55f);
        float nameScale = maxNameWidth / nameWidth;
        if (nameScale > 1.5f) {
            nameScale = 1.2f;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(nameX, nameY, 0);
        guiGraphics.pose().scale(nameScale, nameScale, 1.0f);  // 缩放

        guiGraphics.drawString(this.font, playerName, -nameWidth / 2, 0, 0xFFFFFF);

        guiGraphics.pose().popPose();

        // 绘制玩家名称下方的分割线
        int underlineWidth = (int) (rightWidth * 0.8);  // 分割线宽度（缩放后的名字宽度）
        int underlineHeight = (int) (underlineWidth * 1.5);
        int underlineX = (int) (this.width - rightWidth * 0.9f);         // 分割线起始X（居中）
        int underlineY = (int) (nameY + this.font.lineHeight + 7);  // 分割线Y坐标（名字下方4像素）
        int underlineColor = 0xFFFFFFFF;  // 白色分割线
        guiGraphics.fill(RenderType.gui(), underlineX, underlineY, underlineX + underlineWidth, underlineY + 2, underlineColor);

        //渲染Rank和Title
        int rankTitleY = underlineY + 7;  // 分割线下方7像素

        //获取Rank和Title
        Rank rank = PlayerRankManager.getPlayerRankClient(player);
        String rankName = rank.getRankName();
        int rankLevel = rank.getRankLevel();
        int rankColor = getRankColor(rankLevel);

        Title title = PlayerTitleManager.getPlayerTitleClient(player);
        String titleName = title.getTitleName();
        int titleColor = 0xFF000000 | title.getColor();  // RGB转ARGB

        //渲染Rank
        String rankText = "🏆 " + rankName;
        int rankWidth = this.font.width(rankText);
        int rankX = underlineX;  // 居中偏左

        guiGraphics.drawString(this.font, rankText, rankX, rankTitleY, rankColor);

        // 渲染Title（右侧）
        String titleText = "⭐ " + titleName;
        int titleX = underlineX + underlineWidth - font.width(titleText);  //靠右对齐

        guiGraphics.drawString(this.font, titleText, titleX, rankTitleY, titleColor);

        // 渲染五个属性进度条
        renderAttributeBars(guiGraphics, player, RIGHT_PANEL_START_X, rightWidth, this.height);
    }

    //绘制玩家模型（右侧使用）
    private void renderPlayerModel(GuiGraphics guiGraphics) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        int rightWidth = (int) (this.width * RIGHT_PANEL_PERCENT);  //右侧宽度
        int rightCenterX = this.width - rightWidth / 2;  // 右侧面板中心

        // 渲染玩家模型
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                guiGraphics,
                rightCenterX,  // 模型中心X坐标
                MODEL_FOOT_Y,        // 模型脚部Y坐标
                MODEL_SIZE,     // 模型大小
                0.0F,          // 不跟随鼠标旋转
                0.0F,
                player         // 当前玩家
        );
    }

    //——————————工具方法
    /**
     * 绘制进度环（带进度弧）
     * @param guiGraphics 图形上下文
     * @param centerX 圆心X坐标
     * @param centerY 圆心Y坐标
     * @param radius 半径
     * @param progress 进度 0.0~1.0
     */
    private void drawProgressCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, float progress) {
        int segments = 256;
        float step = (float) (2 * Math.PI / segments);
        int borderColor = 0xFFFFFFFF;  // 白色边 框
        int progressColor = 0xFFFFAA00;  // 金色 进度

        //绘制进度弧（从12点钟方向顺时针）
        if (progress > 0) {
            float startAngle = (float) (-Math.PI / 2);  // -90度，12点钟
            float endAngle = startAngle + (float) (2 * Math.PI * progress);
            int arcRadius = radius - 3;  // 进度 弧稍微靠内

            int segmentsToDraw = (int) (segments * progress);
            if (segmentsToDraw < 1) segmentsToDraw = 1;

            int lastX = centerX + (int) (arcRadius * Math.cos(startAngle));
            int lastY = centerY + (int) (arcRadius * Math.sin(startAngle));

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

        //绘制白色外圈边框
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
     */
    private void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress, int color) {
        // 外发光效果
        int glowColor = 0x40000000 | (color & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x - 1, y - 1, x + width + 1, y + height + 1, glowColor);

        // 背景（半透明白色）
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, 0x80FFFFFF);

        // 前景（带高光）
        int progressWidth = (int)(width * Math.max(0, Math.min(1, progress)));
        if (progressWidth > 2) {
            int deepColor = 0xFF000000 | (color & 0x00FFFFFF);
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + progressWidth - 1, y + height - 1, deepColor);
            // 顶部高光
            guiGraphics.fill(RenderType.gui(), x + 1, y + 1, x + progressWidth - 1, y + Math.min(3, height / 2), 0x60FFFFFF);
        }

        // 白色边框
        int borderColor = 0xFFFFFFFF;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, borderColor);
    }

    /**
     * 渲染五个属性进度条（两行排列在右侧面板底部）
     *
     * 布局示意：
     * 第一行：❤血量 — 🍖饥饿 — 💪力量
     * 第二行：    ⚡勇气 — ☣感染    （居中显示）
     *
     * @param guiGraphics 图形上下文，用于绘制图形和文字
     * @param player 当前玩家，用于获取各属性值
     * @param rightPanelX 右侧面板的起始X坐标
     * @param rightPanelWidth 右侧面板的宽度
     * @param screenHeight 屏幕高度，用于计算进度条的Y坐标
     */
    private void renderAttributeBars(GuiGraphics guiGraphics, LocalPlayer player, int rightPanelX, int rightPanelWidth, int screenHeight) {
        //尺寸参数
        int margin = 12;              // 进度条距离面板边缘的边距
        int barsPerRow = 3;           // 第一行进度条数量
        int barsSecondRow = 2;        // 第二行进度条数量
        int barSpacing = 12;          // 进度条之间的间距
        int rowSpacing = 35;           // 两行之间的间距
        int barHeight = 7;            // 进度条高度
        int bottomMargin = 10;        // 进度条与底边的间距

        //计算第一行进度条宽度
        int totalSpacingFirstRow = barSpacing * (barsPerRow - 1);       // 第一行的总间距
        int availableWidthFirstRow = rightPanelWidth - margin * 2 - totalSpacingFirstRow;  // 第一行可用宽度
        int barWidthFirstRow = availableWidthFirstRow / barsPerRow;     // 第一行单个进度条宽度

        //计算第二行进度条宽度
        int totalSpacingSecondRow = barSpacing * (barsSecondRow - 1);   // 第二行的总间距
        int availableWidthSecondRow = rightPanelWidth - margin * 2 - totalSpacingSecondRow; // 第二行可用宽度
//        int barWidthSecondRow = availableWidthSecondRow / barsSecondRow; // 第二行单个进度条宽度
        int barWidthSecondRow = barWidthFirstRow;      //第二行进度条与第一行长度相同

        //计算进度条Y坐标
        int barY = screenHeight - margin - barHeight - rowSpacing - bottomMargin;  // 第一行进度条Y坐标

        //获取玩家属性值
        // 血量百分比
        float healthPercent = player.getHealth() / player.getMaxHealth();
        // 饥饿值百分比
        float foodPercent = player.getFoodData().getFoodLevel() / 20.0f;
        // 体力值百分比
        int strength = PlayerStrengthClientSync.getCurrentStrengthClient(player);
        int maxStrength = PlayerStrengthClientSync.getMaxStrengthClient(player);
        if (maxStrength <= 0) maxStrength = 100;  //防止除零
        float strengthPercent = (float) strength / maxStrength;
        //勇气值百分比
        float courage = PlayerCourageManager.getCurrentCourageClient(player);
        float maxCourage = PlayerCourageManager.getMaxCourageClient(player);
        if (maxCourage <= 0) maxCourage = 100;
        float couragePercent = courage / maxCourage;
        // 感染值百分比
        float infectionPercent = PlayerInfectionManager.getCurrentInfectionClient(player) / 100.0f;

        //准备进度条数据（图标、颜色、进度、数值文本）
        String[] icons = {"❤", "🍖", "💪", "⚡", "☣"};  // 进度条上方显示的emoji图标
        int[] colors = {BAR_HEALTH_COLOR, BAR_FOOD_COLOR, BAR_STRENGTH_COLOR, BAR_COURAGE_COLOR, BAR_INFECTION_COLOR};  // 各进度条颜色
        float[] percents = {healthPercent, foodPercent, strengthPercent, couragePercent, infectionPercent};  // 各进度百分比
        String[] values = {  // 各进度条下方显示的数值文本
                String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth()),
                String.format("%d/20", player.getFoodData().getFoodLevel()),
                String.format("%d/%d", strength, maxStrength),
                String.format("%.0f/%.0f", courage, maxCourage),
                String.format("%d/100", PlayerInfectionManager.getCurrentInfectionClient(player))
        };

        //第一行：3个进度条（血量、饥饿、力量）
        int barX = rightPanelX + margin;    // 第一行起始X坐标
        int iconY = barY - 16;              // 图标Y坐标（进度条上方16像素）

        for (int i = 0; i < barsPerRow; i++) {
            // 绘制进度条
            drawProgressBar(guiGraphics, barX, barY, barWidthFirstRow, barHeight, percents[i], colors[i] & 0x00FFFFFF);
            // 绘制图标（进度条上方）
            guiGraphics.drawCenteredString(this.font, icons[i], barX + barWidthFirstRow / 2, iconY, colors[i]);
            // 绘制数值文本（进度条下方，缩放到50%）
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
            guiGraphics.drawCenteredString(this.font, values[i], (barX + barWidthFirstRow / 2) * 2, (barY + barHeight + 4) * 2, 0xFFFFFFFF);
            guiGraphics.pose().popPose();

            //移动到下一个进度条的位置
            barX += barWidthFirstRow + barSpacing;
        }

        //第二行：2个进度条（勇气、感染，居中显示）
        int barY2 = barY + rowSpacing;      // 第二行Y坐标
        int iconY2 = barY2 - 16;            // 第二行图标Y坐标
        // 计算第二行的起始X坐标，使2个进度条在右侧面板中水平居中
        int secondRowTotalWidth = barWidthSecondRow * barsSecondRow + barSpacing * (barsSecondRow - 1);
        barX = rightPanelX + (rightPanelWidth - secondRowTotalWidth) / 2;

        for (int i = barsPerRow; i < 5; i++) {
            // 绘制进度条
            drawProgressBar(guiGraphics, barX, barY2, barWidthSecondRow, barHeight, percents[i], colors[i] & 0x00FFFFFF);
            // 绘制图标
            guiGraphics.drawCenteredString(this.font, icons[i], barX + barWidthSecondRow / 2, iconY2, colors[i]);
            // 绘制数值文本
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(0.5f, 0.5f, 1.0f);
            guiGraphics.drawCenteredString(this.font, values[i], (barX + barWidthSecondRow / 2) * 2, (barY2 + barHeight + 4) * 2, 0xFFFFFFFF);
            guiGraphics.pose().popPose();

            // 移动到下一个进度条的位置
            barX += barWidthSecondRow + barSpacing;
        }
    }

    /**
     * 根据Rank等级获取对应颜色
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

    //——————————————————————————————关闭ui逻辑————————————————————
    // ESC关闭
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // 256是ESC键
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //关闭时更新状态
    @Override
    public void onClose() {
        // 使用 toggleUI 来正确处理关闭逻辑（包括恢复信息面板状态）
        if (ServerScreenUI.isShowUI()) {
            ServerScreenUI.toggleUI();
        }
        super.onClose();
    }

    //不暂停游戏
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}