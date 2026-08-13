package com.rkdevstudios.tripledger.workspace.api;

import jakarta.validation.constraints.NotBlank;

public record JoinRequest(
    @NotBlank(message = "Invite token is required")
    String inviteToken
) {}
