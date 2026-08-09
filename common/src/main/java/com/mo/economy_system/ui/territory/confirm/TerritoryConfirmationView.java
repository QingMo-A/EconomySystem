package com.mo.economy_system.ui.territory.confirm;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

public final class TerritoryConfirmationView {
  private TerritoryConfirmationView() {}
  public static void render(EconomyUiRenderer renderer,TerritoryConfirmationState state,TerritoryConfirmationLayout.Layout layout,int mouseX,int mouseY){renderer.fill(new UiRect(0,0,layout.scale().virtualWidth(),layout.scale().virtualHeight()),0xB0000000);renderer.card(layout.card(),EconomyUiTheme.MARKET_CARD,false);renderer.icon(UiIcon.TERRITORY,new UiRect(layout.card().x()+12,layout.card().y()+10,14,14));String title=state.kind()==TerritoryConfirmationKind.REMOVE_TERRITORY?"screen.territory.confirm.remove_title":"screen.territory.confirm.member_title";String body=state.kind()==TerritoryConfirmationKind.REMOVE_TERRITORY?"screen.territory.confirm.remove_body":"screen.territory.confirm.member_body";List<String> args=state.kind()==TerritoryConfirmationKind.REMOVE_TERRITORY?List.of(state.territoryName()):List.of(state.memberName());renderer.translatedTextInRect(title,List.of(),new UiRect(layout.card().x()+30,layout.card().y()+10,layout.card().width()-42,16),EconomyUiTheme.TEXT_ERROR,UiTextAlignment.LEFT);renderer.translatedTextInRect(body,args,layout.message(),EconomyUiTheme.TEXT_PRIMARY,UiTextAlignment.CENTER);renderer.translatedButton(layout.confirm(),EconomyUiTheme.MARKET_BUTTON,"screen.territory.confirm.confirm",List.of(),layout.confirm().contains(mouseX,mouseY),state.can(TerritoryConfirmationAction.CONFIRM));renderer.translatedButton(layout.cancel(),EconomyUiTheme.DISABLED_BUTTON,"screen.territory.confirm.cancel",List.of(),layout.cancel().contains(mouseX,mouseY),state.can(TerritoryConfirmationAction.CANCEL));}
}
