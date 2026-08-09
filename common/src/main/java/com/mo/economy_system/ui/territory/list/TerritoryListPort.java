package com.mo.economy_system.ui.territory.list;

/** Version boundary used by the common territory-list controller. */
public interface TerritoryListPort {
  long nextRequestId();

  void requestTerritories(long requestId);

  void submit(TerritoryListAction action, TerritoryListRow row);
}
