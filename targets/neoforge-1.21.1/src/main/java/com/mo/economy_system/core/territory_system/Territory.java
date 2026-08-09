package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryGeometry;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class Territory {

  private final UUID territoryID; // 领地 ID
  private UUID ownerUUID; // 领主 UUID
  private String ownerName; // 领主名字
  private final String name; // 领地名称
  private int x1, y1, z1, x2, y2, z2; // 领地范围
  private final Set<PlayerInfo> authorizedPlayers; // 保存有权限玩家的列表
  private BlockPos backpoint; // 回城点
  private final ResourceKey<Level> dimension; // 所在维度
  private int territoryOrder; // 领地序号
  private final List<TerritoryBuff> territoryBuffs = new ArrayList<>(); // 领地buff
  private final EnumMap<TerritoryPermissionAction, TerritoryPermissionLevel> permissions =
      new EnumMap<>(TerritoryPermissionAction.class);

  // 用于新建领地的构造方法（生成新 UUID）
  public Territory(
      String name,
      UUID ownerUUID,
      String ownerName,
      int x1,
      int y1,
      int z1,
      int x2,
      int y2,
      int z2,
      BlockPos backpoint,
      ResourceKey<Level> dimension) {
    this(
        UUID.randomUUID(),
        name,
        ownerUUID,
        ownerName,
        x1,
        y1,
        z1,
        x2,
        y2,
        z2,
        backpoint,
        dimension);
  }

  // 用于加载领地的构造方法（使用已存在的 UUID）
  public Territory(
      UUID territoryID,
      String name,
      UUID ownerUUID,
      String ownerName,
      int x1,
      int y1,
      int z1,
      int x2,
      int y2,
      int z2,
      BlockPos backpoint,
      ResourceKey<Level> dimension) {
    this.territoryID = territoryID;
    this.name = name;
    this.ownerUUID = ownerUUID;
    this.ownerName = ownerName;
    this.x1 = x1;
    this.y1 = y1;
    this.z1 = z1;
    this.x2 = x2;
    this.y2 = y2;
    this.z2 = z2;
    this.authorizedPlayers = new HashSet<>();
    this.backpoint = backpoint;
    this.dimension = dimension;
    for (TerritoryPermissionAction action : TerritoryPermissionAction.values()) {
      permissions.put(action, TerritoryPermissionLevel.MEMBERS);
    }
  }

  public UUID getTerritoryID() {
    return territoryID;
  }

  public UUID getOwnerUUID() {
    return ownerUUID;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwner(UUID ownerUUID, String ownerName) {
    this.ownerUUID = ownerUUID;
    this.ownerName = ownerName;
  }

  public String getName() {
    return name;
  }

  public ResourceKey<Level> getDimension() {
    return dimension;
  }

  // 获取领地起点和终点坐标
  public BlockPos getPos1() {
    return new BlockPos(x1, y1, z1);
  }

  public BlockPos getPos2() {
    return new BlockPos(x2, y2, z2);
  }

  public void setX1(int x1) {
    this.x1 = x1;
  }

  public void setZ2(int z2) {
    this.z2 = z2;
  }

  public void setY2(int y2) {
    this.y2 = y2;
  }

  public void setX2(int x2) {
    this.x2 = x2;
  }

  public void setZ1(int z1) {
    this.z1 = z1;
  }

  public void setY1(int y1) {
    this.y1 = y1;
  }

  public Bounds getBounds() {
    TerritoryGeometry.Rectangle rectangle = xzBounds();
    return new Bounds(
        rectangle.minX(),
        rectangle.minZ(),
        Math.toIntExact(rectangle.width() - 1L),
        Math.toIntExact(rectangle.height() - 1L));
  }

  public boolean isWithinBounds(int x, int y, int z) {
    return xzBounds().contains(x, z)
        && y >= Math.min(y1, y2)
        && y <= Math.max(y1, y2);
  }

  public boolean isWithinBoundsIgnoreY(int x, int z) {
    return xzBounds().contains(x, z);
  }

  private TerritoryGeometry.Rectangle xzBounds() {
    return TerritoryGeometry.rectangle(x1, z1, x2, z2);
  }

  public Set<PlayerInfo> getAuthorizedPlayers() {
    return authorizedPlayers;
  }

  public void addAuthorizedPlayer(UUID playerUUID, String playerName) {
    if (isOwner(playerUUID)) {
      return;
    }
    removeAuthorizedPlayer(playerUUID);
    authorizedPlayers.add(new PlayerInfo(playerUUID, playerName));
  }

  public boolean addAuthorizedPlayerIfAbsent(UUID playerUUID, String playerName) {
    Objects.requireNonNull(playerUUID, "playerUUID");
    Objects.requireNonNull(playerName, "playerName");
    String validName = playerName.trim();
    if (validName.isEmpty()
        || validName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH
        || isOwner(playerUUID)
        || hasPermission(playerUUID)) return false;
    authorizedPlayers.add(new PlayerInfo(playerUUID, validName));
    return authorizedPlayers.stream().filter(info -> info.getUuid().equals(playerUUID)).count()
        == 1;
  }

  public long authorizedPlayerCount(UUID playerUUID) {
    Objects.requireNonNull(playerUUID, "playerUUID");
    return authorizedPlayers.stream().filter(info -> info.getUuid().equals(playerUUID)).count();
  }

  public void removeAuthorizedPlayer(UUID playerUUID) {
    authorizedPlayers.removeIf(playerInfo -> playerInfo.getUuid().equals(playerUUID));
  }

  public boolean hasPermission(UUID playerUUID) {
    return authorizedPlayers.stream()
        .anyMatch(playerInfo -> playerInfo.getUuid().equals(playerUUID));
  }

  public TerritoryPermissionLevel getPermissionLevel(TerritoryPermissionAction action) {
    return permissions.getOrDefault(action, TerritoryPermissionLevel.MEMBERS);
  }

  public void setPermissionLevel(TerritoryPermissionAction action, TerritoryPermissionLevel level) {
    permissions.put(action, level);
  }

  public boolean isOwner(UUID playerUUID) {
    return ownerUUID.equals(playerUUID);
  }

  public BlockPos getBackpoint() {
    return backpoint;
  }

  public void setBackpoint(BlockPos backpoint) {
    this.backpoint = backpoint;
  }

  public List<TerritoryBuff> getTerritoryBuffs() {
    return territoryBuffs;
  }

  public TerritoryBuff getBuff(String buffID) {
    for (TerritoryBuff buff : territoryBuffs) {
      if (buff.getId().equals(buffID)) {
        return buff;
      }
    }
    return null; // 找不到返回 null
  }

  public void addBuffs(TerritoryBuff buff) {
    territoryBuffs.add(buff);
  }

  public void removeBuff(String buffId) {
    territoryBuffs.removeIf(buff -> buff.getId().equals(buffId));
  }

  public CompoundTag toNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putUUID("TerritoryID", territoryID);
    tag.putUUID("OwnerUUID", ownerUUID);
    tag.putString("OwnerName", ownerName);
    tag.putString("Name", name);
    tag.putInt("X1", x1);
    tag.putInt("Y1", y1);
    tag.putInt("Z1", z1);
    tag.putInt("X2", x2);
    tag.putInt("Y2", y2);
    tag.putInt("Z2", z2);

    // 保存维度信息
    tag.putString("Dimension", dimension.location().toString());

    // 保存有权限玩家
    ListTag authorizedPlayersTag = new ListTag();
    for (PlayerInfo playerInfo : authorizedPlayers) {
      CompoundTag playerTag = new CompoundTag();
      playerTag.putUUID("PlayerUUID", playerInfo.getUuid());
      playerTag.putString("PlayerName", playerInfo.getName());
      authorizedPlayersTag.add(playerTag);
    }
    tag.put("AuthorizedPlayers", authorizedPlayersTag);

    CompoundTag permissionsTag = new CompoundTag();
    for (TerritoryPermissionAction action : TerritoryPermissionAction.values()) {
      permissionsTag.putString(action.name(), getPermissionLevel(action).name());
    }
    tag.put("Permissions", permissionsTag);

    // 保存回城点
    if (backpoint != null) {
      CompoundTag backpointTag = new CompoundTag();
      backpointTag.putInt("BackX", backpoint.getX());
      backpointTag.putInt("BackY", backpoint.getY());
      backpointTag.putInt("BackZ", backpoint.getZ());
      tag.put("Backpoint", backpointTag);
    }

    // 存储Buff数据
    /*ListTag buffListTag = new ListTag();
    for (TerritoryBuff buff : buffs.values()) {
        buffListTag.add(buff.toNBT());
    }
    tag.put("Buffs", buffListTag);*/

    ListTag buffListTag = new ListTag();
    for (TerritoryBuff buff : territoryBuffs) {
      buffListTag.add(buff.toNBT());
    }
    tag.put("TerritoryBuffs", buffListTag);

    return tag;
  }

  public static Territory fromNBT(CompoundTag tag) {
    UUID territoryID = tag.getUUID("TerritoryID"); // 从 NBT 加载 UUID
    UUID ownerUUID = tag.getUUID("OwnerUUID");
    String ownerName = tag.getString("OwnerName");
    String name = tag.getString("Name");
    int x1 = tag.getInt("X1");
    int y1 = tag.getInt("Y1");
    int z1 = tag.getInt("Z1");
    int x2 = tag.getInt("X2");
    int y2 = tag.getInt("Y2");
    int z2 = tag.getInt("Z2");

    ResourceKey<Level> dimension =
        ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension")));

    Territory territory =
        new Territory(
            territoryID, name, ownerUUID, ownerName, x1, y1, z1, x2, y2, z2, null, dimension);

    // 字段缺失兼容为空；字段存在时严格验证，禁止静默折叠损坏成员。
    if (tag.contains("AuthorizedPlayers")) {
      Tag encodedPlayers = tag.get("AuthorizedPlayers");
      if (!(encodedPlayers instanceof ListTag authorizedPlayersTag)) {
        throw new IllegalArgumentException("AuthorizedPlayers must be a list");
      }
      Set<UUID> loadedPlayers = new HashSet<>();
      for (Tag playerTag : authorizedPlayersTag) {
        if (!(playerTag instanceof CompoundTag playerCompound)
            || !playerCompound.hasUUID("PlayerUUID")
            || !playerCompound.contains("PlayerName", Tag.TAG_STRING)) {
          throw new IllegalArgumentException("Malformed authorized member");
        }
        UUID playerUUID = playerCompound.getUUID("PlayerUUID");
        String playerName = playerCompound.getString("PlayerName").trim();
        if (playerUUID.equals(ownerUUID)
            || !loadedPlayers.add(playerUUID)
            || playerName.isEmpty()
            || playerName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH) {
          throw new IllegalArgumentException("Invalid authorized member");
        }
        territory.authorizedPlayers.add(new PlayerInfo(playerUUID, playerName));
      }
      if (territory.authorizedPlayers.size() != authorizedPlayersTag.size()) {
        throw new IllegalArgumentException("Authorized member count mismatch");
      }
    }

    if (tag.contains("Permissions", Tag.TAG_COMPOUND)) {
      CompoundTag permissionsTag = tag.getCompound("Permissions");
      for (TerritoryPermissionAction action : TerritoryPermissionAction.values()) {
        if (permissionsTag.contains(action.name(), Tag.TAG_STRING)) {
          try {
            territory.setPermissionLevel(
                action, TerritoryPermissionLevel.valueOf(permissionsTag.getString(action.name())));
          } catch (IllegalArgumentException ignored) {
            territory.setPermissionLevel(action, TerritoryPermissionLevel.MEMBERS);
          }
        }
      }
    }

    // 加载回城点
    if (tag.contains("Backpoint", Tag.TAG_COMPOUND)) {
      CompoundTag backpointTag = tag.getCompound("Backpoint");
      BlockPos backpoint =
          new BlockPos(
              backpointTag.getInt("BackX"),
              backpointTag.getInt("BackY"),
              backpointTag.getInt("BackZ"));
      territory.setBackpoint(backpoint);
    }

    ListTag buffsTag = tag.getList("TerritoryBuffs", Tag.TAG_COMPOUND);
    for (Tag buffTag : buffsTag) {
      TerritoryBuff buff = TerritoryBuff.fromNBT((CompoundTag) buffTag);
      territory.territoryBuffs.add(buff);
    }

    return territory;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Territory that = (Territory) obj;
    return Objects.equals(territoryID, that.territoryID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(territoryID);
  }
}
