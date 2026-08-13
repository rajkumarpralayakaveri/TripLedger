package com.rkdevstudios.tripledger.expense.persistence;

import com.rkdevstudios.tripledger.expense.domain.Expense;
import com.rkdevstudios.tripledger.expense.domain.ExpenseRepository;
import com.rkdevstudios.tripledger.expense.domain.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaExpenseRepository extends JpaRepository<Expense, String>, ExpenseRepository {
    List<Expense> findByWorkspaceIdAndStatusNot(String workspaceId, ExpenseStatus status);
}
