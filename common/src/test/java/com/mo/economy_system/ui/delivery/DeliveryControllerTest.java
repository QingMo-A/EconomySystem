package com.mo.economy_system.ui.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import com.mo.economy_system.common.mail.MailAttachmentSnapshot;
import com.mo.economy_system.common.mail.MailSnapshot;
import com.mo.economy_system.common.mail.MailType;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryControllerTest {
  @Test
  void rejectsStaleResponsesFiltersAndClaimsSelectedMailAttachmentWithCurrentRequest() {
    FakePort port = new FakePort();
    DeliveryController controller = new DeliveryController(port, mail ->
        mail.attachments().isEmpty() ? "" : "Diamond Sword");
    UUID firstMail = UUID.randomUUID();
    UUID secondMail = UUID.randomUUID();
    UUID firstAttachment = UUID.randomUUID();
    UUID secondAttachment = UUID.randomUUID();

    controller.handle(new DeliveryEvent.Initialize(10));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(1, port.lastRequest);

    controller.handle(new DeliveryEvent.DataLoaded(0,
        List.of(mail(firstMail, firstAttachment, 1, false))));
    assertEquals(ScreenState.LOADING, controller.state().screenState());

    controller.handle(new DeliveryEvent.DataLoaded(1, List.of(
        mail(firstMail, firstAttachment, 1, false),
        mail(secondMail, secondAttachment, 2, true))));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(1, controller.state().requestId());

    controller.handle(new DeliveryEvent.FilterChanged("Diamond Sword"));
    assertEquals(2, controller.state().filteredRows().size());
    controller.handle(new DeliveryEvent.MailSelected(secondMail));
    controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.CLAIM, secondAttachment, 20));
    assertEquals(ScreenState.LOADING, controller.state().screenState());
    assertEquals(secondMail, port.claimedMail);
    assertEquals(secondAttachment, port.claimedAttachment);
    assertEquals(1, port.claimRequest);

    controller.handle(new DeliveryEvent.DataLoaded(1, List.of()));
    assertEquals(ScreenState.EMPTY, controller.state().screenState());
  }

  @Test
  void silentRefreshAdoptsNewRequestIdWithoutSwitchingToLoading() {
    FakePort port = new FakePort();
    DeliveryController controller = new DeliveryController(port);
    UUID mailId = UUID.randomUUID();
    controller.handle(new DeliveryEvent.Initialize(10));
    controller.handle(new DeliveryEvent.DataLoaded(1,
        List.of(mail(mailId, UUID.randomUUID(), 1, true))));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertTrue(controller.state().can(DeliveryAction.CLAIM));

    controller.handle(new DeliveryEvent.RefreshStarted(2));

    assertEquals(ScreenState.READY, controller.state().screenState());
    assertEquals(2, controller.state().requestId());
    assertFalse(controller.state().can(DeliveryAction.CLAIM));
    assertTrue(controller.state().can(DeliveryAction.BACK));

    controller.handle(new DeliveryEvent.DataLoaded(1, List.of()));
    assertEquals(1, controller.state().rows().size(), "stale pre-refresh response must be ignored");
    assertEquals(2, controller.state().requestId());

    controller.handle(new DeliveryEvent.RefreshFailed(2));
    assertEquals(ScreenState.READY, controller.state().screenState());
    assertTrue(controller.state().can(DeliveryAction.CLAIM));
  }

  @Test
  void liveRefreshKeepsSelectedMailVisibleWhenNewMailShiftsPages() {
    FakePort port = new FakePort();
    DeliveryController controller = new DeliveryController(port);
    UUID firstMail = UUID.randomUUID();
    UUID secondMail = UUID.randomUUID();
    UUID newMail = UUID.randomUUID();

    controller.handle(new DeliveryEvent.Initialize(10));
    controller.handle(new DeliveryEvent.DataLoaded(1, List.of(
        mail(firstMail, UUID.randomUUID(), 1, true),
        mail(secondMail, UUID.randomUUID(), 1, true))));
    controller.handle(new DeliveryEvent.ViewportChanged(1));
    controller.handle(new DeliveryEvent.NextPage());
    controller.handle(new DeliveryEvent.MailSelected(secondMail));
    assertEquals(1, controller.state().page());
    assertEquals(secondMail, controller.state().selectedEntryId());

    controller.handle(new DeliveryEvent.RefreshStarted(2));
    controller.handle(new DeliveryEvent.DataLoaded(2, List.of(
        mail(newMail, UUID.randomUUID(), 1, false),
        mail(firstMail, UUID.randomUUID(), 1, true),
        mail(secondMail, UUID.randomUUID(), 1, true))));

    assertEquals(2, controller.state().requestId());
    assertEquals(secondMail, controller.state().selectedEntryId());
    assertEquals(2, controller.state().page());
    assertEquals(secondMail, controller.state().visibleRows().get(0).mailId());
    assertTrue(controller.state().can(DeliveryAction.CLAIM));
  }

  @Test
  void timesOutRetriesNavigatesHomeAndCompose() {
    FakePort port = new FakePort();
    DeliveryController controller = new DeliveryController(port);
    controller.handle(new DeliveryEvent.Initialize(100));
    controller.handle(new DeliveryEvent.Tick(100 + DeliveryController.TIMEOUT_NANOS));
    assertEquals(ScreenState.ERROR, controller.state().screenState());
    controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.RETRY, null, 200));
    assertEquals(2, port.lastRequest);
    controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.BACK, null, 0));
    UiNavigation.Route home = (UiNavigation.Route) controller.pollNavigation().orElseThrow();
    assertEquals(EconomyUiRoute.HOME, home.route());

    controller.handle(new DeliveryEvent.ActionClicked(DeliveryAction.COMPOSE, null, 0));
    UiNavigation.Route compose = (UiNavigation.Route) controller.pollNavigation().orElseThrow();
    assertEquals(EconomyUiRoute.MAIL_COMPOSE, compose.route());
  }

  @Test
  void deliveryFilterIncludesTargetResolvedNativeDisplayName() {
    UUID id = UUID.randomUUID();
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(id, 1);
    DeliveryState state = new DeliveryState(
        List.of(new DeliveryRow(entry, "Diamond Sword")), 0, 6, "Diamond Sword",
        ScreenState.READY, null, 1, java.util.Set.of(DeliveryAction.CLAIM));
    assertEquals(1, state.filteredRows().size());
  }

  private static MailSnapshot mail(UUID mailId, UUID attachmentId, int count, boolean read) {
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(attachmentId, count);
    return new MailSnapshot(mailId, MailType.MARKET, null, "", "", "", entry.source(),
        1, 0, read, false, true,
        List.of(new MailAttachmentSnapshot(entry.entryId(), entry.item())));
  }

  private static final class FakePort implements DeliveryPort {
    private long next;
    private long lastRequest = -1;
    private long claimRequest = -1;
    private UUID claimedMail;
    private UUID claimedAttachment;

    @Override public long nextRequestId() { return ++next; }
    @Override public void requestData(long requestId) { lastRequest = requestId; }
    @Override public void markRead(UUID mailId, long requestId) {}
    @Override public void delete(UUID mailId, long requestId) {}
    @Override public void claim(UUID mailId, UUID entryId, long requestId) {
      claimedMail = mailId;
      claimedAttachment = entryId;
      claimRequest = requestId;
    }
    @Override public void claimAll(UUID mailId, long requestId) {}
  }
}
