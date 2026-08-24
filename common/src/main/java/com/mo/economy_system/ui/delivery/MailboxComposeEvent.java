package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.network.MailboxSendStatus;
import java.util.List;

public sealed interface MailboxComposeEvent permits
    MailboxComposeEvent.InventoryChanged,
    MailboxComposeEvent.RecipientChanged,
    MailboxComposeEvent.SubjectChanged,
    MailboxComposeEvent.BodyChanged,
    MailboxComposeEvent.SlotToggled,
    MailboxComposeEvent.SendResult,
    MailboxComposeEvent.ActionClicked {
  record InventoryChanged(List<MailboxComposeInventoryItem> inventory) implements MailboxComposeEvent {
    public InventoryChanged { inventory = List.copyOf(inventory); }
  }
  record RecipientChanged(String value) implements MailboxComposeEvent {}
  record SubjectChanged(String value) implements MailboxComposeEvent {}
  record BodyChanged(String value) implements MailboxComposeEvent {}
  record SlotToggled(int slot) implements MailboxComposeEvent {}
  record SendResult(long revision, long requestId, MailboxSendStatus status) implements MailboxComposeEvent {}
  record ActionClicked(MailboxComposeAction action) implements MailboxComposeEvent {}
}
