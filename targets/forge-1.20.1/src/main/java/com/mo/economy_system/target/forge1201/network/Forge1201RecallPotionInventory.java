package com.mo.economy_system.target.forge1201.network;
import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.territory.RecallPotionReservation;
import com.mo.economy_system.common.territory.TerritoryTeleportService;
import com.mo.economy_system.target.forge1201.item.Forge1201Items;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

final class Forge1201RecallPotionInventory implements TerritoryTeleportService.Inventory,RecallPotionReservation.Slots<ItemStack>{
  private static final Logger LOGGER=LogUtils.getLogger();private final ServerPlayer player;Forge1201RecallPotionInventory(ServerPlayer player){this.player=player;}
  public Optional<TerritoryTeleportService.Reservation> reserveRecallPotion() throws Exception{for(int i=0;i<size();i++)if(get(i).is(Forge1201Items.RECALL_POTION.get()))return Optional.of(RecallPotionReservation.reserve(i,get(i),this));return Optional.empty();}
  public int size(){return player.getInventory().items.size();}public ItemStack get(int i){return player.getInventory().items.get(i);}public void set(int i,ItemStack v){player.getInventory().items.set(i,v);}
  public ItemStack copy(ItemStack v){return v.copy();}public ItemStack withCount(ItemStack v,int c){ItemStack r=v.copy();r.setCount(c);return r;}public boolean equivalent(ItemStack a,ItemStack b){return ItemStack.isSameItemSameTags(a,b);}public boolean empty(ItemStack v){return v.isEmpty();}
  public boolean canMerge(ItemStack a,ItemStack b){return !a.isEmpty()&&ItemStack.isSameItemSameTags(a,b);}public int count(ItemStack v){return v.getCount();}public int maximum(ItemStack v){return v.getMaxStackSize();}
  public ItemStack withAddedOne(ItemStack a,ItemStack one){ItemStack r=a.isEmpty()?one.copy():a.copy();if(!a.isEmpty())r.grow(1);return r;}
  public void markChanged(){player.getInventory().setChanged();}public void synchronizeClient(){player.containerMenu.broadcastChanges();}
  public void warning(String stage,Exception error){LOGGER.warn("Territory teleport inventory issue stage={} player={}",stage,player.getUUID(),error);}
}
