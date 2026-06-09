package com.serenare.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serenare.domain.model.SoundscapeType

@Composable
fun SafeScaffold(
    title: String,
    step: String,
    onHelp: () -> Unit,
    onBack: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = SerenareColors.Background,
        topBar = { AppTopBar(title, step, onHelp, onBack) },
        content = content
    )
}

@Composable
fun AppTopBar(title: String, step: String, onHelp: () -> Unit, onBack: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SerenareColors.Background)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                OutlinedButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("Voltar")
                }
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = SerenareColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                Text(step, color = SerenareColors.TextSecondary, fontSize = 13.sp)
            }
            CrisisHelpButton(onClick = onHelp)
        }
    }
}

@Composable
fun CrisisHelpButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SerenareColors.Crisis),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text("Ajuda")
    }
}

@Composable
fun SensoryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SerenareColors.Relax,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = SerenareColors.TextPrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = label }
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

@Composable
fun ProgressHeader(step: Int, total: Int, objective: String, estimatedTime: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(objective, color = SerenareColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text("Tempo estimado: $estimatedTime", color = SerenareColors.TextSecondary, fontSize = 13.sp)
        LinearProgressIndicator(
            progress = { (step + 1).toFloat() / total.coerceAtLeast(1) },
            color = SerenareColors.Relax,
            trackColor = SerenareColors.Surface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun BreathingCircle(label: String, accent: Color, seconds: Int) {
    val transition = rememberInfiniteTransition(label = "breathing-circle")
    val scale by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(seconds * 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing-scale"
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size((180 * scale).dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.22f))
                .border(2.dp, accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = SerenareColors.TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun BoxBreathingPath(progress: Float, accent: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = "Caminho de respiracao quadrada" }
    ) {
        val side = size.minDimension * 0.72f
        val start = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val end = Offset(start.x + side, start.y + side)
        drawRect(accent.copy(alpha = 0.25f), start, androidx.compose.ui.geometry.Size(side, side), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        val p = progress.coerceIn(0f, 1f) * 4f
        val point = when {
            p < 1f -> Offset(start.x + side * p, start.y)
            p < 2f -> Offset(end.x, start.y + side * (p - 1f))
            p < 3f -> Offset(end.x - side * (p - 2f), end.y)
            else -> Offset(start.x, end.y - side * (p - 3f))
        }
        drawCircle(accent, radius = 10.dp.toPx(), center = point)
    }
}

@Composable
fun GroundingOrb(label: String, onClick: () -> Unit, accent: Color) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.25f))
            .border(2.dp, accent, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = SerenareColors.TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SoundscapeSelector(selected: SoundscapeType?, onSelected: (SoundscapeType) -> Unit) {
    val sounds = listOf(
        SoundscapeType.RAIN,
        SoundscapeType.OCEAN,
        SoundscapeType.FOREST,
        SoundscapeType.WHITE_NOISE,
        SoundscapeType.BROWN_NOISE,
        SoundscapeType.GRAY_NOISE,
        SoundscapeType.FOCUS_DRONE
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sounds.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { sound ->
                    val active = selected == sound
                    OutlinedButton(
                        onClick = { onSelected(sound) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (active) SerenareColors.Focus else Color.Transparent,
                            contentColor = SerenareColors.TextPrimary
                        )
                    ) {
                        Text(sound.label)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CompletionCard(initial: Int?, final: Int?, message: String) {
    val delta = if (initial != null && final != null) initial - final else null
    Card(
        colors = CardDefaults.cardColors(containerColor = SerenareColors.Surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Sessao concluida", color = SerenareColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            if (delta != null) Text("Reducao de intensidade: $delta ponto(s)", color = SerenareColors.TextSecondary)
            Text(message, color = SerenareColors.TextPrimary)
        }
    }
}

@Composable
fun ShimmerText(label: String = "Gerando orientacao segura...") {
    Text(label, color = SerenareColors.TextSecondary)
}

@Composable
fun ExpandableKnowledgeCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SerenareColors.Surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = SerenareColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(body.replace("**", "").replace("##", "").replace("_", ""), color = SerenareColors.TextSecondary)
        }
    }
}

@Composable
fun HapticPressableSurface(label: String, onClick: () -> Unit, accent: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.18f))
            .border(1.dp, accent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = SerenareColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ScreenBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
fun VolumeSlider(value: Float, onChange: (Float) -> Unit) {
    Column {
        Text("Volume", color = SerenareColors.TextSecondary)
        Slider(value = value, onValueChange = onChange)
    }
}
