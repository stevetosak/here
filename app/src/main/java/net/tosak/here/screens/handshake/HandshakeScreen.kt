package net.tosak.here.screens.handshake

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.tosak.here.screens.handshake.viewmodel.HandshakeState
import net.tosak.here.screens.handshake.viewmodel.HandshakeViewModel
import net.tosak.here.screens.handshake.viewmodel.MementoData
import net.tosak.here.shared.components.BackButton
import net.tosak.here.shared.components.Mono
import net.tosak.here.shared.components.PxButton
import net.tosak.here.shared.components.Rule
import net.tosak.here.ui.theme.*

/**
 * The "Connect with someone" screen.
 *
 * Flow:
 * 1. User holds the centre button.
 * 2. BLE advertise + scan start; haptic sonar plays; rings pulse outward.
 * 3. A nearby device is detected → LockOn → haptic quickens; rings shift to amber.
 * 4. Server confirms mutual detection → ConfirmationWaveOverlay sweeps the screen.
 * 5. Wave completes → [onConfirmed] is called → navigate to Memento.
 *
 * Permissions: BLUETOOTH_SCAN + BLUETOOTH_ADVERTISE (API 31+) are requested
 * the moment the screen opens. On API < 31, BLUETOOTH/BLUETOOTH_ADMIN are
 * normal permissions (auto-granted) and ACCESS_FINE_LOCATION is assumed already
 * granted by the time the user reaches this screen.
 */
@Composable
fun HandshakeScreen(
    onConfirmed: (MementoData) -> Unit,
    onBack: () -> Unit,
    viewModel: HandshakeViewModel = hiltViewModel(),
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── Permission handling ───────────────────────────────────────────────────

    var blePermissionsGranted by remember { mutableStateOf(checkBlePermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        blePermissionsGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!blePermissionsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        }
    }

    // Reset ViewModel every time this screen leaves composition so that state
    // is always Idle when a new session opens — prevents the confirmation wave
    // from auto-firing on re-entry after a completed handshake.
    DisposableEffect(Unit) {
        onDispose { viewModel.reset() }
    }

    // ── Group mode ────────────────────────────────────────────────────────────

    var groupMode by remember { mutableStateOf(false) }

    LaunchedEffect(groupMode) { viewModel.setGroupMode(groupMode) }

    // ── Haptic side-effects ───────────────────────────────────────────────────

    DisposableEffect(state) {
        when (state) {
            is HandshakeState.Scanning ->
                startVibrationPattern(context, HAPTIC_SCANNING, repeat = 0)
            is HandshakeState.LockOn ->
                startVibrationPattern(context, HAPTIC_LOCK_ON, repeat = 0)
            is HandshakeState.Confirmed ->
                startVibrationPattern(context, HAPTIC_CONFIRMED, repeat = -1)
            else ->
                cancelVibration(context)
        }
        onDispose { cancelVibration(context) }
    }

    // ── Confirmation wave + navigation ────────────────────────────────────────

    var showWave by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state is HandshakeState.Confirmed) showWave = true
    }

    // Alpha-driven visibility for top content — keeps it in the layout so the
    // button never shifts position when the content disappears.
    val topAlpha by animateFloatAsState(
        targetValue   = if (state == HandshakeState.Idle) 1f else 0f,
        animationSpec = tween(durationMillis = 260),
        label         = "topContentAlpha",
    )

    // ── UI root ───────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Always in layout so the button never shifts — only the alpha changes
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(topAlpha),
            ) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    BackButton(onClick = onBack)
                    Mono(
                        text          = "HANDSHAKE",
                        size          = 9.sp,
                        color         = EmberMuted,
                        letterSpacing = 0.28.sp,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Rule()
                Spacer(Modifier.height(22.dp))

                // ── Headline ──────────────────────────────────────────────────────
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Mono(
                        text          = "connect with someone",
                        size          = 10.sp,
                        color         = EmberMuted,
                        letterSpacing = 0.32.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = "hold together.",
                        style = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontSize   = 26.sp,
                            color      = EmberFg,
                            lineHeight = 32.sp,
                        ),
                    )
                    Text(
                        text  = "same moment, same place.",
                        style = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontSize   = 26.sp,
                            color      = EmberMuted,
                            lineHeight = 32.sp,
                        ),
                    )
                }

                Spacer(Modifier.height(18.dp))

                // ── Group mode toggle ─────────────────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Mono(
                            text          = "group mode",
                            size          = 10.sp,
                            color         = if (groupMode) EmberFg else EmberMuted,
                            letterSpacing = 0.22.sp,
                        )
                        Mono(
                            text          = if (groupMode) "connect everyone in range" else "connect the closest person",
                            size          = 8.sp,
                            color         = EmberMuted.copy(alpha = 0.6f),
                            letterSpacing = 0.14.sp,
                        )
                    }
                    Switch(
                        checked         = groupMode,
                        onCheckedChange = { groupMode = it },
                        colors          = SwitchDefaults.colors(
                            checkedThumbColor       = EmberBg,
                            checkedTrackColor       = EmberAccent,
                            uncheckedThumbColor     = EmberMuted,
                            uncheckedTrackColor     = EmberBorder,
                            uncheckedBorderColor    = EmberBorder,
                        ),
                    )
                }

                Spacer(Modifier.height(4.dp))
                Rule(color = EmberBorder)
            }

            // ── Top bar ───────────────────────────────────────────────────────


            // ── Central button + radar animation ──────────────────────────────
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // Radar rings behind the button
                RadarRingsAnimation(
                    state        = state,
                    buttonRadius = 76.dp,
                    size         = 290.dp,
                )

                when {
                    !blePermissionsGranted -> PermissionRequiredButton()
                    !viewModel.isBleAvailable -> BleUnavailableButton()
                    else -> HandshakeHoldButton(
                        state   = state,
                        onPress = { viewModel.onButtonPressed() },
                        onRelease = { viewModel.onButtonReleased() },
                    )
                }
            }

            // ── State caption ─────────────────────────────────────────────────
            AnimatedContent(
                targetState   = state,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label         = "stateCaption",
            ) { s ->
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (s) {
                        is HandshakeState.Idle ->
                            Mono(
                                text          = "hold to begin · release to cancel",
                                size          = 9.sp,
                                color         = EmberMuted.copy(alpha = 0.55f),
                                letterSpacing = 0.18.sp,
                            )
                        is HandshakeState.Scanning ->
                            Mono(
                                text          = "scanning · keep holding",
                                size          = 9.sp,
                                color         = EmberMuted,
                                letterSpacing = 0.22.sp,
                            )
                        is HandshakeState.LockOn ->
                            Mono(
                                text          = "signal locked · confirming",
                                size          = 9.sp,
                                color         = EmberAccent,
                                letterSpacing = 0.22.sp,
                            )
                        is HandshakeState.Confirmed ->
                            Mono(
                                text          = "connected.",
                                size          = 9.sp,
                                color         = EmberFg,
                                letterSpacing = 0.22.sp,
                            )
                        is HandshakeState.Error ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Mono(
                                    text          = s.message,
                                    size          = 9.sp,
                                    color         = EmberAccent,
                                    letterSpacing = 0.14.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                PxButton(
                                    text    = "Try again",
                                    onClick = { viewModel.reset() },
                                )
                            }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }

        // ── Confirmation wave overlay ─────────────────────────────────────────
        if (showWave) {
            ConfirmationWaveOverlay(
                onComplete = {
                    showWave = false
                    val confirmed = state as? HandshakeState.Confirmed ?: return@ConfirmationWaveOverlay
                    onConfirmed(confirmed.memento)
                },
            )
        }
    }
}

// ── Hold button ───────────────────────────────────────────────────────────────

/**
 * The central handshake button. Unlike [net.tosak.here.ui.components.HoldGesture],
 * this button doesn't complete after a fixed duration — it stays "active" as long
 * as the user holds it, firing [onPress] on down and [onRelease] on up.
 */
@Composable
private fun HandshakeHoldButton(
    state: HandshakeState,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = state is HandshakeState.Scanning || state is HandshakeState.LockOn
    val isLockOn = state is HandshakeState.LockOn

    // Pulse the inner circle on lock-on
    val innerScale by animateFloatAsState(
        targetValue   = if (isLockOn) 1.06f else 1f,
        animationSpec = if (isLockOn)
            infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse)
        else
            spring(),
        label = "innerPulse",
    )

    val btnColor = when (state) {
        is HandshakeState.Scanning -> EmberFg
        is HandshakeState.LockOn   -> EmberAccent
        else                       -> EmberFg
    }

    Canvas(
        modifier = modifier
            .size(160.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        onPress()
                        // Wait for all fingers to lift
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                        onRelease()
                    }
                }
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.width * 0.48f
        val innerR = size.width * 0.34f * innerScale

        // Outer dashed ring
        drawCircle(
            color  = EmberFg.copy(alpha = 0.25f),
            radius = outerR,
            center = Offset(cx, cy),
            style  = Stroke(
                width      = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(3f, 5f)
                ),
            ),
        )

        // Lock-on arc ring (full 360° in EmberAccent)
        if (isLockOn) {
            drawArc(
                color      = EmberAccent.copy(alpha = 0.55f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                topLeft    = Offset(cx - outerR, cy - outerR),
                size       = Size(outerR * 2, outerR * 2),
                style      = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Inner filled circle
        drawCircle(
            color  = if (isActive) btnColor else Color.Transparent,
            radius = innerR,
            center = Offset(cx, cy),
        )
        drawCircle(
            color  = btnColor,
            radius = innerR,
            center = Offset(cx, cy),
            style  = Stroke(1.dp.toPx()),
        )

        // Label
        // (Text drawing is delegated to the Column below the Canvas)
    }
}

@Composable
private fun PermissionRequiredButton() {
    Box(
        modifier         = Modifier
            .size(160.dp)
            .border(1.dp, EmberBorder),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Mono(text = "BLUETOOTH", size = 9.sp, color = EmberMuted)
            Mono(text = "PERMISSION", size = 9.sp, color = EmberMuted)
            Mono(text = "REQUIRED", size = 9.sp, color = EmberAccent)
        }
    }
}

@Composable
private fun BleUnavailableButton() {
    Box(
        modifier         = Modifier
            .size(160.dp)
            .border(1.dp, EmberBorder),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Mono(text = "BLUETOOTH", size = 9.sp, color = EmberMuted)
            Mono(text = "UNAVAILABLE", size = 9.sp, color = EmberAccent)
        }
    }
}

// ── Haptic helpers ────────────────────────────────────────────────────────────

/**
 * Sonar scanning pattern — calm, rhythmic. Loops indefinitely (repeat = 0).
 * delay · buzz · pause · buzz · long-pause
 */
private val HAPTIC_SCANNING = longArrayOf(0, 40, 800, 40, 800)

/**
 * Lock-on pattern — quickened to communicate "closing in". Loops (repeat = 0).
 */
private val HAPTIC_LOCK_ON = longArrayOf(0, 40, 400, 40, 400)

/**
 * Confirmation pattern — single conclusive burst. No repeat (repeat = -1).
 */
private val HAPTIC_CONFIRMED = longArrayOf(0, 80, 100, 200)

@Suppress("DEPRECATION")
private fun startVibrationPattern(context: Context, pattern: LongArray, repeat: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val vibrator = getVibrator(context)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, repeat))
    } else {
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(pattern, repeat)
    }
}

@Suppress("DEPRECATION")
private fun cancelVibration(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.cancel()
    } else {
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.cancel()
    }
}

@Suppress("DEPRECATION")
private fun getVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

// ── Permission check ──────────────────────────────────────────────────────────

private fun checkBlePermissions(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        // On pre-31, BLUETOOTH is a normal permission — assumed granted.
        // ACCESS_FINE_LOCATION was already requested by MapScreen.
        true
    }
}
