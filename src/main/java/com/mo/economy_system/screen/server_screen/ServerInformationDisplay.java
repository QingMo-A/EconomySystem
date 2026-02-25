package com.mo.economy_system.screen.server_screen;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_OnlinePlayerCountRequest;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.economy_system.market.Screen_Market;
import com.mo.economy_system.screen.economy_system.shop.Screen_Shop;
import com.mo.economy_system.screen.territory_system.Screen_Territory;
import com.mo.economy_system.server.chattitle.PlayerTitleManager;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.chattitle.TitleRegistry;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.screen.server_screen.customsystemui.SystemMessageDisplay;
import com.mo.economy_system.screen.server_screen.tips.TipDisplayManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class ServerInformationDisplay {
    private static boolean SHOW_UI = true;                  // UI开关
    private static final int BOX_PADDING = 8;              // 框内边距
    private static final int BOX_SPACING = 3;              // 框之间间距
    private static final int RIGHT_OFFSET = 2;             // 右侧偏移
    private static final int TOP_OFFSET = 3;               // 顶部偏移
    private static final int LEFT_OFFSET = 2;              // 左侧偏移
    private static final int BOTTOM_OFFSET = 2;            // 底部偏移
    private static final int BOX_HEIGHT = 10;              // 框高度
    private static final int INFO_BOX_TEXT_PADDING = 4;    // 文字左右内边距
    private static final float INFO_TEXT_SCALE = 0.75f;    // 文字缩放比例
    private static final int PROGRESS_BAR_HEIGHT = 5;      // 进度条高度

    // 客户端缓存数据（从网络包获取）
    public static int ONLINE_PLAYERS = 0;

    private static long LAST_PLAYER_LIST_UPDATE = 0;       // 玩家列表最后刷新时间
    private static long LAST_BALANCE_UPDATE = 0;           // 余额最后刷新时间
    private static final long UPDATE_INTERVAL = 5000;      // 5秒刷新一次

    // 性能优化：缓存RGB颜色值
    private static int CACHED_DYNAMIC_COLOR = 0xFFDDAA55;
    private static long LAST_COLOR_UPDATE = 0;
    private static final long COLOR_UPDATE_INTERVAL = 100; // 100ms更新一次颜色

    // 获取当前玩家UUID
    public static UUID getCurrentPlayerUUID() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null ? mc.player.getUUID() : null;
    }

    // 注册Tick事件
    static {
        // 只注册客户端Tick事件
        MinecraftForge.EVENT_BUS.addListener(ServerInformationDisplay::onClientTick);
    }

    @SubscribeEvent
    public static void onClientLoginToServer(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft mc = Minecraft.getInstance();

        // 单人游戏和多人游戏都显示UI
        SHOW_UI = true;
        System.out.println("玩家进服：默认开启信息面板");

        // 单人游戏发送提示消息
        if (mc.isSingleplayer() && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§e[EconomySystem]§f您处于单人游戏，可以按§6O§f隐藏服务器UI"));
        }
    }

    //客户端Tick，触发网络请求 =====================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null) return;

        boolean skipRequest = false;
        if (mc.screen != null) {
            skipRequest = mc.screen instanceof Screen_Shop
                    || mc.screen instanceof Screen_Home
                    || mc.screen instanceof Screen_Market
                    || mc.screen instanceof Screen_Territory;
        }
        if (skipRequest) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        //请求在线玩家数
        if (currentTime - LAST_PLAYER_LIST_UPDATE > UPDATE_INTERVAL) {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_OnlinePlayerCountRequest());
            LAST_PLAYER_LIST_UPDATE = currentTime;
        }

        //余额请求
        if (currentTime - LAST_BALANCE_UPDATE > UPDATE_INTERVAL) {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
            LAST_BALANCE_UPDATE = currentTime;
        }
    }

    // HUD渲染（左上角小框 + 右上角玩家信息）
    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (!SHOW_UI || mc.isPaused() || mc.screen != null || mc.player == null) return;

        // F3 调试菜单打开时隐藏所有信息栏
        if (mc.options.renderDebug) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        // 获取玩家数据
        Rank playerRank = PlayerRankManager.getPlayerRankClient(mc.player);
        String rankId = playerRank.getRankName();
        String titleName = PlayerTitleManager.getPlayerTitleClient(mc.player).getTitleName();
        int playerLevel = PlayerLevelManager.getPlayerLevelClient(mc.player);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // ========== 第一部分：左上角服务器信息（三个小框） ==========
        List<InfoBox> leftBoxes = new ArrayList<>();
        leftBoxes.add(new InfoBox(
            Component.literal("§b§lDreaming§d§lFish"),
            0xFF6688CC,  // 柔和蓝紫色边框
            0xDD151520  // 深色背景
        ));
        leftBoxes.add(new InfoBox(
            Component.literal("§f" + ONLINE_PLAYERS + " §7在线"),
            0xFF666666,  // 灰色边框
            0xDD151520
        ));
        leftBoxes.add(new InfoBox(
            Component.literal("§f" + getGameTimeString(mc)),
            0xFF666666,  // 灰色边框
            0xDD151520
        ));

        renderLeftBoxes(guiGraphics, font, leftBoxes);

        // ========== 第二部分：右上角玩家信息 ==========
        int[] playerInfoBox = renderPlayerInfo(guiGraphics, font, screenWidth, screenHeight, mc, rankId, titleName, playerLevel);

        // ========== 第三部分：系统消息显示（玩家信息框下方）==========
        SystemMessageDisplay.renderSystemMessages(guiGraphics, font, screenWidth, playerInfoBox[0], playerInfoBox[1]);

        poseStack.popPose();
    }

    // 渲染左上角小框（水平排列）
    private static void renderLeftBoxes(GuiGraphics guiGraphics, Font font, List<InfoBox> boxes) {
        int totalWidth = 0;
        for (InfoBox box : boxes) {
            box.textWidth = font.width(box.text);
            int scaledTextWidth = (int)(box.textWidth * INFO_TEXT_SCALE);
            box.boxWidth = scaledTextWidth + INFO_BOX_TEXT_PADDING * 2;
            totalWidth += box.boxWidth;
        }
        totalWidth += (boxes.size() - 1) * BOX_SPACING;

        // 左上角起始坐标
        int currentX = TOP_OFFSET;
        int baseY = TOP_OFFSET;

        // 计算服务器信息框总高度（供Tips使用）
        int serverInfoHeight = BOX_HEIGHT;

        // 渲染所有小框
        for (InfoBox box : boxes) {
            renderEnhancedSmallBox(guiGraphics, font, currentX, baseY, box);
            currentX += box.boxWidth + BOX_SPACING;
        }

        // 设置服务器信息高度（包含间距）
        TipDisplayManager.setServerInfoHeight(serverInfoHeight + BOX_SPACING);
    }

    // 渲染右上角玩家信息框
    private static int[] renderPlayerInfo(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight,
                                         Minecraft mc, String rankId, String titleName, int playerLevel) {
        // 计算文本宽度
        String nameText = mc.player.getName().getString();
        String levelText = "Lv." + playerLevel;
        long currentExp = PlayerLevelManager.getPlayerExperienceClient(mc.player);
        long nextLevelExp = PlayerLevelManager.getExperienceNeededForNextLevelClient(mc.player);
        float expProgress = PlayerLevelManager.getExperienceProgressClient(mc.player);

        // 获取颜色
        Title titleObj = TitleRegistry.getTitleByName(titleName);
        int titleColor = titleObj != null ? titleObj.getColor() : 0xFFAAAAAA;
        int rankColor = getRankColorByName(rankId);

        // ========== 头像和布局配置 ==========
        int lineHeight = font.lineHeight;
        int spacing = 3; // 行间距（增加到3像素，让昵称和rank之间更宽松）
        int avatarSize = lineHeight * 2 + 4; // 头像高度 = 前两行高度 + 额外4像素（稍微大一点）
        int avatarSpacing = 3; // 头像和右侧文字的间距

        // emoji（已移除皇冠图标）

        // 计算各部分宽度
        int nameWidth = font.width(nameText);
        int levelWidth = font.width(levelText);
        int rankTextWidth = font.width(rankId);
        int titleWidth = font.width(titleName);

        // 框的尺寸
        int padding = 4; // 框的内边距（四边统一为4像素，更紧凑）
        int elementSpacing = 5; // 元素之间的间距（增大间距，显得不拥挤）

        // 进度条到框四边的间距（统一）
        int progressBarMargin = 4; // 进度条上下左右到框边缘的间距，统一为4像素

        // 第1行宽度：头像 + 头像间距 + 等级 + 间距 + 昵称
        int line1Width = avatarSize + avatarSpacing + levelWidth + elementSpacing + nameWidth;
        // 第2行宽度：头像 + 头像间距 + rank + 间距 + 称号
        int line2Width = avatarSize + avatarSpacing + rankTextWidth + elementSpacing + titleWidth;

        int boxWidth = Math.max(line1Width, line2Width) + padding * 2;
        int boxHeight = padding + lineHeight * 2 + spacing + progressBarMargin + PROGRESS_BAR_HEIGHT + padding;

        // 框的位置（右上角）
        int boxX = screenWidth - boxWidth - RIGHT_OFFSET;
        int boxY = TOP_OFFSET;

        // ========== 背景和边框 ==========
        int bgColor = 0xD0181818;
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + boxWidth, boxY + boxHeight, bgColor);

        int dynamicColor = getDynamicBorderColor();
        int glowColor = 0x30000000 | (dynamicColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, glowColor);

        // 主边框
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + boxWidth, boxY + 1, dynamicColor);
        guiGraphics.fill(RenderType.gui(), boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, dynamicColor);
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + 1, boxY + boxHeight, dynamicColor);
        guiGraphics.fill(RenderType.gui(), boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, dynamicColor);

        // ========== 左侧：玩家头像（覆盖前两行） ==========
        int avatarX = boxX + padding;
        int avatarY = boxY + padding;

        PlayerInfo playerInfo = mc.player.connection.getPlayerInfo(mc.player.getUUID());
        if (playerInfo != null) {
            // 渲染头像（覆盖前两行）
            PlayerFaceRenderer.draw(guiGraphics, playerInfo.getSkinLocation(), avatarX, avatarY, avatarSize);
        }

        // ========== 右侧内容区域 ==========
        int contentX = avatarX + avatarSize + avatarSpacing;
        int line1Y = boxY + padding; // 第1行Y坐标
        int line2Y = line1Y + lineHeight + spacing; // 第2行Y坐标

        // ========== 第1行：等级 + 间距 + 玩家昵称 ==========
        int currentX = contentX;

        // 等级（金色）
        guiGraphics.drawString(font, Component.literal(levelText), currentX, line1Y, 0xFFCC8800);
        currentX += levelWidth + elementSpacing;

        // 昵称（黄色）
        guiGraphics.drawString(font, Component.literal(nameText), currentX, line1Y, 0xFFFFAA);

        // ========== 第2行：rank + 间距 + 称号 ==========
        currentX = contentX;

        // Rank（彩色rank）
        guiGraphics.drawString(font, Component.literal(rankId), currentX, line2Y, rankColor);
        currentX += rankTextWidth + elementSpacing;

        // 称号（彩色）
        guiGraphics.drawString(font, Component.literal(titleName), currentX, line2Y, titleColor);

        // ========== 第3行：全宽进度条（跟随框变色） ==========
        int progressBarY = line2Y + lineHeight + progressBarMargin; // 进度条顶部到第二行底部的距离
        int progressBarX = boxX + progressBarMargin; // 进度条左边到框左边 = progressBarMargin
        int progressBarWidth = boxWidth - progressBarMargin * 2; // 进度条右边到框右边 = progressBarMargin

        // 进度条背景
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, 0xDD1A1A1A);

        // 进度条前景（使用动态RGB颜色）
        int progressWidth = (int)(progressBarWidth * expProgress);
        if (progressWidth > 2) {
            guiGraphics.fill(RenderType.gui(), progressBarX + 1, progressBarY + 1,
                progressBarX + progressWidth - 1, progressBarY + PROGRESS_BAR_HEIGHT - 1, dynamicColor);
            guiGraphics.fill(RenderType.gui(), progressBarX + 1, progressBarY + 1,
                progressBarX + progressWidth - 1, progressBarY + 2, 0xFFFFFFFF);
        }

        // 进度条边框（使用动态RGB颜色）
        int progressGlowColor = 0x40000000 | (dynamicColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), progressBarX - 1, progressBarY - 1,
            progressBarX + progressBarWidth + 1, progressBarY + PROGRESS_BAR_HEIGHT + 1, progressGlowColor);
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + progressBarWidth, progressBarY + 1, dynamicColor);
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY + PROGRESS_BAR_HEIGHT - 1,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, dynamicColor);
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + 1, progressBarY + PROGRESS_BAR_HEIGHT, dynamicColor);
        guiGraphics.fill(RenderType.gui(), progressBarX + progressBarWidth - 1, progressBarY,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, dynamicColor);

        // 返回玩家信息框的位置信息（Y坐标和高度）供系统消息使用
        return new int[]{boxY, boxHeight};
    }

    // 渲染增强版小框（带发光背景，无边框线）
    private static void renderEnhancedSmallBox(GuiGraphics guiGraphics, Font font, int x, int y, InfoBox box) {
        int boxHeight = BOX_HEIGHT;

        // ========== 背景效果 ==========
        // 主背景（淡黑色半透明）
        guiGraphics.fill(RenderType.gui(), x, y, x + box.boxWidth, y + boxHeight, box.backgroundColor);

        // ========== 微妙的发光背景效果 ==========
        int glowColor = 0x30000000 | (box.borderColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), x - 1, y - 1, x + box.boxWidth + 1, y + boxHeight + 1, glowColor);

        // ========== 文本居中渲染（应用缩放）==========
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float scaledTextWidth = box.textWidth * INFO_TEXT_SCALE;
        float scaledTextHeight = font.lineHeight * INFO_TEXT_SCALE;

        int textX = x + (box.boxWidth - (int)scaledTextWidth) / 2;
        int textY = y + (boxHeight - (int)scaledTextHeight) / 2;

        poseStack.translate(textX, textY, 0);
        poseStack.scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE, 1.0f);

        // 主文字
        guiGraphics.drawString(font, box.text, 0, 0, 0xFFFFFFFF);

        poseStack.popPose();
    }

    // 渲染小框（带文字缩放）- 保留旧方法备用
    private static void renderSmallBox(GuiGraphics guiGraphics, Font font, int x, int y, InfoBox box) {
        // 背景
        guiGraphics.fill(RenderType.gui(), x, y, x + box.boxWidth, y + BOX_HEIGHT, box.backgroundColor);

        // 边框（上、下、左、右）
        guiGraphics.fill(RenderType.gui(), x, y, x + box.boxWidth, y + 1, box.borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + BOX_HEIGHT - 1, x + box.boxWidth, y + BOX_HEIGHT, box.borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + BOX_HEIGHT, box.borderColor);
        guiGraphics.fill(RenderType.gui(), x + box.boxWidth - 1, y, x + box.boxWidth, y + BOX_HEIGHT, box.borderColor);

        // 文本居中渲染（应用缩放）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        float scaledTextWidth = box.textWidth * INFO_TEXT_SCALE;
        float scaledTextHeight = font.lineHeight * INFO_TEXT_SCALE;

        int textX = x + (box.boxWidth - (int)scaledTextWidth) / 2;
        int textY = y + (BOX_HEIGHT - (int)scaledTextHeight) / 2;

        poseStack.translate(textX, textY, 0);
        poseStack.scale(INFO_TEXT_SCALE, INFO_TEXT_SCALE, 1.0f);
        guiGraphics.drawString(font, box.text, 0, 0, 0xFFFFFF);

        poseStack.popPose();
    }

    // 获取游戏时间字符串
    private static String getGameTimeString(Minecraft mc) {
        if (mc.level == null) return "未知";

        java.time.LocalDateTime realTime = java.time.LocalDateTime.now();
        int year = realTime.getYear();
        int month = realTime.getMonthValue();
        int day = realTime.getDayOfMonth();
        int hour = realTime.getHour();
        int minute = realTime.getMinute();

        return String.format("%d.%d.%d %02d:%02d", year, month, day, hour, minute);
    }

    // 信息框数据类
    private static class InfoBox {
        Component text;
        int borderColor;
        int backgroundColor;
        int textWidth;
        int boxWidth;

        InfoBox(Component text, int borderColor, int backgroundColor) {
            this.text = text;
            this.borderColor = borderColor;
            this.backgroundColor = backgroundColor;
        }
    }

    /**
     * 获取动态RGB变色的边框颜色（基于系统时间循环，颜色更淡，使用缓存优化性能）
     */
    private static int getDynamicBorderColor() {
        long currentTime = System.currentTimeMillis();

        // 每100ms更新一次颜色，避免每帧计算
        if (currentTime - LAST_COLOR_UPDATE > COLOR_UPDATE_INTERVAL) {
            int red = (int) (Math.sin(currentTime * 0.001) * 100 + 155);
            int green = (int) (Math.sin(currentTime * 0.001 + 2) * 100 + 155);
            int blue = (int) (Math.sin(currentTime * 0.001 + 4) * 100 + 155);
            CACHED_DYNAMIC_COLOR = 0xFF000000 | (red << 16) | (green << 8) | blue;
            LAST_COLOR_UPDATE = currentTime;
        }

        return CACHED_DYNAMIC_COLOR;
    }

    /**
     * 根据Rank名称获取对应的颜色
     */
    private static int getRankColorByName(String rankName) {
        return switch (rankName) {
            case "FISH" -> 0xFF55FF55;
            case "FISH+" -> 0xFF55FFFF;
            case "FISH++" -> 0xFFFFAA00;  // 金色（与 SystemMessage 保持一致）
            case "OPERATOR" -> 0xFFFF5555;
            default -> 0xAAAAAA;
        };
    }

    /**
     * 将RGB颜色值转换为Minecraft颜色代码
     * @param rgb RGB颜色值（如0xFFFFFF）
     * @return 颜色代码字符串（如"§f"）
     */
    private static String rgbToColorCode(int rgb) {
        // 提取RGB分量
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        // 寻找最接近的Minecraft颜色
        if (red == 0 && green == 0 && blue == 0) return "§0";       // 黑色
        if (red == 0 && green == 0 && blue == 170) return "§1";   // 深蓝色
        if (red == 0 && green == 170 && blue == 0) return "§2";   // 深绿色
        if (red == 0 && green == 170 && blue == 170) return "§3"; // 深青色
        if (red == 170 && green == 0 && blue == 0) return "§4";   // 深红色
        if (red == 170 && green == 0 && blue == 170) return "§5"; // 深紫色
        if (red == 255 && green == 170 && blue == 0) return "§6"; // 金色
        if (red == 170 && green == 170 && blue == 170) return "§7"; // 灰色
        if (red == 85 && green == 85 && blue == 85) return "§8";  // 深灰色
        if (red == 85 && green == 85 && blue == 255) return "§9"; // 蓝色
        if (red == 85 && green == 255 && blue == 85) return "§a"; // 绿色
        if (red == 85 && green == 255 && blue == 255) return "§b"; // 青色
        if (red == 255 && green == 85 && blue == 85) return "§c";  // 红色
        if (red == 255 && green == 85 && blue == 255) return "§d"; // 粉色
        if (red == 255 && green == 255 && blue == 85) return "§e"; // 黄色
        if (red == 255 && green == 255 && blue == 255) return "§f"; // 白色

        // 默认白色（如果找不到精确匹配）
        return "§f";
    }

    // 对外控制方法
    public static void toggleUI() {
        SHOW_UI = !SHOW_UI;
    }

    public static boolean isShowUI() {
        return SHOW_UI;
    }

    public static void refreshData() {
        LAST_PLAYER_LIST_UPDATE = 0;
        LAST_BALANCE_UPDATE = 0;
    }
}
