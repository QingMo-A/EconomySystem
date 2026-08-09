package com.mo.economy_system.ui.territory.confirm;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

public final class TerritoryConfirmationLayout {
  private TerritoryConfirmationLayout() {}
  public static Layout calculate(int physicalWidth,int physicalHeight,TerritoryConfirmationState state){UiScale scale=UiScale.fit(physicalWidth,physicalHeight,EconomyUiTheme.BASE_WIDTH,EconomyUiTheme.BASE_HEIGHT);int width=scale.virtualWidth(),height=scale.virtualHeight(),cardWidth=Math.min(340,Math.max(220,width-32)),cardHeight=150,x=Math.max(8,(width-cardWidth)/2),y=Math.max(8,(height-cardHeight)/2);return new Layout(scale,new UiRect(x,y,cardWidth,cardHeight),new UiRect(x+16,y+38,cardWidth-32,42),new UiRect(x+cardWidth/2-104,y+cardHeight-32,96,22),new UiRect(x+cardWidth/2+8,y+cardHeight-32,96,22));}
  public record Layout(UiScale scale,UiRect card,UiRect message,UiRect confirm,UiRect cancel){}
}
