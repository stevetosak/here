package net.tosak.here.screens.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.tosak.here.screens.friends.viewmodel.FriendRow
import net.tosak.here.screens.friends.viewmodel.FriendsListViewModel
import net.tosak.here.shared.components.HudStrip
import net.tosak.here.shared.components.Mono
import net.tosak.here.shared.components.PxButton
import net.tosak.here.shared.components.Rule
import net.tosak.here.shared.storage.DmMessageEntity
import net.tosak.here.ui.theme.*

@Composable
fun FriendsListScreen(
    viewModel: FriendsListViewModel = hiltViewModel(),
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EmberBg),
    ) {
        HudStrip(presenceOn = true, minimal = true)

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 52.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top,
        ) {
            Column {
                Mono("FRIENDS", size = 10.sp, color = EmberMuted, letterSpacing = 0.3.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${rows.size} known",
                    style = TextStyle(fontFamily = JetBrainsMono, fontSize = 22.sp, color = EmberFg),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Mono("PERSISTENT", size = 9.sp, color = EmberMuted)
                Spacer(Modifier.height(4.dp))
                Mono("DMS ONLY", size = 9.sp, color = EmberMuted)
            }
        }

        Rule()

        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Mono("NO FRIENDS YET.", size = 10.sp, color = EmberMuted, letterSpacing = 0.2.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(rows, key = { it.friend.id }) { row ->
                    FriendListRow(row, onClick = { viewModel.onFriend(row.friend) })
                    Rule()
                }
            }
        }

        // Bottom back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            PxButton("← MAP", onClick = viewModel::onBack)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FriendListRow(row: FriendRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick           = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "@${row.friend.id}",
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 16.sp, color = EmberFg),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                previewText(row.lastMessage),
                style = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, color = EmberMuted, lineHeight = 14.sp),
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Mono(
                text  = if (row.friend.dist > 0) "${row.friend.dist}M" else "—",
                size  = 9.sp,
                color = EmberMuted,
            )
            Spacer(Modifier.height(4.dp))
            Mono(relativeTime(row.lastMessage?.sentAt), size = 8.sp, color = EmberMuted)
        }
    }
}

private fun previewText(m: DmMessageEntity?): String {
    if (m == null) return "no messages yet"
    val prefix = if (m.fromMe) "you: " else ""
    val body = m.text ?: "[image]"
    return prefix + body
}

private fun relativeTime(ts: Long?): String {
    if (ts == null) return ""
    val diff = System.currentTimeMillis() - ts
    val minute = 60_000L
    val hour   = 60 * minute
    val day    = 24 * hour
    return when {
        diff < minute     -> "now"
        diff < hour       -> "${diff / minute}m"
        diff < day        -> "${diff / hour}h"
        diff < 7 * day    -> "${diff / day}d"
        else              -> "${diff / (7 * day)}w"
    }
}
