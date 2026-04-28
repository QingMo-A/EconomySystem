package com.mo.economy_system.core.economy_system.reward;

import java.io.IOException;
import java.nio.file.*;

public class RewardConfigWatcher {
    private final RewardManager rewardManager;

    public RewardConfigWatcher(RewardManager rewardManager) {
        this.rewardManager = rewardManager;
    }

    public void watchConfigFile() {
        Thread watcherThread = new Thread(() -> {
            try {
                Path configPath = RewardManager.CONFIG_FILE.toPath().getParent();
                WatchService watchService = FileSystems.getDefault().newWatchService();
                configPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                while (true) {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context().toString().equals(RewardManager.CONFIG_FILE.getName())) {
                            System.out.println("Reward config file updated. Reloading...");
                            rewardManager.loadFromConfig();
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
