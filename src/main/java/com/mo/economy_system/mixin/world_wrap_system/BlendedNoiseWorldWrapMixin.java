package com.mo.economy_system.mixin.world_wrap_system;

import com.mo.economy_system.core.world_wrap_system.WorldWrapWorldgenSmoother;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlendedNoise.class)
public abstract class BlendedNoiseWorldWrapMixin {
    @Shadow
    @Final
    private PerlinNoise minLimitNoise;

    @Shadow
    @Final
    private PerlinNoise maxLimitNoise;

    @Shadow
    @Final
    private PerlinNoise mainNoise;

    @Shadow
    @Final
    private double xzMultiplier;

    @Shadow
    @Final
    private double yMultiplier;

    @Shadow
    @Final
    private double xzFactor;

    @Shadow
    @Final
    private double yFactor;

    @Shadow
    @Final
    private double smearScaleMultiplier;

    @Inject(method = "compute", at = @At("HEAD"), cancellable = true)
    private void economySystem$blendWrappedBoundaryNoise(DensityFunction.FunctionContext context,
                                                         CallbackInfoReturnable<Double> cir) {
        WorldWrapWorldgenSmoother.Blend blend = WorldWrapWorldgenSmoother.createBlend(context.blockX(), context.blockZ());
        if (!blend.shouldBlend()) {
            return;
        }

        int blockX = context.blockX();
        int blockY = context.blockY();
        int blockZ = context.blockZ();
        double base = economySystem$computeAt(blockX, blockY, blockZ);
        double xMixed = base;
        double zMixed = base;
        double xzMixed = base;

        if (blend.xWeight() > 0.0D) {
            xMixed = Mth.lerp(blend.xWeight(), base, economySystem$computeAt(blend.xSample(), blockY, blockZ));
        }

        if (blend.zWeight() > 0.0D) {
            zMixed = Mth.lerp(blend.zWeight(), base, economySystem$computeAt(blockX, blockY, blend.zSample()));
        }

        if (blend.xWeight() > 0.0D && blend.zWeight() > 0.0D) {
            double wrappedXBase = economySystem$computeAt(blend.xSample(), blockY, blockZ);
            double wrappedZBase = economySystem$computeAt(blockX, blockY, blend.zSample());
            double wrappedBoth = economySystem$computeAt(blend.xSample(), blockY, blend.zSample());
            double lower = Mth.lerp(blend.xWeight(), base, wrappedXBase);
            double upper = Mth.lerp(blend.xWeight(), wrappedZBase, wrappedBoth);
            xzMixed = Mth.lerp(blend.zWeight(), lower, upper);
        }

        if (blend.xWeight() > 0.0D && blend.zWeight() > 0.0D) {
            cir.setReturnValue(xzMixed);
        } else if (blend.xWeight() > 0.0D) {
            cir.setReturnValue(xMixed);
        } else {
            cir.setReturnValue(zMixed);
        }
    }

    @Unique
    private double economySystem$computeAt(int blockX, int blockY, int blockZ) {
        double scaledX = (double) blockX * this.xzMultiplier;
        double scaledY = (double) blockY * this.yMultiplier;
        double scaledZ = (double) blockZ * this.xzMultiplier;
        double mainX = scaledX / this.xzFactor;
        double mainY = scaledY / this.yFactor;
        double mainZ = scaledZ / this.xzFactor;
        double smearY = this.yMultiplier * this.smearScaleMultiplier;
        double scaledSmearY = smearY / this.yFactor;
        double minValue = 0.0D;
        double maxValue = 0.0D;
        double mainValue = 0.0D;
        double frequency = 1.0D;

        for (int i = 0; i < 8; ++i) {
            ImprovedNoise improvedNoise = this.mainNoise.getOctaveNoise(i);
            if (improvedNoise != null) {
                mainValue += improvedNoise.noise(
                        PerlinNoise.wrap(mainX * frequency),
                        PerlinNoise.wrap(mainY * frequency),
                        PerlinNoise.wrap(mainZ * frequency),
                        scaledSmearY * frequency,
                        mainY * frequency
                ) / frequency;
            }

            frequency /= 2.0D;
        }

        double blend = (mainValue / 10.0D + 1.0D) / 2.0D;
        boolean useOnlyMax = blend >= 1.0D;
        boolean useOnlyMin = blend <= 0.0D;
        frequency = 1.0D;

        for (int j = 0; j < 16; ++j) {
            double octaveX = PerlinNoise.wrap(scaledX * frequency);
            double octaveY = PerlinNoise.wrap(scaledY * frequency);
            double octaveZ = PerlinNoise.wrap(scaledZ * frequency);
            double octaveSmearY = smearY * frequency;
            if (!useOnlyMax) {
                ImprovedNoise minNoise = this.minLimitNoise.getOctaveNoise(j);
                if (minNoise != null) {
                    minValue += minNoise.noise(octaveX, octaveY, octaveZ, octaveSmearY, scaledY * frequency) / frequency;
                }
            }

            if (!useOnlyMin) {
                ImprovedNoise maxNoise = this.maxLimitNoise.getOctaveNoise(j);
                if (maxNoise != null) {
                    maxValue += maxNoise.noise(octaveX, octaveY, octaveZ, octaveSmearY, scaledY * frequency) / frequency;
                }
            }

            frequency /= 2.0D;
        }

        return Mth.clampedLerp(minValue / 512.0D, maxValue / 512.0D, blend) / 128.0D;
    }
}
