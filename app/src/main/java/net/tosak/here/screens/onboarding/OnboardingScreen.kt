package net.tosak.here.screens.onboarding
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.navigation.compose.hiltViewModel
import net.tosak.here.screens.onboarding.components.AuthEmailStep
import net.tosak.here.screens.onboarding.components.AuthVerifyStep
import net.tosak.here.screens.onboarding.components.InfoStepBody
import net.tosak.here.screens.onboarding.data.OnboardStep
import net.tosak.here.screens.onboarding.viewmodel.OnboardingViewModel
import net.tosak.here.shared.components.*
import net.tosak.here.ui.theme.*

// ── Philosophy info panels ────────────────────────────────────────────────────
private val STEPS = listOf(
    OnboardStep(
        "01 / 04", "presence over content.",
        "this app is a trigger, not a destination. it lives between you and the people you actually want to see."
    ),
    OnboardStep(
        "02 / 04", "you must be there.",
        "posts exist only inside their place. step outside the radius and they disappear. nothing follows you home."
    ),
    OnboardStep(
        "03 / 04", "you are invisible by default.",
        "no passive tracking. no last seen. you flip presence on when you want to be findable — and only then."
    ),
    OnboardStep(
        "04 / 04", "nothing persists.",
        "no archive, no inbox, no profile to maintain. moments expire. that is the point."
    ),
)

// ── Internal phase enum ───────────────────────────────────────────────────────
private enum class OnboardPhase { INFO, EMAIL, VERIFY, HANDLE }

// ── Simple email sanity check (full validation happens server-side) ────────────
private fun String.looksLikeEmail() = contains('@') && lastIndexOf('.') > indexOf('@') + 1

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
) {

    var phase       by remember { mutableStateOf(OnboardPhase.INFO) }
    var step        by remember { mutableIntStateOf(0) }
    var email       by remember { mutableStateOf("") }
    var code        by remember { mutableStateOf("") }
    var handle      by remember { mutableStateOf("") }
    val handleFocus = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 28.dp),
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Spacer(Modifier.height(14.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Mono("· here", size = 10.sp, color = EmberFg, letterSpacing = 0.36.sp)
            Mono("v0.1 · SKOPJE", size = 9.sp, color = EmberMuted, letterSpacing = 0.18.sp)
        }

        // ── Body ─────────────────────────────────────────────────────────────
        // BoxWithConstraints captures the available height so the inner Column
        // can use heightIn(min = …) to enable Arrangement.Center while also
        // becoming scrollable when the keyboard shrinks the viewport.
        BoxWithConstraints(
            modifier        = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val minBodyHeight = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = minBodyHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                AnimatedContent(
                    targetState   = phase,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label         = "onboardPhase",
                ) { currentPhase ->
                    when (currentPhase) {

                        OnboardPhase.INFO -> {
                            AnimatedContent(
                                targetState   = step,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label         = "onboardStep",
                            ) { s ->
                                InfoStepBody(STEPS[s], s)
                            }
                        }

                        OnboardPhase.EMAIL -> {
                            AuthEmailStep(
                                email         = email,
                                onEmailChange = { email = it },
                            )
                        }

                        OnboardPhase.VERIFY -> {
                            AuthVerifyStep(
                                email        = email,
                                code         = code,
                                onCodeChange = { code = it },
                            )
                        }

                        OnboardPhase.HANDLE -> {
                            LaunchedEffect(Unit) { handleFocus.requestFocus() }
                            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                                Mono(
                                    "SET HANDLE",
                                    size          = 10.sp,
                                    color         = EmberMuted,
                                    letterSpacing = 0.32.sp,
                                )
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
                                    style = TextStyle(
                                        fontFamily = JetBrainsMono,
                                        fontSize   = 13.sp,
                                        lineHeight = 20.sp,
                                        color      = EmberMuted,
                                    ),
                                )
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text  = "@",
                                        style = TextStyle(
                                            fontFamily = JetBrainsMono,
                                            fontSize   = 20.sp,
                                            color      = EmberMuted,
                                        ),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    BasicTextField(
                                        value         = handle,
                                        onValueChange = { handle = it.replace(" ", "").take(16) },
                                        modifier      = Modifier
                                            .weight(1f)
                                            .focusRequester(handleFocus),
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
                            }
                        }
                    }
                }
            }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        // Lives outside the scroll region so it is always visible above the keyboard.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // Progress bars: 4 info + 1 auth + 1 handle = 6 segments
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                STEPS.forEachIndexed { i, _ ->
                    val filled = when (phase) {
                        OnboardPhase.INFO -> i <= step
                        else              -> true
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (filled) EmberFg else EmberFg.copy(alpha = 0.18f)),
                    )
                }
                // Auth bar (EMAIL + VERIFY share one segment)
                val authColor = when (phase) {
                    OnboardPhase.EMAIL, OnboardPhase.VERIFY -> EmberAccent
                    OnboardPhase.HANDLE                     -> EmberFg
                    else                                    -> EmberFg.copy(alpha = 0.18f)
                }
                Box(Modifier.weight(1f).height(2.dp).background(authColor))
                // Handle bar
                val handleColor = if (phase == OnboardPhase.HANDLE) EmberAccent else EmberFg.copy(alpha = 0.18f)
                Box(Modifier.weight(1f).height(2.dp).background(handleColor))
            }

            // Navigation buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when (phase) {
                    OnboardPhase.INFO -> {
                        if (step > 0) PxButton("BACK", onClick = { step-- })
                        if (step < STEPS.lastIndex) {
                            PxButton(
                                "CONTINUE",
                                onClick  = { step++ },
                                modifier = Modifier.weight(1f),
                                primary  = true,
                            )
                        } else {
                            PxButton(
                                "SIGN IN  →",
                                onClick  = { phase = OnboardPhase.EMAIL },
                                modifier = Modifier.weight(1f),
                                primary  = true,
                            )
                        }
                    }

                    OnboardPhase.EMAIL -> {
                        val valid = email.looksLikeEmail()
                        PxButton("BACK", onClick = {
                            phase = OnboardPhase.INFO
                            step  = STEPS.lastIndex
                        })
                        PxButton(
                            text     = "SEND CODE  →",
                            onClick  = { if (valid) phase = OnboardPhase.VERIFY },
                            modifier = Modifier.weight(1f),
                            primary  = valid,
                        )
                    }

                    OnboardPhase.VERIFY -> {
                        PxButton("BACK", onClick = {
                            phase = OnboardPhase.EMAIL
                            code  = ""
                        })
                        PxButton(
                            text     = "VERIFY  →",
                            onClick  = { if (code.length == 6) phase = OnboardPhase.HANDLE },
                            modifier = Modifier.weight(1f),
                            primary  = code.length == 6,
                        )
                    }

                    OnboardPhase.HANDLE -> {
                        PxButton(
                            text     = if (handle.isNotBlank()) "ENTER AS @${handle.uppercase()}" else "ENTER ANONYMOUSLY",
                            onClick  = { viewModel.onDone(handle) },
                            modifier = Modifier.weight(1f),
                            primary  = true,
                        )
                    }
                }
            }

            // Context note — updates per phase
            Mono(
                text = when (phase) {
                    OnboardPhase.INFO   -> "PRIVACY-FIRST  ·  EPHEMERAL BY DESIGN"
                    OnboardPhase.EMAIL  -> "ONE-TIME CODE  ·  NO PASSWORD  ·  NO TRACKING"
                    OnboardPhase.VERIFY -> "CHECK SPAM IF CODE DOESN'T ARRIVE  ·  MOCK: ANY 6 DIGITS"
                    OnboardPhase.HANDLE -> "YOU CAN CHANGE THIS LATER  ·  FRIENDS FIND YOU BY HANDLE"
                },
                size          = 9.sp,
                color         = EmberMuted,
                letterSpacing = 0.14.sp,
                modifier      = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}