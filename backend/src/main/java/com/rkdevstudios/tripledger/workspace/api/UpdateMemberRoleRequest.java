package com.rkdevstudios.tripledger.workspace.api;

import com.rkdevstudios.tripledger.workspace.domain.MemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Role must be specified")
        MemberRole role
) {}
