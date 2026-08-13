package com.rkdevstudios.tripledger.expense.persistence;

import com.rkdevstudios.tripledger.expense.domain.ExpenseHistory;
import com.rkdevstudios.tripledger.expense.domain.ExpenseHistoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaExpenseHistoryRepository extends JpaRepository<ExpenseHistory, String>, ExpenseHistoryRepository {
    List<ExpenseHistory> findByExpenseId(String expenseId);
}
