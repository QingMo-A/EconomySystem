package com.mo.economy_system.mixin.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mo.economy_system.client.util.UiBackgroundRenderer;
import com.mo.economy_system.client.util.VirtualCoordinateHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * TitleScreen Mixin
 * 虚拟坐标系统 640x360 (2560x1440 ÷ 4)
 * 主面板: 85%宽 × 75%高 = 544x270
 */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    // ==================== 字符串常量 ====================
    // 按钮文本
    private static final String BUTTON_SINGLEPLAYER = "单人游戏";
    private static final String BUTTON_SETTINGS = "设置";
    private static final String BUTTON_MULTIPLAYER = "多人游戏";
    private static final String BUTTON_UPDATE_LOG = "更新日志";
    private static final String BUTTON_MODS = "模组[📦]";
    private static final String BUTTON_LANGUAGE = "语言[🌐]";
    private static final String BUTTON_EXIT = "§c退出[✕]";
    @Unique
    private static volatile String economySystem$updateLogPreview = "§7暂无更新";
    @Unique
    private static volatile boolean economySystem$updateLogFetchStarted = false;

    private static final String UPDATE_LOG_URL = "https://github.com/QingMo-A/EconomySystem/releases";
    private static final String UPDATE_LOG_API_URL = "https://api.github.com/repos/QingMo-A/EconomySystem/releases/latest";

    // 版权和版本信息
    private static final String MINECRAFT_VERSION = "§7Minecraft §f1.20.1";
    private static final String COPYRIGHT_TEXT = "Copyright Mojang AB. Do not distribute!";
    private static final String DREAMINGFISH_TITLE = "§b§lDreaming§d§lFish §7- §6§l梦鱼服-「守望梦屿」 §7v0.1(private)";
    private static final String DREAMINGFISH_COPYRIGHT = "© 2026 DreamingFish - EconomySystem";
    private static final String DEVELOPER_COPYRIGHT = "  Developed by QINGMO & HANHANYU";
    private static final String DEVELOPER_INFO = "§8开发者：QINGMO、HANHANYU";

    // 服务器背景文案
    private static final String STORY_LINE_1 = "§f§l2066年§r§f，随着基因工程的研究，";
    private static final String STORY_LINE_2 = "§f人类可以通过自身的细胞分裂实现重生，";
    private static final String STORY_LINE_3 = "§7然而一场危机随着这次基因工程悄然降临...";
    private static final String STORY_LINE_4 = "§7您需要与其他玩家展开一场冒险，";
    private static final String STORY_LINE_5 = "§7在§e梦屿§7找到阻止这场危机的办法——";
    private static final String STORY_LINE_6 = "§d故事由您和伙伴书写。";
    private static final String STORY_LINE_7 = "§d没有剧本，没有结局。";
    private static final String STORY_LINE_8 = "§d您可以成为拯救服务器的§e英雄§d，";
    private static final String STORY_LINE_9 = "§d也可以成为服务器被毁灭的§c帮凶§d...";
    private static final String STORY_LINE_10 = "§d无论如何，此时此刻您比任何时刻都需要§6共同合作§d。";

    // 资助面板文案
    private static final String DONATE_TITLE = "§7本服为§e非营利公益服§7，";
    private static final String DONATE_LINE_1 = "§e公益服维持不易，感谢所有资助者§7。";
    private static final String DONATE_LINE_2 = "§7无偿资助§c无法获得§7游戏内权益和物资，";
    private static final String DONATE_LINE_3 = "§7请您资助前三思。";
    private static final String DONATE_LINE_4 = "§7资助者可按照您的要求自定义设计武器/装备/物品等，";
    private static final String DONATE_LINE_5 = "§7且可以自定义属性、外观（数值保证合理），";
    private static final String DONATE_LINE_6 = "§7您的自定义物品开发完成后可以让所有人§a获取§7。";
    private static final String DONATE_LINE_7 = "§7如果您有特长（建筑/编程/策划等）欢迎";
    private static final String DONATE_LINE_8 = "§7加入开发团队，参与后续制作！";

    // 图标（已移除emoji，使用纯文本）
    private static final String ICON_MULTIPLAYER = "";
    private static final String ICON_SINGLEPLAYER = "";
    private static final String ICON_SETTINGS = "";
    private static final String ICON_UPDATE_LOG = "";

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

    @Unique
    private int economySystem$copyrightX = 0;
    @Unique
    private int economySystem$copyrightY = 0;
    @Unique
    private int economySystem$copyrightWidth = 0;
    @Unique
    private int economySystem$copyrightHeight = 0;

    @Unique
    private long economySystem$openTime = 0;

    @Unique
    private static final long ANIMATION_DURATION = 600; // 600ms 滑入动画
    @Unique
    private static final float EASE_POWER = 2.0F; // 缓动指数

    @Inject(method = "init", at = @At("RETURN"))
    private void economySystem$init(CallbackInfo ci) {
        economySystem$startUpdateLogFetch();
        // openTime 会在渐显完成时设置
    }

    @Unique
    private String economySystem$getTranslationKey(Component component) {
        // 获取Component的Contents，如果是TranslatableContents则返回key
        if (component.getContents() instanceof TranslatableContents) {
            return ((TranslatableContents) component.getContents()).getKey();
        }
        return null;
    }

    @Unique
    private boolean economySystem$isVanillaButtonKey(String key) {
        if (key == null) {
            return false;
        }

        // 原版按钮的翻译键
        return "menu.singleplayer".equals(key)
                || "menu.multiplayer".equals(key)
                || "menu.online".equals(key)          // Realms
                || "menu.options".equals(key)         // 设置/选项
                || "menu.quit".equals(key)            // 退出
                || "narrator.button.language".equals(key)
                || "narrator.button.accessibility".equals(key)
                || "fml.menu.mods".equals(key);       // Forge模组按钮，我们也隐藏它
    }

    @Unique
    private void economySystem$relayModButtons(java.util.List<AbstractWidget> modButtons) {
        if (modButtons.isEmpty()) {
            return;
        }

        // 右下角区域
        int startX = this.width - 210;  // 从右边210像素开始
        int startY = this.height - 30;  // 从底部30像素开始
        int buttonGap = 5;
        int maxButtonsPerRow = 4;

        for (int i = 0; i < modButtons.size(); i++) {
            AbstractWidget btn = modButtons.get(i);
            int row = i / maxButtonsPerRow;
            int col = i % maxButtonsPerRow;

            // 计算按钮位置（从右到左排列）
            int btnX = startX - col * (btn.getWidth() + buttonGap);
            int btnY = startY - row * (btn.getHeight() + buttonGap);

            btn.setX(btnX);
            btn.setY(btnY);
        }

        // 保存模组按钮数量供渲染使用
        economySystem$modButtonCount = modButtons.size();
    }

    @Unique
    private int economySystem$modButtonCount = 0;

    @Unique
    private void economySystem$hideVanillaButtons() {
        TitleScreen self = (TitleScreen) (Object) this;

        // 遍历所有子元素，隐藏原版按钮
        for (var widget : self.children()) {
            if (widget instanceof AbstractWidget) {
                AbstractWidget aw = (AbstractWidget) widget;
                String translationKey = economySystem$getTranslationKey(aw.getMessage());

                if (economySystem$isVanillaButtonKey(translationKey)) {
                    // 隐藏原版按钮（移到屏幕外）
                    aw.setX(-1000);
                } else if (aw instanceof PlainTextButton) {
                    // 隐藏原版版权按钮
                    aw.setX(-1000);
                }
            }
        }
    }

    @Unique
    private void economySystem$hideVanillaButtonsAndRelayModButtons() {
        TitleScreen self = (TitleScreen) (Object) this;

        // 收集模组按钮（非原版按钮）
        java.util.List<AbstractWidget> modButtons = new java.util.ArrayList<>();

        for (var widget : self.children()) {
            if (widget instanceof AbstractWidget) {
                AbstractWidget aw = (AbstractWidget) widget;
                String translationKey = economySystem$getTranslationKey(aw.getMessage());

                if (economySystem$isVanillaButtonKey(translationKey)) {
                    // 隐藏原版按钮（移到屏幕外）
                    aw.setX(-1000);
                } else if (aw instanceof PlainTextButton) {
                    // 隐藏原版版权按钮
                    aw.setX(-1000);
                } else if (aw.getHeight() <= 15 && aw.getY() > this.height - 30) {
                    // 其他底部小按钮，隐藏（我们自己绘制）
                    aw.setX(-1000);
                } else if (aw.getX() >= 0) {
                    // 收集模组按钮（未被隐藏的）
                    modButtons.add(aw);
                }
            }
        }

        // 将模组按钮重新排列到右下角
        economySystem$relayModButtons(modButtons);
    }

    @Unique
    private void economySystem$renderFadeIn(GuiGraphics guiGraphics, float fadeAlpha) {
        // 渲染淡入效果（类似原版的Panorama渲染）
        // 使用纯黑色背景逐渐淡入
        int alphaValue = (int) (fadeAlpha * 255.0F) << 24;
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000 | alphaValue);
    }

    @Unique
    private void economySystem$renderBackground(GuiGraphics guiGraphics, float fadeAlpha) {
        // 渲染自定义背景图（保持宽高比并裁切铺满）
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, fadeAlpha);
        UiBackgroundRenderer.renderCover(guiGraphics, BACKGROUND_TEXTURE, this.width, this.height);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 如果背景图加载失败，渲染深灰色背景
        // guiGraphics.fill(0, 0, this.width, this.height, 0xFF1a1a1a);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void economySystem$render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        // 取消原版渲染，完全替换标题界面
        ci.cancel();

        // 处理淡入效果
        if (this.fadeInStart == 0L) {
            this.fadeInStart = System.currentTimeMillis();
        }

        // 计算淡入alpha值
        float fadeAlpha = this.fading ? java.lang.Math.min((System.currentTimeMillis() - this.fadeInStart) / 1000.0F, 1.0F) : 1.0F;

        // ========== 步骤1: 渲染背景图 ==========
        economySystem$renderBackground(guiGraphics, fadeAlpha);

        // 如果渐显未完成，提前返回（只渲染背景，等渐显完成后再渲染卡片）
        if (fadeAlpha < 1.0F) {
            return;
        }

        // 渐显完成时，初始化动画开始时间
        if (economySystem$openTime == 0) {
            economySystem$openTime = System.currentTimeMillis();
        }

        // ========== 步骤2: 手动调用Forge钩子，让其他模组可以添加按钮 ==========
        net.minecraftforge.client.ForgeHooksClient.renderMainMenu(
            (TitleScreen) (Object) this,
            guiGraphics,
            this.font,
            this.width,
            this.height,
            0xFFFFFFFF
        );

        // ========== 步骤3: 隐藏原版按钮并重新定位模组按钮 ==========
        economySystem$hideVanillaButtonsAndRelayModButtons();

        // ========== 步骤4: 手动调用super.render()来渲染模组按钮 ==========
        super.render(guiGraphics, mouseX, mouseY, partialTick);

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

        // 计算动画进度
        long elapsed = time - economySystem$openTime;
        float animationProgress = Math.min((float) elapsed / ANIMATION_DURATION, 1.0F);
        float easedProgress = economySystem$easeOutCubic(animationProgress);

        PoseStack poseStack = guiGraphics.pose();

        // 应用缩放变换（主面板使用虚拟坐标）
        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, 0);
        poseStack.scale(scale, scale, 1.0f);

        // 渲染主面板（覆盖在原版内容之上，带动画）
        economySystem$renderMainPanel(guiGraphics, panelX, panelY, panelWidth, panelHeight, time, easedProgress);

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
    private void economySystem$renderMainPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, long time, float animProgress) {
        // 无主面板背景，只有按钮有背景

        // 统一间距系统
        int gap = 6;
        int smallButtonHeight = 36;

        // 左右分栏
        int columnWidth = (width - gap * 3) / 2;
        int leftX = x + gap;
        int rightX = leftX + columnWidth + gap;
        int rightSmallButtonWidth = (columnWidth - gap) / 2;

        // ========== 统一的优雅动画：所有卡片从下方轻微滑入 + 淡入 ==========
        // 多人游戏 (第一个出现)
        float multiProgress = economySystem$getStaggeredProgress(animProgress, 0.0F);
        int multiOffsetY = (int) ((1.0F - multiProgress) * 24); // 从下方24px滑入

        // 单人游戏 (第二个)
        float singleProgress = economySystem$getStaggeredProgress(animProgress, 0.12F);
        int singleOffsetY = (int) ((1.0F - singleProgress) * 24);

        // 设置 (第三个)
        float settingsProgress = economySystem$getStaggeredProgress(animProgress, 0.24F);
        int settingsOffsetY = (int) ((1.0F - settingsProgress) * 24);

        // 更新日志 (第四个)
        int updateLogY = y + gap + smallButtonHeight + gap;
        float updateLogProgress = economySystem$getStaggeredProgress(animProgress, 0.36F);
        int updateLogOffsetY = (int) ((1.0F - updateLogProgress) * 24);

        // 资助面板 (第五个)
        int donateY = updateLogY + smallButtonHeight + gap;
        float donateProgress = economySystem$getStaggeredProgress(animProgress, 0.48F);
        int donateOffsetY = (int) ((1.0F - donateProgress) * 24);

        // ========== 渲染各卡片 ==========
        boolean multiHovered = economySystem$hoveredButtonIndex == 0;
        economySystem$renderMultiplayerButton(guiGraphics, leftX, y + gap + multiOffsetY, columnWidth, height - gap * 2,
            ACCENT_GREEN, multiHovered, multiProgress);

        boolean singleHovered = economySystem$hoveredButtonIndex == 1;
        economySystem$renderSmallButton(guiGraphics, rightX, y + gap + singleOffsetY, rightSmallButtonWidth, smallButtonHeight,
            BUTTON_SINGLEPLAYER, ICON_SINGLEPLAYER, ACCENT_GOLD, singleHovered, singleProgress);

        boolean settingsHovered = economySystem$hoveredButtonIndex == 2;
        economySystem$renderSmallButton(guiGraphics, rightX + rightSmallButtonWidth + gap, y + gap + settingsOffsetY, rightSmallButtonWidth, smallButtonHeight,
            BUTTON_SETTINGS, ICON_SETTINGS, ACCENT_BLUE, settingsHovered, settingsProgress);

        boolean updateLogHovered = economySystem$hoveredButtonIndex == 3;
        int updateLogHeight = smallButtonHeight;
        economySystem$renderUpdateLogButton(guiGraphics, rightX, updateLogY + updateLogOffsetY, columnWidth, updateLogHeight, updateLogHovered, updateLogProgress);

        int donateHeight = height - gap * 2 - smallButtonHeight * 2 - gap * 2;
        economySystem$renderDonatePanel(guiGraphics, rightX, donateY + donateOffsetY, columnWidth, donateHeight, donateProgress);
    }

    @Unique
    private float economySystem$getStaggeredProgress(float animProgress, float delay) {
        // 获取错开的动画进度（delay 0-1 之间的值）
        float adjusted = Math.max((animProgress - delay) / (1.0F - delay), 0);
        adjusted = Math.min(adjusted, 1.0F);
        // 使用 smooth step 让动画更柔和
        return economySystem$easeSmooth(adjusted);
    }

    @Unique
    private void economySystem$renderMultiplayerButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                                       int color, boolean hovered, float progress) {
        // 深色半透明背景（稍微降低不透明度）
        int bgAlpha = (int) ((hovered ? 0xAA : 0x99) * progress);
        int bgColor = (bgAlpha << 24) | 0x000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 左侧颜色条纹（4像素宽）
        int stripeAlpha = (int) ((hovered ? 0xFF : 0xCC) * progress);
        int stripeColor = (stripeAlpha << 24) | (color & 0x00FFFFFF);
        guiGraphics.fill(x, y, x + 4, y + height, stripeColor);

        // 顶部渐变光效（悬停时，颜色更深 - alpha值越小越深）
        if (hovered) {
            for (int i = 0; i < 8; i++) {
                int alpha = 48 - i * 4;
                int glowColor = (alpha << 24) | (color & 0x00FFFFFF);
                guiGraphics.fill(x + 4, y + i, x + width, y + i + 1, glowColor);
            }
        }

        // 细边框（只有右侧和底部）
        int borderColor = hovered ? 0x40FFFFFF : 0x20FFFFFF;
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
        guiGraphics.fill(x + 4, y + height - 1, x + width, y + height, borderColor);

        // 标题（更靠上 - y坐标更小，带阴影）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 1.0f);
        guiGraphics.drawString(font, "§l" + BUTTON_MULTIPLAYER, x / 2.0f + 10, (y + 12) / 2.0f, TEXT_WHITE, true);
        poseStack.popPose();

        // 装饰线
        guiGraphics.fill(x + 10, y + 38, x + 55, y + 39, color);

        // 服务器背景文案（完整版，带阴影）
        int contentX = x + 10;
        int lineY = y + 48;

        guiGraphics.drawString(font, STORY_LINE_1, contentX, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, STORY_LINE_2, contentX, lineY, TEXT_WHITE, true);
        lineY += 12;

        guiGraphics.drawString(font, STORY_LINE_3, contentX, lineY, TEXT_WHITE, true);
        lineY += 13;

        guiGraphics.drawString(font, STORY_LINE_4, contentX, lineY, TEXT_WHITE, true);
        lineY += 11;
        guiGraphics.drawString(font, STORY_LINE_5, contentX, lineY, TEXT_WHITE, true);
        lineY += 13;

        guiGraphics.drawString(font, STORY_LINE_6, contentX, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, STORY_LINE_7, contentX, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, STORY_LINE_8, contentX, lineY, TEXT_WHITE, true);
        lineY += 11;
        guiGraphics.drawString(font, STORY_LINE_9, contentX, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, STORY_LINE_10, contentX, lineY, TEXT_WHITE, true);

        // 底部装饰线
        guiGraphics.fill(x + 10, y + height - 8, x + 50, y + height - 7, color);
    }

    @Unique
    private void economySystem$renderSmallButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                                 String title, String icon, int color, boolean hovered, float progress) {
        // 深色半透明背景（稍微降低不透明度）
        int bgAlpha = (int) ((hovered ? 0xAA : 0x99) * progress);
        int bgColor = (bgAlpha << 24) | 0x000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 左侧颜色条纹（4像素宽）
        int stripeAlpha = (int) ((hovered ? 0xFF : 0xCC) * progress);
        int stripeColor = (stripeAlpha << 24) | (color & 0x00FFFFFF);
        guiGraphics.fill(x, y, x + 4, y + height, stripeColor);

        // 顶部渐变光效（悬停时，颜色更深）
        if (hovered) {
            for (int i = 0; i < 6; i++) {
                int alpha = 36 - i * 4;
                int glowColor = (alpha << 24) | (color & 0x00FFFFFF);
                guiGraphics.fill(x + 4, y + i, x + width, y + i + 1, glowColor);
            }
        }

        // 细边框（只有右侧和底部）
        int borderColor = hovered ? 0x40FFFFFF : 0x20FFFFFF;
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
        guiGraphics.fill(x + 4, y + height - 1, x + width, y + height, borderColor);

        // 标题（垂直居中，留出条纹空间，带阴影）
        int titleX = x + 10;
        int titleY = y + height / 2 - 5;
        guiGraphics.drawString(font, "§l" + title, titleX, titleY, TEXT_WHITE, true);
    }

    @Unique
    private void economySystem$renderUpdateLogButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                                     boolean hovered, float progress) {
        // 深色半透明背景（稍微降低不透明度）
        int bgAlpha = (int) ((hovered ? 0xAA : 0x99) * progress);
        int bgColor = (bgAlpha << 24) | 0x000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 左侧颜色条纹（4像素宽，使用更新日志的绿色）
        int stripeAlpha = (int) (0xFF * progress);
        int stripeColor = (stripeAlpha << 24) | 0x00AA44;
        guiGraphics.fill(x, y, x + 4, y + height, stripeColor);

        // 顶部渐变光效（悬停时，颜色更深）
        if (hovered) {
            for (int i = 0; i < 6; i++) {
                int alpha = 36 - i * 4;
                int glowColor = (alpha << 24) | 0x00AA44;
                guiGraphics.fill(x + 4, y + i, x + width, y + i + 1, glowColor);
            }
        }

        // 细边框（只有右侧和底部）
        int borderColor = hovered ? 0x40FFFFFF : 0x20FFFFFF;
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
        guiGraphics.fill(x + 4, y + height - 1, x + width, y + height, borderColor);

        // 标题（左侧，留出条纹空间，带阴影）
        guiGraphics.drawString(font, "§l" + BUTTON_UPDATE_LOG, x + 10, y + height / 2 - 5, TEXT_WHITE, true);

        // 最新内容预览（右侧）- 使用动态获取的内容，带阴影
        String previewText = economySystem$updateLogPreview;
        int previewX = x + width - this.font.width(previewText) - 10;
        guiGraphics.drawString(font, previewText, previewX, y + height / 2 - 5, TEXT_GRAY, true);
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
    private void economySystem$renderDonatePanel(GuiGraphics guiGraphics, int x, int y, int width, int height, float progress) {
        // 深色半透明背景（稍微降低不透明度）
        int bgAlpha = (int) (0x99 * progress);
        int bgColor = (bgAlpha << 24) | 0x000000;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);

        // 左侧颜色条纹（4像素宽，使用橙红色）
        int stripeAlpha = (int) (0xFF * progress);
        int stripeColor = (stripeAlpha << 24) | 0xAA4444;
        guiGraphics.fill(x, y, x + 4, y + height, stripeColor);

        // 细边框（只有右侧和底部）
        int borderColor = 0x20FFFFFF;
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
        guiGraphics.fill(x + 4, y + height - 1, x + width, y + height, borderColor);

        // 标题（左对齐，留出条纹空间，带阴影）
        guiGraphics.drawString(font, DONATE_TITLE, x + 10, y + 8, TEXT_WHITE, true);

        // 标题下划线
        guiGraphics.fill(x + 6, y + 22, x + width - 6, y + 23, 0x80AA4444);

        // 内容（左对齐，留出条纹空间，带阴影）
        int lineY = y + 36;
        guiGraphics.drawString(font, DONATE_LINE_1, x + 10, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, DONATE_LINE_2, x + 10, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, DONATE_LINE_3, x + 10, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, DONATE_LINE_4, x + 10, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, DONATE_LINE_5, x + 10, lineY, TEXT_WHITE, true);
        lineY += 12;
        guiGraphics.drawString(font, DONATE_LINE_6, x + 10, lineY, TEXT_WHITE, true);
        lineY += 14;
        guiGraphics.drawString(font, DEVELOPER_INFO, x + 10, lineY, TEXT_GRAY, true);
        lineY += 11;
        guiGraphics.drawString(font, DONATE_LINE_7, x + 10, lineY, TEXT_WHITE, true);
        lineY += 11;
        guiGraphics.drawString(font, DONATE_LINE_8, x + 10, lineY, TEXT_WHITE, true);
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
        guiGraphics.drawString(this.font, DREAMINGFISH_TITLE, 5, 5, TEXT_WHITE, false);

        // 左下角 - 版权按钮（可点击）
        int copyrightX = 5;
        int copyrightY = virtualH - 10;
        int copyrightWidth = this.font.width(COPYRIGHT_TEXT);
        int copyrightHeight = 10;

        // 检测版权按钮悬停（需要转换坐标）
        // 检测版权按钮悬停
        boolean copyrightHovered = economySystem$isCopyrightHovered();

        // 保存版权按钮区域供点击检测使用
        economySystem$copyrightX = copyrightX;
        economySystem$copyrightY = copyrightY;
        economySystem$copyrightWidth = copyrightWidth;
        economySystem$copyrightHeight = copyrightHeight;

        // 渲染版权文本（悬停时白色，否则灰色）
        int copyrightColor = copyrightHovered ? TEXT_WHITE : 0xFFAAAAAA;
        guiGraphics.drawString(this.font, COPYRIGHT_TEXT, copyrightX, copyrightY, copyrightColor, false);

        // 悬停时添加下划线
        if (copyrightHovered) {
            guiGraphics.fill(copyrightX, copyrightY + copyrightHeight, copyrightX + copyrightWidth, copyrightY + copyrightHeight + 1, 0xFFFFFFFF);
        }

        // 左上角 - Minecraft 1.20.1（在版权上方）
        guiGraphics.drawString(this.font, MINECRAFT_VERSION, 5, virtualH - 22, TEXT_GRAY, false);

        // 右下角 - Mod、语言和退出按钮
        guiGraphics.drawString(this.font, BUTTON_MODS, virtualW - 155, virtualH - 10, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, BUTTON_LANGUAGE, virtualW - 95, virtualH - 10, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, BUTTON_EXIT, virtualW - 35, virtualH - 10, 0xFF666666, false);

        // 版权声明（右上角，右对齐）
        guiGraphics.drawString(this.font, "§6" + DREAMINGFISH_COPYRIGHT, virtualW - this.font.width(DREAMINGFISH_COPYRIGHT) - 5, 5, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, "§6" + DEVELOPER_COPYRIGHT, virtualW - this.font.width(DEVELOPER_COPYRIGHT) - 5, 17, TEXT_GRAY, false);

        poseStack.popPose();
    }

    @Unique
    private void economySystem$renderCornerText(GuiGraphics guiGraphics) {
        // 使用虚拟坐标（640x360），相对于虚拟坐标系统边缘
        int virtualW = economySystem$virtualSize.virtualWidth;
        int virtualH = economySystem$virtualSize.virtualHeight;

        // 左下角 - Minecraft 1.20.1 + Mojang 版权
        guiGraphics.drawString(this.font, MINECRAFT_VERSION, 5, virtualH - 10, TEXT_GRAY, false);

        // 右下角 - Mod、语言和退出按钮
        guiGraphics.drawString(this.font, BUTTON_MODS, virtualW - 155, virtualH - 10, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, BUTTON_LANGUAGE, virtualW - 95, virtualH - 10, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, BUTTON_EXIT, virtualW - 35, virtualH - 10, 0xFF666666, false);

        // 版权声明（右上角，右对齐）
        guiGraphics.drawString(this.font, "§6" + DREAMINGFISH_COPYRIGHT, virtualW - this.font.width(DREAMINGFISH_COPYRIGHT) - 5, 5, TEXT_GRAY, false);
        guiGraphics.drawString(this.font, "§6" + DEVELOPER_COPYRIGHT, virtualW - this.font.width(DEVELOPER_COPYRIGHT) - 5, 17, TEXT_GRAY, false);
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

        // 版权按钮
        if (vmxNoOffset >= economySystem$copyrightX && vmxNoOffset <= economySystem$copyrightX + economySystem$copyrightWidth
                && vmyNoOffset >= economySystem$copyrightY && vmyNoOffset <= economySystem$copyrightY + economySystem$copyrightHeight) {
            economySystem$openCopyright(mc);
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
    private void economySystem$startUpdateLogFetch() {
        if (economySystem$updateLogFetchStarted) {
            return;
        }
        economySystem$updateLogFetchStarted = true;
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(UPDATE_LOG_API_URL).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Minecraft-Mod");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != 200) {
                    return;
                }

                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    String name = json.has("name") ? json.get("name").getAsString() : "";
                    String tag = json.has("tag_name") ? json.get("tag_name").getAsString() : "";
                    String latest = !name.isBlank() ? name : tag;
                    if (!latest.isBlank()) {
                        economySystem$updateLogPreview = "§a" + latest;
                    }
                }
            } catch (Exception ignored) {
                // Ignore network/parse errors to avoid blocking the title screen.
            }
        }, "economySystem-update-log-fetch").start();
    }

    @Unique
    private void economySystem$openUpdateLog(Minecraft mc) {
        try {
            Util.getPlatform().openUri(new URI(UPDATE_LOG_URL));
        } catch (URISyntaxException e) {
            // Ignore malformed URL to avoid crashing the title screen.
        }
    }

    @Unique
    private boolean economySystem$isCopyrightHovered() {
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        // 转换鼠标坐标到虚拟坐标（只有缩放，无偏移）
        int vmx = (int) (mouseX / economySystem$scale);
        int vmy = (int) (mouseY / economySystem$scale);

        return vmx >= economySystem$copyrightX && vmx <= economySystem$copyrightX + economySystem$copyrightWidth
                && vmy >= economySystem$copyrightY && vmy <= economySystem$copyrightY + economySystem$copyrightHeight;
    }

    @Unique
    private void economySystem$openCopyright(Minecraft mc) {
        TitleScreen self = (TitleScreen) (Object) this;
        mc.setScreen(new CreditsAndAttributionScreen(self));
    }

    @Unique
    private float economySystem$easeOutCubic(float t) {
        // Ease out cubic: 1 - (1-t)^3 - 更柔和的缓动
        return 1.0F - (float) Math.pow(1.0F - t, EASE_POWER);
    }

    @Unique
    private float economySystem$easeSmooth(float t) {
        // Smooth step: 3t^2 - 2t^3 - 最柔和的缓动
        return t * t * (3.0F - 2.0F * t);
    }
}
