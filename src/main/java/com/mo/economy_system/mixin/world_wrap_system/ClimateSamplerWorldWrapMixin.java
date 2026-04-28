package com.mo.economy_system.mixin.world_wrap_system;

import com.mo.economy_system.core.world_wrap_system.WorldWrapWorldgenSmoother;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Climate.Sampler.class)
public abstract class ClimateSamplerWorldWrapMixin {
    @Inject(method = "sample", at = @At("HEAD"), cancellable = true)
    private void economySystem$blendWrappedBoundaryBiomeClimate(int quartX, int quartY, int quartZ,
                                                                CallbackInfoReturnable<Climate.TargetPoint> cir) {
        int blockX = QuartPos.toBlock(quartX);
        int blockY = QuartPos.toBlock(quartY);
        int blockZ = QuartPos.toBlock(quartZ);
        WorldWrapWorldgenSmoother.Blend blend = WorldWrapWorldgenSmoother.createBlend(blockX, blockZ);
        if (!blend.shouldBlend()) {
            return;
        }

        ClimateValues base = economySystem$sampleClimateAt(blockX, blockY, blockZ);
        ClimateValues mixed = base;

        if (blend.xWeight() > 0.0D && blend.zWeight() > 0.0D) {
            ClimateValues wrappedX = economySystem$sampleClimateAt(blend.xSample(), blockY, blockZ);
            ClimateValues wrappedZ = economySystem$sampleClimateAt(blockX, blockY, blend.zSample());
            ClimateValues wrappedBoth = economySystem$sampleClimateAt(blend.xSample(), blockY, blend.zSample());
            ClimateValues lower = economySystem$lerpClimate(blend.xWeight(), base, wrappedX);
            ClimateValues upper = economySystem$lerpClimate(blend.xWeight(), wrappedZ, wrappedBoth);
            mixed = economySystem$lerpClimate(blend.zWeight(), lower, upper);
        } else if (blend.xWeight() > 0.0D) {
            mixed = economySystem$lerpClimate(
                    blend.xWeight(),
                    base,
                    economySystem$sampleClimateAt(blend.xSample(), blockY, blockZ)
            );
        } else if (blend.zWeight() > 0.0D) {
            mixed = economySystem$lerpClimate(
                    blend.zWeight(),
                    base,
                    economySystem$sampleClimateAt(blockX, blockY, blend.zSample())
            );
        }

        cir.setReturnValue(Climate.target(
                (float) mixed.temperature(),
                (float) mixed.humidity(),
                (float) mixed.continentalness(),
                (float) mixed.erosion(),
                (float) mixed.depth(),
                (float) mixed.weirdness()
        ));
    }

    @Unique
    private ClimateValues economySystem$sampleClimateAt(int blockX, int blockY, int blockZ) {
        Climate.Sampler sampler = (Climate.Sampler) (Object) this;
        DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        return new ClimateValues(
                sampler.temperature().compute(context),
                sampler.humidity().compute(context),
                sampler.continentalness().compute(context),
                sampler.erosion().compute(context),
                sampler.depth().compute(context),
                sampler.weirdness().compute(context)
        );
    }

    @Unique
    private static ClimateValues economySystem$lerpClimate(double delta, ClimateValues first, ClimateValues second) {
        return new ClimateValues(
                Mth.lerp(delta, first.temperature(), second.temperature()),
                Mth.lerp(delta, first.humidity(), second.humidity()),
                Mth.lerp(delta, first.continentalness(), second.continentalness()),
                Mth.lerp(delta, first.erosion(), second.erosion()),
                Mth.lerp(delta, first.depth(), second.depth()),
                Mth.lerp(delta, first.weirdness(), second.weirdness())
        );
    }

    @Unique
    private record ClimateValues(double temperature,
                                 double humidity,
                                 double continentalness,
                                 double erosion,
                                 double depth,
                                 double weirdness) {
    }
}
