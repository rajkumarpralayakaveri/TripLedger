package com.rkdevstudios.tripledger.settlement.api;

import com.rkdevstudios.tripledger.common.ApiResponse;
import com.rkdevstudios.tripledger.common.ApiRoutes;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import com.rkdevstudios.tripledger.settlement.application.SettlementService;
import com.rkdevstudios.tripledger.settlement.domain.MemberBalance;
import com.rkdevstudios.tripledger.settlement.domain.SettlementPlan;
import com.rkdevstudios.tripledger.settlement.domain.SettlementTransaction;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/workspaces/{id}")
public class SettlementController {

    private final SettlementService settlementService;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public SettlementController(
            SettlementService settlementService,
            UserRepository userRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.settlementService = settlementService;
        this.userRepository = userRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new SecurityException("User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    private void verifyMembership(String workspaceId, String userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this workspace"));
    }

    private String getUserName(String userId, Map<String, String> nameCache) {
        return nameCache.computeIfAbsent(userId, id -> userRepository.findById(id)
                .map(User::getName)
                .orElse("Someone"));
    }

    @GetMapping("/settlements/plan")
    public ResponseEntity<ApiResponse<SettlementPlanResponse>> getPlan(@PathVariable("id") String id) {
        User user = getAuthenticatedUser();
        verifyMembership(id, user.getId());

        SettlementPlan plan = settlementService.generatePlan(id);

        System.out.println("[SETTLEMENT_DEBUG] plan.transfers size: " + (plan.transfers() != null ? plan.transfers().size() : 0));
        Map<String, String> names = new HashMap<>();
        List<SettlementPlanResponse.SettlementTransferResponse> transferResponses = plan.transfers().stream()
                .map(t -> new SettlementPlanResponse.SettlementTransferResponse(
                         t.id(),
                         t.fromUserId(),
                         getUserName(t.fromUserId(), names),
                         t.toUserId(),
                         getUserName(t.toUserId(), names),
                         t.amount()
                ))
                .collect(Collectors.toList());
        System.out.println("[SETTLEMENT_DEBUG] transferResponses DTO size: " + transferResponses.size());
        for (SettlementPlanResponse.SettlementTransferResponse tr : transferResponses) {
            System.out.println("[SETTLEMENT_DEBUG] Transfer DTO: " + tr.id() 
                + " From: " + tr.fromUserName() + " (" + tr.fromUserId() + ")"
                + " To: " + tr.toUserName() + " (" + tr.toUserId() + ")"
                + " Amount: " + tr.amount().getAmount());
        }

        SettlementPlanResponse response = new SettlementPlanResponse(
                plan.sessionId(),
                plan.workspaceId(),
                transferResponses,
                plan.stateHash(),
                plan.planVersion()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/settlements/{transferId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmTransfer(
            @PathVariable("id") String id,
            @PathVariable("transferId") String transferId,
            @Valid @RequestBody ConfirmSettlementRequest request
    ) {
        User user = getAuthenticatedUser();
        verifyMembership(id, user.getId());

        settlementService.confirmSettlement(id, transferId, request.getSessionId(), user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/balances")
    public ResponseEntity<ApiResponse<BalancesResponse>> getBalances(@PathVariable("id") String id) {
        User user = getAuthenticatedUser();
        verifyMembership(id, user.getId());

        List<MemberBalance> balances = settlementService.getBalances(id);

        Map<String, String> names = new HashMap<>();
        List<BalancesResponse.MemberBalanceResponse> balanceResponses = balances.stream()
                .map(b -> new BalancesResponse.MemberBalanceResponse(
                        b.userId(),
                        getUserName(b.userId(), names),
                        b.paid(),
                        b.owed(),
                        b.balance()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(new BalancesResponse(balanceResponses)));
    }

    @GetMapping("/settlements/history")
    public ResponseEntity<ApiResponse<SettlementHistoryResponse>> getHistory(@PathVariable("id") String id) {
        User user = getAuthenticatedUser();
        verifyMembership(id, user.getId());

        List<SettlementTransaction> history = settlementService.getHistory(id);

        Map<String, String> names = new HashMap<>();
        List<SettlementHistoryResponse.HistoryItem> items = history.stream()
                .map(st -> new SettlementHistoryResponse.HistoryItem(
                        st.getId(),
                        st.getFromUserId(),
                        getUserName(st.getFromUserId(), names),
                        st.getToUserId(),
                        getUserName(st.getToUserId(), names),
                        st.getMoney(),
                        st.getConfirmedAt()
                ))
                .collect(Collectors.toList());

        // Group by local date
        Map<LocalDate, List<SettlementHistoryResponse.HistoryItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(item -> LocalDate.ofInstant(item.confirmedAt(), ZoneId.systemDefault())));

        List<SettlementHistoryResponse.SettlementHistoryGroup> groups = grouped.entrySet().stream()
                .map(entry -> new SettlementHistoryResponse.SettlementHistoryGroup(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(SettlementHistoryResponse.SettlementHistoryGroup::date).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(new SettlementHistoryResponse(groups)));
    }
}
