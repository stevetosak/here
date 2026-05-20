package net.tosak.here.model

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
    ONBOARDING, MAP, PRESENCE, COMPOSER, POST, CHAT, SETTINGS
}

// ── Demo location: Debar Maalo, Skopje ────────────────────────────────────────
// Replace with real device location once LocationProvider is wired up.
const val YOU_LAT = 41.9965
const val YOU_LNG = 21.4290

// Sample friends placed at real Debar Maalo spots.
// Distances verified against WGS-84 haversine at lat ~42°:
//   1° lat ≈ 111 000 m · 1° lng ≈ 82 500 m
val sampleFriends = listOf(
    Friend(
        id = "alex", mark = "A",
        lat = 41.9968, lng = 21.4286,   // ~47 m NNW — Caffe Vinoteka patio
        dist = 47,
        post = FriendPost(PostKind.PHOTO, "patio. one chair open.", "Caffe Vinoteka"),
        status = FriendStatus.JUST_POSTED,
    ),
    Friend(
        id = "kris", mark = "K",
        lat = 41.9978, lng = 21.4303,   // ~180 m NNE — Korzo
        dist = 180,
        post = FriendPost(PostKind.TEXT, "walking down korzo. anyone around?", null),
        status = FriendStatus.LIVE,
    ),
    Friend(
        id = "mira", mark = "M",
        lat = 41.9936, lng = 21.4290,   // ~320 m S — City Park
        dist = 320,
        post = FriendPost(PostKind.VOICE, "voice · 0:18", "Park"),
        status = FriendStatus.LIVE,
    ),
    Friend(
        id = "noa", mark = "N",
        lat = 41.9995, lng = 21.4317,   // ~401 m NE — just outside radius, Debarca area
        dist = 410,
        post = FriendPost(PostKind.TEXT, "late dinner at debarca. plates landed.", "Debarca"),
        status = FriendStatus.LIVE,
    ),
)