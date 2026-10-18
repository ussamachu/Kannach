package com.example

import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Authentic motion constants and easing curves for Kannach (كُنَّاش).
 * Follows the emotional UX specification §4.
 */
object KannachMotion {
    const val INSTANT: Int = 120
    const val SOFT: Int = 320
    const val PAGE: Int = 520
    const val CEREMONY: Int = 900

    // Organic settle curve: swift exit, graceful deceleration into place
    val Settle: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // Gentle breathing curve: rhythmic, slow harmonic swell
    val Breathe: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
}

val LocalReduceMotionOverride = androidx.compose.runtime.compositionLocalOf { false }
val LocalHapticsEnabled = androidx.compose.runtime.compositionLocalOf { true }
val LocalHandwritingSp = androidx.compose.runtime.compositionLocalOf { 20 }

/**
 * Inspects system accessibility settings for reduced motion / animator duration scale,
 * or UserSettings.reduceMotion == "on" override via [LocalReduceMotionOverride].
 * When true, callers should collapse animation durations to 0 or INSTANT.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    if (LocalReduceMotionOverride.current) return true
    val context = LocalContext.current
    return remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
}

/**
 * Sub-screen enter transition (D1, G1, S1):
 * Slides up 24.dp and fades in over PAGE (520ms) with Settle easing.
 * If reduceMotion is enabled, performs a quick 120ms fade in.
 */
@Composable
fun kannachSubScreenEnter(reduceMotion: Boolean = rememberReduceMotion()): EnterTransition {
    val density = LocalDensity.current
    val offset24 = remember(density) { with(density) { 24.dp.roundToPx() } }
    return if (reduceMotion) {
        fadeIn(animationSpec = tween(KannachMotion.INSTANT))
    } else {
        fadeIn(animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)) +
            slideInVertically(
                animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle),
                initialOffsetY = { offset24 }
            )
    }
}

/**
 * Sub-screen exit transition (D1, G1, S1):
 * Slides down 12.dp and fades out over SOFT (320ms).
 * If reduceMotion is enabled, performs a quick 120ms fade out.
 */
@Composable
fun kannachSubScreenExit(reduceMotion: Boolean = rememberReduceMotion()): ExitTransition {
    val density = LocalDensity.current
    val offset12 = remember(density) { with(density) { 12.dp.roundToPx() } }
    return if (reduceMotion) {
        fadeOut(animationSpec = tween(KannachMotion.INSTANT))
    } else {
        fadeOut(animationSpec = tween(KannachMotion.SOFT)) +
            slideOutVertically(
                animationSpec = tween(KannachMotion.SOFT),
                targetOffsetY = { offset12 }
            )
    }
}

/**
 * Memory Card View enter transition (D2, M1):
 * Rises 24.dp, scales from 0.96f to 1f, and fades in over PAGE (520ms) with Settle easing.
 */
@Composable
fun kannachCardEnter(reduceMotion: Boolean = rememberReduceMotion()): EnterTransition {
    val density = LocalDensity.current
    val offset24 = remember(density) { with(density) { 24.dp.roundToPx() } }
    return if (reduceMotion) {
        fadeIn(animationSpec = tween(KannachMotion.INSTANT))
    } else {
        scaleIn(
            initialScale = 0.96f,
            animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
        ) + slideInVertically(
            animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle),
            initialOffsetY = { offset24 }
        ) + fadeIn(
            animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle)
        )
    }
}

/**
 * Memory Card View exit transition (D2, M1):
 * Slides down 12.dp, scales to 0.96f, and fades out over SOFT (320ms).
 */
@Composable
fun kannachCardExit(reduceMotion: Boolean = rememberReduceMotion()): ExitTransition {
    val density = LocalDensity.current
    val offset12 = remember(density) { with(density) { 12.dp.roundToPx() } }
    return if (reduceMotion) {
        fadeOut(animationSpec = tween(KannachMotion.INSTANT))
    } else {
        scaleOut(
            targetScale = 0.96f,
            animationSpec = tween(KannachMotion.SOFT)
        ) + slideOutVertically(
            animationSpec = tween(KannachMotion.SOFT),
            targetOffsetY = { offset12 }
        ) + fadeOut(
            animationSpec = tween(KannachMotion.SOFT)
        )
    }
}

/**
 * Entry Edit Bottom Sheet enter transition (D2, M1):
 * Smooth upward slide from bottom with PAGE duration and Settle easing.
 */
@Composable
fun kannachSheetEnter(reduceMotion: Boolean = rememberReduceMotion()): EnterTransition {
    return if (reduceMotion) {
        fadeIn(animationSpec = tween(KannachMotion.INSTANT))
    } else {
        slideInVertically(
            animationSpec = tween(KannachMotion.PAGE, easing = KannachMotion.Settle),
            initialOffsetY = { it }
        ) + fadeIn(animationSpec = tween(KannachMotion.SOFT))
    }
}

/**
 * Entry Edit Bottom Sheet exit transition (D2, M1):
 * Smooth downward slide to bottom with SOFT duration.
 */
@Composable
fun kannachSheetExit(reduceMotion: Boolean = rememberReduceMotion()): ExitTransition {
    return if (reduceMotion) {
        fadeOut(animationSpec = tween(KannachMotion.INSTANT))
    } else {
        slideOutVertically(
            animationSpec = tween(KannachMotion.SOFT),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(KannachMotion.SOFT))
    }
}
