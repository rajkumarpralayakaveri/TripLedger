package com.rkdevstudios.tripledger.features.expense.domain

import java.math.BigDecimal
import java.time.LocalDate

data class ExpenseItem(
    val id: String,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val paidByUserId: String = "",
    val paidByName: String,
    val date: LocalDate,
    val categoryId: String = "",
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String,
    val expenseAt: String? = null,
    val receiptUrl: String? = null,
    val note: String? = null
)

data class ExpenseTimelineGroup(
    val date: LocalDate,
    val expenses: List<ExpenseItem>
)

data class ActivityFeedItem(
    val id: String,
    val message: String,
    val timestamp: String
)
