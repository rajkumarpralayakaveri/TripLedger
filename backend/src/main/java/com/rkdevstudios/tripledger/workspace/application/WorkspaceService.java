package com.rkdevstudios.tripledger.workspace.application;

import com.rkdevstudios.tripledger.workspace.domain.*;
import com.rkdevstudios.tripledger.contribution.application.ContributionService;
import com.rkdevstudios.tripledger.contribution.domain.ContributionStrategy;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final ContributionService contributionService;
    private final com.rkdevstudios.tripledger.contribution.domain.ContributionEntryRepository contributionEntryRepository;
    private final com.rkdevstudios.tripledger.expense.domain.ExpenseRepository expenseRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            InviteTokenRepository inviteTokenRepository,
            @Lazy ContributionService contributionService,
            com.rkdevstudios.tripledger.contribution.domain.ContributionEntryRepository contributionEntryRepository,
            com.rkdevstudios.tripledger.expense.domain.ExpenseRepository expenseRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.contributionService = contributionService;
        this.contributionEntryRepository = contributionEntryRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public Workspace createWorkspace(
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            String baseCurrency,
            BigDecimal budget,
            Integer plannedMemberCount,
            String createdBy
    ) {
        return createWorkspace(
                name, description, startDate, endDate, baseCurrency, budget, plannedMemberCount, createdBy,
                ContributionStrategy.EQUAL, ContributionMode.COMBINED, null, null
        );
    }

    @Transactional
    public Workspace createWorkspace(
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            String baseCurrency,
            BigDecimal budget,
            Integer plannedMemberCount,
            String createdBy,
            ContributionStrategy strategy,
            ContributionMode contributionMode,
            Map<String, BigDecimal> customAmounts,
            Map<String, BigDecimal> percentages
    ) {
        if (plannedMemberCount == null || plannedMemberCount < 1) {
            throw new IllegalArgumentException("Planned member count must be at least 1");
        }

        // Enforce DateRange invariant check via Value Object
        new DateRange(startDate, endDate);

        // Generate UUID v7 style ID
        String workspaceId = UUID.randomUUID().toString();

        Workspace workspace = new Workspace(
                workspaceId,
                name,
                description,
                startDate,
                endDate,
                baseCurrency,
                budget,
                plannedMemberCount,
                createdBy
        );
        workspace.setStatus(WorkspaceStatus.PLANNING);
        workspace.setContributionStrategy(strategy != null ? strategy : ContributionStrategy.EQUAL);
        workspace.setContributionMode(contributionMode != null ? contributionMode : ContributionMode.COMBINED);

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Creator automatically joins as ADMIN
        WorkspaceMember adminMember = new WorkspaceMember(workspaceId, createdBy, MemberRole.ADMIN);
        workspaceMemberRepository.save(adminMember);

        // Initialize planned contributions for the creator
        contributionService.initializeContributions(
                workspaceId,
                workspace.getContributionStrategy(),
                budget != null ? budget : BigDecimal.ZERO,
                List.of(createdBy),
                customAmounts,
                percentages
        );

        savedWorkspace.setMemberCount(1);
        return savedWorkspace;
    }


    @Transactional
    public Workspace updateWorkspace(
            String workspaceId,
            String name,
            String description,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal budget,
            Integer plannedMemberCount,
            WorkspaceStatus targetStatus,
            ContributionMode newMode,
            String callerUserId
    ) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        // Validate caller permissions (ADMIN)
        verifyRole(workspaceId, callerUserId, MemberRole.ADMIN);

        // Contribution Mode Lock Validation
        if (newMode != null && newMode != workspace.getContributionMode()) {
            var entries = contributionEntryRepository.findByWorkspaceId(workspaceId);
            var expenses = expenseRepository.findByWorkspaceIdAndStatusNot(workspaceId, com.rkdevstudios.tripledger.expense.domain.ExpenseStatus.DELETED);
            if (!entries.isEmpty() || !expenses.isEmpty()) {
                throw new IllegalStateException("Contribution mode cannot be changed after financial activity (contributions or expenses) has started.");
            }
            workspace.setContributionMode(newMode);
        }

        // Validate state transitions
        if (targetStatus != null && targetStatus != workspace.getStatus()) {
            validateStateTransition(workspace.getStatus(), targetStatus, callerUserId, workspaceId);
            workspace.setStatus(targetStatus);
        }

        if (name != null) workspace.setName(name);
        if (description != null) workspace.setDescription(description);
        if (startDate != null && endDate != null) {
            new DateRange(startDate, endDate);
            workspace.setStartDate(startDate);
            workspace.setEndDate(endDate);
        }

        if (plannedMemberCount != null && plannedMemberCount < 1) {
            throw new IllegalArgumentException("Planned member count must be at least 1");
        }

        boolean recalculate = false;

        if (budget != null && (workspace.getBudget() == null || budget.compareTo(workspace.getBudget()) != 0)) {
            workspace.setBudget(budget);
            recalculate = true;
        }

        if (plannedMemberCount != null && !plannedMemberCount.equals(workspace.getPlannedMemberCount())) {
            List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);
            if (plannedMemberCount < members.size()) {
                throw new IllegalArgumentException("Cannot set planned member count to less than the number of current members (" + members.size() + ")");
            }
            workspace.setPlannedMemberCount(plannedMemberCount);
            recalculate = true;
        }

        Workspace saved = workspaceRepository.save(workspace);

        if (recalculate) {
            List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);
            List<String> memberIds = members.stream().map(WorkspaceMember::getUserId).toList();
            contributionService.initializeContributions(
                    workspaceId,
                    workspace.getContributionStrategy(),
                    workspace.getBudget() != null ? workspace.getBudget() : BigDecimal.ZERO,
                    memberIds,
                    null,
                    null
            );
        }

        int count = workspaceMemberRepository.findByWorkspaceId(workspaceId).size();
        saved.setMemberCount(count);
        return saved;
    }

    @Transactional
    public void archiveWorkspace(String workspaceId, String callerUserId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        // ADMIN can archive a workspace
        verifyRole(workspaceId, callerUserId, MemberRole.ADMIN);

        validateStateTransition(workspace.getStatus(), WorkspaceStatus.ARCHIVED, callerUserId, workspaceId);
        workspace.setStatus(WorkspaceStatus.ARCHIVED);
        workspaceRepository.save(workspace);
    }

    @Transactional
    public InviteToken createInviteToken(
            String workspaceId,
            String callerUserId,
            int maxUses,
            long expirationSeconds
    ) {
        // ADMIN can create invites
        verifyRole(workspaceId, callerUserId, MemberRole.ADMIN);

        String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant expiresAt = Instant.now().plusSeconds(expirationSeconds);

        InviteToken inviteToken = new InviteToken(
                token,
                workspaceId,
                expiresAt,
                callerUserId,
                maxUses
        );

        return inviteTokenRepository.save(inviteToken);
    }

    @Transactional
    public WorkspaceMember joinWorkspace(String token, String userId) {
        InviteToken inviteToken = inviteTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));

        if (!inviteToken.isValid()) {
            throw new IllegalStateException("Invite token is expired, inactive, or usage limit reached");
        }

        Workspace workspace = workspaceRepository.findById(inviteToken.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspace.getId());
        if (members.size() >= workspace.getPlannedMemberCount()) {
            throw new IllegalStateException("Workspace has reached its maximum planned member capacity (" + workspace.getPlannedMemberCount() + ")");
        }

        // Check if user is already a member
        Optional<WorkspaceMember> existingOpt = workspaceMemberRepository
                .findByWorkspaceIdAndUserId(inviteToken.getWorkspaceId(), userId);
        if (existingOpt.isPresent()) {
            throw new IllegalArgumentException("User is already a member of this workspace");
        }

        // Join as regular MEMBER
        WorkspaceMember newMember = new WorkspaceMember(
                inviteToken.getWorkspaceId(),
                userId,
                MemberRole.MEMBER
        );

        inviteToken.setCurrentUses(inviteToken.getCurrentUses() + 1);
        if (inviteToken.isUsageLimitReached()) {
            inviteToken.setActive(false);
        }
        inviteTokenRepository.save(inviteToken);

        WorkspaceMember savedMember = workspaceMemberRepository.save(newMember);

        // Recalculate contributions when a new member joins
        List<WorkspaceMember> updatedMembers = workspaceMemberRepository.findByWorkspaceId(workspace.getId());
        List<String> memberIds = updatedMembers.stream().map(WorkspaceMember::getUserId).toList();
        contributionService.initializeContributions(
                workspace.getId(),
                workspace.getContributionStrategy(),
                workspace.getBudget() != null ? workspace.getBudget() : BigDecimal.ZERO,
                memberIds,
                null,
                null
        );

        return savedMember;
    }

    @Transactional
    public void removeMember(String workspaceId, String targetUserId, String callerUserId) {
        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        WorkspaceMember callerMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Caller is not a member of this workspace"));

        if (callerMember.getRole() != MemberRole.ADMIN) {
            throw new SecurityException("Permission denied. Only ADMINs can remove members.");
        }

        if (targetMember.getRole() == MemberRole.ADMIN) {
            throw new SecurityException("ADMIN cannot remove another ADMIN.");
        }

        // Prevent removing member if financial activity exists for this member
        var entries = contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId);
        var expenses = expenseRepository.findByWorkspaceIdAndStatusNot(workspaceId, com.rkdevstudios.tripledger.expense.domain.ExpenseStatus.DELETED)
                .stream().filter(e -> e.getPaidByUserId().equals(targetUserId)).toList();
        if (!entries.isEmpty() || !expenses.isEmpty()) {
            throw new IllegalStateException("Cannot remove member because financial contributions or expenses are recorded for this user");
        }

        workspaceMemberRepository.delete(targetMember);
    }

    @Transactional
    public WorkspaceMember updateMemberRole(String workspaceId, String targetUserId, MemberRole newRole, String callerUserId) {
        verifyRole(workspaceId, callerUserId, MemberRole.ADMIN);

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (targetUserId.equals(callerUserId)) {
            throw new IllegalArgumentException("ADMIN cannot promote or demote themselves");
        }

        targetMember.setRole(newRole);
        return workspaceMemberRepository.save(targetMember);
    }

    @Transactional
    public void leaveWorkspace(String workspaceId, String callerUserId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerUserId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this workspace"));

        List<WorkspaceMember> allMembers = workspaceMemberRepository.findByWorkspaceId(workspaceId);

        // If caller is ADMIN, ensure caller is not the last ADMIN when other members exist
        if (member.getRole() == MemberRole.ADMIN) {
            long adminCount = allMembers.stream().filter(m -> m.getRole() == MemberRole.ADMIN).count();
            if (adminCount <= 1 && allMembers.size() > 1) {
                throw new IllegalStateException("Last ADMIN cannot leave the workspace while other members remain. Promote another member to ADMIN first.");
            }
        }

        // Prevent leaving if financial activity exists for this member
        var entries = contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, callerUserId);
        var expenses = expenseRepository.findByWorkspaceIdAndStatusNot(workspaceId, com.rkdevstudios.tripledger.expense.domain.ExpenseStatus.DELETED)
                .stream().filter(e -> e.getPaidByUserId().equals(callerUserId)).toList();
        if (!entries.isEmpty() || !expenses.isEmpty()) {
            throw new IllegalStateException("Cannot leave workspace because financial contributions or expenses are recorded for this user");
        }

        workspaceMemberRepository.delete(member);
    }

    public List<WorkspaceMember> getWorkspaceMembers(String workspaceId, String callerUserId) {
        // Caller must be a member
        verifyRole(workspaceId, callerUserId, MemberRole.ADMIN, MemberRole.MEMBER);
        return workspaceMemberRepository.findByWorkspaceId(workspaceId);
    }

    public Workspace getWorkspaceById(String workspaceId, String callerUserId) {
        verifyRole(workspaceId, callerUserId, MemberRole.ADMIN, MemberRole.MEMBER);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        int count = workspaceMemberRepository.findByWorkspaceId(workspaceId).size();
        workspace.setMemberCount(count);
        return workspace;
    }

    public List<Workspace> getCallerWorkspaces(String userId) {
        // Query workspaces linked to member
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByUserId(userId);
        return memberships.stream()
                .map(m -> workspaceRepository.findById(m.getWorkspaceId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(ws -> {
                    int count = workspaceMemberRepository.findByWorkspaceId(ws.getId()).size();
                    ws.setMemberCount(count);
                })
                .toList();
    }

    private void verifyRole(String workspaceId, String userId, MemberRole... allowedRoles) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this workspace"));

        for (MemberRole role : allowedRoles) {
            if (member.getRole() == role) {
                return;
            }
        }
        throw new SecurityException("Permission denied. Required roles: " + java.util.Arrays.toString(allowedRoles));
    }

    private void validateStateTransition(
            WorkspaceStatus current,
            WorkspaceStatus target,
            String callerUserId,
            String workspaceId
    ) {
        if (current == WorkspaceStatus.ARCHIVED) {
            throw new IllegalStateException("Archived workspace states cannot change");
        }

        switch (target) {
            case ACTIVE -> {
                if (current != WorkspaceStatus.PLANNING) {
                    throw new IllegalStateException("Workspace can only transition to ACTIVE from PLANNING");
                }
            }
            case COMPLETED -> {
                if (current != WorkspaceStatus.ACTIVE) {
                    throw new IllegalStateException("Workspace can only transition to COMPLETED from ACTIVE");
                }
            }
            case ARCHIVED -> {
                // Verified in caller
            }
            case PLANNING -> throw new IllegalStateException("Cannot transition back to PLANNING");
        }
    }
}
