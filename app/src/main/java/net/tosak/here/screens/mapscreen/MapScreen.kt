package net.tosak.here.screens.mapscreen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.components.*
import net.tosak.here.ui.theme.*
import net.tosak.here.screens.mapscreen.viewmodel.MapViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.unit.DpOffset
import net.tosak.here.shared.location.rememberLocationEnabled
import net.tosak.here.shared.model.YOU_LAT
import net.tosak.here.shared.model.YOU_LNG
import kotlin.math.absoluteValue

@Composable
fun MapScreen(
    presenceOn: Boolean,
    friendsVisible: Boolean,
    onActivate: () -> Unit,
    onCompose: () -> Unit,
    onFriend: (Friend) -> Unit,
    onSettings: () -> Unit,
    onHandshake: () -> Unit = {},
    onChat: () -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context      = LocalContext.current
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val friends      by viewModel.friends.collectAsStateWithLifecycle()
    val isLocationEnabled = rememberLocationEnabled()

    // ── Runtime permission + location updates lifecycle ───────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]   == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.startLocationUpdates()
    }

    DisposableEffect(Unit) {
        val fineGranted   = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.startLocationUpdates()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ))
        }

        onDispose { viewModel.stopLocationUpdates() }
    }

    val youLat = userLocation?.latitude  ?: YOU_LAT
    val youLng = userLocation?.longitude ?: YOU_LNG

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
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

            // ── Top toolbar ───────────────────────────────────────────────────────
            MapTopBar(
                presenceOn = presenceOn,
                youLat     = youLat,
                youLng     = youLng,
                onSettings = onSettings,
                onChat     = onChat,
                modifier   = Modifier.align(Alignment.TopCenter),
            )


        if (!presenceOn) Curtain {
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

        if(userLocation == null){
            Curtain {
                Mono("Fetching Location...")
            }

        }

        if(!isLocationEnabled.value){
            Curtain {
                Mono("Location turned off.")
            }
        }


        if (presenceOn && !friendsVisible) {
            EmptyStatePoem(modifier = Modifier.align(Alignment.Center))
        }

        CompassRose(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 100.dp),
        )

        // ── Bottom bar ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(EmberBg),
        ) {
            Rule()
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Primary presence action
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (presenceOn) {
                        PxButton("＋ POST A MOMENT", onClick = onCompose, modifier = Modifier.weight(1f), primary = true)
                        PxButton("OFF", onClick = onActivate)
                    } else {
                        PxButton("GO LIVE →", onClick = onActivate, modifier = Modifier.weight(1f), primary = true)
                    }
                }
                // Connect — always present, secondary
                PxButton(
                    text     = "⬡  CONNECT",
                    onClick  = onHandshake,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Top toolbar ───────────────────────────────────────────────────────────────

@Composable
private fun MapTopBar(
    presenceOn: Boolean,
    youLat: Double,
    youLng: Double,
    onSettings: () -> Unit,
    onChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // ── Menu toggle + dropdown ────────────────────────────────────────────
        Box {
            // Single pill toggle
            Box(
                modifier = Modifier
                    .background(EmberBg.copy(alpha = 0.92f))
                    .border(1.dp, if (menuExpanded) EmberFg.copy(alpha = 0.45f) else EmberBorder)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { menuExpanded = !menuExpanded }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Mono(
                    text          = "☰",
                    size          = 12.sp,
                    color         = if (menuExpanded) EmberFg else EmberFg.copy(alpha = 0.65f),
                    letterSpacing = 0.sp,
                )
            }

            // Dropdown
            DropdownMenu(
                expanded         = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                offset           = DpOffset(x = 0.dp, y = 4.dp),
                shape            = RoundedCornerShape(0.dp),
                containerColor   = EmberBg,
                tonalElevation   = 0.dp,
                shadowElevation  = 0.dp,
                border           = BorderStroke(1.dp, EmberBorder),
                modifier         = Modifier.widthIn(min = 160.dp),
            ) {
                MapMenuItem(label = "⚙  SETTINGS") { menuExpanded = false; onSettings() }
                Box(Modifier.fillMaxWidth().height(1.dp).background(EmberBorder))
                MapMenuItem(label = "✉  MSGS")     { menuExpanded = false; onChat() }
                // ── Add future menu items here, separated by a Box(…) divider ──
            }
        }

        // ── Presence chip + coords ────────────────────────────────────────────
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusChip(presenceOn = presenceOn)
            Mono(
                text          = formatCoords(youLat, youLng),
                size          = 8.sp,
                color         = EmberMuted,
                letterSpacing = 0.14.sp,
            )
        }
    }
}

/** Single row inside the [MapTopBar] dropdown. */
@Composable
private fun MapMenuItem(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(EmberBg)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Mono(label, size = 9.sp, color = EmberFg, letterSpacing = 0.20.sp)
    }
}

/** Formats a WGS-84 coordinate pair as  41°59′N · 21°25′E */
private fun formatCoords(lat: Double, lng: Double): String {
    val latD   = lat.absoluteValue.toInt()
    val latM   = ((lat.absoluteValue - latD) * 60).toInt()
    val latDir = if (lat >= 0) "N" else "S"
    val lngD   = lng.absoluteValue.toInt()
    val lngM   = ((lng.absoluteValue - lngD) * 60).toInt()
    val lngDir = if (lng >= 0) "E" else "W"
    return "${latD}°${latM}′${latDir} · ${lngD}°${lngM}′${lngDir}"
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun Curtain(content: @Composable () -> Unit) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(EmberBg.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier            = Modifier.padding(horizontal = 40.dp),
        ) {
           content()
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