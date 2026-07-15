package com.crowdedlibs.cafe.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.crowdedlibs.cafe.ui.theme.LocalCafeColors
import com.crowdedlibs.cafe.ui.theme.LocalCafeType

/**
 * The café's own text primitive — a thin [BasicText] wrapper so the app draws
 * type without depending on Material at all. It mirrors the slice of the
 * Material `Text` API the app uses (style + textAlign/maxLines/overflow), and
 * resolves colour from the style, then falls back to café ink so nothing ever
 * renders in default black. Since foundation has no `LocalContentColor`, every
 * colour is explicit — which is exactly what a bespoke design system wants.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalCafeType.current.body,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
) {
    val fallback = LocalCafeColors.current.ink
    val resolved = style.merge(
        TextStyle(
            color = color.takeOrElse { style.color.takeOrElse { fallback } },
            textAlign = textAlign ?: TextAlign.Unspecified,
        ),
    )
    BasicText(
        text = text,
        modifier = modifier,
        style = resolved,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}
