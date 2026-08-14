package com.rkdevstudios.tripledger.contribution.application;

import com.rkdevstudios.tripledger.contribution.domain.*;
import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ContributionService {

    private final PlannedContributionRepository plannedContributionRepository;
    private final ContributionEntryRepository contributionEntryRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;

    public ContributionService(
            PlannedContributionRepository plannedContributionRepository,
            ContributionEntryRepository contributionEntryRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            WorkspaceRepository workspaceRepository
    ) {
        this.plannedContributionRepository = plannedContributionRepository;
        this.contributionEntryRepository = contributionEntryRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public void initializeContributions(
            String workspaceId,
            ContributionStrategy strategy,
            BigDecimal budget,
            List<String> memberIds,
            Map<String, BigDecimal> customAmounts,
            Map<String, BigDecimal> percentages
    ) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        int plannedCount = workspace.getPlannedMemberCount() != null ? workspace.getPlannedMemberCount() : 1;

        // Validate strategy constraints
        Map<String, BigDecimal> calculatedPlanned = new HashMap<>();

        if (strategy == ContributionStrategy.EQUAL) {
            BigDecimal size = BigDecimal.valueOf(plannedCount);
            BigDecimal baseShare = budget.divide(size, 4, RoundingMode.DOWN);
            BigDecimal totalAllocatedBase = baseShare.multiply(size);
            BigDecimal remainder = budget.subtract(totalAllocatedBase);
            int remainderUnits = remainder.divide(BigDecimal.valueOf(0.0001), 0, RoundingMode.HALF_UP).intValue();

            List<String> sortedMemberIds = new java.util.ArrayList<>(memberIds);
            java.util.Collections.sort(sortedMemberIds);

            for (int i = 0; i < sortedMemberIds.size(); i++) {
                String userId = sortedMemberIds.get(i);
                BigDecimal amount = baseShare;
                if (i < remainderUnits) {
                    amount = amount.add(BigDecimal.valueOf(0.0001));
                }
                calculatedPlanned.put(userId, amount);
            }
        } else if (strategy == ContributionStrategy.PERCENTAGE) {
            BigDecimal totalPercentage = BigDecimal.ZERO;
            if (percentages != null) {
                for (BigDecimal pct : percentages.values()) {
                    totalPercentage = totalPercentage.add(pct);
                }
            }
            if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new IllegalArgumentException("Percentages must sum up to exactly 100%");
            }
            for (String userId : memberIds) {
                BigDecimal pct = percentages.getOrDefault(userId, BigDecimal.ZERO);
                BigDecimal amount = budget.multiply(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                calculatedPlanned.put(userId, amount);
            }
        } else if (strategy == ContributionStrategy.CUSTOM) {
            BigDecimal totalCustom = BigDecimal.ZERO;
            if (customAmounts != null) {
                for (BigDecimal amt : customAmounts.values()) {
                    totalCustom = totalCustom.add(amt);
                }
            }
            if (totalCustom.compareTo(budget) != 0) {
                throw new IllegalArgumentException("Custom contribution amounts must sum to exactly the workspace budget (" + budget + ")");
            }
            for (String userId : memberIds) {
                calculatedPlanned.put(userId, customAmounts.getOrDefault(userId, BigDecimal.ZERO));
            }
        }

        // Save planned contributions
        for (Map.Entry<String, BigDecimal> entry : calculatedPlanned.entrySet()) {
            String userId = entry.getKey();
            BigDecimal amount = entry.getValue();

            Optional<PlannedContribution> existing = plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
            if (existing.isPresent()) {
                PlannedContribution planned = existing.get();
                planned.setPlannedAmount(amount);
                plannedContributionRepository.save(planned);
            } else {
                PlannedContribution planned = new PlannedContribution(
                        UUID.randomUUID().toString(),
                        workspaceId,
                        userId,
                        amount
                );
                plannedContributionRepository.save(planned);
            }
        }
    }

    @Transactional
    public ContributionEntry recordCashContribution(
            String workspaceId,
            String userId,
            BigDecimal amount,
            String description,
            String callerUserId
    ) {
        verifyAuthorized(workspaceId, callerUserId);

        ContributionEntry entry = new ContributionEntry(
                UUID.randomUUID().toString(),
                workspaceId,
                userId,
                ContributionEntryType.CASH,
                amount,
                description,
                null
        );
        return contributionEntryRepository.save(entry);
    }

    @Transactional
    public ContributionEntry recordAdjustment(
            String workspaceId,
            String userId,
            BigDecimal amount,
            String description,
            String callerUserId
    ) {
        verifyAuthorized(workspaceId, callerUserId);
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Adjustment requires a description/reason");
        }

        ContributionEntry entry = new ContributionEntry(
                UUID.randomUUID().toString(),
                workspaceId,
                userId,
                ContributionEntryType.ADJUSTMENT,
                amount,
                description,
                null
        );
        return contributionEntryRepository.save(entry);
    }

    @Transactional
    public ContributionEntry recordDirectExpense(
            String workspaceId,
            String userId,
            BigDecimal amount,
            String description,
            String expenseId
    ) {
        ContributionEntry entry = new ContributionEntry(
                UUID.randomUUID().toString(),
                workspaceId,
                userId,
                ContributionEntryType.DIRECT_EXPENSE,
                amount,
                description,
                expenseId
        );
        return contributionEntryRepository.save(entry);
    }

    public ContributionSummary getContributionSummary(String workspaceId, String userId) {
        PlannedContribution planned = plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElse(new PlannedContribution("", workspaceId, userId, BigDecimal.ZERO));

        List<ContributionEntry> entries = contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, userId);

        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal directExpense = BigDecimal.ZERO;
        BigDecimal adjustments = BigDecimal.ZERO;

        for (ContributionEntry entry : entries) {
            switch (entry.getEntryType()) {
                case CASH -> cash = cash.add(entry.getAmount());
                case DIRECT_EXPENSE -> directExpense = directExpense.add(entry.getAmount());
                case ADJUSTMENT -> adjustments = adjustments.add(entry.getAmount());
            }
        }

        return ContributionSummary.compute(
                workspaceId,
                userId,
                planned.getPlannedAmount(),
                cash,
                directExpense,
                adjustments
        );
    }

    public List<ContributionSummary> getAllContributionSummaries(String workspaceId, String callerUserId) {
        // Enforce membership
        verifyMember(workspaceId, callerUserId);

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);
        List<ContributionSummary> summaries = new ArrayList<>();
        for (WorkspaceMember m : members) {
            summaries.add(getContributionSummary(workspaceId, m.getUserId()));
        }
        return summaries;
    }

    public TripFund getTripFund(String workspaceId, String callerUserId, BigDecimal budget) {
        verifyMember(workspaceId, callerUserId);

        List<ContributionEntry> entries = contributionEntryRepository.findByWorkspaceId(workspaceId);

        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal directExpense = BigDecimal.ZERO;
        BigDecimal adjustments = BigDecimal.ZERO;

        for (ContributionEntry entry : entries) {
            switch (entry.getEntryType()) {
                case CASH -> cash = cash.add(entry.getAmount());
                case DIRECT_EXPENSE -> directExpense = directExpense.add(entry.getAmount());
                case ADJUSTMENT -> adjustments = adjustments.add(entry.getAmount());
            }
        }

        return TripFund.compute(workspaceId, budget, cash, directExpense, adjustments);
    }

    public WorkspaceFinancialSnapshot getFinancialSnapshot(String workspaceId, String callerUserId, BigDecimal budget, BigDecimal spent) {
        List<ContributionSummary> memberContributions = getAllContributionSummaries(workspaceId, callerUserId);
        TripFund fund = getTripFund(workspaceId, callerUserId, budget);

        return WorkspaceFinancialSnapshot.of(
                workspaceId,
                budget,
                spent,
                fund.currentFund(),
                memberContributions
        );
    }

    private void verifyAuthorized(String workspaceId, String userId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this workspace"));
        switch (member.getRole()) {
            case OWNER:
            case ADMIN:
                return;
            default:
                throw new SecurityException("Permission denied. OWNER or ADMIN role required.");
        }
    }

    private void verifyMember(String workspaceId, String userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this workspace"));
    }
}
