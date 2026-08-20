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
    private com.rkdevstudios.tripledger.contribution.domain.ContributionEntryRepository contributionEntryRepository;
    private com.rkdevstudios.tripledger.expense.domain.ExpenseRepository expenseRepository;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        workspaceRepository = mock(WorkspaceRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        inviteTokenRepository = mock(InviteTokenRepository.class);
        contributionService = mock(ContributionService.class);
        contributionEntryRepository = mock(com.rkdevstudios.tripledger.contribution.domain.ContributionEntryRepository.class);
        expenseRepository = mock(com.rkdevstudios.tripledger.expense.domain.ExpenseRepository.class);
        workspaceService = new WorkspaceService(
                workspaceRepository,
                workspaceMemberRepository,
                inviteTokenRepository,
                contributionService,
                contributionEntryRepository,
                expenseRepository
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
        WorkspaceMember existingMember = new WorkspaceMember(workspaceId, "usr_123", MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceId(workspaceId)).thenReturn(Arrays.asList(existingMember));

        assertThrows(IllegalStateException.class, () -> {
            workspaceService.joinWorkspace(token, userId);
        });
    }

    @Test
    void testArchiveWorkspace_ForbiddenForNonAdmin() {
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
            workspaceService.updateWorkspace(wsId, null, null, null, null, null, null, WorkspaceStatus.ARCHIVED, null, callerId);
        });
    }

    @Test
    void testUpdateWorkspace_ContributionModeLockedAfterFinancialActivity() {
        String wsId = "ws_111";
        String callerId = "usr_123";

        Workspace workspace = new Workspace(wsId, "Goa", "Fun", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.TEN, 5, callerId);
        workspace.setContributionMode(ContributionMode.COMBINED);

        when(workspaceRepository.findById(wsId)).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, callerId)).thenReturn(Optional.of(new WorkspaceMember(wsId, callerId, MemberRole.ADMIN)));

        // Mock existing financial activity
        when(contributionEntryRepository.findByWorkspaceId(wsId)).thenReturn(Collections.singletonList(new com.rkdevstudios.tripledger.contribution.domain.ContributionEntry()));

        assertThrows(IllegalStateException.class, () -> {
            workspaceService.updateWorkspace(wsId, null, null, null, null, null, null, null, ContributionMode.INDIVIDUAL, callerId);
        });
    }

    @Test
    void testRemoveMember_BlockedWhenFinancialActivityExists() {
        String wsId = "ws_111";
        String callerId = "usr_admin";
        String targetId = "usr_member";

        WorkspaceMember caller = new WorkspaceMember(wsId, callerId, MemberRole.ADMIN);
        WorkspaceMember target = new WorkspaceMember(wsId, targetId, MemberRole.MEMBER);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, targetId)).thenReturn(Optional.of(target));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, callerId)).thenReturn(Optional.of(caller));

        // Mock financial entry exists for target member
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(wsId, targetId)).thenReturn(Collections.singletonList(new com.rkdevstudios.tripledger.contribution.domain.ContributionEntry()));

        assertThrows(IllegalStateException.class, () -> {
            workspaceService.removeMember(wsId, targetId, callerId);
        });
    }

    @Test
    void testUpdateMemberRole_AdminCanPromoteMemberToAdmin() {
        String wsId = "ws_111";
        String callerId = "usr_admin";
        String targetId = "usr_member";

        WorkspaceMember caller = new WorkspaceMember(wsId, callerId, MemberRole.ADMIN);
        WorkspaceMember target = new WorkspaceMember(wsId, targetId, MemberRole.MEMBER);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, callerId)).thenReturn(Optional.of(caller));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, targetId)).thenReturn(Optional.of(target));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(i -> i.getArgument(0));

        WorkspaceMember updated = workspaceService.updateMemberRole(wsId, targetId, MemberRole.ADMIN, callerId);

        assertEquals(MemberRole.ADMIN, updated.getRole());
    }

    @Test
    void testUpdateMemberRole_CannotPromoteOrDemoteSelf() {
        String wsId = "ws_111";
        String callerId = "usr_admin";

        WorkspaceMember caller = new WorkspaceMember(wsId, callerId, MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, callerId)).thenReturn(Optional.of(caller));

        assertThrows(IllegalArgumentException.class, () -> {
            workspaceService.updateMemberRole(wsId, callerId, MemberRole.MEMBER, callerId);
        });
    }

    @Test
    void testRemoveMember_AdminCannotRemoveAnotherAdmin() {
        String wsId = "ws_111";
        String callerId = "usr_admin1";
        String targetId = "usr_admin2";

        WorkspaceMember caller = new WorkspaceMember(wsId, callerId, MemberRole.ADMIN);
        WorkspaceMember target = new WorkspaceMember(wsId, targetId, MemberRole.ADMIN);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, callerId)).thenReturn(Optional.of(caller));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, targetId)).thenReturn(Optional.of(target));

        assertThrows(SecurityException.class, () -> {
            workspaceService.removeMember(wsId, targetId, callerId);
        });
    }

    @Test
    void testLeaveWorkspace_LastAdminCannotLeavePopulatedWorkspace() {
        String wsId = "ws_111";
        String callerId = "usr_admin1";
        String otherMemberId = "usr_member2";

        WorkspaceMember admin = new WorkspaceMember(wsId, callerId, MemberRole.ADMIN);
        WorkspaceMember member = new WorkspaceMember(wsId, otherMemberId, MemberRole.MEMBER);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(wsId, callerId)).thenReturn(Optional.of(admin));
        when(workspaceMemberRepository.findByWorkspaceId(wsId)).thenReturn(Arrays.asList(admin, member));

        assertThrows(IllegalStateException.class, () -> {
            workspaceService.leaveWorkspace(wsId, callerId);
        });
    }
}
