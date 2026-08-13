package com.rkdevstudios.tripledger.expense.api;

public record CategoryResponse(
    String id,
    String workspaceId,
    String name,
    String icon,
    String color,
    boolean active,
    boolean isSystemCategory
) {}
