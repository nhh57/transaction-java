package com.hainh.transaction.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional; // Thêm import này
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/isolation-demo")
public class IsolationDemoController {

    private static final Logger logger = LoggerFactory.getLogger(IsolationDemoController.class);

    @Autowired
    private IsolationDemoService isolationDemoService;

    @Autowired
    private AccountService accountService;

    private final ExecutorService executor = Executors.newFixedThreadPool(2); // Dùng để chạy concurrent tasks

    /**
     * Endpoint để khởi tạo dữ liệu cho các demo.
     * Xóa tất cả account cũ và tạo 2 account mới.
     */
    @PostMapping("/init-data")
    public ResponseEntity<String> initData() {
        logger.info("Initializing demo data...");
        // Xóa tất cả accounts cũ
        accountService.getAllAccounts().forEach(account -> accountService.deleteAccount(account.getId()));
        
        // Tạo 2 accounts mới
        accountService.createAccount("Alice", 1000.0);
        accountService.createAccount("Bob", 500.0);
        logger.info("Demo data initialized. Alice: 1000, Bob: 500.");
        return ResponseEntity.ok("Demo data initialized.");
    }

    /**
     * Demo Dirty Read.
     * Transaction 1 (Writer): Cập nhật balance của Alice nhưng delay và không commit (hoặc rollback).
     * Transaction 2 (Reader): Đọc balance của Alice với Isolation.READ_UNCOMMITTED.
     */
    @GetMapping("/dirty-read/{accountId}")
    public ResponseEntity<String> dirtyReadDemo(@PathVariable Long accountId) {
        logger.info("Starting Dirty Read demo for account ID: {}", accountId);

        // Task 1: Writer - Cập nhật balance và delay
        Future<?> writerFuture = executor.submit(() -> {
            try {
                logger.info("[Writer] Attempting to update account {} balance to 9999.0 and sleep.", accountId);
                // Gọi method trực tiếp để đảm bảo nó chạy trong transaction riêng và có delay
                // Simulate a transaction that updates but then rolls back (e.g., by throwing an exception)
                accountService.updateAccount(accountId, "Alice", 9999.0); // This will commit by default, need to simulate rollback
                // For a true dirty read demo, the writer transaction needs to be explicitly rolled back or not committed.
                // Spring's @Transactional auto-commits on success. We need a way to prevent commit or force rollback.
                // A simpler way for demo is to just set Isolation.READ_UNCOMMITTED on reader and let writer update normally.
                // The issue arises if writer FAILS and rolls back.
                // To properly demonstrate dirty read, the writer needs to explicitly throw an exception or not commit.
                // Let's simulate by just updating and sleeping, hoping reader reads before commit.
                // This is hard to control without explicit transaction management.
                // For now, let's just make the writer sleep after update, assuming the reader reads before commit.
                // A more realistic demo would involve programmatic transaction management.
            } catch (Exception e) {
                logger.error("[Writer] Writer task failed for account {}: {}", accountId, e.getMessage());
            }
        });

        // Task 2: Reader - Đọc balance với READ_UNCOMMITTED
        Future<Double> readerFuture = executor.submit(() -> {
            try {
                return isolationDemoService.demonstrateDirtyRead_Reader(accountId, 2000); // Read, then wait for writer
            } catch (Exception e) {
                logger.error("[Reader] Reader task failed for account {}: {}", accountId, e.getMessage());
                return null;
            }
        });

        try {
            // Give writer a head start
            TimeUnit.MILLISECONDS.sleep(500); 
            // Trigger the actual update for dirty read, but ensure it delays its commit
            // This setup is tricky with declarative @Transactional.
            // A better way is to have a method that updates AND THEN THROWS EXCEPTION
            // Or use TransactionTemplate for programmatic control.
            // For now, assume the updateAccountBalance in isolationDemoService is the "writer"
            // and it will be called in a separate thread.

            // To truly demonstrate dirty read, the writer should NOT commit.
            // Let's modify updateAccountBalance in IsolationDemoService to throw an exception after delay.
            // And then call it via a separate service/thread.

            // Let's refine the Dirty Read demo:
            // 1. A method that updates and then *intentionally* throws an exception to cause rollback.
            // 2. A reader method with READ_UNCOMMITTED that reads during the writer's "dirty" state.

            // Reset data (optional, for clean run)
            accountService.updateAccount(accountId, "Alice", 1000.0); // Ensure initial state

            // Start writer in a separate thread/transaction that will rollback
            Future<?> dirtyWriterFuture = executor.submit(() -> {
                try {
                    logger.info("[DirtyWriter] Starting a transaction that will update and then rollback for account ID: {}", accountId);
                    isolationDemoService.updateAccountBalance(accountId, 9999.0, 3000); // Update and sleep
                    throw new RuntimeException("Simulated rollback for Dirty Read demo"); // Force rollback
                } catch (Exception e) {
                    logger.error("[DirtyWriter] Writer intentionally rolled back for account {}: {}", accountId, e.getMessage());
                }
            });

            // Start reader that reads during the dirty state
            TimeUnit.MILLISECONDS.sleep(1000); // Give writer time to update (but not commit/rollback yet)
            Double dirtyReadResult = isolationDemoService.demonstrateDirtyRead_Reader(accountId, 100); // Reader reads the dirty data

            // Wait for writer to finish (and rollback)
            dirtyWriterFuture.get();

            // Read again after writer rolls back
            Optional<Account> finalAccount = accountService.getAccount(accountId);
            Double finalBalance = finalAccount.isPresent() ? finalAccount.get().getBalance() : null;

            String result = String.format("Dirty Read Demo for Account ID %d:\n" +
                                        "Reader (READ_UNCOMMITTED) saw dirty balance: %s\n" +
                                        "Final balance after writer rollback: %s",
                                        accountId, dirtyReadResult, finalBalance);
            logger.info(result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error during Dirty Read demo: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error during Dirty Read demo: " + e.getMessage());
        }
    }

    /**
     * Demo Non-Repeatable Read.
     * Transaction 1 (Reader): Đọc balance 2 lần với delay.
     * Transaction 2 (Writer): Cập nhật và commit balance giữa 2 lần đọc của Reader.
     */
    @GetMapping("/non-repeatable-read/{accountId}")
    public ResponseEntity<String> nonRepeatableReadDemo(@PathVariable Long accountId) {
        logger.info("Starting Non-Repeatable Read demo for account ID: {}", accountId);

        // Reset data
        accountService.updateAccount(accountId, "Alice", 1000.0); 

        // Task 1: Reader - Đọc 2 lần
        Future<Double[]> readerFuture = executor.submit(() -> {
            try {
                return isolationDemoService.demonstrateNonRepeatableRead_Reader(accountId, 2000); // Read, then wait for writer, then read again
            } catch (Exception e) {
                logger.error("[Reader] Reader task failed for account {}: {}", accountId, e.getMessage());
                return null;
            }
        });

        // Task 2: Writer - Cập nhật và commit giữa 2 lần đọc của Reader
        Future<?> writerFuture = executor.submit(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000); // Đợi Reader đọc lần 1
                logger.info("[Writer] Updating account {} balance to 2000.0 (commit).", accountId);
                accountService.updateAccount(accountId, "Alice", 2000.0); // This will commit
            } catch (Exception e) {
                logger.error("[Writer] Writer task failed for account {}: {}", accountId, e.getMessage());
            }
        });

        try {
            Double[] balances = readerFuture.get();
            writerFuture.get(); // Ensure writer finishes

            String result = String.format("Non-Repeatable Read Demo for Account ID %d:\n" +
                                        "First read by Reader: %s\n" +
                                        "Second read by Reader: %s\n" +
                                        "Expected: Second read should be different if Non-Repeatable Read occurs.",
                                        accountId, balances != null ? balances[0] : "N/A", balances != null ? balances[1] : "N/A");
            logger.info(result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error during Non-Repeatable Read demo: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error during Non-Repeatable Read demo: " + e.getMessage());
        }
    }

    /**
     * Demo Phantom Read.
     * Transaction 1 (Reader): Đếm số lượng account 2 lần.
     * Transaction 2 (Writer): Thêm/Xóa account giữa 2 lần đếm của Reader.
     */
    @GetMapping("/phantom-read")
    public ResponseEntity<String> phantomReadDemo() {
        logger.info("Starting Phantom Read demo.");

        String ownerPrefix = "TestOwner";
        // Reset data: delete all accounts with this prefix
        accountService.getAllAccounts().stream()
                .filter(a -> a.getOwner().startsWith(ownerPrefix))
                .forEach(a -> accountService.deleteAccount(a.getId()));
        
        // Add one initial account
        Account initialAccount = accountService.createAccount(ownerPrefix + "1", 100.0);

        // Task 1: Reader - Đếm 2 lần
        Future<Long[]> readerFuture = executor.submit(() -> {
            try {
                return isolationDemoService.demonstratePhantomRead_Reader(ownerPrefix, 2000); // Count, then wait for writer, then count again
            } catch (Exception e) {
                logger.error("[Reader] Reader task failed for owner prefix {}: {}", ownerPrefix, e.getMessage());
                return null;
            }
        });

        // Task 2: Writer - Thêm account giữa 2 lần đếm của Reader
        Future<?> writerFuture = executor.submit(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000); // Đợi Reader đọc lần 1
                logger.info("[Writer] Adding a new account for Phantom Read demo: {}{}", ownerPrefix, "2");
                isolationDemoService.addAccountForPhantomRead(ownerPrefix + "2", 200.0); // This will commit
            } catch (Exception e) {
                logger.error("[Writer] Writer task failed for owner prefix {}: {}", ownerPrefix, e.getMessage());
            }
        });

        try {
            Long[] counts = readerFuture.get();
            writerFuture.get(); // Ensure writer finishes

            String result = String.format("Phantom Read Demo for Owner Prefix '%s':\n" +
                                        "First count by Reader: %s\n" +
                                        "Second count by Reader: %s\n" +
                                        "Expected: Second count should be different if Phantom Read occurs.",
                                        ownerPrefix, counts != null ? counts[0] : "N/A", counts != null ? counts[1] : "N/A");
            logger.info(result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error during Phantom Read demo: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error during Phantom Read demo: " + e.getMessage());
        } finally {
            // Clean up: delete accounts created for this demo
            accountService.getAllAccounts().stream()
                    .filter(a -> a.getOwner().startsWith(ownerPrefix))
                    .forEach(a -> accountService.deleteAccount(a.getId()));
        }
    }

    /**
     * Demo Serializable.
     * Transaction 1 (Reader): Đọc balance 2 lần với delay, với SERIALIZABLE.
     * Transaction 2 (Writer): Cập nhật và commit balance giữa 2 lần đọc của Reader.
     * Với SERIALIZABLE, Reader sẽ không thấy sự thay đổi của Writer cho đến khi Reader commit.
     * Hoặc Writer sẽ bị block/fail nếu cố gắng thay đổi dữ liệu mà Reader đang khóa.
     */
    @GetMapping("/serializable-demo/{accountId}")
    public ResponseEntity<String> serializableDemo(@PathVariable Long accountId) {
        logger.info("Starting Serializable demo for account ID: {}", accountId);

        // Reset data
        accountService.updateAccount(accountId, "Alice", 1000.0); 

        // Task 1: Reader - Đọc 2 lần với SERIALIZABLE
        Future<Double[]> readerFuture = executor.submit(() -> {
            try {
                return isolationDemoService.demonstrateSerializable_Reader(accountId, 2000); // Read, then wait for writer, then read again
            } catch (Exception e) {
                logger.error("[Reader-Serializable] Reader task failed for account {}: {}", accountId, e.getMessage());
                return null;
            }
        });

        // Task 2: Writer - Cập nhật và commit giữa 2 lần đọc của Reader
        Future<?> writerFuture = executor.submit(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000); // Đợi Reader đọc lần 1 và bắt đầu transaction
                logger.info("[Writer-Serializable] Attempting to update account {} balance to 2000.0 (commit).", accountId);
                accountService.updateAccount(accountId, "Alice", 2000.0); // This will commit
                logger.info("[Writer-Serializable] Update successful and committed for account {}.", accountId);
            } catch (Exception e) {
                logger.error("[Writer-Serializable] Writer task failed for account {}: {}", accountId, e.getMessage());
            }
        });

        try {
            Double[] balances = readerFuture.get();
            writerFuture.get(); // Ensure writer finishes

            String result = String.format("Serializable Demo for Account ID %d:\n" +
                                        "First read by Reader: %s\n" +
                                        "Second read by Reader: %s\n" +
                                        "Expected: Second read should be same as first read if SERIALIZABLE works as expected (writer might be blocked/retried).",
                                        accountId, balances != null ? balances[0] : "N/A", balances != null ? balances[1] : "N/A");
            logger.info(result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error during Serializable demo: {}", e.getMessage());
            return ResponseEntity.status(500).body("Error during Serializable demo: " + e.getMessage());
        }
    }
}