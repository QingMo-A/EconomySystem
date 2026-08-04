package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.EconomyConstants;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Minimal Forge item registrations required by protocol 19. */
public final class Forge1201Items {
  static final int RECALL_POTION_MAX_STACK_SIZE = 1;
  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, EconomyConstants.MOD_ID);

  public static final RegistryObject<Item> RECALL_POTION = ITEMS.register(
      "recall_potion", Forge1201Items::createRecallPotion);

  private Forge1201Items() {}

  public static void register(IEventBus bus) {
    ITEMS.register(bus);
  }

  static Item createRecallPotion() {
    return new Item(new Item.Properties().stacksTo(RECALL_POTION_MAX_STACK_SIZE).fireResistant());
  }
}
