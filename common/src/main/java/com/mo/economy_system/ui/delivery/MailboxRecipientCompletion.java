package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.network.PlayerSummary;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral filtering and selection state for mailbox recipient completion. */
public final class MailboxRecipientCompletion {
  private int selection;

  public List<PlayerSummary> suggestions(List<PlayerSummary> players, String query, UUID selfId) {
    String needle = Objects.requireNonNullElse(query, "").trim().toLowerCase(Locale.ROOT);
    if (needle.isEmpty()) return List.of();
    List<PlayerSummary> result = players.stream()
        .filter(player -> selfId == null || !player.playerId().equals(selfId))
        .filter(player -> {
          String name = player.playerName().toLowerCase(Locale.ROOT);
          String id = player.playerId().toString().toLowerCase(Locale.ROOT);
          return name.contains(needle) || id.startsWith(needle);
        })
        .sorted((left, right) -> {
          boolean leftPrefix = left.playerName().toLowerCase(Locale.ROOT).startsWith(needle);
          boolean rightPrefix = right.playerName().toLowerCase(Locale.ROOT).startsWith(needle);
          if (leftPrefix != rightPrefix) return leftPrefix ? -1 : 1;
          return left.playerName().compareToIgnoreCase(right.playerName());
        })
        .toList();
    clamp(result.size());
    return result;
  }

  public int selection() { return selection; }

  public void reset() { selection = 0; }

  public void move(int delta, int total) {
    if (total <= 0) { selection = 0; return; }
    selection = Math.floorMod(selection + Integer.signum(delta), total);
  }

  public void select(int index, int total) {
    if (total <= 0) { selection = 0; return; }
    selection = Math.max(0, Math.min(index, total - 1));
  }

  private void clamp(int total) {
    if (total <= 0) selection = 0;
    else selection = Math.max(0, Math.min(selection, total - 1));
  }
}
