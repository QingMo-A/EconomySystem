package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.target.forge1201.item.Forge1201ItemStackBridge;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

record Forge1201DemandItemResolver(Forge1201ItemStackBridge bridge, HolderLookup.Provider registries)
        implements CreateDemandOrderService.ItemResolver {
    public DemandItemResolveResult resolve(String rawId) {
        ResourceLocation id;
        try { id = new ResourceLocation(rawId); }
        catch (RuntimeException exception) { return DemandItemResolveResult.failure(DemandItemResolveResult.Error.INVALID_ITEM_ID); }
        if (!BuiltInRegistries.ITEM.containsKey(id)) return DemandItemResolveResult.failure(DemandItemResolveResult.Error.ITEM_NOT_FOUND);
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) return DemandItemResolveResult.failure(DemandItemResolveResult.Error.ITEM_NOT_FOUND);
        ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) return DemandItemResolveResult.failure(DemandItemResolveResult.Error.ITEM_NOT_FOUND);
        stack.setCount(1);
        var captured = bridge.captureSnapshot(stack, registries);
        if (!captured.isSuccess()) return DemandItemResolveResult.failure(DemandItemResolveResult.Error.SNAPSHOT_REJECTED);
        return DemandItemResolveResult.success(new ResolvedDemandItem(
                BuiltInRegistries.ITEM.getKey(item).toString(), captured.orElseThrow(), stack.getMaxStackSize()));
    }
}
