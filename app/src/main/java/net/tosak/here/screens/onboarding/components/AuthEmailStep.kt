package net.tosak.here.screens.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.shared.components.Mono
import net.tosak.here.shared.components.Rule
import net.tosak.here.ui.theme.*

@Composable
fun AuthEmailStep(
    email: String,
    onEmailChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusReq.requestFocus() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(22.dp)) {

        Mono("SIGN IN", size = 10.sp, color = EmberMuted, letterSpacing = 0.32.sp)

        Text(
            text  = "enter your email.",
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize   = 30.sp,
                lineHeight = 36.sp,
                color      = EmberFg,
            ),
        )

        Text(
            text  = "we'll send a one-time code. no password. no tracking. nothing stored on our servers.",
            style = TextStyle(
                fontFamily = JetBrainsMono,
                fontSize   = 13.sp,
                lineHeight = 20.sp,
                color      = EmberMuted,
            ),
        )

        // ── Email input ────────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value         = email,
                onValueChange = { onEmailChange(it.trim().take(80)) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusReq),
                textStyle     = TextStyle(
                    fontFamily = JetBrainsMono,
                    fontSize   = 18.sp,
                    color      = EmberFg,
                ),
                cursorBrush   = SolidColor(EmberAccent),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                decorationBox = { inner ->
                    Box {
                        if (email.isEmpty()) {
                            Text(
                                text  = "you@example.com",
                                style = TextStyle(
                                    fontFamily = JetBrainsMono,
                                    fontSize   = 18.sp,
                                    color      = EmberMuted,
                                ),
                            )
                        }
                        inner()
                    }
                },
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(EmberFg))
        }

        // ── OAuth divider ──────────────────────────────────────────────────────
        Spacer(Modifier.height(4.dp))

        Row(
            modifier             = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment    = Alignment.CenterVertically,
        ) {
            Rule(modifier = Modifier.weight(1f), color = EmberBorder)
            Mono("OR", size = 8.sp, color = EmberMuted, letterSpacing = 0.24.sp)
            Rule(modifier = Modifier.weight(1f), color = EmberBorder)
        }

        // OAuth placeholder (disabled — wired up in a future iteration)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmberBorder)
                .padding(horizontal = 20.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Mono(
                "CONTINUE WITH GOOGLE  ·  COMING SOON",
                size          = 9.sp,
                color         = EmberMuted.copy(alpha = 0.38f),
                letterSpacing = 0.18.sp,
            )
        }
    }
}
