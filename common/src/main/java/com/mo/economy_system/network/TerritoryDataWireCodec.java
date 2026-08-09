package com.mo.economy_system.network;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseKind;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;

/** Explicit, NBT-free wire format shared by both target loaders. */
public final class TerritoryDataWireCodec {
  private TerritoryDataWireCodec() {}

  public static void encodeRequest(TerritoryDataRequestMessage message, WireBuffer buffer) {
    buffer.writeLong(message.requestId());
  }

  public static TerritoryDataRequestMessage decodeRequest(WireBuffer buffer) {
    requireBytes(buffer, Long.BYTES);
    TerritoryDataRequestMessage message = new TerritoryDataRequestMessage(buffer.readLong());
    requireConsumed(buffer);
    return message;
  }

  public static void encodeResponse(TerritoryDataResponseMessage message, WireBuffer buffer) {
    encodeResponse(message, buffer, EconomyNetworkLimits.MAX_TERRITORY_RESPONSE_WIRE_BYTES);
  }

  static void encodeResponse(
      TerritoryDataResponseMessage message, WireBuffer buffer, int maximumBytes) {
    if (maximumBytes < 0 || maximumBytes > EconomyNetworkLimits.MAX_TERRITORY_RESPONSE_WIRE_BYTES) {
      throw new IllegalArgumentException("invalid territory wire budget");
    }
    try (WireBuffer temporary = buffer.temporary()) {
      temporary.writeUtf(message.kind().id(), 16);
      temporary.writeLong(message.requestId());
      if (message.kind() == TerritoryDataResponseKind.DATA) {
        temporary.writeInt(message.owned().size());
        for (Owned value : message.owned()) writeOwned(temporary, value);
        temporary.writeInt(message.authorized().size());
        for (Summary value : message.authorized()) writeSummary(temporary, value);
      }
      int length = temporary.readableBytes();
      if (length > maximumBytes) {
        throw new IllegalArgumentException("territory response exceeds wire budget");
      }
      buffer.writeRemaining(temporary);
    }
  }

  public static TerritoryDataResponseMessage decodeResponse(WireBuffer buffer) {
    if (buffer.readableBytes() > EconomyNetworkLimits.MAX_TERRITORY_RESPONSE_WIRE_BYTES) {
      throw new WireDecodeException("territory response exceeds wire budget");
    }
    TerritoryDataResponseKind kind;
    try {
      kind = TerritoryDataResponseKind.fromId(buffer.readUtf(16));
    } catch (IllegalArgumentException error) {
      throw new WireDecodeException("invalid territory response kind", error);
    }
    long requestId = buffer.readLong();
    if (kind == TerritoryDataResponseKind.ERROR) {
      requireConsumed(buffer);
      try {
        return TerritoryDataResponseMessage.error(requestId);
      } catch (IllegalArgumentException error) {
        throw new WireDecodeException("invalid territory error response", error);
      }
    }
    int ownedCount = count(buffer, EconomyNetworkLimits.MAX_TERRITORIES_PER_RESPONSE, "owned");
    List<Owned> owned = new ArrayList<>(ownedCount);
    for (int i = 0; i < ownedCount; i++) owned.add(readOwned(buffer));
    int authorizedCount = count(buffer,
        EconomyNetworkLimits.MAX_TERRITORIES_PER_RESPONSE - ownedCount, "authorized");
    List<Summary> authorized = new ArrayList<>(authorizedCount);
    for (int i = 0; i < authorizedCount; i++) authorized.add(readSummary(buffer));
    requireConsumed(buffer);
    try {
      return TerritoryDataResponseMessage.data(requestId, owned, authorized);
    } catch (IllegalArgumentException error) {
      throw new WireDecodeException("invalid territory response", error);
    }
  }

  private static void writeOwned(WireBuffer buffer, Owned value) {
    writeSummary(buffer, value.summary());
    buffer.writeInt(value.authorizedMembers().size());
    for (Member member : value.authorizedMembers()) {
      buffer.writeUuid(member.playerId());
      buffer.writeUtf(member.playerName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    }
    buffer.writeBoolean(value.backpoint().isPresent());
    value.backpoint().ifPresent(position -> writePosition(buffer, position));
    buffer.writeInt(value.rules().size());
    for (Rule rule : value.rules()) {
      buffer.writeUtf(rule.action().id(), 32);
      buffer.writeUtf(rule.level().id(), 32);
    }
    buffer.writeInt(value.buffs().size());
    for (Buff buff : value.buffs()) writeBuff(buffer, buff);
  }

  private static Owned readOwned(WireBuffer buffer) {
    Summary summary = readSummary(buffer);
    int memberCount = count(buffer, EconomyNetworkLimits.MAX_TERRITORY_MEMBERS, "members");
    List<Member> members = new ArrayList<>(memberCount);
    for (int i = 0; i < memberCount; i++) members.add(new Member(buffer.readUuid(),
        buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH)));
    Optional<Position> backpoint = buffer.readBoolean() ? Optional.of(readPosition(buffer)) : Optional.empty();
    int ruleCount = count(buffer, EconomyNetworkLimits.MAX_TERRITORY_RULES, "rules");
    List<Rule> rules = new ArrayList<>(ruleCount);
    for (int i = 0; i < ruleCount; i++) rules.add(new Rule(
        RuleAction.fromId(buffer.readUtf(32)), RuleLevel.fromId(buffer.readUtf(32))));
    int buffCount = count(buffer, EconomyNetworkLimits.MAX_TERRITORY_BUFFS, "buffs");
    List<Buff> buffs = new ArrayList<>(buffCount);
    for (int i = 0; i < buffCount; i++) buffs.add(readBuff(buffer));
    return new Owned(summary, members, backpoint, rules, buffs);
  }

  private static void writeSummary(WireBuffer buffer, Summary value) {
    buffer.writeUuid(value.territoryId());
    buffer.writeUuid(value.ownerId());
    buffer.writeUtf(value.ownerName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    buffer.writeUtf(value.name(), EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH);
    writePosition(buffer, value.pos1());
    writePosition(buffer, value.pos2());
    buffer.writeUtf(value.dimensionId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
  }

  private static Summary readSummary(WireBuffer buffer) {
    return new Summary(buffer.readUuid(), buffer.readUuid(),
        buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH), readPosition(buffer),
        readPosition(buffer), buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH));
  }

  private static void writePosition(WireBuffer buffer, Position value) {
    buffer.writeInt(value.x()); buffer.writeInt(value.y()); buffer.writeInt(value.z());
  }
  private static Position readPosition(WireBuffer buffer) {
    return new Position(buffer.readInt(), buffer.readInt(), buffer.readInt());
  }

  private static void writeBuff(WireBuffer buffer, Buff value) {
    buffer.writeUtf(value.id(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
    buffer.writeUtf(value.displayText(), EconomyNetworkLimits.MAX_TERRITORY_TEXT_LENGTH);
    buffer.writeUtf(value.effectId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
    buffer.writeBoolean(value.initialUnlocked()); buffer.writeInt(value.initialLevel());
    buffer.writeInt(value.singleUpgradeLevel()); buffer.writeInt(value.maxLevel());
    buffer.writeBoolean(value.unlocked()); buffer.writeInt(value.level());
    buffer.writeInt(value.upgradeCosts().size());
    for (BuffUpgradeCost cost : value.upgradeCosts()) {
      buffer.writeInt(cost.items().size());
      for (ItemRequirement item : cost.items()) {
        buffer.writeUtf(item.itemId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
        buffer.writeInt(item.count());
      }
      buffer.writeInt(cost.experience()); buffer.writeInt(cost.currency());
    }
  }

  private static Buff readBuff(WireBuffer buffer) {
    String id = buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
    String display = buffer.readUtf(EconomyNetworkLimits.MAX_TERRITORY_TEXT_LENGTH);
    String effect = buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
    boolean initialUnlocked = buffer.readBoolean(); int initialLevel = buffer.readInt();
    int step = buffer.readInt(); int max = buffer.readInt();
    boolean unlocked = buffer.readBoolean(); int level = buffer.readInt();
    int costCount = count(buffer, EconomyNetworkLimits.MAX_TERRITORY_BUFF_COST_LEVELS, "buff costs");
    List<BuffUpgradeCost> costs = new ArrayList<>(costCount);
    for (int i = 0; i < costCount; i++) {
      int itemCount = count(buffer, EconomyNetworkLimits.MAX_TERRITORY_COST_ITEMS, "cost items");
      List<ItemRequirement> items = new ArrayList<>(itemCount);
      for (int j = 0; j < itemCount; j++) items.add(new ItemRequirement(
          buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH), buffer.readInt()));
      costs.add(new BuffUpgradeCost(items, buffer.readInt(), buffer.readInt()));
    }
    return new Buff(id, display, effect, initialUnlocked, initialLevel, step, max, unlocked, level, costs);
  }

  private static int count(WireBuffer buffer, int max, String name) {
    int value = buffer.readInt();
    if (value < 0 || value > max) throw new WireDecodeException("invalid " + name + " count: " + value);
    return value;
  }
  private static void requireBytes(WireBuffer buffer, int count) {
    if (buffer.readableBytes() < count) throw new WireDecodeException("truncated territory payload");
  }
  private static void requireConsumed(WireBuffer buffer) {
    if (buffer.isReadable()) throw new WireDecodeException("trailing territory payload data");
  }
}
