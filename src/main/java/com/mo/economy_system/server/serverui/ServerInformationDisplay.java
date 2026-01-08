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
    private static final int BOX_PADDING = 8;              // 框内边距（更宽松）
    private static final int BOX_SPACING = 1;              // 框之间间距
    private static final int RIGHT_OFFSET = 2;             // 右侧偏移
    private static final int TOP_OFFSET = 3;               // 顶部偏移
    private static final int BOX_HEIGHT = 11;              // 框高度
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

    // HUD渲染（分为两部分：中间顶部服务器信息 + 右上角玩家信息）
    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        // 单人游戏直接返回
        if (mc.isSingleplayer()) return;

        if (!SHOW_UI || mc.isPaused() || mc.screen != null || mc.player == null) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        Font font = mc.font;
        int screenWidth = event.getWindow().getGuiScaledWidth();

        // 获取玩家数据
        Rank playerRank = PlayerRankManager.getPlayerRankClient(mc.player);
        String rankId = playerRank.getRankName();
        String titleName = PlayerTitleManager.getPlayerTitleClient(mc.player).getTitleName();
        int playerLevel = PlayerLevelManager.getPlayerLevelClient(mc.player);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // ========== 第一部分：中间顶部服务器信息 ==========
        List<InfoBox> centerBoxes = new ArrayList<>();

        // 1. DreamingFish标题框 - RGB动态边框 + 淡黑色背景
        centerBoxes.add(new InfoBox(
            Component.literal("§bDreaming§dFish"),
            getDynamicBorderColor(),
            0xB0202020  // 淡黑色背景
        ));

        // 2. 在线人数框 - 黄色边框 + 淡黑色背景
        centerBoxes.add(new InfoBox(
            Component.literal("§e在线: §f" + ONLINE_PLAYERS),
            0xFFDD00,  // 黄色边框
            0xB0202020  // 淡黑色背景
        ));

        // 3. 游戏时间框 - 彩虹色边框 + 淡黑色背景
        String gameTime = getGameTimeString(mc);
        centerBoxes.add(new InfoBox(
            Component.literal("§a" + gameTime),
            0x00FF88,  // 青绿色边框
            0xB0202020  // 淡黑色背景
        ));

        // 渲染中间顶部信息框
        renderCenterBoxes(guiGraphics, font, screenWidth, centerBoxes);

        // ========== 第二部分：右上角玩家信息 ==========
        renderPlayerInfo(guiGraphics, font, screenWidth, mc, rankId, titleName, playerLevel);

        poseStack.popPose();
    }

    // 获取游戏时间字符串（简写格式：2026.1.8 14:30）
    private static String getGameTimeString(Minecraft mc) {
        if (mc.level == null) return "未知";

        // 使用真实世界时间
        java.time.LocalDateTime realTime = java.time.LocalDateTime.now();
        int year = realTime.getYear();
        int month = realTime.getMonthValue();
        int day = realTime.getDayOfMonth();
        int hour = realTime.getHour();
        int minute = realTime.getMinute();

        return String.format("%d.%d.%d %02d:%02d", year, month, day, hour, minute);
    }

    // 渲染中间顶部信息框
    private static void renderCenterBoxes(GuiGraphics guiGraphics, Font font, int screenWidth, List<InfoBox> boxes) {
        // 计算总宽度
        int totalWidth = 0;
        for (InfoBox box : boxes) {
            box.textWidth = font.width(box.text);
            box.boxWidth = box.textWidth + 12;  // 文字两边各6像素边距
            totalWidth += box.boxWidth;
        }
        totalWidth += (boxes.size() - 1) * BOX_SPACING;

        // 居中计算起始X坐标
        int currentX = (screenWidth - totalWidth) / 2;
        int baseY = TOP_OFFSET;

        // 渲染所有框框
        for (InfoBox box : boxes) {
            renderBox(guiGraphics, font, currentX, baseY, box);
            currentX += box.boxWidth + BOX_SPACING;
        }
    }

    // 渲染右上角玩家信息（三行布局，无头像）
    private static void renderPlayerInfo(GuiGraphics guiGraphics, Font font, int screenWidth,
                                         Minecraft mc, String rankId, String titleName, int playerLevel) {
        int baseY = TOP_OFFSET;

        // 计算文本宽度
        String nameText = "§e" + mc.player.getName().getString();
        String rankTitleText = "§7" + rankId + " §r§7| §r" + titleName;  // rank | 称号
        String levelText = "§6Lv." + playerLevel;

        // 计算距离下一级所需经验
        int currentExp = mc.player.totalExperience;
        int nextLevelExp = mc.player.getXpNeededForNextLevel();
        String expNeededText = "§7还需要: §f" + nextLevelExp + " §7经验升级";

        int nameWidth = font.width(nameText);
        int rankTitleWidth = font.width(rankTitleText);
        int levelTextWidth = font.width(levelText);
        int expNeededWidth = font.width(expNeededText);

        // 计算框的宽度和高度（四边等宽）
        int padding = 3;  // 四周边框的间距
        int lineHeight = font.lineHeight;
        int spacing = 3;  // 所有间距都相同（行间距、进度条间距）

        // 计算第三行文本的总宽度（等级 + 间距 + 经验文本）
        int bottomLineWidth = levelTextWidth + spacing + expNeededWidth;
        // 框宽度为所有文本的最大值
        int minBoxWidth = Math.max(nameWidth, Math.max(rankTitleWidth, bottomLineWidth));
        int boxWidth = minBoxWidth + padding * 2;

        // 计算框高度：顶部padding + 昵称行 + 间距 + rank行 + 间距 + 进度条 + 间距 + 等级行 + 底部padding
        int boxHeight = padding + lineHeight + spacing + lineHeight + spacing + PROGRESS_BAR_HEIGHT + spacing + lineHeight + padding;

        // 框的位置（右上角）
        int boxX = screenWidth - boxWidth - RIGHT_OFFSET;
        int boxY = baseY;

        // 淡黑色背景
        int bgColor = 0xB0202020;
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + boxWidth, boxY + boxHeight, bgColor);

        // 边框
        int borderColor = 0xFFAAAAAA;
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + boxWidth, boxY + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), boxX, boxY, boxX + 1, boxY + boxHeight, borderColor);
        guiGraphics.fill(RenderType.gui(), boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, borderColor);

        // 第一行：玩家昵称（左对齐）
        int line1Y = boxY + padding;
        guiGraphics.drawString(font, Component.literal(nameText), boxX + padding, line1Y, 0xFFFFFF);

        // 第二行：rank | 称号（左对齐）
        int line2Y = line1Y + lineHeight + spacing;
        guiGraphics.drawString(font, Component.literal(rankTitleText), boxX + padding, line2Y, 0xFFFFFF);

        // 第三行：进度条（根据底部文本宽度自适应）
        int progressBarY = line2Y + lineHeight + spacing;
        int progressBarX = boxX + padding;
        // 进度条宽度与底部文本总宽度相同
        int progressBarWidth = bottomLineWidth;

        // 进度条背景
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, 0xFF333333);

        // 进度条前景
        float experienceProgress = mc.player.experienceProgress;
        int progressWidth = (int)(progressBarWidth * experienceProgress);
        if (progressWidth > 0) {
            guiGraphics.fill(RenderType.gui(), progressBarX + 1, progressBarY + 1,
                progressBarX + progressWidth, progressBarY + PROGRESS_BAR_HEIGHT - 1, 0xFFCC8800);
        }

        // 进度条边框
        int progressBorderColor = 0xFFDDAA55;
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + progressBarWidth, progressBarY + 1, progressBorderColor);
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY + PROGRESS_BAR_HEIGHT - 1,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, progressBorderColor);
        guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
            progressBarX + 1, progressBarY + PROGRESS_BAR_HEIGHT, progressBorderColor);
        guiGraphics.fill(RenderType.gui(), progressBarX + progressBarWidth - 1, progressBarY,
            progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, progressBorderColor);

        // 进度条下方文字（与上面等间距）
        int belowProgressBarY = progressBarY + PROGRESS_BAR_HEIGHT + spacing;

        // 左边：当前等级
        int levelX = boxX + padding;
        guiGraphics.drawString(font, Component.literal(levelText), levelX, belowProgressBarY, 0xFFFFFF);

        // 右边：距离下一级所需经验
        int expNeededX = boxX + padding + levelTextWidth + spacing;
        guiGraphics.drawString(font, Component.literal(expNeededText), expNeededX, belowProgressBarY, 0xFFFFFF);
    }

    // 信息框数据类
    private static class InfoBox {
        Component text;
        int borderColor;
        int backgroundColor;  // 新增背景色
        int textWidth;
        int boxWidth;

        InfoBox(Component text, int borderColor, int backgroundColor) {
            this.text = text;
            this.borderColor = borderColor;
            this.backgroundColor = backgroundColor;
        }
    }

    // 渲染单个信息框（普通矩形边框）
    private static void renderBox(GuiGraphics guiGraphics, Font font, int x, int y, InfoBox box) {
        // 使用自定义彩色背景
        guiGraphics.fill(RenderType.gui(), x, y, x + box.boxWidth, y + BOX_HEIGHT, box.backgroundColor);

        // 边框（上、下、左、右）
        guiGraphics.fill(RenderType.gui(), x, y, x + box.boxWidth, y + 1, box.borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + BOX_HEIGHT - 1, x + box.boxWidth, y + BOX_HEIGHT, box.borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + BOX_HEIGHT, box.borderColor);
        guiGraphics.fill(RenderType.gui(), x + box.boxWidth - 1, y, x + box.boxWidth, y + BOX_HEIGHT, box.borderColor);

        // 文本居中渲染
        int textX = x + (box.boxWidth - box.textWidth) / 2;
        int textY = y + (BOX_HEIGHT - font.lineHeight) / 2;
        guiGraphics.drawString(font, box.text, textX, textY, 0xFFFFFF);
    }

    // 将ChatFormatting转换为颜色代码
    private static String getRankColorCode(ChatFormatting formatting) {
        if (formatting == ChatFormatting.BLACK) return "§0";
        if (formatting == ChatFormatting.DARK_BLUE) return "§1";
        if (formatting == ChatFormatting.DARK_GREEN) return "§2";
        if (formatting == ChatFormatting.DARK_AQUA) return "§3";
        if (formatting == ChatFormatting.DARK_RED) return "§4";
        if (formatting == ChatFormatting.DARK_PURPLE) return "§5";
        if (formatting == ChatFormatting.GOLD) return "§6";
        if (formatting == ChatFormatting.GRAY) return "§7";
        if (formatting == ChatFormatting.DARK_GRAY) return "§8";
        if (formatting == ChatFormatting.BLUE) return "§9";
        if (formatting == ChatFormatting.GREEN) return "§a";
        if (formatting == ChatFormatting.AQUA) return "§b";
        if (formatting == ChatFormatting.RED) return "§c";
        if (formatting == ChatFormatting.LIGHT_PURPLE) return "§d";
        if (formatting == ChatFormatting.YELLOW) return "§e";
        if (formatting == ChatFormatting.WHITE) return "§f";
        return "§f";  // 默认白色
    }

    /**
     * 获取动态RGB变色的边框颜色（基于系统时间循环）
     * @return ARGB格式的颜色值（透明度255）
     */
    private static int getDynamicBorderColor() {
        long currentTime = System.currentTimeMillis();
        // 正弦函数周期：0.001控制变色速度（值越小越慢），+2/+4让三通道错位，实现彩虹渐变
        int red = (int) (Math.sin(currentTime * 0.001) * 127 + 128);   // 0-255范围
        int green = (int) (Math.sin(currentTime * 0.001 + 2) * 127 + 128);
        int blue = (int) (Math.sin(currentTime * 0.001 + 4) * 127 + 128);
        // 组合为ARGB格式（0xFF开头表示透明度100%）
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    // 对外控制方法 =
    //切换UI显示/隐藏
    public static void toggleUI() {
        SHOW_UI = !SHOW_UI;
    }

    //获取UI显示状态
    public static boolean isShowUI() {
        return SHOW_UI;
    }

    //手动刷新数据
    public static void refreshData() {
        LAST_PLAYER_LIST_UPDATE = 0;
        LAST_BALANCE_UPDATE = 0;
    }
}
