package com.mo.economy_system.server.serverui;

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
import com.mo.economy_system.server.rank.RankRegistry;
import com.mo.economy_system.server.serverui.tips.TipDisplayManager;
import com.mo.economy_system.utils.Util_Skull;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class ServerInformationDisplay {
    private static boolean SHOW_UI = true;                  // UI开关
    private static final int BOX_PADDING = 8;              // 框内边距
    private static final int BOX_SPACING = 3;              // 框之间间距
    private static final int RIGHT_OFFSET = 2;             // 右侧偏移
    private static final int TOP_OFFSET = 3;               // 顶部偏移
    private static final int BOX_HEIGHT = 10;              // 框高度
    private static final int INFO_BOX_TEXT_PADDING = 4;    // 文字左右内边距
    private static final float INFO_TEXT_SCALE = 0.75f;    // 文字缩放比例
    private static final int PROGRESS_BAR_HEIGHT = 5;      // 进度条高度

    // 客户端缓存数据（从网络包获取）
    public static int ONLINE_PLAYERS = 0;
    public static int PLAYER_BALANCE = 0;

    private static long LAST_PLAYER_LIST_UPDATE = 0;       // 玩家列表最后刷新时间
    private static long LAST_BALANCE_UPDATE = 0;           // 余额最后刷新时间
    private static final long UPDATE_INTERVAL = 5000;      // 5秒刷新一次

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
        // 仅多人服务器生效，单人跳过
        if (!mc.isSingleplayer()) {
            SHOW_UI = true;
            System.out.println("玩家进服：默认开启信息面板");
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

        // 单人游戏直接返回
        if (mc.isSingleplayer()) return;

        if (!SHOW_UI || mc.isPaused() || mc.screen != null || mc.player == null) return;

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
            Component.literal("§b§r§bDreaming§dFish"),
            0xFF9988FF,  // 梦幻蓝紫色边框
            0xB0202020  // 淡黑色背景
        ));
        leftBoxes.add(new InfoBox(
            Component.literal("§e📊 §f在线: " + ONLINE_PLAYERS),
            0xFFDD00,  // 黄色边框
            0xB0202020
        ));
        leftBoxes.add(new InfoBox(
            Component.literal("§a🕐 " + getGameTimeString(mc)),
            0x00FF88,  // 青绿色边框
            0xB0202020
        ));

        renderLeftBoxes(guiGraphics, font, leftBoxes);

        // ========== 第二部分：右上角玩家信息 ==========
        renderPlayerInfo(guiGraphics, font, screenWidth, screenHeight, mc, rankId, titleName, playerLevel);

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

    // 渲染右上角玩家信息框（优化版）
    private static void renderPlayerInfo(GuiGraphics guiGraphics, Font font, int screenWidth, int screenHeight,
                                         Minecraft mc, String rankId, String titleName, int playerLevel) {
        int baseY = TOP_OFFSET;

        // 计算文本宽度
        String nameText = "§e" + mc.player.getName().getString();
        String levelText = "§6✦ Lv." + playerLevel;
        long currentExp = PlayerLevelManager.getPlayerExperienceClient(mc.player);
        long nextLevelExp = PlayerLevelManager.getExperienceNeededForNextLevelClient(mc.player);
        float expProgress = PlayerLevelManager.getExperienceProgressClient(mc.player);
        int expPercent = (int)(expProgress * 100);
        String expNeededText = "§7" + expPercent + "% §8(§7还需: §f" + nextLevelExp + "§7)";

        // 计算rank+称号行的完整宽度
        Title titleObj = TitleRegistry.getTitleByName(titleName);
        int titleColor = titleObj != null ? titleObj.getColor() : 0xFFAAAAAA;

        int prefixWidth = font.width(Component.literal("§7🎖️ "));
        int rankWidth = font.width(Component.literal(rankId));
        int separatorWidth = font.width(Component.literal(" §r§7 | §r🏅 "));
        int titleWidth = font.width(titleName);
        int rankTitleWidth = prefixWidth + rankWidth + separatorWidth + titleWidth;

        int nameWidth = font.width(nameText);
        int levelTextWidth = font.width(levelText);
        int expNeededWidth = font.width(expNeededText);

        // 计算框的宽度和高度（增加内边距使布局更舒适）
        int padding = 5;
        int lineHeight = font.lineHeight;
        int spacing = 4;

        int bottomLineWidth = levelTextWidth + spacing + expNeededWidth;
        int minBoxWidth = Math.max(nameWidth, Math.max(rankTitleWidth, bottomLineWidth));
        int boxWidth = minBoxWidth + padding * 2;

        int boxHeight = padding + lineHeight + spacing + lineHeight + spacing + PROGRESS_BAR_HEIGHT + spacing + lineHeight + padding;

        // 框的位置（右上角）
        int boxX = screenWidth - boxWidth - RIGHT_OFFSET;
        int boxY = baseY;

        // ========== 渐变背景 ==========
        // 主背景
        int bgColor = 0xD0181818;  // 更深的半透明背景
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + boxWidth, boxY + boxHeight, bgColor);

        // ========== 获取动态RGB颜色 ==========
        int dynamicColor = getDynamicBorderColor();

        // ========== 微妙的RGB外发光效果 ==========
        int glowColor = 0x30000000 | (dynamicColor & 0x00FFFFFF);
        guiGraphics.fill(RenderType.gui(), boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, glowColor);

        // ========== RGB动态边框效果 ==========
        int borderColor = dynamicColor;  // 动态RGB边框

        // 主边框
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + boxWidth, boxY + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + 1, boxY + boxHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, borderColor);

        // ========== 第一行：玩家昵称 ==========
        int line1Y = boxY + padding;
        guiGraphics.drawString(font, Component.literal(nameText), boxX + padding, line1Y, 0xFFFFFF);

        // ========== 第二行：rank | 称号 ==========
        int line2Y = line1Y + lineHeight + spacing;
        int currentX = boxX + padding;

        // 渲染 "🎖️ " 前缀
        guiGraphics.drawString(font, Component.literal("§7🎖️ "), currentX, line2Y, 0xFFAAAAAA);
        currentX += font.width("§7🎖️ ");

        // 渲染 rank
        int rankColor = getRankColorByName(rankId);
        guiGraphics.drawString(font, Component.literal(rankId), currentX, line2Y, rankColor);
        currentX += rankWidth;

        // 渲染 " §r§7 | §r🏅 "
        String separator = " §r§7 | §r🏅 ";
        guiGraphics.drawString(font, Component.literal(separator), currentX, line2Y, 0xFFAAAAAA);
        currentX += font.width(separator);

        // 渲染称号
        guiGraphics.drawString(font, Component.literal(titleName), currentX, line2Y, titleColor);

        // ========== 第三行：经验进度条 ==========
        int progressBarY = line2Y + lineHeight + spacing;
        int progressBarX = boxX + padding;
        int progressBarWidth = boxWidth - padding * 2;

        // 进度条背景（渐变效果）
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, 0xDD1A1A1A);
        // 进度条背景内边框
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + progressBarWidth, progressBarY + 1, 0xFF333333);
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY + PROGRESS_BAR_HEIGHT - 1,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, 0xFF333333);

        // 进度条前景（金色渐变）
        int progressWidth = (int)(progressBarWidth * expProgress);
        if (progressWidth > 0) {
            // 主体
            for (int i = 0; i < progressWidth - 2; i++) {
                float ratio = (float)i / progressBarWidth;
                int r = Math.min(255, (int)(204 + ratio * 51));
                int g = Math.min(255, (int)(136 + ratio * 51));
                int b = Math.min(255, (int)(0 + ratio * 85));
                int segmentColor = 0xFF000000 | (r << 16) | (g << 8) | b;
                guiGraphics.fill(RenderType.gui(), progressBarX + 2 + i, progressBarY + 1,
                    progressBarX + 2 + i + 1, progressBarY + PROGRESS_BAR_HEIGHT - 1, segmentColor);
            }

            // 高光效果（顶部亮线）
            if (progressWidth > 2) {
                guiGraphics.fill(RenderType.gui(), progressBarX + 2, progressBarY + 1,
                    progressBarX + progressWidth - 1, progressBarY + 2, 0xFFFFDD88);
            }
        }

        // 进度条外边框（使用动态RGB颜色）
        int progressBorderColor = dynamicColor;
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + 2, progressBarY + PROGRESS_BAR_HEIGHT, progressBorderColor);
        guiGraphics.fill(RenderType.gui(), progressBarX + progressBarWidth - 2, progressBarY,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, progressBorderColor);
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + progressBarWidth, progressBarY + 1, progressBorderColor);
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY + PROGRESS_BAR_HEIGHT - 1,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, progressBorderColor);

        // ========== 第四行：等级 + 经验信息 ==========
        int belowProgressBarY = progressBarY + PROGRESS_BAR_HEIGHT + spacing;

        // 等级信息（左对齐）
        int levelX = boxX + padding;
        guiGraphics.drawString(font, Component.literal(levelText), levelX, belowProgressBarY, 0xFFFFFF);

        // 经验百分比（右对齐）
        int expNeededX = boxX + padding + progressBarWidth - expNeededWidth;
        guiGraphics.drawString(font, Component.literal(expNeededText), expNeededX, belowProgressBarY, 0xFFFFFF);
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
     * 获取动态RGB变色的边框颜色（基于系统时间循环，颜色更淡）
     */
    private static int getDynamicBorderColor() {
        long currentTime = System.currentTimeMillis();
        int red = (int) (Math.sin(currentTime * 0.001) * 100 + 155);
        int green = (int) (Math.sin(currentTime * 0.001 + 2) * 100 + 155);
        int blue = (int) (Math.sin(currentTime * 0.001 + 4) * 100 + 155);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    /**
     * 根据Rank名称获取对应的颜色
     */
    private static int getRankColorByName(String rankName) {
        return switch (rankName) {
            case "FISH" -> 0xFF55FF55;
            case "FISH+" -> 0xFF55FFFF;
            case "FISH++" -> 0xFFFFD700;
            case "OPERATOR" -> 0xFFFF5555;
            default -> 0xFFFFFFFF;
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
