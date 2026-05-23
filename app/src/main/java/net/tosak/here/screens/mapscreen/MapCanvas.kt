package net.tosak.here.screens.mapscreen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import net.tosak.here.shared.components.FriendMarkerView
import net.tosak.here.shared.components.Mono
import net.tosak.here.shared.model.Friend
import net.tosak.here.shared.model.YOU_LAT
import net.tosak.here.shared.model.YOU_LNG
import net.tosak.here.shared.storage.PostEntity
import net.tosak.here.ui.theme.HereTheme
import net.tosak.here.ui.theme.*
import kotlin.math.*

// ── Constants ─────────────────────────────────────────────────────────────────
private const val STYLE_URL = "mapbox://styles/stevetosak/cmpegiuhf001l01sc8dwo4o3j"

// Fraction of screen half-width that the range radius should occupy at min zoom.
private const val RADIUS_FRACTION = 0.448f

// ── Zoom helper ───────────────────────────────────────────────────────────────
/**
 * Computes the Mapbox zoom level so [radiusMeters] fills [RADIUS_FRACTION]
 * of the screen's half-width.
 *
 * Mapbox GL uses 512-px tiles, so:
 *   resolution = (earthCircumference × cos(lat)) / (2^zoom × 512)  m/px
 *   desired    = radiusMeters / (screenWidthPx × RADIUS_FRACTION)  m/px
 */
fun radiusToZoom(
    radiusMeters: Double,
    latitudeDeg: Double,
    screenWidthPx: Int,
): Double {
    val desiredMPP    = radiusMeters / (screenWidthPx * RADIUS_FRACTION)
    val earthCirc     = 40_075_016.686
    val mppAtZoomZero = earthCirc * cos(latitudeDeg * PI / 180.0) / 512.0
    return log2(mppAtZoomZero / desiredMPP)
}

// ── Main composable ───────────────────────────────────────────────────────────
@Composable
fun SchematicMap(
    presenceOn: Boolean,
    showFriends: Boolean,
    friends: List<Friend>,
    onFriendTap: (Friend) -> Unit,
    activePost: PostEntity?,
    onYouTap: () -> Unit,
    modifier: Modifier = Modifier,
    youLat: Double = YOU_LAT,
    youLng: Double = YOU_LNG,
    radiusMeters: Double = 400.0,
) {
    val density       = LocalDensity.current
    val screenWidthPx = with(density) { 390.dp.roundToPx() }

    // Minimum zoom = radius fills RADIUS_FRACTION of the screen — cannot zoom out past this.
    val minZoom = remember(radiusMeters, youLat, screenWidthPx) {
        radiusToZoom(radiusMeters, youLat, screenWidthPx)
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(youLng, youLat))
            zoom(minZoom)
            pitch(0.0)
            bearing(0.0)
        }
    }

    // Current zoom read from Compose-observable CameraState — drives range-circle scaling.
    val currentZoom = mapViewportState.cameraState?.zoom ?: minZoom

    // Stable ref so ViewAnnotation lambdas always call the current callback.
    val onFriendTapRef = rememberUpdatedState(onFriendTap)

    Box(modifier = modifier) {

        MapboxMap(
            modifier         = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style            = { MapStyle(style = STYLE_URL) },
            scaleBar         = {},
        ) {

            // ── One-time setup: gestures ──────────────────────────────────────
            // Pan and pinch-zoom are enabled; rotate/pitch/zoom-out are locked.
            MapEffect(Unit) { mapView ->
                mapView.gestures.apply {
                    scrollEnabled                           = true
                    pinchToZoomEnabled                      = true
                    rotateEnabled                           = false
                    pitchEnabled                            = false
                    doubleTapToZoomInEnabled                = true
                    doubleTouchToZoomOutEnabled             = false
                    quickZoomEnabled                        = false
                    simultaneousRotateAndPinchToZoomEnabled = false
                }
            }

            // ── Camera + bounds sync ──────────────────────────────────────────
            // ORDER MATTERS: clear bounds → setCamera → re-apply bounds.
            // This prevents the new position from being clamped by stale bounds
            // when the device's GPS accuracy improves mid-session.
            MapEffect(minZoom, youLat, youLng, radiusMeters) { mapView ->
                // 1 — lift any existing pan/zoom restriction
                mapView.mapboxMap.setBounds(CameraBoundsOptions.Builder().build())

                // 2 — re-centre on the (possibly new) GPS position
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(youLng, youLat))
                        .zoom(minZoom)
                        .build()
                )

                // 3 — lock pan to ±radius around the new position
                val dLat = radiusMeters / 111_000.0
                val dLng = radiusMeters / (111_000.0 * cos(youLat * PI / 180.0))
                mapView.mapboxMap.setBounds(
                    CameraBoundsOptions.Builder()
                        .bounds(
                            CoordinateBounds(
                                Point.fromLngLat(youLng - dLng, youLat - dLat), // SW
                                Point.fromLngLat(youLng + dLng, youLat + dLat), // NE
                            )
                        )
                        .minZoom(minZoom)
                        .build()
                )
            }

            // ── "You" marker — always rendered at the GPS coordinate ──────────
            // Lives inside Mapbox so it tracks the real position when panning.
            ViewAnnotation(
                options = viewAnnotationOptions {
                    geometry(Point.fromLngLat(youLng, youLat))
                    allowOverlap(true)
                },
            ) {
                HereTheme { YouMarkerView(presenceOn = presenceOn, activePost = activePost, onClick = onYouTap) }
            }

            // ── Friend markers ────────────────────────────────────────────────
            if (showFriends && presenceOn) {
                friends.forEach { friend ->
                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(friend.lng, friend.lat))
                            allowOverlap(true)
                        },
                    ) {
                        HereTheme {
                            FriendMarkerView(
                                friend  = friend,
                                onClick = { onFriendTapRef.value(friend) },
                            )
                        }
                    }
                }
            }
        }

        // ── Compose Canvas overlay: range rings + vignette ────────────────────
        // The "You" position marker has been moved to a ViewAnnotation so it
        // follows the real GPS coordinate when the user pans the map.
        MapOverlayCanvas(
            presenceOn     = presenceOn,
            radiusFraction = RADIUS_FRACTION,
            currentZoom    = currentZoom,
            minZoom        = minZoom,
            modifier       = Modifier.fillMaxSize(),
        )
    }
}

// ── Canvas overlay: range rings · vignette ────────────────────────────────────
@Composable
private fun MapOverlayCanvas(
    presenceOn: Boolean,
    radiusFraction: Float,
    currentZoom: Double,
    minZoom: Double,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val cx = size.width  / 2f
        val cy = size.height / 2f

        // ── Range circles — scale with zoom so the geographic boundary stays correct ──
        // At minZoom, R = radiusFraction * screenWidth (the calibration point).
        // Each additional zoom level doubles the pixel radius (same metres, more px/m).
        if (presenceOn) {
            val zoomDelta = (currentZoom - minZoom).toFloat().coerceAtLeast(0f)
            val R = size.width * radiusFraction * 2f.pow(zoomDelta)

            drawCircle(
                brush  = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f   to EmberAccent.copy(alpha = 0.06f),
                        0.6f to EmberAccent.copy(alpha = 0.02f),
                        1f   to Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = R,
                ),
                radius = R,
                center = Offset(cx, cy),
            )
            // Outer dashed boundary ring
            drawCircle(
                color  = EmberFg.copy(alpha = 0.35f),
                radius = R,
                center = Offset(cx, cy),
                style  = Stroke(
                    width      = 0.6.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)),
                ),
            )
            // Mid + inner rings (decorative)
            drawCircle(EmberFg.copy(alpha = 0.18f), R * 0.66f, Offset(cx, cy), style = Stroke(0.4.dp.toPx()))
            drawCircle(EmberFg.copy(alpha = 0.14f), R * 0.33f, Offset(cx, cy), style = Stroke(0.4.dp.toPx()))
        }

        // ── Vignette ──────────────────────────────────────────────────────────
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f)),
                center     = Offset(cx, cy),
                radius     = size.width * 0.8f,
            ),
        )
    }
}

// ── "You" position marker (ViewAnnotation — tracks real GPS position) ─────────
/**
 * When [activePost] is non-null, a callout card appears above the crosshair
 * showing the post-kind glyph and remaining time, consistent with FriendMarkerView:
 *
 *   ┌──────────────┐
 *   │  ≡ · 1H 43M │  ← EmberAccent border; your own post
 *   └──────┬───────┘
 *          │           ← 1 dp stem
 *          ◉           ← dot + animated pulse when presence on
 */
@Composable
private fun YouMarkerView(presenceOn: Boolean, activePost: PostEntity?, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "youPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue  = 0.7f, targetValue  = 1.4f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing), RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            enabled           = activePost != null,
            indication        = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick           = onClick,
        ),
    ) {

        // ── Callout card — only shown when the user has an active post ────────
        if (activePost != null) {
            Box(
                modifier = Modifier
                    .background(EmberBg)
                    .border(0.5.dp, EmberAccent)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Mono(postGlyphForKind(activePost.kind), size = 9.sp, color = EmberAccent, letterSpacing = 0.sp)
                    Mono("·",                              size = 9.sp, color = EmberMuted,   letterSpacing = 0.sp)
                    Mono(timeLeft(activePost.expiresAt),   size = 8.sp, color = EmberMuted,   letterSpacing = 0.10.sp)
                }
            }

            // Stem connecting card to dot
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(6.dp)
                    .background(EmberAccent),
            )
        }

        // ── Dot + pulse ring + crosshair ──────────────────────────────────────
        Canvas(modifier = Modifier.size(48.dp)) {
            val cx   = size.width  / 2f
            val cy   = size.height / 2f
            val base = 6.dp.toPx()

            // ── Animated presence pulse ───────────────────────────────────────
            if (presenceOn) {
                drawCircle(EmberAccent.copy(alpha = pulseAlpha), base + base * pulseScale, Offset(cx, cy))
                drawCircle(EmberAccent.copy(alpha = 0.55f), base, Offset(cx, cy))
            }

            // ── Dot ───────────────────────────────────────────────────────────
            val dotR = 4.dp.toPx()
            drawCircle(if (presenceOn) EmberAccent else Color.Transparent, dotR, Offset(cx, cy))
            drawCircle(EmberFg, dotR, Offset(cx, cy), style = Stroke(1.2.dp.toPx()))

            // ── Crosshair ─────────────────────────────────────────────────────
            val arm = 11.dp.toPx()
            val gap = 5.dp.toPx()
            val lw  = 0.8.dp.toPx()
            val col = EmberFg.copy(alpha = 0.65f)
            drawLine(col, Offset(cx - arm, cy), Offset(cx - gap, cy), lw)
            drawLine(col, Offset(cx + gap, cy), Offset(cx + arm, cy), lw)
            drawLine(col, Offset(cx, cy - arm), Offset(cx, cy - gap), lw)
            drawLine(col, Offset(cx, cy + gap), Offset(cx, cy + arm), lw)
        }
    }
}

/** Maps a [PostEntity.kind] string to its display glyph. */
private fun postGlyphForKind(kind: String): String = when (kind) {
    "PHOTO" -> "⊡"
    "TEXT"  -> "≡"
    else    -> "·"
}

/** Formats remaining post lifetime as "1H 43M" or "12M". */
private fun timeLeft(expiresAt: Long): String {
    val ms = (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    val h  = ms / 3_600_000L
    val m  = (ms % 3_600_000L) / 60_000L
    return if (h > 0L) "${h}H ${m}M" else "${m}M"
}
