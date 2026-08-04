package com.mo.economy_system.screen.territory_system;

import com.mo.economy_system.common.network.RemoveTerritoryMessage;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class Screen_ConfirmTerritoryRemoval extends Screen {
  private final Territory territory;
  private boolean submitted;
  public Screen_ConfirmTerritoryRemoval(Territory territory) { super(Component.translatable("screen.territory_remove.title")); this.territory=territory; }
  @Override protected void init(){
    addRenderableWidget(Button.builder(Component.translatable("button.territory.delete_confirm"),b->{if(submitted)return;submitted=true;b.active=false;EconomySystem_NetworkManager.sendToServer(new RemoveTerritoryMessage(territory.getTerritoryID()));Minecraft.getInstance().setScreen(new Screen_Territory());}).bounds(width/2-105,height/2+35,100,20).build());
    addRenderableWidget(Button.builder(Component.translatable("button.territory.delete_cancel"),b->onClose()).bounds(width/2+5,height/2+35,100,20).build());
  }
  @Override public void render(GuiGraphics g,int x,int y,float tick){renderBackground(g,x,y,tick);g.drawCenteredString(font,title,width/2,height/2-50,0xffffff);g.drawCenteredString(font,Component.literal(territory.getName()),width/2,height/2-30,0xffaaaa);g.drawCenteredString(font,Component.translatable("screen.territory_remove.irreversible"),width/2,height/2-10,0xff5555);g.drawCenteredString(font,Component.translatable("screen.territory_remove.no_refund"),width/2,height/2+5,0xffaa55);super.render(g,x,y,tick);}
  @Override public void onClose(){Minecraft.getInstance().setScreen(new Screen_ManageTerritory(territory));}
}
