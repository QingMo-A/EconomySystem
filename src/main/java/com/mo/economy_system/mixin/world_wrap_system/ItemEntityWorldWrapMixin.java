package com.mo.economy_system.mixin.world_wrap_system;

import com.mo.economy_system.core.world_wrap_system.WorldWrapItemEntityManager;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityWorldWrapMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void economySystem$wrapDroppedItem(CallbackInfo ci) {
        WorldWrapItemEntityManager.wrapIfNeeded((ItemEntity) (Object) this);
    }
}
