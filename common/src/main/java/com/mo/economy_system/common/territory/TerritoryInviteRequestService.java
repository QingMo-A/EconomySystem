package com.mo.economy_system.common.territory;

import java.util.*;

public final class TerritoryInviteRequestService {
  public record Territory(UUID id,UUID ownerId,String name,Set<UUID> members){public Territory{members=Set.copyOf(members);}}
  public record Player(UUID id,String name){}
  public interface Repository{Optional<Territory> find(UUID id) throws Exception;}
  public interface Directory{Optional<Player> online(UUID id) throws Exception;}
  public interface Ids{UUID next();}
  public record Outcome(TerritoryInviteResult result,TerritoryInvite invite){}
  private final Repository repository;private final Directory directory;private final TerritoryInviteStore store;private final TerritoryInviteRateLimiter limiter;private final Ids ids;private final long lifetime;
  public TerritoryInviteRequestService(Repository r,Directory d,TerritoryInviteStore s,TerritoryInviteRateLimiter l,Ids ids){this(r,d,s,l,ids,1200);}
  public TerritoryInviteRequestService(Repository r,Directory d,TerritoryInviteStore s,TerritoryInviteRateLimiter l,Ids ids,long lifetime){repository=r;directory=d;store=s;limiter=l;this.ids=ids;this.lifetime=lifetime;}
  public Outcome create(UUID senderId,String senderName,UUID territoryId,UUID targetId,long tick){
    Objects.requireNonNull(senderId);Objects.requireNonNull(targetId);Objects.requireNonNull(territoryId);
    try{Territory t=repository.find(territoryId).orElse(null);if(t==null)return out(TerritoryInviteResult.TERRITORY_NOT_FOUND);if(!t.ownerId().equals(senderId))return out(TerritoryInviteResult.NO_PERMISSION);if(targetId.equals(senderId))return out(TerritoryInviteResult.CANNOT_INVITE_SELF);if(targetId.equals(t.ownerId()))return out(TerritoryInviteResult.CANNOT_INVITE_OWNER);if(t.members().contains(targetId))return out(TerritoryInviteResult.ALREADY_MEMBER);Player target=directory.online(targetId).orElse(null);if(target==null)return out(TerritoryInviteResult.TARGET_OFFLINE);if(!limiter.allowed(senderId,tick))return out(TerritoryInviteResult.RATE_LIMITED);TerritoryInvite invite=new TerritoryInvite(ids.next(),t.id(),t.ownerId(),senderId,targetId,t.name(),senderName,target.name(),tick,Math.addExact(tick,lifetime));TerritoryInviteStore.PutResult p=store.put(invite,tick);if(p==TerritoryInviteStore.PutResult.ALREADY_PENDING){limiter.record(senderId,tick);return out(TerritoryInviteResult.ALREADY_PENDING);}if(p==TerritoryInviteStore.PutResult.FULL)return out(TerritoryInviteResult.STORE_FULL);limiter.record(senderId,tick);return new Outcome(TerritoryInviteResult.SUCCESS,invite);}catch(Exception e){return out(TerritoryInviteResult.CREATE_FAILED);}}
  private static Outcome out(TerritoryInviteResult r){return new Outcome(r,null);}
}
