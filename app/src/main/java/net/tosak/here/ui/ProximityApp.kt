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
import net.tosak.here.screens.dm.DmChatScreen
import net.tosak.here.screens.friendprofile.FriendProfileScreen
import net.tosak.here.screens.friends.FriendsListScreen
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
import net.tosak.here.shared.ping.PingShellViewModel
import net.tosak.here.ui.theme.*

@Composable
fun ProximityApp() {
    val nav:             NavigationViewModel = hiltViewModel()
    val presenceViewModel: PresenceViewModel = hiltViewModel()
    val pingShell:       PingShellViewModel = hiltViewModel()

    // ── Shell-level state ─────────────────────────────────────────────────────
    val activeFriend   by nav.activeFriend.collectAsStateWithLifecycle()
    val chatSeed       by nav.chatSeed.collectAsStateWithLifecycle()
    val pendingMemento by nav.pendingMemento.collectAsStateWithLifecycle()
    val presenceOn     by presenceViewModel.presenceOn.collectAsStateWithLifecycle()
    val toast          by nav.toast.collectAsStateWithLifecycle()
    val incomingPing   by pingShell.incomingPing.collectAsStateWithLifecycle()

    // System back button — disabled at the root (MAP / ONBOARDING)
    BackHandler(enabled = nav.backStack.size > 1) { nav.goBack() }

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
                    friendsVisible = presenceOn,
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

                AppScreen.FRIENDS -> FriendsListScreen()

                AppScreen.DM -> DmChatScreen()

                AppScreen.FRIEND_PROFILE -> FriendProfileScreen()
            }
        }

        // Incoming ping overlay — on top of everything, driven by PingEngine
        incomingPing?.let { ping ->
            PingOverlay(
                incoming  = ping,
                onOnMyWay = pingShell::onOnMyWay,
                onIgnore  = pingShell::onIgnore,
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
