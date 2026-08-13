package com.rkdevstudios.tripledger.expense.api;

import java.util.List;

public record ExpenseTimelineResponse(
    List<ExpenseTimelineGroup> timeline
) {}
