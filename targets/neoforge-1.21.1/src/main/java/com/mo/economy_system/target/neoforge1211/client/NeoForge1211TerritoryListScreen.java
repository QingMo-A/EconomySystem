package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.client.ClientTerritoryState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.TeleportToTerritoryMessage;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.territory.list.TerritoryListAction;
import com.mo.economy_system.ui.territory.list.TerritoryListController;
import com.mo.economy_system.ui.territory.list.TerritoryListEvent;
import com.mo.economy_system.ui.territory.list.TerritoryListLayout;
import com.mo.economy_system.ui.territory.list.TerritoryListPort;
import com.mo.economy_system.ui.territory.list.TerritoryListRow;
import com.mo.economy_system.ui.territory.list.TerritoryListView;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge shell for the common territory card grid. */
public final class NeoForge1211TerritoryListScreen extends Screen {
  private static final AtomicLong IDS = new AtomicLong(1);
  private final Screen parent;
  private final Port port = new Port();
  private final TerritoryListController controller = new TerritoryListController(port);
  private EditBox search;
  private long appliedResponse = -1;

  public NeoForge1211TerritoryListScreen() {
    this(null);
  }

  public NeoForge1211TerritoryListScreen(Screen parent) {
    super(Component.translatable("screen.territory.title"));
    this.parent = parent;
  }

  @Override protected void init() {
    String value = search == null ? "" : search.getValue();
    TerritoryListLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    search = new NeoForge1211UnderlinedEditBox(font, Math.round(layout.search().x() * scale.value()),
        Math.round(layout.search().y() * scale.value()),
        Math.max(1, Math.round(layout.search().width() * scale.value())),
        Math.max(1, Math.round(layout.search().height() * scale.value())),
        Component.translatable("screen.territory.list.search"));
    NeoForge1211UiInputAdapter.apply(search);
    search.setMaxLength(64);
    search.setValue(value);
    search.setResponder(text -> controller.handle(new TerritoryListEvent.FilterChanged(text)));
    addRenderableWidget(search);
    if (controller.state().screenState() == ScreenState.IDLE) {
      controller.handle(new TerritoryListEvent.Initialize(System.nanoTime()));
    }
  }

  @Override public void tick() {
    super.tick();
    controller.handle(new TerritoryListEvent.Tick(System.nanoTime()));
    ClientTerritoryState.Snapshot snapshot = ClientTerritoryState.snapshot();
    if (snapshot.requestId() == port.requestId && !snapshot.loading()
        && snapshot.requestId() != appliedResponse) {
      appliedResponse = snapshot.requestId();
      if (snapshot.error()) {
        controller.handle(new TerritoryListEvent.DataFailed(snapshot.requestId(),
            "screen.territory.sync_failed"));
      } else {
        controller.handle(new TerritoryListEvent.DataLoaded(snapshot.requestId(), snapshot.owned(),
            snapshot.authorized()));
      }
    }
    controller.pollNavigation().ifPresent(this::navigate);
  }

  private void navigate(UiNavigation navigation) {
    if (minecraft == null) return;
    if (navigation instanceof UiNavigation.Route route && route.route() == EconomyUiRoute.HOME) {
      minecraft.setScreen(new NeoForge1211HomeScreen());
    } else if (navigation instanceof UiNavigation.Back) {
      onClose();
    }
  }

  @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    TerritoryListLayout.Layout layout = commonLayout();
    UiScale scale = layout.scale();
    NeoForge1211UiRenderer renderer = new NeoForge1211UiRenderer(graphics, font);
    renderer.fillPhysicalBackground(width, height, TerritoryListLayout.BACKGROUND_COLOR);
    graphics.pose().pushPose();
    graphics.pose().scale(scale.value(), scale.value(), 1.0f);
    TerritoryListView.render(renderer, controller.state(), layout,
        scale.toVirtualX(mouseX), scale.toVirtualY(mouseY));
    graphics.pose().popPose();
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
    TerritoryListLayout.Layout layout = commonLayout();
    int x = layout.scale().toVirtualX(mouseX), y = layout.scale().toVirtualY(mouseY);
    for (TerritoryListLayout.Card card : layout.cards()) {
      if (card.teleportButton().contains(x, y)) {
        controller.handle(new TerritoryListEvent.ActionClicked(TerritoryListAction.TELEPORT,
            card.row().summary().territoryId()));
        return true;
      }
      if (card.row().owned() && card.manageButton().contains(x, y)) {
        controller.handle(new TerritoryListEvent.ActionClicked(TerritoryListAction.MANAGE,
            card.row().summary().territoryId()));
        return true;
      }
    }
    if (controller.state().screenState() == ScreenState.ERROR && layout.retryButton().contains(x, y)) {
      controller.handle(new TerritoryListEvent.Retry(System.nanoTime()));
      return true;
    }
    if (layout.previousButton().contains(x, y)) {
      controller.handle(new TerritoryListEvent.PreviousPage());
      return true;
    }
    if (layout.nextButton().contains(x, y)) {
      controller.handle(new TerritoryListEvent.NextPage());
      return true;
    }
    if (layout.esc().contains(x, y)) {
      controller.handle(new TerritoryListEvent.ActionClicked(TerritoryListAction.BACK, null));
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    if (scrollY != 0 && controller.state().totalPages() > 1) {
      controller.handle(new TerritoryListEvent.Scroll(scrollY < 0 ? 1 : -1));
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
  }

  @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == 256) { onClose(); return true; }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override public void onClose() {
    if (minecraft == null) return;
    if (parent != null) minecraft.setScreen(parent);
    else minecraft.setScreen(new NeoForge1211HomeScreen());
  }

  @Override public boolean isPauseScreen() { return false; }
  @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

  private TerritoryListLayout.Layout commonLayout() {
    TerritoryListLayout.Layout layout = TerritoryListLayout.calculate(width, height, controller.state(),
        new NeoForge1211UiTextMetrics(font));
    if (layout.pageSize() != controller.state().pageSize()) {
      controller.handle(new TerritoryListEvent.ViewportChanged(layout.pageSize()));
      layout = TerritoryListLayout.calculate(width, height, controller.state(),
          new NeoForge1211UiTextMetrics(font));
    }
    return layout;
  }

  private final class Port implements TerritoryListPort {
    private long requestId = -1;

    @Override public long nextRequestId() {
      long id = IDS.getAndIncrement();
      if (id < 0) throw new IllegalStateException("territory list request id exhausted");
      return id;
    }

    @Override public void requestTerritories(long id) {
      requestId = id;
      ClientTerritoryState.begin(id);
      EconomyServices.platform().network().sendToServer(new TerritoryDataRequestMessage(id));
    }

    @Override public void submit(TerritoryListAction action, TerritoryListRow row) {
      if (action == TerritoryListAction.TELEPORT) {
        EconomyServices.platform().network().sendToServer(
            new TeleportToTerritoryMessage(row.summary().territoryId()));
      } else if (action == TerritoryListAction.MANAGE && row.owned()) {
        Minecraft current = Minecraft.getInstance();
        if (current != null) current.setScreen(new NeoForge1211TerritoryDetailScreen(
            row.ownedSnapshot().orElseThrow(), NeoForge1211TerritoryListScreen.this));
      }
    }
  }
}
