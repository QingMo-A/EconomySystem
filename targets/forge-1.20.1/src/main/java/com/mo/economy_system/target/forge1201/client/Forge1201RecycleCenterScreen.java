package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.client.ClientRecycleState;
import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.network.RecycleDataResponseMessage;
import com.mo.economy_system.common.network.RecycleSubmitMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.recycle.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.UUID;

public final class Forge1201RecycleCenterScreen extends Screen {
  private final Screen parent; private final Port port=new Port(); private final RecycleCenterController controller=new RecycleCenterController(port); private long applied=-1;
  public Forge1201RecycleCenterScreen(){this(null);} public Forge1201RecycleCenterScreen(Screen parent){super(Component.translatable(EconomyUiRoute.RECYCLE.titleKey()));this.parent=parent;}
  @Override protected void init(){if(controller.state().screenState()==ScreenState.IDLE)controller.handle(new RecycleCenterEvent.Initialize(System.nanoTime()));}
  @Override public void tick(){super.tick();controller.handle(new RecycleCenterEvent.Tick(System.nanoTime()));var s=ClientRecycleState.snapshot();if(s.requestId()!=applied&&s.requestId()==port.requestId&&!s.loading()){applied=s.requestId();controller.handle(new RecycleCenterEvent.DataLoaded(RecycleDataResponseMessage.data(s.requestId(),s.serverNowMillis(),s.cycleEndsAt(),s.offers())));}controller.pollNavigation().ifPresent(this::navigate);}
  @Override public void render(GuiGraphics g,int mx,int my,float pt){var l=RecycleCenterLayout.calculate(width,height,controller.state(),new Forge1201UiTextMetrics(font));UiScale s=l.scale();Forge1201UiRenderer r=new Forge1201UiRenderer(g,font);r.fillPhysicalBackground(width,height,RecycleCenterLayout.BACKGROUND_COLOR);g.pose().pushPose();g.pose().scale(s.value(),s.value(),1);RecycleCenterView.render(r,controller.state(),l,s.toVirtualX(mx),s.toVirtualY(my));g.pose().popPose();super.render(g,mx,my,pt);}
  @Override public boolean mouseClicked(double mx,double my,int button){var l=RecycleCenterLayout.calculate(width,height,controller.state(),new Forge1201UiTextMetrics(font));int x=l.scale().toVirtualX(mx),y=l.scale().toVirtualY(my);for(var row:l.rows())if(row.rect().contains(x,y)){controller.handle(new RecycleCenterEvent.Selected(row.offer().itemId()));return true;}if(l.minus().contains(x,y)){controller.handle(new RecycleCenterEvent.AmountChanged(controller.state().amount()-1));return true;}if(l.plus().contains(x,y)){controller.handle(new RecycleCenterEvent.AmountChanged(controller.state().amount()+1));return true;}if(l.all().contains(x,y)){var o=controller.state().selected();if(o!=null)controller.handle(new RecycleCenterEvent.AmountChanged(o.maxSubmitAmount()));return true;}if(l.submit().contains(x,y)){controller.handle(new RecycleCenterEvent.ActionClicked(RecycleCenterAction.SUBMIT));return true;}if(l.back().contains(x,y)){onClose();return true;}if(controller.state().screenState()==ScreenState.ERROR&&l.retry().contains(x,y)){controller.handle(new RecycleCenterEvent.ActionClicked(RecycleCenterAction.RETRY));return true;}return super.mouseClicked(mx,my,button);}
  @Override public boolean keyPressed(int key,int scan,int mods){if(key==256){onClose();return true;}return super.keyPressed(key,scan,mods);}
  @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent!=null?parent:new Forge1201HomeScreen());}
  @Override public boolean isPauseScreen(){return false;}
  private void navigate(UiNavigation n){if(n instanceof UiNavigation.Route r&&r.route()==EconomyUiRoute.HOME)onClose();}
  private final class Port implements RecycleCenterPort{long requestId=-1;public long nextRequestId(){return ClientRecycleState.nextRequestId();}public void requestData(long id){requestId=id;ClientRecycleState.begin(id);EconomyServices.platform().network().sendToServer(new com.mo.economy_system.common.network.RecycleDataRequestMessage(id));}public void submit(long id,UUID submissionId,String itemId,int amount){EconomyServices.platform().network().sendToServer(new RecycleSubmitMessage(id,submissionId,itemId,amount));}}
}
