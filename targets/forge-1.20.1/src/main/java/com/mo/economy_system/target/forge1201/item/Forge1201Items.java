package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.EconomyConstants;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Forge item registrations required by migrated gameplay protocols. */
public final class Forge1201Items {
  static final int RECALL_POTION_MAX_STACK_SIZE = 1;
  public static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, EconomyConstants.MOD_ID);
  public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
      DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EconomyConstants.MOD_ID);
  public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EconomyConstants.MOD_ID);

  public static final RegistryObject<Item> GUITAR = ITEMS.register(
      "guitar", () -> new Item(new Item.Properties().stacksTo(1).fireResistant()));

  public static final RegistryObject<Item> RECALL_POTION = ITEMS.register(
      "recall_potion", Forge1201Items::createRecallPotion);
  public static final RegistryObject<Item> WORMHOLE_POTION = ITEMS.register(
      "wormhole_potion", Forge1201Items::createWormholePotion);
  public static final RegistryObject<Item> CLAIM_WAND = ITEMS.register(
      "claim_wand", () -> new Forge1201ClaimWand(new Item.Properties().stacksTo(1)));

  public static final RegistryObject<Item> SUPPORTER_HAT = ITEMS.register(
      "supporter_hat", () -> new Forge1201SupporterHat(
          ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1)));
  public static final RegistryObject<Item> PLAYER_DOLL_HAT = registerDollHat(
      "player_doll_hat", "dc5eb054-afdc-44d2-9062-9d18dbe3d30c", "___QingMo___");
  public static final RegistryObject<Item> POXIAOJIN_DOLL_HAT = registerDollHat(
      "poxiaojin_doll_hat", "a08caa8a-2e6a-418d-8bae-4980ddaba41d", "poxiaojin");
  public static final RegistryObject<Item> HANHANYU_DOLL_HAT = registerDollHat(
      "hanhanyu_doll_hat", "5c728c19-aed5-4143-af08-bc40f5069f98", "__HanHanYu__");
  public static final RegistryObject<Item> PLAYER_351987654321_DOLL_HAT = registerDollHat(
      "player_351987654321_doll_hat", "e453107e-a998-47b9-a55f-d3aeded19a96", "351987654321");

  public static final RegistryObject<SoundEvent> NOTE_C = registerSound("note.c");
  public static final RegistryObject<SoundEvent> NOTE_DM = registerSound("note.dm");
  public static final RegistryObject<SoundEvent> NOTE_EM = registerSound("note.em");
  public static final RegistryObject<SoundEvent> NOTE_F = registerSound("note.f");
  public static final RegistryObject<SoundEvent> NOTE_G = registerSound("note.g");
  public static final RegistryObject<SoundEvent> NOTE_AM = registerSound("note.am");
  public static final RegistryObject<SoundEvent> NOTE_BM = registerSound("note.bm");

  public static final RegistryObject<CreativeModeTab> ECONOMY_SYSTEM_TAB = CREATIVE_TABS.register(
      "economy_system_tab", () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.economy_system.tab"))
          .icon(() -> new ItemStack(CLAIM_WAND.get()))
          .displayItems((parameters, output) -> {
            output.accept(CLAIM_WAND.get());
            output.accept(WORMHOLE_POTION.get());
            output.accept(RECALL_POTION.get());
            output.accept(GUITAR.get());
            output.accept(SUPPORTER_HAT.get());
            output.accept(PLAYER_DOLL_HAT.get());
            output.accept(POXIAOJIN_DOLL_HAT.get());
            output.accept(HANHANYU_DOLL_HAT.get());
            output.accept(PLAYER_351987654321_DOLL_HAT.get());
          })
          .build());

  private Forge1201Items() {}

  public static void register(IEventBus bus) {
    ITEMS.register(bus);
    SOUND_EVENTS.register(bus);
    CREATIVE_TABS.register(bus);
  }

  static Item createRecallPotion() {
    return new Forge1201RecallPotion(new Item.Properties()
        .stacksTo(RECALL_POTION_MAX_STACK_SIZE).fireResistant());
  }

  static Item createWormholePotion() {
    return new Item(new Item.Properties().stacksTo(1).fireResistant());
  }

  private static RegistryObject<Item> registerDollHat(String id, String uuid, String name) {
    return ITEMS.register(id, () -> new Forge1201PlayerDollHatItem(
        ArmorMaterials.LEATHER, ArmorItem.Type.HELMET,
        new Item.Properties().stacksTo(1), UUID.fromString(uuid), name, false));
  }

  private static RegistryObject<SoundEvent> registerSound(String id) {
    ResourceLocation location = new ResourceLocation(EconomyConstants.MOD_ID, id);
    return SOUND_EVENTS.register(id, () -> SoundEvent.createVariableRangeEvent(location));
  }
}
