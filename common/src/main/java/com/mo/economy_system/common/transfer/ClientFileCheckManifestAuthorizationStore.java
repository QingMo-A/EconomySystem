package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.*;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.*;

/** Server-scoped, content-free authorization derived only after protocol-25 delivery succeeds. */
public final class ClientFileCheckManifestAuthorizationStore {
  public record Scope(UUID targetPlayerId,UUID requesterPlayerId,ClientFileCheckType checkType){public Scope{Objects.requireNonNull(targetPlayerId);Objects.requireNonNull(requesterPlayerId);Objects.requireNonNull(checkType);}}
  public record Key(UUID targetPlayerId,UUID requesterPlayerId,ClientFileCheckType checkType,String fileName){public Key{fileName=CheckedFileTransferValidation.fileName(fileName);}public Scope scope(){return new Scope(targetPlayerId,requesterPlayerId,checkType);}}
  public record Authorization(long expectedByteLength,String expectedSha256,long createdTick,long expiresTick){public Authorization{if(expectedByteLength<0||expectedByteLength>EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES||createdTick<0||expiresTick<=createdTick||expiresTick-createdTick>EconomyNetworkLimits.FILE_TRANSFER_TTL_TICKS)throw new IllegalArgumentException("authorization");expectedSha256=CheckedFileTransferValidation.sha256(expectedSha256);}}
  private final int capacity; private final LinkedHashMap<Key,Authorization> entries=new LinkedHashMap<>(); private long lastTick=-1;
  public ClientFileCheckManifestAuthorizationStore(){this(EconomyNetworkLimits.MAX_PENDING_FILE_TRANSFERS*4);} public ClientFileCheckManifestAuthorizationStore(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");this.capacity=capacity;}
  public synchronized void replace(UUID target,UUID requester,ClientFileCheckResult result,long tick){cleanup(tick);Scope scope=new Scope(target,requester,result.checkType());entries.keySet().removeIf(k->k.scope().equals(scope));if(result.status()!=ClientFileCheckStatus.SUCCESS&&result.status()!=ClientFileCheckStatus.TRUNCATED)return;for(ClientFileCheckEntry e:result.files()){if(entries.size()>=capacity)break;if(e.size()<=EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES)entries.put(new Key(target,requester,result.checkType(),e.fileName()),new Authorization(e.size(),e.sha256(),tick,tick+EconomyNetworkLimits.FILE_TRANSFER_TTL_TICKS));}}
  public synchronized Optional<Authorization> find(Key key,long tick){cleanup(tick);return Optional.ofNullable(entries.get(Objects.requireNonNull(key)));}
  public synchronized void removeScope(Scope scope,long tick){cleanup(tick);entries.keySet().removeIf(k->k.scope().equals(scope));}
  public synchronized int size(long tick){cleanup(tick);return entries.size();}
  public synchronized void clear(){entries.clear();lastTick=-1;}
  private void cleanup(long tick){if(tick<0)throw new IllegalArgumentException("tick");if(lastTick>=0&&tick<lastTick)entries.clear();lastTick=tick;entries.entrySet().removeIf(e->tick>=e.getValue().expiresTick());}
}
