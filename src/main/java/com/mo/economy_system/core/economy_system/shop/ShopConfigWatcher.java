package com.mo.economy_system.core.economy_system.shop;

import java.io.IOException;
import java.nio.file.*;

public class ShopConfigWatcher {
    private final ShopManager shopManager;

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
                        if (event.context().toString().equals(ShopManager.CONFIG_FILE.getName())) {
                            System.out.println("Config file updated. Reloading...");
                            shopManager.loadFromConfig();
                        }
                    }
                    key.reset();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        watcherThread.setDaemon(true);
        watcherThread.start();
    }
}
