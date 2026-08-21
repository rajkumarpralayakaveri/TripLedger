package com.rkdevstudios.tripledger.expense.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rkdevstudios.tripledger.common.ApiResponse;
import com.rkdevstudios.tripledger.common.ApiRoutes;
import com.rkdevstudios.tripledger.expense.application.ExpenseService;
import com.rkdevstudios.tripledger.expense.domain.*;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/workspaces/{id}")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final UserRepository userRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ActivityEntryRepository activityEntryRepository;
    private final SplitAllocationRepository splitAllocationRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public ExpenseController(
            ExpenseService expenseService,
            UserRepository userRepository,
            ExpenseCategoryRepository categoryRepository,
            ActivityEntryRepository activityEntryRepository,
            SplitAllocationRepository splitAllocationRepository
    ) {
        this.expenseService = expenseService;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.activityEntryRepository = activityEntryRepository;
        this.splitAllocationRepository = splitAllocationRepository;
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new SecurityException("User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    private String getUserName(String userId) {
        return userRepository.findById(userId)
                .map(User::getName)
                .orElse("Unknown Member");
    }

    private ExpenseCategory getCategory(String categoryId) {
        return categoryRepository.findById(categoryId).orElse(null);
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<Expense>> createExpense(
            @PathVariable("id") String id,
            @Valid @RequestBody CreateExpenseRequest request
    ) {
        User user = getAuthenticatedUser();
        Expense expense = expenseService.createExpense(
                id,
                request.paidByUserId(),
                request.amount(),
                request.currency(),
                request.description(),
                request.categoryId(),
                request.expenseDate(),
                request.expenseAt(),
                request.receiptUrl(),
                request.note(),
                request.splitType(),
                request.participantIds(),
                request.splitValues(),
                user.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(expense));
    }

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<ExpenseTimelineResponse>> getExpenseTimeline(@PathVariable("id") String id) {
        User user = getAuthenticatedUser();
        List<Expense> expenses = expenseService.getWorkspaceExpenses(id, user.getId());

        List<ExpenseTimelineItem> items = expenses.stream().map(e -> {
            String userName = getUserName(e.getPaidByUserId());
            ExpenseCategory cat = getCategory(e.getCategoryId());
            String catName = cat != null ? cat.getName() : "Others";
            String catIcon = cat != null ? cat.getIcon() : "more_horiz";
            String catColor = cat != null ? cat.getColor() : "#607D8B";

            List<SplitAllocationDto> allocations = splitAllocationRepository.findByExpenseId(e.getId())
                    .stream()
                    .map(sa -> new SplitAllocationDto(
                            sa.getUserId(),
                            getUserName(sa.getUserId()),
                            sa.getMoney().getAmount(),
                            sa.getMoney().getCurrency(),
                            sa.getValue()
                    )).toList();

            return new ExpenseTimelineItem(
                    e.getId(),
                    e.getDescription(),
                    e.getMoney().getAmount(),
                    e.getMoney().getCurrency(),
                    e.getPaidByUserId(),
                    userName,
                    e.getCategoryId(),
                    catName,
                    catIcon,
                    catColor,
                    e.getExpenseDate(),
                    e.getExpenseAt(),
                    e.getReceiptUrl(),
                    e.getNote(),
                    e.getSplitType().name(),
                    allocations,
                    e.getCreatedByUserId()
            );
        }).toList();

        // Group by Date, sorted descending
        Map<LocalDate, List<ExpenseTimelineItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(
                        ExpenseTimelineItem::expenseDate,
                        () -> new TreeMap<>(Collections.reverseOrder()),
                        Collectors.toList()
                ));

        List<ExpenseTimelineGroup> groups = grouped.entrySet().stream()
                .map(entry -> new ExpenseTimelineGroup(entry.getKey(), entry.getValue()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(new ExpenseTimelineResponse(groups)));
    }

    @GetMapping("/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseDetailResponse>> getExpenseDetails(
            @PathVariable("id") String id,
            @PathVariable("expenseId") String expenseId
    ) {
        getAuthenticatedUser(); // Verify authentication
        Expense expense = expenseService.getWorkspaceExpenses(id, getAuthenticatedUser().getId())
                .stream()
                .filter(e -> e.getId().equals(expenseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Expense not found or unauthorized"));

        List<SplitAllocationDto> allocations = splitAllocationRepository.findByExpenseId(expense.getId())
                .stream()
                .map(sa -> new SplitAllocationDto(
                        sa.getUserId(),
                        getUserName(sa.getUserId()),
                        sa.getMoney().getAmount(),
                        sa.getMoney().getCurrency(),
                        sa.getValue()
                )).toList();

        return ResponseEntity.ok(ApiResponse.success(new ExpenseDetailResponse(expense, allocations)));
    }

    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<Expense>> updateExpense(
            @PathVariable("id") String id,
            @PathVariable("expenseId") String expenseId,
            @Valid @RequestBody UpdateExpenseRequest request
    ) {
        User user = getAuthenticatedUser();
        Expense expense = expenseService.updateExpense(
                expenseId,
                request.amount(),
                request.currency(),
                request.description(),
                request.categoryId(),
                request.expenseDate(),
                request.splitType(),
                request.participantIds(),
                request.splitValues(),
                request.reason(),
                user.getId()
        );
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable("id") String id,
            @PathVariable("expenseId") String expenseId,
            @Valid @RequestBody DeleteExpenseRequest request
    ) {
        User user = getAuthenticatedUser();
        expenseService.deleteExpense(expenseId, request.reason(), user.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<ActivityResponse>>> getActivities(@PathVariable("id") String id) {
        User user = getAuthenticatedUser();
        // Check membership
        verifyMember(id, user.getId());

        List<ActivityEntry> entries = activityEntryRepository.findByWorkspaceIdOrderByCreatedAtDesc(id);
        List<ActivityResponse> responses = entries.stream().map(entry -> {
            String userName = getUserName(entry.getUserId());
            String detail = null;
            try {
                if (entry.getMetadataJson() != null) {
                    Map<?, ?> map = objectMapper.readValue(entry.getMetadataJson(), Map.class);
                    detail = (String) map.get("description");
                }
            } catch (Exception ignored) {
            }
            return ActivityResponse.fromDomain(entry, userName, detail);
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    private void verifyMember(String workspaceId, String userId) {
        // Enforce membership
        expenseService.getWorkspaceExpenses(workspaceId, userId);
    }
}
