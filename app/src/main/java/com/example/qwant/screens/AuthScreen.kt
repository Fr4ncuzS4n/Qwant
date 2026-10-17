package com.example.qwant.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qwant.R
import com.example.qwant.data.SessionStore
import com.example.qwant.ui.SectionCard
import com.example.qwant.ui.theme.GraphiteBackground
import com.example.qwant.ui.theme.GraphiteSurface
import com.example.qwant.ui.theme.MoltenOrange

/**
 * Экран входа: логин и пароль пользователя. Роль (диспетчер/водитель)
 * определяется учётной записью на сервере, поэтому выбор кабинета не нужен.
 *
 * Настройка адреса сервера вынесена на отдельный служебный экран
 * «Для разработчика» ([DeveloperScreen]) — сюда ведёт [onOpenDeveloper].
 */
@Composable
fun AuthScreen(
    onLoggedIn: (role: String) -> Unit,
    onOpenDeveloper: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Успешный вход: сохраняем сессию и передаём роль наверх (навигация по ролям).
    val loggedIn = state.loggedIn
    LaunchedEffect(loggedIn) {
        if (loggedIn != null) {
            SessionStore.save(context, loggedIn)
            onLoggedIn(loggedIn.user.role)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                // Клавиатура: экран не «проваливается» под неё — область прокрутки
                // заканчивается над клавиатурой (edge-to-edge + adjustResize).
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            BrandHeader()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                SectionCard(title = stringResource(R.string.auth_title)) {
                    OutlinedTextField(
                        value = state.login,
                        onValueChange = viewModel::updateLogin,
                        label = { Text(stringResource(R.string.auth_login)) },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AuthTags.LOGIN),
                    )

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text(stringResource(R.string.auth_password)) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            TextButton(onClick = viewModel::togglePasswordVisibility) {
                                Text(
                                    text = stringResource(
                                        if (state.passwordVisible) R.string.auth_hide_password
                                        else R.string.auth_show_password
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (state.passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AuthTags.PASSWORD),
                    )

                    state.error?.let { error ->
                        Text(
                            text = stringResource(error.messageRes()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(AuthTags.ERROR),
                        )
                    }

                    Button(
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(AuthTags.SUBMIT),
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.auth_submit),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Text(
                        text = stringResource(R.string.auth_demo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Настройка адреса сервера — на отдельном служебном экране.
                TextButton(
                    onClick = onOpenDeveloper,
                    modifier = Modifier.fillMaxWidth().testTag(AuthTags.DEVELOPER),
                ) {
                    Text(stringResource(R.string.developer_open))
                }
            }
        }
    }
}

/** Фирменная «шапка»: графитовый градиент с логотипом-электролизёром. */
@Composable
private fun BrandHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(listOf(GraphiteSurface, GraphiteBackground))
            )
            .padding(horizontal = 24.dp, vertical = 36.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_electrolysis_logo),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
            )
            Text(
                text = stringResource(R.string.role_title),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.role_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MoltenOrange,
            )
        }
    }
}

/** Русский текст ошибки входа. */
private fun AuthError.messageRes(): Int = when (this) {
    AuthError.REQUIRED -> R.string.auth_error_required
    AuthError.INVALID -> R.string.auth_error_invalid
    AuthError.NETWORK -> R.string.auth_error_network
}

/** Test-теги экрана входа (используются в инструментальных тестах дизайна). */
object AuthTags {
    const val LOGIN = "auth_login_field"
    const val PASSWORD = "auth_password_field"
    const val SUBMIT = "auth_submit_button"
    const val ERROR = "auth_error_text"
    const val DEVELOPER = "auth_developer_button"
}
