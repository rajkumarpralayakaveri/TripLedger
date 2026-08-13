package com.rkdevstudios.tripledger.expense.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
    @NotBlank(message = "Category name is required")
    String name,

    @NotBlank(message = "Icon name is required")
    String icon,

    @NotBlank(message = "Color hex code is required")
    @Size(min = 7, max = 7, message = "Color must be a 7-character hex code (e.g. #FF9800)")
    String color
) {}
