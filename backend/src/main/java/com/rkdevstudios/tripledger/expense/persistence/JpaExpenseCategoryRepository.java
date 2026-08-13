package com.rkdevstudios.tripledger.expense.persistence;

import com.rkdevstudios.tripledger.expense.domain.ExpenseCategory;
import com.rkdevstudios.tripledger.expense.domain.ExpenseCategoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaExpenseCategoryRepository extends JpaRepository<ExpenseCategory, String>, ExpenseCategoryRepository {
    List<ExpenseCategory> findByIsSystemCategoryTrue();
}
