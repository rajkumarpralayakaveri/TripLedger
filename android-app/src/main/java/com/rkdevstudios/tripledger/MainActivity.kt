package com.rkdevstudios.tripledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.auth.SessionManager
import com.rkdevstudios.tripledger.core.auth.SharedPreferencesSessionStore
import com.rkdevstudios.tripledger.core.network.RetrofitClient
import com.rkdevstudios.tripledger.core.designsystem.theme.TripLedgerTheme
import com.rkdevstudios.tripledger.features.auth.AuthViewModel
import com.rkdevstudios.tripledger.features.auth.LoginScreen
import com.rkdevstudios.tripledger.features.auth.SplashScreen
import com.rkdevstudios.tripledger.features.auth.data.AuthRepository
import com.rkdevstudios.tripledger.features.auth.data.api.AuthApiService
import com.rkdevstudios.tripledger.features.workspace.CreateWorkspaceScreen
import com.rkdevstudios.tripledger.features.workspace.DashboardScreen
import com.rkdevstudios.tripledger.features.workspace.InviteMembersScreen
import com.rkdevstudios.tripledger.features.workspace.JoinWorkspaceScreen
import com.rkdevstudios.tripledger.features.workspace.WorkspaceDetailsScreen
import com.rkdevstudios.tripledger.features.workspace.PaymentSubmissionScreen
import com.rkdevstudios.tripledger.features.workspace.PaymentVerificationScreen
import com.rkdevstudios.tripledger.features.workspace.WorkspaceViewModel
import com.rkdevstudios.tripledger.features.workspace.data.WorkspaceRepository
import com.rkdevstudios.tripledger.features.workspace.data.PaymentProofRepository
import com.rkdevstudios.tripledger.features.workspace.PaymentProofViewModel
import com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceApiService
import com.rkdevstudios.tripledger.features.expense.presentation.ExpenseDetailScreen
import com.rkdevstudios.tripledger.features.expense.presentation.ExpenseTimelineScreen
import com.rkdevstudios.tripledger.features.expense.presentation.AddEditExpenseScreen
import com.rkdevstudios.tripledger.features.expense.presentation.ActivityFeedScreen
import com.rkdevstudios.tripledger.features.settlement.presentation.BalancesScreen
import com.rkdevstudios.tripledger.features.settlement.presentation.SettlementPlanScreen
import com.rkdevstudios.tripledger.features.settlement.presentation.SettlementHistoryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SharedPreferencesSessionStore(applicationContext)
        val sessionManager = SessionManager(sessionStore)
        val retrofitClient = RetrofitClient(sessionManager)
        
        val authApiService = retrofitClient.createService<AuthApiService>()
        val authRepository = AuthRepository(authApiService, sessionManager)
        
        val workspaceApiService = retrofitClient.createService<WorkspaceApiService>()
        val workspaceRepository = WorkspaceRepository(workspaceApiService)
        val paymentProofRepository = PaymentProofRepository(workspaceApiService)
        
        val factory = ViewModelFactory(sessionStore, sessionManager, authRepository, workspaceRepository, paymentProofRepository)
        val authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
        val workspaceViewModel = ViewModelProvider(this, factory)[WorkspaceViewModel::class.java]
        val paymentProofViewModel = ViewModelProvider(this, factory)[PaymentProofViewModel::class.java]

        setContent {
            TripLedgerTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                },
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToDashboard = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = workspaceViewModel,
                                onCreateWorkspace = { navController.navigate("create-workspace") },
                                onJoinWorkspace = { navController.navigate("join-workspace") },
                                onNavigateToDetails = { id -> navController.navigate("workspace-details/$id") }
                            )
                        }
                        composable("create-workspace") {
                            CreateWorkspaceScreen(
                                viewModel = workspaceViewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable("join-workspace") {
                            JoinWorkspaceScreen(
                                viewModel = workspaceViewModel,
                                onNavigateBack = { navController.navigateUp() },
                                onJoinSuccess = { workspaceId ->
                                    navController.navigate("workspace-details/$workspaceId") {
                                        popUpTo("join-workspace") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            route = "workspace-details/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            val currentUserId = sessionStore.getSession()?.userId.orEmpty()
                            WorkspaceDetailsScreen(
                                workspaceId = workspaceId,
                                viewModel = workspaceViewModel,
                                currentUserId = currentUserId,
                                onNavigateBack = { navController.navigateUp() },
                                onInviteMembers = { id -> navController.navigate("invite-members/$id") },
                                onNavigateToExpenses = { id -> navController.navigate("expense-timeline/$id") },
                                onNavigateToSettlements = { id -> navController.navigate("balances/$id") },
                                onNavigateToSubmission = { id -> navController.navigate("payment-submission/$id") },
                                onNavigateToVerification = { id -> navController.navigate("payment-verification/$id") }
                            )
                        }

                        composable(
                            route = "payment-submission/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            val currentUserId = sessionStore.getSession()?.userId.orEmpty()
                            PaymentSubmissionScreen(
                                workspaceId = workspaceId,
                                viewModel = paymentProofViewModel,
                                workspaceViewModel = workspaceViewModel,
                                currentUserId = currentUserId,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }

                        composable(
                            route = "payment-verification/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            val currentUserId = sessionStore.getSession()?.userId.orEmpty()
                            PaymentVerificationScreen(
                                workspaceId = workspaceId,
                                viewModel = paymentProofViewModel,
                                workspaceViewModel = workspaceViewModel,
                                verifierId = currentUserId,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable(
                            route = "invite-members/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            InviteMembersScreen(
                                workspaceId = workspaceId,
                                viewModel = workspaceViewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable(
                            route = "expense-timeline/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            ExpenseTimelineScreen(
                                workspaceId = workspaceId,
                                viewModel = workspaceViewModel,
                                onAddExpense = { navController.navigate("add-expense/$workspaceId") },
                                onExpenseClick = { expenseId -> navController.navigate("expense-detail/$workspaceId/$expenseId") },
                                onNavigateBack = { navController.navigateUp() },
                                onNavigateToActivities = { navController.navigate("activities/$workspaceId") }
                            )
                        }
                        composable(
                            route = "expense-detail/{wsId}/{expenseId}",
                            arguments = listOf(
                                navArgument("wsId") { type = NavType.StringType },
                                navArgument("expenseId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("wsId").orEmpty()
                            val expenseId = backStackEntry.arguments?.getString("expenseId").orEmpty()
                            ExpenseDetailScreen(
                                workspaceId = workspaceId,
                                expenseId = expenseId,
                                viewModel = workspaceViewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable(
                            route = "add-expense/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            AddEditExpenseScreen(
                                workspaceId = workspaceId,
                                viewModel = workspaceViewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable(
                            route = "activities/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            ActivityFeedScreen(
                                workspaceId = workspaceId,
                                viewModel = workspaceViewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable(
                            route = "balances/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            BalancesScreen(
                                workspaceId = workspaceId,
                                viewModel = workspaceViewModel,
                                onNavigateToPlan = { navController.navigate("settlement-plan/$workspaceId") },
                                onNavigateToHistory = { navController.navigate("settlement-history/$workspaceId") },
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable(
                            route = "settlement-plan/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            SettlementPlanScreen(
                                workspaceId = workspaceId,
                                viewModel = workspaceViewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                        composable(
                            route = "settlement-history/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val workspaceId = backStackEntry.arguments?.getString("id").orEmpty()
                            SettlementHistoryScreen(
                                workspaceId = workspaceId,
                                viewModel = workspaceViewModel,
                                onNavigateBack = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}