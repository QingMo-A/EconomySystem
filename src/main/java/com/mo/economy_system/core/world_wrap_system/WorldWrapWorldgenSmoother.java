package com.mo.economy_system.core.world_wrap_system;

public class WorldWrapWorldgenSmoother {
    private static final double MIN_BLEND_DISTANCE = 1.0D;

    public static Blend createBlend(int blockX, int blockZ) {
        WorldWrapConfig.WorldWrapConfigData config = WorldWrapConfig.getConfig();
        if (!config.isEnabled() || !config.isWorldgenSmoothingEnabled()) {
            return Blend.none(blockX, blockZ);
        }

        double blendDistance = Math.min(config.getWorldgenBlendDistance(), Math.min(config.getWidth(), config.getHeight()) / 4.0D);
        if (blendDistance < MIN_BLEND_DISTANCE) {
            return Blend.none(blockX, blockZ);
        }

        AxisBlend xBlend = createAxisBlend(blockX, config.getMinX(), config.getMaxX(), config.getWidth(), blendDistance);
        AxisBlend zBlend = createAxisBlend(blockZ, config.getMinZ(), config.getMaxZ(), config.getHeight(), blendDistance);
        return new Blend(xBlend.weight(), zBlend.weight(), xBlend.sampleCoordinate(), zBlend.sampleCoordinate());
    }

    private static AxisBlend createAxisBlend(int coordinate, double min, double max, double size, double blendDistance) {
        if (coordinate < min || coordinate >= max) {
            return new AxisBlend(1.0D, (int) Math.floor(wrapCoordinate(coordinate, min, size)));
        }

        double lowerDistance = coordinate - min;
        if (lowerDistance < blendDistance) {
            double weight = smoothstep(1.0D - lowerDistance / blendDistance);
            return new AxisBlend(weight, (int) Math.floor(coordinate + size));
        }

        double upperDistance = max - coordinate;
        if (upperDistance <= blendDistance) {
            double weight = smoothstep(1.0D - upperDistance / blendDistance);
            return new AxisBlend(weight, (int) Math.floor(coordinate - size));
        }

        return new AxisBlend(0.0D, coordinate);
    }

    private static double wrapCoordinate(double coordinate, double min, double size) {
        double wrapped = (coordinate - min) % size;
        if (wrapped < 0.0D) {
            wrapped += size;
        }
        return min + wrapped;
    }

    private static double smoothstep(double value) {
        double clamped = Math.max(0.0D, Math.min(1.0D, value));
        return clamped * clamped * (3.0D - 2.0D * clamped);
    }

    private record AxisBlend(double weight, int sampleCoordinate) {
    }

    public record Blend(double xWeight, double zWeight, int xSample, int zSample) {
        private static Blend none(int blockX, int blockZ) {
            return new Blend(0.0D, 0.0D, blockX, blockZ);
        }

        public boolean shouldBlend() {
            return xWeight > 0.0D || zWeight > 0.0D;
        }
    }
}
