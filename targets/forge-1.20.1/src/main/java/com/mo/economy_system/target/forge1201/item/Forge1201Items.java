package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.EconomyConstants;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Minimal Forge item registrations required by protocol 19. */
public final class Forge1201Items {
  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, EconomyConstants.MOD_ID);

  public static final RegistryObject<Item> RECALL_POTION = ITEMS.register(
      "recall_potion", () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));

  private Forge1201Items() {}

  public static void register(IEventBus bus) {
    ITEMS.register(bus);
  }
}
