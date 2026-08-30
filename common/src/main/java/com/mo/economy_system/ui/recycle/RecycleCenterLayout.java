package com.mo.economy_system.ui.recycle;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.ArrayList;
import java.util.List;
public final class RecycleCenterLayout {
  public static final int BACKGROUND_COLOR = 0x500A0A14;
  private RecycleCenterLayout() {}
  public static Layout calculate(int physicalWidth, int physicalHeight, RecycleCenterState state, UiTextMetrics metrics) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT); int w=scale.virtualWidth(),h=scale.virtualHeight(),p=EconomyUiTheme.PANEL_PADDING;
    UiRect list = new UiRect(p,18,Math.max(1,w/2-p),Math.max(1,h-38)); UiRect detail = new UiRect(list.right()+10,18,Math.max(1,w-list.right()-10-p),list.height());
    List<Row> rows=new ArrayList<>(); int rowH=24; for(int i=0;i<state.offers().size();i++){int y=list.y()+28+i*(rowH+4);if(y+rowH>list.bottom()-26)break;rows.add(new Row(state.offers().get(i),new UiRect(list.x()+8,y,list.width()-16,rowH),state.offers().get(i).itemId().equals(state.selectedItemId())));}
    UiRect submit=new UiRect(detail.x()+12,detail.bottom()-34,Math.max(1,detail.width()-24),22); UiRect back=new UiRect(p,h-p-22,82,22); UiRect retry=new UiRect(detail.x()+12,detail.y()+detail.height()/2,Math.max(1,detail.width()-24),22); UiRect minus=new UiRect(detail.x()+12,detail.y()+100,30,22); UiRect plus=new UiRect(minus.right()+6,minus.y(),30,22); UiRect all=new UiRect(plus.right()+6,minus.y(),52,22);
    return new Layout(scale,list,detail,List.copyOf(rows),submit,back,retry,minus,plus,all,metrics==null?UiTextMetrics.APPROXIMATE:metrics);
  }
  public record Layout(UiScale scale,UiRect list,UiRect detail,List<Row> rows,UiRect submit,UiRect back,UiRect retry,UiRect minus,UiRect plus,UiRect all,UiTextMetrics metrics){public Layout{rows=List.copyOf(rows);}}
  public record Row(com.mo.economy_system.common.network.RecycleOfferSnapshot offer,UiRect rect,boolean selected) {}
}
