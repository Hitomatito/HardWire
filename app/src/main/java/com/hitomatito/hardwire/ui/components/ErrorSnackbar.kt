package com.hitomatito.hardwire.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class ErrorSeverity {
    INFO, WARNING, ERROR
}

data class AppError(
    val message: String,
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    val retryAction: (() -> Unit)? = null
)

@Composable
fun ErrorSnackbar(
    error: AppError?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        if (error != null) {
            val result = snackbarHostState.showSnackbar(
                message = error.message,
                duration = when (error.severity) {
                    ErrorSeverity.INFO -> SnackbarDuration.Short
                    ErrorSeverity.WARNING -> SnackbarDuration.Long
                    ErrorSeverity.ERROR -> SnackbarDuration.Indefinite
                },
                actionLabel = if (error.retryAction != null) "Reintentar" else null
            )
            
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    error.retryAction?.invoke()
                    onDismiss()
                }
                SnackbarResult.Dismissed -> onDismiss()
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(16.dp)
    ) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = when (error?.severity) {
                ErrorSeverity.ERROR -> MaterialTheme.colorScheme.errorContainer
                ErrorSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = when (error?.severity) {
                ErrorSeverity.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                ErrorSeverity.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
