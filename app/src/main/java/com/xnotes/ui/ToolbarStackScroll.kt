package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xnotes.R
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlinx.coroutines.launch

/** A bounded stack with explicit controls whenever bars extend beyond its viewport. */
@Composable
internal fun ToolbarStackScroll(
    maxHeight: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val palette = LocalPalette.current
    val overflow = scroll.maxValue > 0
    Column(modifier.heightIn(max = maxHeight), horizontalAlignment = Alignment.CenterHorizontally) {
        if (overflow) {
            IconButton(
                modifier = Modifier.background(palette.panel.toComposeColor(), CircleShape),
                enabled = scroll.canScrollBackward,
                onClick = { scope.launch { scroll.animateScrollBy(-scroll.viewportSize.toFloat()) } },
            ) {
                Icon(XnotesIcons.chevronUp, stringResource(R.string.toolbar_stack_up),
                    tint = (if (scroll.canScrollBackward) palette.text else palette.disabled).toComposeColor())
            }
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        if (overflow) {
            IconButton(
                modifier = Modifier.background(palette.panel.toComposeColor(), CircleShape),
                enabled = scroll.canScrollForward,
                onClick = { scope.launch { scroll.animateScrollBy(scroll.viewportSize.toFloat()) } },
            ) {
                Icon(XnotesIcons.chevronDown, stringResource(R.string.toolbar_stack_down),
                    tint = (if (scroll.canScrollForward) palette.text else palette.disabled).toComposeColor())
            }
        }
    }
}
