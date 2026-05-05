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

    @Unique
    private static volatile String economySystem$updateLogPreview = "§7暂无更新";
    @Unique
    private static volatile boolean economySystem$updateLogFetchStarted = false;

    private static final String UPDATE_LOG_URL = "https://github.com/QingMo-A/EconomySystem/releases";
    private static final String UPDATE_LOG_API_URL = "https://api.github.com/repos/QingMo-A/EconomySystem/releases/latest";

    private static final String COPYRIGHT_TEXT = "Copyright Mojang AB. Do not distribute!";
    private static final String DREAMINGFISH_COPYRIGHT = "© 2026 DreamingFish - EconomySystem";
    private static final String DEVELOPER_COPYRIGHT = "  Developed by QINGMO & HANHANYU";
    private static final String DEVELOPER_INFO = "§8开发者：QINGMO、HANHANYU";

    // 资助面板文案
    private static final String DONATE_WELCOME = "§e§l欢§6§l迎§a§l来§b§l到 §d§l守§9§l望§c§l梦§6§l屿 §8— §7梦鱼服";
    private static final String DONATE_TITLE = "§7本服为§e非营利公益服§7，";
    private static final String DONATE_LINE_1 = "§e公益服维持不易，感谢所有资助者§7。";
    private static final String DONATE_LINE_2 = "§7无偿资助§c无法获得§7游戏内权益和物资，";
    private static final String DONATE_LINE_3 = "§7请您资助前三思。";
    private static final String DONATE_LINE_4 = "§7资助者可自定义设计武器/装备/物品等，";
    private static final String DONATE_LINE_5 = "§7且可以自定义属性、外观（数值保证合理）。";
    private static final String DONATE_LINE_6 = "§7开发完成后可以让所有人§a获取§7。";

    // 颜色定义
    private static final int ACCENT_BLUE = 0xFF0088FF;
    private static final int ACCENT_GREEN = 0xFF44FF88;
    private static final int ACCENT_GOLD = 0xFFFFAA44;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GRAY = 0xFFAAAAAA;

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

        // 右上角区域，放在语言/模组/更新日志按钮下方
        int startX = this.width - 16;
        int startY = 28;
        int buttonGap = 4;
        int maxButtonsPerRow = 3;

        for (int i = 0; i < modButtons.size(); i++) {
            AbstractWidget btn = modButtons.get(i);
            int row = i / maxButtonsPerRow;
            int col = i % maxButtonsPerRow;

            int btnX = startX - (col + 1) * (btn.getWidth() + buttonGap);
            int btnY = startY + row * (btn.getHeight() + buttonGap);

            btn.setX(btnX);
            btn.setY(btnY);
        }
    }

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
    private void economySystem$renderBackground(GuiGraphics guiGraphics, float fadeAlpha) {
        UiBackgroundRenderer.renderCyclingBackgroundCrossfade(guiGraphics, this.width, this.height, fadeAlpha);
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
        net.neoforged.neoforge.client.ClientHooks.renderMainMenu(
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

        // 屏幕空间虚拟坐标（用于右下角/右上角按钮定位，无居中偏移）
        int vmxScreen = (int) (mouseX / scale);
        int vmyScreen = (int) (mouseY / scale);

        // ===== 按钮悬停检测（使用屏幕空间坐标） =====
        int newHoveredIndex = economySystem$detectButtonHover(vmxScreen, vmyScreen, virtualW, virtualH);
        if (newHoveredIndex != this.economySystem$hoveredButtonIndex) {
            this.economySystem$hoveredButtonIndex = newHoveredIndex;
            this.economySystem$hoverTime = time;
        }

        // 计算动画进度
        long elapsed = time - economySystem$openTime;
        float animationProgress = Math.min((float) elapsed / ANIMATION_DURATION, 1.0F);
        float easedProgress = economySystem$easeOutCubic(animationProgress);

        // 渲染右下角+右上角按钮（屏幕空间，无居中偏移）
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(scale, scale, 1.0f);
        economySystem$renderBottomRightButtons(guiGraphics, virtualW, virtualH, easedProgress);
        poseStack.popPose();

        // 渲染左下角版权（屏幕空间，无居中偏移）
        economySystem$renderFooter(guiGraphics, scale);

        economySystem$scale = scale;
    }

    // ==================== 右下角主按钮 + 右上角辅助按钮 ====================

    // 右下角主按钮（从上到下排列）
    @Unique private static final String[] MAIN_BTN_TEXTS = {"§l多人游戏", "单人游戏", "设置", "退出游戏"};
    @Unique private static final String[] MAIN_BTN_DESCS = {
        "§a多人游戏 §7— 选择服务器，加入§e梦屿§7与其他玩家共同冒险",
        "§6单人游戏 §7— 选择或创建你的单人世界",
        "§9设置 §7— 调整游戏选项，自定义你的体验",
        "§c退出游戏 §7— 离开梦屿，返回现实世界"
    };
    @Unique private static final int[] MAIN_BTN_COLORS = {ACCENT_GREEN, ACCENT_GOLD, ACCENT_BLUE, 0xFFCC6666};
    @Unique private static final int MAIN_MULTIPLAYER = 0;
    @Unique private static final int MAIN_SINGLEPLAYER = 1;
    @Unique private static final int MAIN_SETTINGS = 2;
    @Unique private static final int MAIN_EXIT = 3;

    // 右上角辅助按钮（水平排列，纯文字+图标）
    @Unique private static final String[] AUX_BTN_TEXTS = {"语言[🌐]", "模组[📦]", "更新日志"};
    @Unique private static final String[] AUX_BTN_DESCS = {
        "§e语言 §7— 切换游戏语言 / Language",
        "§e模组 §7— 查看和管理已安装的模组",
        "§e更新日志 §7— 查看 EconomySystem 最新更新内容"
    };
    @Unique private static final int AUX_LANGUAGE = 0;
    @Unique private static final int AUX_MODS = 1;
    @Unique private static final int AUX_UPDATE_LOG = 2;

    // hover 索引编码：bits 0-3=主按钮索引, bits 4-7=辅助按钮索引, bit 8=区分(0=主,1=辅助)
    @Unique
    private int economySystem$detectButtonHover(int vmx, int vmy, int virtualW, int virtualH) {
        // 检测右下角主按钮
        int mainBtnW = 110;
        int mainBtnH = 22;
        int mainRight = virtualW - 20;
        int mainBtnY = virtualH - 6 - mainBtnH;
        for (int i = MAIN_BTN_TEXTS.length - 1; i >= 0; i--) {
            int mainX = mainRight - mainBtnW;
            if (vmx >= mainX && vmx <= mainRight && vmy >= mainBtnY && vmy <= mainBtnY + mainBtnH) {
                return i;
            }
            mainBtnY -= mainBtnH + 6;
        }

        // 检测右上角辅助按钮
        int auxX = virtualW - 16;
        for (int i = AUX_BTN_TEXTS.length - 1; i >= 0; i--) {
            int textW = this.font.width(AUX_BTN_TEXTS[i]);
            int auxStartX = auxX - textW;
            if (vmx >= auxStartX - 6 && vmx <= auxX + 6 && vmy >= 5 && vmy <= 17) {
                return 0x80 | i;
            }
            auxX = auxStartX - 14;
        }
        return -1;
    }

    @Unique
    private void economySystem$renderBottomRightButtons(GuiGraphics guiGraphics, int virtualW, int virtualH, float animProgress) {
        int hovered = economySystem$hoveredButtonIndex;
        boolean isAux = hovered >= 0x80;
        int auxHover = isAux ? hovered & 0x7F : -1;
        int mainHover = isAux ? -1 : hovered;

        // ===== 右上角辅助按钮（简约圆点+文字） =====
        int auxX = virtualW - 16;
        for (int i = AUX_BTN_TEXTS.length - 1; i >= 0; i--) {
            String label = AUX_BTN_TEXTS[i];
            int textW = this.font.width(label);
            int ax = auxX - textW;
            boolean hov = auxHover == i;

            // hover 时添加半透明药丸背景
            if (hov) {
                int pad = 4;
                guiGraphics.fill(ax - pad, 3, ax + textW + pad, 18, 0x44000000);
                guiGraphics.fill(ax - pad, 3, ax + textW + pad, 4, 0x22FFFFFF);
                guiGraphics.fill(ax - pad, 17, ax + textW + pad, 18, 0x22FFFFFF);
            }

            int color = hov ? TEXT_WHITE : 0xFFAAAAAA;
            guiGraphics.drawString(this.font, label, ax, 5, color, false);

            // hover 下划线
            if (hov) {
                guiGraphics.fill(ax + 1, 16, ax + textW - 1, 18, ACCENT_BLUE);
                // tooltip below
                String desc = AUX_BTN_DESCS[i];
                int dw = this.font.width(desc) + 12;
                int tipX = ax + textW / 2 - dw / 2;
                guiGraphics.fill(tipX, 22, tipX + dw, 36, 0xEE151515);
                guiGraphics.fill(tipX, 22, tipX + dw, 23, 0x33FFFFFF);
                guiGraphics.drawString(this.font, "§7" + desc, tipX + 6, 25, TEXT_WHITE, false);
            }
            auxX = ax - 16;
        }

        // ===== 右下角主按钮（深色圆角药丸 + 微妙样式） =====
        int mainBtnW = 110;
        int mainBtnH = 22;
        int mainRadius = 4;
        int mainRight = virtualW - 20;
        int mainBtnY = virtualH - 6 - mainBtnH;

        for (int i = MAIN_BTN_TEXTS.length - 1; i >= 0; i--) {
            float stagger = (MAIN_BTN_TEXTS.length - 1 - i) * 0.08F;
            float progress = economySystem$getStaggeredProgress(animProgress, stagger);
            int slideOff = (int) ((1.0F - progress) * 12);
            int mainX = mainRight - mainBtnW;

            boolean hov = mainHover == i;
            int bgAlpha = hov ? 0xBB : 0x77;
            // 用强调色给背景上微弱的色相
            int accent = MAIN_BTN_COLORS[i];
            int tintR = (((accent >> 16) & 0xFF) * 18) / 255;
            int tintG = (((accent >> 8) & 0xFF) * 18) / 255;
            int tintB = ((accent & 0xFF) * 18) / 255;
            int bgColor = (bgAlpha << 24) | (tintR << 16) | (tintG << 8) | tintB;
            int borderAlpha = hov ? 0x60 : 0x28;

            int bx = mainX + slideOff;
            int by = mainBtnY;
            int bw = mainBtnW;
            int bh = mainBtnH;

            // 圆角背景
            economySystem$fillRounded(guiGraphics, bx, by, bw, bh, mainRadius, bgColor);

            // 细边框（带一点颜色）
            int bc = (borderAlpha << 24) | (tintR * 3 << 16) | (tintG * 3 << 8) | (tintB * 3);
            economySystem$drawRoundedBorder(guiGraphics, bx, by, bw, bh, mainRadius, bc);

            // hover 效果：左侧强调条 + 顶部微光
            if (hov) {
                guiGraphics.fill(bx + 1, by + 2, bx + 4, by + bh - 2, accent);
                guiGraphics.fill(bx + 4, by + 1, bx + bw - 1, by + 2, 0x30FFFFFF);
            }

            // 文字（垂直居中）
            int textW = this.font.width(MAIN_BTN_TEXTS[i]);
            int textX = bx + (bw - textW) / 2;
            int textY = by + (bh - 9) / 2;
            guiGraphics.drawString(this.font, MAIN_BTN_TEXTS[i], textX, textY, TEXT_WHITE, false);

            // hover tooltip
            if (hov && progress >= 0.95F) {
                String desc = MAIN_BTN_DESCS[i];
                int dw = this.font.width(desc) + 12;
                int tipX = bx - dw - 8;
                int tipY = by + (bh - 16) / 2;
                guiGraphics.fill(tipX, tipY, tipX + dw, tipY + 16, 0xEE151515);
                guiGraphics.fill(tipX, tipY, tipX + dw, tipY + 1, 0x33FFFFFF);
                guiGraphics.drawString(this.font, "§7" + desc, tipX + 6, tipY + 4, TEXT_WHITE, false);
            }

            mainBtnY -= mainBtnH + 6;
        }
    }

    // ===== 圆角绘制辅助 =====
    @Unique
    private static void economySystem$fillRounded(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if ((color >>> 24) == 0) return;
        int rr = Math.min(r, Math.min(w / 2, h / 2));
        int right = x + w;
        int bottom = y + h;
        g.fill(x + rr, y, right - rr, bottom, color);
        g.fill(x, y + rr, right, bottom - rr, color);
        if (rr >= 2) {
            g.fill(x + 1, y + 1, x + rr, y + rr, color);
            g.fill(right - rr, y + 1, right - 1, y + rr, color);
            g.fill(x + 1, bottom - rr, x + rr, bottom - 1, color);
            g.fill(right - rr, bottom - rr, right - 1, bottom - 1, color);
        }
        if (rr >= 3) {
            g.fill(x + 1, y + 2, x + 2, bottom - 2, color);
            g.fill(right - 2, y + 2, right - 1, bottom - 2, color);
            g.fill(x + 2, y + 1, right - 2, y + 2, color);
            g.fill(x + 2, bottom - 2, right - 2, bottom - 1, color);
        }
    }

    @Unique
    private static void economySystem$drawRoundedBorder(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if ((color >>> 24) == 0) return;
        int rr = Math.min(r, Math.min(w / 2, h / 2));
        int right = x + w;
        int bottom = y + h;
        g.fill(x + rr, y, right - rr, y + 1, color);
        g.fill(x + rr, bottom - 1, right - rr, bottom, color);
        g.fill(x, y + rr, x + 1, bottom - rr, color);
        g.fill(right - 1, y + rr, right, bottom - rr, color);
        if (rr >= 2) {
            g.fill(x + 1, y + 1, x + 2, y + 2, color);
            g.fill(right - 2, y + 1, right - 1, y + 2, color);
            g.fill(x + 1, bottom - 2, x + 2, bottom - 1, color);
            g.fill(right - 2, bottom - 2, right - 1, bottom - 1, color);
        }
        if (rr >= 3) {
            g.fill(x + 1, y + 2, x + 2, y + 3, color);
            g.fill(x + 2, y + 1, x + 3, y + 2, color);
            g.fill(right - 2, y + 2, right - 1, y + 3, color);
            g.fill(right - 3, y + 1, right - 2, y + 2, color);
            g.fill(x + 1, bottom - 3, x + 2, bottom - 2, color);
            g.fill(x + 2, bottom - 2, x + 3, bottom - 1, color);
            g.fill(right - 2, bottom - 3, right - 1, bottom - 2, color);
            g.fill(right - 3, bottom - 2, right - 2, bottom - 1, color);
        }
    }

    @Unique
    private float economySystem$scale = 1.0f;


    @Unique
    private float economySystem$getStaggeredProgress(float animProgress, float delay) {
        // 获取错开的动画进度（delay 0-1 之间的值）
        float adjusted = Math.max((animProgress - delay) / (1.0F - delay), 0);
        adjusted = Math.min(adjusted, 1.0F);
        // 使用 smooth step 让动画更柔和
        return economySystem$easeSmooth(adjusted);
    }

    @Unique
    private void economySystem$renderFooter(GuiGraphics guiGraphics, float scale) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(scale, scale, 1.0f);

        int virtualW = economySystem$virtualSize.virtualWidth;
        int virtualH = economySystem$virtualSize.virtualHeight;

        // ===== 左上角 DreamingFish 品牌标识 =====
        guiGraphics.drawString(this.font, "§bDreaming§dFish  §7v1.0", 8, 6, TEXT_WHITE, true);

        // ===== 左上角资助说明（品牌标识下方） =====
        int donateX = 6;
        int donateY = 20;
        int donateW = 205;
        int lineH = 12;
        int lineCount = 8;
        int donateH = lineCount * lineH;
        int pad = 5;

        guiGraphics.fill(donateX, donateY, donateX + donateW, donateY + donateH + pad * 2, 0x88000000);
        guiGraphics.fill(donateX, donateY, donateX + donateW, donateY + 1, 0x33FFFFFF);

        int ty = donateY + pad;
        guiGraphics.drawString(this.font, DONATE_WELCOME,  donateX + 3, ty, TEXT_WHITE, false); ty += lineH;
        guiGraphics.drawString(this.font, DONATE_TITLE,    donateX + 3, ty, TEXT_GRAY, true);  ty += lineH;
        guiGraphics.drawString(this.font, DONATE_LINE_1,   donateX + 3, ty, TEXT_GRAY, true);  ty += lineH;
        guiGraphics.drawString(this.font, DONATE_LINE_2,   donateX + 3, ty, TEXT_GRAY, true);  ty += lineH;
        guiGraphics.drawString(this.font, DONATE_LINE_3,   donateX + 3, ty, TEXT_GRAY, true);  ty += lineH;
        guiGraphics.drawString(this.font, DONATE_LINE_4,   donateX + 3, ty, TEXT_GRAY, true);  ty += lineH;
        guiGraphics.drawString(this.font, DONATE_LINE_5,   donateX + 3, ty, TEXT_GRAY, true);  ty += lineH;
        guiGraphics.drawString(this.font, DONATE_LINE_6,   donateX + 3, ty, TEXT_GRAY, true);

        // ===== 左下角版权标识（两行） =====
        String copyrightLine = "§7" + DREAMINGFISH_COPYRIGHT + "  " + DEVELOPER_COPYRIGHT.trim();
        int copyrightLineY = virtualH - 22;
        guiGraphics.drawString(this.font, copyrightLine, 6, copyrightLineY, TEXT_GRAY, false);

        int mojangW = this.font.width(COPYRIGHT_TEXT);
        int myVirtual = virtualH - 10;

        boolean copyrightHovered = economySystem$isCopyrightHovered();
        economySystem$copyrightX = 6;
        economySystem$copyrightY = myVirtual;
        economySystem$copyrightWidth = mojangW;
        economySystem$copyrightHeight = 10;

        int mojangColor = copyrightHovered ? TEXT_WHITE : 0xFF777777;
        guiGraphics.drawString(this.font, COPYRIGHT_TEXT, 6, myVirtual, mojangColor, false);
        if (copyrightHovered) {
            guiGraphics.fill(6, myVirtual + 10, 6 + mojangW, myVirtual + 11, 0xFFFFFFFF);
        }

        poseStack.popPose();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void economySystem$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;

        Minecraft mc = Minecraft.getInstance();

        // 右下角按钮坐标（与 renderBottomRightButtons 对应，使用 offset 坐标系）
        int vmx = (int) (mouseX / economySystem$scale);
        int vmy = (int) (mouseY / economySystem$scale);

        int virtualW = economySystem$virtualSize.virtualWidth;
        int virtualH = economySystem$virtualSize.virtualHeight;

        // 检测右下角主按钮
        int mainBtnW = 110;
        int mainBtnH = 22;
        int mainRight = virtualW - 20;
        int mainBtnY = virtualH - 6 - mainBtnH;
        for (int i = MAIN_BTN_TEXTS.length - 1; i >= 0; i--) {
            int mainX = mainRight - mainBtnW;
            if (vmx >= mainX && vmx <= mainRight && vmy >= mainBtnY && vmy <= mainBtnY + mainBtnH) {
                switch (i) {
                    case MAIN_MULTIPLAYER -> economySystem$openMultiplayer(mc);
                    case MAIN_SINGLEPLAYER -> economySystem$openSingleplayer(mc);
                    case MAIN_SETTINGS -> economySystem$openSettings(mc);
                    case MAIN_EXIT -> mc.stop();
                }
                cir.setReturnValue(true);
                return;
            }
            mainBtnY -= mainBtnH + 6;
        }

        // 检测右上角辅助按钮
        int auxX = virtualW - 16;
        for (int i = AUX_BTN_TEXTS.length - 1; i >= 0; i--) {
            int textW = this.font.width(AUX_BTN_TEXTS[i]);
            int auxStartX = auxX - textW;
            if (vmx >= auxStartX - 6 && vmx <= auxX + 6 && vmy >= 5 && vmy <= 17) {
                switch (i) {
                    case AUX_MODS -> {
                        TitleScreen self = (TitleScreen) (Object) this;
                        mc.setScreen(new net.neoforged.neoforge.client.gui.ModListScreen(self));
                    }
                    case AUX_LANGUAGE -> {
                        TitleScreen self = (TitleScreen) (Object) this;
                        mc.setScreen(new net.minecraft.client.gui.screens.options.LanguageSelectScreen(self, mc.options, mc.getLanguageManager()));
                    }
                    case AUX_UPDATE_LOG -> economySystem$openUpdateLog(mc);
                }
                cir.setReturnValue(true);
                return;
            }
            auxX = auxStartX - 14;
        }

        // 版权按钮（无 offset，仅缩放的虚拟坐标）
        int vmyNoOffset = (int) (mouseY / economySystem$scale);
        int vmxNoOffset = (int) (mouseX / economySystem$scale);
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
        mc.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(self, mc.options));
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
