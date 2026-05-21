package net.tosak.here.screens.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.ui.components.Mono
import net.tosak.here.ui.theme.*

@Composable
fun AuthVerifyStep(
    email: String,
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusReq       = remember { FocusRequester() }
    val keyboardCtrl   = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusReq.requestFocus() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {

        Mono("VERIFY", size = 10.sp, color = EmberMuted, letterSpacing = 0.32.sp)

        Text(
            text  = "check your inbox.",
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize   = 30.sp,
                lineHeight = 36.sp,
                color      = EmberFg,
            ),
        )

        Text(
            text  = "a 6-digit code was sent to $email.\nit expires in 10 minutes.",
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize   = 13.sp,
                lineHeight = 20.sp,
                color      = EmberMuted,
            ),
        )

        // ── OTP boxes ─────────────────────────────────────────────────────────
        // A hidden BasicTextField captures keyboard input; the visual Row
        // renders 6 individual digit boxes on top.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
        ) {
            // Hidden input layer — invisible but receives all keyboard events
            BasicTextField(
                value         = code,
                onValueChange = { v ->
                    if (v.length <= 6 && v.all { it.isDigit() }) onCodeChange(v)
                },
                modifier      = Modifier
                    .matchParentSize()
                    .alpha(0f)
                    .focusRequester(focusReq),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle     = TextStyle(color = EmberBg, fontSize = 1.sp),
                cursorBrush   = SolidColor(EmberBg),
                singleLine    = true,
            )

            // Visual layer — tapping re-focuses the hidden field AND explicitly
            // shows the keyboard so it reappears after a user-dismiss.
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        indication        = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) {
                        focusReq.requestFocus()
                        keyboardCtrl?.show()
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(6) { i ->
                    val char    = code.getOrNull(i)
                    val isActive = i == code.length

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = 1.dp,
                                color = when {
                                    isActive     -> EmberAccent
                                    char != null -> EmberFg.copy(alpha = 0.65f)
                                    else         -> EmberBorder
                                },
                            )
                            .background(EmberSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (char != null) {
                            Text(
                                text  = char.toString(),
                                style = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize   = 22.sp,
                                    color      = EmberFg,
                                ),
                            )
                        }
                        // Blinking cursor in the active empty box
                        if (isActive && char == null) {
                            CursorLine()
                        }
                    }
                }
            }
        }

        // Mock hint
        Mono(
            "MOCK MODE · ANY 6-DIGIT CODE ACCEPTED",
            size          = 9.sp,
            color         = EmberAccent.copy(alpha = 0.55f),
            letterSpacing = 0.14.sp,
        )
    }
}

// ── Blinking cursor inside the active OTP box ─────────────────────────────────
@Composable
private fun CursorLine() {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(530)
            visible = !visible
        }
    }
    if (visible) {
        Box(
            modifier = Modifier
                .width(1.5.dp)
                .height(24.dp)
                .background(EmberAccent),
        )
    }
}