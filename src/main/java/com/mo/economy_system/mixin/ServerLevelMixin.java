package com.mo.economy_system.mixin;

import com.mo.economy_system.core.realtime_system.RealTimeManager;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * 服务器时间 Mixin
 * 当实时时间系统启用时，将游戏时间同步到现实时间（东八区）
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements WorldGenLevel {

    // 初始游戏时间，仅设置一次
    private static boolean initialized = false;

    protected ServerLevelMixin(WritableLevelData p_270739_, ResourceKey<Level> p_270683_, RegistryAccess p_270200_, Holder<DimensionType> p_270240_, Supplier<ProfilerFiller> p_270692_, boolean p_270904_, boolean p_270470_, long p_270248_, int p_270466_) {
        super(p_270739_, p_270683_, p_270200_, p_270240_, p_270692_, p_270904_, p_270470_, p_270248_, p_270466_);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void syncRealTime(CallbackInfo ci) {
        ServerLevel serverLevel = (ServerLevel) (Object) this;

        // 检查实时时间系统是否启用
        if (!RealTimeManager.isEnabled(serverLevel)) {
            return;
        }

        if (!initialized) {
            long realWorldTime = RealTimeManager.getRealWorldTimeInGameTicks();
            serverLevel.setDayTime(realWorldTime);
            initialized = true;
        }

        // 每 tick 校正时间
        long realWorldTime = RealTimeManager.getRealWorldTimeInGameTicks();
        serverLevel.setDayTime(realWorldTime);
    }
}
