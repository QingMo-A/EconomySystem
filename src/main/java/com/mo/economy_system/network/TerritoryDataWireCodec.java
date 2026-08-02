package com.mo.economy_system.network;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;

/** Explicit, NBT-free wire format shared by both target loaders. */
public final class TerritoryDataWireCodec {
  private TerritoryDataWireCodec() {}

  public static void encodeRequest(TerritoryDataRequestMessage message, FriendlyByteBuf buffer) {
    buffer.writeLong(message.requestId());
  }

  public static TerritoryDataRequestMessage decodeRequest(FriendlyByteBuf buffer) {
    requireBytes(buffer, Long.BYTES);
    TerritoryDataRequestMessage message = new TerritoryDataRequestMessage(buffer.readLong());
    requireConsumed(buffer);
    return message;
  }

  public static void encodeResponse(TerritoryDataResponseMessage message, FriendlyByteBuf buffer) {
    int start = buffer.writerIndex();
    buffer.writeLong(message.requestId());
    buffer.writeInt(message.owned().size());
    for (Owned value : message.owned()) writeOwned(buffer, value);
    buffer.writeInt(message.authorized().size());
    for (Summary value : message.authorized()) writeSummary(buffer, value);
    if (buffer.writerIndex() - start > EconomyNetworkLimits.MAX_TERRITORY_RESPONSE_WIRE_BYTES) {
      throw new IllegalArgumentException("territory response exceeds wire budget");
    }
  }

  public static TerritoryDataResponseMessage decodeResponse(FriendlyByteBuf buffer) {
    if (buffer.readableBytes() > EconomyNetworkLimits.MAX_TERRITORY_RESPONSE_WIRE_BYTES) {
      throw new DecoderException("territory response exceeds wire budget");
    }
    long requestId = buffer.readLong();
    int ownedCount = count(buffer, EconomyNetworkLimits.MAX_TERRITORIES_PER_RESPONSE, "owned");
    List<Owned> owned = new ArrayList<>(ownedCount);
    for (int i = 0; i < ownedCount; i++) owned.add(readOwned(buffer));
    int authorizedCount = count(buffer,
        EconomyNetworkLimits.MAX_TERRITORIES_PER_RESPONSE - ownedCount, "authorized");
    List<Summary> authorized = new ArrayList<>(authorizedCount);
    for (int i = 0; i < authorizedCount; i++) authorized.add(readSummary(buffer));
    requireConsumed(buffer);
    try {
      return new TerritoryDataResponseMessage(requestId, owned, authorized);
    } catch (IllegalArgumentException error) {
      throw new DecoderException("invalid territory response", error);
    }
  }

  private static void writeOwned(FriendlyByteBuf buffer, Owned value) {
    writeSummary(buffer, value.summary());
    buffer.writeInt(value.authorizedMembers().size());
    for (Member member : value.authorizedMembers()) {
      buffer.writeUUID(member.playerId());
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

  private static Owned readOwned(FriendlyByteBuf buffer) {
    Summary summary = readSummary(buffer);
    int memberCount = count(buffer, EconomyNetworkLimits.MAX_TERRITORY_MEMBERS, "members");
    List<Member> members = new ArrayList<>(memberCount);
    for (int i = 0; i < memberCount; i++) members.add(new Member(buffer.readUUID(),
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

  private static void writeSummary(FriendlyByteBuf buffer, Summary value) {
    buffer.writeUUID(value.territoryId());
    buffer.writeUUID(value.ownerId());
    buffer.writeUtf(value.ownerName(), EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    buffer.writeUtf(value.name(), EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH);
    writePosition(buffer, value.pos1());
    writePosition(buffer, value.pos2());
    buffer.writeUtf(value.dimensionId(), EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH);
  }

  private static Summary readSummary(FriendlyByteBuf buffer) {
    return new Summary(buffer.readUUID(), buffer.readUUID(),
        buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH),
        buffer.readUtf(EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH), readPosition(buffer),
        readPosition(buffer), buffer.readUtf(EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH));
  }

  private static void writePosition(FriendlyByteBuf buffer, Position value) {
    buffer.writeInt(value.x()); buffer.writeInt(value.y()); buffer.writeInt(value.z());
  }
  private static Position readPosition(FriendlyByteBuf buffer) {
    return new Position(buffer.readInt(), buffer.readInt(), buffer.readInt());
  }

  private static void writeBuff(FriendlyByteBuf buffer, Buff value) {
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

  private static Buff readBuff(FriendlyByteBuf buffer) {
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

  private static int count(FriendlyByteBuf buffer, int max, String name) {
    int value = buffer.readInt();
    if (value < 0 || value > max) throw new DecoderException("invalid " + name + " count: " + value);
    return value;
  }
  private static void requireBytes(FriendlyByteBuf buffer, int count) {
    if (buffer.readableBytes() < count) throw new DecoderException("truncated territory payload");
  }
  private static void requireConsumed(FriendlyByteBuf buffer) {
    if (buffer.isReadable()) throw new DecoderException("trailing territory payload data");
  }
}
