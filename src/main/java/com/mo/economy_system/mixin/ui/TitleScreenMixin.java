package com.mo.economy_system.mixin.ui;

import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TitleScreen Mixin
 * 虚拟坐标系统 640x360 (2560x1440 ÷ 4)
 * 主面板: 85%宽 × 75%高 = 544x270
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    // 颜色定义
    private static final int ACCENT_BLUE = 0xFF0088FF;
    private static final int ACCENT_GREEN = 0xFF44FF88;
    private static final int ACCENT_GOLD = 0xFFFFAA44;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;

    @Unique
    private static final ResourceLocation BACKGROUND_TEXTURE =
        new ResourceLocation("economy_system", "background.png");

    @Shadow @Final
    private boolean fading;

    @Shadow
    private long fadeInStart;

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Unique
    private VirtualCoordinateHelper.VirtualSizeResult economySystem$virtualSize = new VirtualCoordinateHelper.VirtualSizeResult();

    @Unique
    private long economySystem$hoverTime = 0;

    @Unique
    private int economySystem$hoveredButtonIndex = -1;

    @Unique
    private int economySystem$panelX = 0;
    @Unique
    private int economySystem$panelY = 0;
    @Unique
    private int economySystem$panelWidth = 0;
    @Unique
    private int economySystem$panelHeight = 0;

    @Inject(method = "init", at = @At("RETURN"))
    private void economySystem$init(CallbackInfo ci) {
        economySystem$hideOriginalButtons();
    }

    @Unique
    private void economySystem$hideOriginalButtons() {
        TitleScreen self = (TitleScreen) (Object) this;
        for (var widget : self.children()) {
            if (widget instanceof AbstractWidget) {
                AbstractWidget aw = (AbstractWidget) widget;
                aw.setX(-1000);
            }
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();

        if (this.fadeInStart == 0L) {
            this.fadeInStart = System.currentTimeMillis();
        }

        long time = System.currentTimeMillis();

        // 计算虚拟坐标系统
        VirtualCoordinateHelper.calculateVirtualSize(this, economySystem$virtualSize);

        float scale = economySystem$virtualSize.uiScale;
        int virtualW = economySystem$virtualSize.virtualWidth;
        int virtualH = economySystem$virtualSize.virtualHeight;

        // 计算缩放后的偏移（居中）
        int scaledWidth = (int) (VirtualCoordinateHelper.BASE_WIDTH * scale);
        int scaledHeight = (int) (VirtualCoordinateHelper.BASE_HEIGHT * scale);
        int offsetX = (this.width - scaledWidth) / 2;
        int offsetY = (this.height - scaledHeight) / 2;

        // 转换鼠标坐标到虚拟坐标
        int vmx = (int) ((mouseX - offsetX) / scale);
        int vmy = (int) ((mouseY - offsetY) / scale);

        // 虚拟坐标下的布局参数（基于 640x360）
        int centerX = virtualW / 2;
        int centerY = virtualH / 2;

        // 主面板: 85%宽 × 75%高
        int panelWidth = (int) (virtualW * 0.85);
        int panelHeight = (int) (virtualH * 0.75);
        int panelX = centerX - panelWidth / 2;
        int panelY = centerY - panelHeight / 2;

        // 检测悬停的按钮
        int newHoveredIndex = -1;
        int gap = 6;
        int smallButtonHeight = 36;
        int columnWidth = (panelWidth - gap * 3) / 2;
        int leftX = panelX + gap;
        int rightX = leftX + columnWidth + gap;
        int rightSmallButtonWidth = (columnWidth - gap) / 2;

        // 左侧 - 多人游戏
        if (vmx >= leftX && vmx <= leftX + columnWidth && vmy >= panelY + gap && vmy <= panelY + panelHeight - gap) {
            newHoveredIndex = 0; // 多人游戏
        }

        // 单人游戏（右上左）
        int singleButtonEnd = rightX + rightSmallButtonWidth;
        if (vmx >= rightX && vmx <= singleButtonEnd && vmy >= panelY + gap && vmy <= panelY + gap + smallButtonHeight) {
            newHoveredIndex = 1; // 单人游戏
        }

        // 设置（右上右）
        int settingsButtonStart = rightX + rightSmallButtonWidth + gap;
        int settingsButtonEnd = rightX + columnWidth;
        if (vmx >= settingsButtonStart && vmx <= settingsButtonEnd && vmy >= panelY + gap && vmy <= panelY + gap + smallButtonHeight) {
            newHoveredIndex = 2; // 设置
        }

        // 更新日志（右中）
        int updateLogY = panelY + gap + smallButtonHeight + gap;
        int updateLogX = rightX;
        int updateLogWidth = columnWidth;
        if (vmx >= updateLogX && vmx <= updateLogX + updateLogWidth && vmy >= updateLogY && vmy <= updateLogY + smallButtonHeight) {
            newHoveredIndex = 3; // 更新日志
        }

        if (newHoveredIndex != this.economySystem$hoveredButtonIndex) {
            this.economySystem$hoveredButtonIndex = newHoveredIndex;
            this.economySystem$hoverTime = time;
        }

        // 渲染背景图片（填满整个屏幕）
        guiGraphics.blit(BACKGROUND_TEXTURE,
            0, 0, this.width, this.height,
            0, 0, 256, 144, 256, 144);

        PoseStack poseStack = guiGraphics.pose();

        // 应用缩放变换（主面板使用虚拟坐标）
        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, 0);
        poseStack.scale(scale, scale, 1.0f);

        // 渲染主面板
        economySystem$renderMainPanel(guiGraphics, panelX, panelY, panelWidth, panelHeight, time);

        poseStack.popPose();

        // 渲染角落文字（使用缩放后的坐标，但位置固定在屏幕边缘）
        economySystem$renderCornerTextScaled(guiGraphics, scale);

        // 保存偏移量和缩放供点击检测使用
        economySystem$panelX = panelX;
        economySystem$panelY = panelY;
        economySystem$panelWidth = panelWidth;
        economySystem$panelHeight = panelHeight;
        economySystem$offsetX = offsetX;
        economySystem$offsetY = offsetY;
        economySystem$scale = scale;
    }

    @Unique
    private int economySystem$offsetX = 0;
    @Unique
    private int economySystem$offsetY = 0;
    @Unique
    private float economySystem$scale = 1.0f;


    @Unique
    private void economySystem$renderMainPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, long time) {
        // 无主面板背景，只有按钮有背景

        // 统一间距系统
        int gap = 6;  // 所有间距统一为 6
        int smallButtonHeight = 36;  // 小按钮高度

        // 左右分栏：[gap][左栏][gap][右栏][gap]
        int columnWidth = (width - gap * 3) / 2;
        int leftX = x + gap;
        int rightX = leftX + columnWidth + gap;

        // 右上单人/设置按钮：精确平分右侧宽度
        int rightSmallButtonWidth = (columnWidth - gap) / 2;

        // ========== 左侧 - 多人游戏（整个区域） ==========
        boolean multiHovered = economySystem$hoveredButtonIndex == 0;
        economySystem$renderMultiplayerButton(guiGraphics, leftX, y + gap, columnWidth, height - gap * 2,
            ACCENT_GREEN, multiHovered);

        // ========== 右上左 - 单人游戏 ==========
        boolean singleHovered = economySystem$hoveredButtonIndex == 1;
        economySystem$renderSmallButton(guiGraphics, rightX, y + gap, rightSmallButtonWidth, smallButtonHeight,
            "单人游戏", "⚔", ACCENT_GOLD, singleHovered);

        // ========== 右上右 - 设置 ==========
        boolean settingsHovered = economySystem$hoveredButtonIndex == 2;
        economySystem$renderSmallButton(guiGraphics, rightX + rightSmallButtonWidth + gap, y + gap, rightSmallButtonWidth, smallButtonHeight,
            "设置", "⚙", ACCENT_BLUE, settingsHovered);

        // ========== 右中 - 更新日志按钮 ==========
        int updateLogY = y + gap + smallButtonHeight + gap;
        int updateLogHeight = smallButtonHeight;
        int updateLogX = rightX;
        int updateLogWidth = columnWidth;
        boolean updateLogHovered = economySystem$hoveredButtonIndex == 3;
        economySystem$renderUpdateLogButton(guiGraphics, updateLogX, updateLogY, updateLogWidth, updateLogHeight, updateLogHovered);

        // ========== 右下 - 资助说明 ==========
        int donateY = updateLogY + updateLogHeight + gap;
        int donateHeight = height - gap * 2 - smallButtonHeight * 2 - gap * 2;  // 总高度 - 上下边距 - 两个按钮高度 - 中间间距
        int donateX = rightX;
        int donateWidth = columnWidth;
        economySystem$renderDonatePanel(guiGraphics, donateX, donateY, donateWidth, donateHeight);
    }

    @Unique
    private void economySystem$renderMultiplayerButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                                       int color, boolean hovered) {
        // 深色半透明背景（只有按钮有背景）
        int bgColor = hovered ? 0xDD000000 : 0xBB000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 边框（悬停时变色）
        int borderColor = hovered ? color : 0xFF666666;
        guiGraphics.fill(x, y, x + width, y + 2, borderColor);
        guiGraphics.fill(x, y + height - 2, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 2, y + height, borderColor);
        guiGraphics.fill(x + width - 2, y, x + width, y + height, borderColor);

        // 图标和标题（左对齐）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 1.0f);
        guiGraphics.drawString(font, "⚁", x / 2.0f + 8, (y + 18) / 2.0f, color, false);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 1.0f);
        guiGraphics.drawString(font, "§l多人游戏", x / 2.0f + 30, (y + 18) / 2.0f, TEXT_WHITE, false);
        poseStack.popPose();

        // 装饰线
        guiGraphics.fill(x + 10, y + 40, x + 55, y + 41, color);

        // 服务器背景文案（完整版）
        int contentX = x + 10;
        int lineY = y + 50;

        guiGraphics.drawString(font, "§f§l2066年§r§f，随着基因工程的研究，", contentX, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§f人类可以通过自身的细胞分裂实现重生，", contentX, lineY, TEXT_WHITE, false);
        lineY += 12;

        guiGraphics.drawString(font, "§7然而一场危机随着这次基因工程悄然降临...", contentX, lineY, TEXT_WHITE, false);
        lineY += 13;

        guiGraphics.drawString(font, "§7您需要与其他玩家展开一场冒险，", contentX, lineY, TEXT_WHITE, false);
        lineY += 11;
        guiGraphics.drawString(font, "§7在§e梦屿§7找到阻止这场危机的办法——", contentX, lineY, TEXT_WHITE, false);
        lineY += 13;

        guiGraphics.drawString(font, "§d故事由您和伙伴书写。", contentX, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§d没有剧本，没有结局。", contentX, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§d您可以成为拯救服务器的§e英雄§d，", contentX, lineY, TEXT_WHITE, false);
        lineY += 11;
        guiGraphics.drawString(font, "§d也可以成为服务器被毁灭的§c帮凶§d...", contentX, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§d无论如何，此时此刻您比任何时刻都需要§6共同合作§d。", contentX, lineY, TEXT_WHITE, false);

        // 底部装饰线
        guiGraphics.fill(x + 10, y + height - 8, x + 50, y + height - 7, color);
    }

    @Unique
    private void economySystem$renderSmallButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                                 String title, String icon, int color, boolean hovered) {
        // 深色半透明背景（只有按钮有背景）
        int bgColor = hovered ? 0xDD000000 : 0xBB000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 边框（悬停时变色）
        int borderColor = hovered ? color : 0xFF666666;
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        // 图标（左侧，垂直居中）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(1.8f, 1.8f, 1.0f);
        int iconX = (int) ((x + 10) / 1.8f);
        int iconY = (int) ((y + height / 2 - 4) / 1.8f);
        guiGraphics.drawString(font, icon, iconX, iconY, color, false);
        poseStack.popPose();

        // 标题（图标右侧，与图标垂直对齐）
        int titleX = x + 28;
        int titleY = y + height / 2 - 5;
        guiGraphics.drawString(font, "§l" + title, titleX, titleY, TEXT_WHITE, false);
    }

    @Unique
    private void economySystem$renderUpdateLogButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                                     boolean hovered) {
        // 深色半透明背景
        int bgColor = hovered ? 0xDD000000 : 0xBB000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 边框（悬停时变色）
        int borderColor = hovered ? 0xFF00AA44 : 0xFF666666;
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        // 图标（左侧）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(1.8f, 1.8f, 1.0f);
        guiGraphics.drawString(font, "📋", (int) ((x + 10) / 1.8f), (int) ((y + height / 2 - 4) / 1.8f), 0xFF00AA44, false);
        poseStack.popPose();

        // 标题（图标右侧）
        guiGraphics.drawString(font, "§l更新日志", x + 28, y + height / 2 - 5, TEXT_WHITE, false);

        // 最新内容预览（右侧）
        String latestUpdate = "§7暂无更新";
        int previewX = x + width - this.font.width(latestUpdate) - 10;
        guiGraphics.drawString(font, latestUpdate, previewX, y + height / 2 - 5, TEXT_GRAY, false);
    }

    @Unique
    private void economySystem$renderInfoPanel(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                                String title, String[] lines) {
        // 半透明黑色背景
        guiGraphics.fill(x, y, x + width, y + height, 0xBB000000);

        // 标题（左对齐）
        guiGraphics.drawString(font, title, x + 10, y + 8, TEXT_WHITE, false);

        // 标题下划线
        guiGraphics.fill(x + 6, y + 22, x + width - 6, y + 23, 0xFF0088FF);

        // 内容（左对齐）
        int lineY = y + 36;
        for (String line : lines) {
            guiGraphics.drawString(font, line, x + 10, lineY, TEXT_WHITE, false);
            lineY += 12;
        }
    }

    @Unique
    private void economySystem$renderDonatePanel(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // 半透明黑色背景
        guiGraphics.fill(x, y, x + width, y + height, 0xBB000000);

        // 标题（左对齐）
        guiGraphics.drawString(font, "§7本服为§e非营利公益服§7，", x + 10, y + 8, TEXT_WHITE, false);

        // 标题下划线
        guiGraphics.fill(x + 6, y + 22, x + width - 6, y + 23, 0xFFAA4444);

        // 内容（左对齐）
        int lineY = y + 36;
        guiGraphics.drawString(font, "§e公益服维持不易，感谢所有资助者§7。", x + 10, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§7无偿资助§c无法获得§7游戏内权益和物资，", x + 10, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§7请您资助前三思。", x + 10, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§7资助者可按照您的要求自定义设计武器/装备/物品等，", x + 10, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§7且可以自定义属性、外观（数值保证合理），", x + 10, lineY, TEXT_WHITE, false);
        lineY += 12;
        guiGraphics.drawString(font, "§7您的自定义物品开发完成后可以让所有人§a获取§7。", x + 10, lineY, TEXT_WHITE, false);
        lineY += 14;
        guiGraphics.drawString(font, "§8开发者：QINGMO、HANHANYU", x + 10, lineY, TEXT_GRAY, false);
        lineY += 11;
        guiGraphics.drawString(font, "§7如果您有特长（建筑/编程/策划等）欢迎", x + 10, lineY, TEXT_WHITE, false);
        lineY += 11;
        guiGraphics.drawString(font, "§7加入开发团队，参与后续制作！", x + 10, lineY, TEXT_WHITE, false);
    }

    /**
     * 渲染角落文字（使用虚拟坐标缩放，但位置固定在屏幕边缘）
     */
    @Unique
    private void economySystem$renderCornerTextScaled(GuiGraphics guiGraphics, float scale) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 只应用缩放，不应用偏移（让文字固定在屏幕边缘）
        poseStack.scale(scale, scale, 1.0f);

        // 使用虚拟坐标位置（会随 scale 缩放，但始终从屏幕边缘开始）
        int virtualW = economySystem$virtualSize.virtualWidth;
        int virtualH = economySystem$virtualSize.virtualHeight;

        // 左上角 - DreamingFish
        guiGraphics.drawString(this.font, "§b§lDreaming§d§lFish §7- §6§l梦鱼服-「守望梦屿」 §7v0.1(private)", 5, 5, TEXT_WHITE, false);

        // 左下角 - Minecraft 1.20.1
        guiGraphics.drawString(this.font, "§7Minecraft §f1.20.1", 5, virtualH - 10, TEXT_GRAY, false);

        // 右下角 - Mod、语言和退出按钮
        guiGraphics.drawString(this.font, "§7模组[📦]", virtualW - 155, virtualH - 10, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, "§7语言[🌐]", virtualW - 95, virtualH - 10, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, "§c退出[✕]", virtualW - 35, virtualH - 10, 0xFF666666, false);

        // 版权声明（右上角，右对齐）
        String dreamingFishCopyright = "© 2026 DreamingFish - EconomySystem";
        String developerCopyright = "  Developed by QINGMO & HANHANYU";
        guiGraphics.drawString(this.font, "§6" + dreamingFishCopyright, virtualW - this.font.width(dreamingFishCopyright) - 5, 5, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, "§6" + developerCopyright, virtualW - this.font.width(developerCopyright) - 5, 17, TEXT_GRAY, false);

        poseStack.popPose();
    }

    @Unique
    private void economySystem$renderCornerText(GuiGraphics guiGraphics) {
        // 使用虚拟坐标（640x360），相对于虚拟坐标系统边缘
        int virtualW = economySystem$virtualSize.virtualWidth;
        int virtualH = economySystem$virtualSize.virtualHeight;

        // 左下角 - Minecraft 1.20.1
        guiGraphics.drawString(this.font, "§7Minecraft §f1.20.1", 5, virtualH - 10, TEXT_GRAY, false);

        // 右下角 - Mod、语言和退出按钮
        guiGraphics.drawString(this.font, "§7模组[📦]", virtualW - 155, virtualH - 10, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, "§7语言[🌐]", virtualW - 95, virtualH - 10, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, "§c退出[✕]", virtualW - 35, virtualH - 10, 0xFF666666, false);

        // 版权声明（右上角，右对齐）
        String dreamingFishCopyright = "© 2026 DreamingFish - EconomySystem";
        String developerCopyright = "  Developed by QINGMO & HANHANYU";
        guiGraphics.drawString(this.font, "§6" + dreamingFishCopyright, virtualW - this.font.width(dreamingFishCopyright) - 5, 5, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, "§6" + developerCopyright, virtualW - this.font.width(developerCopyright) - 5, 17, TEXT_GRAY, false);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void economySystem$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;

        Minecraft mc = Minecraft.getInstance();

        // 转换鼠标坐标到虚拟坐标
        int vmx = (int) ((mouseX - economySystem$offsetX) / economySystem$scale);
        int vmy = (int) ((mouseY - economySystem$offsetY) / economySystem$scale);

        int panelX = economySystem$panelX;
        int panelY = economySystem$panelY;
        int panelWidth = economySystem$panelWidth;
        int panelHeight = economySystem$panelHeight;

        // 统一间距系统（与渲染保持一致）
        int gap = 6;
        int smallButtonHeight = 36;
        int columnWidth = (panelWidth - gap * 3) / 2;
        int leftX = panelX + gap;
        int rightX = leftX + columnWidth + gap;
        int rightSmallButtonWidth = (columnWidth - gap) / 2;

        // 左侧 - 多人游戏（整个区域）
        if (vmx >= leftX && vmx <= leftX + columnWidth && vmy >= panelY + gap && vmy <= panelY + panelHeight - gap) {
            economySystem$openMultiplayer(mc);
            cir.setReturnValue(true);
            return;
        }

        // 右上左 - 单人游戏
        int singleButtonEnd = rightX + rightSmallButtonWidth;
        if (vmx >= rightX && vmx <= singleButtonEnd && vmy >= panelY + gap && vmy <= panelY + gap + smallButtonHeight) {
            economySystem$openSingleplayer(mc);
            cir.setReturnValue(true);
            return;
        }

        // 右上右 - 设置
        int settingsButtonStart = rightX + rightSmallButtonWidth + gap;
        int settingsButtonEnd = rightX + columnWidth;
        if (vmx >= settingsButtonStart && vmx <= settingsButtonEnd && vmy >= panelY + gap && vmy <= panelY + gap + smallButtonHeight) {
            economySystem$openSettings(mc);
            cir.setReturnValue(true);
            return;
        }

        // 右中 - 更新日志
        int updateLogY = panelY + gap + smallButtonHeight + gap;
        int updateLogX = rightX;
        int updateLogWidth = columnWidth;
        if (vmx >= updateLogX && vmx <= updateLogX + updateLogWidth && vmy >= updateLogY && vmy <= updateLogY + smallButtonHeight) {
            economySystem$openUpdateLog(mc);
            cir.setReturnValue(true);
            return;
        }

        // ========== 右下角按钮（使用虚拟坐标，但无 offset） ==========
        // 转换：屏幕坐标 -> 只有缩放的虚拟坐标
        int vmxNoOffset = (int) (mouseX / economySystem$scale);
        int vmyNoOffset = (int) (mouseY / economySystem$scale);

        int virtualW = economySystem$virtualSize.virtualWidth;
        int virtualH = economySystem$virtualSize.virtualHeight;
        int buttonY = virtualH - 10;

        // 模组按钮
        int modButtonX = virtualW - 155;
        if (vmxNoOffset >= modButtonX && vmxNoOffset <= modButtonX + 50 && vmyNoOffset >= buttonY && vmyNoOffset <= buttonY + 10) {
            TitleScreen self = (TitleScreen) (Object) this;
            mc.setScreen(new net.minecraftforge.client.gui.ModListScreen(self));
            cir.setReturnValue(true);
            return;
        }

        // 语言按钮
        int langButtonX = virtualW - 95;
        if (vmxNoOffset >= langButtonX && vmxNoOffset <= langButtonX + 50 && vmyNoOffset >= buttonY && vmyNoOffset <= buttonY + 10) {
            TitleScreen self = (TitleScreen) (Object) this;
            mc.setScreen(new net.minecraft.client.gui.screens.LanguageSelectScreen(self, mc.options, mc.getLanguageManager()));
            cir.setReturnValue(true);
            return;
        }

        // 退出按钮
        int exitButtonX = virtualW - 35;
        if (vmxNoOffset >= exitButtonX && vmxNoOffset <= exitButtonX + 30 && vmyNoOffset >= buttonY && vmyNoOffset <= buttonY + 10) {
            mc.stop();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void economySystem$openMultiplayer(Minecraft mc) {
        TitleScreen self = (TitleScreen) (Object) this;
        boolean skipWarning = mc.options.skipMultiplayerWarning;
        net.minecraft.client.gui.screens.Screen newScreen = skipWarning
            ? new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(self)
            : new net.minecraft.client.gui.screens.multiplayer.SafetyScreen(self);
        mc.setScreen(newScreen);
    }

    @Unique
    private void economySystem$openSettings(Minecraft mc) {
        TitleScreen self = (TitleScreen) (Object) this;
        mc.setScreen(new net.minecraft.client.gui.screens.OptionsScreen(self, mc.options));
    }

    @Unique
    private void economySystem$openSingleplayer(Minecraft mc) {
        TitleScreen self = (TitleScreen) (Object) this;
        mc.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(self));
    }

    @Unique
    private void economySystem$openUpdateLog(Minecraft mc) {
        // TODO: 实现更新日志界面
        // 暂时不做任何操作，等后续实现界面
    }
}
