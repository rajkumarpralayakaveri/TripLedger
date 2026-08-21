package com.rkdevstudios.tripledger.expense;

import com.rkdevstudios.tripledger.expense.api.*;
import com.rkdevstudios.tripledger.expense.application.ExpenseService;
import com.rkdevstudios.tripledger.expense.application.SplitCalculationService;
import com.rkdevstudios.tripledger.expense.domain.*;
import com.rkdevstudios.tripledger.workspace.domain.MemberRole;
import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import com.rkdevstudios.tripledger.common.ApiResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpenseSplitIntegrationTest {

    private ExpenseService expenseService;
    private ExpenseRepository expenseRepository;
    private SplitAllocationRepository splitAllocationRepository;
    private ExpenseHistoryRepository expenseHistoryRepository;
    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private SplitCalculationService splitCalculationService;
    private UserRepository userRepository;
    private ExpenseCategoryRepository categoryRepository;
    private ExpenseController expenseController;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        splitAllocationRepository = mock(SplitAllocationRepository.class);
        expenseHistoryRepository = mock(ExpenseHistoryRepository.class);
        workspaceRepository = mock(WorkspaceRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        splitCalculationService = new SplitCalculationService(); // Using real engine to verify splits!
        userRepository = mock(UserRepository.class);
        categoryRepository = mock(ExpenseCategoryRepository.class);

        expenseService = new ExpenseService(
                expenseRepository,
                splitAllocationRepository,
                expenseHistoryRepository,
                workspaceRepository,
                workspaceMemberRepository,
                splitCalculationService,
                mock(org.springframework.context.ApplicationEventPublisher.class)
        );

        expenseController = new ExpenseController(
                expenseService,
                userRepository,
                categoryRepository,
                mock(com.rkdevstudios.tripledger.expense.domain.ActivityEntryRepository.class),
                splitAllocationRepository
        );

        // Mock security context
        User authUser = new User("usr_1", "Raj", "raj@example.com", "pass", null);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(authUser);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById("usr_1")).thenReturn(Optional.of(authUser));
        when(userRepository.findById("usr_2")).thenReturn(Optional.of(new User("usr_2", "Amit", "amit@example.com", "pass", null)));
        when(userRepository.findById("usr_3")).thenReturn(Optional.of(new User("usr_3", "Neha", "neha@example.com", "pass", null)));

        // Setup workspace and members
        Workspace ws = new Workspace("ws_1", "Goa", "Trip", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.valueOf(50000), 5, "usr_1");
        when(workspaceRepository.findById("ws_1")).thenReturn(Optional.of(ws));

        WorkspaceMember wm1 = new WorkspaceMember("ws_1", "usr_1", MemberRole.ADMIN);
        WorkspaceMember wm2 = new WorkspaceMember("ws_1", "usr_2", MemberRole.MEMBER);
        WorkspaceMember wm3 = new WorkspaceMember("ws_1", "usr_3", MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId("ws_1", "usr_1")).thenReturn(Optional.of(wm1));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId("ws_1", "usr_2")).thenReturn(Optional.of(wm2));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId("ws_1", "usr_3")).thenReturn(Optional.of(wm3));

        when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void testEqualSplitIntegration() {
        List<String> participants = Arrays.asList("usr_1", "usr_2", "usr_3");
        List<SplitAllocation> savedAllocations = new ArrayList<>();
        when(splitAllocationRepository.save(any(SplitAllocation.class))).thenAnswer(i -> {
            SplitAllocation sa = i.getArgument(0);
            savedAllocations.add(sa);
            return sa;
        });

        Expense expense = expenseService.createExpense(
                "ws_1", "usr_1", BigDecimal.valueOf(100), "INR", "Lunch", "cat_general",
                LocalDate.now(), java.time.Instant.now(), null, null, SplitType.EQUAL, participants, null, "usr_1"
        );

        assertEquals(3, savedAllocations.size());
        assertEquals(0, savedAllocations.get(0).getMoney().getAmount().compareTo(BigDecimal.valueOf(33.3333)));
        assertEquals(0, savedAllocations.get(1).getMoney().getAmount().compareTo(BigDecimal.valueOf(33.3333)));
        assertEquals(0, savedAllocations.get(2).getMoney().getAmount().compareTo(BigDecimal.valueOf(33.3334))); // verify remainder assignment
    }

    @Test
    void testExactSplitIntegration_Success() {
        List<String> participants = Arrays.asList("usr_1", "usr_2");
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("usr_1", BigDecimal.valueOf(60));
        values.put("usr_2", BigDecimal.valueOf(40));

        List<SplitAllocation> savedAllocations = new ArrayList<>();
        when(splitAllocationRepository.save(any(SplitAllocation.class))).thenAnswer(i -> {
            SplitAllocation sa = i.getArgument(0);
            savedAllocations.add(sa);
            return sa;
        });

        Expense expense = expenseService.createExpense(
                "ws_1", "usr_1", BigDecimal.valueOf(100), "INR", "Lunch", "cat_general",
                LocalDate.now(), java.time.Instant.now(), null, null, SplitType.EXACT, participants, values, "usr_1"
        );

        assertEquals(2, savedAllocations.size());
        assertEquals(0, savedAllocations.get(0).getMoney().getAmount().compareTo(BigDecimal.valueOf(60)));
        assertEquals(0, savedAllocations.get(1).getMoney().getAmount().compareTo(BigDecimal.valueOf(40)));
    }

    @Test
    void testExactSplitIntegration_InvalidSumThrows() {
        List<String> participants = Arrays.asList("usr_1", "usr_2");
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("usr_1", BigDecimal.valueOf(60));
        values.put("usr_2", BigDecimal.valueOf(30)); // 60 + 30 != 100

        assertThrows(IllegalArgumentException.class, () -> {
            expenseService.createExpense(
                    "ws_1", "usr_1", BigDecimal.valueOf(100), "INR", "Lunch", "cat_general",
                    LocalDate.now(), java.time.Instant.now(), null, null, SplitType.EXACT, participants, values, "usr_1"
            );
        });
    }

    @Test
    void testPercentageSplitIntegration_Success() {
        List<String> participants = Arrays.asList("usr_1", "usr_2", "usr_3");
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("usr_1", BigDecimal.valueOf(33.33));
        values.put("usr_2", BigDecimal.valueOf(33.33));
        values.put("usr_3", BigDecimal.valueOf(33.34));

        List<SplitAllocation> savedAllocations = new ArrayList<>();
        when(splitAllocationRepository.save(any(SplitAllocation.class))).thenAnswer(i -> {
            SplitAllocation sa = i.getArgument(0);
            savedAllocations.add(sa);
            return sa;
        });

        Expense expense = expenseService.createExpense(
                "ws_1", "usr_1", BigDecimal.valueOf(1000), "INR", "Lunch", "cat_general",
                LocalDate.now(), java.time.Instant.now(), null, null, SplitType.PERCENTAGE, participants, values, "usr_1"
        );

        assertEquals(3, savedAllocations.size());
        assertEquals(0, savedAllocations.get(0).getMoney().getAmount().compareTo(BigDecimal.valueOf(333.3000)));
        assertEquals(0, savedAllocations.get(1).getMoney().getAmount().compareTo(BigDecimal.valueOf(333.3000)));
        assertEquals(0, savedAllocations.get(2).getMoney().getAmount().compareTo(BigDecimal.valueOf(333.4000)));
    }

    @Test
    void testSharesSplitIntegration_Success() {
        List<String> participants = Arrays.asList("usr_1", "usr_2", "usr_3");
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("usr_1", BigDecimal.valueOf(1));
        values.put("usr_2", java.math.BigDecimal.valueOf(2));
        values.put("usr_3", java.math.BigDecimal.valueOf(3));

        List<SplitAllocation> savedAllocations = new ArrayList<>();
        when(splitAllocationRepository.save(any(SplitAllocation.class))).thenAnswer(i -> {
            SplitAllocation sa = i.getArgument(0);
            savedAllocations.add(sa);
            return sa;
        });

        Expense expense = expenseService.createExpense(
                "ws_1", "usr_1", BigDecimal.valueOf(600), "INR", "Lunch", "cat_general",
                LocalDate.now(), java.time.Instant.now(), null, null, SplitType.SHARES, participants, values, "usr_1"
        );

        assertEquals(3, savedAllocations.size());
        assertEquals(0, savedAllocations.get(0).getMoney().getAmount().compareTo(BigDecimal.valueOf(100)));
        assertEquals(0, savedAllocations.get(1).getMoney().getAmount().compareTo(BigDecimal.valueOf(200)));
        assertEquals(0, savedAllocations.get(2).getMoney().getAmount().compareTo(BigDecimal.valueOf(300)));
    }

    @Test
    void testGetExpenseDetailsEndpointReturnsCalculatedAllocations() {
        Expense mockExpense = new Expense("exp_100", "ws_1", "usr_1", new Money(BigDecimal.valueOf(600), "INR"), "Dinner", "cat_food", LocalDate.now(), java.time.Instant.now(), null, null, SplitType.SHARES);
        when(expenseRepository.findByWorkspaceIdAndStatusNot(anyString(), any())).thenReturn(Collections.singletonList(mockExpense));

        List<SplitAllocation> mockAllocations = Arrays.asList(
            new SplitAllocation("sa_1", "exp_100", "usr_1", new Money(BigDecimal.valueOf(100), "INR"), BigDecimal.valueOf(1)),
            new SplitAllocation("sa_2", "exp_100", "usr_2", new Money(BigDecimal.valueOf(200), "INR"), BigDecimal.valueOf(2)),
            new SplitAllocation("sa_3", "exp_100", "usr_3", new Money(BigDecimal.valueOf(300), "INR"), BigDecimal.valueOf(3))
        );
        when(splitAllocationRepository.findByExpenseId("exp_100")).thenReturn(mockAllocations);

        ResponseEntity<ApiResponse<ExpenseDetailResponse>> responseEntity = expenseController.getExpenseDetails("ws_1", "exp_100");
        assertNotNull(responseEntity.getBody());
        ExpenseDetailResponse response = responseEntity.getBody().getData();
        assertNotNull(response);
        assertEquals("exp_100", response.expense().getId());
        assertEquals(3, response.splitAllocations().size());
        assertEquals(0, response.splitAllocations().get(0).amount().compareTo(BigDecimal.valueOf(100)));
        assertEquals("Raj", response.splitAllocations().get(0).name());
        assertEquals(0, response.splitAllocations().get(1).amount().compareTo(BigDecimal.valueOf(200)));
        assertEquals("Amit", response.splitAllocations().get(1).name());
        assertEquals(0, response.splitAllocations().get(2).amount().compareTo(BigDecimal.valueOf(300)));
        assertEquals("Neha", response.splitAllocations().get(2).name());
    }
}
