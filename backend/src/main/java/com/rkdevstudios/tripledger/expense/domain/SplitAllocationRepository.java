package com.rkdevstudios.tripledger.expense.domain;

import java.util.List;

public interface SplitAllocationRepository {
    SplitAllocation save(SplitAllocation allocation);
    List<SplitAllocation> findByExpenseId(String expenseId);
    List<SplitAllocation> findByUserId(String userId);
    void deleteByExpenseId(String expenseId);
}
