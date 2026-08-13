package com.rkdevstudios.tripledger.workspace.api;

import com.rkdevstudios.tripledger.common.ApiResponse;
import com.rkdevstudios.tripledger.common.ApiRoutes;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.workspace.application.WorkspaceService;
import com.rkdevstudios.tripledger.workspace.domain.InviteToken;
import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new SecurityException("User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Workspace>> createWorkspace(
            @Valid @RequestBody WorkspaceCreateRequest request
    ) {
        User user = getAuthenticatedUser();
        Workspace workspace = workspaceService.createWorkspace(
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate(),
                request.baseCurrency(),
                request.budget(),
                user.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(workspace));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Workspace>>> getWorkspaces() {
        User user = getAuthenticatedUser();
        List<Workspace> workspaces = workspaceService.getCallerWorkspaces(user.getId());
        return ResponseEntity.ok(ApiResponse.success(workspaces));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Workspace>> getWorkspaceById(@PathVariable("id") String id) {
        User user = getAuthenticatedUser();
        Workspace workspace = workspaceService.getWorkspaceById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(workspace));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Workspace>> updateWorkspace(
            @PathVariable("id") String id,
            @Valid @RequestBody WorkspaceUpdateRequest request
    ) {
        User user = getAuthenticatedUser();
        Workspace workspace = workspaceService.updateWorkspace(
                id,
                request.name(),
                request.description(),
                request.startDate(),
                request.endDate(),
                request.budget(),
                request.status(),
                user.getId()
        );
        return ResponseEntity.ok(ApiResponse.success(workspace));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkspace(@PathVariable("id") String id) {
        User user = getAuthenticatedUser();
        workspaceService.archiveWorkspace(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Members Subroutes
    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<WorkspaceMember>>> getWorkspaceMembers(@PathVariable("id") String id) {
        User user = getAuthenticatedUser();
        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeWorkspaceMember(
            @PathVariable("id") String id,
            @PathVariable("userId") String userId
    ) {
        User user = getAuthenticatedUser();
        workspaceService.removeMember(id, userId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // Invites
    @PostMapping("/{id}/invite")
    public ResponseEntity<ApiResponse<InviteToken>> createInviteToken(
            @PathVariable("id") String id,
            @Valid @RequestBody InviteRequest request
    ) {
        User user = getAuthenticatedUser();
        InviteToken token = workspaceService.createInviteToken(
                id,
                user.getId(),
                request.maxUses(),
                request.expirationSeconds()
        );
        return ResponseEntity.ok(ApiResponse.success(token));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<WorkspaceMember>> joinWorkspace(
            @Valid @RequestBody JoinRequest request
    ) {
        User user = getAuthenticatedUser();
        WorkspaceMember member = workspaceService.joinWorkspace(request.inviteToken(), user.getId());
        return ResponseEntity.ok(ApiResponse.success(member));
    }
}
