package net.neoforged.fml;

import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class DistExecutor {
    private DistExecutor() {
    }

    public interface SafeRunnable extends Runnable {
    }

    public static void safeRunWhenOn(Dist dist, Supplier<? extends SafeRunnable> runnable) {
        if (FMLEnvironment.dist == dist) {
            runnable.get().run();
        }
    }

    public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> runnable) {
        if (FMLEnvironment.dist == dist) {
            runnable.get().run();
        }
    }
}
