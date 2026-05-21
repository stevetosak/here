package net.tosak.here.ui

import androidx.activity.compose.BackHandler
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
import androidx.hilt.navigation.compose.hiltViewModel
import net.tosak.here.auth.AuthViewModel
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
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    // ── Navigation stack ───────────────────────────────────────────────────────
    // Seed from auth state: authenticated users go straight to MAP; fresh
    // installs (or after sign-out) start at ONBOARDING.
    // ONBOARDING is cleared from the stack on completion so users can never
    // swipe back into it.  Any push/pop triggers recomposition via SnapshotStateList.
    val navStack = remember {
        mutableStateListOf(
            if (authViewModel.isAuthenticated.value) AppScreen.MAP else AppScreen.ONBOARDING
        )
    }
    val currentScreen = navStack.last()

    fun navigate(s: AppScreen) { navStack.add(s) }
    fun goBack() { if (navStack.size > 1) navStack.removeLast() }

    // System back button — disabled at the root (MAP / ONBOARDING)
    BackHandler(enabled = navStack.size > 1) { goBack() }

    // React to sign-out: reset the entire stack to ONBOARDING
    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            navStack.clear()
            navStack.add(AppScreen.ONBOARDING)
        }
    }

    // Handle is seeded from the saved session on authenticated starts
    var handle       by remember { mutableStateOf(authViewModel.savedHandle) }
    var presenceOn   by remember { mutableStateOf(false) }
    var activeFriend by remember { mutableStateOf(sampleFriends[0]) }
    var chatSeed     by remember { mutableStateOf<String?>(null) }
    var showPing     by remember { mutableStateOf(false) }
    var toast        by remember { mutableStateOf<String?>(null) }

    // Scenario: empty | cluster | ping (mirror of design prototype)
    var scenario     by remember { mutableStateOf("cluster") }
    val friendsVisible = scenario == "cluster" || scenario == "ping"

    LaunchedEffect(scenario, currentScreen) {
        if (scenario == "ping" && currentScreen == AppScreen.MAP) showPing = true
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
            targetState   = currentScreen,
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
                        navStack.clear()
                        navStack.add(AppScreen.MAP)
                        flashToast("READY · YOU ARE INVISIBLE")
                    },
                )
                AppScreen.MAP -> MapScreen(
                    presenceOn     = presenceOn,
                    friendsVisible = friendsVisible,
                    onActivate     = { navigate(AppScreen.PRESENCE) },
                    onCompose      = { navigate(AppScreen.COMPOSER) },
                    onFriend       = { f ->
                        activeFriend = f
                        navigate(AppScreen.POST)
                        showPing     = false
                    },
                    onSettings     = { navigate(AppScreen.SETTINGS) },
                )
                AppScreen.PRESENCE -> PresenceScreen(
                    currentlyOn = presenceOn,
                    onActivated = { newOn ->
                        presenceOn = newOn
                        goBack()
                        flashToast(if (newOn) "PRESENCE ON · 400M · 2H" else "PRESENCE OFF · NOTHING REMAINS")
                    },
                    onCancel    = { goBack() },
                )
                AppScreen.COMPOSER -> ComposerScreen(
                    onClose  = { goBack() },
                    onSubmit = { _, _ ->
                        goBack()
                        flashToast("POSTED · EXPIRES 2H")
                    },
                )
                AppScreen.POST -> PostViewScreen(
                    friend  = activeFriend,
                    onClose = { goBack() },
                    onChat  = { seed ->
                        chatSeed = seed
                        navigate(AppScreen.CHAT)
                    },
                )
                AppScreen.CHAT -> ChatScreen(
                    friend     = activeFriend,
                    seedReply  = chatSeed,
                    onClose    = { goBack() },
                )
                AppScreen.SETTINGS -> SettingsScreen(
                    handle    = handle,
                    onClose   = { goBack() },
                    onSignOut = { authViewModel.signOut() },
                )
            }
        }

        // Ping overlay — on top of everything
        if (showPing && currentScreen == AppScreen.MAP) {
            PingOverlay(
                friend    = sampleFriends[0],
                onSee     = {
                    showPing     = false
                    activeFriend = sampleFriends[0]
                    navigate(AppScreen.POST)
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