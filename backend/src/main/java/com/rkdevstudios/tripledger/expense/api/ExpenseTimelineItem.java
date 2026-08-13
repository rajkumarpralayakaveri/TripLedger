package com.rkdevstudios.tripledger.expense.api;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseTimelineItem(
    String id,
    String description,
    BigDecimal amount,
    String currency,
    String paidByUserId,
    String paidByName,
    String categoryId,
    String categoryName,
    String categoryIcon,
    String categoryColor,
    LocalDate expenseDate
) {}
