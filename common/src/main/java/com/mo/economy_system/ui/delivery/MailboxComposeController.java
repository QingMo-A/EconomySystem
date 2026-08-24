package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.MailboxSendPlayerMessage;
import com.mo.economy_system.common.network.MailboxSendStatus;
import com.mo.economy_system.ui.core.AbstractEconomyScreenController;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Common composition, attachment-selection and one-shot submission state machine. */
public final class MailboxComposeController
    extends AbstractEconomyScreenController<MailboxComposeState, MailboxComposeEvent> {
  private final MailboxComposePort port;

  public MailboxComposeController(List<MailboxComposeInventoryItem> inventory, MailboxComposePort port) {
    super(new MailboxComposeState(inventory, "", "", "", Set.of(), false, -1, -1, null));
    this.port = Objects.requireNonNull(port, "port");
  }

  @Override public void handle(MailboxComposeEvent event) {
    Objects.requireNonNull(event, "event");
    if (event instanceof MailboxComposeEvent.InventoryChanged value) inventory(value.inventory());
    else if (event instanceof MailboxComposeEvent.RecipientChanged value) text(value.value(), null, null);
    else if (event instanceof MailboxComposeEvent.SubjectChanged value) text(null, value.value(), null);
    else if (event instanceof MailboxComposeEvent.BodyChanged value) text(null, null, value.value());
    else if (event instanceof MailboxComposeEvent.SlotToggled value) toggle(value.slot());
    else if (event instanceof MailboxComposeEvent.SendResult value) result(value);
    else if (event instanceof MailboxComposeEvent.ActionClicked value) action(value.action());
  }

  private void inventory(List<MailboxComposeInventoryItem> inventory) {
    Set<Integer> occupied = inventory.stream().map(MailboxComposeInventoryItem::slot)
        .collect(java.util.stream.Collectors.toSet());
    LinkedHashSet<Integer> selected = new LinkedHashSet<>(state().selectedSlots());
    selected.retainAll(occupied);
    replace(inventory, state().recipient(), state().subject(), state().body(), selected,
        state().sending(), state().requestId(), state().appliedRevision(), state().status());
  }

  private void text(String recipient, String subject, String body) {
    replace(state().inventory(), recipient == null ? state().recipient() : recipient,
        subject == null ? state().subject() : subject,
        body == null ? state().body() : body, state().selectedSlots(), state().sending(),
        state().requestId(), state().appliedRevision(), state().status());
  }

  private void toggle(int slot) {
    if (state().sending() || state().itemAt(slot) == null) return;
    LinkedHashSet<Integer> selected = new LinkedHashSet<>(state().selectedSlots());
    if (!selected.remove(slot) && selected.size() < EconomyNetworkLimits.MAX_PLAYER_MAIL_ATTACHMENTS) {
      selected.add(slot);
    }
    replace(state().inventory(), state().recipient(), state().subject(), state().body(), selected,
        false, state().requestId(), state().appliedRevision(), state().status());
  }

  private void action(MailboxComposeAction action) {
    if (action == MailboxComposeAction.BACK) navigate(new UiNavigation.Back());
    else if (action == MailboxComposeAction.SEND) send();
  }

  private void send() {
    if (state().sending()) return;
    long requestId = port.nextRequestId();
    try {
      MailboxSendPlayerMessage message = new MailboxSendPlayerMessage(
          state().recipient(), state().subject(), state().body(),
          List.copyOf(state().selectedSlots()), requestId);
      replace(state().inventory(), state().recipient(), state().subject(), state().body(),
          state().selectedSlots(), true, requestId, state().appliedRevision(), null);
      port.send(message);
    } catch (IllegalArgumentException invalid) {
      replace(state().inventory(), state().recipient(), state().subject(), state().body(),
          state().selectedSlots(), false, -1, state().appliedRevision(), MailboxSendStatus.INVALID_CONTENT);
    }
  }

  private void result(MailboxComposeEvent.SendResult result) {
    if (!state().sending() || result.requestId() != state().requestId()
        || result.revision() <= state().appliedRevision()) return;
    replace(state().inventory(), state().recipient(), state().subject(), state().body(),
        state().selectedSlots(), false, state().requestId(), result.revision(), result.status());
    if (result.status() == MailboxSendStatus.SUCCESS) navigate(new UiNavigation.Back());
  }

  private void replace(List<MailboxComposeInventoryItem> inventory, String recipient, String subject,
                       String body, Set<Integer> slots, boolean sending, long requestId,
                       long revision, MailboxSendStatus status) {
    replaceState(new MailboxComposeState(inventory, recipient, subject, body, slots,
        sending, requestId, revision, status));
  }
}
