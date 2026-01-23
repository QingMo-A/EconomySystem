package com.mo.economy_system.server.serverui.serverscreen;

import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.core.playerattributes_system.courage.PlayerCourageManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import com.mo.economy_system.core.playerattributes_system.strength.PlayerStrengthClientSync;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.core.task_system.TaskPlayerData;
import com.mo.economy_system.core.story_system.StoryStageData;
import com.mo.economy_system.core.story_system.StoryTaskData;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.notice.NoticeData;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.mo.economy_system.server.serverui.serverscreen.ServerScreenUI_RendererUtils.*;

/**
 * ServerScreenUI 页面渲染器
 *
 * 负责渲染服务器UI的所有页面内容，包括：
 * - 左侧按钮面板
 * - 中间玩家信息区域
 * - 右侧属性卡片区域
 * - 公告页面
 * - 任务页面
 *
 * 此类持有 Screen 的引用，可以访问所有必要的状态
 */
public class ServerScreenUI_PageRenderer {

    private final ServerScreenUI_Screen screen;
    private final Minecraft mc;

    // ==================== 点击区域（由Screen提供引用）====================
    private final int[] rankBoxClick = new int[4];   // x1, y1, x2, y2
    private final int[] goldBoxClick = new int[4];
    private final int[] territoryBoxClick = new int[4];
    private final int[] noticeClickArea = new int[4];
    private final int[] taskClickArea = new int[4];
    private final int[] taskTabArea = new int[4];   // 任务分类按钮点击区域
    private final int[] stageClickArea = new int[4];  // 阶段卡片点击区域
    private final int[] backButtonArea = new int[4];  // 返回按钮点击区域

    // 左侧按钮点击区域
    private final int[][] leftButtonClickAreas;

    public ServerScreenUI_PageRenderer(ServerScreenUI_Screen screen, int buttonCount) {
        this.screen = screen;
        this.mc = Minecraft.getInstance();
        this.leftButtonClickAreas = new int[buttonCount][4];
    }

    // ==================== 点击区域访问方法 ====================
    public int[] getRankBoxClick() { return rankBoxClick; }
    public int[] getGoldBoxClick() { return goldBoxClick; }
    public int[] getTerritoryBoxClick() { return territoryBoxClick; }
    public int[] getNoticeClickArea() { return noticeClickArea; }
    public int[] getTaskClickArea() { return taskClickArea; }
    public int[] getTaskTabArea() { return taskTabArea; }
    public int[] getStageClickArea() { return stageClickArea; }
    public int[] getBackButtonArea() { return backButtonArea; }
    public int[][] getLeftButtonClickAreas() { return leftButtonClickAreas; }

    // ==================== 属性进度条渲染 ====================

    /**
     * 渲染五个属性进度条（横向排列）
     */
    public void renderAttributeBarsHorizontal(GuiGraphics guiGraphics, LocalPlayer player,
                                               int rightPanelX, int rightPanelWidth, int offsetY) {
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

        String[] icons = {"❤", "🍖", "💪", "⚡", "☣"};
        int[] colors = {BAR_HEALTH_COLOR, BAR_FOOD_COLOR, BAR_STRENGTH_COLOR, BAR_COURAGE_COLOR, BAR_INFECTION_COLOR};
        float[] percents = {healthPercent, foodPercent, strengthPercent, couragePercent, infectionPercent};
        String[] values = {
                String.format("%.0f/%.0f", player.getHealth(), player.getMaxHealth()),
                String.format("%d/20", player.getFoodData().getFoodLevel()),
                String.format("%d/%d", strength, maxStrength),
                String.format("%.0f/%.0f", courage, maxCourage),
                String.format("%.1f/100", PlayerInfectionManager.getCurrentInfectionClient(player))
        };

        int boxMargin = 5;
        int boxWidth = rightPanelWidth - boxMargin * 2;
        int innerMargin = 8;
        int itemCount = 5;
        int itemSpacing = 8;
        int extraPadding = 4;
        int cornerRadius = 12;
        int barHeight = 6;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight + 2 + barHeight + 3 + lineHeight + 8 + lineHeight;

        int boxX = rightPanelX + boxMargin;
        int boxY = boxMargin + offsetY;

        int totalSpacing = (itemCount - 1) * itemSpacing;
        int itemWidth = (boxWidth - innerMargin * 2 - extraPadding * 2 - totalSpacing) / itemCount;

        int startX = boxX + innerMargin + extraPadding;
        int iconY = boxY + innerMargin;
        int barY = iconY + mc.font.lineHeight + 2;
        int textY = barY + barHeight + 3;

        for (int i = 0; i < itemCount; i++) {
            int itemX = startX + i * (itemWidth + itemSpacing);

            int barColor;
            if (i == 4) {
                barColor = getInfectionColor(infectionPercent);
            } else {
                barColor = colors[i];
            }

            int iconX = itemX + itemWidth / 2 - mc.font.width(icons[i]) / 2;
            guiGraphics.drawString(mc.font, icons[i], iconX, iconY, barColor);

            int barX = itemX;
            drawProgressBar(guiGraphics, barX, barY, itemWidth, barHeight, percents[i], barColor);

            int valueX = itemX + itemWidth / 2 - mc.font.width(values[i]) / 2;
            guiGraphics.drawString(mc.font, values[i], valueX, textY, 0xFFFFFFFF);
        }

        int tipY = textY + lineHeight + 8;
        String tipText = "属性与您的等级密切相关，提升等级可以提高您的属性";
        int tipX = boxX + boxWidth / 2 - mc.font.width(tipText) / 2;
        guiGraphics.drawString(mc.font, tipText, tipX, tipY, 0xFFAAAAAA);

        drawRoundedRectOutline(guiGraphics, boxX, boxY, boxWidth, boxHeight, cornerRadius, 0x40FFAAAA, 0xFFFFFFFF);
    }

    // ==================== 卡片框渲染 ====================

    /**
     * 渲染 Rank 框
     */
    public void renderRankBox(GuiGraphics guiGraphics, LocalPlayer player, int boxX, int boxY, int boxWidth, int mouseX, int mouseY, float uiScale) {
        Rank rank = PlayerRankManager.getPlayerRankClient(player);
        int rankColor = getRankColor(rank.getRankLevel());

        int innerMargin = 10;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        boolean isHovered = virtualMouseX >= boxX && virtualMouseX < boxX + boxWidth &&
                           virtualMouseY >= boxY && virtualMouseY < boxY + boxHeight;

        drawGameCard(guiGraphics, boxX, boxY, boxWidth, boxHeight, CARD_RANK_GOLD, isHovered);

        String rankIcon = "🏆 ";
        String rankText = rank.getRankName();
        String fullRankText = rankIcon + rankText;

        guiGraphics.drawString(mc.font, fullRankText, boxX + innerMargin + 4, boxY + innerMargin, rankColor);

        String descText = "您的Rank";
        int descWidth = mc.font.width(descText);
        guiGraphics.drawString(mc.font, descText, boxX + boxWidth - innerMargin - descWidth, boxY + innerMargin, CARD_TEXT_DESC);

        if (isHovered) {
            rankBoxClick[0] = boxX;
            rankBoxClick[1] = boxY;
            rankBoxClick[2] = boxX + boxWidth;
            rankBoxClick[3] = boxY + boxHeight;
        }
    }

    /**
     * 渲染 Title 框
     */
    public void renderTitleBox(GuiGraphics guiGraphics, LocalPlayer player, int boxX, int boxY, int boxWidth, int mouseX, int mouseY, float uiScale) {
        Title title = PlayerTitleManager.getPlayerTitleClient(player);
        int titleColor = 0xFF000000 | title.getColor();

        int innerMargin = 10;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        boolean isHovered = virtualMouseX >= boxX && virtualMouseX < boxX + boxWidth &&
                           virtualMouseY >= boxY && virtualMouseY < boxY + boxHeight;

        drawGameCard(guiGraphics, boxX, boxY, boxWidth, boxHeight, CARD_TITLE_PURPLE, isHovered);

        String titleText = "⭐ " + title.getTitleName();
        guiGraphics.drawString(mc.font, titleText, boxX + innerMargin + 4, boxY + innerMargin, titleColor);

        String descText = "您的称号";
        int descWidth = mc.font.width(descText);
        guiGraphics.drawString(mc.font, descText, boxX + boxWidth - innerMargin - descWidth, boxY + innerMargin, CARD_TEXT_DESC);
    }

    /**
     * 渲染金币框
     */
    public void renderGoldBox(GuiGraphics guiGraphics, int boxX, int boxY, int boxWidth, int mouseX, int mouseY, float uiScale) {
        int innerMargin = 10;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        int goldBalance = mc.player != null ? ClientCacheManager.getPlayerBalance(mc.player.getUUID()) : 0;

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        boolean isHovered = virtualMouseX >= boxX && virtualMouseX < boxX + boxWidth &&
                           virtualMouseY >= boxY && virtualMouseY < boxY + boxHeight;

        drawGameCard(guiGraphics, boxX, boxY, boxWidth, boxHeight, CARD_GOLD_ORANGE, isHovered);

        String goldText = "💰 " + formatNumber(goldBalance);
        guiGraphics.drawString(mc.font, goldText, boxX + innerMargin + 4, boxY + innerMargin, CARD_GOLD_ORANGE);

        String descText = "梦鱼币";
        int descWidth = mc.font.width(descText);
        guiGraphics.drawString(mc.font, descText, boxX + boxWidth - innerMargin - descWidth, boxY + innerMargin, CARD_TEXT_DESC);

        goldBoxClick[0] = boxX;
        goldBoxClick[1] = boxY;
        goldBoxClick[2] = boxX + boxWidth;
        goldBoxClick[3] = boxY + boxHeight;
    }

    /**
     * 渲染领地框
     */
    public void renderTerritoryBox(GuiGraphics guiGraphics, int boxX, int boxY, int boxWidth, int mouseX, int mouseY, float uiScale) {
        int innerMargin = 10;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        List<Territory> territories = mc.player != null ? ClientCacheManager.getTerritories(mc.player.getUUID()) : new java.util.ArrayList<>();

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        boolean isHovered = virtualMouseX >= boxX && virtualMouseX < boxX + boxWidth &&
                           virtualMouseY >= boxY && virtualMouseY < boxY + boxHeight;

        drawGameCard(guiGraphics, boxX, boxY, boxWidth, boxHeight, CARD_TERRITORY_GREEN, isHovered);

        String countText = territories.isEmpty() ? "🏰 0" : "🏰 " + territories.size();
        guiGraphics.drawString(mc.font, countText, boxX + innerMargin + 4, boxY + innerMargin, CARD_TERRITORY_GREEN);

        String descText = "领地";
        int descWidth = mc.font.width(descText);
        guiGraphics.drawString(mc.font, descText, boxX + boxWidth - innerMargin - descWidth, boxY + innerMargin, CARD_TEXT_DESC);

        territoryBoxClick[0] = boxX;
        territoryBoxClick[1] = boxY;
        territoryBoxClick[2] = boxX + boxWidth;
        territoryBoxClick[3] = boxY + boxHeight;
    }

    /**
     * 渲染群系框
     */
    public void renderExplorationStats(GuiGraphics guiGraphics, int boxX, int boxY, int boxWidth, int mouseX, int mouseY, float uiScale) {
        int innerMargin = 10;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        int biomesCount = mc.player != null ? ClientCacheManager.getExploredBiomesCount(mc.player.getUUID()) : 0;

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        boolean isHovered = virtualMouseX >= boxX && virtualMouseX < boxX + boxWidth &&
                           virtualMouseY >= boxY && virtualMouseY < boxY + boxHeight;

        drawGameCard(guiGraphics, boxX, boxY, boxWidth, boxHeight, CARD_BIOME_CYAN, isHovered);

        String biomesText = "🗺️ " + biomesCount;
        guiGraphics.drawString(mc.font, biomesText, boxX + innerMargin + 4, boxY + innerMargin, CARD_BIOME_CYAN);

        String descText = "已探索";
        int descWidth = mc.font.width(descText);
        guiGraphics.drawString(mc.font, descText, boxX + boxWidth - innerMargin - descWidth, boxY + innerMargin, CARD_TEXT_DESC);
    }

    /**
     * 渲染蓝图框
     */
    public void renderBlueprintBox(GuiGraphics guiGraphics, int boxX, int boxY, int boxWidth, int mouseX, int mouseY, float uiScale) {
        int innerMargin = 10;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight;

        int blueprintCount = mc.player != null ? ClientCacheManager.getUnlockedRecipesCount(mc.player.getUUID()) : 0;

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        boolean isHovered = virtualMouseX >= boxX && virtualMouseX < boxX + boxWidth &&
                           virtualMouseY >= boxY && virtualMouseY < boxY + boxHeight;

        drawGameCard(guiGraphics, boxX, boxY, boxWidth, boxHeight, CARD_BLUEPRINT_BLUE, isHovered);

        String blueprintText = "📜 " + blueprintCount;
        guiGraphics.drawString(mc.font, blueprintText, boxX + innerMargin + 4, boxY + innerMargin, CARD_BLUEPRINT_BLUE);

        String descText = "已解锁";
        int descWidth = mc.font.width(descText);
        guiGraphics.drawString(mc.font, descText, boxX + boxWidth - innerMargin - descWidth, boxY + innerMargin, CARD_TEXT_DESC);
    }

    /**
     * 渲染感染度信息框
     */
    public void renderInfectionInfoBox(GuiGraphics guiGraphics, net.minecraft.world.entity.player.Player player, int x, int y, int width) {
        int innerMargin = 6;
        int lineHeight = mc.font.lineHeight;
        int boxHeight = innerMargin * 2 + lineHeight * 6 + 5 * 3;

        UUID playerUUID = player.getUUID();
        boolean isInfected = ClientCacheManager.isInfected(playerUUID);
        float respawnPoint = ClientCacheManager.getRespawnPoint(playerUUID);
        int respawnTimes = (int) (respawnPoint / (isInfected ? 20 : 5));

        int bgColor, borderColor;

        if (isInfected) {
            bgColor = 0xD04D0000;
            borderColor = 0xFFFF6666;
        } else {
            if (respawnTimes < 5 && respawnTimes > 0) {
                bgColor = 0xD04D4D00;
                borderColor = 0xFFFFFF00;
            } else if (respawnTimes <= 0) {
                bgColor = 0xD04D0000;
                borderColor = 0xFFFF0000;
            } else {
                bgColor = 0xD0004D00;
                borderColor = 0xFF66FF66;
            }
        }

        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + boxHeight, bgColor);

        int glowColor = 0x30000000 | (borderColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x - 1, y - 1, x + width + 1, y + boxHeight + 1, glowColor);

        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + boxHeight - 1, x + width, y + boxHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + boxHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + boxHeight, borderColor);

        int contentX = x + innerMargin;
        int contentY = y + innerMargin;
        int currentLineY = 0;

        String statusText = isInfected ? "§c§l您是感染者" : "§a§l您是幸存者";
        guiGraphics.drawString(mc.font, statusText, contentX, contentY + currentLineY, 0xFFFFFF);
        currentLineY += lineHeight + 3;

        if (isInfected) {
            guiGraphics.drawString(mc.font, "§7很不幸，您被感染了，您的身体会", contentX, contentY + currentLineY, 0xFFFFFF);
            currentLineY += lineHeight + 3;
            guiGraphics.drawString(mc.font, "§7随时间出现不同的变化，周围生物", contentX, contentY + currentLineY, 0xFFFFFF);
            currentLineY += lineHeight + 3;
            guiGraphics.drawString(mc.font, "§7对您产生的行为可能也会发生变化...", contentX, contentY + currentLineY, 0xFFFFFF);
            currentLineY += lineHeight + 3;
        } else {
            guiGraphics.drawString(mc.font, "§7请继续加油生存下去", contentX, contentY + currentLineY, 0xFFFFFF);
            currentLineY += lineHeight + 3;
            guiGraphics.drawString(mc.font, "§7保持警惕，远离被感染的生物", contentX, contentY + currentLineY, 0xFFFFFF);
            currentLineY += lineHeight + 3;
            guiGraphics.drawString(mc.font, "§7您的每一次生存都是胜利", contentX, contentY + currentLineY, 0xFFFFFF);
            currentLineY += lineHeight + 3;
        }

        int deathCost = isInfected ? 20 : 5;
        String costText = isInfected ?
                String.format("§7作为感染者您每次死亡需要扣除 §c%d §7点分裂次数", deathCost) :
                String.format("§7作为幸存者您每次死亡需要扣除 §a%d §7点分裂次数", deathCost);
        guiGraphics.drawString(mc.font, costText, contentX, contentY + currentLineY, 0xFFFFFF);
        currentLineY += lineHeight + 3;

        String respawnText;
        if (respawnTimes <= 0) {
            respawnText = String.format("§c§l警告：分裂次数不足（§b%.1f§7/100），不足以复活一次", respawnPoint);
        } else if (respawnTimes < 5) {
            respawnText = String.format("§e§l警告：您还可以重生 §c%d §7次（剩余分裂次数：§b%.1f§7/100）", respawnTimes, respawnPoint);
        } else {
            respawnText = String.format("§7您还可以重生 §e%d §7次（剩余分裂次数：§b%.1f§7/100）", respawnTimes, respawnPoint);
        }
        guiGraphics.drawString(mc.font, respawnText, contentX, contentY + currentLineY, 0xFFFFFF);
    }

    /**
     * 渲染玩家模型
     */
    public void renderPlayerModel(GuiGraphics guiGraphics, int offsetY, int mouseX, int mouseY,
                                   int centerCenterX, int modelFootY, int modelSize, float uiScale) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;

        int relativeMouseX = (int) (virtualMouseX - centerCenterX);
        int relativeMouseY = (int) ((virtualMouseY - modelFootY * 0.4) * 0.5);

        InventoryScreen.renderEntityInInventoryFollowsMouse(
            guiGraphics, centerCenterX, modelFootY, modelSize, -relativeMouseX, -relativeMouseY, player
        );
    }

    // ==================== 公告页面渲染 ====================

    /**
     * 渲染公告列表
     */
    public void renderNoticeList(GuiGraphics guiGraphics, int rightPanelX, int rightPanelWidth,
                                   List<NoticeData> notices, Set<Integer> readNoticeIds,
                                   long scrollOffset, int cardHeight, int maxVisible) {
        int boxMargin = 5;
        int cardMargin = 4;
        int innerMargin = 6;

        String titleText = "📢 服务器公告";
        int titleY = boxMargin + 4;
        guiGraphics.drawString(mc.font, titleText, rightPanelX + boxMargin, titleY, 0xFF4FC3F7);

        int listStartY = titleY + mc.font.lineHeight + 8;
        int cardWidth = rightPanelWidth - boxMargin * 2;

        if (notices.isEmpty()) {
            String noNoticeText = "暂无公告";
            int textWidth = mc.font.width(noNoticeText);
            guiGraphics.drawString(mc.font, noNoticeText,
                rightPanelX + boxMargin + (cardWidth - textWidth) / 2, listStartY + 20, 0xFFAAAAAA);
            noticeClickArea[0] = noticeClickArea[1] = noticeClickArea[2] = noticeClickArea[3] = 0;
            return;
        }

        int totalNotices = notices.size();
        int maxCards = Math.min(maxVisible, totalNotices);

        int firstCardY = listStartY;
        int lastCardY = firstCardY;

        for (int i = 0; i < maxCards; i++) {
            int noticeIndex = (int) (i + scrollOffset);
            if (noticeIndex >= notices.size()) break;

            NoticeData notice = notices.get(noticeIndex);
            boolean isRead = readNoticeIds.contains(notice.getNoticeId());

            int cardY = listStartY + i * (cardHeight + cardMargin);
            renderNoticeCard(guiGraphics, rightPanelX + boxMargin, cardY, cardWidth, notice, isRead);
            lastCardY = cardY + cardHeight;
        }

        noticeClickArea[0] = rightPanelX + boxMargin;
        noticeClickArea[1] = firstCardY;
        noticeClickArea[2] = rightPanelX + boxMargin + cardWidth;
        noticeClickArea[3] = lastCardY;

        if (totalNotices > maxVisible) {
            int indicatorY = lastCardY + 4;
            String scrollHint = String.format("▼ 滚动查看 (%d/%d)", (int) scrollOffset + 1, totalNotices);
            int hintWidth = mc.font.width(scrollHint);
            guiGraphics.drawString(mc.font, scrollHint,
                rightPanelX + boxMargin + (cardWidth - hintWidth) / 2, indicatorY, 0xFF666666);
        }
    }

    /**
     * 渲染单个公告卡片
     */
    private void renderNoticeCard(GuiGraphics guiGraphics, int x, int y, int width, NoticeData notice, boolean isRead) {
        int innerMargin = 6;
        int cardHeight = 52;

        int cardBg = isRead ? 0x20FFFFFF : 0x30FFFFFF;
        int cardBorder = isRead ? 0x60FFFFFF : 0xFFFFFFFF;

        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + cardHeight, cardBg);
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, cardBorder);
        guiGraphics.fill(RenderType.gui(), x, y + cardHeight - 1, x + width, y + cardHeight, cardBorder);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + cardHeight, cardBorder);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + cardHeight, cardBorder);

        int contentX = x + innerMargin;
        int contentY = y + innerMargin;

        String dateTime = formatDateTime(notice.getPublishTime());
        String statusText = isRead ? "已读" : "未读";
        int statusColor = isRead ? 0xFF888888 : 0xFF4FC3F7;

        guiGraphics.drawString(mc.font, dateTime, contentX, contentY, 0xFFAAAAAA);
        int statusWidth = mc.font.width(statusText);
        guiGraphics.drawString(mc.font, statusText, x + width - innerMargin - statusWidth, contentY, statusColor);

        String title = notice.getNoticeTitle();
        guiGraphics.drawString(mc.font, title, contentX, contentY + mc.font.lineHeight + 2,
            isRead ? 0xFFCCCCCC : 0xFFFFFFFF);

        String content = notice.getNoticeContent();
        int maxWidth = width - innerMargin * 2 - 10;
        if (mc.font.width(content) > maxWidth) {
            content = truncateText(mc.font, content, maxWidth) + "...";
        }
        guiGraphics.drawString(mc.font, content, contentX, contentY + mc.font.lineHeight * 2 + 4, 0xFF999999);
    }

    // ==================== 任务页面渲染 ====================

    /**
     * 渲染任务页面
     * @param selectedStageId 当前选中的阶段ID，null表示显示阶段列表
     * @param stageScrollOffset 阶段列表滚动偏移量
     */
    public void renderTaskPage(GuiGraphics guiGraphics, int x, int width, int mouseX, int mouseY,
                                int virtualWidth, float uiScale, boolean showServerTasks,
                                java.util.Map<Integer, StoryStageData> storyStages,
                                java.util.Map<Integer, TaskPlayerData> playerTasks,
                                long scrollOffset, int cardHeight, int maxVisible,
                                String selectedStageId, long stageScrollOffset) {
        guiGraphics.fill(RenderType.gui(), x, 0, virtualWidth, screen.getVirtualHeight(), screen.getPanelBackgroundColor());

        // 顶部按钮区域
        int buttonY = 10;
        int buttonHeight = 24;
        int buttonSpacing = 8;
        int buttonX = x + 10;

        // 计算按钮宽度
        String storyText = "📋 故事";
        String personalText = "📜 个人任务";
        int storyWidth = mc.font.width(storyText) + 16;
        int personalWidth = mc.font.width(personalText) + 16;

        // 故事任务按钮
        int storyBtnX1 = buttonX;
        int storyBtnY1 = buttonY;
        int storyBtnX2 = storyBtnX1 + storyWidth;
        int storyBtnY2 = buttonY + buttonHeight;

        boolean storyHovered = (mouseX / uiScale) >= storyBtnX1 && (mouseX / uiScale) < storyBtnX2 &&
                               (mouseY / uiScale) >= storyBtnY1 && (mouseY / uiScale) < storyBtnY2;

        int storyBgColor = showServerTasks ? (storyHovered ? 0x40FFD700 : 0x30FFD700) : (storyHovered ? 0x20FFFFFF : 0x10FFFFFF);
        int storyBorderColor = showServerTasks ? 0xFFFFD700 : 0x60FFD700;

        guiGraphics.fill(RenderType.gui(), storyBtnX1, storyBtnY1, storyBtnX2, storyBtnY2, storyBgColor);
        guiGraphics.fill(RenderType.gui(), storyBtnX1, storyBtnY1, storyBtnX2, storyBtnY1 + 1, storyBorderColor);
        guiGraphics.fill(RenderType.gui(), storyBtnX1, storyBtnY2 - 1, storyBtnX2, storyBtnY2, storyBorderColor);
        guiGraphics.fill(RenderType.gui(), storyBtnX1, storyBtnY1, storyBtnX1 + 1, storyBtnY2, storyBorderColor);
        guiGraphics.fill(RenderType.gui(), storyBtnX2 - 1, storyBtnY1, storyBtnX2, storyBtnY2, storyBorderColor);

        int storyTextX = storyBtnX1 + (storyWidth - mc.font.width(storyText)) / 2;
        int storyTextY = storyBtnY1 + (buttonHeight - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, storyText, storyTextX, storyTextY, 0xFFFFFFFF);

        // 个人任务按钮
        int personalBtnX1 = storyBtnX2 + buttonSpacing;
        int personalBtnY1 = buttonY;
        int personalBtnX2 = personalBtnX1 + personalWidth;
        int personalBtnY2 = buttonY + buttonHeight;

        boolean personalHovered = (mouseX / uiScale) >= personalBtnX1 && (mouseX / uiScale) < personalBtnX2 &&
                                  (mouseY / uiScale) >= personalBtnY1 && (mouseY / uiScale) < personalBtnY2;

        int personalBgColor = !showServerTasks ? (personalHovered ? 0x409B59B6 : 0x309B59B6) : (personalHovered ? 0x20FFFFFF : 0x10FFFFFF);
        int personalBorderColor = !showServerTasks ? 0xFF9B59B6 : 0x609B59B6;

        guiGraphics.fill(RenderType.gui(), personalBtnX1, personalBtnY1, personalBtnX2, personalBtnY2, personalBgColor);
        guiGraphics.fill(RenderType.gui(), personalBtnX1, personalBtnY1, personalBtnX2, personalBtnY1 + 1, personalBorderColor);
        guiGraphics.fill(RenderType.gui(), personalBtnX1, personalBtnY2 - 1, personalBtnX2, personalBtnY2, personalBorderColor);
        guiGraphics.fill(RenderType.gui(), personalBtnX1, personalBtnY1, personalBtnX1 + 1, personalBtnY2, personalBorderColor);
        guiGraphics.fill(RenderType.gui(), personalBtnX2 - 1, personalBtnY1, personalBtnX2, personalBtnY2, personalBorderColor);

        int personalTextX = personalBtnX1 + (personalWidth - mc.font.width(personalText)) / 2;
        int personalTextY = personalBtnY1 + (buttonHeight - mc.font.lineHeight) / 2;
        guiGraphics.drawString(mc.font, personalText, personalTextX, personalTextY, 0xFFFFFFFF);

        // 保存按钮点击区域
        taskTabArea[0] = storyBtnX1;
        taskTabArea[1] = buttonY;
        taskTabArea[2] = personalBtnX2;
        taskTabArea[3] = buttonY + buttonHeight;

        // 任务列表区域
        int listX = x + 15;
        int listY = buttonY + buttonHeight + 15;
        int listWidth = width - 30;

        // 根据是否选中阶段来决定显示内容
        if (showServerTasks) {
            // 故事任务：显示阶段列表或任务列表
            if (selectedStageId == null) {
                // 显示阶段列表
                renderStageList(guiGraphics, listX, listY, listWidth, storyStages, mouseX, mouseY, uiScale, stageScrollOffset, maxVisible);
            } else {
                // 显示选中阶段的任务列表
                renderTaskListForStage(guiGraphics, listX, listY, listWidth, storyStages, selectedStageId,
                    mouseX, mouseY, uiScale, scrollOffset, cardHeight, maxVisible);
            }
        } else {
            // 个人任务：直接显示任务列表（无阶段）
            renderPlayerTaskList(guiGraphics, listX, listY, listWidth, playerTasks,
                mouseX, mouseY, uiScale, scrollOffset, cardHeight, maxVisible);
        }
    }

    /**
     * 渲染排行榜页面
     */
    public void renderRankPage(GuiGraphics guiGraphics, int x, int width, int mouseX, int mouseY,
                                int virtualWidth, float uiScale, LocalPlayer player) {
        guiGraphics.fill(RenderType.gui(), x, 0, virtualWidth, screen.getVirtualHeight(), screen.getPanelBackgroundColor());

        int listX = x + 15;
        int listY = 20;
        int listWidth = width - 30;

        // 显示开发中提示
        String devText = "🚧 懒狗hanhanyu还没开发完该功能";
        String hintText = "敬请期待...";
        int devWidth = mc.font.width(devText);
        int hintWidth = mc.font.width(hintText);

        guiGraphics.drawString(mc.font, devText,
            listX + (listWidth - devWidth) / 2, listY + 60, 0xFFFFD700);
        guiGraphics.drawString(mc.font, hintText,
            listX + (listWidth - hintWidth) / 2, listY + 60 + mc.font.lineHeight + 10, 0xFFAAAAAA);
    }

    /**
     * 渲染成就页面
     */
    public void renderAchievementPage(GuiGraphics guiGraphics, int x, int width, int mouseX, int mouseY,
                                       int virtualWidth, float uiScale) {
        guiGraphics.fill(RenderType.gui(), x, 0, virtualWidth, screen.getVirtualHeight(), screen.getPanelBackgroundColor());

        int listX = x + 15;
        int listY = 20;
        int listWidth = width - 30;

        // 显示开发中提示
        String devText = "🚧 懒狗hanhanyu还没开发完该功能";
        String hintText = "敬请期待...";
        int devWidth = mc.font.width(devText);
        int hintWidth = mc.font.width(hintText);

        guiGraphics.drawString(mc.font, devText,
            listX + (listWidth - devWidth) / 2, listY + 60, 0xFFFFD700);
        guiGraphics.drawString(mc.font, hintText,
            listX + (listWidth - hintWidth) / 2, listY + 60 + mc.font.lineHeight + 10, 0xFFAAAAAA);
    }

    /**
     * 渲染阶段列表
     */
    private void renderStageList(GuiGraphics guiGraphics, int listX, int listY, int listWidth,
                                  java.util.Map<Integer, StoryStageData> storyStages,
                                  int mouseX, int mouseY, float uiScale, long scrollOffset, int maxVisible) {
        // 故事标题
        String introTitle = "📜 在梦屿上和其他玩家一起推进剧情，拯救服务器";
        guiGraphics.drawString(mc.font, introTitle, listX, listY, 0xFFFFD700);

        // 阶段列表
        int cardY = listY + mc.font.lineHeight + 15;
        int stageCardHeight = 80;
        int cardSpacing = 10;
        int firstCardY = cardY;
        int lastCardY = cardY;

        int visibleCount = Math.min(maxVisible, storyStages.size());

        // 按阶段ID排序
        java.util.List<Integer> sortedStageIds = new java.util.ArrayList<>(storyStages.keySet());
        java.util.Collections.sort(sortedStageIds);

        for (int i = 0; i < visibleCount; i++) {
            int stageIndex = (int) (i + scrollOffset);
            if (stageIndex >= sortedStageIds.size()) break;

            Integer stageId = sortedStageIds.get(stageIndex);
            StoryStageData stage = storyStages.get(stageId);
            if (stage == null) continue;

            // 计算该阶段的完成进度
            int finishedCount = 0;
            java.util.List<StoryTaskData> tasks = stage.getTasks();
            if (tasks != null) {
                for (StoryTaskData task : tasks) {
                    if (task.isClientPlayerFinished()) finishedCount++;
                }
            }
            int totalInStage = stage.getTotalTaskCount();
            float progress = stage.getProgressPercentage();

            float virtualMouseX = mouseX / uiScale;
            float virtualMouseY = mouseY / uiScale;
            boolean isHovered = virtualMouseX >= listX && virtualMouseX < listX + listWidth &&
                               virtualMouseY >= cardY && virtualMouseY < cardY + stageCardHeight;

            renderStageCard(guiGraphics, listX, cardY, listWidth, stage.getStageName(), stage.getStageDescription(),
                finishedCount, totalInStage, progress, isHovered);

            lastCardY = cardY + stageCardHeight;
            cardY += stageCardHeight + cardSpacing;
        }

        stageClickArea[0] = listX;
        stageClickArea[1] = firstCardY;
        stageClickArea[2] = listX + listWidth;
        stageClickArea[3] = lastCardY;

        if (storyStages.size() > maxVisible) {
            int indicatorY = lastCardY + 4;
            String scrollHint = String.format("▼ 滚动查看 (%d/%d)", (int) scrollOffset + 1, storyStages.size());
            int hintWidth = mc.font.width(scrollHint);
            guiGraphics.drawString(mc.font, scrollHint,
                listX + (listWidth - hintWidth) / 2, indicatorY, 0xFF666666);
        }

        if (storyStages.isEmpty()) {
            String emptyText = "暂无故事任务";
            int emptyWidth = mc.font.width(emptyText);
            int emptyY = listY + 60;
            guiGraphics.drawString(mc.font, emptyText,
                listX + (listWidth - emptyWidth) / 2, emptyY, 0xFF888888);
        }
    }

    /**
     * 渲染阶段卡片
     */
    private void renderStageCard(GuiGraphics guiGraphics, int x, int y, int width, String stageName,
                                 String stageDescription, int finishedCount, int totalCount,
                                 float progress, boolean isHovered) {
        int innerMargin = 10;
        int cardHeight = 80;

        // 背景
        int bgColor = isHovered ? 0x30FFFFFF : 0x20FFFFFF;
        int borderColor = isHovered ? 0xFFFFD700 : 0x60FFD700;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + cardHeight, bgColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + cardHeight - 1, x + width, y + cardHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + cardHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + cardHeight, borderColor);

        // 阶段标题
        guiGraphics.drawString(mc.font, stageName, x + innerMargin, y + innerMargin, 0xFFFFD700);

        // 阶段描述（自动换行，最多显示2行）
        int descMaxWidth = width - innerMargin * 2;
        int descY = y + innerMargin + mc.font.lineHeight + 4;
        String wrappedDesc = wrapString(mc.font, stageDescription, descMaxWidth);
        String[] descLines = wrappedDesc.split("\n", 3); // 最多2行
        for (int i = 0; i < descLines.length && i < 2; i++) {
            guiGraphics.drawString(mc.font, descLines[i], x + innerMargin, descY, 0xFFAAAAAA);
            descY += mc.font.lineHeight + 2;
        }

        // 进度信息（根据描述行数调整位置）
        int progressY = descY + 4;
        String progressText = String.format("进度: %d/%d (%.0f%%)", finishedCount, totalCount, progress * 100);
        guiGraphics.drawString(mc.font, progressText, x + innerMargin, progressY, 0xFF888888);

        // 进度条
        int barY = y + cardHeight - innerMargin - 8;
        int barHeight = 6;
        int barWidth = width - innerMargin * 2;
        drawProgressBar(guiGraphics, x + innerMargin, barY, barWidth, barHeight, progress, 0xFFFFD700);
    }

    /**
     * 将字符串按指定宽度换行
     */
    private String wrapString(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String c = text.substring(i, i + 1);
            if (font.width(currentLine.toString() + c) > maxWidth) {
                if (currentLine.length() > 0) {
                    result.append(currentLine.toString()).append("\n");
                    currentLine = new StringBuilder();
                }
            }
            currentLine.append(c);
        }
        if (currentLine.length() > 0) {
            result.append(currentLine);
        }
        return result.toString();
    }

    /**
     * 渲染选中阶段的任务列表
     */
    private void renderTaskListForStage(GuiGraphics guiGraphics, int listX, int listY, int listWidth,
                                         java.util.Map<Integer, StoryStageData> storyStages, String selectedStageId,
                                         int mouseX, int mouseY, float uiScale, long scrollOffset,
                                         int cardHeight, int maxVisible) {
        // 获取选中的阶段
        StoryStageData stage = null;
        for (StoryStageData s : storyStages.values()) {
            if (String.valueOf(s.getStageId()).equals(selectedStageId)) {
                stage = s;
                break;
            }
        }

        if (stage == null) {
            String emptyText = "阶段不存在";
            int emptyWidth = mc.font.width(emptyText);
            guiGraphics.drawString(mc.font, emptyText,
                listX + (listWidth - emptyWidth) / 2, listY + 80, 0xFF888888);
            return;
        }

        String stageName = stage.getStageName();
        java.util.List<StoryTaskData> stageTasks = stage.getTasks();
        if (stageTasks == null) stageTasks = new java.util.ArrayList<>();

        int titleY = listY;

        // 渲染返回按钮和标题
        renderBackButton(guiGraphics, listX, titleY, mouseX, mouseY, uiScale);

        String titleText = "📋 " + stageName;
        guiGraphics.drawString(mc.font, titleText, listX + 50, titleY + 6, 0xFFFFFFFF);

        int cardY = titleY + 40;
        int cardSpacing = 8;
        int firstCardY = cardY;
        int lastCardY = cardY;

        int visibleCount = Math.min(maxVisible, stageTasks.size());

        for (int i = 0; i < visibleCount; i++) {
            int taskIndex = (int) (i + scrollOffset);
            if (taskIndex >= stageTasks.size()) break;

            StoryTaskData task = stageTasks.get(taskIndex);
            boolean isFinished = task.isClientPlayerFinished();

            float virtualMouseX = mouseX / uiScale;
            float virtualMouseY = mouseY / uiScale;
            boolean isHovered = virtualMouseX >= listX && virtualMouseX < listX + listWidth &&
                               virtualMouseY >= cardY && virtualMouseY < cardY + cardHeight;

            renderTaskCard(guiGraphics, listX, cardY, listWidth, task.getTaskName(), task.getTaskContent(),
                0xFFFFD700, isFinished, true, isHovered);
            lastCardY = cardY + cardHeight;
            cardY += cardHeight + cardSpacing;
        }

        taskClickArea[0] = listX;
        taskClickArea[1] = firstCardY;
        taskClickArea[2] = listX + listWidth;
        taskClickArea[3] = lastCardY;

        if (stageTasks.size() > maxVisible) {
            int indicatorY = lastCardY + 4;
            String scrollHint = String.format("▼ 滚动查看 (%d/%d)", (int) scrollOffset + 1, stageTasks.size());
            int hintWidth = mc.font.width(scrollHint);
            guiGraphics.drawString(mc.font, scrollHint,
                listX + (listWidth - hintWidth) / 2, indicatorY, 0xFF666666);
        }

        if (stageTasks.isEmpty()) {
            String emptyText = "该阶段暂无任务";
            int emptyWidth = mc.font.width(emptyText);
            int emptyY = listY + 80;
            guiGraphics.drawString(mc.font, emptyText,
                listX + (listWidth - emptyWidth) / 2, emptyY, 0xFF888888);
        }
    }

    /**
     * 渲染个人任务列表（无阶段）
     */
    private void renderPlayerTaskList(GuiGraphics guiGraphics, int listX, int listY, int listWidth,
                                       java.util.Map<Integer, TaskPlayerData> playerTasks,
                                       int mouseX, int mouseY, float uiScale, long scrollOffset,
                                       int cardHeight, int maxVisible) {
        int totalTasks = playerTasks.size();

        String titleText = "📜 个人任务";
        String countText = String.format("%s (%d)", titleText, totalTasks);
        int titleY = listY;
        guiGraphics.drawString(mc.font, countText, listX, titleY, 0xFFFFFFFF);

        int cardY = titleY + mc.font.lineHeight + 10;
        int cardSpacing = 8;
        int firstCardY = cardY;
        int lastCardY = cardY;

        int visibleCount = Math.min(maxVisible, totalTasks);

        for (int i = 0; i < visibleCount; i++) {
            int taskIndex = (int) (i + scrollOffset);
            if (taskIndex >= playerTasks.size()) break;

            java.util.Optional<java.util.Map.Entry<Integer, TaskPlayerData>> taskEntry =
                playerTasks.entrySet().stream().skip(taskIndex).findFirst();
            if (taskEntry.isEmpty()) break;

            TaskPlayerData task = taskEntry.get().getValue();
            boolean isFinished = task.isClientPlayerFinished();

            float virtualMouseX = mouseX / uiScale;
            float virtualMouseY = mouseY / uiScale;
            boolean isHovered = virtualMouseX >= listX && virtualMouseX < listX + listWidth &&
                               virtualMouseY >= cardY && virtualMouseY < cardY + cardHeight;

            renderTaskCard(guiGraphics, listX, cardY, listWidth, task.getTaskName(), task.getTaskContent(),
                0xFF9B59B6, isFinished, false, isHovered);
            lastCardY = cardY + cardHeight;
            cardY += cardHeight + cardSpacing;
        }

        taskClickArea[0] = listX;
        taskClickArea[1] = firstCardY;
        taskClickArea[2] = listX + listWidth;
        taskClickArea[3] = lastCardY;

        if (totalTasks > maxVisible) {
            int indicatorY = lastCardY + 4;
            String scrollHint = String.format("▼ 滚动查看 (%d/%d)", (int) scrollOffset + 1, totalTasks);
            int hintWidth = mc.font.width(scrollHint);
            guiGraphics.drawString(mc.font, scrollHint,
                listX + (listWidth - hintWidth) / 2, indicatorY, 0xFF666666);
        }

        if (totalTasks == 0) {
            String emptyText = "暂无个人任务";
            int emptyWidth = mc.font.width(emptyText);
            int emptyY = listY + 60;
            guiGraphics.drawString(mc.font, emptyText,
                listX + (listWidth - emptyWidth) / 2, emptyY, 0xFF888888);
        }
    }

    /**
     * 渲染返回按钮
     */
    private void renderBackButton(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, float uiScale) {
        int btnWidth = 40;
        int btnHeight = 24;

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        boolean isHovered = virtualMouseX >= x && virtualMouseX < x + btnWidth &&
                           virtualMouseY >= y && virtualMouseY < y + btnHeight;

        int bgColor = isHovered ? 0x60FFD700 : 0x40FFFFFF;
        int textColor = isHovered ? 0xFFFFFFFF : 0xFFAAAAAA;

        guiGraphics.fill(RenderType.gui(), x, y, x + btnWidth, y + btnHeight, bgColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + btnWidth, y + 1, textColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + btnHeight, textColor);
        guiGraphics.fill(RenderType.gui(), x, y + btnHeight - 1, x + btnWidth, y + btnHeight, textColor);
        guiGraphics.fill(RenderType.gui(), x + btnWidth - 1, y, x + btnWidth, y + btnHeight, textColor);

        String backText = "◀";
        int textWidth = mc.font.width(backText);
        guiGraphics.drawString(mc.font, backText, x + (btnWidth - textWidth) / 2, y + 4, textColor);

        backButtonArea[0] = x;
        backButtonArea[1] = y;
        backButtonArea[2] = x + btnWidth;
        backButtonArea[3] = y + btnHeight;
    }

    /**
     * 渲染单个任务卡片
     */
    private void renderTaskCard(GuiGraphics guiGraphics, int x, int y, int width,
                                String taskName, String taskContent, int themeColor,
                                boolean isFinished, boolean isServerTask, boolean isHovered) {
        int innerMargin = 8;
        int cardHeight = 60;

        drawGameCard(guiGraphics, x, y, width, cardHeight, themeColor, isHovered || isFinished);

        String taskIcon = isServerTask ? "📋" : "📜";
        String fullTitle = taskIcon + " " + taskName;
        guiGraphics.drawString(mc.font, fullTitle, x + innerMargin + 4, y + innerMargin,
            isFinished ? 0xFF888888 : themeColor);

        // 故事任务不显示完成按钮，右侧留更多空间显示描述
        int rightMargin = isServerTask ? innerMargin : 40;
        int maxWidth = width - innerMargin * 2 - rightMargin;
        String displayContent = taskContent;
        if (mc.font.width(displayContent) > maxWidth) {
            displayContent = truncateText(mc.font, displayContent, maxWidth) + "...";
        }
        guiGraphics.drawString(mc.font, displayContent, x + innerMargin + 4,
            y + innerMargin + mc.font.lineHeight + 2, 0xFFAAAAAA);

        // 只有个人任务显示完成按钮
        if (!isServerTask) {
            int btnSize = 20;
            int btnX = x + width - innerMargin - btnSize;
            int btnY = y + (cardHeight - btnSize) / 2;

            if (isFinished) {
                guiGraphics.drawString(mc.font, "✓", btnX, btnY, 0xFF00FF00);
            } else {
                int btnColor = 0x40FFFFFF;
                guiGraphics.fill(RenderType.gui(), btnX, btnY, btnX + btnSize, btnY + btnSize, btnColor);
                guiGraphics.fill(RenderType.gui(), btnX, btnY, btnX + btnSize, btnY + 1, 0x60FFFFFF);
                guiGraphics.fill(RenderType.gui(), btnX, btnY, btnX + 1, btnY + btnSize, 0x60FFFFFF);
            }
        } else if (isFinished) {
            // 故事任务已完成显示标记
            int btnSize = 20;
            int btnX = x + width - innerMargin - btnSize;
            int btnY = y + (cardHeight - btnSize) / 2;
            guiGraphics.drawString(mc.font, "✓", btnX, btnY, 0xFF00FF00);
        }
    }
}
