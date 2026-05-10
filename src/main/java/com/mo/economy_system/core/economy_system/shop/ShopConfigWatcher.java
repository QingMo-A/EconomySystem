package com.mo.economy_system.core.economy_system.shop;

import com.mo.economy_system.EconomySystem;

import java.io.IOException;
import java.nio.file.*;

public class ShopConfigWatcher {
    private final ShopManager shopManager;
    private volatile long lastReloadTime = 0L;

    public ShopConfigWatcher(ShopManager shopManager) {
        this.shopManager = shopManager;
    }

    public void watchConfigFile() {
        Thread watcherThread = new Thread(() -> {
            try {
                Path configPath = ShopManager.CONFIG_FILE.toPath().getParent();
                WatchService watchService = FileSystems.getDefault().newWatchService();
                configPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() != StandardWatchEventKinds.ENTRY_MODIFY) {
                            continue;
                        }
                        if (event.context().toString().equals(ShopManager.CONFIG_FILE.getName())) {
                            long now = System.currentTimeMillis();
                            if (now - lastReloadTime < 500L) {
                                continue;
                            }
                            lastReloadTime = now;
                            Thread.sleep(400L);
                            EconomySystem.LOGGER.info("Shop config file updated. Reloading...");
                            shopManager.loadFromConfig();
                        }
                    }
                    if (!key.reset()) {
                        break;
                    }
                }
            } catch (IOException | InterruptedException e) {
                EconomySystem.LOGGER.error("Shop config watcher stopped", e);
                Thread.currentThread().interrupt();
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.start();
    }
}
