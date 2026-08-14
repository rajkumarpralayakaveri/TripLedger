package com.rkdevstudios.tripledger.workspace;

import com.rkdevstudios.tripledger.workspace.application.WorkspaceService;
import com.rkdevstudios.tripledger.workspace.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rkdevstudios.tripledger.contribution.application.ContributionService;

class WorkspaceServiceTest {

    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private InviteTokenRepository inviteTokenRepository;
    private ContributionService contributionService;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        inviteTokenRepository = mock(InviteTokenRepository.class);
        contributionService = mock(ContributionService.class);
        workspaceService = new WorkspaceService(
                workspaceRepository,
                workspaceMemberRepository,
                inviteTokenRepository,
                contributionService
        );
    }

    @Test
    void testCreateWorkspace_Success() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(5);
        String name = "Goa Trip";
        String createdBy = "usr_123";

        Workspace mockWorkspace = new Workspace(
                "ws_111", name, "Fun trip", start, end, "INR", BigDecimal.valueOf(10000), 5, createdBy
        );

        when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);

        Workspace result = workspaceService.createWorkspace(
                name, "Fun trip", start, end, "INR", BigDecimal.valueOf(10000), 5, createdBy
        );

        assertNotNull(result);
        assertEquals(name, result.getName());
        verify(workspaceRepository, times(1)).save(any(Workspace.class));
        verify(workspaceMemberRepository, times(1)).save(any(WorkspaceMember.class));
    }

    @Test
    void testCreateWorkspace_InvalidDateRange() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now(); // End before start

        assertThrows(IllegalArgumentException.class, () -> {
            workspaceService.createWorkspace(
                    "Goa", "Fun", start, end, "INR", BigDecimal.valueOf(100), 5, "usr_123"
            );
        });
    }

    @Test
    void testJoinWorkspace_Success() {
        String token = "TOKEN123";
        String userId = "usr_456";
        String workspaceId = "ws_111";

        InviteToken invite = new InviteToken(token, workspaceId, Instant.now().plusSeconds(3600), "usr_123", 5);
        Workspace workspace = new Workspace(workspaceId, "Goa", "Fun", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.valueOf(10000), 5, "usr_123");

        when(inviteTokenRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findByWorkspaceId(workspaceId)).thenReturn(Collections.emptyList());

        WorkspaceMember mockMember = new WorkspaceMember(workspaceId, userId, MemberRole.MEMBER);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(mockMember);

        WorkspaceMember result = workspaceService.joinWorkspace(token, userId);

        assertNotNull(result);
        assertEquals(MemberRole.MEMBER, result.getRole());
        assertEquals(1, invite.getCurrentUses());
        verify(inviteTokenRepository, times(1)).save(invite);
        verify(workspaceMemberRepository, times(1)).save(any(WorkspaceMember.class));
    }

    @Test
    void testJoinWorkspace_Failure_CapacityReached() {
        String token = "TOKEN123";
        String userId = "usr_456";
        String workspaceId = "ws_111";

        InviteToken invite = new InviteToken(token, workspaceId, Instant.now().plusSeconds(3600), "usr_123", 5);
        Workspace workspace = new Workspace(workspaceId, "Goa", "Fun", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.valueOf(10000), 1, "usr_123");

        when(inviteTokenRepository.findByToken(token)).thenReturn(Optional.of(invite));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        // Workspace has plannedMemberCount = 1, and already has 1 member
        WorkspaceMember existingMember = new WorkspaceMember(workspaceId, "usr_123", MemberRole.OWNER);
        when(workspaceMemberRepository.findByWorkspaceId(workspaceId)).thenReturn(Arrays.asList(existingMember));

        assertThrows(IllegalStateException.class, () -> {
            workspaceService.joinWorkspace(token, userId);
        });
    }

    @Test
    void testArchiveWorkspace_ForbiddenForNonOwner() {
        String wsId = "ws_111";
        String callerId = "usr_456";

        Workspace workspace = new Workspace(
                wsId, "Goa", "Fun", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.TEN, 5, "usr_123"
        );
        workspace.setStatus(WorkspaceStatus.ACTIVE);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(workspace));

        // Caller is only a regular MEMBER
        WorkspaceMember member = new WorkspaceMember(wsId, callerId, MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, callerId)).thenReturn(Optional.of(member));

        assertThrows(SecurityException.class, () -> {
            workspaceService.archiveWorkspace(wsId, callerId);
        });
    }
}
