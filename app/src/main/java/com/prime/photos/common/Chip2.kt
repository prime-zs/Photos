package com.prime.photos.common

import androidx.annotation.FloatRange
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.primex.extra.*
import kotlinx.coroutines.flow.collect

/**
 * Represents the elevation for a button in different states.
 *
 * See [ChipDefaults.elevation] for the default elevation used in a [Chip].
 */
@Stable
interface ChipElevation {
    /**
     * Represents the elevation used in a chip, depending on [enabled] and
     * [interactionSource].
     *
     * @param enabled whether the button is enabled
     * @param interactionSource the [InteractionSource] for this button
     */
    @Composable
    fun elevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp>
}

/**
 * Default [ChipElevation] implementation.
 */
@Stable
private class ChipElevationImpl(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val disabledElevation: Dp,
    private val hoveredElevation: Dp,
    private val focusedElevation: Dp,
) : ChipElevation {

    private val DefaultIncomingSpec = TweenSpec<Dp>(
        durationMillis = 120,
        easing = FastOutSlowInEasing
    )

    private val DefaultOutgoingSpec = TweenSpec<Dp>(
        durationMillis = 150,
        easing = CubicBezierEasing(0.40f, 0.00f, 0.60f, 1.00f)
    )

    private val HoveredOutgoingSpec = TweenSpec<Dp>(
        durationMillis = 120,
        easing = CubicBezierEasing(0.40f, 0.00f, 0.60f, 1.00f)
    )


    /**
     * Returns the [AnimationSpec]s used when animating elevation to [interaction], either from a
     * previous [Interaction], or from the default state. If [interaction] is unknown, then
     * returns `null`.
     *
     * @param interaction the [Interaction] that is being animated to
     */
    fun incomingAnimationSpecForInteraction(interaction: Interaction): AnimationSpec<Dp>? {
        return when (interaction) {
            is PressInteraction.Press -> DefaultIncomingSpec
            is DragInteraction.Start -> DefaultIncomingSpec
            is HoverInteraction.Enter -> DefaultIncomingSpec
            is FocusInteraction.Focus -> DefaultIncomingSpec
            else -> null
        }
    }


    /**
     * Returns the [AnimationSpec]s used when animating elevation away from [interaction], to the
     * default state. If [interaction] is unknown, then returns `null`.
     *
     * @param interaction the [Interaction] that is being animated away from
     */
    fun outgoingAnimationSpecForInteraction(interaction: Interaction): AnimationSpec<Dp>? {
        return when (interaction) {
            is PressInteraction.Press -> DefaultOutgoingSpec
            is DragInteraction.Start -> DefaultOutgoingSpec
            is HoverInteraction.Enter -> HoveredOutgoingSpec
            is FocusInteraction.Focus -> DefaultOutgoingSpec
            else -> null
        }
    }


    /**
     * Animates the [Dp] value of [this] between [from] and [to] [Interaction]s, to [target]. The
     * [AnimationSpec] used depends on the values for [from] and [to], see
     * [ElevationDefaults.incomingAnimationSpecForInteraction] and
     * [ElevationDefaults.outgoingAnimationSpecForInteraction] for more details.
     *
     * @param target the [Dp] target elevation for this component, corresponding to the elevation
     * desired for the [to] state.
     * @param from the previous [Interaction] that was used to calculate elevation. `null` if there
     * was no previous [Interaction], such as when the component is in its default state.
     * @param to the [Interaction] that this component is moving to, such as [PressInteraction.Press]
     * when this component is being pressed. `null` if this component is moving back to its default
     * state.
     */
    suspend fun Animatable<Dp, *>.animateElevation(
        target: Dp,
        from: Interaction? = null,
        to: Interaction? = null
    ) {
        val spec = when {
            // Moving to a new state
            to != null -> incomingAnimationSpecForInteraction(to)
            // Moving to default, from a previous state
            from != null -> outgoingAnimationSpecForInteraction(from)
            // Loading the initial state, or moving back to the baseline state from a disabled /
            // unknown state, so just snap to the final value.
            else -> null
        }
        if (spec != null) animateTo(target, spec) else snapTo(target)
    }


    @Composable
    override fun elevation(enabled: Boolean, interactionSource: InteractionSource): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target = if (!enabled) {
            disabledElevation
        } else {
            when (interaction) {
                is PressInteraction.Press -> pressedElevation
                is HoverInteraction.Enter -> hoveredElevation
                is FocusInteraction.Focus -> focusedElevation
                else -> defaultElevation
            }
        }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        if (!enabled) {
            // No transition when moving to a disabled state
            LaunchedEffect(target) {
                animatable.snapTo(target)
            }
        } else {
            LaunchedEffect(target) {
                val lastInteraction = when (animatable.targetValue) {
                    pressedElevation -> PressInteraction.Press(Offset.Zero)
                    hoveredElevation -> HoverInteraction.Enter()
                    focusedElevation -> FocusInteraction.Focus()
                    else -> null
                }
                animatable.animateElevation(
                    from = lastInteraction,
                    to = interaction,
                    target = target
                )
            }
        }

        return animatable.asState()
    }
}


/**
 * Represents the background and content colors used in [ToggleChip]s and [SplitToggleChip]s
 * in different states.
 */
@Stable
interface ChipColors {
    /**
     * Represents the background treatment for this chip, depending on the [enabled] and [checked]
     * properties. Backgrounds are typically a linear gradient when the chip is checked/selected
     * and solid when it is not.
     *
     * @param enabled Whether the chip is enabled
     * @param checked Whether the chip is currently checked/selected or unchecked/not selected
     */
    @Composable
    fun background(enabled: Boolean, checked: Boolean): State<Color>

    /**
     * Represents the content color for this chip, depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the chip is enabled
     * @param checked Whether the chip is currently checked/selected or unchecked/not selected
     */
    @Composable
    fun contentColor(enabled: Boolean, checked: Boolean): State<Color>
}

/**
 * Default [Chip] implementation.
 *
 * @param backgroundColor the color of the background of the chip.
 * @param contentColor the color of content of chip.
 * @param checkedBackgroundColor the color of background when chip is checked.
 * @param checkedContentColor The color of chip's content when state is checked.
 * @param enabledAlpha The alpha of chip when state is enabled. *Note:* Relative to [Color] alpha.
 * @param disabledAlpha The alpha of chip when state is disabled. *Note:* Relative to [Color] alpha.
 */
@Immutable
private class ChipColorsImpl(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val checkedBackgroundColor: Color,
    private val checkedContentColor: Color,
    @FloatRange(from = 0.0, to = 1.0)
    private val enabledAlpha: Float,
    @FloatRange(from = 0.0, to = 1.0)
    private val disabledAlpha: Float,
) : ChipColors {

    //private val spec: AnimationSpec<Color> = tween(Anim.durationMedium)

    @Composable
    override fun background(enabled: Boolean, checked: Boolean): State<Color> {
        return (if (checked) checkedBackgroundColor else backgroundColor).let { color ->
            val alpha = (if (enabled) enabledAlpha else disabledAlpha) * color.alpha
            // animateColorAsState(targetValue = color.copy(alpha), spec)
            rememberUpdatedState(newValue = color.copy(alpha))
        }
    }

    @Composable
    override fun contentColor(enabled: Boolean, checked: Boolean): State<Color> {
        return (if (checked) checkedContentColor else contentColor).let { color ->
            val alpha = (if (enabled) enabledAlpha else disabledAlpha) * color.alpha
            //animateColorAsState(targetValue = color.copy(alpha), spec)
            rememberUpdatedState(newValue = color.copy(alpha))
        }
    }
}

/**
 * Contains the default values used by [Chip]
 */
object ChipDefaults {
    private val ChipHorizontalPadding = 12.dp
    private val ChipVerticalPadding = 6.dp


    /**
     * The default content padding used by [Chip]
     */
    val ContentPadding = PaddingValues(
        horizontal = ChipHorizontalPadding,
        vertical = ChipVerticalPadding,
    )

    const val EnabledRelativeAlpha = 1.0f
    const val DisabledRelativeAlpha = 0.5f


    val ChipTextStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp
    )


    /**
     * The default min height applied for the [Chip].
     * Note that you can override it by applying Modifier.heightIn directly on [Chip].
     */
    val MinHeight = 36.dp


    /**
     * The default size of the icon when used inside a [Chip].
     */
    val IconSize = 18.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside a [Chip].
     */
    val IconSpacing = 8.dp

    /**
     * The default shape of the [Chip]
     */
    internal val shape: Shape = RoundedCornerShape(50)

    /**
     * Creates a [IChipElevation] that will animate between the provided values according to the
     * Material specification for a [Chip].
     *
     * @param defaultElevation the elevation to use when the [Chip] is enabled, and has no
     * other [Interaction]s.
     * @param pressedElevation the elevation to use when the [Chip] is enabled and
     * is pressed.
     * @param disabledElevation the elevation to use when the [Chip] is not enabled.
     * @param hoveredElevation the elevation to use when the [Chip] is enabled and is hovered.
     * @param focusedElevation the elevation to use when the [Chip] is enabled and is focused.
     */
    @Suppress("UNUSED_PARAMETER")
    @Composable
    fun elevation(
        defaultElevation: Dp = 2.dp,
        pressedElevation: Dp = 8.dp,
        disabledElevation: Dp = 0.dp,
        hoveredElevation: Dp = 4.dp,
        focusedElevation: Dp = 4.dp,
    ): ChipElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            disabledElevation,
            hoveredElevation,
            focusedElevation
        ) {
            ChipElevationImpl(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                disabledElevation = disabledElevation,
                hoveredElevation = hoveredElevation,
                focusedElevation = focusedElevation
            )
        }
    }

    /**
     * Creates a [ChipColors] that represents the default background and content colors used in a [Chip].
     *
     * @param backgroundColor the color of the background of the chip.
     * @param contentColor the color of content of chip.
     * @param enabledAlpha The alpha of chip when state is enabled. *Note:* Relative to [Color] alpha.
     * @param disabledAlpha The alpha of chip when state is disabled. *Note:* Relative to [Color] alpha.
     */
    @Composable
    fun chipColors(
        backgroundColor: Color = Material.colors.surface.copy(ContentAlpha.Indication),
        contentColor: Color = Material.colors.onSurface,
        enabledAlpha: Float = EnabledRelativeAlpha,
        disabledAlpha: Float = DisabledRelativeAlpha,
    ): ChipColors = ChipColorsImpl(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        checkedBackgroundColor = backgroundColor,
        checkedContentColor = contentColor,
        enabledAlpha = enabledAlpha,
        disabledAlpha = disabledAlpha
    )

    /**
     * Creates a [ChipColors] that represents the default background and content colors used in
     * a [ToggleChip].
     *
     * @param backgroundColor the color of the background of the chip.
     * @param contentColor the color of content of chip.
     * @param checkedBackgroundColor the color of background when chip is checked.
     * @param checkedContentColor The color of chip's content when state is checked.
     * @param enabledAlpha The alpha of chip when state is enabled. *Note:* Relative to [Color] alpha.
     * @param disabledAlpha The alpha of chip when state is disabled. *Note:* Relative to [Color] alpha.
     *
     */
    @Composable
    fun toggleChipColors(
        backgroundColor: Color = Material.colors.onSurface.copy(ContentAlpha.Indication),
        contentColor: Color = Material.colors.onSurface,
        checkedBackgroundColor: Color = Material.colors.primary.copy(ContentAlpha.Indication),
        checkedContentColor: Color = Material.colors.primary,
        enabledAlpha: Float = EnabledRelativeAlpha,
        disabledAlpha: Float = DisabledRelativeAlpha,
    ): ChipColors = ChipColorsImpl(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        checkedBackgroundColor = checkedBackgroundColor,
        checkedContentColor = checkedContentColor,
        enabledAlpha = enabledAlpha,
        disabledAlpha = disabledAlpha
    )
}

/**
 * Chips are compact elements that represent an input, attribute, or action.
 *
 * Chips allow users to enter information, make selections, filter content, or trigger actions.
 * While buttons are expected to appear consistently and with familiar calls to action, chips
 * should appear dynamically as a group of multiple interactive elements.
 *
 * **Anatomy**
 *
 *  1. Chip containers hold all chip elements, and their size is determined by those elements.
 *     A optional stroke can be also be set.
 *
 *  2. **Leading Icon {Optional}**
 *  Thumbnails identify entities (like individuals) by displaying an avatar, logo, or icon.
 *
 *  3. **Label**
 *  Chip text can be an entity name, description, tag, action, or conversational.
 *
 *  4. **Trailing Icon {Optional}**
 *
 *  More At [https://material.io/components/chips]
 *
 *  @param label: The label of the chip.
 *  @param leading: The leading icon of the chip.
 *  @param trailing: The trailing icon of the chip.
 *  @param borderWidth: The width of the border if any.
 */
@Composable
private fun Chip(
    modifier: Modifier = Modifier,
    clickAndSemanticsModifier: Modifier?,
    label: @Composable () -> Unit,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    elevation: ChipElevation?,
    colors: ChipColors,
    enabled: Boolean,
    selected: Boolean,
    borderWidth: Dp?,
    interactionSource: MutableInteractionSource,
    shape: Shape,
    contentPadding: PaddingValues,
) {
    val backgroundColor = colors.background(enabled = enabled, selected).value
    //  .compositeOver(background = Material.colors.surface)
    val elevation1 = elevation?.elevation(enabled, interactionSource)?.value ?: 0.dp
    val contentColor = colors.contentColor(enabled = enabled, selected).value

    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalTextStyle provides ChipDefaults.ChipTextStyle,
    ) {
        Box(
            modifier
                .shadow(elevation1, shape, clip = false)
                .then(
                    if (borderWidth != null) Modifier.border(
                        BorderStroke(borderWidth, contentColor),
                        shape
                    ) else Modifier
                )
                .background(color = backgroundColor, shape = shape)
                .clip(shape)
                .then(clickAndSemanticsModifier ?: Modifier),
            propagateMinConstraints = false
        ) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    leading()
                    Spacer(modifier = Modifier.size(ChipDefaults.IconSpacing))
                }

                label()

                if (trailing != null) {
                    Spacer(modifier = Modifier.size(ChipDefaults.IconSpacing))
                    trailing()
                }
            }
        }
    }
}

@Composable
fun Chip(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    elevation: ChipElevation? = ChipDefaults.elevation(),
    colors: ChipColors = ChipDefaults.chipColors(),
    enabled: Boolean = true,
    selected: Boolean = false,
    borderWidth: Dp? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = ChipDefaults.shape,
    role: Role? = Role.Button,
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
) {
    Chip(
        modifier = modifier,
        label = label,
        leading = leading,
        trailing = trailing,
        elevation = elevation,
        colors = colors,
        enabled = enabled,
        selected = selected,
        borderWidth = borderWidth,
        interactionSource = interactionSource,
        shape = shape,
        contentPadding = contentPadding,
        clickAndSemanticsModifier = Modifier.clickable(
            enabled = enabled,
            onClick = onClick,
            role = role,
            indication = rememberRipple(
                color = colors.contentColor(
                    enabled = true,
                    checked = false
                ).value
            ),
            interactionSource = interactionSource,
        )
    )
}

@Composable
fun ToggleChip(
    checked: Boolean,
    onRequestChange: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    elevation: ChipElevation? = ChipDefaults.elevation(),
    colors: ChipColors = ChipDefaults.toggleChipColors(),
    enabled: Boolean = true,
    borderWidth: Dp? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = ChipDefaults.shape,
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
) {
    Chip(
        label = label,
        modifier = modifier,
        leading = leading,
        trailing = trailing,
        elevation = elevation,
        colors = colors,
        enabled = enabled,
        borderWidth = borderWidth,
        interactionSource = interactionSource,
        shape = shape,
        contentPadding = contentPadding,
        clickAndSemanticsModifier = Modifier
            .selectable(
                selected = checked,
                onClick = onRequestChange,
                interactionSource = interactionSource,
                enabled = enabled,
                role = Role.Checkbox,
                indication = rememberRipple(
                    color = colors.contentColor(
                        enabled = enabled,
                        checked = checked
                    ).value
                ),
            ),
        selected = checked
    )
}
