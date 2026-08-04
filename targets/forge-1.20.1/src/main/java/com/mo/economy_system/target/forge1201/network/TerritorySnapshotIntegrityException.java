package com.mo.economy_system.target.forge1201.network;

/** Signals malformed authoritative territory persistence during an invitation lookup. */
final class TerritorySnapshotIntegrityException extends IllegalStateException {
  TerritorySnapshotIntegrityException(String message) {
    super(message);
  }
}
