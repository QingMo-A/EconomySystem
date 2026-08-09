package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ClientPlayerListState;
import com.mo.economy_system.common.network.InvitePlayerMessage;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.territory.invite.TerritoryInviteController;
import com.mo.economy_system.ui.territory.invite.TerritoryInviteEvent;
import com.mo.economy_system.ui.territory.invite.TerritoryInviteLayout;
import com.mo.economy_system.ui.territory.invite.TerritoryInvitePort;
import com.mo.economy_system.ui.territory.invite.TerritoryInviteView;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge shell for the common territory invite directory. */
public final class NeoForge1211TerritoryInviteScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Screen parent;
  private final Port port = new Port();
  private final TerritoryInviteController controller;
  private EditBox search;
  private long appliedRevision;
  private long clientTick;

  public NeoForge1211TerritoryInviteScreen(Owned territory, Screen parent) {
    super(Component.translatable("screen.invite.title"));
    this.parent = parent;
    UUID viewer = Minecraft.getInstance().player == null ? territory.summary().ownerId()
        : Minecraft.getInstance().player.getUUID();
    Set<UUID> members = territory.authorizedMembers().stream().map(value -> value.playerId()).collect(java.util.stream.Collectors.toSet());
    appliedRevision = ClientPlayerListState.snapshot().revision();
    controller = new TerritoryInviteController(territory.summary().territoryId(), territory.summary().name(),
        territory.summary().ownerId(), viewer, members, port);
  }

  @Override protected void init() {
    var layout = commonLayout();
    float scale = layout.scale().value();
    var rect = layout.search();
    search = new EditBox(font, Math.round(rect.x() * scale), Math.round(rect.y() * scale),
        Math.max(1, Math.round(rect.width() * scale)), Math.max(1, Math.round(rect.height() * scale)),
        Component.translatable("screen.invite.search"));
    search.setMaxLength(64);
    search.setResponder(value -> controller.handle(new TerritoryInviteEvent.FilterChanged(value)));
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) controller.handle(new TerritoryInviteEvent.Initialize(System.nanoTime()));
  }
  @Override public void tick() {
    super.tick(); clientTick++; controller.handle(new TerritoryInviteEvent.Tick(System.nanoTime()));
    var snapshot = ClientPlayerListState.snapshot();
    if (snapshot.revision() != appliedRevision && port.requestId >= 0) {
      appliedRevision = snapshot.revision();
      controller.handle(new TerritoryInviteEvent.PlayersLoaded(port.requestId, snapshot.revision(), snapshot.players()));
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }
  private void navigate(UiNavigation navigation) { if (minecraft != null && navigation instanceof UiNavigation.Back) minecraft.setScreen(parent); }
  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    var layout = commonLayout(); UiScale scale = layout.scale(); graphics.pose().pushPose(); graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    TerritoryInviteView.render(new NeoForge1211UiRenderer(graphics, font), controller.state(), layout, scale.toVirtualX(mouseX), scale.toVirtualY(mouseY), clientTick);
    graphics.pose().popPose(); super.render(graphics, mouseX, mouseY, partialTick);
  }
  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    var layout = commonLayout(); int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (var row : layout.playerRows()) if (row.inviteButton().contains(x, y)) { controller.handle(new TerritoryInviteEvent.InviteClicked(row.player().playerId(), clientTick)); return true; }
    if (layout.previousButton().contains(x, y)) { controller.handle(new TerritoryInviteEvent.Scroll(-1)); return true; }
    if (layout.nextButton().contains(x, y)) { controller.handle(new TerritoryInviteEvent.Scroll(1)); return true; }
    if (controller.state().screenState() == ScreenState.ERROR && layout.retryButton().contains(x, y)) { controller.handle(new TerritoryInviteEvent.Retry(System.nanoTime())); return true; }
    if (layout.backButton().contains(x, y)) { controller.handle(new TerritoryInviteEvent.Back()); return true; }
    return super.mouseClicked(mouseX, mouseY, button);
  }
  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (scrollY != 0 && controller.state().totalPages() > 1) { controller.handle(new TerritoryInviteEvent.Scroll(scrollY < 0 ? 1 : -1)); return true; }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }
  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { if (keyCode == 256) { onClose(); return true; } return super.keyPressed(keyCode, scanCode, modifiers); }
  @Override public void onClose() { controller.handle(new TerritoryInviteEvent.Back()); controller.pollNavigation().ifPresent(this::navigate); }
  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
  private TerritoryInviteLayout.Layout commonLayout() { var layout = TerritoryInviteLayout.calculate(width, height, controller.state()); if (layout.pageSize() != controller.state().pageSize()) { controller.handle(new TerritoryInviteEvent.ViewportChanged(layout.pageSize())); layout = TerritoryInviteLayout.calculate(width, height, controller.state()); } return layout; }
  private final class Port implements TerritoryInvitePort {
    private long requestId = -1;
    @Override public long nextRequestId() { long id = IDS.getAndIncrement(); if (id < 0) throw new IllegalStateException("invite id exhausted"); return id; }
    @Override public void requestPlayers(long id) { requestId = id; EconomyServices.platform().network().sendToServer(ServerPlayerListRequestMessage.INSTANCE); }
    @Override public void submitInvite(UUID territoryId, UUID playerId) { EconomyServices.platform().network().sendToServer(new InvitePlayerMessage(territoryId, playerId)); }
  }
}
