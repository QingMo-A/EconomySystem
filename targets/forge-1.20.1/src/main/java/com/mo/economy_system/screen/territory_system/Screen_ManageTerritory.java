package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.client.ClientPlayerListState;
import com.mo.economy_system.common.client.TerritoryRequestIds;
import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseKind;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UnlockTerritoryBuffMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.network.UpgradeTerritoryBuffMessage;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.network.Forge1201SingleTerritoryClientState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Forge 1.20.1 management UI for canonical protocols 36-43. */
public final class Screen_ManageTerritory extends Screen {
  private static final int ROW_HEIGHT = 28;

  private final Screen parent;
  private Owned territory;
  private View view = View.MAIN;
  private long requestId = -1;
  private long playerRevision = -1;
  private int refreshDelay = -1;
  private int scroll;
  private boolean failed;

  private enum View { MAIN, BUFFS, ACCESS, RULES, TRANSFER }

  public Screen_ManageTerritory(Owned territory, Screen parent) {
    super(Component.translatable("screen.territory.manage"));
    this.territory = territory;
    this.parent = parent;
  }

  @Override
  protected void init() {
    if (requestId < 0) requestTerritory();
    if (playerRevision < 0) {
      playerRevision = ClientPlayerListState.snapshot().revision();
      EconomyServices.platform().network().sendToServer(ServerPlayerListRequestMessage.INSTANCE);
    }
    rebuildButtons();
  }

  @Override
  public void tick() {
    super.tick();
    if (refreshDelay == 0) {
      refreshDelay = -1;
      requestTerritory();
    } else if (refreshDelay > 0) {
      refreshDelay--;
    }
    Forge1201SingleTerritoryClientState.Snapshot snapshot =
        Forge1201SingleTerritoryClientState.snapshot();
    if (snapshot.requestId() == requestId) {
      if (snapshot.kind() == SingleTerritoryDataResponseKind.DATA
          && snapshot.territory() != null
          && !snapshot.territory().equals(territory)) {
        territory = snapshot.territory();
        failed = false;
        scroll = clampScroll(scroll);
        rebuildButtons();
      } else if (snapshot.kind() != SingleTerritoryDataResponseKind.DATA && !failed) {
        failed = true;
        rebuildButtons();
      }
    }
    long revision = ClientPlayerListState.snapshot().revision();
    if (revision != playerRevision) {
      playerRevision = revision;
      if (view == View.ACCESS || view == View.TRANSFER) {
        scroll = clampScroll(scroll);
        rebuildButtons();
      }
    }
  }

  private void requestTerritory() {
    long next = TerritoryRequestIds.nextSingleTerritory();
    requestId = next;
    EconomyServices.platform()
        .network()
        .sendToServer(new SingleTerritoryDataRequestMessage(territory.summary().territoryId(), next));
  }

  private void scheduleRefresh() {
    refreshDelay = 2;
  }

  private void rebuildButtons() {
    clearWidgets();
    if (failed) {
      addButton(width / 2 - 60, height / 2 + 18, 120, "button.retry", ignored -> {
        failed = false;
        requestTerritory();
      });
      return;
    }
    switch (view) {
      case MAIN -> mainButtons();
      case BUFFS -> buffButtons();
      case ACCESS -> accessButtons();
      case RULES -> ruleButtons();
      case TRANSFER -> transferButtons();
    }
    if (view != View.MAIN) {
      addButton(18, height - 30, 80, "gui.back", ignored -> setView(View.MAIN));
    } else {
      addButton(18, height - 30, 80, "gui.back", ignored -> Minecraft.getInstance().setScreen(parent));
    }
  }

  private void mainButtons() {
    int x = width / 2 - 100;
    int y = 48;
    addButton(x, y, 200, "message.territory_management.resize_territory", ignored -> {
      EconomyServices.platform()
          .network()
          .sendToServer(new ModifyTerritoryModeMessage(territory.summary().territoryId()));
      Minecraft.getInstance().setScreen(null);
    });
    addButton(x, y + 26, 200, "message.territory_management.buff", ignored -> setView(View.BUFFS));
    addButton(x, y + 52, 200, "message.territory_management.access", ignored -> setView(View.ACCESS));
    addButton(x, y + 78, 200, "message.territory_management.permissions", ignored -> setView(View.RULES));
    addButton(x, y + 104, 200, "message.territory_management.transfer_ownership", ignored -> setView(View.TRANSFER));
  }

  private void buffButtons() {
    List<Buff> values = territory.buffs();
    int count = Math.min(visibleRows(), Math.max(0, values.size() - scroll));
    for (int index = 0; index < count; index++) {
      Buff buff = values.get(scroll + index);
      String key = !buff.unlocked()
          ? "button.territory.buff.unlock"
          : buff.level() >= buff.maxLevel()
              ? "button.territory.buff.max"
              : "button.territory.buff.upgrade";
      Button button = addButton(width - 104, 47 + index * ROW_HEIGHT, 86, key, ignored -> {
        if (!buff.unlocked()) {
          EconomyServices.platform().network().sendToServer(
              new UnlockTerritoryBuffMessage(territory.summary().territoryId(), buff.id()));
        } else {
          EconomyServices.platform().network().sendToServer(
              new UpgradeTerritoryBuffMessage(territory.summary().territoryId(), buff.id()));
        }
        scheduleRefresh();
      });
      button.active = !buff.unlocked() || buff.level() < buff.maxLevel();
    }
  }

  private void accessButtons() {
    List<AccessTarget> values = accessTargets();
    int count = Math.min(visibleRows(), Math.max(0, values.size() - scroll));
    for (int index = 0; index < count; index++) {
      AccessTarget target = values.get(scroll + index);
      addButton(
          width - 104,
          47 + index * ROW_HEIGHT,
          86,
          target.allowed ? "button.territory.access.remove" : "button.territory.access.add",
          ignored -> {
            EconomyServices.platform().network().sendToServer(
                new UpdateTerritoryPermissionMessage(
                    territory.summary().territoryId(), target.id, !target.allowed));
            scheduleRefresh();
          });
    }
  }

  private void ruleButtons() {
    List<Rule> values = territory.rules();
    for (int index = 0; index < values.size(); index++) {
      Rule rule = values.get(index);
      addButton(width - 118, 47 + index * ROW_HEIGHT, 100, ruleLevelKey(rule.level()), ignored -> {
        RuleLevel next = switch (rule.level()) {
          case OWNER_ONLY -> RuleLevel.MEMBERS;
          case MEMBERS -> RuleLevel.EVERYONE;
          case EVERYONE -> RuleLevel.OWNER_ONLY;
        };
        EconomyServices.platform().network().sendToServer(
            new UpdateTerritoryRuleMessage(territory.summary().territoryId(), rule.action(), next));
        scheduleRefresh();
      });
    }
  }

  private void transferButtons() {
    List<PlayerSummary> values = transferTargets();
    int count = Math.min(visibleRows(), Math.max(0, values.size() - scroll));
    for (int index = 0; index < count; index++) {
      PlayerSummary player = values.get(scroll + index);
      addButton(width - 104, 47 + index * ROW_HEIGHT, 86, "button.territory.transfer", ignored -> {
        EconomyServices.platform().network().sendToServer(
            new TransferTerritoryOwnershipMessage(
                territory.summary().territoryId(), player.playerId()));
        Minecraft.getInstance().setScreen(parent);
      });
    }
  }

  private Button addButton(
      int x,
      int y,
      int buttonWidth,
      String translationKey,
      Button.OnPress onPress) {
    Button button = Button.builder(Component.translatable(translationKey), onPress)
        .bounds(x, y, buttonWidth, 20)
        .build();
    addRenderableWidget(button);
    return button;
  }

  private void setView(View next) {
    view = next;
    scroll = 0;
    rebuildButtons();
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(graphics);
    graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
    graphics.drawCenteredString(font, territory.summary().name(), width / 2, 28, 0xAAAAAA);
    if (failed) {
      graphics.drawCenteredString(
          font,
          Component.translatable("message.territory.management.load_failed"),
          width / 2,
          height / 2,
          0xFF8080);
    } else {
      renderRows(graphics);
    }
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  private void renderRows(GuiGraphics graphics) {
    switch (view) {
      case BUFFS -> {
        List<Buff> values = territory.buffs();
        int count = Math.min(visibleRows(), Math.max(0, values.size() - scroll));
        for (int index = 0; index < count; index++) {
          Buff buff = values.get(scroll + index);
          graphics.drawString(
              font,
              buff.displayText() + "  " + buff.level() + "/" + buff.maxLevel(),
              18,
              53 + index * ROW_HEIGHT,
              0xFFFFFF);
        }
      }
      case ACCESS -> {
        List<AccessTarget> values = accessTargets();
        int count = Math.min(visibleRows(), Math.max(0, values.size() - scroll));
        for (int index = 0; index < count; index++) {
          AccessTarget target = values.get(scroll + index);
          graphics.drawString(font, target.name, 18, 53 + index * ROW_HEIGHT, 0xFFFFFF);
        }
      }
      case RULES -> {
        List<Rule> values = territory.rules();
        for (int index = 0; index < values.size(); index++) {
          graphics.drawString(
              font,
              Component.translatable("message.territory.rule." + values.get(index).action().id()),
              18,
              53 + index * ROW_HEIGHT,
              0xFFFFFF);
        }
      }
      case TRANSFER -> {
        List<PlayerSummary> values = transferTargets();
        int count = Math.min(visibleRows(), Math.max(0, values.size() - scroll));
        for (int index = 0; index < count; index++) {
          graphics.drawString(
              font, values.get(scroll + index).playerName(), 18, 53 + index * ROW_HEIGHT, 0xFFFFFF);
        }
      }
      case MAIN -> {}
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if ((view == View.BUFFS || view == View.ACCESS || view == View.TRANSFER) && delta != 0) {
      int updated = clampScroll(scroll + (delta < 0 ? 1 : -1));
      if (updated != scroll) {
        scroll = updated;
        rebuildButtons();
      }
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  private List<AccessTarget> accessTargets() {
    Map<UUID, AccessTarget> values = new LinkedHashMap<>();
    for (PlayerSummary player : ClientPlayerListState.snapshot().players()) {
      if (!player.playerId().equals(territory.summary().ownerId())) {
        values.put(player.playerId(), new AccessTarget(player.playerId(), player.playerName(), false));
      }
    }
    for (Member member : territory.authorizedMembers()) {
      values.put(member.playerId(), new AccessTarget(member.playerId(), member.playerName(), true));
    }
    return List.copyOf(values.values());
  }

  private List<PlayerSummary> transferTargets() {
    List<PlayerSummary> values = new ArrayList<>();
    for (PlayerSummary player : ClientPlayerListState.snapshot().players()) {
      if (!player.playerId().equals(territory.summary().ownerId())) values.add(player);
    }
    return List.copyOf(values);
  }

  private int clampScroll(int value) {
    int size = switch (view) {
      case BUFFS -> territory.buffs().size();
      case ACCESS -> accessTargets().size();
      case TRANSFER -> transferTargets().size();
      case MAIN, RULES -> 0;
    };
    return Math.max(0, Math.min(value, Math.max(0, size - visibleRows())));
  }

  private int visibleRows() {
    return Math.max(1, (height - 88) / ROW_HEIGHT);
  }

  private static String ruleLevelKey(RuleLevel level) {
    return "message.territory.rule.level." + level.id();
  }

  private record AccessTarget(UUID id, String name, boolean allowed) {}

  @Override
  public void onClose() {
    Minecraft.getInstance().setScreen(parent);
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
