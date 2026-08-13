package com.rkdevstudios.tripledger.expense.api;

import com.rkdevstudios.tripledger.expense.domain.SplitType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record CreateExpenseRequest(
    @NotBlank(message = "Payer ID is required")
    String paidByUserId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    String currency,

    @NotBlank(message = "Description is required")
    String description,

    @NotBlank(message = "Category ID is required")
    String categoryId,

    @NotNull(message = "Expense date is required")
    LocalDate expenseDate,

    @NotNull(message = "Split type is required")
    SplitType splitType,

    @NotEmpty(message = "Participant list cannot be empty")
    List<String> participantIds,

    Map<String, BigDecimal> splitValues
) {}
