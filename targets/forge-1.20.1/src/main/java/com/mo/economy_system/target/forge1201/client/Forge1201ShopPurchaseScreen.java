package com.mo.economy_system.target.forge1201.client;

import com.mo.economy_system.common.network.ShopBuyItemMessage;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.ui.core.UiNavigation;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.shop.ShopPurchaseAction;
import com.mo.economy_system.ui.shop.ShopPurchaseController;
import com.mo.economy_system.ui.shop.ShopPurchaseEvent;
import com.mo.economy_system.ui.shop.ShopPurchaseLayout;
import com.mo.economy_system.ui.shop.ShopPurchasePort;
import com.mo.economy_system.ui.shop.ShopPurchaseView;
import com.mo.economy_system.ui.shop.ShopRow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Forge widget shell for common shop purchase state and validation. */
public final class Forge1201ShopPurchaseScreen extends Screen {
  private final Screen parent; private final ShopPurchaseController controller; private EditBox quantity;
  public Forge1201ShopPurchaseScreen(ShopRow row, Screen parent) { super(Component.translatable("screen.shop.purchase.title")); this.parent = parent; controller = new ShopPurchaseController(row, new Port()); }
  @Override protected void init() { ShopPurchaseLayout.Layout layout = layout(); UiScale scale = layout.scale(); quantity = new Forge1201UnderlinedEditBox(font, Math.round(layout.quantity().x()*scale.value()), Math.round(layout.quantity().y()*scale.value()), Math.max(1,Math.round(layout.quantity().width()*scale.value())), Math.max(1,Math.round(layout.quantity().height()*scale.value())), Component.translatable("screen.shop.purchase.quantity")); Forge1201UiInputAdapter.apply(quantity); quantity.setMaxLength(9); quantity.setValue(Integer.toString(controller.state().quantity())); quantity.setResponder(value -> controller.handle(new ShopPurchaseEvent.QuantityChanged(parse(value)))); addRenderableWidget(quantity); }
  private int parse(String value) { try { return Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) { return 0; } }
  @Override public void tick() { super.tick(); controller.pollNavigation().ifPresent(this::navigate); }
  private void navigate(UiNavigation navigation) { if (minecraft != null && navigation instanceof UiNavigation.Back) minecraft.setScreen(parent == null ? new Forge1201ShopScreen() : parent); }
  @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick) { Forge1201UiRenderer renderer = new Forge1201UiRenderer(graphics,font); renderer.fillPhysicalBackground(width,height,ShopPurchaseLayout.BACKGROUND_COLOR); ShopPurchaseLayout.Layout layout=layout(); UiScale scale=layout.scale(); graphics.pose().pushPose(); graphics.pose().scale(scale.value(),scale.value(),1); ShopPurchaseView.render(renderer,controller.state(),layout,scale.toVirtualX(mouseX),scale.toVirtualY(mouseY)); graphics.pose().popPose(); super.render(graphics,mouseX,mouseY,partialTick); }
  @Override public boolean mouseClicked(double mouseX,double mouseY,int button) { ShopPurchaseLayout.Layout layout=layout(); int x=layout.scale().toVirtualX(mouseX),y=layout.scale().toVirtualY(mouseY); if(layout.confirm().contains(x,y)){controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.CONFIRM));return true;}if(layout.back().contains(x,y)){controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.BACK));return true;}return super.mouseClicked(mouseX,mouseY,button); }
  @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(keyCode==256){controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.BACK));return true;}return super.keyPressed(keyCode,scanCode,modifiers);}
  @Override public void onClose(){controller.handle(new ShopPurchaseEvent.ActionClicked(ShopPurchaseAction.BACK));}
  @Override public boolean isPauseScreen(){return false;}
  /** The purchase dialog draws its own physical backdrop; vanilla's default blur must not run. */
  @Override public void renderBackground(GuiGraphics graphics) {}
  private ShopPurchaseLayout.Layout layout(){return ShopPurchaseLayout.calculate(width,height,controller.state());}
  private final class Port implements ShopPurchasePort {
    @Override public int availableQuantity(ShopRow row) { Player player=Minecraft.getInstance().player; ResourceLocation id=ResourceLocation.tryParse(row.item().itemId()); if(player==null||id==null||!BuiltInRegistries.ITEM.containsKey(id))return 0; Item item=BuiltInRegistries.ITEM.get(id); int max=item.getMaxStackSize(), available=0; for(ItemStack stack:player.getInventory().items){if(stack.isEmpty())available+=max;else if(stack.getItem()==item)available+=Math.max(0,stack.getMaxStackSize()-stack.getCount());}return available; }
    @Override public void submit(ShopRow row,int quantity){EconomyServices.platform().network().sendToServer(new ShopBuyItemMessage(row.item().shopItemId(),quantity));}
  }
}
