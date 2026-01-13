package com.mo.economy_system.core.playerlevel_system.overalllevel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mo.economy_system.EconomySystem;

/**
 * 经验获得 Toast 提示（样式同步右上角玩家信息框）
 *
 * 功能：玩家获得经验时在屏幕上方显示提示框，包含等级、获得经验、当前经验进度
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class ExperienceToast {

    /** 当前显示的Toast实例（单例模式，同时只显示一个） */
    private static ToastInstance currentToast = null;

    /**
     * 显示经验Toast
     * @param experienceGained 本次获得的经验值
     * @param currentLevel 当前等级
     * @param progress 当前等级进度（0.0-1.0）
     */
    public static void show(long experienceGained, int currentLevel, float progress) {
        if (currentToast != null && !currentToast.isFinished()) {
            // 如果已有Toast在显示，更新数据并重置计时器
            currentToast.update(experienceGained, currentLevel, progress);
        } else {
            // 创建新Toast
            currentToast = new ToastInstance(experienceGained, currentLevel, progress);
        }
    }

    /** 客户端Tick事件处理（每20ms一次） */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (currentToast != null) {
            currentToast.tick();  // 增加计时器
            if (currentToast.isFinished()) {
                currentToast = null;  // 时间到，清除Toast
            }
        }
    }

    /** 渲染事件处理（每帧调用，用于绘制Toast） */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (currentToast != null && !currentToast.isFinished()) {
            currentToast.render(event.getGuiGraphics(), Minecraft.getInstance());
        }
    }

    /**
     * Toast实例类
     * 包含Toast的所有数据和渲染逻辑
     */
    private static class ToastInstance {

        // ==================== 可调整参数 ====================

        /** Toast显示时长（单位：tick，20tick = 1秒） */
        private static final int DISPLAY_TIME_TICKS = 30;  // 1.5秒

        /** 进度条高度（像素） */
        private static final int PROGRESS_BAR_HEIGHT = 5;

        // ==================== 数据字段 ====================

        /** 本次获得的经验值（可能累加多次） */
        private long experienceGained;

        /** 当前等级内已获得的经验 */
        private long currentExp;

        /** 升到下一级所需的总经验 */
        private long neededExp;

        /** 当前等级 */
        private int currentLevel;

        /** 当前等级进度（0.0-1.0，用于进度条） */
        private float progress;

        /** 计时器（记录已显示的tick数） */
        private int tickCounter;

        /**
         * 构造Toast实例
         */
        public ToastInstance(long experienceGained, int currentLevel, float progress) {
            this.experienceGained = experienceGained;
            this.currentLevel = currentLevel;
            this.progress = progress;
            this.tickCounter = 0;

            // 从客户端获取当前经验和所需经验
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                this.currentExp = PlayerLevelManager.getPlayerExperienceClient(mc.player);
                this.neededExp = PlayerLevelManager.getExperienceNeededForNextLevelClient(mc.player);
            }
        }

        /**
         * 更新Toast数据（当玩家连续获得经验时调用）
         */
        public void update(long experienceGained, int currentLevel, float progress) {
            this.experienceGained += experienceGained;  // 累加获得的经验
            this.currentLevel = currentLevel;
            this.progress = progress;
            this.tickCounter = 0;  // 重置计时器，延长显示时间

            // 更新经验值
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                this.currentExp = PlayerLevelManager.getPlayerExperienceClient(mc.player);
                this.neededExp = PlayerLevelManager.getExperienceNeededForNextLevelClient(mc.player);
            }
        }

        /** 计时器前进一步 */
        public void tick() {
            tickCounter++;
        }

        /** 检查是否显示完毕 */
        public boolean isFinished() {
            return tickCounter >= DISPLAY_TIME_TICKS;
        }

        /**
         * 渲染Toast
         */
        public void render(GuiGraphics guiGraphics, Minecraft mc) {
            int screenWidth = mc.getWindow().getGuiScaledWidth();

            // ==================== 文本内容 ====================
            // 格式代码：§e=黄色 §f=白色 §7=灰色
            String line1 = "§eLv." + currentLevel + " §f+§e" + experienceGained + "§7(" + currentExp + "/" + neededExp + ")";

            // ==================== 尺寸参数 ====================
            int lineHeight = mc.font.lineHeight;  // 字体行高（通常为9像素）
            int padding = 6;           // 框内边距（文字到框边缘的距离）
            int progressBarMargin = 4; // 进度条到框边缘的距离
            int spacing = 2;           // 文字和进度条之间的间距

            // 计算Toast宽度和高度
            int width = mc.font.width(line1) + padding * 2;
            int height = padding + lineHeight + spacing + PROGRESS_BAR_HEIGHT + progressBarMargin + padding;

            // 计算Toast位置（屏幕水平居中，顶部偏移8像素）
            int x = (screenWidth - width) / 2;
            int y = 8;  // 修改此处可调整垂直位置（数值越大越靠下）

            // ==================== 颜色定义 ====================
            // 动态RGB边框颜色（随时间变化，彩虹效果）
            int dynamicColor = getDynamicBorderColor();

            // 背景颜色：0xD0181818 = 0xD0(透明度约81%) + 0x181818(深灰色)
            // 格式：AARRGGBB（AA=透明度，RR=红色，GG=绿色，BB=蓝色）
            int bgColor = 0xD0181818;

            // 发光颜色：基于动态颜色，透明度30%
            int glowColor = 0x30000000 | (dynamicColor & 0x00FFFFFF);

            // ==================== 绘制背景 ====================
            guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, bgColor);

            // ==================== 绘制发光边框 ====================
            // 在主框外围1像素处绘制发光效果
            guiGraphics.fill(RenderType.gui(), x - 1, y - 1, x + width + 1, y + height + 1, glowColor);

            // ==================== 绘制主边框（四条边） ====================
            guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, dynamicColor);              // 上边
            guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, dynamicColor);  // 下边
            guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, dynamicColor);             // 左边
            guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, dynamicColor);   // 右边

            // ==================== 绘制文字 ====================
            guiGraphics.drawString(mc.font, line1, x + padding, y + padding, 0xFFFFFFFF);  // 0xFFFFFFFF = 白色

            // ==================== 绘制进度条 ====================
            // 进度条位置：文字下方，留出spacing+lineHeight的空间
            int progressBarY = y + padding + lineHeight + spacing + lineHeight;
            int progressBarX = x + progressBarMargin;
            int progressBarWidth = width - progressBarMargin * 2;

            // --- 进度条背景（深灰色） ---
            // 0xDD1A1A1A = 深黑色，透明度约87%
            guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
                    progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, 0xDD1A1A1A);

            // --- 进度条前景（动态颜色） ---
            int progressWidth = (int) (progressBarWidth * progress);  // 根据进度计算宽度
            if (progressWidth > 2) {  // 只有宽度大于2像素才绘制
                // 主体（动态颜色）
                guiGraphics.fill(RenderType.gui(), progressBarX + 1, progressBarY + 1,
                        progressBarX + progressWidth - 1, progressBarY + PROGRESS_BAR_HEIGHT - 1, dynamicColor);
                // 顶部高光（白色，增加立体感）
                guiGraphics.fill(RenderType.gui(), progressBarX + 1, progressBarY + 1,
                        progressBarX + progressWidth - 1, progressBarY + 2, 0xFFFFFFFF);
            }

            // --- 进度条边框（发光效果） ---
            int progressGlowColor = 0x40000000 | (dynamicColor & 0x00FFFFFF);  // 透明度40%
            guiGraphics.fill(RenderType.gui(), progressBarX - 1, progressBarY - 1,
                    progressBarX + progressBarWidth + 1, progressBarY + PROGRESS_BAR_HEIGHT + 1, progressGlowColor);
            // 进度条四边
            guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
                    progressBarX + progressBarWidth, progressBarY + 1, dynamicColor);
            guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY + PROGRESS_BAR_HEIGHT - 1,
                    progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, dynamicColor);
            guiGraphics.fill(RenderType.gui(), progressBarX, progressBarY,
                    progressBarX + 1, progressBarY + PROGRESS_BAR_HEIGHT, dynamicColor);
            guiGraphics.fill(RenderType.gui(), progressBarX + progressBarWidth - 1, progressBarY,
                    progressBarX + progressBarWidth, progressBarY + PROGRESS_BAR_HEIGHT, dynamicColor);
        }

        /**
         * 获取动态RGB边框颜色
         * 使用正弦函数生成随时间循环变化的彩虹颜色
         *
         * @return ARGB格式的颜色值
         */
        private int getDynamicBorderColor() {
            long time = System.currentTimeMillis();

            // RGB分量计算：使用正弦函数生成0-255的值
            // time * 0.001：将毫秒转换为秒，控制变化速度
            // Math.sin(...) * 100 + 155：正弦值(-1到1)映射到(55到255)
            // +2、+4：相位偏移，让三个颜色通道变化不同步，产生彩虹效果
            int red = (int) (Math.sin(time * 0.001) * 100 + 155);      // 红色通道
            int green = (int) (Math.sin(time * 0.001 + 2) * 100 + 155); // 绿色通道（偏移）
            int blue = (int) (Math.sin(time * 0.001 + 4) * 100 + 155);  // 蓝色通道（偏移）

            // 组合成ARGB格式：0xFF000000(完全不透明) + RGB分量
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        }
    }
}
