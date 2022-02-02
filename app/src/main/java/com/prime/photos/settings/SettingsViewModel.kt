package com.prime.photos.settings

import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import com.prime.photos.theme.FontFamily
import com.prime.photos.theme.NightMode
import com.primex.extra.*
import com.primex.preferences.StringSaver
import com.primex.preferences.booleanPreferenceKey
import com.primex.preferences.stringPreferenceKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json

/**
 * A shot alias for [SettingsViewModel.Companion]
 */
typealias GlobalKeys = SettingsViewModel.Companion

private const val TAG = "SettingsViewModel"

class SettingsViewModel : ViewModel() {


    companion object {
        /**
         * Retrieves/Sets The [NightMode] Strategy
         */
        val NIGHT_MODE = stringPreferenceKey(
            "${TAG}_night_mode",
            NightMode.NO,
            object : StringSaver<NightMode> {
                override fun save(value: NightMode): String = value.name

                override fun restore(value: String): NightMode = NightMode.valueOf(value)
            }
        )

        val FONT_FAMILY = stringPreferenceKey(
            TAG + "_font_family",
            FontFamily.SYSTEM_DEFAULT,
            object : StringSaver<FontFamily> {
                override fun save(value: FontFamily): String = value.name

                override fun restore(value: String): FontFamily = FontFamily.valueOf(value)
            }
        )


        val FORCE_COLORIZE = booleanPreferenceKey(TAG + "_force_colorize", false)

        val COLOR_STATUS_BAR = booleanPreferenceKey(TAG + "_color_status_bar", false)

        val HIDE_STATUS_BAR = booleanPreferenceKey(TAG + "_hide_status_bar", false)

        val LIGHT_COLORS = stringPreferenceKey(
            name = TAG + "light_colors",
            defaultValue = LightColorScheme,
            saver = object : StringSaver<Colors> {
                override fun restore(value: String): Colors =
                    Json.decodeFromString(ColorSchemeSerializer, value)

                override fun save(value: Colors): String =
                    Json.encodeToString(ColorSchemeSerializer, value)
            }
        )

        val DARK_COLORS = stringPreferenceKey(
            name = TAG + "dark_colors",
            defaultValue = DarkColorScheme,
            saver = object : StringSaver<Colors> {
                override fun restore(value: String): Colors =
                    Json.decodeFromString(ColorSchemeSerializer, value)

                override fun save(value: Colors): String =
                    Json.encodeToString(ColorSchemeSerializer, value)
            }
        )
    }
}

private val LightColorScheme = lightColors(
    primary = Color(0xFFEE2318),
    primaryVariant = Color(0xFFC20000),
    secondary = Color(0xFF028C02),
    secondaryVariant = Color(0xFF005D00),
    onPrimary = Color.SignalWhite,
    onSecondary = Color.SignalWhite,
    surface = Color.White,
    onSurface = Color.UmbraGrey,
    background = Color.SignalWhite,
    error = Color.OrientRed,
    onBackground = Color.UmbraGrey,
    onError = Color.SignalWhite
)

private val DarkColorScheme = darkColors(
    primary = Color.Amber,
    primaryVariant = Color(0xFFC43E00),
    secondary = Color.DahliaYellow,
    secondaryVariant = Color(0xFFFF9800),
    onPrimary = Color.SignalWhite,
    onSecondary = Color.SignalWhite,
    surface = Color.JetBlack,
    onSurface = Color.SignalWhite,
    background = Color.Black,
    error = Color.OrientRed,
    onBackground = Color.SignalWhite,
    onError = Color.SignalWhite,
)

private object ColorSchemeSerializer : KSerializer<Colors> {
    override fun deserialize(decoder: Decoder): Colors {
        return decoder.decodeStructure(descriptor) {
            Colors(
                primary = Color(decodeIntElement(descriptor, 0)),
                primaryVariant = Color(decodeIntElement(descriptor, 1)),
                secondary = Color(decodeIntElement(descriptor, 2)),
                secondaryVariant = Color(decodeIntElement(descriptor, 3)),
                background = Color(decodeIntElement(descriptor, 4)),
                surface = Color(decodeIntElement(descriptor, 5)),
                error = Color(decodeIntElement(descriptor, 6)),
                onPrimary = Color(decodeIntElement(descriptor, 7)),
                onSecondary = Color(decodeIntElement(descriptor, 8)),
                onBackground = Color(decodeIntElement(descriptor, 9)),
                onSurface = Color(decodeIntElement(descriptor, 10)),
                onError = Color(decodeIntElement(descriptor, 11)),
                isLight = decodeBooleanElement(descriptor, 12)
            )
        }
    }

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ColorsSerializerDescriptor") {
            val color = "color"
            for (i in 0 until 11) {
                element<Int>(color + i)
            }
            element<Boolean>("isLight")
        }

    override fun serialize(encoder: Encoder, value: Colors) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.primary.toArgb())
            encodeIntElement(descriptor, 1, value.primaryVariant.toArgb())
            encodeIntElement(descriptor, 2, value.secondary.toArgb())
            encodeIntElement(descriptor, 3, value.secondaryVariant.toArgb())
            encodeIntElement(descriptor, 4, value.background.toArgb())
            encodeIntElement(descriptor, 5, value.surface.toArgb())
            encodeIntElement(descriptor, 6, value.error.toArgb())
            encodeIntElement(descriptor, 7, value.onPrimary.toArgb())
            encodeIntElement(descriptor, 8, value.onSecondary.toArgb())
            encodeIntElement(descriptor, 9, value.onBackground.toArgb())
            encodeIntElement(descriptor, 10, value.onSurface.toArgb())
            encodeIntElement(descriptor, 11, value.onError.toArgb())
            encodeBooleanElement(descriptor, 12, value.isLight)
        }
    }
}