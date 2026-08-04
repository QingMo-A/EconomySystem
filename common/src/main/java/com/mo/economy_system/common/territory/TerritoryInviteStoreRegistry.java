package com.mo.economy_system.common.territory;
import java.util.*;
public final class TerritoryInviteStoreRegistry<K>{private final Map<K,TerritoryInviteStore> stores=new WeakHashMap<>();public synchronized TerritoryInviteStore get(K server){return stores.computeIfAbsent(Objects.requireNonNull(server),k->new TerritoryInviteStore());}public synchronized int serverCount(){return stores.size();}}
