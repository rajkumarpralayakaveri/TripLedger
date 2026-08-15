package com.rkdevstudios.tripledger.expense.domain;

public enum ActivityType {
    EXPENSE_CREATED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    MEMBER_JOINED,
    WORKSPACE_CREATED,
    SETTLEMENT_CONFIRMED,
    PAYMENT_SUBMITTED,
    PAYMENT_APPROVED,
    PAYMENT_REJECTED
}
