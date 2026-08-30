package com.mo.economy_system.ui.recycle;
import com.mo.economy_system.common.network.RecycleDataResponseMessage;
public sealed interface RecycleCenterEvent permits RecycleCenterEvent.Initialize, RecycleCenterEvent.DataLoaded,
    RecycleCenterEvent.DataFailed, RecycleCenterEvent.Selected, RecycleCenterEvent.AmountChanged,
    RecycleCenterEvent.ActionClicked, RecycleCenterEvent.Tick {
  record Initialize(long nowNanos) implements RecycleCenterEvent {}
  record DataLoaded(RecycleDataResponseMessage response) implements RecycleCenterEvent {}
  record DataFailed(String errorKey) implements RecycleCenterEvent {}
  record Selected(String itemId) implements RecycleCenterEvent {}
  record AmountChanged(int amount) implements RecycleCenterEvent {}
  record ActionClicked(RecycleCenterAction action) implements RecycleCenterEvent {}
  record Tick(long nowNanos) implements RecycleCenterEvent {}
}
