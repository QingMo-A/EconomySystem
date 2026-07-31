package com.mo.economy_system.platform.item;

import net.minecraft.nbt.CompoundTag;
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

    /**
     * Existing compact storage schema used by market and delivery data.
     *
     * <p>This preserves only item id, count, and the target's native custom
     * data. It is not a complete cross-version ItemStack snapshot: damage,
     * enchantments, display metadata, and other 1.21 data components require
     * an explicit component-level bridge before those features move to
     * Forge 1.20.1.</p>
     */
    CompoundTag saveSimple(ItemStack stack);

    /** Reads the compact storage schema produced by {@link #saveSimple}. */
    ItemStack loadSimple(CompoundTag tag);
}
