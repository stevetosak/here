# Backend Architecture — here

> **Scope**: v1, single-city rollout. Decisions marked **(revisit at scale)** are consciously deferred.

---

## Design Principles

- **Presence-scoped**: the entire location pipeline runs only inside an explicit session. Nothing phone-homes when the user is offline.
- **Privacy-first**: exact coordinates are never written to disk. They live in Redis for the duration of a session and nowhere else.
- **Ephemeral by default**: posts expire on session end; location state expires on disconnect.
- **Simple over clever**: Redis Geo + a single WebSocket server is enough for a city. No geo-sharding, no Kafka, no microservices at v1.

---

## Presence State Machine

```
OFFLINE ──(go live)──► LIVE ──(go offline)──► OFFLINE
                          │
                          └──(disconnect / 5min TTL)──► OFFLINE
```

Every component in this document is scoped to the `LIVE` state. On transition to `OFFLINE`, all ephemeral state is torn down.

---

## Location Transport: WebSocket

### Why WebSocket

The app needs two things simultaneously: upload location from client, and receive proximity events from the server. WebSocket is the natural fit — one persistent connection handles both directions. It also maps cleanly to the presence model: the socket opens on go-live and closes on go-offline.

Polling would require a separate push channel for arrivals anyway. MQTT adds operational complexity that isn't justified at v1.

### Connection

```
wss://api.here.app/ws/presence
Authorization: Bearer <token>          ← in the HTTP Upgrade request headers
```

The server authenticates the token before accepting the upgrade. Invalid or missing token → `401`, connection rejected.

### Update trigger (client-side)

Use Android's `FusedLocationProviderClient` with a `LocationRequest` configured for:
- **Displacement threshold**: 30m — update fires when the user moves meaningfully, not on GPS jitter
- **Heartbeat**: if no displacement update in 30s, send a heartbeat anyway to reset the server-side TTL

This is the right battery/latency tradeoff: a stationary user generates one packet per 30s; a moving user generates updates proportional to actual movement.

### Message Protocol

```jsonc
// ── Client → Server ──────────────────────────────────────────────────────

{ "type": "location_update", "lat": 41.9973, "lng": 21.4316 }

{ "type": "heartbeat" }   // sent every 30s if no location update fired


// ── Server → Client ──────────────────────────────────────────────────────

// Sent once immediately after connection is established
{ "type": "snapshot", "friends": [
    { "id": "alex", "dist": 47 },
    { "id": "sam",  "dist": 90 }
]}

// Emitted whenever a friend crosses into 400m radius
{ "type": "friend_arrived", "id": "alex", "dist": 47 }

// Emitted whenever a friend drops out of 400m radius or goes offline
{ "type": "friend_left", "id": "alex" }

// Forwarded from the pings pipeline (manual or auto)
{ "type": "ping_received", "from": "alex", "message": "heading your way", "ts": 1748441600000 }
```

---

## Proximity Engine

### Redis Geo

All live users are stored in a single Redis geospatial sorted set keyed `geo:live`.

| Operation | Redis command |
|---|---|
| User goes live / location update | `GEOADD geo:live <lng> <lat> <userId>` |
| User goes offline (graceful) | `ZREM geo:live <userId>` |
| Query who is within 400m | `GEOSEARCH geo:live FROMMEMBER <userId> BYRADIUS 400 m ASC` |
| Backup expiry (crash/kill) | background sweep removes entries not updated in 5min |

No coordinates ever leave Redis for the database. Redis is the entire location store.

### Neighbor Diffing

On every `location_update` or `heartbeat` from user **A**:

```
1.  GEOADD geo:live lat lng A               // upsert A's position
2.  raw  ← GEOSEARCH geo:live FROMMEMBER A BYRADIUS 400m
3.  cur  ← filter raw by A's friend list   // only friends matter
4.  prev ← GET presence:neighbors:{A}      // who was in range on the last update

    // What changed since last update?
5.  arrivals   = cur − prev                // in range now, weren't before
6.  departures = prev − cur               // were in range, aren't now

    // Persist the new baseline for next diff
7.  SET presence:neighbors:{A} = cur  (TTL: 5min)

    // Notify A — A's map needs to update
8.  push friend_arrived(B) to A  for each B in arrivals
9.  push friend_left(B)    to A  for each B in departures

    // Notify each B — B is stationary and will never detect A's movement on their own update
10. push friend_arrived(A) to B  for each B in arrivals   (only if A ∈ B's friend list)
11. push friend_left(A)    to B  for each B in departures
```

### Friend List Caching

Friend lists are read on every update. Cache them in Redis as a `SET` per user:

- Key: `friends:{userId}`
- TTL: 60s
- On miss: load from PostgreSQL, populate cache
- On unfriend: `SREM friends:{userId} <targetId>` immediately (don't wait for TTL)

### Initial Snapshot

On WS connection (user just went live), run steps 2–3 above and send the `snapshot` message before processing any location updates. This populates the UI immediately without waiting for a location update to arrive.

---

## Data Storage

| Data | Store | Retention |
|---|---|---|
| Live user positions | Redis `geo:live` | Removed on disconnect; 5min backup TTL |
| Neighbor state | Redis `presence:neighbors:{id}` | 5min TTL |
| Friend list cache | Redis `friends:{id}` | 60s TTL |
| Active WS connection map | Server memory (or Redis `SET ws:live`) | Session duration |
| Friends | PostgreSQL | Persistent |
| Posts | PostgreSQL | Deleted on session end or explicit action |
| Auth tokens | PostgreSQL | Until sign-out |
| Location history | — | **Not stored** |

---

## Infrastructure (v1)

```
Android client
      │  wss (port 443)
      ▼
┌─────────────┐
│  Load Balancer  │  ← sticky sessions (by userId cookie or IP hash)
└──────┬──────┘
       │
┌──────▼──────┐       ┌───────────┐
│  WS Server  │──────►│   Redis   │  geo:live, neighbors, friend cache
│  (1–2 inst) │       └───────────┘
└──────┬──────┘
       │
┌──────▼──────┐
│ PostgreSQL  │  friends, posts, auth tokens
└─────────────┘
```

**WS Server**: stateful (holds open sockets). Sticky sessions on the load balancer ensure a user's socket always lands on the same instance. With 2 instances, cross-instance proximity events need a small Redis pub/sub channel: instance A publishes `friend_arrived` for user B; instance B (holding B's socket) receives it and forwards to the client.

**(revisit at scale)**: At higher concurrency, replace the WS server with a horizontally-scaled cluster backed by Redis Streams for event fan-out.

---

