package com.mo.economy_system.common.reward;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Owns the live reward catalog and its closeable hot-reload lifecycle. */
public final class RewardConfiguration implements AutoCloseable {
  @FunctionalInterface
  public interface Diagnostics {
    void warning(String operation, String detail, Throwable error);
  }

  private final RewardConfigFile file;
  private final RewardCatalog catalog = new RewardCatalog(RewardDefaults.entries());
  private final Diagnostics diagnostics;
  private RewardConfigWatcher watcher;
  private boolean started;

  public RewardConfiguration(Path file, Diagnostics diagnostics) {
    this.file = new RewardConfigFile(file);
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
  }

  public RewardConfiguration(Path file) {
    this(file, (operation, detail, error) -> {});
  }

  public RewardCatalog catalog() {
    return catalog;
  }

  public synchronized void start() {
    if (started) return;
    reload("initial-load");
    watcher =
        new RewardConfigWatcher(
            file.path(),
            () -> reload("hot-reload"),
            error -> warn("watch", "reward config watcher failed", error));
    try {
      watcher.start();
    } catch (IOException | RuntimeException error) {
      warn("watch-start", "reward config will not hot reload", error);
      watcher.close();
      watcher = null;
    }
    started = true;
  }

  public synchronized void reload() {
    reload("manual-reload");
  }

  private void reload(String operation) {
    RewardConfigFile.LoadResult result = file.loadOrCreate(RewardDefaults.entries());
    for (String issue : result.issues()) warn(operation, issue, result.error());
    if (!result.usable()) return;
    try {
      catalog.replace(result.entries());
    } catch (RuntimeException error) {
      warn(operation, "validated reward table could not be installed", error);
    }
  }

  private void warn(String operation, String detail, Throwable error) {
    try {
      diagnostics.warning(operation, detail, error);
    } catch (RuntimeException ignored) {
      // Diagnostics must never alter the active reward table.
    }
  }

  @Override
  public synchronized void close() {
    if (watcher != null) watcher.close();
    watcher = null;
    started = false;
  }
}
