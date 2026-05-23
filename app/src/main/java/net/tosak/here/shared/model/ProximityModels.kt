package net.tosak.here.shared.model

data class FriendPost(
    val kind: PostKind,
    val caption: String,
    val place: String?,
)

enum class PostKind { PHOTO, TEXT, VOICE }

data class Friend(
    val id: String,
    val mark: String,
    val lat: Double,     // WGS-84 latitude
    val lng: Double,     // WGS-84 longitude
    val dist: Int,       // metres from current user
    val post: FriendPost,
    val status: FriendStatus,
)

enum class FriendStatus { JUST_POSTED, LIVE }

enum class AppScreen {
    ONBOARDING, MAP, PRESENCE, COMPOSER, POST, CHAT, SETTINGS, HANDSHAKE, MEMENTO
}

// ── Demo anchor — Debar Maalo, Skopje ─────────────────────────────────────────
// Replaced at runtime with the device's real GPS fix; kept here as a fallback.
const val YOU_LAT = 41.9965
const val YOU_LNG = 21.4290

// ── Demo friends expressed as metre-accurate degree offsets ───────────────────
// Storing offsets rather than absolute coords means anchoredSampleFriends()
// can re-place them around whatever location the device reports, so they are
// always inside the 400 m radius during local testing.
//
// 1° lat ≈ 111 000 m  |  1° lng ≈ 82 500 m  (at lat ~42°)
private data class FriendOffset(
    val id: String,
    val mark: String,
    val dLat: Double,   // signed degrees latitude
    val dLng: Double,   // signed degrees longitude
    val dist: Int,      // approximate metres (label only)
    val post: FriendPost,
    val status: FriendStatus,
)

private val SAMPLE_OFFSETS = listOf(
    FriendOffset(
        id = "alex", mark = "A",
        dLat = +0.0003, dLng = -0.0004,
        dist = 47,
        post   = FriendPost(PostKind.PHOTO, "patio. one chair open.", "Caffe Vinoteka"),
        status = FriendStatus.JUST_POSTED,
    ),
    FriendOffset(
        id = "kris", mark = "K",
        dLat = +0.0013, dLng = +0.0013,
        dist = 180,
        post   = FriendPost(PostKind.TEXT, "walking down korzo. anyone around?", null),
        status = FriendStatus.LIVE,
    ),
    FriendOffset(
        id = "mira", mark = "M",
        dLat = -0.0029, dLng = +0.0000,
        dist = 320,
        post   = FriendPost(PostKind.VOICE, "voice · 0:18", "Park"),
        status = FriendStatus.LIVE,
    ),
    FriendOffset(
        id = "noa", mark = "N",
        dLat = +0.0030, dLng = +0.0027,   // ~410 m NE — just outside radius
        dist = 410,
        post   = FriendPost(PostKind.TEXT, "late dinner at debarca. plates landed.", "Debarca"),
        status = FriendStatus.LIVE,
    ),
)

/**
 * Returns the demo friend list anchored to [userLat]/[userLng].
 * Call with the real device location so markers always appear inside
 * the visible 400 m range during development and testing.
 */
fun anchoredSampleFriends(userLat: Double, userLng: Double): List<Friend> =
    SAMPLE_OFFSETS.map { o ->
        Friend(
            id     = o.id,
            mark   = o.mark,
            lat    = userLat + o.dLat,
            lng    = userLng + o.dLng,
            dist   = o.dist,
            post   = o.post,
            status = o.status,
        )
    }

/** Convenience — demo friends anchored to the fallback Skopje coordinates. */
val sampleFriends: List<Friend> = anchoredSampleFriends(YOU_LAT, YOU_LNG)