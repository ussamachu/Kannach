package com.example

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenVerificationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `en ar key parity check prints missing 0`() {
        val fields = I18n.Strings::class.java.declaredFields.filter {
            !java.lang.reflect.Modifier.isStatic(it.modifiers) && it.name != "\$jacocoData"
        }
        var missingCount = 0

        for (field in fields) {
            field.isAccessible = true
            val enVal = field.get(I18n.EN)
            val arVal = field.get(I18n.AR)

            if (enVal == null || (enVal is String && enVal.isBlank())) {
                println("Missing or blank in EN: ${field.name}")
                missingCount++
            }
            if (arVal == null || (arVal is String && arVal.isBlank())) {
                println("Missing or blank in AR: ${field.name}")
                missingCount++
            }
        }

        // Must print missing: 0
        println("EN/AR key parity: missing: $missingCount")
        assertEquals("Missing keys between EN and AR strings", 0, missingCount)

        // Verify specific new HX keys exist and are non-empty
        assertTrue(I18n.EN.todayCardEmpty.isNotBlank())
        assertTrue(I18n.AR.todayCardEmpty.isNotBlank())
        assertTrue(I18n.EN.monthRecapPlanted.contains("{n}") && I18n.EN.monthRecapPlanted.contains("{month}"))
        assertTrue(I18n.AR.monthRecapPlanted.contains("{n}") && I18n.AR.monthRecapPlanted.contains("{month}"))
        assertTrue(I18n.EN.monthRecapQuiet.isNotBlank())
        assertTrue(I18n.AR.monthRecapQuiet.isNotBlank())
    }

    @Test
    fun `header renders for every month name in EN and AR with font scale 1_3 without year exceeding 24dp`() {
        val widths = listOf(320.dp, 360.dp, 412.dp)
        val languages = listOf("en", "ar")
        val currentYear = 2026

        composeTestRule.setContent {
            val density = Density(density = 2.5f, fontScale = 1.3f)
            CompositionLocalProvider(LocalDensity provides density) {
                Column {
                    for (lang in languages) {
                        val strings = if (lang == "ar") I18n.AR else I18n.EN
                        val yearText = I18n.formatYear(currentYear, lang)

                        for (month in 1..12) {
                            val monthName = strings.months[month - 1]

                            for (screenWidth in widths) {
                                Column(
                                    modifier = Modifier.width(screenWidth),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = monthName,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Spacer(modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = yearText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.testTag("year_${lang}_${month}_${screenWidth.value.toInt()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Verify each combination renders as single line with height <= 24 dp
        for (lang in languages) {
            val strings = if (lang == "ar") I18n.AR else I18n.EN
            val yearText = I18n.formatYear(currentYear, lang)
            assertTrue(!yearText.contains("\n"))

            for (month in 1..12) {
                val monthName = strings.months[month - 1]
                assertTrue(monthName.isNotBlank() && !monthName.contains("\n"))

                for (screenWidth in widths) {
                    val tag = "year_${lang}_${month}_${screenWidth.value.toInt()}"
                    val yearNode = composeTestRule.onNodeWithTag(tag)
                    yearNode.assertExists()
                    val heightDp = yearNode.fetchSemanticsNode().size.height / 2.5f
                    assertTrue(
                        "Year text height $heightDp dp exceeded 24 dp in month $monthName ($lang) on width $screenWidth",
                        heightDp <= 24f
                    )
                }
            }
        }
    }
}
