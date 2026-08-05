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
    TerritoryMembersLayout.Layout layout = layout();
    search =
        new EditBox(
            font,
            layout.search().x(),
            layout.search().y(),
            layout.search().width(),
            layout.search().height(),
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
            .bounds(
                layout.invite().x(),
                layout.invite().y(),
                layout.invite().width(),
                layout.invite().height())
            .build());
    addRenderableWidget(
        Button.builder(Component.translatable("gui.back"), b -> onClose())
            .bounds(
                layout.back().x(), layout.back().y(), layout.back().width(), layout.back().height())
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
    TerritoryMembersLayout.Layout layout = layout();
    scroll = layout.scroll();
    if (layout.rows().isEmpty())
      g.drawCenteredString(
          font,
          Component.translatable("screen.territory_members.empty"),
          width / 2,
          height / 2,
          0xaaaaaa);
    for (TerritoryMembersLayout.MemberRow row : layout.rows()) {
      g.drawString(font, row.member().playerName(), 20, row.row().y() + 6, 0xffffff);
      var button = row.removeButton();
      g.fill(
          button.x(),
          button.y(),
          button.x() + button.width(),
          button.y() + button.height(),
          0xff8b2525);
      g.drawCenteredString(
          font,
          Component.translatable("button.territory.member_remove"),
          button.x() + button.width() / 2,
          button.y() + 6,
          0xffffff);
    }
    super.render(g, mx, my, tick);
  }

  public boolean mouseClicked(double mx, double my, int button) {
    if (button == 0) {
      for (TerritoryMembersLayout.MemberRow row : layout().rows()) {
        if (row.removeButton().contains(mx, my)) {
          Minecraft.getInstance()
              .setScreen(
                  new Screen_ConfirmTerritoryMemberRemoval(
                      territory.summary().territoryId(),
                      territory.summary().name(),
                      row.member().playerId(),
                      row.member().playerName(),
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

  private TerritoryMembersLayout.Layout layout() {
    return TerritoryMembersLayout.layout(
        width,
        height,
        visible().stream()
            .map(
                member ->
                    new TerritoryMembersLayout.MemberValue(member.playerId(), member.playerName()))
            .toList(),
        scroll);
  }
}
