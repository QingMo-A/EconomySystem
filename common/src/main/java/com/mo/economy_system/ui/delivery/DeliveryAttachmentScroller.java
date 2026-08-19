package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.ui.geometry.UiRect;
import java.util.Objects;
import java.util.UUID;

/** Shared horizontal attachment-strip interaction state used by both loader screens. */
public final class DeliveryAttachmentScroller {
  private UUID mailId;
  private int firstIndex;
  private boolean dragging;
  private int dragOffset;

  public int firstIndex() {
    return firstIndex;
  }

  public boolean dragging() {
    return dragging;
  }

  public void syncMail(UUID selectedMailId) {
    if (Objects.equals(mailId, selectedMailId)) return;
    mailId = selectedMailId;
    firstIndex = 0;
    dragging = false;
    dragOffset = 0;
  }

  public boolean clamp(int total, int capacity) {
    int max = maxFirstIndex(total, capacity);
    int next = Math.max(0, Math.min(firstIndex, max));
    if (next == firstIndex) return false;
    firstIndex = next;
    return true;
  }

  public boolean scroll(int direction, int total, int capacity) {
    int max = maxFirstIndex(total, capacity);
    if (max <= 0 || direction == 0) return false;
    int next = Math.max(0, Math.min(max, firstIndex + Integer.signum(direction)));
    if (next == firstIndex) return false;
    firstIndex = next;
    return true;
  }

  public boolean press(int mouseX, int mouseY, UiRect track, UiRect thumb, int total, int capacity) {
    int max = maxFirstIndex(total, capacity);
    if (max <= 0 || track.width() <= 0 || track.height() <= 0) return false;
    if (thumb.contains(mouseX, mouseY)) {
      dragging = true;
      dragOffset = mouseX - thumb.x();
      return true;
    }
    if (track.contains(mouseX, mouseY)) {
      dragging = true;
      dragOffset = Math.max(0, thumb.width() / 2);
      updateFromMouse(mouseX, track, thumb, max);
      return true;
    }
    return false;
  }

  public boolean drag(int mouseX, UiRect track, UiRect thumb, int total, int capacity) {
    if (!dragging) return false;
    int max = maxFirstIndex(total, capacity);
    if (max <= 0) {
      firstIndex = 0;
      return true;
    }
    updateFromMouse(mouseX, track, thumb, max);
    return true;
  }

  public boolean release() {
    if (!dragging) return false;
    dragging = false;
    return true;
  }

  private void updateFromMouse(int mouseX, UiRect track, UiRect thumb, int maxFirstIndex) {
    int travel = Math.max(1, track.width() - thumb.width());
    int raw = mouseX - dragOffset - track.x();
    int clamped = Math.max(0, Math.min(travel, raw));
    double ratio = (double) clamped / travel;
    firstIndex = Math.max(0, Math.min(maxFirstIndex, (int) Math.round(ratio * maxFirstIndex)));
  }

  private static int maxFirstIndex(int total, int capacity) {
    return Math.max(0, total - Math.max(1, capacity));
  }
}
