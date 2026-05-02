package com.mo.economy_system.client.cinematic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class JoinCinematicController {
    private static final int INTRO_DELAY_TICKS = 10;
    private static final int HOVER_END = 20;
    private static final int CUT_1_END = 30;
    private static final int HOLD_1_END = 44;
    private static final int CUT_2_END = 54;
    private static final int HOLD_2_END = 68;
    private static final int CUT_3_END = 78;
    private static final int TOTAL_TICKS = 114;

    private static final double HEIGHT_TOP = 82.0D;
    private static final double HEIGHT_STEP_1 = 42.0D;
    private static final double HEIGHT_STEP_2 = 18.0D;
    private static final double HEIGHT_NEAR = 4.2D;

    private static boolean pendingStart;
    private static boolean active;
    private static int ageTicks;
    private static int delayTicks;

    private JoinCinematicController() {
    }

    public static void requestStart() {
        pendingStart = true;
        delayTicks = INTRO_DELAY_TICKS;
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            active = false;
            ageTicks = 0;
            return;
        }

        if (pendingStart) {
            if (delayTicks-- > 0) {
                return;
            }
            pendingStart = false;
            active = true;
            ageTicks = 0;
        }

        if (active && ++ageTicks >= TOTAL_TICKS) {
            active = false;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isInputBlocked() {
        return active;
    }

    public static float getBlackOverlayAlpha(float partialTick) {
        if (!active) {
            return 0.0F;
        }

        double time = (double) ageTicks + partialTick;
        return (float) Math.max(cutAlpha(time, HOVER_END, CUT_1_END),
                Math.max(cutAlpha(time, HOLD_1_END, CUT_2_END), cutAlpha(time, HOLD_2_END, CUT_3_END)));
    }

    public static CameraFrame getCameraFrame(LocalPlayer player, float partialTick) {
        double time = Mth.clamp((double) ageTicks + partialTick, 0.0D, TOTAL_TICKS);
        Vec3 eye = player.getEyePosition(partialTick);
        float targetYaw = player.getViewYRot(partialTick);
        float targetPitch = player.getViewXRot(partialTick);

        if (time < CUT_1_END) {
            double t = easeOut((time - HOVER_END) / (double) (CUT_1_END - HOVER_END));
            return topDownFrame(eye, Mth.lerp(t, HEIGHT_TOP, HEIGHT_STEP_1), targetYaw);
        }

        if (time < HOLD_1_END) {
            return topDownFrame(eye, HEIGHT_STEP_1, targetYaw);
        }

        if (time < CUT_2_END) {
            double t = easeOut((time - HOLD_1_END) / (double) (CUT_2_END - HOLD_1_END));
            return topDownFrame(eye, Mth.lerp(t, HEIGHT_STEP_1, HEIGHT_STEP_2), targetYaw);
        }

        if (time < HOLD_2_END) {
            return topDownFrame(eye, HEIGHT_STEP_2, targetYaw);
        }

        if (time < CUT_3_END) {
            double t = easeOut((time - HOLD_2_END) / (double) (CUT_3_END - HOLD_2_END));
            return topDownFrame(eye, Mth.lerp(t, HEIGHT_STEP_2, HEIGHT_NEAR), targetYaw);
        }

        double t = smooth((time - CUT_3_END) / (double) (TOTAL_TICKS - CUT_3_END));
        Vec3 start = eye.add(0.0D, HEIGHT_NEAR, 0.0D);
        Vec3 position = lerp(start, eye, t);
        float yaw = targetYaw;
        float pitch = Mth.lerp((float) t, 89.0F, targetPitch);
        return new CameraFrame(position, yaw, pitch);
    }

    private static CameraFrame topDownFrame(Vec3 eye, double height, float yaw) {
        return new CameraFrame(eye.add(0.0D, height, 0.0D), yaw, 89.0F);
    }

    private static Vec3 lerp(Vec3 from, Vec3 to, double delta) {
        return new Vec3(
                Mth.lerp(delta, from.x, to.x),
                Mth.lerp(delta, from.y, to.y),
                Mth.lerp(delta, from.z, to.z)
        );
    }

    private static double cutAlpha(double time, double start, double end) {
        if (time < start || time > end) {
            return 0.0D;
        }
        double fadeTicks = 1.2D;
        return Math.min(Mth.clamp((time - start) / fadeTicks, 0.0D, 1.0D),
                Mth.clamp((end - time) / fadeTicks, 0.0D, 1.0D));
    }

    private static double easeOut(double value) {
        double t = Mth.clamp(value, 0.0D, 1.0D);
        double inverse = 1.0D - t;
        return 1.0D - inverse * inverse * inverse;
    }

    private static double smooth(double value) {
        double t = Mth.clamp(value, 0.0D, 1.0D);
        return t * t * (3.0D - 2.0D * t);
    }

    private static float rotLerp(float from, float to, float delta) {
        return from + Mth.wrapDegrees(to - from) * delta;
    }

    public record CameraFrame(Vec3 position, float yaw, float pitch) {
    }
}
