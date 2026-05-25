package net.tosak.here.screens.presence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.tosak.here.screens.presence.viewmodel.PresenceViewModel
import net.tosak.here.shared.components.*
import net.tosak.here.ui.theme.*

@Composable
fun PresenceScreen(
    viewModel: PresenceViewModel = hiltViewModel(),
) {
    val currentlyOn by viewModel.presenceOn.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg)
            .padding(horizontal = 28.dp),
    ) {
        HudStrip(presenceOn = false, minimal = true)

        Spacer(Modifier.height(38.dp))

        // Headline
        Mono(
            text          = if (currentlyOn) "TURN OFF" else "GO LIVE",
            size          = 10.sp,
            color         = EmberMuted,
            letterSpacing = 0.32.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (currentlyOn) {
            Text(
                "leave the radius.",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 28.sp, color = EmberFg, lineHeight = 34.sp),
            )
        } else {
            Text(
                "become findable",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 28.sp, color = EmberFg, lineHeight = 34.sp),
            )
            Text(
                "for the next 2 hours.",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 28.sp, color = EmberMuted, lineHeight = 34.sp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text  = if (currentlyOn)
                "your dot will vanish from every map. content you posted will expire as if you had walked out the door."
            else
                "friends within 400m will see your dot. they can see your posts. nothing leaves this radius.",
            style = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 20.sp, color = EmberMuted),
        )

        // Hold gesture — centred, fills remaining space
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            HoldGesture(onComplete = { viewModel.onActivated(!currentlyOn) })
        }

        // Cancel
        PxButton("CANCEL", onClick = viewModel::onCancel, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(28.dp))
    }
}
