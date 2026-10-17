package com.example.qwant

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.qwant.data.QwantApi
import com.example.qwant.data.Role
import com.example.qwant.screens.AuthScreen
import com.example.qwant.screens.AuthTags
import com.example.qwant.screens.AuthViewModel
import com.example.qwant.ui.theme.QwantTheme
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Тесты экрана входа: поля, кнопка, ошибки и успешный вход. */
class AuthScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(
        viewModel: AuthViewModel,
        onLoggedIn: (String) -> Unit = {},
    ) {
        rule.setContent {
            QwantTheme {
                AuthScreen(onLoggedIn = onLoggedIn, viewModel = viewModel)
            }
        }
    }

    @Test
    fun loginScreenShowsBrandedFieldsAndDisabledSubmit() {
        show(AuthViewModel(loginCall = { _, _ -> TestData.driverLogin() }, healthCheck = { true }))

        rule.onNodeWithText("Вход в систему").assertExists()
        rule.onNodeWithTag(AuthTags.LOGIN).assertExists()
        rule.onNodeWithTag(AuthTags.PASSWORD).assertExists()
        rule.onNodeWithTag(AuthTags.SUBMIT).assertExists()
        rule.onNodeWithTag(AuthTags.SUBMIT).assertIsNotEnabled()
    }

    @Test
    fun submitBecomesEnabledAfterFillingBothFields() {
        show(AuthViewModel(loginCall = { _, _ -> TestData.driverLogin() }, healthCheck = { true }))

        rule.onNodeWithTag(AuthTags.LOGIN).performTextInput("ivanov")
        rule.onNodeWithTag(AuthTags.SUBMIT).assertIsNotEnabled() // пароль ещё пуст
        rule.onNodeWithTag(AuthTags.PASSWORD).performTextInput("1234")
        rule.onNodeWithTag(AuthTags.SUBMIT).assertIsEnabled()
    }

    @Test
    fun wrongCredentialsShowARussianError() {
        show(
            AuthViewModel(
                loginCall = { _, _ -> throw QwantApi.UnauthorizedException("invalid") },
                healthCheck = { true },
            )
        )

        rule.onNodeWithTag(AuthTags.LOGIN).performTextInput("ivanov")
        rule.onNodeWithTag(AuthTags.PASSWORD).performTextInput("wrong")
        rule.onNodeWithTag(AuthTags.SUBMIT).performClick()

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTagSafe(AuthTags.ERROR)
        }
        rule.onNodeWithText("Неверный логин или пароль").assertExists()
    }

    @Test
    fun successfulLoginReportsTheRoleUpwards() {
        val role = AtomicReference<String?>(null)
        show(
            viewModel = AuthViewModel(loginCall = { _, _ -> TestData.dispatcherLogin() }, healthCheck = { true }),
            onLoggedIn = { role.set(it) },
        )

        rule.onNodeWithTag(AuthTags.LOGIN).performTextInput("dispatcher")
        rule.onNodeWithTag(AuthTags.PASSWORD).performTextInput("1234")
        rule.onNodeWithTag(AuthTags.SUBMIT).performClick()

        rule.waitUntil(timeoutMillis = 5_000) { role.get() != null }
        assertEquals(Role.DISPATCHER, role.get())
    }

    @Test
    fun networkFailureShowsTheServerError() {
        show(
            AuthViewModel(
                loginCall = { _, _ -> throw IOException("connection refused") },
                healthCheck = { false },
            )
        )

        rule.onNodeWithTag(AuthTags.LOGIN).performTextInput("ivanov")
        rule.onNodeWithTag(AuthTags.PASSWORD).performTextInput("1234")
        rule.onNodeWithTag(AuthTags.SUBMIT).performClick()

        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTagSafe(AuthTags.ERROR)
        }
        rule.onNodeWithText("Сервер недоступен. Проверьте адрес сервера.").assertExists()
    }
}

/** Есть ли на экране хотя бы один узел с данным тегом. */
private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagSafe(tag: String): Boolean =
    onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
