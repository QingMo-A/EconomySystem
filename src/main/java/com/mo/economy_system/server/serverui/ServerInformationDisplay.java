package com.mo.economy_system.server.serverui;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_OnlinePlayerCountRequest;
import com.mo.economy_system.network.packets.economy_system.Packet_BalanceRequest;
import com.mo.economy_system.server.chattitle.capability.TitleCapabilityProvider;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import com.mo.economy_system.server.rank.capability.RankCapabilityProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class ServerInformationDisplay {
    // UI配置（贴近右上角）
    private static boolean SHOW_UI = true;                  // UI开关
    private static final int BACKGROUND_ALPHA = 128;       // 背景透明度
    private static final int BACKGROUND_PADDING = 3;       // 内边距（贴近边框）
    private static final int LINE_SPACING = 10;            // 文本行间距
    private static final int RIGHT_OFFSET = 5;             // 右上角X偏移（仅5像素）
    private static final int TOP_OFFSET = 5;               // 右上角Y偏移（仅5像素）

    // 客户端缓存数据（从网络包获取）
    public static int ONLINE_PLAYERS = 0;                  // 改为public，让Packet直接更新
    public static int PLAYER_BALANCE = 0;                 // 改为public，让Packet直接更新
    private static double SERVER_TPS = 20.0;               // 服务器TPS（可扩展）

    private static long LAST_PLAYER_LIST_UPDATE = 0;       // 玩家列表最后刷新时间
    private static long LAST_BALANCE_UPDATE = 0;           // 余额最后刷新时间
    private static final long UPDATE_INTERVAL = 5000;      // 5秒刷新一次

    public static int PLAYER_OVERALL_LEVEL = 0; //玩家总等级

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

    // 客户端Tick，触发网络请求 =====================
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.player == null) return;

        long currentTime = System.currentTimeMillis();

        // 请求在线玩家数
        if (currentTime - LAST_PLAYER_LIST_UPDATE > UPDATE_INTERVAL) {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_OnlinePlayerCountRequest());
            LAST_PLAYER_LIST_UPDATE = currentTime;
        }

        // 余额请求
        if (currentTime - LAST_BALANCE_UPDATE > UPDATE_INTERVAL) {
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_BalanceRequest());
            LAST_BALANCE_UPDATE = currentTime;
        }
    }

    // HUD渲染（贴近右上角）
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

        List<Component> infoLines = buildInfoLines(mc);
        if (infoLines.isEmpty()) return;

        int maxTextWidth = calculateMaxTextWidth(infoLines, font);
        int bgWidth = maxTextWidth + BACKGROUND_PADDING * 2;
        int bgHeight = infoLines.size() * LINE_SPACING + BACKGROUND_PADDING * 2;
        int bgX = screenWidth - bgWidth - RIGHT_OFFSET;
        int bgY = TOP_OFFSET;
        int baseX = bgX + BACKGROUND_PADDING;
        int baseY = bgY + BACKGROUND_PADDING;

        renderBackground(guiGraphics, bgX, bgY, bgWidth, bgHeight);
        renderInfoLines(guiGraphics, infoLines, font, baseX, baseY);
    }

    // 构建显示文本
    private static List<Component> buildInfoLines(Minecraft mc) {
        List<Component> lines = new ArrayList<>();

        // 标题
        lines.add(Component.literal("§b=== " + "Dreaming" + "§dFish" + " ==="));
        lines.add(Component.literal(""));

        Rank playerRank = RankCapabilityProvider.getPlayerRank(mc.player);
        String rankId = "NO_RANK";
        rankId = playerRank.getRankName();
        String titleName = "萌新鱼友";
        titleName = TitleCapabilityProvider.getPlayerTitle(mc.player).getTitleName();

        // 玩家信息
        if (mc.player != null) {
            lines.add(Component.literal("§6鱼友: §e" + mc.player.getName().getString()));
            lines.add(Component.literal("§6余额: §e" + PLAYER_BALANCE + " 梦鱼币"));
            lines.add(Component.literal("§6梦鱼等级: §e" + PLAYER_OVERALL_LEVEL));

            ChatFormatting textColorFormatting;
            if (Objects.equals(playerRank.getRankName(), RankRegistry.NO_RANK.getRankName())) {
                textColorFormatting = ChatFormatting.WHITE;
            } else if (Objects.equals(playerRank.getRankName(), RankRegistry.FISH.getRankName())) {
                textColorFormatting = ChatFormatting.GREEN;
            } else if (Objects.equals(playerRank.getRankName(), RankRegistry.FISH_PLUS.getRankName())) {
                textColorFormatting = ChatFormatting.AQUA;
            } else if (Objects.equals(playerRank.getRankName(), RankRegistry.FISH_PLUS_PLUS.getRankName())) {
                textColorFormatting = ChatFormatting.GOLD;
            } else if  (Objects.equals(playerRank.getRankName(), RankRegistry.OPERATOR.getRankName())) {
                textColorFormatting = ChatFormatting.RED;
            } else {
                textColorFormatting = ChatFormatting.WHITE;
            }


            Component rankLine = Component.literal("RANK:")
                    .withStyle(ChatFormatting.GOLD) // 前缀金色
                    .append(Component.literal(rankId).withStyle(textColorFormatting));
            lines.add(rankLine);

            Component titleLine = Component.literal("称号:")
                    .withStyle(ChatFormatting.GOLD) // 前缀金色
                    .append(Component.literal(titleName).withStyle(textColorFormatting));
            lines.add(titleLine);
        }

        lines.add(Component.literal(""));

        // 服务器信息
        lines.add(Component.literal("§b在线人数: §7" + ONLINE_PLAYERS));
        lines.add(Component.literal("§bTPS: §7" + String.format("%.1f", SERVER_TPS)));
        lines.add(Component.literal(""));

        lines.add(Component.literal("§bDreaming§dFish§6.top"));

        return lines;
    }

    // 计算文本最大宽度
    private static int calculateMaxTextWidth(List<Component> lines, Font font) {
        int maxWidth = 0;
        for (Component line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        return maxWidth;
    }

    // ========== 关键修改：新增动态颜色计算方法 ==========
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

    // 渲染背景
    private static void renderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 半透明黑色背景（ARGB）
        int bgColor = (BACKGROUND_ALPHA << 24) | 0x000000;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, bgColor);

        int borderColor = getDynamicBorderColor();
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);          // 上边框
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, borderColor); // 下边框
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, borderColor);          // 左边框
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, borderColor); // 右边框
    }

    // 渲染文本
    private static void renderInfoLines(GuiGraphics guiGraphics, List<Component> lines, Font font, int baseX, int baseY) {
        int currentY = baseY;
        for (Component line : lines) {
            guiGraphics.drawString(font, line, baseX, currentY, 0xFFFFFF); // 白色文本
            currentY += LINE_SPACING;
        }
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