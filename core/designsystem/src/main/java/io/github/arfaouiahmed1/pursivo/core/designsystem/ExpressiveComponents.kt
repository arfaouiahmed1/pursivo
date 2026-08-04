package io.github.arfaouiahmed1.pursivo.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DockDestination(val label: String, val icon: ImageVector)

@Composable
fun PursivoDock(
    destinations: List<DockDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEachIndexed { index, destination ->
                    if (index == destinations.size / 2) {
                        FloatingActionButton(
                            onClick = onAdd,
                            modifier = Modifier.size(58.dp),
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 14.dp, bottomEnd = 24.dp, bottomStart = 14.dp),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add application")
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    DockItem(destination, selectedIndex == index) { onSelect(index) }
                }
            }
        }
    }
}

@Composable
private fun DockItem(destination: DockDestination, selected: Boolean, onClick: () -> Unit) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "dockColor",
    )
    val emphasis by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(dampingRatio = 0.68f, stiffness = 420f),
        label = "dockEmphasis",
    )
    val horizontalPadding by animateDpAsState(
        if (selected) 15.dp else 11.dp,
        spring(dampingRatio = 0.7f, stiffness = 480f),
        label = "dockPadding",
    )
    val selectedShape = RoundedCornerShape(
        topStart = (17 + emphasis * 7).dp,
        topEnd = (17 - emphasis * 4).dp,
        bottomEnd = (17 + emphasis * 7).dp,
        bottomStart = (17 - emphasis * 4).dp,
    )
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = 0.96f + emphasis * 0.04f
                scaleY = 0.96f + emphasis * 0.04f
            }
            .background(color, selectedShape)
            .padding(horizontal = horizontalPadding, vertical = 7.dp)
            .semantics { contentDescription = destination.label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            destination.icon,
            contentDescription = null,
            modifier = Modifier.size((22 + emphasis * 2).dp),
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            destination.label,
            fontSize = (10 + emphasis).sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun PursuitPath(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val target = if (total == 0) 0f else completed.toFloat() / total.coerceAtLeast(1)
    val progress by animateFloatAsState(target, spring(dampingRatio = 0.72f, stiffness = 260f), label = "pursuitProgress")
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)

    Canvas(modifier = modifier.semantics { contentDescription = "$completed of $total next actions complete" }) {
        val stroke = size.minDimension * 0.085f
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.82f)
            cubicTo(size.width * 0.18f, size.height * 0.28f, size.width * 0.62f, size.height * 0.18f, size.width * 0.62f, size.height * 0.48f)
            cubicTo(size.width * 0.62f, size.height * 0.76f, size.width * 0.36f, size.height * 0.72f, size.width * 0.38f, size.height * 0.38f)
            cubicTo(size.width * 0.42f, size.height * 0.10f, size.width * 0.82f, size.height * 0.08f, size.width * 0.86f, size.height * 0.22f)
        }
        drawPath(path, track, style = Stroke(stroke, cap = StrokeCap.Round))
        val boundedProgress = progress.coerceIn(0f, 1f)
        val measure = PathMeasure().apply { setPath(path, false) }
        val activePath = Path()
        if (boundedProgress > 0f) {
            measure.getSegment(0f, measure.length * boundedProgress, activePath, true)
            drawPath(activePath, primary, style = Stroke(stroke, cap = StrokeCap.Round))
            drawCircle(
                color = PursivoSaffron,
                radius = stroke * 0.68f,
                center = measure.getPosition(measure.length * boundedProgress),
            )
        }
        drawArc(
            color = PursivoSaffron,
            startAngle = -42f,
            sweepAngle = 300f * boundedProgress,
            useCenter = false,
            topLeft = Offset(size.width * 0.66f, size.height * 0.02f),
            size = Size(size.width * 0.24f, size.width * 0.24f),
            style = Stroke(stroke * 0.55f, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun MetricPill(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(24.dp), color = color.copy(alpha = 0.14f)) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
