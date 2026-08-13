package com.rkdevstudios.tripledger.workspace.api;

import jakarta.validation.constraints.Positive;

public record InviteRequest(
    @Positive(message = "maxUses must be positive")
    int maxUses,

    @Positive(message = "expirationSeconds must be positive")
    long expirationSeconds
) {}
