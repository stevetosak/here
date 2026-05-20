package net.tosak.here.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.viewannotation.ViewAnnotationAnchor
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import net.tosak.here.model.Friend
import net.tosak.here.model.YOU_LAT
import net.tosak.here.model.YOU_LNG
import net.tosak.here.ui.theme.HereTheme
import net.tosak.here.ui.theme.*
import kotlin.math.*

// ── Constants ─────────────────────────────────────────────────────────────────
private const val STYLE_URL = "mapbox://styles/stevetosak/cmpegiuhf001l01sc8dwo4o3j"

// Fraction of screen half-width that the range radius should occupy.
// Matches the original prototype: 170 px out of 380 px canvas ≈ 0.448.
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
    val desiredMPP       = radiusMeters / (screenWidthPx * RADIUS_FRACTION)
    val earthCirc        = 40_075_016.686
    val mppAtZoomZero    = earthCirc * cos(latitudeDeg * PI / 180.0) / 512.0
    return log2(mppAtZoomZero / desiredMPP)
}

// ── Main composable ───────────────────────────────────────────────────────────
@Composable
fun SchematicMap(
    presenceOn: Boolean,
    showFriends: Boolean,
    friends: List<Friend>,
    onFriendTap: (Friend) -> Unit,
    modifier: Modifier = Modifier,
    youLat: Double = YOU_LAT,
    youLng: Double = YOU_LNG,
    radiusMeters: Double = 400.0,
) {
    val context       = LocalContext.current
    val density       = LocalDensity.current
    // Use actual screen width for zoom calculation
    val screenWidthPx = with(density) { 390.dp.roundToPx() }

    val zoom = remember(radiusMeters, youLat, screenWidthPx) {
        radiusToZoom(radiusMeters, youLat, screenWidthPx)
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(youLng, youLat))
            zoom(zoom)
            pitch(0.0)
            bearing(0.0)
        }
    }

    // Stable ref so ViewAnnotation ComposeViews always call the current callback
    val onFriendTapRef = rememberUpdatedState(onFriendTap)

    Box(modifier = modifier) {

        // ── Mapbox map (Compose extension) ────────────────────────────────
        MapboxMap(
            modifier         = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            style            = { MapStyle(style = STYLE_URL) },
        ) {

            // Lock all gestures on first composition
            MapEffect(Unit) { mapView ->
                mapView.gestures.apply {
                    scrollEnabled                         = false
                    pinchToZoomEnabled                    = false
                    rotateEnabled                         = false
                    pitchEnabled                          = false
                    doubleTapToZoomInEnabled              = false
                    doubleTouchToZoomOutEnabled           = false
                    quickZoomEnabled                      = false
                    simultaneousRotateAndPinchToZoomEnabled = false
                }
            }

            // Sync camera whenever radius / position changes
            MapEffect(zoom, youLat, youLng) { mapView ->
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(youLng, youLat))
                        .zoom(zoom)
                        .build()
                )
            }

            // Friend ViewAnnotations — rebuilt when visibility or list changes
            MapEffect(showFriends, presenceOn, friends) { mapView ->
                val vam = mapView.viewAnnotationManager
                vam.removeAllViewAnnotations()

                if (!showFriends || !presenceOn) return@MapEffect

                friends.forEach { friend ->
                    val composeView = ComposeView(context).apply {
                        setViewCompositionStrategy(
                            ViewCompositionStrategy.DisposeOnDetachedFromWindow
                        )
                        setContent {
                            HereTheme {
                                FriendMarkerView(
                                    friend  = friend,
                                    onClick = { onFriendTapRef.value(friend) },
                                )
                            }
                        }
                    }

                    vam.addViewAnnotation(
                        view    = composeView,
                        options = viewAnnotationOptions {
                            geometry(Point.fromLngLat(friend.lng, friend.lat))
                            allowOverlap(true)
                            anchor(ViewAnnotationAnchor.CENTER)
                        },
                    )
                }
            }
        }

        // ── Compose Canvas overlay ────────────────────────────────────────
        // The map is always centered on the user and never panned, so the
        // user is always at the screen center — no coordinate projection needed.
        MapOverlayCanvas(
            presenceOn     = presenceOn,
            radiusFraction = RADIUS_FRACTION,
            modifier       = Modifier.fillMaxSize(),
        )
    }
}

// ── Canvas overlay: range circle · user dot · vignette ───────────────────────
@Composable
private fun MapOverlayCanvas(
    presenceOn: Boolean,
    radiusFraction: Float,
    modifier: Modifier = Modifier,
) {
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

    Canvas(modifier = modifier) {
        val cx = size.width  / 2f
        val cy = size.height / 2f
        val R  = size.width  * radiusFraction   // range radius in px

        // ── Range circles ─────────────────────────────────────────────────
        if (presenceOn) {
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
            // Outer dashed ring
            drawCircle(
                color  = EmberFg.copy(alpha = 0.35f),
                radius = R,
                center = Offset(cx, cy),
                style  = Stroke(
                    width      = 0.6.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)),
                ),
            )
            // Mid + inner rings
            drawCircle(EmberFg.copy(alpha = 0.18f), R * 0.66f, Offset(cx, cy), style = Stroke(0.4.dp.toPx()))
            drawCircle(EmberFg.copy(alpha = 0.14f), R * 0.33f, Offset(cx, cy), style = Stroke(0.4.dp.toPx()))
        }

        // ── "You" animated pulse ──────────────────────────────────────────
        if (presenceOn) {
            val base = 6.dp.toPx()
            drawCircle(EmberAccent.copy(alpha = pulseAlpha), base + base * pulseScale, Offset(cx, cy))
            drawCircle(EmberAccent.copy(alpha = 0.55f), base, Offset(cx, cy))
        }

        // ── "You" dot + ring ──────────────────────────────────────────────
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

        // ── Vignette ──────────────────────────────────────────────────────
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.5f)),
                center     = Offset(cx, cy),
                radius     = size.width * 0.8f,
            ),
        )
    }
}