package com.example

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat

/**
 * Section 8 — Arabic handwriting font (fixes Y7):
 * - FT1 — One swap point: exposes [handAr] used for every Arabic handwritten text:
 *   diary body, letters body, sketch labels, today card, and the export signature.
 *   No composable references a font resource directly for Arabic hand text.
 * - FT2 — Default until the owner decides: keeps the current Arabic hand font (Playpen Sans Arabic).
 * - FT3 — Swapping: change only [handArResId] / [handAr] here to swap across the entire app.
 * - FT4 — Licence: SIL Open Font License 1.1 from Google Fonts.
 */
object KannachFonts {

    const val handArFontName: String = "Playpen Sans Arabic"
    const val handArFontLicense: String = "SIL Open Font License 1.1 (Google Fonts)"

    @FontRes
    val handArResId: Int = R.font.playpen_sans_arabic

    @FontRes
    val handLatinResId: Int = R.font.playpen_sans

    /**
     * FT1 & FT2: Single swap point for all Arabic handwritten text across Kannach.
     */
    val handAr: FontFamily = FontFamily(
        Font(handArResId, FontWeight.Normal)
    )

    val handLatin: FontFamily = FontFamily(
        Font(handLatinResId, FontWeight.Normal)
    )

    /**
     * Resolves the Android [Typeface] for Canvas-based rendering (e.g., export signature in StrokeMath)
     * from the single swap point.
     */
    fun resolveHandTypeface(context: Context, isRtl: Boolean): Typeface {
        val resId = if (isRtl) handArResId else handLatinResId
        return try {
            ResourcesCompat.getFont(context, resId)
        } catch (_: Exception) {
            null
        } ?: Typeface.DEFAULT_BOLD
    }
}
