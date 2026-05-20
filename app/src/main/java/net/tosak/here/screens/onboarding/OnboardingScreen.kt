package net.tosak.here.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.screens.onboarding.components.InfoStepBody
import net.tosak.here.screens.onboarding.data.OnboardStep
import net.tosak.here.ui.components.*
import net.tosak.here.ui.theme.*


private val STEPS = listOf(
    OnboardStep("01 / 04", "presence over content.", "this app is a trigger, not a destination. it lives between you and the people you actually want to see."),
    OnboardStep(
        "02 / 04",
        "you must be there.",
        "posts exist only inside their place. step outside the radius and they disappear. nothing follows you home."
    ),
    OnboardStep(
        "03 / 04",
        "you are invisible by default.",
        "no passive tracking. no last seen. you flip presence on when you want to be findable — and only then."
    ),
    OnboardStep(
        "04 / 04",
        "nothing persists.",
        "no archive, no inbox, no profile to maintain. moments expire. that is the point."
    ),
)

@Composable
fun OnboardingScreen(onDone: (String) -> Unit) {
    var step      by remember { mutableIntStateOf(0) }
    var showName  by remember { mutableStateOf(false) }
    var handle    by remember { mutableStateOf("") }
    val focusReq  = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 28.dp),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Mono("· here", size = 10.sp, color = EmberFg, letterSpacing = 0.36.sp)
            Mono("v0.1 · SKOPJE", size = 9.sp, color = EmberMuted, letterSpacing = 0.18.sp)
        }

        // Body
        AnimatedContent(
            targetState   = showName,
            modifier      = Modifier.align(Alignment.Center),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label         = "onboardBody",
        ) { nameStep ->
            if (!nameStep) {
                AnimatedContent(
                    targetState   = step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label         = "onboardStep",
                ) { s ->
                    val info = STEPS[s]
                    InfoStepBody(info,s);
                }
            } else {
                LaunchedEffect(Unit) { focusReq.requestFocus() }
                Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    Mono("SET HANDLE", size = 10.sp, color = EmberMuted, letterSpacing = 0.32.sp)
                    Text(
                        text  = "name yourself.",
                        style = TextStyle(
                            fontFamily = JetBrainsMono,
                            fontSize   = 30.sp,
                            color      = EmberFg,
                        ),
                    )
                    Text(
                        text  = "one word. no real name required. people will see this if they are nearby and you are live.",
                        style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 20.sp, color = EmberMuted),
                    )
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("@", style = TextStyle(fontFamily = JetBrainsMono, fontSize = 20.sp, color = EmberMuted))
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value         = handle,
                            onValueChange = { handle = it.replace(" ", "").take(16) },
                            modifier      = Modifier
                                .weight(1f)
                                .focusRequester(focusReq),
                            textStyle     = TextStyle(
                                fontFamily = JetBrainsMono,
                                fontSize   = 20.sp,
                                color      = EmberFg,
                            ),
                            cursorBrush   = SolidColor(EmberAccent),
                            singleLine    = true,
                        )
                        Mono("${16 - handle.length}", size = 9.sp, color = EmberMuted)
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(EmberFg))
                    Mono(
                        "YOU CAN CHANGE IT LATER. FRIENDS FIND YOU BY HANDLE.",
                        size = 9.sp, color = EmberMuted, letterSpacing = 0.10.sp,
                    )
                }
            }
        }

        // Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Step indicators
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                STEPS.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (!showName && i <= step) EmberFg else EmberFg.copy(alpha = 0.18f)),
                    )
                }
                if (showName) {
                    Box(Modifier.weight(1f).height(2.dp).background(EmberAccent))
                }
            }

            // Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!showName && step > 0) {
                    PxButton("BACK", onClick = { step-- })
                }
                if (!showName && step < STEPS.lastIndex) {
                    PxButton("CONTINUE", onClick = { step++ }, modifier = Modifier.weight(1f), primary = true)
                } else if (!showName) {
                    PxButton("ALMOST DONE →", onClick = { showName = true }, modifier = Modifier.weight(1f), primary = true)
                } else {
                    PxButton(
                        text     = if (handle.isNotBlank()) "ENTER AS @${handle.uppercase()}" else "ENTER ANONYMOUSLY",
                        onClick  = { onDone(handle.ifBlank { "you" }) },
                        modifier = Modifier.weight(1f),
                        primary  = true,
                    )
                }
            }

            Mono(
                "NO EMAIL · NO PHONE · NO PROFILE",
                size          = 9.sp,
                color         = EmberMuted,
                letterSpacing = 0.2.sp,
                modifier      = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

// Simple per-step SVG-style glyphs drawn with Canvas
