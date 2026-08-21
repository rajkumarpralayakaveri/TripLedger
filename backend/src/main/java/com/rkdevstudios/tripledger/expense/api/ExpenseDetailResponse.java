package com.rkdevstudios.tripledger.expense.api;

import com.rkdevstudios.tripledger.expense.domain.Expense;
import java.util.List;

public record ExpenseDetailResponse(
    Expense expense,
    List<SplitAllocationDto> splitAllocations
) {}
