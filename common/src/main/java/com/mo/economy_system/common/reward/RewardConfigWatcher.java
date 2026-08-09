package com.mo.economy_system.common.reward;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Closeable daemon watcher for one reward config file. */
public final class RewardConfigWatcher implements AutoCloseable {
  private final Path file;
  private final Runnable reload;
  private final Consumer<Throwable> failures;
  private final AtomicBoolean closed = new AtomicBoolean();
  private WatchService watchService;
  private Thread thread;

  public RewardConfigWatcher(Path file, Runnable reload, Consumer<Throwable> failures) {
    this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    this.reload = Objects.requireNonNull(reload, "reload");
    this.failures = Objects.requireNonNull(failures, "failures");
  }

  public synchronized void start() throws IOException {
    if (thread != null) return;
    if (closed.get()) throw new IllegalStateException("watcher is closed");
    Path parent = Objects.requireNonNull(file.getParent(), "config parent");
    Files.createDirectories(parent);
    watchService = FileSystems.getDefault().newWatchService();
    parent.register(
        watchService,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_MODIFY,
        StandardWatchEventKinds.ENTRY_DELETE);
    thread = new Thread(this::run, "economy-system-reward-config");
    thread.setDaemon(true);
    thread.start();
  }

  private void run() {
    try {
      while (!closed.get()) {
        WatchKey key = watchService.take();
        boolean relevant = false;
        for (WatchEvent<?> event : key.pollEvents()) {
          if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
            relevant = true;
          } else if (event.context() instanceof Path changed
              && changed.equals(file.getFileName())) {
            relevant = true;
          }
        }
        if (!key.reset()) {
          report(new IOException("reward config directory is no longer watchable"));
          return;
        }
        if (relevant && !closed.get()) {
          try {
            reload.run();
          } catch (RuntimeException error) {
            report(error);
          }
        }
      }
    } catch (ClosedWatchServiceException ignored) {
      // Normal close path.
    } catch (InterruptedException error) {
      if (!closed.get()) {
        Thread.currentThread().interrupt();
        report(error);
      }
    } catch (RuntimeException error) {
      report(error);
    }
  }

  private void report(Throwable error) {
    try {
      failures.accept(error);
    } catch (RuntimeException ignored) {
      // Diagnostics cannot keep the watcher alive or change config state.
    }
  }

  @Override
  public synchronized void close() {
    if (!closed.compareAndSet(false, true)) return;
    if (watchService != null) {
      try {
        watchService.close();
      } catch (IOException error) {
        report(error);
      }
    }
    if (thread != null) thread.interrupt();
  }
}
