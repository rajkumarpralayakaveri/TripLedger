package com.rkdevstudios.tripledger.expense.domain;

public record ExpenseUpdatedEvent(
    Expense oldExpense,
    Expense newExpense
) {}
