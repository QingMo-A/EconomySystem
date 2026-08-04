package com.mo.economy_system.common.territory;

import java.util.*;

/** Bounded, expiring, server-session invitation state machine. */
public final class TerritoryInviteStore {
  public enum PutResult { CREATED, ALREADY_PENDING, ID_COLLISION, FULL }
  enum ClaimStatus { CLAIMED, NOT_FOUND, NOT_TARGET, BUSY }
  enum CompleteResult { COMPLETED, INVALID_CLAIM }
  enum ReleaseResult { RELEASED, EXPIRED, INVALID_CLAIM }
  public enum SoleStatus { NONE, SOLE, MULTIPLE }
  public record Key(UUID targetPlayerId, UUID territoryId) {
    public Key { Objects.requireNonNull(targetPlayerId); Objects.requireNonNull(territoryId); }
  }
  record Claim(long token, TerritoryInvite invite) {
    Claim { if (token <= 0) throw new IllegalArgumentException("token"); Objects.requireNonNull(invite); }
  }
  record ClaimResult(ClaimStatus status, Claim claim) {
    ClaimResult { Objects.requireNonNull(status); if ((status == ClaimStatus.CLAIMED) != (claim != null)) throw new IllegalArgumentException("claim/status"); }
  }
  public record SoleResult(SoleStatus status, UUID inviteId) {
    public SoleResult { Objects.requireNonNull(status); if ((status == SoleStatus.SOLE) != (inviteId != null)) throw new IllegalArgumentException("id/status"); }
  }

  private final int capacity;
  private final Map<UUID, TerritoryInvite> invites = new HashMap<>();
  private final Map<Key, UUID> keys = new HashMap<>();
  private final Map<UUID, Long> processing = new HashMap<>();
  private long lastTick = -1;
  private long nextToken = 1;

  public TerritoryInviteStore() { this(4096); }
  public TerritoryInviteStore(int capacity) { if (capacity < 1) throw new IllegalArgumentException("capacity"); this.capacity = capacity; }

  public synchronized PutResult put(TerritoryInvite invite, long tick) {
    Objects.requireNonNull(invite, "invite");
    if (tick < 0 || invite.createdTick() > tick || tick >= invite.expiresTick()) throw new IllegalArgumentException("invite/tick");
    cleanup(tick);
    if (invites.containsKey(invite.inviteId())) return PutResult.ID_COLLISION;
    Key key = new Key(invite.targetPlayerId(), invite.territoryId());
    if (keys.containsKey(key)) return PutResult.ALREADY_PENDING;
    if (invites.size() >= capacity) return PutResult.FULL;
    invites.put(invite.inviteId(), invite); keys.put(key, invite.inviteId());
    return PutResult.CREATED;
  }

  public synchronized Optional<TerritoryInvite> find(UUID id, long tick) { Objects.requireNonNull(id); cleanup(tick); return Optional.ofNullable(invites.get(id)); }
  public synchronized List<TerritoryInvite> listForTarget(UUID target, long tick) { Objects.requireNonNull(target); cleanup(tick); return invites.values().stream().filter(i -> i.targetPlayerId().equals(target)).sorted(Comparator.comparing(TerritoryInvite::createdTick)).toList(); }
  public synchronized SoleResult resolveSole(UUID target, long tick) { List<TerritoryInvite> found=listForTarget(target,tick); return found.isEmpty()?new SoleResult(SoleStatus.NONE,null):found.size()==1?new SoleResult(SoleStatus.SOLE,found.get(0).inviteId()):new SoleResult(SoleStatus.MULTIPLE,null); }

  synchronized ClaimResult claim(UUID id, UUID actor, long tick) {
    Objects.requireNonNull(id); Objects.requireNonNull(actor); cleanup(tick);
    TerritoryInvite invite=invites.get(id); if(invite==null||tick>=invite.expiresTick())return new ClaimResult(ClaimStatus.NOT_FOUND,null);
    if(!invite.targetPlayerId().equals(actor))return new ClaimResult(ClaimStatus.NOT_TARGET,null);
    if(processing.containsKey(id))return new ClaimResult(ClaimStatus.BUSY,null);
    long token=nextToken++; if(nextToken<=0)nextToken=1; processing.put(id,token);
    return new ClaimResult(ClaimStatus.CLAIMED,new Claim(token,invite));
  }

  synchronized CompleteResult complete(Claim claim) {
    if (!valid(claim)) return CompleteResult.INVALID_CLAIM;
    TerritoryInvite invite=invites.remove(claim.invite().inviteId()); processing.remove(invite.inviteId());
    keys.remove(new Key(invite.targetPlayerId(),invite.territoryId()),invite.inviteId());
    return CompleteResult.COMPLETED;
  }
  synchronized ReleaseResult release(Claim claim) {
    if (!valid(claim)) return ReleaseResult.INVALID_CLAIM;
    processing.remove(claim.invite().inviteId());
    if(lastTick>=claim.invite().expiresTick()){remove(claim.invite());return ReleaseResult.EXPIRED;}
    return ReleaseResult.RELEASED;
  }
  public synchronized int size(long tick){cleanup(tick);return invites.size();}
  synchronized boolean consistent(){return invites.size()==keys.size()&&processing.keySet().stream().allMatch(invites::containsKey)&&invites.values().stream().allMatch(i->Objects.equals(keys.get(new Key(i.targetPlayerId(),i.territoryId())),i.inviteId()));}
  private boolean valid(Claim claim){return claim!=null&&Objects.equals(processing.get(claim.invite().inviteId()),claim.token())&&Objects.equals(invites.get(claim.invite().inviteId()),claim.invite());}
  private void remove(TerritoryInvite invite){invites.remove(invite.inviteId());processing.remove(invite.inviteId());keys.remove(new Key(invite.targetPlayerId(),invite.territoryId()),invite.inviteId());}
  private void cleanup(long tick){if(tick<0)throw new IllegalArgumentException("tick");if(lastTick>=0&&tick<lastTick){invites.clear();keys.clear();processing.clear();}lastTick=tick;for(TerritoryInvite i:new ArrayList<>(invites.values()))if(!processing.containsKey(i.inviteId())&&tick>=i.expiresTick())remove(i);}
}
