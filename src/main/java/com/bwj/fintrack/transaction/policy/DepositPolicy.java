package com.bwj.fintrack.transaction.policy;

import com.bwj.fintrack.account.entity.Account;
import java.math.BigDecimal;

public interface DepositPolicy {
    void validate(Account account, BigDecimal amount);
}
