package com.mo.economy_system.network;

import static com.mo.economy_system.common.territory.TerritoryManagementTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseMessage;
import com.mo.economy_system.common.network.TransferTerritoryOwnershipMessage;
import com.mo.economy_system.common.network.UnlockTerritoryBuffMessage;
import com.mo.economy_system.common.network.UpdateTerritoryPermissionMessage;
import com.mo.economy_system.common.network.UpdateTerritoryRuleMessage;
import com.mo.economy_system.common.network.UpgradeTerritoryBuffMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryManagementWireCodecTest {
  @Test
  void fixedUuidMessagesHaveGoldenFieldOrder() {
    TestWireBuffer modify = buffer();
    TerritoryManagementWireCodec.encodeModifyMode(
        new ModifyTerritoryModeMessage(TERRITORY), modify);
    assertEquals(16, modify.readableBytes());
    assertEquals(TERRITORY, modify.readUUID());

    TestWireBuffer request = buffer();
    TerritoryManagementWireCodec.encodeSingleRequest(
        new SingleTerritoryDataRequestMessage(TERRITORY, 17), request);
    assertEquals(24, request.readableBytes());
    assertEquals(TERRITORY, request.readUUID());
    assertEquals(17, request.readLong());

    TestWireBuffer permission = buffer();
    TerritoryManagementWireCodec.encodePermission(
        new UpdateTerritoryPermissionMessage(TERRITORY, MEMBER, true), permission);
    assertEquals(33, permission.readableBytes());
    assertEquals(TERRITORY, permission.readUUID());
    assertEquals(MEMBER, permission.readUUID());
    assertTrue(permission.readBoolean());

    TestWireBuffer transfer = buffer();
    TerritoryManagementWireCodec.encodeTransfer(
        new TransferTerritoryOwnershipMessage(TERRITORY, MEMBER), transfer);
    assertEquals(32, transfer.readableBytes());
    assertEquals(TERRITORY, transfer.readUUID());
    assertEquals(MEMBER, transfer.readUUID());
  }

  @Test
  void variableAndResponseMessagesRoundTrip() {
    UnlockTerritoryBuffMessage unlock =
        new UnlockTerritoryBuffMessage(TERRITORY, "economy_system:speed");
    TestWireBuffer unlockBuffer = buffer();
    TerritoryManagementWireCodec.encodeUnlockBuff(unlock, unlockBuffer);
    assertEquals(unlock, TerritoryManagementWireCodec.decodeUnlockBuff(unlockBuffer));

    UpgradeTerritoryBuffMessage upgrade =
        new UpgradeTerritoryBuffMessage(TERRITORY, "economy_system:speed");
    TestWireBuffer upgradeBuffer = buffer();
    TerritoryManagementWireCodec.encodeUpgradeBuff(upgrade, upgradeBuffer);
    assertEquals(upgrade, TerritoryManagementWireCodec.decodeUpgradeBuff(upgradeBuffer));

    UpdateTerritoryRuleMessage rule = new UpdateTerritoryRuleMessage(
        TERRITORY, RuleAction.OPEN_CONTAINER, RuleLevel.OWNER_ONLY);
    TestWireBuffer ruleBuffer = buffer();
    TerritoryManagementWireCodec.encodeRule(rule, ruleBuffer);
    assertEquals(rule, TerritoryManagementWireCodec.decodeRule(ruleBuffer));

    SingleTerritoryDataResponseMessage response =
        SingleTerritoryDataResponseMessage.data(91, owned());
    TestWireBuffer responseBuffer = buffer();
    TerritoryManagementWireCodec.encodeSingleResponse(response, responseBuffer);
    assertEquals(response, TerritoryManagementWireCodec.decodeSingleResponse(responseBuffer));
  }

  @Test
  void rejectsUnknownEnumsTrailingBytesAndEveryFixedTruncation() {
    TestWireBuffer unknownAction = buffer();
    unknownAction.writeUUID(TERRITORY);
    unknownAction.writeUtf("future_action");
    unknownAction.writeUtf("members");
    assertThrows(
        RuntimeException.class,
        () -> TerritoryManagementWireCodec.decodeRule(unknownAction));

    TestWireBuffer trailing = buffer();
    TerritoryManagementWireCodec.encodeTransfer(
        new TransferTerritoryOwnershipMessage(TERRITORY, MEMBER), trailing);
    trailing.writeByte(1);
    assertThrows(
        RuntimeException.class,
        () -> TerritoryManagementWireCodec.decodeTransfer(trailing));

    for (int size = 0; size < 33; size++) {
      TestWireBuffer truncated = buffer();
      truncated.writeZero(size);
      assertThrows(
          RuntimeException.class,
          () -> TerritoryManagementWireCodec.decodePermission(truncated));
    }
  }

  @Test
  void clientCannotInjectNamesOrRuntimeEnumsIntoAdministrationWire() {
    TestWireBuffer permission = buffer();
    TerritoryManagementWireCodec.encodePermission(
        new UpdateTerritoryPermissionMessage(TERRITORY, MEMBER, false), permission);
    assertEquals(33, permission.readableBytes());
    TestWireBuffer transfer = buffer();
    TerritoryManagementWireCodec.encodeTransfer(
        new TransferTerritoryOwnershipMessage(TERRITORY, MEMBER), transfer);
    assertEquals(32, transfer.readableBytes());
  }

  private static TestWireBuffer buffer() {
    return new TestWireBuffer();
  }
}
