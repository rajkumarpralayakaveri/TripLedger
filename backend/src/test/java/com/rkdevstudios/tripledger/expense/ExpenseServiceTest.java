package com.rkdevstudios.tripledger.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {

    private ExpenseRepository expenseRepository;
    private SplitAllocationRepository splitAllocationRepository;
    private ExpenseHistoryRepository expenseHistoryRepository;
    private WorkspaceRepository workspaceRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private SplitCalculationService splitCalculationService;
    private ApplicationEventPublisher eventPublisher;
    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseRepository = mock(ExpenseRepository.class);
        splitAllocationRepository = mock(SplitAllocationRepository.class);
        expenseHistoryRepository = mock(ExpenseHistoryRepository.class);
        workspaceRepository = mock(WorkspaceRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        splitCalculationService = mock(SplitCalculationService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        expenseService = new ExpenseService(
                expenseRepository,
                splitAllocationRepository,
                expenseHistoryRepository,
                workspaceRepository,
                workspaceMemberRepository,
                splitCalculationService,
                eventPublisher
        );
    }

    @Test
    void testCreateExpense_Success() {
        String workspaceId = "ws_1";
        String payerId = "usr_1";
        BigDecimal amount = BigDecimal.valueOf(1000);
        String currency = "INR";
        List<String> participants = Arrays.asList("usr_1", "usr_2");

        Workspace ws = new Workspace(workspaceId, "Goa", "Fun", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.valueOf(50000), 5, payerId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(ws));

        WorkspaceMember m1 = new WorkspaceMember(workspaceId, "usr_1", MemberRole.ADMIN);
        WorkspaceMember m2 = new WorkspaceMember(workspaceId, "usr_2", MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_1")).thenReturn(Optional.of(m1));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_2")).thenReturn(Optional.of(m2));

        Map<String, BigDecimal> calculatedSplits = new HashMap<>();
        calculatedSplits.put("usr_1", BigDecimal.valueOf(500));
        calculatedSplits.put("usr_2", BigDecimal.valueOf(500));
        when(splitCalculationService.calculateSplits(any(), any(), any(), any()))
                .thenReturn(calculatedSplits);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expense result = expenseService.createExpense(
                workspaceId, payerId, amount, currency, "Lunch", "cat_1",
                LocalDate.now(), SplitType.EQUAL, participants, null, payerId
        );

        assertNotNull(result);
        assertEquals("Lunch", result.getDescription());
        assertEquals(ExpenseStatus.UNSETTLED, result.getStatus());
        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(splitAllocationRepository, times(2)).save(any(SplitAllocation.class));
        verify(eventPublisher, times(1)).publishEvent(any(ExpenseCreatedEvent.class));
    }

    @Test
    void testCreateExpense_MismatchedCurrency() {
        String workspaceId = "ws_1";
        String payerId = "usr_1";
        BigDecimal amount = BigDecimal.valueOf(1000);
        String currency = "USD"; // Mismatched currency (Workspace is INR)
        List<String> participants = Arrays.asList("usr_1", "usr_2");

        Workspace ws = new Workspace(workspaceId, "Goa", "Fun", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.valueOf(50000), 5, payerId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(ws));

        WorkspaceMember m1 = new WorkspaceMember(workspaceId, "usr_1", MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_1")).thenReturn(Optional.of(m1));

        assertThrows(IllegalArgumentException.class, () -> {
            expenseService.createExpense(
                    workspaceId, payerId, amount, currency, "Lunch", "cat_1",
                    LocalDate.now(), SplitType.EQUAL, participants, null, payerId
            );
        });
    }

    @Test
    void testCreateExpense_FutureDate() {
        String workspaceId = "ws_1";
        String payerId = "usr_1";
        BigDecimal amount = BigDecimal.valueOf(1000);
        String currency = "INR";
        List<String> participants = Arrays.asList("usr_1", "usr_2");
        LocalDate futureDate = LocalDate.now().plusDays(1); // Future Date

        Workspace ws = new Workspace(workspaceId, "Goa", "Fun", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.valueOf(50000), 5, payerId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(ws));

        WorkspaceMember m1 = new WorkspaceMember(workspaceId, "usr_1", MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_1")).thenReturn(Optional.of(m1));

        assertThrows(IllegalArgumentException.class, () -> {
            expenseService.createExpense(
                    workspaceId, payerId, amount, currency, "Lunch", "cat_1",
                    futureDate, SplitType.EQUAL, participants, null, payerId
            );
        });
    }

    @Test
    void testDeleteExpense_SoftDelete() {
        String expenseId = "exp_1";
        String workspaceId = "ws_1";
        String payerId = "usr_1";

        Expense expense = new Expense(expenseId, workspaceId, payerId, new Money(BigDecimal.valueOf(1000), "INR"), "Lunch", "cat_1", LocalDate.now());
        expense.setStatus(ExpenseStatus.UNSETTLED);
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        WorkspaceMember m1 = new WorkspaceMember(workspaceId, payerId, MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, payerId)).thenReturn(Optional.of(m1));

        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.deleteExpense(expenseId, "User requested", payerId);

        assertEquals(ExpenseStatus.DELETED, expense.getStatus());
        verify(expenseRepository, times(1)).save(expense);
        verify(eventPublisher, times(1)).publishEvent(any(ExpenseDeletedEvent.class));
    }

    @Test
    void testCreateExpense_WithReceiptUrlAndExpenseAt_PersistsCorrectly() {
        String workspaceId = "ws_1";
        String payerId = "usr_1";
        BigDecimal amount = BigDecimal.valueOf(1500);
        String currency = "INR";
        List<String> participants = Arrays.asList("usr_1", "usr_2");
        Map<String, BigDecimal> shares = new HashMap<>();
        shares.put("usr_1", BigDecimal.valueOf(750));
        shares.put("usr_2", BigDecimal.valueOf(750));

        Workspace ws = new Workspace(workspaceId, "Goa", "Fun", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.valueOf(50000), 5, payerId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(ws));

        WorkspaceMember m1 = new WorkspaceMember(workspaceId, "usr_1", MemberRole.ADMIN);
        WorkspaceMember m2 = new WorkspaceMember(workspaceId, "usr_2", MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_1")).thenReturn(Optional.of(m1));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_2")).thenReturn(Optional.of(m2));

        when(splitCalculationService.calculateSplits(eq(amount), eq(SplitType.EQUAL), eq(participants), any()))
                .thenReturn(shares);

        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        java.time.Instant now = java.time.Instant.now();
        String receiptUrl = "https://res.cloudinary.com/test_cloud/image/upload/receipt_123.jpg";
        String note = "Dinner bill receipt attached";

        Expense created = expenseService.createExpense(
                workspaceId, payerId, amount, currency, "Dinner", "cat_food",
                LocalDate.now(), now, receiptUrl, note, SplitType.EQUAL, participants, null, payerId
        );

        assertNotNull(created);
        assertEquals(receiptUrl, created.getReceiptUrl());
        assertEquals(now, created.getExpenseAt());
        assertEquals(note, created.getNote());

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();
        assertEquals(receiptUrl, saved.getReceiptUrl());
        assertEquals(now, saved.getExpenseAt());
        assertEquals(note, saved.getNote());
    }
}
