package com.mo.economy_system.client.world_wrap_system;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientWorldWrapData {
    private static boolean enabled;
    private static String dimension = "minecraft:overworld";
    private static double centerX;
    private static double centerZ;
    private static double width = 8000.0D;
    private static double height = 8000.0D;
    private static int cooldownTicks = 20;
    private static double boundaryWarningDistance = 50.0D;

    public static void update(boolean enabled, String dimension, double centerX, double centerZ,
                              double width, double height, int cooldownTicks, double boundaryWarningDistance) {
        ClientWorldWrapData.enabled = enabled;
        ClientWorldWrapData.dimension = dimension == null || dimension.isEmpty() ? "minecraft:overworld" : dimension;
        ClientWorldWrapData.centerX = centerX;
        ClientWorldWrapData.centerZ = centerZ;
        ClientWorldWrapData.width = Math.max(16.0D, width);
        ClientWorldWrapData.height = Math.max(16.0D, height);
        ClientWorldWrapData.cooldownTicks = Math.max(1, cooldownTicks);
        ClientWorldWrapData.boundaryWarningDistance = Math.max(0.0D, boundaryWarningDistance);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String getDimension() {
        return dimension;
    }

    public static double getWidth() {
        return width;
    }

    public static double getHeight() {
        return height;
    }

    public static double getMinX() {
        return centerX - width / 2.0D;
    }

    public static double getMaxX() {
        return centerX + width / 2.0D;
    }

    public static double getMinZ() {
        return centerZ - height / 2.0D;
    }

    public static double getMaxZ() {
        return centerZ + height / 2.0D;
    }

    public static int getCooldownTicks() {
        return cooldownTicks;
    }

    public static double getBoundaryWarningDistance() {
        return boundaryWarningDistance;
    }
}
