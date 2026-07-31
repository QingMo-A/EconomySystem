package com.mo.economy_system.platform.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

/** Cross-version ItemStack operations whose implementations changed in 1.21. */
public interface EconomyItemStackBridge {
    boolean hasCustomData(ItemStack stack);

    /** Returns a defensive copy, or {@code null} when no custom data exists. */
    CompoundTag copyCustomData(ItemStack stack);

    /** Replaces custom data; {@code null} or an empty tag removes it. */
    void setCustomData(ItemStack stack, CompoundTag tag);

    /** Compares item identity and all version-specific stack data/components. */
    boolean sameItemAndData(ItemStack first, ItemStack second);

    /** Captures every supported component or fails before any data is discarded. */
    ItemStackSnapshotResult<ItemStackSnapshot> captureSnapshot(ItemStack stack, HolderLookup.Provider registries);

    /** Restores a snapshot or returns an explicit target/version compatibility error. */
    ItemStackSnapshotResult<ItemStack> restoreSnapshot(ItemStackSnapshot snapshot, HolderLookup.Provider registries);

    /**
     * Existing compact storage schema used by market and delivery data.
     *
     * <p>This preserves only item id, count, and the target's native custom
     * data. It is not a complete cross-version ItemStack snapshot: damage,
     * enchantments, display metadata, and other 1.21 data components require
     * an explicit component-level bridge before those features move to
     * Forge 1.20.1.</p>
     */
    @Deprecated(forRemoval = false)
    CompoundTag saveSimple(ItemStack stack);

    /** Reads the compact storage schema produced by {@link #saveSimple}. */
    @Deprecated(forRemoval = false)
    ItemStack loadSimple(CompoundTag tag);
}
