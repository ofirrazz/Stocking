# StockSocial

A social-network Android app for retail investors. Share insights, track favorite tickers,
view live quotes and analyst recommendations from Finnhub, and read curated market articles.

## Features

- **Authentication** — Email/password and Google Sign-In (Firebase Auth).
- **Feed** — Posts with text, image, video, likes and comments. Per-user ownership rules.
- **Profile** — Public profile with stats (posts, likes, followers, following), follow / unfollow.
- **Portfolio** — Per-user holdings stored in Firestore with live P&L from Finnhub.
- **Hot stocks** — Search, favorites, and trending tickers with live quotes.
- **Stock details** — Live price, day stats, analyst recommendations, related posts.
- **Articles** — Cached market articles with offline support via Room.
- **Offline-first** — Posts and articles cache locally with Room.

## Tech stack

| Layer | Library |
|-------|---------|
| Language | Kotlin |
| Architecture | MVVM (`ViewModel` + `LiveData` + `Repository`) |
| Async | Coroutines |
| Networking | Retrofit + OkHttp |
| Backend | Firebase (Auth, Firestore, Storage, Analytics) |
| Local cache | Room |
| Navigation | Jetpack Navigation Component |
| UI | Material 3, ViewBinding, Glide |
| Splash | `androidx.core:core-splashscreen` |

## Project layout

```
app/src/main/java/com/stocksocial
├── data/local       # Room database + DAOs + prefs
├── model            # Domain models + cache entities
├── network          # Retrofit API + DTOs + mappers
├── repository       # Repository classes (single source of truth)
├── ui
│   ├── adapters     # RecyclerView adapters
│   ├── articles     # Articles screens
│   ├── auth         # Welcome / Login / Register
│   ├── feed         # Feed screen
│   ├── hotstocks    # Hot stocks screen
│   ├── main         # MainActivity
│   ├── post         # Create / details / comments
│   ├── profile      # Profile + Portfolio + Public profile
│   └── stocks       # Stock search + details
├── utils            # AppContainer (DI), helpers, constants
└── viewmodel        # ViewModels + factory
```

## Setup

### 1. `local.properties`

In the project root, create `local.properties`:

```properties
sdk.dir=/Users/<your-user>/Library/Android/sdk
FINNHUB_TOKEN=your_finnhub_token
```

Get a free Finnhub token at https://finnhub.io.
`local.properties` is git-ignored.

### 2. Firebase

1. Firebase Console → Project Settings → Android app (`com.stocksocial`).
2. Add the debug SHA-1 fingerprint of every developer machine
   (`./gradlew signingReport` → `SHA1` from variant `debug`).
3. Enable **Email/Password** and **Google** providers under Authentication.
4. Download `google-services.json` and place it at `app/google-services.json`
   (the file is git-ignored).
5. Publish `firestore.rules` and `storage.rules` from this repo to your project.

### 3. Build

```bash
./gradlew :app:assembleDebug
```

If `google-services.json` is missing the project still builds, but Firebase-backed
flows are disabled at runtime.

## Security rules

- `firestore.rules`
  - **`users/{uid}`** — public read (required so the registration screen can verify
    username uniqueness before the user exists in Firebase Auth, and so login-by-username
    can resolve `username -> email`). Writes are restricted to the document owner.
    If you don't need login-by-username you can tighten this to `signedIn()`.
  - **`posts/{postId}`** — author can do anything; any signed-in user can update
    only the like/comment counters (`likesCount`, `likedUserIds`, `lastLikedBy`,
    `commentsCount`). Comments are owner-write under `posts/{postId}/comments`.
  - **Subcollections** (`followers`, `following`, `portfolio`, `favoriteSymbols`,
    `notifications`) — owner-only writes; reads are signed-in only.
- `storage.rules` — per-user folders for `posts_images/{uid}`, `posts_videos/{uid}`
  and `profile_images/{uid}`.
- `firestore.indexes.json` documents the composite indexes that complex queries
  (filter + sort) require. Publish them via Firebase CLI or create them when the
  app prints the "requires an index" log on first run.

## Testing

Pure-JVM unit tests live in `app/src/test`. Run them with:

```bash
./gradlew test
```

Current coverage:

- `TickerParserTest` — `$AAPL` extraction, multi-ticker, length boundaries, case.
- `PortfolioHoldingTest` — invested value, current value, P&L absolute and percent.

## Run checklist

See [`RUN_CHECKLIST.md`](./RUN_CHECKLIST.md) for end-to-end test scenarios used
before demo and submission.

## Course requirements coverage (group of 2)

- Login + Register + auto-login + logout (incl. **Forgot password** via Firebase Auth)
- Social interactions — posts (text/image/video), **like + unlike**, comments, follow
- Profile + image upload (display name editable; `@username` is immutable post-registration)
- REST API integration with Finnhub (quotes, candles, recommendations, search)
- Local cache (Room) with **offline mode** — Feed and Articles fall back to cache
- Firebase Auth, Firestore and Storage
- Custom theme + Material 3 + splash screen (`androidx.core:core-splashscreen`)
- Unit tests under `app/src/test` (JUnit 4)
