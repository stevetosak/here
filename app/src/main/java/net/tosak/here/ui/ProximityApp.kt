package net.tosak.here.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tosak.here.model.*
import net.tosak.here.screens.ChatScreen
import net.tosak.here.screens.ComposerScreen
import net.tosak.here.screens.mapscreen.MapScreen
import net.tosak.here.screens.PingOverlay
import net.tosak.here.screens.PostViewScreen
import net.tosak.here.screens.PresenceScreen
import net.tosak.here.screens.SettingsScreen
import net.tosak.here.screens.onboarding.OnboardingScreen
import net.tosak.here.ui.theme.*

@Composable
fun ProximityApp() {
    var screen       by remember { mutableStateOf(AppScreen.ONBOARDING) }
    var presenceOn   by remember { mutableStateOf(false) }
    var handle       by remember { mutableStateOf("you") }
    var activeFriend by remember { mutableStateOf(sampleFriends[0]) }
    var chatSeed     by remember { mutableStateOf<String?>(null) }
    var showPing     by remember { mutableStateOf(false) }
    var toast        by remember { mutableStateOf<String?>(null) }

    // Scenario: empty | cluster | ping (mirror of design prototype)
    var scenario     by remember { mutableStateOf("cluster") }
    val friendsVisible = scenario == "cluster" || scenario == "ping"

    LaunchedEffect(scenario, screen) {
        if (scenario == "ping" && screen == AppScreen.MAP) showPing = true
        else if (scenario != "ping") showPing = false
    }

    // Toast auto-dismiss
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(2200)
            toast = null
        }
    }

    fun flashToast(msg: String) { toast = msg }

    Box(modifier = Modifier.fillMaxSize().background(EmberBg).imePadding()) {
        AnimatedContent(
            targetState   = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label         = "screenTransition",
        ) { s ->
            when (s) {
                AppScreen.ONBOARDING -> OnboardingScreen(
                    onDone = { name ->
                        handle = name
                        screen = AppScreen.MAP
                        flashToast("READY · YOU ARE INVISIBLE")
                    },
                )
                AppScreen.MAP -> MapScreen(
                    presenceOn     = presenceOn,
                    friendsVisible = friendsVisible,
                    friends        = sampleFriends,
                    onActivate     = { screen = AppScreen.PRESENCE },
                    onCompose      = { screen = AppScreen.COMPOSER },
                    onFriend       = { f ->
                        activeFriend = f
                        screen       = AppScreen.POST
                        showPing     = false
                    },
                    onSettings     = { screen = AppScreen.SETTINGS },
                )
                AppScreen.PRESENCE -> PresenceScreen(
                    currentlyOn = presenceOn,
                    onActivated = { newOn ->
                        presenceOn = newOn
                        screen     = AppScreen.MAP
                        flashToast(if (newOn) "PRESENCE ON · 400M · 2H" else "PRESENCE OFF · NOTHING REMAINS")
                    },
                    onCancel    = { screen = AppScreen.MAP },
                )
                AppScreen.COMPOSER -> ComposerScreen(
                    onClose  = { screen = AppScreen.MAP },
                    onSubmit = { _, _ ->
                        screen = AppScreen.MAP
                        flashToast("POSTED · EXPIRES 2H")
                    },
                )
                AppScreen.POST -> PostViewScreen(
                    friend  = activeFriend,
                    onClose = { screen = AppScreen.MAP },
                    onChat  = { seed ->
                        chatSeed = seed
                        screen   = AppScreen.CHAT
                    },
                )
                AppScreen.CHAT -> ChatScreen(
                    friend     = activeFriend,
                    seedReply  = chatSeed,
                    onClose    = { screen = AppScreen.MAP },
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    handle  = handle,
                    onClose = { screen = AppScreen.MAP },
                )
            }
        }

        // Ping overlay — on top of everything
        if (showPing && screen == AppScreen.MAP) {
            PingOverlay(
                friend    = sampleFriends[0],
                onSee     = {
                    showPing     = false
                    activeFriend = sampleFriends[0]
                    screen       = AppScreen.POST
                },
                onDismiss = { showPing = false },
            )
        }

        // Toast
        AnimatedVisibility(
            visible = toast != null,
            enter   = fadeIn() + slideInVertically { -it },
            exit    = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp),
        ) {
            toast?.let {
                Text(
                    text     = it,
                    modifier = Modifier
                        .background(EmberBg)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    style    = TextStyle(
                        fontFamily    = JetBrainsMono,
                        fontSize      = 10.sp,
                        color         = EmberFg,
                        letterSpacing = 0.22.sp,
                    ),
                )
            }
        }
    }
}