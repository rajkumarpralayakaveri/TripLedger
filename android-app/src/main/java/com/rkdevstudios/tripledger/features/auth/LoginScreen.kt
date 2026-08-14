package com.rkdevstudios.tripledger.features.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.auth.AuthState
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripLoadingIndicator
import com.rkdevstudios.tripledger.core.designsystem.components.TripTextField
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToDashboard()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegisterMode) "Create Account" else "Welcome to TripLedger",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(TripSpacing.L))

        if (isRegisterMode) {
            TripTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name"
            )
            Spacer(modifier = Modifier.height(TripSpacing.S))
        }

        TripTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address"
        )

        Spacer(modifier = Modifier.height(TripSpacing.S))

        TripTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password"
        )

        Spacer(modifier = Modifier.height(TripSpacing.M))

        if (authState is AuthState.Loading) {
            TripLoadingIndicator()
        } else {
            TripButton(
                text = if (isRegisterMode) "Register" else "Log In",
                onClick = {
                    if (isRegisterMode) {
                        viewModel.registerWithEmail(name, email, password)
                    } else {
                        viewModel.loginWithEmail(email, password)
                    }
                }
            )

            if (!isRegisterMode) {
                Spacer(modifier = Modifier.height(TripSpacing.S))

                TripButton(
                    text = "Sign In with Google",
                    onClick = { viewModel.loginWithGoogle("mock_google_token") }
                )
            }

            Spacer(modifier = Modifier.height(TripSpacing.M))

            Text(
                text = if (isRegisterMode) "Already have an account? Log In" else "Don't have an account? Sign Up",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable { isRegisterMode = !isRegisterMode }
                    .padding(TripSpacing.S)
            )
        }

        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = TripSpacing.S)
            )
        }
    }
}
