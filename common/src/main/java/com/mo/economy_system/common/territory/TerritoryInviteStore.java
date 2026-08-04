package com.mo.economy_system.common.territory;

import java.util.*;

/** Bounded, expiring, server-session invitation store. All methods are atomic. */
public final class TerritoryInviteStore {
  public enum PutResult { CREATED, ALREADY_PENDING, FULL }
  public enum ClaimResult { CLAIMED, NOT_FOUND, NOT_TARGET, BUSY }
  public record Key(UUID targetPlayerId, UUID territoryId) {}
  private final int capacity;
  private final Map<UUID,TerritoryInvite> invites=new HashMap<>();
  private final Map<Key,UUID> keys=new HashMap<>();
  private final Set<UUID> processing=new HashSet<>();
  private long lastTick=-1;
  public TerritoryInviteStore(){this(4096);} public TerritoryInviteStore(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");this.capacity=capacity;}
  public synchronized PutResult put(TerritoryInvite invite,long tick){cleanup(tick);Key k=new Key(invite.targetPlayerId(),invite.territoryId());if(keys.containsKey(k))return PutResult.ALREADY_PENDING;if(invites.size()>=capacity)return PutResult.FULL;invites.put(invite.inviteId(),invite);keys.put(k,invite.inviteId());return PutResult.CREATED;}
  public synchronized Optional<TerritoryInvite> find(UUID id,long tick){cleanup(tick);return Optional.ofNullable(invites.get(id));}
  public synchronized List<TerritoryInvite> listForTarget(UUID target,long tick){cleanup(tick);return invites.values().stream().filter(i->i.targetPlayerId().equals(target)).sorted(Comparator.comparing(TerritoryInvite::createdTick)).toList();}
  public synchronized ClaimResult claim(UUID id,UUID target,long tick){cleanup(tick);TerritoryInvite i=invites.get(id);if(i==null)return ClaimResult.NOT_FOUND;if(!i.targetPlayerId().equals(target))return ClaimResult.NOT_TARGET;if(!processing.add(id))return ClaimResult.BUSY;return ClaimResult.CLAIMED;}
  public synchronized void release(UUID id){processing.remove(id);}
  public synchronized boolean consume(UUID id){processing.remove(id);TerritoryInvite i=invites.remove(id);if(i==null)return false;keys.remove(new Key(i.targetPlayerId(),i.territoryId()),id);return true;}
  public synchronized int size(long tick){cleanup(tick);return invites.size();}
  private void cleanup(long tick){if(tick<0)throw new IllegalArgumentException("tick");if(lastTick>=0&&tick<lastTick){invites.clear();keys.clear();processing.clear();}lastTick=tick;Iterator<TerritoryInvite> it=invites.values().iterator();while(it.hasNext()){TerritoryInvite i=it.next();if(tick>=i.expiresTick()){it.remove();keys.remove(new Key(i.targetPlayerId(),i.territoryId()));processing.remove(i.inviteId());}}}
}
