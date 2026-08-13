package com.rkdevstudios.tripledger.expense.domain;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository {
    ExpenseCategory save(ExpenseCategory category);
    Optional<ExpenseCategory> findById(String id);
    List<ExpenseCategory> findByWorkspaceId(String workspaceId);
    List<ExpenseCategory> findByIsSystemCategoryTrue();
}
