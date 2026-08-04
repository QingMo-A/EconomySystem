package com.mo.economy_system.target.neoforge1211.protocol;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.territory.RecallPotionReservation;
import com.mo.economy_system.common.territory.TerritoryTeleportService;
import com.mo.economy_system.item.EconomySystem_Items;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class NeoForge1211RecallPotionInventory implements TerritoryTeleportService.Inventory, RecallPotionReservation.Slots<ItemStack>{
  private final ServerPlayer player;NeoForge1211RecallPotionInventory(ServerPlayer player){this.player=player;}
  public Optional<TerritoryTeleportService.Reservation> reserveRecallPotion() throws Exception{for(int i=0;i<size();i++)if(get(i).is(EconomySystem_Items.RECALL_POTION.get()))return Optional.of(RecallPotionReservation.reserve(i,get(i),this));return Optional.empty();}
  public int size(){return player.getInventory().items.size();}public ItemStack get(int i){return player.getInventory().items.get(i);}public void set(int i,ItemStack v){player.getInventory().items.set(i,v);}
  public ItemStack copy(ItemStack v){return v.copy();}public ItemStack withCount(ItemStack v,int c){return v.copyWithCount(c);}public boolean equivalent(ItemStack a,ItemStack b){return ItemStack.isSameItemSameComponents(a,b);}public boolean empty(ItemStack v){return v.isEmpty();}
  public boolean canMerge(ItemStack a,ItemStack b){return !a.isEmpty()&&ItemStack.isSameItemSameComponents(a,b);}public int count(ItemStack v){return v.getCount();}public int maximum(ItemStack v){return v.getMaxStackSize();}
  public ItemStack withAddedOne(ItemStack a,ItemStack one){ItemStack r=a.isEmpty()?one.copy():a.copy();if(!a.isEmpty())r.grow(1);return r;}
  public void markChanged(){player.getInventory().setChanged();}public void synchronizeClient(){player.containerMenu.broadcastChanges();}
  public void warning(String stage,Exception error){EconomySystem.LOGGER.warn("Territory teleport inventory issue stage={} player={}",stage,player.getUUID(),error);}
}
