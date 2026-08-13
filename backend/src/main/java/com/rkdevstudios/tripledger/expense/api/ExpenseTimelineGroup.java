package com.rkdevstudios.tripledger.expense.api;

import java.time.LocalDate;
import java.util.List;

public record ExpenseTimelineGroup(
    LocalDate date,
    List<ExpenseTimelineItem> expenses
) {}
