package com.mo.economy_system.common.territory;
import java.util.*;
public final class TerritoryInviteDecisionService {
 public enum Result{ACCEPTED,DECLINED,NOT_FOUND,NOT_TARGET,TERRITORY_NOT_FOUND,OWNER_CHANGED,ALREADY_MEMBER,PERSIST_FAILED,STATE_UNKNOWN,BUSY,MULTIPLE_PENDING}
 public enum WriteResult{ADDED,TERRITORY_NOT_FOUND,OWNER_CHANGED,ALREADY_MEMBER,PERSIST_FAILED,STATE_UNKNOWN}
 public interface Repository{WriteResult authorize(UUID territoryId,UUID expectedOwner,UUID playerId,String playerName);}
 private final TerritoryInviteStore store;private final Repository repository;public TerritoryInviteDecisionService(TerritoryInviteStore s,Repository r){store=s;repository=r;}
 public Result accept(UUID id,UUID actor,String actorName,long tick){TerritoryInviteStore.ClaimResult c=store.claim(id,actor,tick);if(c!=TerritoryInviteStore.ClaimResult.CLAIMED)return map(c);TerritoryInvite i=store.find(id,tick).orElse(null);if(i==null){store.release(id);return Result.NOT_FOUND;}WriteResult w;try{w=repository.authorize(i.territoryId(),i.territoryOwnerId(),actor,actorName);}catch(RuntimeException e){store.release(id);return Result.PERSIST_FAILED;}if(w==WriteResult.PERSIST_FAILED){store.release(id);return Result.PERSIST_FAILED;}store.consume(id);return switch(w){case ADDED->Result.ACCEPTED;case TERRITORY_NOT_FOUND->Result.TERRITORY_NOT_FOUND;case OWNER_CHANGED->Result.OWNER_CHANGED;case ALREADY_MEMBER->Result.ALREADY_MEMBER;case STATE_UNKNOWN->Result.STATE_UNKNOWN;case PERSIST_FAILED->throw new AssertionError();};}
 public Result decline(UUID id,UUID actor,long tick){TerritoryInviteStore.ClaimResult c=store.claim(id,actor,tick);if(c!=TerritoryInviteStore.ClaimResult.CLAIMED)return map(c);store.consume(id);return Result.DECLINED;}
 public Optional<UUID> sole(UUID actor,long tick){List<TerritoryInvite> all=store.listForTarget(actor,tick);return all.size()==1?Optional.of(all.get(0).inviteId()):Optional.empty();}
 public int pending(UUID actor,long tick){return store.listForTarget(actor,tick).size();}
 private static Result map(TerritoryInviteStore.ClaimResult c){return switch(c){case NOT_FOUND->Result.NOT_FOUND;case NOT_TARGET->Result.NOT_TARGET;case BUSY->Result.BUSY;case CLAIMED->throw new AssertionError();};}
}
