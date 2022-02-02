package com.prime.photos.theme

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.PowerManager
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.prime.photos.R
import com.prime.photos.settings.GlobalKeys
import com.primex.extra.*
import com.primex.preferences.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontFamily as FontFamily2

private const val TAG = "Theme"

val FolderShape
    get() = GenericShape { size, _ ->
        val x = size.width
        val y = size.height
        val r = 0.1f * x
        val b = 0.4f * x

        moveTo(r, 0f)
        lineTo(b, 0f)
        lineTo(b + r, r)
        lineTo(x - r, r)
        quadraticBezierTo(x, r, x, 2 * r)
        lineTo(x, y - r)
        quadraticBezierTo(x, y, x - r, y)
        lineTo(r, y)
        quadraticBezierTo(0f, y, 0f, y - r)
        lineTo(0f, r)
        quadraticBezierTo(0f, 0f, r, 0f)
        close()
    }

private val LocalSystemUiController = staticCompositionLocalOf<SystemUiController> {
    error("No ui controller defined!!")
}

/**
 * The default [Color] change Spec
 */
private val DefaultColorAnimSpec = tween<Color>(Anim.durationLong)

/**
 * checks If [GlobalKeys.FORCE_COLORIZE]
 */
val Material.forceColorize
    @Composable inline get() = Preferences.get(LocalContext.current).run {
        get(GlobalKeys.FORCE_COLORIZE).observeAsState().value
    }

private val small2 = RoundedCornerShape(8.dp)

/**
 * A [Material] shape with corners of 8dp
 */
val Material.small2 get() = com.prime.photos.theme.small2

/**
 * Returns if device is in portrait mode.
 */
val Material.isPortrait: Boolean @Composable inline get() = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT


@Composable
private inline fun forceOrElse(condition: Boolean, color: Color, elze: Color) = animateColorAsState(
    targetValue = if (condition) color else elze,
    DefaultColorAnimSpec
)

/**
 * returns [primary] if [requires] is met else [elze].
 * @param requires The condition for primary to return. default value is [requiresAccent]
 * @param elze The color to return if [requires] is  not met The default value is [surface]
 */
@Composable
fun Material.primary(requires: Boolean = forceColorize, elze: Color = colors.surface) =
    forceOrElse(condition = requires, color = Material.colors.primary, elze = elze).value

/**
 * returns [onPrimary] if [requires] is met else [otherwise].
 * @param requires The condition for onPrimary to return. default value is [requiresAccent]
 * @param otherwise The color to return if [requires] is  not met The default value is [onSurface]
 */
@Composable
fun Material.onPrimary(requires: Boolean = forceColorize, otherwise: Color = colors.onSurface) =
    forceOrElse(condition = requires, color = Material.colors.onPrimary, elze = otherwise).value


/**
 * @see primary()
 */
@Composable
fun Material.secondary(requires: Boolean = forceColorize, elze: Color = colors.surface) =
    forceOrElse(condition = requires, color = Material.colors.secondary, elze = elze).value

/**
 * @see onPrimary()
 */
@Composable
fun Material.onSecondary(requires: Boolean = forceColorize, otherwise: Color = colors.onSurface) =
    forceOrElse(condition = requires, color = Material.colors.onSecondary, elze = otherwise).value

@Composable
private fun animate(
    palette: Colors,
    spec: AnimationSpec<Color> = tween(Anim.durationLong)
): Colors {
    return Colors(
        isLight = Material.isLight,
        primary = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.primary
        ).value,
        primaryVariant = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.primaryVariant
        ).value,
        secondary = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.primaryVariant
        ).value,
        secondaryVariant = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.secondaryVariant
        ).value,
        background = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.background
        ).value,
        surface = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.surface
        ).value,
        error = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.error
        ).value,
        onPrimary = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.onPrimary
        ).value,
        onSecondary = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.onSecondary
        ).value,
        onBackground = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.onBackground
        ).value,
        onSurface = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.onSurface
        ).value,
        onError = animateColorAsState(
            animationSpec = spec,
            targetValue = palette.onError
        ).value,
    )
}


private const val OverlayAlpha = 0.16f

val Material.OverlayAlpha get() = com.prime.photos.theme.OverlayAlpha

/**
 * An overlay is color which is suitable for selection or container background.
 * returns [overlay] [compositeOver] [Material.colors.surface].
 */
val Material.overlay
    @Composable inline get() =
        (if (isLight) Color.Black else Color.White).copy(Material.OverlayAlpha) + Material.colors.surface

operator fun Color.plus(surface: Color): Color = compositeOver(surface)

/**
 * return overlay version of primary color compositeOver Surface color.
 * The on color of overlay is [Material.colors.primary]
 */
val Material.primaryOverlay: Color
    @Composable inline get() = Material.colors.primary.copy(OverlayAlpha) + Material.colors.surface

/**
 * @see primaryOverlay
 */
val Material.secondaryOverlay: Color
    @Composable inline get() = Material.colors.secondary.copy(OverlayAlpha) + Material.colors.surface

/**
 * Changes [Color] of statusBar to [color] (if specified else [Material.color.primaryVarient]) if [GlobalKeys.ColorStatusBar] returns true.
 */
@Composable
fun ObserveStatusBar(color: Color = Color.Unspecified) {
    Preferences.get(LocalContext.current).run {
        val controller = LocalSystemUiController.current

        val requires by get(GlobalKeys.COLOR_STATUS_BAR).observeAsState()
        val isLight = Material.isLight

        val paint =
            if (!requires) Color.Transparent else color.takeOrElse { Material.colors.primaryVariant }

        // change status bar icons accordingly
        SideEffect { controller.setStatusBarColor(paint, isLight && !requires) }
    }
}

/**
 *  The [Modifier] performs below actions.
 *   * Observes theme for light/dark.
 *   * Observes [GlobalKeys.COLOR_STATUS_BAR] for force paint accent.
 *   * Adds padding using [statusBarsPadding] and statusBarColor accordingly.
 */
fun Modifier.statusBarDecor(color: Color = Color.Unspecified) = composed {
    ObserveStatusBar(color)
    // add padding
    statusBarsPadding()
        .then(this@composed)
}


private abstract class AutoNightModeManager(
    private val context: Application,
) {
    private var mReceiver: BroadcastReceiver? = null
    lateinit var isDark: State<Boolean>


    fun init() {
        cleanup()
        // update for first time
        isDark = mutableStateOf(
            shouldApplyNightMode()
        )
        val filter = createIntentFilterForBroadcastReceiver()
        if (filter.countActions() == 0) {
            // Null or empty IntentFilter, skip
            return
        }
        if (mReceiver == null) {
            mReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    runBlocking {
                        val isDark = shouldApplyNightMode()
                        Log.d(TAG, "onReceive: $isDark")
                        withContext(Dispatchers.IO) {
                            (this@AutoNightModeManager.isDark as MutableState).value = isDark
                        }
                    }
                }
            }
        }
        context.registerReceiver(mReceiver, filter)
    }


    protected abstract fun shouldApplyNightMode(): Boolean

    fun cleanup() {
        if (mReceiver != null) {
            try {
                context.unregisterReceiver(mReceiver)
            } catch (e: IllegalArgumentException) {
                // If the receiver has already been unregistered, unregisterReceiver() will
                // throw an exception. Just ignore and carry-on...
            }
            mReceiver = null
        }
    }

    fun isListening(): Boolean = mReceiver != null

    abstract fun createIntentFilterForBroadcastReceiver(): IntentFilter
}

private class AutoTimeNightModeManager(context: Application) :
    AutoNightModeManager(context) {

    private val mTwilightManager: TwilightManager by lazy {
        TwilightManager.getInstance(context)
    }

    override fun shouldApplyNightMode() = mTwilightManager.isNight

    override fun createIntentFilterForBroadcastReceiver(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        filter.addAction(Intent.ACTION_TIME_TICK)
        return filter
    }
}

private class AutoBatteryNightModeManager(context: Application) :
    AutoNightModeManager(context) {
    private val mPowerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    override fun shouldApplyNightMode() = mPowerManager.isPowerSaveMode

    override fun createIntentFilterForBroadcastReceiver(): IntentFilter {
        val filter = IntentFilter()
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        return filter
    }
}


/**
 * Observes [NightMode] and returns if the [Light] or [Dark] Theme is required.
 */
val Material.isDarkThemeRequired: Boolean
    @Composable get() {
        val preferences = Preferences.get(LocalContext.current)
        val mode by with(preferences) { get(GlobalKeys.NIGHT_MODE).observeAsState() }
        var manager: AutoNightModeManager? = remember {
            null
        }
        return when (mode) {
            NightMode.YES -> true
            NightMode.NO -> false
            NightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            NightMode.AUTO_BATTER -> {
                val app = (LocalContext.current.applicationContext as Application)
                manager?.cleanup()
                manager = remember {
                    AutoBatteryNightModeManager(app).apply {
                        init()
                    }
                }
                manager.isDark.value
            }
            NightMode.AUTO_TIME -> {
                val app = (LocalContext.current.applicationContext as Application)
                manager?.cleanup()
                manager = remember {
                    AutoTimeNightModeManager(app).apply {
                        init()
                    }
                }
                manager.isDark.value
            }
        }
    }

@Composable
fun Material(isDark: Boolean, content: @Composable() () -> Unit) {
    val context = LocalContext.current
    val preferences = Preferences.get(context = context)

    val colors = with(preferences) {
        val palette by get(if (isDark) GlobalKeys.DARK_COLORS else GlobalKeys.LIGHT_COLORS).observeAsState()
        animate(palette = palette, spec = tween(Anim.durationLong))
    }

    val fontFamily by with(preferences) {
        preferences[GlobalKeys.FONT_FAMILY].map { font ->
            when (font) {
                FontFamily.SYSTEM_DEFAULT -> FontFamily2.Default
                FontFamily.PROVIDED -> FontFamily2(
                    Font(R.font.lato_bold, FontWeight.Bold),
                    Font(R.font.lato_regular, FontWeight.Normal),
                    Font(R.font.lato_light, FontWeight.Light),
                )
                FontFamily.SAN_SERIF -> FontFamily2.SansSerif
                FontFamily.SARIF -> FontFamily2.Serif
                FontFamily.CURSIVE -> FontFamily2.Cursive
            }
        }.observeAsState()
    }

    Log.i(TAG, "Material: $colors")

    CompositionLocalProvider(LocalSystemUiController provides rememberSystemUiController()) {
        MaterialTheme(
            typography = Typography(defaultFontFamily = fontFamily),
            content = content,
            colors = colors
        )
    }
}
