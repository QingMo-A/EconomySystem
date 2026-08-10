package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.about.AboutAction;
import com.mo.economy_system.ui.about.AboutController;
import com.mo.economy_system.ui.about.AboutEvent;
import com.mo.economy_system.ui.about.AboutLayout;
import com.mo.economy_system.ui.about.AboutPort;
import com.mo.economy_system.ui.about.AboutView;
import com.mo.economy_system.ui.about.AboutOpenAnimation;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge Screen shell for the common About page. */
public final class NeoForge1211AboutScreen extends Screen {
  private final Screen parent;
  private final Port port = new Port();
  private final AboutController controller = new AboutController(port);
  private long animationStartedAtNanos = -1L;

  public NeoForge1211AboutScreen() { this(null); }
  public NeoForge1211AboutScreen(Screen parent) {
    super(Component.translatable(EconomyUiRoute.ABOUT.titleKey()));
    this.parent = parent;
  }

  @Override protected void init() {
    if (animationStartedAtNanos < 0L) animationStartedAtNanos = System.nanoTime();
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    NeoForge1211UiRenderer renderer = new NeoForge1211UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, AboutLayout.BACKGROUND_COLOR);
    AboutLayout.Layout layout = AboutLayout.calculate(width, height, renderer.metrics(), animationProgress());
    UiScale scale = layout.scale();
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    AboutView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    AboutLayout.Layout layout = AboutLayout.calculate(width, height,
        new NeoForge1211UiTextMetrics(font), animationProgress());
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    if (layout.github().contains(x, y)) {
      controller.handle(new AboutEvent.ActionClicked(AboutAction.COPY_GITHUB));
      return true;
    }
    if (layout.backButton().contains(x, y) || layout.esc().contains(x, y)) {
      controller.handle(new AboutEvent.ActionClicked(AboutAction.BACK));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() {
    if (minecraft == null) return;
    if (parent != null) minecraft.setScreen(parent); else minecraft.setScreen(new NeoForge1211HomeScreen());
  }
  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

  private float animationProgress() {
    return AboutOpenAnimation.easedProgressAt(animationStartedAtNanos, System.nanoTime());
  }

  private final class Port implements AboutPort {
    @Override public void copyToClipboard(String value) {
      Minecraft.getInstance().keyboardHandler.setClipboard(value);
    }
  }
}
