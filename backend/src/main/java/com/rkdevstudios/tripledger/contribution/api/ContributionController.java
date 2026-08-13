package com.rkdevstudios.tripledger.contribution.api;

import com.rkdevstudios.tripledger.common.ApiResponse;
import com.rkdevstudios.tripledger.common.ApiRoutes;
import com.rkdevstudios.tripledger.contribution.application.ContributionService;
import com.rkdevstudios.tripledger.contribution.domain.ContributionEntry;
import com.rkdevstudios.tripledger.contribution.domain.ContributionSummary;
import com.rkdevstudios.tripledger.contribution.domain.TripFund;
import com.rkdevstudios.tripledger.contribution.domain.WorkspaceFinancialSnapshot;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.rkdevstudios.tripledger.expense.domain.ExpenseRepository;
import com.rkdevstudios.tripledger.expense.domain.ExpenseStatus;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/workspaces/{id}")
public class ContributionController {

    private final ContributionService contributionService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ExpenseRepository expenseRepository;

    public ContributionController(
            ContributionService contributionService,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            ExpenseRepository expenseRepository
    ) {
        this.contributionService = contributionService;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.expenseRepository = expenseRepository;
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new SecurityException("User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    private Workspace getWorkspaceAndVerifyMember(String workspaceId, String userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this workspace"));
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    @GetMapping("/contributions")
    public ResponseEntity<ApiResponse<List<ContributionSummary>>> getContributions(@PathVariable String id) {
        User user = getAuthenticatedUser();
        List<ContributionSummary> summaries = contributionService.getAllContributionSummaries(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    @PutMapping("/contributions")
    public ResponseEntity<ApiResponse<ContributionEntry>> recordCashContribution(
            @PathVariable String id,
            @Valid @RequestBody CashContributionRequest request
    ) {
        User user = getAuthenticatedUser();
        ContributionEntry entry = contributionService.recordCashContribution(
                id,
                request.userId(),
                request.amount(),
                request.description(),
                user.getId()
        );
        return ResponseEntity.ok(ApiResponse.success(entry));
    }

    @PostMapping("/contributions/adjust")
    public ResponseEntity<ApiResponse<ContributionEntry>> recordAdjustment(
            @PathVariable String id,
            @Valid @RequestBody AdjustmentRequest request
    ) {
        User user = getAuthenticatedUser();
        ContributionEntry entry = contributionService.recordAdjustment(
                id,
                request.userId(),
                request.amount(),
                request.reason(),
                user.getId()
        );
        return ResponseEntity.ok(ApiResponse.success(entry));
    }

    @GetMapping("/fund")
    public ResponseEntity<ApiResponse<TripFund>> getTripFund(@PathVariable String id) {
        User user = getAuthenticatedUser();
        Workspace workspace = getWorkspaceAndVerifyMember(id, user.getId());
        BigDecimal budget = workspace.getBudget() != null ? workspace.getBudget() : BigDecimal.ZERO;
        TripFund fund = contributionService.getTripFund(id, user.getId(), budget);
        return ResponseEntity.ok(ApiResponse.success(fund));
    }

    @GetMapping("/budget")
    public ResponseEntity<ApiResponse<BudgetResponse>> getBudget(@PathVariable String id) {
        User user = getAuthenticatedUser();
        Workspace workspace = getWorkspaceAndVerifyMember(id, user.getId());
        BigDecimal budget = workspace.getBudget() != null ? workspace.getBudget() : BigDecimal.ZERO;

        BigDecimal totalSpent = expenseRepository.findByWorkspaceIdAndStatusNot(id, ExpenseStatus.DELETED)
                .stream()
                .map(e -> e.getMoney().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingBudget = budget.subtract(totalSpent).max(BigDecimal.ZERO);

        return ResponseEntity.ok(ApiResponse.success(new BudgetResponse(budget, totalSpent, remainingBudget)));
    }

    @GetMapping("/financial-summary")
    public ResponseEntity<ApiResponse<WorkspaceFinancialSnapshot>> getFinancialSummary(@PathVariable String id) {
        User user = getAuthenticatedUser();
        Workspace workspace = getWorkspaceAndVerifyMember(id, user.getId());
        BigDecimal budget = workspace.getBudget() != null ? workspace.getBudget() : BigDecimal.ZERO;

        BigDecimal totalSpent = expenseRepository.findByWorkspaceIdAndStatusNot(id, ExpenseStatus.DELETED)
                .stream()
                .map(e -> e.getMoney().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        WorkspaceFinancialSnapshot snapshot = contributionService.getFinancialSnapshot(
                id,
                user.getId(),
                budget,
                totalSpent
        );
        return ResponseEntity.ok(ApiResponse.success(snapshot));
    }

    @PutMapping("/strategy")
    public ResponseEntity<ApiResponse<Void>> updateStrategy(
            @PathVariable String id,
            @Valid @RequestBody ContributionStrategyRequest request
    ) {
        User user = getAuthenticatedUser();
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));

        // Only OWNER can change strategy
        WorkspaceMember callerMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(id, user.getId())
                .orElseThrow(() -> new SecurityException("User is not a member of this workspace"));

        if (callerMember.getRole() != com.rkdevstudios.tripledger.workspace.domain.MemberRole.OWNER) {
            throw new SecurityException("Only the workspace OWNER can modify the contribution strategy");
        }

        workspace.setContributionStrategy(request.strategy());
        workspaceRepository.save(workspace);

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(id);
        List<String> memberIds = members.stream().map(WorkspaceMember::getUserId).toList();

        BigDecimal budget = workspace.getBudget() != null ? workspace.getBudget() : BigDecimal.ZERO;

        contributionService.initializeContributions(
                id,
                request.strategy(),
                budget,
                memberIds,
                request.customAmounts(),
                request.percentages()
        );

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
