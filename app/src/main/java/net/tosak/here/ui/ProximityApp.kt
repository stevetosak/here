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
import net.tosak.here.shared.model.*
import net.tosak.here.screens.chat.ChatScreen
import net.tosak.here.screens.composer.ComposerScreen
import net.tosak.here.screens.composer.components.PostPhotoScreen
import net.tosak.here.screens.mapscreen.MapScreen
import net.tosak.here.screens.ping.PingOverlay
import net.tosak.here.screens.post.OwnPostScreen
import net.tosak.here.screens.post.PostViewScreen
import net.tosak.here.screens.presence.PresenceScreen
import net.tosak.here.screens.presence.viewmodel.PresenceViewModel
import net.tosak.here.screens.settings.SettingsScreen
import net.tosak.here.screens.handshake.HandshakeScreen
import net.tosak.here.screens.handshake.MementoScreen
import net.tosak.here.screens.onboarding.OnboardingScreen
import net.tosak.here.shared.navigation.NavigationViewModel
import net.tosak.here.ui.theme.*

@Composable
fun ProximityApp() {
    val nav:             NavigationViewModel = hiltViewModel()
    val presenceViewModel: PresenceViewModel = hiltViewModel()

    // ── Shell-level state ─────────────────────────────────────────────────────
    val activeFriend   by nav.activeFriend.collectAsStateWithLifecycle()
    val chatSeed       by nav.chatSeed.collectAsStateWithLifecycle()
    val pendingMemento by nav.pendingMemento.collectAsStateWithLifecycle()
    val presenceOn     by presenceViewModel.presenceOn.collectAsStateWithLifecycle()
    val toast          by nav.toast.collectAsStateWithLifecycle()

    // System back button — disabled at the root (MAP / ONBOARDING)
    BackHandler(enabled = nav.backStack.size > 1) { nav.goBack() }

    // ── Demo scenario state ───────────────────────────────────────────────────
    var scenario       by remember { mutableStateOf("cluster") }
    val friendsVisible = scenario == "cluster" || scenario == "ping"
    var showPing       by remember { mutableStateOf(false) }

    LaunchedEffect(scenario, nav.current) {
        if (scenario == "ping" && nav.current == AppScreen.MAP) showPing = true
        else if (scenario != "ping") showPing = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(EmberBg)
            .imePadding(),
    ) {
        AnimatedContent(
            targetState  = nav.current,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label        = "screenTransition",
        ) { s ->
            when (s) {
                AppScreen.ONBOARDING -> OnboardingScreen()

                AppScreen.MAP -> MapScreen(
                    presenceOn     = presenceOn,
                    friendsVisible = friendsVisible,
                )

                AppScreen.PRESENCE -> PresenceScreen()

                // ── Composer flow (in-progress refactor — callbacks retained) ──
                AppScreen.COMPOSER -> ComposerScreen(
                    onClose  = { nav.goBack() },
                    onSubmit = { _, _ -> nav.goBack() },
                    onPhotoComposerSelected = { nav.navigate(AppScreen.POST_PHOTO) },
                )

                AppScreen.POST_PHOTO -> PostPhotoScreen()

                AppScreen.POST -> activeFriend?.let { PostViewScreen(friend = it) }

                AppScreen.OWN_POST -> OwnPostScreen()

                AppScreen.CHAT -> activeFriend?.let { f ->
                    ChatScreen(friend = f, seedReply = chatSeed)
                }

                AppScreen.SETTINGS -> SettingsScreen()

                AppScreen.HANDSHAKE -> HandshakeScreen()

                AppScreen.MEMENTO -> pendingMemento?.let { MementoScreen(memento = it) }
            }
        }

        // Ping overlay — on top of everything (demo only)
        if (showPing && nav.current == AppScreen.MAP) {
            PingOverlay(
                friend    = sampleFriends[0],
                onSee     = {
                    showPing = false
                    nav.navigate(AppScreen.POST)
                },
                onDismiss = { showPing = false },
            )
        }

        // Toast banner — driven by Event.Toast via NavigationViewModel
        AnimatedVisibility(
            visible  = toast != null,
            enter    = fadeIn() + slideInVertically { -it },
            exit     = fadeOut() + slideOutVertically { -it },
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
