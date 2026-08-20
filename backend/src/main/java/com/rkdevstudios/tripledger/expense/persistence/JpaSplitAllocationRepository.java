package com.rkdevstudios.tripledger.expense.persistence;

import com.rkdevstudios.tripledger.expense.domain.SplitAllocation;
import com.rkdevstudios.tripledger.expense.domain.SplitAllocationRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaSplitAllocationRepository extends JpaRepository<SplitAllocation, String>, SplitAllocationRepository {
    List<SplitAllocation> findByExpenseId(String expenseId);
    List<SplitAllocation> findByUserId(String userId);
    void deleteByExpenseId(String expenseId);
}
