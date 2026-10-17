package com.example.qwant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.qwant.ui.theme.AluminumSurface
import com.example.qwant.ui.theme.FurnaceOrange
import com.example.qwant.ui.theme.GraphiteBackground
import com.example.qwant.ui.theme.GraphiteSurface
import com.example.qwant.ui.theme.MoltenOrange
import com.example.qwant.ui.theme.QwantTheme
import com.example.qwant.ui.theme.SteelBlue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Тесты фирменного дизайна: приложение всегда использует индустриальную
 * палитру предприятия («графит + расплав»), а не системные динамические цвета.
 */
class ThemeDesignTest {

    @get:Rule
    val rule = createComposeRule()

    private data class Colors(
        val primary: Color,
        val background: Color,
        val surface: Color,
        val secondary: Color,
    )

    private fun capture(dark: Boolean): Colors {
        lateinit var captured: Colors
        rule.setContent {
            QwantTheme(darkTheme = dark) {
                captured = Colors(
                    primary = MaterialTheme.colorScheme.primary,
                    background = MaterialTheme.colorScheme.background,
                    surface = MaterialTheme.colorScheme.surface,
                    secondary = MaterialTheme.colorScheme.secondary,
                )
                Box(Modifier.fillMaxSize())
            }
        }
        rule.waitForIdle()
        return captured
    }

    @Test
    fun lightThemeUsesAluminumBackgroundAndMoltenAccent() {
        val colors = capture(dark = false)
        assertEquals(FurnaceOrange, colors.primary)
        assertEquals(AluminumSurface, colors.background)
        assertEquals(SteelBlue, colors.secondary)
    }

    @Test
    fun darkThemeUsesGraphiteShopFloorAndGlowingMeltAccent() {
        val colors = capture(dark = true)
        assertEquals(MoltenOrange, colors.primary)
        assertEquals(GraphiteBackground, colors.background)
        assertEquals(GraphiteSurface, colors.surface)
    }

    @Test
    fun typographyIsEnlargedForTabletReadability() {
        var displaySize = 0f
        rule.setContent {
            QwantTheme {
                displaySize = MaterialTheme.typography.displaySmall.fontSize.value
            }
            Box(Modifier.fillMaxSize())
        }
        rule.waitForIdle()
        assertTrue("displaySmall must be tablet-sized", displaySize >= 32f)
    }
}
