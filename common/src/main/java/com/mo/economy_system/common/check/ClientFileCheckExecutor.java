package com.mo.economy_system.common.check;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ClientFileCheckExecutor implements AutoCloseable {
  private final ThreadPoolExecutor executor =
      new ThreadPoolExecutor(
          1,
          1,
          0,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(1),
          runnable -> {
            Thread thread = new Thread(runnable, "economy-client-file-check");
            thread.setDaemon(true);
            return thread;
          },
          new ThreadPoolExecutor.AbortPolicy());
  private boolean busy;
  private Future<?> current;

  public synchronized boolean submit(Runnable task) {
    if (busy) return false;
    busy = true;
    try {
      current =
          executor.submit(
              () -> {
                try {
                  task.run();
                } finally {
                  synchronized (ClientFileCheckExecutor.this) {
                    busy = false;
                    current = null;
                  }
                }
              });
      return true;
    } catch (RuntimeException busy) {
      this.busy = false;
      current = null;
      return false;
    }
  }

  public synchronized void cancelPending() {
    if (current != null) current.cancel(true);
    executor.getQueue().clear();
  }

  @Override
  public synchronized void close() {
    cancelPending();
    executor.shutdownNow();
  }
}
