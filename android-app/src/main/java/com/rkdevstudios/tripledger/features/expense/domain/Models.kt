package com.rkdevstudios.tripledger.features.expense.domain

import java.math.BigDecimal
import java.time.LocalDate

data class ExpenseItem(
    val id: String,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val paidByName: String,
    val date: LocalDate,
    val categoryName: String,
    val categoryColor: String,
    val categoryIcon: String
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
