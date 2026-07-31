package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotError;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class Forge1201ItemStackBridgeTest {
    private final Forge1201ItemStackBridge bridge = new Forge1201ItemStackBridge();

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void roundTripsOrdinaryAndMultiCountStacks() {
        roundTrip(new ItemStack(Items.STONE));
        roundTrip(new ItemStack(Items.STONE, 32));
    }

    @Test
    void roundTripsAllEquivalentNativeFields() {
        ItemStack source = new ItemStack(Items.LEATHER_CHESTPLATE);
        source.setHoverName(Component.literal("Ledger Coat"));
        source.enchant(Enchantments.UNBREAKING, 3);
        source.enchant(Enchantments.THORNS, 2);
        CompoundTag tag = source.getOrCreateTag();
        CompoundTag display = tag.getCompound("display");
        ListTag lore = new ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("first"))));
        lore.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(Component.literal("second"))));
        display.put("Lore", lore);
        display.putInt("color", 0x123456);
        tag.put("display", display);
        tag.putInt("Damage", 8);
        tag.putInt("RepairCost", 3);
        tag.putBoolean("Unbreakable", true);
        tag.putInt("CustomModelData", 77);
        tag.putString("owner", "alice");
        ItemStack restored = roundTrip(source);
        assertTrue(ItemStack.isSameItemSameTags(source, restored), () -> "source=" + source.getTag() + " restored=" + restored.getTag());
    }

    @Test
    void rejectsUnsupportedNativeDataUnknownItemInvalidCountAndSplitTooltipFlags() {
        ItemStack unsupported = new ItemStack(Items.STONE);
        unsupported.getOrCreateTag().put("AttributeModifiers", new ListTag());
        assertEquals(ItemStackSnapshotError.UNSUPPORTED_COMPONENT,
                bridge.captureSnapshot(unsupported, RegistryAccess.EMPTY).error().orElseThrow());
        ItemStack potionData = new ItemStack(Items.POTION);
        potionData.getOrCreateTag().putString("Potion", "minecraft:healing");
        assertEquals(ItemStackSnapshotError.UNSUPPORTED_COMPONENT,
                bridge.captureSnapshot(potionData, RegistryAccess.EMPTY).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.UNKNOWN_ITEM_ID,
                bridge.restoreSnapshot(empty("missing_mod:not_here", 1), RegistryAccess.EMPTY).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.INVALID_COUNT,
                bridge.restoreSnapshot(empty("minecraft:stone", 65), RegistryAccess.EMPTY).error().orElseThrow());

        ItemStackSnapshot splitFlags = new ItemStackSnapshot("minecraft:enchanted_book", 1, Optional.empty(), List.of(),
                Map.of("minecraft:unbreaking", 1), Map.of("minecraft:mending", 1), true, false,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag());
        assertEquals(ItemStackSnapshotError.LOSSY_COMPONENT_CONVERSION,
                bridge.restoreSnapshot(splitFlags, RegistryAccess.EMPTY).error().orElseThrow());
    }

    private ItemStack roundTrip(ItemStack source) {
        ItemStackSnapshot snapshot = bridge.captureSnapshot(source, RegistryAccess.EMPTY).orElseThrow();
        return bridge.restoreSnapshot(snapshot, RegistryAccess.EMPTY).orElseThrow();
    }

    private static ItemStackSnapshot empty(String id, int count) {
        return new ItemStackSnapshot(id, count, Optional.empty(), List.of(), Map.of(), Map.of(), true, true,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag());
    }
}
