package com.mo.economy_system.target.neoforge1211.item;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshotError;
import com.mo.economy_system.platform.item.ItemStackSnapshotResult;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class NeoForge1211ItemStackBridgeTest {
    private final NeoForge1211ItemStackBridge bridge = new NeoForge1211ItemStackBridge();
    private static HolderLookup.Provider registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        Bootstrap.bootStrap();
        registries = VanillaRegistries.createLookup();
    }

    @Test
    void roundTripsOrdinaryAndMultiCountStacks() {
        roundTrip(new ItemStack(Items.STONE));
        roundTrip(new ItemStack(Items.STONE, 32));
    }

    @Test
    void roundTripsSupportedDataComponentsTogether() {
        ItemStack source = new ItemStack(Items.LEATHER_CHESTPLATE);
        source.set(DataComponents.CUSTOM_NAME, Component.literal("Ledger Coat"));
        source.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("first"), Component.literal("second"))));
        source.set(DataComponents.DAMAGE, 8);
        source.set(DataComponents.REPAIR_COST, 3);
        source.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        source.set(DataComponents.DYED_COLOR, new DyedItemColor(0x123456, false));
        source.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(77));
        CompoundTag custom = new CompoundTag();
        custom.putString("owner", "alice");
        source.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        ItemStack restored = roundTrip(source);
        assertTrue(ItemStack.isSameItemSameComponents(source, restored));
    }

    @Test
    void roundTripsMultipleEnchantmentsAndLevels() {
        ItemStackSnapshot source = new ItemStackSnapshot("minecraft:diamond_sword", 1, Optional.empty(), List.of(),
                Map.of("minecraft:sharpness", 5, "minecraft:unbreaking", 3), Map.of(), false, true,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag());
        ItemStack stack = bridge.restoreSnapshot(source, registries).orElseThrow();
        ItemStackSnapshot captured = bridge.captureSnapshot(stack, registries).orElseThrow();
        assertEquals(source.enchantments(), captured.enchantments());
        assertFalse(captured.enchantmentsShown());
    }

    @Test
    void rejectsUnsupportedPatchUnknownItemAndInvalidCount() {
        ItemStack unsupported = new ItemStack(Items.STONE);
        unsupported.set(DataComponents.MAX_STACK_SIZE, 8);
        ItemStackSnapshotResult<ItemStackSnapshot> captured = bridge.captureSnapshot(unsupported, RegistryAccess.EMPTY);
        assertEquals(ItemStackSnapshotError.UNSUPPORTED_COMPONENT, captured.error().orElseThrow());

        ItemStackSnapshot missing = empty("missing_mod:not_here", 1);
        assertEquals(ItemStackSnapshotError.UNKNOWN_ITEM_ID,
                bridge.restoreSnapshot(missing, RegistryAccess.EMPTY).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.INVALID_COUNT,
                bridge.restoreSnapshot(empty("minecraft:stone", 65), RegistryAccess.EMPTY).error().orElseThrow());
    }

    private ItemStack roundTrip(ItemStack source) {
        ItemStackSnapshot snapshot = bridge.captureSnapshot(source, registries).orElseThrow();
        return bridge.restoreSnapshot(snapshot, registries).orElseThrow();
    }

    private static ItemStackSnapshot empty(String id, int count) {
        return new ItemStackSnapshot(id, count, Optional.empty(), List.of(), Map.of(), Map.of(), true, true,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag());
    }
}
