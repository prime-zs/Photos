package com.prime.photos.common

import androidx.annotation.PluralsRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.primex.extra.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.*
import java.io.*

/**
 * Loads Image from an [uri]. if error displays [R.drawable.default_art]
 */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun Image(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    durationMillis: Int = Anim.durationMedium,
    uri: Any?,
    contentDescription: String?,
) {
    val painter = rememberImagePainter(uri, builder = {
        crossfade(true)
        crossfade(durationMillis)
    })
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}

interface INavActions {
    val controller: NavHostController

    fun navigateUp(): Boolean

    val route
        @Composable
        get() =
            controller.currentBackStackEntryAsState().value?.destination?.route

    /**
     * Checks if [dest] is current
     */
    @Composable
    fun current(dest: String) = this.route == dest
}

val LocalNavActionProvider = staticCompositionLocalOf<INavActions> {
    error("no local nav controller found")
}

@Composable
fun <T> rememberState(initial: T): MutableState<T> = remember {
    mutableStateOf(initial)
}


@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    imageVector: ImageVector,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(imageVector = imageVector, contentDescription = contentDescription)
    }
}


@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    bitmap: ImageBitmap,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(bitmap = bitmap, contentDescription = contentDescription)
    }
}

@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    painter: Painter,
    contentDescription: String?,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Icon(painter = painter, contentDescription = contentDescription)
    }
}


/**
 * Represents the common [GridTile2].
 *
 *  The content is placed horizontally. [icon] -> [text] -> [secondaryText].
 *  @param modifier Control size, clickable, etc using [modifier].
 *  @param selected: if selected a background of color [LocalContentColor] with alpha [ContentAlpha.Indication] is added.
 *  @param enabled: changes tyle colors etc. of components.
 *  @param alignment The horizontal alignment of the layout's children.
 */
@Composable
fun GridTile2(
    modifier: Modifier = Modifier,
    text: @Composable (() -> Unit)? = null,
    secondaryText: @Composable (() -> Unit)? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 2.dp, vertical = Dp.pSmall),
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    color: Color = Color.Transparent,
    alignment: Alignment.Horizontal = Alignment.Start,
    child: @Composable () -> Unit,
) {
    val typography = MaterialTheme.typography

    val styledText = ProvideTextStyle(
        typography.caption.copy(fontWeight = FontWeight.SemiBold),
        alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled,
        content = text
    )

    val styledSecondaryText = ProvideTextStyle(
        typography.caption,
        alpha = if (enabled) ContentAlpha.medium else ContentAlpha.disabled,
        content = secondaryText
    )

    val bg by animateColorAsState(
        targetValue = if (selected)
            LocalContentColor.current.copy(ContentAlpha.Indication)
        else Color.Transparent
    )

    val styledChild = ProvideTextStyle(
        typography.caption,
        if (enabled) ContentAlpha.medium else ContentAlpha.disabled,
        child
    )!!

    Frame(
        modifier = modifier,
        color = bg.compositeOver(color),
        shape = shape,
        elevation = elevation,
        border = border,
        contentColor = LocalContentColor.current
    ) {
        Column(
            modifier = Modifier
                .padding(contentPadding),
            horizontalAlignment = alignment,
        ) {
            // stacked over one another.
            styledChild()

            //
            if (styledText != null) {
                Spacer(modifier = Modifier.padding(top = Dp.pMedium))
                styledText()
            }

            if (styledSecondaryText != null) {
                styledSecondaryText()
            }
        }
    }
}

@Suppress("filename")
fun ProvideTextStyle(
    textStyle: TextStyle,
    alpha: Float? = null,
    content: @Composable (() -> Unit)?
): @Composable (() -> Unit)? {
    if (content == null) return null
    return {
        CompositionLocalProvider(LocalContentAlpha provides (alpha ?: LocalContentAlpha.current)) {
            ProvideTextStyle(textStyle, content)
        }
    }
}


/**
 * Load a quantity string resource.
 *
 * @param id the resource identifier
 * @param quantity The number used to get the correct string for the current language's
 *           plural rules.
 * @return String The string data associated with the resource,
 * stripped of styled text information.
 */
@Composable
@ReadOnlyComposable
fun stringQuantityResource(@PluralsRes id: Int, quantity: Int): String {
    val resources = LocalContext.current.resources
    return resources.getQuantityString(id, quantity)
}


/**
 * Return the string value associated with a particular resource ID,
 * substituting the format arguments as defined in {@link java.util.Formatter}
 * and {@link java.lang.String#format}. It will be stripped of any styled text
 * information.
 * {@more}
 *
 * @param id The desired resource identifier, as generated by the aapt
 *           tool. This integer encodes the package, type, and resource
 *           entry. The value 0 is an invalid identifier.
 *
 * @param formatArgs The format arguments that will be used for substitution.
 *
 * @throws NotFoundException Throws NotFoundException if the given ID does not exist.
 *
 * @return String The string data associated with the resource,
 *         stripped of styled text information.
 */
@Composable
@ReadOnlyComposable
fun stringQuantityResource(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String {
    val resources = LocalContext.current.resources
    return resources.getQuantityString(id, quantity, *formatArgs)
}

@Composable
operator fun PaddingValues.plus(value: PaddingValues): PaddingValues {
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = this.calculateStartPadding(direction) + value.calculateStartPadding(direction),
        top = this.calculateTopPadding() + value.calculateTopPadding(),
        bottom = this.calculateBottomPadding() + value.calculateBottomPadding(),
        end = this.calculateEndPadding(direction) + value.calculateEndPadding(direction)
    )
}


/*
* The Message Channel Api
* //////////////////////////////////
*/
data class Message(val label: String = "", val message: String, val action: (() -> Unit)? = null)

typealias Messenger = Channel<Message>

fun Messenger(
    capacity: Int = Channel.RENDEZVOUS,
    onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    onUndeliveredElement: ((Message) -> Unit)? = null
): Messenger = Channel(
    capacity,
    onBufferOverflow,
    onUndeliveredElement
)

suspend fun Messenger.message(label: String = "", message: String, action: (() -> Unit)? = null) =
    send(Message(label, message, action))

val LocalMessenger = staticCompositionLocalOf<Messenger> {
    error("no local messenger provided!!")
}
