package com.mo.economy_system.common.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.platform.nbt.NbtData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MailboxCodecTest {
  @Test
  void monetaryMailRoundTripsUsingCurrentSchema() {
    UUID owner = UUID.randomUUID();
    MailRecord mail = new MailRecord(
        UUID.randomUUID(), MailType.SYSTEM, null, "", "Payment", "Body", "mail.system",
        10L, 0L, List.of(), 250, false, true);
    MailboxLedger.State state = new MailboxLedger.State(
        Map.of(owner, List.of(mail)), List.of(), Map.of(), Map.of());

    NbtData.Compound encoded = MailboxCodec.encode(state);
    MailboxLedger.State decoded = MailboxCodec.decode(encoded);

    assertEquals(MailboxCodec.SCHEMA_VERSION, ((NbtData.IntValue) encoded.get("schema")).value());
    assertEquals(state, decoded);
    assertEquals(250, decoded.personal().get(owner).get(0).moneyAmount());
    assertEquals(0, decoded.personal().get(owner).get(0).currencyRewardAmount());
  }

  @Test
  void deferredCurrencyRewardRoundTripsSeparatelyFromImmediateMoney() {
    UUID owner = UUID.randomUUID();
    UUID rewardRecordId = UUID.randomUUID();
    MailRecord mail = new MailRecord(
        UUID.randomUUID(), MailType.SYSTEM, null, "", "Reward", "Body", "mail.commission",
        10L, 0L, List.of(), rewardRecordId, 375, false, false, true);
    MailboxLedger.State state = new MailboxLedger.State(
        Map.of(owner, List.of(mail)), List.of(), Map.of(), Map.of());

    MailRecord decoded = MailboxCodec.decode(MailboxCodec.encode(state))
        .personal().get(owner).get(0);

    assertEquals(0, decoded.moneyAmount());
    assertEquals(rewardRecordId, decoded.rewardRecordId());
    assertEquals(375, decoded.currencyRewardAmount());
    assertFalse(decoded.currencyRewardClaimed());
  }

  @Test
  void legacySchemaWithoutMoneyDefaultsToZero() {
    UUID owner = UUID.randomUUID();
    UUID mailId = UUID.randomUUID();
    NbtData.Compound oldMail = NbtData.compoundBuilder()
        .putUuid("id", mailId)
        .putString("type", "system")
        .putString("sender_name", "")
        .putString("subject", "Subject")
        .putString("body", "Body")
        .putString("source", "mail.system")
        .putLong("created", 1L)
        .putLong("expires", 0L)
        .putBoolean("read", false)
        .putBoolean("protected", true)
        .put("attachments", NbtData.list(List.of()))
        .build();
    NbtData.Compound root = NbtData.compoundBuilder()
        .putInt("schema", 2)
        .put("personal", NbtData.list(List.of(
            NbtData.compoundBuilder().putUuid("owner", owner)
                .put("mails", NbtData.list(List.of(oldMail))).build())))
        .put("announcements", NbtData.list(List.of()))
        .put("announcement_reads", NbtData.list(List.of()))
        .put("announcement_dismissed", NbtData.list(List.of()))
        .build();

    MailRecord decoded = MailboxCodec.decode(root).personal().get(owner).get(0);
    assertEquals(0, decoded.moneyAmount());
    assertEquals(null, decoded.rewardRecordId());
    assertEquals(0, decoded.currencyRewardAmount());
    assertFalse(decoded.currencyRewardClaimed());
  }

  @Test
  void schemaThreeImmediateMoneyRemainsSeparateFromDeferredRewardFields() {
    UUID owner = UUID.randomUUID();
    UUID mailId = UUID.randomUUID();
    NbtData.Compound oldMail = NbtData.compoundBuilder()
        .putUuid("id", mailId)
        .putString("type", "system")
        .putString("sender_name", "")
        .putString("subject", "Subject")
        .putString("body", "Body")
        .putString("source", "mail.system")
        .putLong("created", 1L)
        .putLong("expires", 0L)
        .putBoolean("read", false)
        .putBoolean("protected", true)
        .putInt("money", 250)
        .put("attachments", NbtData.list(List.of()))
        .build();
    NbtData.Compound root = NbtData.compoundBuilder()
        .putInt("schema", 3)
        .put("personal", NbtData.list(List.of(
            NbtData.compoundBuilder().putUuid("owner", owner)
                .put("mails", NbtData.list(List.of(oldMail))).build())))
        .put("announcements", NbtData.list(List.of()))
        .put("announcement_reads", NbtData.list(List.of()))
        .put("announcement_dismissed", NbtData.list(List.of()))
        .build();

    MailRecord decoded = MailboxCodec.decode(root).personal().get(owner).get(0);
    assertEquals(250, decoded.moneyAmount());
    assertEquals(null, decoded.rewardRecordId());
    assertEquals(0, decoded.currencyRewardAmount());
    assertFalse(decoded.currencyRewardClaimed());
  }

  @Test
  void futureMailboxSchemaIsRejected() {
    NbtData.Compound root = NbtData.compoundBuilder()
        .putInt("schema", MailboxCodec.SCHEMA_VERSION + 1).build();
    assertThrows(IllegalArgumentException.class, () -> MailboxCodec.decode(root));
  }
}
