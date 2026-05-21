package net.tosak.here.screens.mapscreen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.tosak.here.model.Friend
import net.tosak.here.model.YOU_LAT
import net.tosak.here.model.YOU_LNG
import net.tosak.here.ui.components.*
import net.tosak.here.ui.theme.*
import net.tosak.here.viewmodel.MapViewModel

@Composable
fun MapScreen(
    presenceOn: Boolean,
    friendsVisible: Boolean,
    onActivate: () -> Unit,
    onCompose: () -> Unit,
    onFriend: (Friend) -> Unit,
    onSettings: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context      = LocalContext.current
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val friends      by viewModel.friends.collectAsStateWithLifecycle()

    // ── Runtime permission + location updates lifecycle ───────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]   == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.startLocationUpdates()
    }

    // Start updates when the screen enters composition; stop on exit.
    DisposableEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }

        onDispose { viewModel.stopLocationUpdates() }
    }

    // ── Resolved coordinates — real location, falling back to demo ───────────
    val youLat = userLocation?.latitude  ?: YOU_LAT
    val youLng = userLocation?.longitude ?: YOU_LNG

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(EmberBg),
    ) {
        SchematicMap(
            presenceOn  = presenceOn,
            showFriends = presenceOn && friendsVisible,
            friends     = friends,
            onFriendTap = onFriend,
            youLat      = youLat,
            youLng      = youLng,
        )

        HudStrip(presenceOn = presenceOn)

        if (!presenceOn) PresenceOffCurtain()

        if (presenceOn && !friendsVisible) {
            EmptyStatePoem(modifier = Modifier.align(Alignment.Center))
        }

        if (presenceOn) {
            PxButton(
                text     = "⚙",
                onClick  = onSettings,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 80.dp),
            )
        }

        CompassRose(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 100.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (presenceOn) {
                PxButton("＋ POST A MOMENT", onClick = onCompose, modifier = Modifier.weight(1f), primary = true)
                PxButton("OFF", onClick = onActivate)
            } else {
                PxButton("GO LIVE →", onClick = onActivate, modifier = Modifier.weight(1f), primary = true)
            }
        }
    }
}

// ── Private composables (unchanged from original) ────────────────────────────

@Composable
private fun PresenceOffCurtain() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier            = Modifier.padding(horizontal = 40.dp),
        ) {
            Mono("YOU ARE INVISIBLE.", size = 10.sp, color = EmberMuted, letterSpacing = 0.32.sp)
            Text(
                text  = "nothing here yet.",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg, lineHeight = 30.sp),
            )
            Text(
                text  = "go live to see who is near.",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberMuted, lineHeight = 30.sp),
            )
        }
    }
}

@Composable
private fun EmptyStatePoem(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    Column(
        modifier            = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "nobody is near.",
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 18.sp, color = EmberFg, lineHeight = 26.sp),
        )
        Text(
            "go outside.",
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 18.sp, color = EmberMuted, lineHeight = 26.sp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { i ->
                val alpha by infiniteTransition.animateFloat(
                    initialValue       = 0.25f,
                    targetValue        = 0.75f,
                    animationSpec      = infiniteRepeatable(
                        animation          = tween(800),
                        repeatMode         = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(i * 300),
                    ),
                    label = "dot$i",
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(EmberMuted.copy(alpha = alpha), shape = CircleShape),
                )
            }
        }
    }
}

@Composable
private fun CompassRose(modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier,
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(Modifier.size(38.dp)) {
            val cx = size.width / 2; val cy = size.height / 2; val r = size.width * 0.45f
            drawCircle(
                color  = EmberFg.copy(alpha = 0.35f),
                radius = r,
                center = Offset(cx, cy),
                style  = Stroke(0.8f),
            )
            val path = Path().apply {
                moveTo(cx, cy - r * 0.8f)
                lineTo(cx - size.width * 0.08f, cy)
                lineTo(cx, cy - size.width * 0.1f)
                lineTo(cx + size.width * 0.08f, cy)
                close()
            }
            drawPath(path, color = EmberFg)
        }
        Mono("N · 0°", size = 9.sp, color = EmberMuted)
    }
}