package com.rkdevstudios.tripledger.expense.domain;

public record ExpenseCreatedEvent(
    Expense expense
) {}
