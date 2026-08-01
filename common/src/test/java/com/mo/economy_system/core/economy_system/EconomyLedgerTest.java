package com.mo.economy_system.core.economy_system;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyLedgerTest {
    @Test
    void balanceRulesAndLogOrderingMatchBaseline() {
        AtomicInteger dirtyCalls = new AtomicInteger();
        EconomyLedger ledger = new EconomyLedger(dirtyCalls::incrementAndGet);
        UUID player = UUID.randomUUID();

        assertFalse(ledger.addBalance(player, 0));
        assertFalse(ledger.minBalance(player, 1));
        assertTrue(ledger.addBalance(player, 100, "交易", "收入"));
        assertTrue(ledger.minBalance(player, 25, "交易", "支出"));
        assertEquals(75, ledger.getBalance(player));

        List<BalanceLogEntry> logs = ledger.getBalanceLogs(player);
        assertEquals(2, logs.size());
        assertEquals(-25, logs.get(0).delta());
        assertEquals(100, logs.get(0).beforeBalance());
        assertEquals(75, logs.get(0).afterBalance());
        assertEquals(100, logs.get(1).delta());

        BalanceLogPage page = ledger.getBalanceLogs(player, "交易", 0, 1);
        assertEquals(2, page.total());
        assertEquals(1, page.logs().size());
        assertTrue(dirtyCalls.get() >= 2);
    }

    @Test
    void overflowClampsToMaximumBalance() {
        EconomyLedger ledger = new EconomyLedger(() -> {
        });
        UUID player = UUID.randomUUID();
        ledger.setBalance(player, EconomyLedger.MAX_BALANCE - 2);

        assertTrue(ledger.addBalance(player, 10));
        assertEquals(EconomyLedger.MAX_BALANCE, ledger.getBalance(player));
    }

    @Test
    void successfulTransferMovesTheFullAmountAndWritesBothLogsAtomically() {
        AtomicInteger dirtyCalls = new AtomicInteger();
        EconomyLedger ledger = new EconomyLedger(dirtyCalls::incrementAndGet);
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        ledger.setBalance(sender, 100);
        ledger.setBalance(recipient, 25);
        int dirtyBeforeTransfer = dirtyCalls.get();

        BalanceTransferResult result = ledger.transferBalance(
                sender,
                recipient,
                40,
                "transfer",
                "sent",
                "received"
        );

        assertEquals(BalanceTransferResult.SUCCESS, result);
        assertEquals(60, ledger.getBalance(sender));
        assertEquals(65, ledger.getBalance(recipient));
        assertEquals(dirtyBeforeTransfer + 1, dirtyCalls.get());

        BalanceLogEntry senderLog = ledger.getBalanceLogs(sender).get(0);
        BalanceLogEntry recipientLog = ledger.getBalanceLogs(recipient).get(0);
        assertEquals(-40, senderLog.delta());
        assertEquals(100, senderLog.beforeBalance());
        assertEquals(60, senderLog.afterBalance());
        assertEquals("sent", senderLog.reason());
        assertEquals(40, recipientLog.delta());
        assertEquals(25, recipientLog.beforeBalance());
        assertEquals(65, recipientLog.afterBalance());
        assertEquals("received", recipientLog.reason());
    }

    @Test
    void failedTransfersNeverMutateAccountsOrMarkDataDirty() {
        AtomicInteger dirtyCalls = new AtomicInteger();
        EconomyLedger ledger = new EconomyLedger(dirtyCalls::incrementAndGet);
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        ledger.setBalance(sender, 100);
        ledger.setBalance(recipient, EconomyLedger.MAX_BALANCE - 5);
        int dirtyBeforeFailures = dirtyCalls.get();

        assertEquals(BalanceTransferResult.INVALID_AMOUNT, ledger.transferBalance(
                sender, recipient, 0, "transfer", "sent", "received"
        ));
        assertEquals(BalanceTransferResult.SAME_ACCOUNT, ledger.transferBalance(
                sender, sender, 1, "transfer", "sent", "received"
        ));
        assertEquals(BalanceTransferResult.INSUFFICIENT_FUNDS, ledger.transferBalance(
                sender, recipient, 101, "transfer", "sent", "received"
        ));
        assertEquals(BalanceTransferResult.RECIPIENT_BALANCE_LIMIT, ledger.transferBalance(
                sender, recipient, 10, "transfer", "sent", "received"
        ));

        assertEquals(100, ledger.getBalance(sender));
        assertEquals(EconomyLedger.MAX_BALANCE - 5, ledger.getBalance(recipient));
        assertEquals(dirtyBeforeFailures, dirtyCalls.get());
    }

    @Test
    void snapshotIsDeeplyImmutableAndRestoreDoesNotMarkDirty() {
        EconomyLedger source = new EconomyLedger(() -> {
        });
        UUID player = UUID.randomUUID();
        source.setBalance(player, 42);
        source.storeOfflineMessage(player, "hello");
        EconomyLedger.Snapshot snapshot = source.snapshot();

        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.accounts().put(UUID.randomUUID(), 1));
        assertThrows(UnsupportedOperationException.class, () ->
                snapshot.offlineMessages().get(player).add("mutate"));

        AtomicInteger dirtyCalls = new AtomicInteger();
        EconomyLedger restored = new EconomyLedger(dirtyCalls::incrementAndGet);
        restored.restore(snapshot);
        assertEquals(0, dirtyCalls.get());
        assertEquals(42, restored.getBalance(player));
        assertEquals(List.of("hello"), restored.getOfflineMessages(player));
        assertEquals(1, dirtyCalls.get());
    }

    @Test void exactMutationsNeverClampAndWriteOneLog() {
        AtomicInteger dirty=new AtomicInteger();EconomyLedger ledger=new EconomyLedger(dirty::incrementAndGet);UUID player=UUID.randomUUID();
        assertEquals(BalanceMutationResult.INVALID_AMOUNT,ledger.creditExact(player,0,"market","bad"));
        assertEquals(BalanceMutationResult.SUCCESS,ledger.creditExact(player,100,"market","credit"));
        assertEquals(BalanceMutationResult.SUCCESS,ledger.debitExact(player,40,"market","debit"));
        assertEquals(60,ledger.getBalance(player));assertEquals(2,ledger.getBalanceLogs(player).size());assertEquals(2,dirty.get());
        assertEquals(BalanceMutationResult.INSUFFICIENT_FUNDS,ledger.debitExact(player,61,"market","bad"));assertEquals(60,ledger.getBalance(player));
        ledger.restore(new EconomyLedger.Snapshot(Map.of(player,EconomyLedger.MAX_BALANCE-5),Map.of(),Map.of()));
        assertEquals(BalanceMutationResult.SUCCESS,ledger.creditExact(player,5,"market","limit"));assertEquals(EconomyLedger.MAX_BALANCE,ledger.getBalance(player));
        List<BalanceLogEntry> before=ledger.getBalanceLogs(player);int dirtyBefore=dirty.get();
        assertEquals(BalanceMutationResult.BALANCE_LIMIT,ledger.creditExact(player,1,"market","overflow"));
        assertEquals(EconomyLedger.MAX_BALANCE,ledger.getBalance(player));assertEquals(before,ledger.getBalanceLogs(player));assertEquals(dirtyBefore,dirty.get());
    }

    @Test void exactMutationRestoresBalanceLogsAndAccountExistenceWhenDirtyFails() {
        UUID existing=UUID.randomUUID(),missing=UUID.randomUUID();EconomyLedger ledger=new EconomyLedger(()->{throw new IllegalStateException("dirty");});
        BalanceLogEntry oldLog=new BalanceLogEntry(1,"old","old",10,0,10);
        ledger.restore(new EconomyLedger.Snapshot(Map.of(existing,10),Map.of(),Map.of(existing,List.of(oldLog))));
        assertEquals(BalanceMutationResult.PERSIST_FAILED,ledger.creditExact(existing,5,"market","credit"));
        assertEquals(10,ledger.getBalance(existing));assertEquals(List.of(oldLog),ledger.getBalanceLogs(existing));
        assertEquals(BalanceMutationResult.PERSIST_FAILED,ledger.creditExact(missing,5,"market","credit"));
        assertFalse(ledger.snapshot().accounts().containsKey(missing));assertTrue(ledger.getBalanceLogs(missing).isEmpty());
    }

    @Test void exactTransferRestoresBothAccountsLogsAndExistenceWhenDirtyFails() {
        UUID sender=UUID.randomUUID(),recipient=UUID.randomUUID();BalanceLogEntry oldLog=new BalanceLogEntry(1,"old","old",20,0,20);
        AtomicInteger dirty=new AtomicInteger();EconomyLedger ledger=new EconomyLedger(()->{dirty.incrementAndGet();throw new IllegalStateException("dirty");});
        ledger.restore(new EconomyLedger.Snapshot(Map.of(sender,20),Map.of(),Map.of(sender,List.of(oldLog))));
        assertEquals(BalanceTransferResult.PERSIST_FAILED,ledger.transferExact(sender,recipient,10,"market","buy","sell"));
        assertEquals(20,ledger.getBalance(sender));assertFalse(ledger.snapshot().accounts().containsKey(recipient));
        assertEquals(List.of(oldLog),ledger.getBalanceLogs(sender));assertTrue(ledger.getBalanceLogs(recipient).isEmpty());assertEquals(1,dirty.get());
    }

    @Test void transferPreviewAcceptsExactLimitAndRejectsOverflow() {
        UUID sender=UUID.randomUUID(),recipient=UUID.randomUUID();EconomyLedger ledger=new EconomyLedger(()->{});
        ledger.restore(new EconomyLedger.Snapshot(Map.of(sender,20,recipient,EconomyLedger.MAX_BALANCE-10),Map.of(),Map.of()));
        assertEquals(BalanceTransferResult.SUCCESS,ledger.previewTransferExact(sender,recipient,10));
        assertEquals(BalanceTransferResult.RECIPIENT_BALANCE_LIMIT,ledger.previewTransferExact(sender,recipient,11));
    }

    @Test void concurrentExactTransfersDoNotLoseUpdates() throws InterruptedException {
        UUID sender=UUID.randomUUID(),recipient=UUID.randomUUID();EconomyLedger ledger=new EconomyLedger(()->{});
        ledger.restore(new EconomyLedger.Snapshot(Map.of(sender,1000),Map.of(),Map.of()));
        int workers=10,perWorker=50;CountDownLatch start=new CountDownLatch(1);List<Thread> threads=new java.util.ArrayList<>();
        for(int worker=0;worker<workers;worker++){Thread thread=new Thread(()->{try{start.await();for(int i=0;i<perWorker;i++)assertEquals(BalanceTransferResult.SUCCESS,ledger.transferExact(sender,recipient,1,"test","send","receive"));}catch(InterruptedException exception){Thread.currentThread().interrupt();throw new AssertionError(exception);}});threads.add(thread);thread.start();}
        start.countDown();for(Thread thread:threads)thread.join();assertEquals(500,ledger.getBalance(sender));assertEquals(500,ledger.getBalance(recipient));
    }
}
