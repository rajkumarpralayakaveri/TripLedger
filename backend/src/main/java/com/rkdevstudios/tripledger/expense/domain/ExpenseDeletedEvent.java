package com.rkdevstudios.tripledger.expense.domain;

public record ExpenseDeletedEvent(
    Expense expense,
    String reason,
    String actorUserId
) {}
