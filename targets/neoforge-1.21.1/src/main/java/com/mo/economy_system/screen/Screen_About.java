package com.mo.economy_system.screen;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.client.util.UiAnimation;
import com.mo.economy_system.screen.components.CardRenderer;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;
import com.mo.economy_system.target.neoforge1211.client.NeoForge1211HomeScreen;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class Screen_About extends Screen {

    private static final String AUTHOR_NAME = "QingMo HanHanYu";
    private static final String GITHUB_URL = "https://github.com/QingMo-A/EconoeySystem";

    private static final ResourceLocation VX_TEXTURE = ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "textures/gui/vx.png");
    private static final ResourceLocation ZFB_TEXTURE = ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "textures/gui/zfb.png");

    private static final int BASE_WIDTH = 640;
    private static final int BASE_HEIGHT = 360;
    private static final int PANEL_PADDING = 12;
    private static final int INFO_PANEL_WIDTH = 420;
    private static final int INFO_PANEL_HEIGHT = 170;
    private static final int QR_CARD_SIZE = 110;
    private static final int QR_IMAGE_PADDING = 6;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_WIDTH = 110;
    private static final int PANEL_ANIMATION_OFFSET = 50;
    private static final long ANIMATION_DURATION = 500;

    private float uiScale;
    private int virtualWidth;
    private int virtualHeight;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int githubX1, githubY1, githubX2, githubY2;
    private int backBtnX1, backBtnY1, backBtnX2, backBtnY2;
    private int lastLeftOffsetX;
    private int lastLeftOffsetY;

    private final UiButtonStyle backStyle;
    private final UiAnimation openAnimation = new UiAnimation(ANIMATION_DURATION, UiAnimation.Easing.EASE_OUT_CUBIC);
    private boolean skipAnimation = false;

    public Screen_About() {
        super(Component.translatable(Util_MessageKeys.ABOUT_TITLE_KEY));
        backStyle = createButtonStyle(CardRenderer.THEME_ABOUT);
    }

    @Override
    protected void init() {
        super.init();
        if (skipAnimation) {
            openAnimation.finish();
        } else {
            openAnimation.start();
        }
        calculateVirtualSize();
        updateLayout();
    }

    private void calculateVirtualSize() {
        float scaleX = (float) this.width / BASE_WIDTH;
        float scaleY = (float) this.height / BASE_HEIGHT;
        uiScale = Math.min(scaleX, scaleY);
        virtualWidth = (int) (this.width / uiScale);
        virtualHeight = (int) (this.height / uiScale);
    }

    private void updateLayout() {
        panelWidth = Math.min(INFO_PANEL_WIDTH, virtualWidth - PANEL_PADDING * 2);
        int availableHeight = virtualHeight - PANEL_PADDING * 2;
        panelHeight = Math.min(INFO_PANEL_HEIGHT, Math.max(120, availableHeight - 120));
        panelX = (virtualWidth - panelWidth) / 2;
        panelY = PANEL_PADDING + 16;

        int buttonWidth = Math.min(BUTTON_WIDTH, Math.max(90, panelWidth - PANEL_PADDING * 2));
        backBtnX1 = panelX + (panelWidth - buttonWidth) / 2;
        backBtnY1 = panelY + panelHeight - 34;
        backBtnX2 = backBtnX1 + buttonWidth;
        backBtnY2 = backBtnY1 + BUTTON_HEIGHT;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderFullScreenBackground(guiGraphics);

        calculateVirtualSize();
        updateLayout();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(uiScale, uiScale, 1.0f);

        float virtualMouseX = mouseX / uiScale;
        float virtualMouseY = mouseY / uiScale;
        float animProgress = openAnimation.value();
        int titleOffsetX = (int) ((1.0f - animProgress) * -PANEL_ANIMATION_OFFSET);
        int panelOffsetY = (int) ((1.0f - animProgress) * -PANEL_ANIMATION_OFFSET);
        int escOffsetX = (int) ((1.0f - animProgress) * PANEL_ANIMATION_OFFSET);
        int leftQrOffsetX = (int) ((1.0f - animProgress) * -PANEL_ANIMATION_OFFSET);
        int rightQrOffsetX = (int) ((1.0f - animProgress) * PANEL_ANIMATION_OFFSET);
        lastLeftOffsetX = 0;
        lastLeftOffsetY = panelOffsetY;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(titleOffsetX, 0, 0);
        drawTitle(guiGraphics);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, panelOffsetY, 0);
        renderInfoPanel(guiGraphics, virtualMouseX, virtualMouseY - panelOffsetY);
        guiGraphics.pose().popPose();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(escOffsetX, 0, 0);
        drawEscHint(guiGraphics);
        guiGraphics.pose().popPose();

        renderQrCards(guiGraphics, leftQrOffsetX, rightQrOffsetX);

        guiGraphics.pose().popPose();

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderFullScreenBackground(GuiGraphics guiGraphics) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB0000000);
    }

    private void drawTitle(GuiGraphics guiGraphics) {
        int y = PANEL_PADDING + font.lineHeight + 10;
        CardRenderer.drawVersionInfo(guiGraphics, font, PANEL_PADDING, y, 200, CardRenderer.UiIcon.ABOUT, "关于");
    }

    private void drawEscHint(GuiGraphics guiGraphics) {
        String hint = "按 ESC 返回";
        int hintWidth = font.width(hint);
        int x = virtualWidth - PANEL_PADDING - hintWidth;
        int y = PANEL_PADDING + 6;
        guiGraphics.drawString(font, hint, x, y, 0x90FFFFFF);
    }

    private void renderInfoPanel(GuiGraphics guiGraphics, float mouseX, float mouseY) {
        CardRenderer.drawCard(guiGraphics, panelX, panelY, panelWidth, panelHeight, CardRenderer.THEME_ABOUT, false);

        int textX = panelX + PANEL_PADDING;
        int textY = panelY + 8;

        String titleText = Component.translatable(Util_MessageKeys.ABOUT_TITLE_KEY).getString();
        guiGraphics.drawString(font, titleText, textX, textY, CardRenderer.TEXT_TITLE);
        textY += font.lineHeight + 4;

        String modName = Component.translatable(Util_MessageKeys.ABOUT_MOD_NAME_KEY).getString();
        guiGraphics.drawString(font, modName, textX, textY, CardRenderer.TEXT_DESC);
        textY += font.lineHeight + 2;

        String author = Component.translatable(Util_MessageKeys.ABOUT_AUTHOR_NAME_KEY, AUTHOR_NAME).getString();
        guiGraphics.drawString(font, author, textX, textY, CardRenderer.TEXT_DESC);
        textY += font.lineHeight + 8;

        String githubLine = Component.translatable(Util_MessageKeys.ABOUT_GITHUB_URL_KEY, GITHUB_URL).getString();
        int githubWidth = font.width(githubLine);
        int githubColor = (mouseX >= textX && mouseX <= textX + githubWidth &&
                           mouseY >= textY && mouseY <= textY + font.lineHeight) ? 0xFF6AB8FF : CardRenderer.THEME_MARKET;
        guiGraphics.drawString(font, githubLine, textX, textY, githubColor);

        githubX1 = textX;
        githubY1 = textY;
        githubX2 = textX + githubWidth;
        githubY2 = textY + font.lineHeight;

        textY += font.lineHeight + 2;
        String hint = Component.translatable(Util_MessageKeys.ABOUT_TEXT_SHOW_KEY).getString();
        guiGraphics.drawString(font, hint, textX, textY, 0x80FFFFFF);

        boolean backHovered = mouseX >= backBtnX1 && mouseX <= backBtnX2 &&
                              mouseY >= backBtnY1 && mouseY <= backBtnY2;
        drawStripedButton(guiGraphics, backBtnX1, backBtnY1, backBtnX2 - backBtnX1, BUTTON_HEIGHT,
            Component.translatable(Util_MessageKeys.ABOUT_BACK_BUTTON_KEY).getString(), backStyle, backHovered);
    }

    private void renderQrCards(GuiGraphics guiGraphics, int leftOffsetX, int rightOffsetX) {
        int qrY = virtualHeight - PANEL_PADDING - QR_CARD_SIZE;
        int leftX = PANEL_PADDING;
        int rightX = virtualWidth - PANEL_PADDING - QR_CARD_SIZE;

        CardRenderer.drawCard(guiGraphics, leftX + leftOffsetX, qrY, QR_CARD_SIZE, QR_CARD_SIZE, CardRenderer.THEME_ABOUT, false);
        CardRenderer.drawCard(guiGraphics, rightX + rightOffsetX, qrY, QR_CARD_SIZE, QR_CARD_SIZE, CardRenderer.THEME_ABOUT, false);

        int imgSize = QR_CARD_SIZE - QR_IMAGE_PADDING * 2;
        guiGraphics.blit(VX_TEXTURE, leftX + leftOffsetX + QR_IMAGE_PADDING, qrY + QR_IMAGE_PADDING,
            imgSize, imgSize, 0, 0, 256, 256, 256, 256);
        guiGraphics.blit(ZFB_TEXTURE, rightX + rightOffsetX + QR_IMAGE_PADDING, qrY + QR_IMAGE_PADDING,
            imgSize, imgSize, 0, 0, 256, 256, 256, 256);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float virtualMouseX = (float) mouseX / uiScale;
        float virtualMouseY = (float) mouseY / uiScale;
        float leftMouseX = virtualMouseX - lastLeftOffsetX;
        float leftMouseY = virtualMouseY - lastLeftOffsetY;

        if (leftMouseX >= githubX1 && leftMouseX <= githubX2 &&
            leftMouseY >= githubY1 && leftMouseY <= githubY2) {
            Minecraft.getInstance().keyboardHandler.setClipboard(GITHUB_URL);
            Minecraft.getInstance().getChatListener().handleSystemMessage(
                Component.translatable(Util_MessageKeys.ABOUT_COPY_URL).withStyle(style -> style.withColor(0x00FF00)),
                false
            );
            return true;
        }

        if (leftMouseX >= backBtnX1 && leftMouseX <= backBtnX2 &&
            leftMouseY >= backBtnY1 && leftMouseY <= backBtnY2) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new NeoForge1211HomeScreen());
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new NeoForge1211HomeScreen());
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawStripedButton(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                   String text, UiButtonStyle style, boolean hovered) {
        UiButtonRenderer.drawStripedButton(guiGraphics, this.font, x, y, width, height,
            text, "", style, hovered, UiButtonRenderer.TextAlign.CENTER, false);
    }

    private UiButtonStyle createButtonStyle(int accentColor) {
        return UiButtonStyle.accent(accentColor)
            .setPadding(8)
            .setStripeWidth(4)
            .setGlowHeight(6)
            .setBgAlpha(0x55)
            .setBgAlphaHover(0x70)
            .setBorderAlpha(0x25)
            .setBorderAlphaHover(0x40)
            .setTextShadow(false);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {}
}
