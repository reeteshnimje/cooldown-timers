package com.cooldown.timers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Refined Tactile Palette
private val CanvasColor = Color(0xFFF6F5F0)
private val CardColor = Color(0xFFFFFFFF)
private val SurfaceSubtle = Color(0xFFEFEDE6)
private val BorderSubtle = Color(0xFFE4E1D8)
private val Accent = Color(0xFF4F46E5)
private val AccentDark = Color(0xFF3730A3)
private val AccentSoft = Color(0xFFEEF0FF)
private val ReadyGreen = Color(0xFF0D9488)
private val ReadySoft = Color(0xFFE6F7F5)
private val AmberWarn = Color(0xFFD97706)
private val AmberSoft = Color(0xFFFEF3C7)
private val TextMain = Color(0xFF151823)
private val TextSecondary = Color(0xFF565C6E)
private val Muted = Color(0xFF858B9C)
private val Danger = Color(0xFFDC2626)
private val DangerSoft = Color(0xFFFEE2E2)

private data class AccentPalette(
    val primary: Color,
    val soft: Color,
    val dark: Color,
)

private val TimerPalettes = listOf(
    AccentPalette(Color(0xFF4F46E5), Color(0xFFEEF0FF), Color(0xFF3730A3)), // Indigo
    AccentPalette(Color(0xFF0284C7), Color(0xFFE0F2FE), Color(0xFF0369A1)), // Sky
    AccentPalette(Color(0xFF7C3AED), Color(0xFFF3E8FF), Color(0xFF5B21B6)), // Violet
    AccentPalette(Color(0xFFEA580C), Color(0xFFFFEDD5), Color(0xFFC2410C)), // Terracotta
    AccentPalette(Color(0xFFDB2777), Color(0xFFFCE7F3), Color(0xFF9D174D)), // Rose
    AccentPalette(Color(0xFF2563EB), Color(0xFFDBEAFE), Color(0xFF1D4ED8)), // Cobalt
)

private fun paletteForLabel(label: String): AccentPalette {
    val hash = label.trim().lowercase(Locale.ROOT).hashCode()
    val index = (hash and 0x7FFFFFFF) % TimerPalettes.size
    return TimerPalettes[index]
}

private data class CooldownTimer(
    val id: String,
    val label: String,
    val durationMillis: Long,
    val endsAtMillis: Long,
) {
    fun isComplete(now: Long) = now >= endsAtMillis

    fun progress(now: Long): Float {
        if (durationMillis <= 0L) return 1f
        val remaining = (endsAtMillis - now).coerceAtLeast(0L)
        val elapsed = (durationMillis - remaining).coerceIn(0L, durationMillis)
        return (elapsed.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        setContent { CooldownApp() }
    }
}

@Composable
private fun CooldownApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cooldowns", android.content.Context.MODE_PRIVATE) }
    val timers = remember {
        mutableStateListOf<CooldownTimer>().apply { addAll(loadTimers(context)) }
    }
    var isGridView by remember {
        mutableStateOf(prefs.getBoolean("grid_view", false))
    }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var deleteTarget by remember { mutableStateOf<CooldownTimer?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var isForeground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    fun persist() = saveTimers(context, timers)
    fun toggleGridView() {
        isGridView = !isGridView
        prefs.edit().putBoolean("grid_view", isGridView).apply()
    }

    LaunchedEffect(isForeground) {
        if (isForeground) {
            while (true) {
                now = System.currentTimeMillis()
                delay(250)
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    now = System.currentTimeMillis()
                    isForeground = true
                }
                Lifecycle.Event.ON_PAUSE -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            background = CanvasColor,
            surface = CardColor,
            onSurface = TextMain,
            onBackground = TextMain,
            error = Danger,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = CanvasColor) {
            val running = timers.filterNot { it.isComplete(now) }.sortedBy { it.endsAtMillis }
            val completed = timers.filter { it.isComplete(now) }.sortedByDescending { it.endsAtMillis }

            fun restartTimer(timer: CooldownTimer) {
                val stamp = System.currentTimeMillis()
                val index = timers.indexOfFirst { it.id == timer.id }
                if (index >= 0) {
                    timers[index] = timer.copy(endsAtMillis = stamp + timer.durationMillis)
                }
                now = stamp
                persist()
            }

            Scaffold(
                containerColor = CanvasColor,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    HomeHeader(
                        runningCount = running.size,
                        readyCount = completed.size,
                        nextUpTimer = running.firstOrNull(),
                        now = now,
                        isGridView = isGridView,
                        hasTimers = timers.isNotEmpty(),
                        onToggleGridView = ::toggleGridView,
                        onNewTimer = { editor = EditorState.create() },
                    )
                },
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (timers.isEmpty()) {
                        item {
                            EmptyTimersState(onCreate = { editor = EditorState.create() })
                        }
                    }
                    if (running.isNotEmpty()) {
                        item {
                            SectionHeading(
                                title = "RUNNING",
                                count = running.size,
                                subtitle = if (running.size > 1) "Sorted by soonest ready" else null,
                                badgeColor = AccentSoft,
                                badgeTextColor = Accent,
                            )
                        }
                        if (isGridView) {
                            val rows = running.chunked(2)
                            items(rows, key = { row -> "grid_run_${row.first().id}" }) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    row.forEach { timer ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            TimerGridCard(
                                                timer = timer,
                                                now = now,
                                                onEdit = { editor = EditorState.edit(timer, active = true, now = now) },
                                                onDelete = { deleteTarget = timer },
                                                onRestart = { restartTimer(timer) },
                                            )
                                        }
                                    }
                                    if (row.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(running, key = { it.id }) { timer ->
                                TimerCard(
                                    timer = timer,
                                    now = now,
                                    onEdit = { editor = EditorState.edit(timer, active = true, now = now) },
                                    onDelete = { deleteTarget = timer },
                                    onRestart = { restartTimer(timer) },
                                )
                            }
                        }
                    }
                    if (completed.isNotEmpty()) {
                        item {
                            SectionHeading(
                                title = "READY",
                                count = completed.size,
                                subtitle = "Tap restart for a new cycle",
                                badgeColor = ReadySoft,
                                badgeTextColor = ReadyGreen,
                                modifier = Modifier.padding(top = if (running.isNotEmpty()) 10.dp else 0.dp),
                            )
                        }
                        if (isGridView) {
                            val rows = completed.chunked(2)
                            items(rows, key = { row -> "grid_done_${row.first().id}" }) { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    row.forEach { timer ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            TimerGridCard(
                                                timer = timer,
                                                now = now,
                                                onEdit = { editor = EditorState.edit(timer, active = false, now = now) },
                                                onDelete = { deleteTarget = timer },
                                                onRestart = { restartTimer(timer) },
                                            )
                                        }
                                    }
                                    if (row.size == 1) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            items(completed, key = { it.id }) { timer ->
                                TimerCard(
                                    timer = timer,
                                    now = now,
                                    onEdit = { editor = EditorState.edit(timer, active = false, now = now) },
                                    onDelete = { deleteTarget = timer },
                                    onRestart = { restartTimer(timer) },
                                )
                            }
                        }
                    }
                }
            }
        }

        editor?.let { state ->
            TimerEditorScreen(
                state = state,
                onClose = { editor = null },
                onSave = { label, duration ->
                    val stamp = System.currentTimeMillis()
                    if (state.original == null) {
                        timers.add(CooldownTimer(UUID.randomUUID().toString(), label, duration, stamp + duration))
                    } else {
                        val old = state.original
                        val edited = old.copy(
                            label = label,
                            durationMillis = duration,
                            endsAtMillis = if (state.isActive) stamp + duration else old.endsAtMillis,
                        )
                        val index = timers.indexOfFirst { it.id == old.id }
                        if (index >= 0) timers[index] = edited
                    }
                    now = stamp
                    persist()
                    editor = null
                },
            )
        }

        deleteTarget?.let { timer ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                shape = RoundedCornerShape(24.dp),
                containerColor = CardColor,
                title = {
                    Text(
                        "Delete cooldown?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                    )
                },
                text = {
                    Text(
                        "Remove “${timer.label}” (${formatDurationCompact(timer.durationMillis)}) from your list?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            timers.removeAll { it.id == timer.id }
                            persist()
                            deleteTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeHeader(
    runningCount: Int,
    readyCount: Int,
    nextUpTimer: CooldownTimer?,
    now: Long,
    isGridView: Boolean,
    hasTimers: Boolean,
    onToggleGridView: () -> Unit,
    onNewTimer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 18.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cooldowns",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 29.sp,
                        letterSpacing = (-0.9).sp,
                    ),
                    color = TextMain,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(1.dp))
                val subtitle = when {
                    runningCount == 0 && readyCount == 0 -> "Track habits, breaks & resets"
                    runningCount > 0 && readyCount > 0 -> "$runningCount running • $readyCount ready to use"
                    runningCount > 0 -> "$runningCount active cooldown${if (runningCount == 1) "" else "s"}"
                    else -> "All $readyCount cooldown${if (readyCount == 1) "" else "s"} ready"
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (hasTimers) {
                Surface(
                    onClick = onToggleGridView,
                    shape = RoundedCornerShape(14.dp),
                    color = CardColor,
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LayoutModeGlyph(isGrid = isGridView, tint = TextMain)
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            Button(
                onClick = onNewTimer,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp,
                ),
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text("New timer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Compact Next-Up Live Status Strip when there is at least one running timer
        if (nextUpTimer != null) {
            Spacer(Modifier.height(10.dp))
            val nextRemaining = (nextUpTimer.endsAtMillis - now).coerceAtLeast(0L)
            val nextProgress = nextUpTimer.progress(now)
            val nextPalette = paletteForLabel(nextUpTimer.label)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = nextPalette.soft,
                    ) {
                        Text(
                            text = "NEXT UP",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = nextPalette.primary,
                            letterSpacing = 0.7.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = nextUpTimer.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "in ${formatRemainingShort(nextRemaining)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = nextPalette.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(nextPalette.soft),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(nextProgress.coerceIn(0.06f, 1f))
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(nextPalette.primary),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutModeGlyph(isGrid: Boolean, tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 1.7.dp.toPx()
        if (isGrid) {
            // Draw 3 horizontal list lines to switch back to list view
            val lineH = 2.2.dp.toPx()
            val gap = (size.height - lineH * 3) / 2f
            for (i in 0..2) {
                val y = i * (lineH + gap)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(0f, y),
                    size = Size(size.width, lineH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(lineH / 2f),
                )
            }
        } else {
            // Draw 2x2 bento grid squares to switch to grid view
            val gap = 3.dp.toPx()
            val cell = (size.width - gap) / 2f
            val r = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            for (row in 0..1) {
                for (col in 0..1) {
                    drawRoundRect(
                        color = tint,
                        topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                        size = Size(cell, cell),
                        cornerRadius = r,
                        style = Stroke(width = stroke),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTimersState(onCreate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 36.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(AccentSoft, CircleShape),
                contentAlignment = Alignment.CenterVertically.let { Alignment.Center },
            ) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    drawArc(
                        color = Accent.copy(alpha = 0.25f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = Accent,
                        startAngle = -90f,
                        sweepAngle = 230f,
                        useCenter = false,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No cooldowns yet",
                style = MaterialTheme.typography.titleLarge,
                color = TextMain,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Create a timer for breaks, games, workouts, or mindful pauses.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onCreate,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create first timer", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    count: Int,
    subtitle: String? = null,
    badgeColor: Color,
    badgeTextColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.width(7.dp))
        Surface(shape = CircleShape, color = badgeColor) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeTextColor,
            )
        }
        if (subtitle != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Muted,
            )
        }
    }
}

@Composable
private fun TimerCard(
    timer: CooldownTimer,
    now: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestart: () -> Unit,
) {
    val complete = timer.isComplete(now)
    val remainingMillis = (timer.endsAtMillis - now).coerceAtLeast(0L)
    val rawProgress = if (complete) 1f else timer.progress(now)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "progress",
    )

    val palette = remember(timer.label) { paletteForLabel(timer.label) }
    val isNearlyDone = !complete && remainingMillis in 1..60_000L
    val statusColor by animateColorAsState(
        targetValue = when {
            complete -> ReadyGreen
            isNearlyDone -> AmberWarn
            else -> palette.primary
        },
        label = "statusColor",
    )
    val statusSoftColor by animateColorAsState(
        targetValue = when {
            complete -> ReadySoft
            isNearlyDone -> AmberSoft
            else -> palette.soft
        },
        label = "statusSoftColor",
    )

    val shape = RoundedCornerShape(20.dp)
    val percentElapsed = (rawProgress * 100).toInt().coerceIn(0, 100)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (complete) ReadyGreen.copy(alpha = 0.3f) else BorderSubtle,
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            statusSoftColor.copy(alpha = 0.28f),
                            CardColor,
                            CardColor,
                        ),
                    ),
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
        ) {
            // Top Row: Monogram/Status Pill + Title + Duration Badge + Compact Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(statusSoftColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val initial = timer.label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "•"
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                        )
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = timer.label,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        color = TextMain,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(7.dp))
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = SurfaceSubtle,
                    ) {
                        Text(
                            text = formatDurationCompact(timer.durationMillis),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                        )
                    }
                }

                // Compact icon actions: Reset (for active), Edit, Delete
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!complete) {
                        SmallIconAction(
                            icon = Icons.Rounded.Refresh,
                            contentDescription = "Reset cooldown",
                            tint = TextSecondary,
                            backgroundColor = SurfaceSubtle,
                            onClick = onRestart,
                        )
                    }
                    SmallIconAction(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Edit timer",
                        tint = statusColor,
                        backgroundColor = statusSoftColor.copy(alpha = 0.75f),
                        onClick = onEdit,
                    )
                    SmallIconAction(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Delete timer",
                        tint = Danger.copy(alpha = 0.85f),
                        backgroundColor = DangerSoft.copy(alpha = 0.55f),
                        onClick = onDelete,
                    )
                }
            }

            Spacer(Modifier.height(7.dp))

            // Middle Row: Big Monospace Countdown + Right Side Ring / Restart CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (complete) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ready",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 26.sp,
                                    letterSpacing = (-0.7).sp,
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = ReadyGreen,
                            )
                            Spacer(Modifier.width(9.dp))
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = ReadySoft,
                            ) {
                                Text(
                                    text = "Done ${formatFinishedAgo(now - timer.endsAtMillis)}",
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ReadyGreen,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = formatRemaining(remainingMillis),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 29.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1.0).sp,
                            ),
                            color = TextMain,
                            maxLines = 1,
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = if (complete) {
                            "Cycle complete • Restart for another ${formatDurationCompact(timer.durationMillis)}"
                        } else {
                            "Ready at ${formatEtaTime(timer.endsAtMillis)} • $percentElapsed% elapsed"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(10.dp))

                if (complete) {
                    Button(
                        onClick = onRestart,
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ReadyGreen,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("Restart", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 4.dp.toPx()
                            val inset = strokeWidth / 2f
                            drawArc(
                                color = statusSoftColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = statusColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                        Text(
                            text = "$percentElapsed%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = statusColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(9.dp))

            // Sleek Bottom Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(statusSoftColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(statusColor.copy(alpha = 0.75f), statusColor),
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun TimerGridCard(
    timer: CooldownTimer,
    now: Long,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRestart: () -> Unit,
) {
    val complete = timer.isComplete(now)
    val remainingMillis = (timer.endsAtMillis - now).coerceAtLeast(0L)
    val rawProgress = if (complete) 1f else timer.progress(now)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "gridProgress",
    )

    val palette = remember(timer.label) { paletteForLabel(timer.label) }
    val isNearlyDone = !complete && remainingMillis in 1..60_000L
    val statusColor by animateColorAsState(
        targetValue = when {
            complete -> ReadyGreen
            isNearlyDone -> AmberWarn
            else -> palette.primary
        },
        label = "gridStatusColor",
    )
    val statusSoftColor by animateColorAsState(
        targetValue = when {
            complete -> ReadySoft
            isNearlyDone -> AmberSoft
            else -> palette.soft
        },
        label = "gridStatusSoftColor",
    )

    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (complete) ReadyGreen.copy(alpha = 0.3f) else BorderSubtle,
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CardColor,
                            statusSoftColor.copy(alpha = 0.25f),
                        ),
                    ),
                )
                .padding(horizontal = 13.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = timer.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        color = TextMain,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceSubtle,
                ) {
                    Text(
                        text = formatDurationCompact(timer.durationMillis),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (complete) {
                Text(
                    text = "Ready",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        letterSpacing = (-0.5).sp,
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = ReadyGreen,
                )
                Text(
                    text = "Done ${formatFinishedAgo(now - timer.endsAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                    maxLines = 1,
                )
            } else {
                Text(
                    text = formatRemaining(remainingMillis),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp,
                    ),
                    color = TextMain,
                    maxLines = 1,
                )
                Text(
                    text = "ETA ${formatEtaTime(timer.endsAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(statusSoftColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0.03f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
            }

            Spacer(Modifier.height(9.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onRestart,
                    shape = RoundedCornerShape(9.dp),
                    color = if (complete) ReadyGreen else SurfaceSubtle,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Restart",
                            tint = if (complete) Color.White else TextSecondary,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (complete) "Restart" else "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (complete) Color.White else TextSecondary,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallIconAction(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Edit timer",
                        tint = statusColor,
                        backgroundColor = statusSoftColor.copy(alpha = 0.75f),
                        onClick = onEdit,
                    )
                    SmallIconAction(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Delete timer",
                        tint = Danger.copy(alpha = 0.85f),
                        backgroundColor = DangerSoft.copy(alpha = 0.55f),
                        onClick = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

private data class EditorState(
    val original: CooldownTimer?,
    val isActive: Boolean,
    val initialHours: String,
    val initialMinutes: String,
    val initialSeconds: String,
) {
    companion object {
        fun create() = EditorState(null, true, "", "", "")

        fun edit(timer: CooldownTimer, active: Boolean, now: Long): EditorState {
            val millis = if (active) (timer.endsAtMillis - now).coerceAtLeast(1_000L) else timer.durationMillis
            val totalSeconds = (millis + 999L) / 1_000L
            val hours = totalSeconds / 3_600L
            val minutes = totalSeconds % 3_600L / 60L
            val seconds = totalSeconds % 60L
            return EditorState(
                timer,
                active,
                if (hours > 0) hours.toString() else "",
                if (minutes > 0 || hours > 0) minutes.toString() else "",
                if (seconds > 0) seconds.toString() else "",
            )
        }
    }
}

private data class DurationPreset(val label: String, val totalSeconds: Long)

private val QuickPresets = listOf(
    DurationPreset("5m", 5 * 60L),
    DurationPreset("10m", 10 * 60L),
    DurationPreset("15m", 15 * 60L),
    DurationPreset("30m", 30 * 60L),
    DurationPreset("1h", 3_600L),
    DurationPreset("2h", 2 * 3_600L),
    DurationPreset("6h", 6 * 3_600L),
    DurationPreset("24h", 24 * 3_600L),
)

private val LabelSuggestions = listOf(
    "Browsing",
    "Coffee",
    "Gaming",
    "Social Media",
    "Stretch",
    "Snack",
    "Workout",
    "Deep Work",
)

@Composable
private fun TimerEditorScreen(
    state: EditorState,
    onClose: () -> Unit,
    onSave: (String, Long) -> Unit,
) {
    var label by remember(state) { mutableStateOf(state.original?.label.orEmpty()) }
    var hours by remember(state) { mutableStateOf(state.initialHours) }
    var minutes by remember(state) { mutableStateOf(state.initialMinutes) }
    var seconds by remember(state) { mutableStateOf(state.initialSeconds) }
    val focusManager = LocalFocusManager.current

    val enteredHours = hours.toLongOrNull() ?: 0L
    val enteredMinutes = minutes.toLongOrNull() ?: 0L
    val enteredSeconds = seconds.toLongOrNull() ?: 0L

    val totalSecondsEntered = runCatching {
        Math.addExact(
            Math.addExact(Math.multiplyExact(enteredHours, 3_600L), Math.multiplyExact(enteredMinutes, 60L)),
            enteredSeconds,
        )
    }.getOrNull()

    val duration = runCatching {
        totalSecondsEntered?.let { Math.multiplyExact(it, 1_000L) }
    }.getOrNull()

    val minuteSecondValid = enteredMinutes in 0..59 && enteredSeconds in 0..59
    val timeValid = duration != null && duration > 0 && minuteSecondValid
    val hasTimeInput = hours.isNotBlank() || minutes.isNotBlank() || seconds.isNotBlank()
    val isCreating = state.original == null
    val title = if (isCreating) "New Cooldown" else "Edit Cooldown"
    val activePalette = if (label.isBlank()) TimerPalettes.first() else paletteForLabel(label)

    fun setFromTotalSeconds(sec: Long) {
        val clamped = sec.coerceAtLeast(0L)
        val h = clamped / 3_600L
        val m = (clamped % 3_600L) / 60L
        val s = clamped % 60L
        hours = if (h > 0) h.toString() else ""
        minutes = if (m > 0 || h > 0) m.toString() else ""
        seconds = if (s > 0) s.toString() else ""
    }

    fun adjustSeconds(deltaSeconds: Long) {
        val current = (totalSecondsEntered ?: 0L).coerceAtLeast(0L)
        setFromTotalSeconds((current + deltaSeconds).coerceAtLeast(0L))
    }

    fun stepUnit(unitDelta: Int, field: String) {
        when (field) {
            "H" -> {
                val next = (enteredHours + unitDelta).coerceIn(0L, 999_999L)
                hours = if (next > 0L) next.toString() else ""
            }
            "M" -> {
                val next = (enteredMinutes + unitDelta).coerceIn(0L, 59L)
                minutes = if (next > 0L || enteredHours > 0L) next.toString() else ""
            }
            "S" -> {
                val next = (enteredSeconds + unitDelta).coerceIn(0L, 59L)
                seconds = if (next > 0L) next.toString() else ""
            }
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = CanvasColor) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding(),
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        onClick = onClose,
                        shape = CircleShape,
                        color = SurfaceSubtle,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = TextMain,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        if (!isCreating) {
                            Text(
                                text = if (state.isActive) "Active timer • Resets on save" else "Completed timer • Keeps Ready state",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isActive) AmberWarn else ReadyGreen,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (hasTimeInput || label.isNotBlank()) {
                        TextButton(
                            onClick = {
                                if (isCreating) label = ""
                                hours = ""
                                minutes = ""
                                seconds = ""
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("Reset", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    } else {
                        Spacer(Modifier.width(56.dp))
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Section 1: Label Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "LABEL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Muted,
                                    letterSpacing = 1.1.sp,
                                )
                                Text(
                                    text = "Tap a suggestion or type custom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Muted,
                                )
                            }
                            Spacer(Modifier.height(9.dp))

                            var labelFocused by remember { mutableStateOf(false) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (labelFocused) activePalette.soft.copy(alpha = 0.5f) else SurfaceSubtle)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (labelFocused) activePalette.primary else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .background(activePalette.soft, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    val initial = label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activePalette.primary,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (label.isEmpty()) {
                                        Text(
                                            text = "e.g. Browsing, Coffee, Gaming...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Muted,
                                        )
                                    }
                                    BasicTextField(
                                        value = label,
                                        onValueChange = { label = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = TextMain,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                        ),
                                        cursorBrush = SolidColor(activePalette.primary),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { labelFocused = it.isFocused },
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Quick suggestion chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                LabelSuggestions.forEach { suggestion ->
                                    val selected = label.equals(suggestion, ignoreCase = true)
                                    Surface(
                                        onClick = { label = suggestion },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) activePalette.primary else SurfaceSubtle,
                                    ) {
                                        Text(
                                            text = suggestion,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) Color.White else TextSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Section 2: Duration Console Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = CardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "DURATION",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Muted,
                                    letterSpacing = 1.1.sp,
                                )
                                Text(
                                    text = "Hours can exceed 24",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Muted,
                                )
                            }

                            Spacer(Modifier.height(11.dp))

                            // Tactile Time Wells (HH : MM : SS) with built-in +/- steppers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                TimePartField(
                                    caption = "HOURS",
                                    value = hours,
                                    maxLength = 6,
                                    accentColor = activePalette.primary,
                                    accentSoft = activePalette.soft,
                                    onValueChange = { hours = it },
                                    onIncrement = { stepUnit(+1, "H") },
                                    onDecrement = { stepUnit(-1, "H") },
                                    modifier = Modifier.weight(1.12f),
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                )
                                TimeSeparator()
                                TimePartField(
                                    caption = "MINUTES",
                                    value = minutes,
                                    maxLength = 2,
                                    accentColor = activePalette.primary,
                                    accentSoft = activePalette.soft,
                                    onValueChange = { minutes = it },
                                    onIncrement = { stepUnit(+1, "M") },
                                    onDecrement = { stepUnit(-1, "M") },
                                    modifier = Modifier.weight(1f),
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                )
                                TimeSeparator()
                                TimePartField(
                                    caption = "SECONDS",
                                    value = seconds,
                                    maxLength = 2,
                                    accentColor = activePalette.primary,
                                    accentSoft = activePalette.soft,
                                    onValueChange = { seconds = it },
                                    onIncrement = { stepUnit(+5, "S") },
                                    onDecrement = { stepUnit(-5, "S") },
                                    modifier = Modifier.weight(1f),
                                    onNext = { focusManager.clearFocus() },
                                )
                            }

                            if (hasTimeInput && !minuteSecondValid) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DangerSoft,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "Minutes and seconds must be between 00 and 59.",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Danger,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            } else if (hasTimeInput && duration == null) {
                                Spacer(Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DangerSoft,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "That duration is too large.",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Danger,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // 2-Row Bento Grid of Quick Presets
                            Text(
                                text = "QUICK PRESETS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Muted,
                                letterSpacing = 1.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                QuickPresets.chunked(4).forEach { rowPresets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    ) {
                                        rowPresets.forEach { preset ->
                                            val isSelected = minuteSecondValid && totalSecondsEntered == preset.totalSeconds
                                            Surface(
                                                onClick = {
                                                    setFromTotalSeconds(preset.totalSeconds)
                                                    focusManager.clearFocus()
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(11.dp),
                                                color = if (isSelected) activePalette.primary else SurfaceSubtle,
                                            ) {
                                                Text(
                                                    text = preset.label,
                                                    modifier = Modifier.padding(vertical = 9.dp),
                                                    textAlign = TextAlign.Center,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else TextMain,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Fine-tune nudges (-5m, -1m, +1m, +5m, +15m, +1h)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                NudgeButton("-5m", Modifier.weight(1f), isNegative = true, accentColor = activePalette.primary, accentSoft = activePalette.soft) {
                                    adjustSeconds(-5 * 60L)
                                }
                                NudgeButton("-1m", Modifier.weight(1f), isNegative = true, accentColor = activePalette.primary, accentSoft = activePalette.soft) {
                                    adjustSeconds(-60L)
                                }
                                NudgeButton("+1m", Modifier.weight(1f), isNegative = false, accentColor = activePalette.primary, accentSoft = activePalette.soft) {
                                    adjustSeconds(60L)
                                }
                                NudgeButton("+5m", Modifier.weight(1f), isNegative = false, accentColor = activePalette.primary, accentSoft = activePalette.soft) {
                                    adjustSeconds(5 * 60L)
                                }
                                NudgeButton("+15m", Modifier.weight(1f), isNegative = false, accentColor = activePalette.primary, accentSoft = activePalette.soft) {
                                    adjustSeconds(15 * 60L)
                                }
                                NudgeButton("+1h", Modifier.weight(1f), isNegative = false, accentColor = activePalette.primary, accentSoft = activePalette.soft) {
                                    adjustSeconds(3_600L)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Live Cooldown Ticket Preview Banner
                    val previewEndMillis = if (timeValid) {
                        System.currentTimeMillis() + duration
                    } else null

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (previewEndMillis != null) activePalette.primary.copy(alpha = 0.25f) else BorderSubtle,
                                shape = RoundedCornerShape(18.dp),
                            ),
                        shape = RoundedCornerShape(18.dp),
                        color = if (previewEndMillis != null) activePalette.soft.copy(alpha = 0.7f) else SurfaceSubtle,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        if (previewEndMillis != null) activePalette.primary else BorderSubtle,
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (!isCreating && state.isActive) Icons.Rounded.Refresh else Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = if (previewEndMillis != null) Color.White else Muted,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (previewEndMillis != null && duration != null) {
                                    Text(
                                        text = "${if (label.isBlank()) "Untitled Timer" else label.trim()} • ${formatDurationCompact(duration)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = activePalette.dark,
                                    )
                                    Text(
                                        text = when {
                                            state.original != null && !state.isActive ->
                                                "Updates saved duration for next restart • Stays in Ready"
                                            state.original != null && state.isActive ->
                                                "Resets active countdown from now • Ready at ${formatEtaTime(previewEndMillis)}"
                                            else ->
                                                "Starts immediately • Ready at ${formatEtaTime(previewEndMillis)}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = activePalette.primary,
                                    )
                                } else {
                                    Text(
                                        text = "Ready to configure",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary,
                                    )
                                    Text(
                                        text = "Pick a preset, tap +/- steppers, or type custom hours, minutes, or seconds.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Muted,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val canSave = label.isNotBlank() && timeValid
                    val buttonText = when {
                        isCreating && timeValid ->
                            "Start ${formatDurationCompact(duration)} Cooldown"
                        isCreating ->
                            "Start Cooldown"
                        state.isActive && timeValid ->
                            "Save & Restart (${formatDurationCompact(duration)})"
                        state.isActive ->
                            "Save & Restart"
                        else ->
                            "Save Changes"
                    }
                    Button(
                        onClick = { onSave(label.trim(), duration!!) },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = activePalette.primary,
                            contentColor = Color.White,
                            disabledContainerColor = SurfaceSubtle,
                            disabledContentColor = Muted,
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (canSave) 3.dp else 0.dp,
                        ),
                    ) {
                        Text(
                            text = buttonText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NudgeButton(
    text: String,
    modifier: Modifier = Modifier,
    isNegative: Boolean = false,
    accentColor: Color = Accent,
    accentSoft: Color = AccentSoft,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (isNegative) SurfaceSubtle else accentSoft.copy(alpha = 0.7f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(vertical = 7.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Bold,
            color = if (isNegative) TextSecondary else accentColor,
        )
    }
}

@Composable
private fun TimePartField(
    caption: String,
    value: String,
    maxLength: Int,
    accentColor: Color,
    accentSoft: Color,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier,
    onNext: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val hasValue = value.isNotEmpty()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    isFocused -> accentSoft.copy(alpha = 0.6f)
                    hasValue -> SurfaceSubtle.copy(alpha = 0.95f)
                    else -> SurfaceSubtle.copy(alpha = 0.6f)
                },
            )
            .border(
                width = 1.5.dp,
                color = when {
                    isFocused -> accentColor
                    hasValue -> accentColor.copy(alpha = 0.28f)
                    else -> BorderSubtle.copy(alpha = 0.7f)
                },
                shape = RoundedCornerShape(18.dp),
            )
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Stepper (+)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onIncrement),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = if (isFocused || hasValue) accentColor else TextSecondary,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "00",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Muted.copy(alpha = 0.42f),
                        textAlign = TextAlign.Center,
                    ),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    if (input.length <= maxLength && input.all(Char::isDigit)) {
                        onValueChange(input)
                    }
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMain,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(accentColor),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { onNext() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
            )
        }

        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (isFocused) accentColor else Muted,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.9.sp,
        )

        // Bottom Stepper (-)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDecrement),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "−",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.Bold,
                color = if (isFocused || hasValue) accentColor else TextSecondary,
            )
        }
    }
}

@Composable
private fun TimeSeparator() {
    Text(
        text = ":",
        modifier = Modifier.padding(bottom = 14.dp),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Muted.copy(alpha = 0.6f),
    )
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) + 999L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun formatRemainingShort(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) + 999L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

private fun formatDurationCompact(millis: Long): String {
    val totalSeconds = (millis.coerceAtLeast(0L) + 999L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    val parts = mutableListOf<String>()
    if (hours > 0) parts.add("${hours}h")
    if (minutes > 0) parts.add("${minutes}m")
    if (seconds > 0 || parts.isEmpty()) parts.add("${seconds}s")
    return parts.joinToString(" ")
}

private fun formatFinishedAgo(elapsedSinceEndMillis: Long): String {
    val sec = (elapsedSinceEndMillis.coerceAtLeast(0L)) / 1_000L
    return when {
        sec < 60L -> "just now"
        sec < 3_600L -> "${sec / 60L}m ago"
        sec < 86_400L -> "${sec / 3_600L}h ago"
        else -> "${sec / 86_400L}d ago"
    }
}

private fun formatEtaTime(endsAtMillis: Long): String {
    val now = System.currentTimeMillis()
    val diffDays = (endsAtMillis - now) / (24 * 3_600_000L)
    val timePart = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(endsAtMillis))
    return if (diffDays >= 1L) {
        val dayPart = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(endsAtMillis))
        "$dayPart $timePart"
    } else {
        timePart
    }
}

private fun loadTimers(context: android.content.Context): List<CooldownTimer> {
    val raw = context.getSharedPreferences("cooldowns", android.content.Context.MODE_PRIVATE).getString("timers", null)
        ?: return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            CooldownTimer(
                id = item.getString("id"),
                label = item.getString("label"),
                durationMillis = item.getLong("durationMillis"),
                endsAtMillis = item.getLong("endsAtMillis"),
            )
        }
    }.getOrDefault(emptyList())
}

private fun saveTimers(context: android.content.Context, timers: List<CooldownTimer>) {
    val array = JSONArray()
    timers.forEach { timer ->
        array.put(
            JSONObject()
                .put("id", timer.id)
                .put("label", timer.label)
                .put("durationMillis", timer.durationMillis)
                .put("endsAtMillis", timer.endsAtMillis),
        )
    }
    context.getSharedPreferences("cooldowns", android.content.Context.MODE_PRIVATE)
        .edit().putString("timers", array.toString()).apply()
}
