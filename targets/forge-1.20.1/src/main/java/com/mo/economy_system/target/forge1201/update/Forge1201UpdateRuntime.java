package com.mo.economy_system.target.forge1201.update;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.update.UpdateCheckResult;
import com.mo.economy_system.common.update.UpdateFeedback;
import com.mo.economy_system.common.update.UpdateRelease;
import com.mo.economy_system.common.update.UpdateReleaseJsonCodec;
import com.mo.economy_system.platform.EconomyServices;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/** Forge HTTP/thread/player adapter for the common update policy. */
public final class Forge1201UpdateRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final URI RELEASES_URI = URI.create(
      "https://api.github.com/repos/QingMo-A/EconomySystem/releases/latest");
  private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
  private static final int READ_TIMEOUT_MILLIS = 5_000;
  private static final Map<MinecraftServer, ServerState> STATES =
      Collections.synchronizedMap(new IdentityHashMap<>());

  private Forge1201UpdateRuntime() {}

  public static void start(MinecraftServer server) { state(server); }

  public static void checkForUpdates(MinecraftServer server, UUID playerId) {
    ServerState state = state(server);
    if (!state.pending.add(playerId)) return;
    try {
      state.executor.execute(() -> fetchAndDispatch(state, playerId));
    } catch (RejectedExecutionException error) {
      state.pending.remove(playerId);
      LOGGER.debug("update check rejected after server shutdown", error);
    }
  }

  public static void shutdown(MinecraftServer server) {
    ServerState state;
    synchronized (STATES) {
      state = STATES.remove(server);
    }
    if (state != null) state.executor.shutdownNow();
  }

  private static void fetchAndDispatch(ServerState state, UUID playerId) {
    UpdateCheckResult result;
    try {
      UpdateRelease release = fetchRelease();
      result = UpdateCheckResult.evaluate(
          EconomyServices.platform().modVersion(EconomyConstants.MOD_ID), release);
    } catch (Exception error) {
      LOGGER.debug("could not check EconomySystem updates", error);
      dispatchFailure(state.server, playerId);
      return;
    } finally {
      state.pending.remove(playerId);
    }
    dispatchResult(state.server, playerId, result);
  }

  private static UpdateRelease fetchRelease() throws IOException {
    HttpURLConnection connection = (HttpURLConnection) RELEASES_URI.toURL().openConnection();
    try {
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
      connection.setReadTimeout(READ_TIMEOUT_MILLIS);
      connection.setUseCaches(false);
      connection.setRequestProperty("Accept", "application/vnd.github+json");
      connection.setRequestProperty("User-Agent", "EconomySystem-UpdateChecker");
      int response = connection.getResponseCode();
      if (response != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + response);
      try (InputStream input = connection.getInputStream();
          InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
        StringBuilder payload = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
          payload.append(buffer, 0, read);
          if (payload.length() > 256_000) throw new IOException("release response too large");
        }
        return UpdateReleaseJsonCodec.decode(payload.toString());
      }
    } finally {
      connection.disconnect();
    }
  }

  private static void dispatchFailure(MinecraftServer server, UUID playerId) {
    dispatch(server, playerId, null, UpdateFeedback.UNAVAILABLE);
  }

  private static void dispatchResult(MinecraftServer server, UUID playerId, UpdateCheckResult result) {
    dispatch(server, playerId, result, null);
  }

  private static void dispatch(
      MinecraftServer server, UUID playerId, UpdateCheckResult result, String failureKey) {
    try {
      server.execute(() -> {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return;
        if (failureKey != null) {
          player.sendSystemMessage(Component.translatable(failureKey));
          return;
        }
        switch (result.status()) {
          case UPDATE_AVAILABLE -> {
            Component link = Component.translatable("message.update.copy_link")
                .withStyle(style -> style
                    .withColor(0x55FF55)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,
                        result.downloadUrl()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("message.update.copy_link_hover"))));
            player.sendSystemMessage(Component.translatable(
                UpdateFeedback.AVAILABLE, result.latest().toString()).append(" ").append(link));
          }
          case CURRENT -> player.sendSystemMessage(Component.translatable(UpdateFeedback.CURRENT));
          case INVALID_CURRENT_VERSION, INVALID_LATEST_VERSION ->
              player.sendSystemMessage(Component.translatable(UpdateFeedback.INVALID_RESPONSE));
        }
      });
    } catch (RejectedExecutionException ignored) {
      // Server stopped before the result could be delivered.
    }
  }

  private static ServerState state(MinecraftServer server) {
    Objects.requireNonNull(server, "server");
    synchronized (STATES) {
      return STATES.computeIfAbsent(server, ServerState::new);
    }
  }

  private static final class ServerState {
    private final MinecraftServer server;
    private final ExecutorService executor;
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    private ServerState(MinecraftServer server) {
      this.server = server;
      this.executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "economy-system-update-check");
        thread.setDaemon(true);
        return thread;
      });
    }
  }
}
