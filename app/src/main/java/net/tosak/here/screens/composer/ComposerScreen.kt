package net.tosak.here.screens.composer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.tosak.here.shared.model.PostKind
import net.tosak.here.shared.components.*
import net.tosak.here.screens.composer.viewmodel.ComposerViewModel
import net.tosak.here.ui.theme.*
import java.io.File

@Composable
fun ComposerScreen(
    onClose: () -> Unit,
    onSubmit: (PostKind, String) -> Unit,
    viewModel: ComposerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    var kind by remember { mutableStateOf<PostKind?>(null) }
    var text by remember { mutableStateOf("") }

    // ── Photo capture state ───────────────────────────────────────────────────
    // capturedPath: set when TakePicture returns success; passed to the ViewModel on submit.
    // pendingFile:  the File we created before launching the camera (needed in the callback).
    // imageBitmap:  decoded bitmap for display; loaded off-thread when capturedPath changes.
    // triggerCapture: flipped to true after permission is confirmed, consumed by LaunchedEffect.
    var capturedPath   by remember { mutableStateOf<String?>(null) }
    var imageBitmap    by remember { mutableStateOf<ImageBitmap?>(null) }
    val pendingFile     = remember { mutableStateOf<File?>(null) }
    var triggerCapture by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPath = pendingFile.value?.absolutePath
        } else {
            pendingFile.value?.delete()   // discard the empty file if the user cancelled
        }
        pendingFile.value = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) triggerCapture = true }

    // Launches the system camera after permission is confirmed.
    LaunchedEffect(triggerCapture) {
        if (!triggerCapture) return@LaunchedEffect
        triggerCapture = false
        val imgDir = File(context.filesDir, "images").also { it.mkdirs() }
        val file   = File(imgDir, "photo_${System.currentTimeMillis()}.jpg")
        val uri    = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingFile.value = file
        cameraLauncher.launch(uri)
    }

    // Decodes the JPEG off the main thread whenever a new path is captured.
    LaunchedEffect(capturedPath) {
        imageBitmap = withContext(Dispatchers.IO) {
            capturedPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        }
    }

    val startCapture: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (granted) triggerCapture = true
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .padding(horizontal = 22.dp),
    ) {
        HudStrip(presenceOn = true, minimal = true)

        Row(
            modifier              = Modifier.fillMaxWidth().padding(top = 38.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom,
        ) {
            Mono("POST A MOMENT", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
            Mono("EXPIRES 2H · 400M", size = 9.sp, color = EmberMuted)
        }

        Spacer(Modifier.height(20.dp))

        // ── Kind picker ───────────────────────────────────────────────────────
        if (kind == null) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                KindCard("PHOTO", "A frame from where you are.", "⊡") { kind = PostKind.PHOTO }
                Spacer(Modifier.height(12.dp))
                KindCard("TEXT", "140 characters. A line, a thought, a question.", "≡") { kind = PostKind.TEXT }
            }
        }

        // ── Photo composer ────────────────────────────────────────────────────
        if (kind == PostKind.PHOTO) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, if (imageBitmap != null) EmberAccent else EmberFg)
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick           = startCapture,
                    )
                    .then(
                        // Only draw hatching when no photo has been captured yet
                        if (imageBitmap == null) Modifier.drawBehind {
                            val step = 12.dp.toPx()
                            var x = -size.height
                            while (x < size.width + size.height) {
                                drawLine(EmberFg.copy(alpha = 0.07f), Offset(x, 0f), Offset(x + size.height, size.height), 1f)
                                x += step
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (imageBitmap != null) {
                    // ── Captured photo ────────────────────────────────────────
                    Image(
                        bitmap             = imageBitmap!!,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop,
                    )
                    // "RETAKE" tap target — top-right corner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(EmberBg.copy(alpha = 0.82f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Mono("RETAKE", size = 8.sp, color = EmberFg, letterSpacing = 0.22.sp)
                    }
                } else {
                    // ── Placeholder ───────────────────────────────────────────
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Mono("[FRAME]", size = 9.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("tap to capture", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = EmberMuted))
                    }
                    Mono(
                        "● REC READY",
                        size     = 8.sp,
                        color    = EmberMuted,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            BasicTextField(
                value         = text,
                onValueChange = { text = it.take(80) },
                modifier      = Modifier.fillMaxWidth().border(1.dp, EmberBorder).padding(12.dp),
                textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, color = EmberFg),
                cursorBrush   = SolidColor(EmberAccent),
                decorationBox = { inner ->
                    if (text.isEmpty()) Mono("caption (optional)…", size = 13.sp, color = EmberMuted)
                    inner()
                },
            )
        }

        // ── Text composer ─────────────────────────────────────────────────────
        if (kind == PostKind.TEXT) {
            BasicTextField(
                value         = text,
                onValueChange = { text = it.take(140) },
                modifier      = Modifier.weight(1f).fillMaxWidth().border(1.dp, EmberBorder).padding(14.dp),
                textStyle     = TextStyle(fontFamily = JetBrainsMono, fontSize = 15.sp, lineHeight = 22.sp, color = EmberFg),
                cursorBrush   = SolidColor(EmberAccent),
                maxLines      = Int.MAX_VALUE,
                decorationBox = { inner ->
                    if (text.isEmpty()) Text(
                        "say something only the people here can read…",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 15.sp, color = EmberMuted),
                    )
                    inner()
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Mono("${text.length} / 140", size = 9.sp, color = EmberMuted)
                Mono("~ ${text.split(Regex("\\s+")).filter { it.isNotBlank() }.size.coerceAtLeast(1)} WORDS", size = 9.sp, color = EmberMuted)
            }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PxButton("← BACK", onClick = {
                if (kind != null) {
                    kind = null
                    capturedPath = null
                    imageBitmap  = null
                } else {
                    onClose()
                }
            })
            if (kind != null) {
                val canPost = kind == PostKind.TEXT || imageBitmap != null
                PxButton(
                    text     = "POST TO RADIUS →",
                    onClick  = {
                        val k = kind!!
                        viewModel.submit(k, text, capturedPath) { onSubmit(k, text) }
                    },
                    modifier = Modifier.weight(1f),
                    primary  = canPost,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun KindCard(label: String, hint: String, icon: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmberFg)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 20.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(icon, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg))
        Column(modifier = Modifier.weight(1f)) {
            Mono(label, size = 13.sp, letterSpacing = 0.22.sp)
            Spacer(Modifier.height(4.dp))
            Text(hint, style = TextStyle(fontFamily = JetBrainsMono, fontSize = 10.sp, color = EmberMuted))
        }
        Text("→", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 16.sp, color = EmberMuted))
    }
}
