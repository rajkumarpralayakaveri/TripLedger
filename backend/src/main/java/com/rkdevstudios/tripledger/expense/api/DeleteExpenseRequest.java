package com.rkdevstudios.tripledger.expense.api;

import jakarta.validation.constraints.NotBlank;

public record DeleteExpenseRequest(
    @NotBlank(message = "Delete reason is required")
    String reason
) {}
