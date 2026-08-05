package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Screen_TerritoryMembers extends Screen {
  private final Owned territory;
  private final Screen back;
  private EditBox search;
  private int scroll;

  Screen_TerritoryMembers(Owned territory, Screen back) {
    super(Component.translatable("screen.territory_members.title"));
    this.territory = Objects.requireNonNull(territory);
    this.back = Objects.requireNonNull(back);
  }

  protected void init() {
    search =
        new EditBox(
            font,
            width / 2 - 100,
            24,
            200,
            20,
            Component.translatable("screen.territory_members.search"));
    search.setResponder(v -> scroll = 0);
    addRenderableWidget(search);
    addRenderableWidget(
        Button.builder(
                Component.translatable("button.territory.member_invite"),
                b ->
                    Minecraft.getInstance()
                        .setScreen(
                            new Screen_InvitePlayer(
                                territory.summary().territoryId(),
                                territory.summary().name(),
                                territory.summary().ownerId(),
                                memberIds(),
                                this)))
            .bounds(12, height - 24, 100, 20)
            .build());
    addRenderableWidget(
        Button.builder(Component.translatable("gui.back"), b -> onClose())
            .bounds(width - 72, height - 24, 60, 20)
            .build());
  }

  private Set<java.util.UUID> memberIds() {
    return territory.authorizedMembers().stream()
        .map(Member::playerId)
        .collect(Collectors.toUnmodifiableSet());
  }

  private List<Member> visible() {
    String q = search == null ? "" : search.getValue().toLowerCase(Locale.ROOT);
    return territory.authorizedMembers().stream()
        .filter(m -> m.playerName().toLowerCase(Locale.ROOT).contains(q))
        .toList();
  }

  public void render(GuiGraphics g, int mx, int my, float tick) {
    renderBackground(g);
    g.drawCenteredString(font, title, width / 2, 8, 0xffffff);
    g.drawString(font, territory.summary().name(), 12, 50, 0xffffff);
    List<Member> rows = visible();
    int count = Math.max(0, (height - 100) / 24);
    scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - count)));
    if (rows.isEmpty())
      g.drawCenteredString(
          font,
          Component.translatable("screen.territory_members.empty"),
          width / 2,
          height / 2,
          0xaaaaaa);
    for (int i = 0; i < Math.min(count, rows.size() - scroll); i++) {
      Member m = rows.get(scroll + i);
      int y = 66 + i * 24;
      g.drawString(font, m.playerName(), 20, y + 6, 0xffffff);
      int x = width - 82;
      g.fill(x, y, x + 62, y + 20, 0xff8b2525);
      g.drawCenteredString(
          font, Component.translatable("button.territory.member_remove"), x + 31, y + 6, 0xffffff);
    }
    super.render(g, mx, my, tick);
  }

  public boolean mouseClicked(double mx, double my, int button) {
    if (button == 0) {
      List<Member> rows = visible();
      int count = Math.max(0, (height - 100) / 24);
      for (int i = 0; i < Math.min(count, rows.size() - scroll); i++) {
        int y = 66 + i * 24;
        int x = width - 82;
        if (mx >= x && mx < x + 62 && my >= y && my < y + 20) {
          Member m = rows.get(scroll + i);
          Minecraft.getInstance()
              .setScreen(
                  new Screen_ConfirmTerritoryMemberRemoval(
                      territory.summary().territoryId(),
                      territory.summary().name(),
                      m.playerId(),
                      m.playerName(),
                      this));
          return true;
        }
      }
    }
    return super.mouseClicked(mx, my, button);
  }

  public boolean mouseScrolled(double x, double y, double delta) {
    if (delta != 0) {
      scroll += delta < 0 ? 1 : -1;
      return true;
    }
    return super.mouseScrolled(x, y, delta);
  }

  public void onClose() {
    Minecraft.getInstance().setScreen(back);
  }
}
