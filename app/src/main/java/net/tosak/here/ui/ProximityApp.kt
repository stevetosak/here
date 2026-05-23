package net.tosak.here.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import net.tosak.here.shared.auth.AuthViewModel
import net.tosak.here.shared.model.*
import net.tosak.here.screens.chat.ChatScreen
import net.tosak.here.screens.composer.ComposerScreen
import net.tosak.here.screens.mapscreen.MapScreen
import net.tosak.here.screens.ping.PingOverlay
import net.tosak.here.screens.post.OwnPostScreen
import net.tosak.here.screens.post.PostViewScreen
import net.tosak.here.screens.presence.PresenceScreen
import net.tosak.here.screens.presence.viewmodel.PresenceViewModel
import net.tosak.here.screens.settings.SettingsScreen
import net.tosak.here.screens.handshake.HandshakeScreen
import net.tosak.here.screens.handshake.MementoScreen
import net.tosak.here.screens.handshake.viewmodel.MementoData
import net.tosak.here.screens.onboarding.OnboardingScreen
import net.tosak.here.shared.navigation.NavigationViewModel
import net.tosak.here.ui.theme.*

@Composable
fun ProximityApp() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val nav: NavigationViewModel = hiltViewModel()
    val presenceViewModel: PresenceViewModel = hiltViewModel()

    // System back button — disabled at the root (MAP / ONBOARDING)
    BackHandler(enabled = nav.backStack.size > 1) { nav.goBack() }


    // Handle is seeded from the saved session on authenticated starts
    var handle         by remember { mutableStateOf(authViewModel.savedHandle) }
    val presenceOn     by presenceViewModel.presenceOn.collectAsStateWithLifecycle()
    var activeFriend   by remember { mutableStateOf(sampleFriends[0]) }
    var chatSeed       by remember { mutableStateOf<String?>(null) }
    var showPing       by remember { mutableStateOf(false) }
    var toast          by remember { mutableStateOf<String?>(null) }
    var pendingMemento by remember { mutableStateOf<MementoData?>(null) }

    // Scenario: empty | cluster | ping (mirror of design prototype)
    var scenario     by remember { mutableStateOf("cluster") }
    val friendsVisible = scenario == "cluster" || scenario == "ping"

    LaunchedEffect(scenario, nav.current) {
        if (scenario == "ping" && nav.current == AppScreen.MAP) showPing = true
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

    Box(modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .background(EmberBg).imePadding()) {
        AnimatedContent(
            targetState   = nav.current,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label         = "screenTransition",
        ) { s ->
            when (s) {
                AppScreen.ONBOARDING -> OnboardingScreen(
                    onDone = { name ->
                        handle = name.ifBlank { "you" }
                        // Persist the session so next launch skips onboarding
                        authViewModel.saveSession(handle)
                        // Replace onboarding with MAP as the stack root
                        nav.reset(AppScreen.MAP)
                        flashToast("READY · YOU ARE INVISIBLE")
                    },
                )
                AppScreen.MAP -> MapScreen(
                    presenceOn     = presenceOn,
                    friendsVisible = friendsVisible,
                    onActivate     = { nav.navigate(AppScreen.PRESENCE) },
                    onCompose      = { nav.navigate(AppScreen.COMPOSER) },
                    onFriend       = { f ->
                        activeFriend = f
                        nav.navigate(AppScreen.POST)
                        showPing     = false
                    },
                    onOwnPost      = { nav.navigate(AppScreen.OWN_POST) },
                    onSettings     = { nav.navigate(AppScreen.SETTINGS) },
                    onHandshake    = { nav.navigate(AppScreen.HANDSHAKE) },
                )
                AppScreen.PRESENCE -> PresenceScreen(
                    currentlyOn = presenceOn,
                    onActivated = { newOn ->
                        presenceViewModel.setPresence(newOn)
                        nav.goBack()

                        flashToast(if (newOn) "PRESENCE ON · 400M · 2H" else "PRESENCE OFF · NOTHING REMAINS")
                    },
                    onCancel    = { nav.goBack() },
                )
                AppScreen.COMPOSER -> ComposerScreen(
                    onClose  = { nav.goBack() },
                    onSubmit = { _, _ ->
                        nav.goBack()
                        flashToast("POSTED · EXPIRES 2H")
                    },
                )
                AppScreen.POST -> PostViewScreen(
                    friend  = activeFriend,
                    onClose = { nav.goBack() },
                    onChat  = { seed ->
                        chatSeed = seed
                        nav.navigate(AppScreen.CHAT)
                    },
                )
                AppScreen.OWN_POST -> OwnPostScreen(
                    onClose = { nav.goBack() },
                )
                AppScreen.CHAT -> ChatScreen(
                    friend     = activeFriend,
                    seedReply  = chatSeed,
                    onClose    = { nav.goBack() },
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    handle    = handle,
                    onClose   = { nav.goBack() },
                    onSignOut = { authViewModel.signOut() },
                )
                AppScreen.HANDSHAKE -> HandshakeScreen(
                    onConfirmed = { memento ->
                        pendingMemento = memento
                        // Replace HANDSHAKE with MEMENTO in the stack so back
                        // from Memento returns to MAP, not back to HandshakeScreen.
                        nav.replaceTop(AppScreen.MEMENTO)
                    },
                    onBack = { nav.goBack() },
                )
                AppScreen.MEMENTO -> {
                    val memento = pendingMemento
                    if (memento != null) {
                        MementoScreen(
                            memento    = memento,
                            onContinue = {
                                pendingMemento = null
                                // Pop back to MAP, clearing MEMENTO from the stack
                                nav.reset(AppScreen.MAP)
                                flashToast("CONNECTED · SAVED TO MOMENTS")
                            },
                        )
                    }
                }
            }
        }

        // Ping overlay — on top of everything
        if (showPing && nav.current == AppScreen.MAP) {
            PingOverlay(
                friend    = sampleFriends[0],
                onSee     = {
                    showPing     = false
                    activeFriend = sampleFriends[0]
                    nav.navigate(AppScreen.POST)
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