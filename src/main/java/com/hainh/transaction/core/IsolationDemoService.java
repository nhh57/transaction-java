package com.hainh.transaction.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class IsolationDemoService {

    private static final Logger logger = LoggerFactory.getLogger(IsolationDemoService.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService; // Để sử dụng các phương thức tạo/tìm account cơ bản

    /**
     * Phương thức này sẽ update balance của một account sau một khoảng thời gian.
     * Dùng để mô phỏng một transaction khác đang thay đổi dữ liệu.
     */
    @Transactional
    public void updateAccountBalance(Long accountId, Double newBalance, long delayMillis) {
        logger.info("[Txn-Update] Starting update for account ID: {} to balance: {}", accountId, newBalance);
        try {
            Optional<Account> optionalAccount = accountRepository.findById(accountId);
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();
                logger.info("[Txn-Update] Account {} current balance: {}", accountId, account.getBalance());
                TimeUnit.MILLISECONDS.sleep(delayMillis); // Simulate long running operation
                account.setBalance(newBalance);
                accountRepository.save(account);
                logger.info("[Txn-Update] Account {} balance updated to: {}", accountId, newBalance);
            } else {
                logger.warn("[Txn-Update] Account ID {} not found for update.", accountId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[Txn-Update] Update interrupted for account ID: {}", accountId, e);
        } catch (Exception e) {
            logger.error("[Txn-Update] Error updating account ID: {}", accountId, e);
            throw e;
        }
    }

    /**
     * Mô phỏng Dirty Read:
     * Transaction A: Cập nhật balance nhưng chưa commit.
     * Transaction B: Đọc balance chưa commit của A.
     */
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Double demonstrateDirtyRead_Reader(Long accountId, long delayMillis) {
        logger.info("[DirtyRead-Reader] Starting to read account ID: {} with READ_UNCOMMITTED", accountId);
        try {
            Optional<Account> optionalAccount = accountRepository.findById(accountId);
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();
                Double balance = account.getBalance();
                logger.info("[DirtyRead-Reader] Read balance: {} for account ID: {}", balance, accountId);
                TimeUnit.MILLISECONDS.sleep(delayMillis); // Giữ transaction mở
                
                // Đọc lại lần nữa để thấy giá trị có thể đã bị rollback
                optionalAccount = accountRepository.findById(accountId);
                if (optionalAccount.isPresent()) {
                    account = optionalAccount.get();
                    Double secondReadBalance = account.getBalance();
                    logger.info("[DirtyRead-Reader] Second read balance: {} for account ID: {}", secondReadBalance, accountId);
                    return secondReadBalance;
                }
                return balance;
            } else {
                logger.warn("[DirtyRead-Reader] Account ID {} not found.", accountId);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[DirtyRead-Reader] Reader interrupted for account ID: {}", accountId, e);
            return null;
        } catch (Exception e) {
            logger.error("[DirtyRead-Reader] Error reading account ID: {}", accountId, e);
            throw e;
        }
    }

    /**
     * Mô phỏng Non-Repeatable Read:
     * Transaction A: Đọc balance 2 lần.
     * Transaction B: Cập nhật và commit balance giữa 2 lần đọc của A.
     *
     * Phương thức này sẽ đọc 2 lần với delay ở giữa.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED) // Mặc định đã là READ_COMMITTED
    public Double[] demonstrateNonRepeatableRead_Reader(Long accountId, long delayMillis) {
        logger.info("[NonRepeatableRead-Reader] Starting to read account ID: {} with READ_COMMITTED", accountId);
        Double[] balances = new Double[2];
        try {
            Optional<Account> optionalAccount = accountRepository.findById(accountId);
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();
                balances[0] = account.getBalance();
                logger.info("[NonRepeatableRead-Reader] First read balance: {} for account ID: {}", balances[0], accountId);

                TimeUnit.MILLISECONDS.sleep(delayMillis); // Cho phép transaction khác commit

                optionalAccount = accountRepository.findById(accountId);
                if (optionalAccount.isPresent()) {
                    account = optionalAccount.get();
                    balances[1] = account.getBalance();
                    logger.info("[NonRepeatableRead-Reader] Second read balance: {} for account ID: {}", balances[1], accountId);
                }
                return balances;
            } else {
                logger.warn("[NonRepeatableRead-Reader] Account ID {} not found.", accountId);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[NonRepeatableRead-Reader] Reader interrupted for account ID: {}", accountId, e);
            return null;
        } catch (Exception e) {
            logger.error("[NonRepeatableRead-Reader] Error reading account ID: {}", accountId, e);
            throw e;
        }
    }

    /**
     * Mô phỏng Phantom Read:
     * Transaction A: Đếm số lượng account theo điều kiện 2 lần.
     * Transaction B: Thêm/xóa account giữa 2 lần đếm của A.
     *
     * Phương thức này sẽ đếm 2 lần với delay ở giữa.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ) // REPEATABLE_READ vẫn có thể bị Phantom Read
    public Long[] demonstratePhantomRead_Reader(String ownerPrefix, long delayMillis) {
        logger.info("[PhantomRead-Reader] Starting to count accounts for owner prefix: {} with REPEATABLE_READ", ownerPrefix);
        Long[] counts = new Long[2];
        try {
            counts[0] = accountRepository.countByOwnerStartingWith(ownerPrefix);
            logger.info("[PhantomRead-Reader] First count for owner prefix '{}': {}", ownerPrefix, counts[0]);

            TimeUnit.MILLISECONDS.sleep(delayMillis); // Cho phép transaction khác thêm/xóa account

            counts[1] = accountRepository.countByOwnerStartingWith(ownerPrefix);
            logger.info("[PhantomRead-Reader] Second count for owner prefix '{}': {}", ownerPrefix, counts[1]);
            return counts;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[PhantomRead-Reader] Reader interrupted for owner prefix: {}", ownerPrefix, e);
            return null;
        } catch (Exception e) {
            logger.error("[PhantomRead-Reader] Error counting accounts for owner prefix: {}", ownerPrefix, e);
            throw e;
        }
    }

    /**
     * Phương thức hỗ trợ cho Phantom Read: Thêm một account mới.
     */
    @Transactional
    public Account addAccountForPhantomRead(String owner, Double balance) {
        logger.info("[PhantomRead-Writer] Adding new account: {} with balance {}", owner, balance);
        return accountService.createAccount(owner, balance);
    }

    /**
     * Phương thức hỗ trợ cho Phantom Read: Xóa một account.
     */
    @Transactional
    public void deleteAccountForPhantomRead(Long accountId) {
        logger.info("[PhantomRead-Writer] Deleting account ID: {}", accountId);
        accountRepository.deleteById(accountId);
    }

    /**
     * Phương thức đọc với Isolation.SERIALIZABLE để ngăn chặn tất cả các vấn đề.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Double[] demonstrateSerializable_Reader(Long accountId, long delayMillis) {
        logger.info("[Serializable-Reader] Starting to read account ID: {} with SERIALIZABLE", accountId);
        Double[] balances = new Double[2];
        try {
            Optional<Account> optionalAccount = accountRepository.findById(accountId);
            if (optionalAccount.isPresent()) {
                Account account = optionalAccount.get();
                balances[0] = account.getBalance();
                logger.info("[Serializable-Reader] First read balance: {} for account ID: {}", balances[0], accountId);

                TimeUnit.MILLISECONDS.sleep(delayMillis); // Cho phép transaction khác commit

                optionalAccount = accountRepository.findById(accountId);
                if (optionalAccount.isPresent()) {
                    account = optionalAccount.get();
                    balances[1] = account.getBalance();
                    logger.info("[Serializable-Reader] Second read balance: {} for account ID: {}", balances[1], accountId);
                }
                return balances;
            } else {
                logger.warn("[Serializable-Reader] Account ID {} not found.", accountId);
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("[Serializable-Reader] Reader interrupted for account ID: {}", accountId, e);
            return null;
        } catch (Exception e) {
            logger.error("[Serializable-Reader] Error reading account ID: {}", accountId, e);
            throw e;
        }
    }
}