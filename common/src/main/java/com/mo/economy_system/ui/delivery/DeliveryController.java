package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.mail.MailSnapshot;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Shared mailbox request, filtering, selection and mutation controller. */
public final class DeliveryController extends AbstractEconomyScreenController<DeliveryState, DeliveryEvent> {
  public static final long TIMEOUT_NANOS = 10_000_000_000L;
  private final DeliveryPort port;
  private final Function<MailSnapshot, String> displayNameResolver;
  private long startedAt;
  private boolean requestInFlight;

  public DeliveryController(DeliveryPort port) { this(port, ignored -> ""); }

  public DeliveryController(DeliveryPort port, Function<MailSnapshot, String> displayNameResolver) {
    super(new DeliveryState(List.of(), 0, 1, "", DeliveryCategory.ALL, null,
        ScreenState.IDLE, null, -1, Set.of(DeliveryAction.BACK, DeliveryAction.COMPOSE)));
    this.port = java.util.Objects.requireNonNull(port, "port");
    this.displayNameResolver = java.util.Objects.requireNonNull(displayNameResolver, "displayNameResolver");
  }

  @Override public void handle(DeliveryEvent event) {
    if (event instanceof DeliveryEvent.Initialize value) request(value.nowNanos());
    else if (event instanceof DeliveryEvent.Retry value) request(value.nowNanos());
    else if (event instanceof DeliveryEvent.DataLoaded value) loaded(value);
    else if (event instanceof DeliveryEvent.DataFailed value) failed(value);
    else if (event instanceof DeliveryEvent.FilterChanged value) filter(value.value());
    else if (event instanceof DeliveryEvent.CategoryChanged value) category(value.category());
    else if (event instanceof DeliveryEvent.MailSelected value) select(value.entryId());
    else if (event instanceof DeliveryEvent.ViewportChanged value) pageSize(value.pageSize());
    else if (event instanceof DeliveryEvent.NextPage) page(1);
    else if (event instanceof DeliveryEvent.PreviousPage) page(-1);
    else if (event instanceof DeliveryEvent.Scroll value) page(Integer.signum(value.steps()));
    else if (event instanceof DeliveryEvent.ActionClicked value) action(value);
    else if (event instanceof DeliveryEvent.Tick value) tick(value.nowNanos());
  }

  private void request(long nowNanos) {
    long id = port.nextRequestId();
    if (id < 0) throw new IllegalStateException("mailbox request id must be non-negative");
    startedAt = nowNanos;
    requestInFlight = true;
    replaceState(new DeliveryState(state().rows(), 0, state().pageSize(), state().filter(),
        state().category(), state().selectedEntryId(), ScreenState.LOADING, null, id,
        Set.of(DeliveryAction.BACK, DeliveryAction.COMPOSE)));
    port.requestData(id);
  }

  private void loaded(DeliveryEvent.DataLoaded event) {
    if (event.requestId() != state().requestId()) return;
    List<DeliveryRow> rows = event.mails().stream()
        .map(mail -> new DeliveryRow(mail, displayNameResolver.apply(mail))).toList();
    requestInFlight = false;
    UUID selected = selectionFor(rows, state().filter(), state().category(), state().selectedEntryId());
    replaceState(new DeliveryState(rows, 0, state().pageSize(), state().filter(), state().category(), selected,
        rows.isEmpty() ? ScreenState.EMPTY : ScreenState.READY, null, event.requestId(),
        Set.of(DeliveryAction.CLAIM, DeliveryAction.CLAIM_ALL, DeliveryAction.DELETE,
            DeliveryAction.COMPOSE, DeliveryAction.BACK)));
    DeliveryRow selectedRow = state().selectedRow();
    if (selectedRow != null && !selectedRow.mail().read()) {
      port.markRead(selectedRow.mailId(), event.requestId());
    }
  }

  private void failed(DeliveryEvent.DataFailed event) {
    if (event.requestId() != state().requestId()) return;
    requestInFlight = false;
    replaceState(new DeliveryState(state().rows(), state().page(), state().pageSize(), state().filter(),
        state().category(), state().selectedEntryId(), ScreenState.ERROR, event.errorKey(), -1,
        Set.of(DeliveryAction.RETRY, DeliveryAction.BACK, DeliveryAction.COMPOSE)));
  }

  private void filter(String value) {
    String next = value == null ? "" : value;
    UUID selected = selectionFor(state().rows(), next, state().category(), state().selectedEntryId());
    replaceState(new DeliveryState(state().rows(), 0, state().pageSize(), next,
        state().category(), selected, state().screenState(), state().errorKey(), state().requestId(), state().actions()));
  }

  private void category(DeliveryCategory category) {
    if (category == state().category()) return;
    UUID selected = selectionFor(state().rows(), state().filter(), category, null);
    replaceState(new DeliveryState(state().rows(), 0, state().pageSize(), state().filter(),
        category, selected, state().screenState(), state().errorKey(), state().requestId(), state().actions()));
    DeliveryRow row = state().selectedRow();
    if (row != null && !row.mail().read() && state().requestId() >= 0) port.markRead(row.mailId(), state().requestId());
  }

  private void select(UUID mailId) {
    if (mailId == null) return;
    DeliveryRow row = state().filteredRows().stream().filter(value -> value.mailId().equals(mailId)).findFirst().orElse(null);
    if (row == null) return;
    if (!mailId.equals(state().selectedEntryId())) {
      replaceState(new DeliveryState(state().rows(), state().page(), state().pageSize(), state().filter(),
          state().category(), mailId, state().screenState(), state().errorKey(), state().requestId(), state().actions()));
    }
    if (!row.mail().read() && state().requestId() >= 0) port.markRead(mailId, state().requestId());
  }

  private void pageSize(int value) {
    if (value < 1 || value == state().pageSize()) return;
    int page = Math.min(state().page(), Math.max(0, (state().filteredRows().size() + value - 1) / value - 1));
    UUID selected = firstOnPage(state().filteredRows(), page, value, state().selectedEntryId());
    replaceState(new DeliveryState(state().rows(), page, value, state().filter(), state().category(), selected,
        state().screenState(), state().errorKey(), state().requestId(), state().actions()));
  }

  private void page(int delta) {
    int next = Math.max(0, Math.min(state().totalPages() - 1, state().page() + delta));
    if (next == state().page()) return;
    UUID selected = firstOnPage(state().filteredRows(), next, state().pageSize(), null);
    replaceState(new DeliveryState(state().rows(), next, state().pageSize(), state().filter(),
        state().category(), selected, state().screenState(), state().errorKey(), state().requestId(), state().actions()));
  }

  private void action(DeliveryEvent.ActionClicked event) {
    if (!state().can(event.action())) return;
    if (event.action() == DeliveryAction.BACK) {
      navigate(new UiNavigation.Route(EconomyUiRoute.HOME));
      return;
    }
    if (event.action() == DeliveryAction.COMPOSE) {
      navigate(new UiNavigation.Route(EconomyUiRoute.MAIL_COMPOSE));
      return;
    }
    if (event.action() == DeliveryAction.RETRY) {
      request(event.nowNanos());
      return;
    }
    DeliveryRow row = state().selectedRow();
    if (row == null || state().requestId() < 0) return;
    requestInFlight = true;
    startedAt = event.nowNanos();
    replaceState(new DeliveryState(state().rows(), state().page(), state().pageSize(), state().filter(),
        state().category(), row.mailId(), ScreenState.LOADING, null, state().requestId(),
        Set.of(DeliveryAction.BACK, DeliveryAction.COMPOSE)));
    switch (event.action()) {
      case DELETE -> port.delete(row.mailId(), state().requestId());
      case CLAIM_ALL -> port.claimAll(row.mailId(), state().requestId());
      case CLAIM -> {
        UUID attachmentId = event.entryId();
        if (attachmentId == null && row.firstAttachment() != null) attachmentId = row.firstAttachment().entryId();
        if (attachmentId == null) {
          requestInFlight = false;
          return;
        }
        port.claim(row.mailId(), attachmentId, state().requestId());
      }
      default -> requestInFlight = false;
    }
  }

  private void tick(long nowNanos) {
    if (requestInFlight && nowNanos - startedAt >= TIMEOUT_NANOS) {
      requestInFlight = false;
      replaceState(new DeliveryState(state().rows(), state().page(), state().pageSize(), state().filter(),
          state().category(), state().selectedEntryId(), ScreenState.ERROR,
          "screen.delivery_box.sync_timeout", -1, Set.of(DeliveryAction.RETRY, DeliveryAction.BACK, DeliveryAction.COMPOSE)));
    }
  }

  private static UUID selectionFor(List<DeliveryRow> rows, String filter, DeliveryCategory category, UUID preferred) {
    String needle = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
    List<DeliveryRow> matching = rows.stream().filter(row -> matches(row, needle, category)).toList();
    if (matching.isEmpty()) return null;
    if (preferred != null && matching.stream().anyMatch(row -> row.mailId().equals(preferred))) return preferred;
    return matching.get(0).mailId();
  }

  private static boolean matches(DeliveryRow row, String needle, DeliveryCategory category) {
    if (!row.matchesCategory(category)) return false;
    if (needle.isEmpty()) return true;
    if (row.mailId().toString().contains(needle) || row.searchableText().contains(needle)
        || row.mail().type().name().toLowerCase(Locale.ROOT).contains(needle)) return true;
    for (var attachment : row.mail().attachments()) {
      if (attachment.item().itemId().toLowerCase(Locale.ROOT).contains(needle)) return true;
    }
    return false;
  }

  private static UUID firstOnPage(List<DeliveryRow> filtered, int page, int pageSize, UUID preferred) {
    int start = Math.min(page * pageSize, filtered.size());
    int end = Math.min(filtered.size(), start + pageSize);
    if (start >= end) return null;
    if (preferred != null) {
      for (int i = start; i < end; i++) if (filtered.get(i).mailId().equals(preferred)) return preferred;
    }
    return filtered.get(start).mailId();
  }
}
