//package com.mo.economy_system.core.playerattributes_system.strength;
//
//import com.mo.economy_system.EconomySystem;
//import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
//import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.client.event.RenderGuiEvent;
//import net.minecraftforge.event.TickEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//import java.util.UUID;
//
///**
// * 体力UI渲染类（修复实时更新+调整尺寸/位置）
// * 物品栏右侧小型柱状进度条（2个格子长，靠底部显示）
// */
//@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
//public class StrengthUI {
//    // ========== 修复：调整进度条尺寸（更短）+ 靠底部 ==========
//    private static final int BAR_WIDTH = 36;          // 2个物品栏格子长度（18px/格 × 2），可改54px（3格）
//    private static final int BAR_HEIGHT = 8;          // 更小巧的高度
//    private static final int BAR_MARGIN_X = 5;        // 水平间距（物品栏右侧）
//    private static final int BAR_MARGIN_Y = 2;        // 垂直间距（物品栏底部）
//    private static final int BACKGROUND_ALPHA = 100;  // 背景透明度
//
//    // 修复：缓存当前体力，用于对比更新（确保实时性）
//    private static int LAST_STRENGTH = -1;
//    private static int LAST_MAX_STRENGTH = -1;
//
//    /**
//     * 客户端Tick：重置缓存，强制进度条更新
//     */
//    @SubscribeEvent
//    public static void onClientTick(TickEvent.ClientTickEvent event) {
//        if (event.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) {
//            return;
//        }
//        // 每帧重置缓存，确保渲染时重新获取最新数据
//        LAST_STRENGTH = -1;
//        LAST_MAX_STRENGTH = -1;
//    }
//
//    /**
//     * 渲染体力进度条（修复实时更新+位置靠底部）
//     */
//    @SubscribeEvent
//    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
//        Minecraft mc = Minecraft.getInstance();
//
//        // 空值防护
//        if (mc.player == null || mc.isPaused() || mc.screen != null) {
//            return;
//        }
//
//        GuiGraphics guiGraphics = event.getGuiGraphics();
//        int screenWidth = event.getWindow().getGuiScaledWidth();
//        int screenHeight = event.getWindow().getGuiScaledHeight();
//
//        // ========== 1. 强制获取最新体力数据（修复实时更新） ==========
//        UUID playerUUID = mc.player.getUUID();
//        // 每次渲染都重新获取，不缓存
//        PlayerAttributesData attrData = PlayerAttributesDataManager.getPlayerAttributesData(playerUUID);
//        if (attrData == null) {
//            return;
//        }
//        int currentStrength = attrData.getCurrentStrength();
//        int maxStrength = attrData.getMaxStrength();
//
//        // 避免重复渲染（可选优化，不影响实时性）
//        if (currentStrength == LAST_STRENGTH && maxStrength == LAST_MAX_STRENGTH) {
//            return;
//        }
//        LAST_STRENGTH = currentStrength;
//        LAST_MAX_STRENGTH = maxStrength;
//
//        float strengthPercent = maxStrength == 0 ? 0 : (float) currentStrength / maxStrength;
//
//        // ========== 2. 调整位置：靠物品栏底部（修复没靠到底部） ==========
//        // MC原版物品栏：x=(screenWidth/2)-91，y=screenHeight-39（底部Y坐标）
//        int hotbarRightX = (screenWidth / 2) + 91;  // 物品栏最右侧X
//        // 进度条Y坐标：物品栏底部 + 2px 间距（靠底部）
//        int barY = screenHeight - 39 + BAR_MARGIN_Y;
//        // 进度条X坐标：物品栏右侧 + 5px 间距
//        int barX = hotbarRightX + BAR_MARGIN_X;
//
//        // ========== 3. 绘制进度条（更短+靠底部） ==========
//        // 背景（半透深灰）
//        int bgColor = (BACKGROUND_ALPHA << 24) | 0x1A1A1A;
//        guiGraphics.fill(RenderType.gui(), barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, bgColor);
//
//        // 1px黑边框
//        int borderColor = 0xFF000000;
//        guiGraphics.fill(RenderType.gui(), barX, barY, barX + BAR_WIDTH, barY + 1, borderColor);          // 上边框
//        guiGraphics.fill(RenderType.gui(), barX, barY + BAR_HEIGHT - 1, barX + BAR_WIDTH, barY + BAR_HEIGHT, borderColor); // 下边框
//        guiGraphics.fill(RenderType.gui(), barX, barY, barX + 1, barY + BAR_HEIGHT, borderColor);          // 左边框
//        guiGraphics.fill(RenderType.gui(), barX + BAR_WIDTH - 1, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, borderColor); // 右边框
//
//        // 进度填充（内缩1px）
//        int fillWidth = Math.max(1, (int) (BAR_WIDTH * strengthPercent)); // 至少1px，避免完全消失
//        int fillColor;
//        if (strengthPercent > 0.6f) {
//            fillColor = 0xFF4CAF50; // 绿
//        } else if (strengthPercent > 0.3f) {
//            fillColor = 0xFFFFC107; // 黄
//        } else {
//            fillColor = 0xFFF44336; // 红
//        }
//        guiGraphics.fill(RenderType.gui(),
//                barX + 1, barY + 1,
//                barX + fillWidth, barY + BAR_HEIGHT - 1,
//                fillColor);
//    }
//}