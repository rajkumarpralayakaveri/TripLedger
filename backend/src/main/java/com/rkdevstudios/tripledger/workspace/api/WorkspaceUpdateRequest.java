package com.rkdevstudios.tripledger.workspace.api;

import com.rkdevstudios.tripledger.workspace.domain.WorkspaceStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkspaceUpdateRequest(
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    String description,

    LocalDate startDate,

    LocalDate endDate,

    @Positive(message = "Budget must be positive")
    BigDecimal budget,

    WorkspaceStatus status
) {}
