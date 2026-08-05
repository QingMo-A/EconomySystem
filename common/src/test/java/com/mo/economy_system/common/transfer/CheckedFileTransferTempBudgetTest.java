package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CheckedFileTransferTempBudgetTest {
  @Test
  void reservesCountAndBytesAtomically() {
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(2, 10);

    CheckedFileTransferTempBudget.Reservation first = budget.reserve(6);
    assertTrue(first != null);
    assertEquals(1, budget.reservedFiles());
    assertEquals(6, budget.reservedBytes());

    CheckedFileTransferTempBudget.ReservationResult tooLarge = budget.tryReserve(5);
    assertEquals(
        CheckedFileTransferTempBudget.ResultCode.TEMP_STORAGE_LIMIT, tooLarge.code());
    assertEquals(1, budget.reservedFiles());
    assertEquals(6, budget.reservedBytes());

    CheckedFileTransferTempBudget.Reservation zero = budget.reserve(0);
    assertTrue(zero != null);
    assertEquals(2, budget.reservedFiles());
    assertEquals(6, budget.reservedBytes());
    assertNull(budget.reserve(1));
  }

  @Test
  void releaseIsOneShotAndClearIsIdempotent() {
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(4, 100);
    CheckedFileTransferTempBudget.Reservation reservation = budget.reserve(25);

    assertTrue(budget.release(reservation));
    assertFalse(budget.release(reservation));
    assertFalse(reservation.release());
    assertEquals(0, budget.reservedFiles());
    assertEquals(0, budget.reservedBytes());

    CheckedFileTransferTempBudget.Reservation second = budget.reserve(30);
    budget.clear();
    assertTrue(second.isReleased());
    assertEquals(0, budget.reservedFiles());
    assertEquals(0, budget.reservedBytes());
    budget.clear();
    assertFalse(second.release());
  }

  @Test
  void rejectsNegativeAndOverflowWithoutMutation() {
    CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(2, 10);
    CheckedFileTransferTempBudget.ReservationResult negative = budget.tryReserve(-1);
    assertEquals(CheckedFileTransferTempBudget.ResultCode.INVALID_BYTES, negative.code());
    CheckedFileTransferTempBudget.ReservationResult overflow = budget.tryReserve(Long.MAX_VALUE);
    assertEquals(CheckedFileTransferTempBudget.ResultCode.TEMP_STORAGE_LIMIT, overflow.code());
    assertEquals(0, budget.reservedFiles());
    assertEquals(0, budget.reservedBytes());
  }
}
