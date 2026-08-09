package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.network.RemoveTerritoryMemberMessage;
import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationAction;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationController;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationEvent;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationKind;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationLayout;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationPort;
import com.mo.economy_system.ui.territory.confirm.TerritoryConfirmationView;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** NeoForge shell for shared territory destructive-operation confirmation. */
public final class NeoForge1211TerritoryConfirmationScreen extends Screen {
  private final Screen parent;private final TerritoryConfirmationController controller;
  public NeoForge1211TerritoryConfirmationScreen(TerritoryConfirmationKind kind,UUID territoryId,String territoryName,UUID memberId,String memberName,Screen parent){super(Component.translatable("screen.territory.confirm.remove_title"));this.parent=parent;controller=new TerritoryConfirmationController(kind,territoryId,territoryName,memberId,memberName,new Port());}
  @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){TerritoryConfirmationLayout.Layout layout=TerritoryConfirmationLayout.calculate(width,height,controller.state());UiScale scale=layout.scale();graphics.pose().pushPose();graphics.pose().scale(scale.value(),scale.value(),1);TerritoryConfirmationView.render(new NeoForge1211UiRenderer(graphics,font),controller.state(),layout,scale.toVirtualX(mouseX),scale.toVirtualY(mouseY));graphics.pose().popPose();super.render(graphics,mouseX,mouseY,partialTick);}
  @Override public boolean mouseClicked(double mouseX,double mouseY,int button){TerritoryConfirmationLayout.Layout layout=TerritoryConfirmationLayout.calculate(width,height,controller.state());int x=layout.scale().toVirtualX(mouseX),y=layout.scale().toVirtualY(mouseY);if(layout.confirm().contains(x,y)){controller.handle(new TerritoryConfirmationEvent.ActionClicked(TerritoryConfirmationAction.CONFIRM));return true;}if(layout.cancel().contains(x,y)){controller.handle(new TerritoryConfirmationEvent.ActionClicked(TerritoryConfirmationAction.CANCEL));return true;}return super.mouseClicked(mouseX,mouseY,button);}
  @Override public void tick(){super.tick();controller.pollNavigation().ifPresent(this::navigate);}
  private void navigate(UiNavigation navigation){if(minecraft!=null&&navigation instanceof UiNavigation.Back)minecraft.setScreen(parent);}
  @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(keyCode==256){controller.handle(new TerritoryConfirmationEvent.ActionClicked(TerritoryConfirmationAction.CANCEL));return true;}return super.keyPressed(keyCode,scanCode,modifiers);}
  @Override public void onClose(){controller.handle(new TerritoryConfirmationEvent.ActionClicked(TerritoryConfirmationAction.CANCEL));}
  @Override public boolean isPauseScreen(){return false;}
  private final class Port implements TerritoryConfirmationPort{@Override public void removeTerritory(UUID territoryId){EconomyServices.platform().network().sendToServer(new RemoveTerritoryMessage(territoryId));}@Override public void removeMember(UUID territoryId,UUID memberId){EconomyServices.platform().network().sendToServer(new RemoveTerritoryMemberMessage(territoryId,memberId));}}
}
