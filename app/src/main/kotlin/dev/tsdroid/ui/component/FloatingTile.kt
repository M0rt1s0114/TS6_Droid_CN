package dev.tsdroid.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * The shared floating-tile surface used by home cards, the server header and
 * the channel panel. On AMOLED it stays near-black and relies on a 1dp
 * luminous border instead of shadows for depth.
 */
@Composable
fun FloatingTile(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    contentPadding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderAlpha = if (MaterialTheme.colorScheme.background.luminance() < 0.05f) 0.14f else 0.10f
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = borderAlpha),
        ),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding.dp),
            content = content,
        )
    }
}
