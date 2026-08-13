package com.rkdevstudios.tripledger.expense.domain;

import java.util.List;

public interface ExpenseHistoryRepository {
    ExpenseHistory save(ExpenseHistory history);
    List<ExpenseHistory> findByExpenseId(String expenseId);
}
