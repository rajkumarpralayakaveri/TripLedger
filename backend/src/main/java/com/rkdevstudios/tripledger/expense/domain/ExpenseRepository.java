package com.rkdevstudios.tripledger.expense.domain;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {
    Expense save(Expense expense);
    Optional<Expense> findById(String id);
    List<Expense> findByWorkspaceIdAndStatusNot(String workspaceId, ExpenseStatus status);
}
