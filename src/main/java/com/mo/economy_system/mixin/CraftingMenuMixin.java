package com.mo.economy_system.mixin;

import com.mo.economy_system.core.blueprint_system.PlayerBlueprintData;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

    /**
     * @author
     * @reason
     *//*
    @Overwrite
    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftingContainer, ResultContainer resultContainer) {
        if (!level.isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            ItemStack itemStack = ItemStack.EMPTY;
            Optional<CraftingRecipe> recipeFor = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);
            if (recipeFor.isPresent()) {
                CraftingRecipe recipe = (CraftingRecipe)recipeFor.get();
                // 如果配方一致
                if (resultContainer.setRecipeUsed(level, serverPlayer, recipe)) {
                    ItemStack assemble = recipe.assemble(craftingContainer, level.registryAccess());
                    // 如果该物品在此世界被启用
                    if (assemble.isItemEnabled(level.enabledFeatures())) {
                        if (PlayerBlueprintData.canCraftItem(player, assemble)) {
                            itemStack = assemble;
                        }

                    }
                }
            }

            // 设置结果
            resultContainer.setItem(0, itemStack);
            menu.setRemoteSlot(0, itemStack);
            // 给客户端发包
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(menu.containerId, menu.incrementStateId(), 0, itemStack));
        }
    }*/

    @Inject(
            method = "slotChangedCraftingGrid",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void onSlotChangedCraftingGrid(
            AbstractContainerMenu menu,
            Level level,
            Player player,
            CraftingContainer craftingContainer,
            ResultContainer resultContainer,
            CallbackInfo ci
    ) {
        if (level.isClientSide) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack result = resultContainer.getItem(0);
        if (result.isEmpty()) return;

        // ⭐ 蓝图权限判断
        if (!PlayerBlueprintData.canCraftItem(player, result)) {
            ItemStack empty = ItemStack.EMPTY;

            resultContainer.setItem(0, empty);
            menu.setRemoteSlot(0, empty);

            serverPlayer.connection.send(
                    new ClientboundContainerSetSlotPacket(
                            menu.containerId,
                            menu.incrementStateId(),
                            0,
                            empty
                    )
            );
        }
    }
}
