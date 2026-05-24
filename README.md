# StockSocial

Android app for sharing stock-market insights and accessing live financial data. Users publish finance-focused posts, follow other investors, manage a portfolio and watchlist, and browse live quotes, charts, and news from **Finnhub**.

> **Not** an Instagram-style photo social network — the focus is stocks, investing, and financial discussion.

---

## Features

| Area | What it does |
|------|----------------|
| **Feed** | View posts from all users; like, comment, share; search users or tickers |
| **Posts** | Create posts with text + optional image/video; edit or delete your own |
| **Profiles** | Edit name & avatar; view your posts; open other users' public profiles |
| **Follow** | Follow / unfollow users; follower counts on profiles |
| **Portfolio** | Track holdings, add/remove positions, view performance |
| **Hot Stocks** | Browse stocks by category (Tech, Banking, Crypto, …) with live quotes |
| **Stock Details** | Price, chart, volume, P/E, EPS, market cap, analyst recommendations |
| **Articles** | Company news from Finnhub REST API |
| **Auth** | Email/password registration, Google Sign-In, auto-login, logout |

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| Architecture | **MVVM** |
| UI | Fragments, ViewBinding, **Material Design 3** (dark theme) |
| Navigation | **Navigation Component** + Safe Args (`nav_graph.xml`) |
| Reactive UI | ViewModel, LiveData, StateFlow |
| Local cache | **Room** (SQLite) — posts & articles |
| Backend | **Firebase** Auth, Firestore, Storage |
| External API | **Finnhub** REST via Retrofit + OkHttp |
| Async | Kotlin Coroutines |
| Images | Glide + local filesystem cache |

---

## Project Structure

```
app/src/main/java/com/stocksocial/
├── ui/              # Fragments & adapters (Feed, Profile, Stocks, Auth, …)
├── viewmodel/       # ViewModels + AppViewModelFactory
├── repository/      # Data layer (Firebase, Finnhub, Room)
├── model/           # Domain models & Room entities
├── data/
│   ├── local/       # AppDatabase, PostDao, ArticleDao
│   └── remote/      # Firestore mappers
├── network/         # Retrofit ApiService
└── utils/           # Constants, image cache, helpers
```

---

## Screens & Navigation

**Start:** Welcome → Login / Register → Feed

**Bottom navigation:** Feed · Portfolio · Hot Stocks · Profile

**Secondary screens:** Create Post, Post Details, User Profile, Stock Details, Articles

Navigation is defined in `app/src/main/res/navigation/nav_graph.xml`.

---

## Local Database (Room)

Database file: `stocksocial.db` (version 4)

| Table | Purpose |
|-------|---------|
| `posts` | Cached feed posts (author, content, likes, stock symbol, image paths, …) |
| `articles` | Cached Finnhub news articles |

User profiles and portfolio data live in **Firestore**, not Room. Firebase local persistence is explicitly disabled; Room is the on-device cache.

---

## Setup

### 1. Clone & open

```bash
git clone https://github.com/ofirrazz/Stocking.git
cd Stocking
```

Open the project in **Android Studio**.

### 2. `local.properties`

Create `local.properties` in the project root (git-ignored):

```properties
sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
FINNHUB_TOKEN=your_finnhub_api_key
```

Get a free API key at [finnhub.io](https://finnhub.io/).

### 3. Firebase

1. Firebase Console → Project Settings → Android app (`com.stocksocial`)
2. Download `google-services.json`
3. Place it at: `app/google-services.json`

This file is git-ignored. Without it the app builds, but auth and cloud features will not work.

### 4. Build & run

```bash
./gradlew :app:assembleDebug
```

Or use **Run** in Android Studio on an emulator or device (API 24+).

---

## External API (Finnhub)

Base URL: `https://finnhub.io/api/v1/`

| Endpoint | Used for |
|----------|----------|
| `GET /company-news` | Financial news |
| `GET /search` | Stock symbol search |
| `GET /quote` | Live price & volume |
| `GET /stock/recommendation` | Analyst ratings |
| `GET /stock/candle` | Price chart data |

---

## Firebase Collections

| Path | Contents |
|------|----------|
| `users/{uid}` | Profile (username, displayName, photoUrl, …) |
| `users/{uid}/followers`, `/following` | Follow graph |
| `users/{uid}/portfolio/{symbol}` | Holdings |
| `posts/{postId}` | Posts + like/comment counts |
| `posts/{postId}/comments/{id}` | Comments |

Images and videos are stored in **Firebase Storage**.

---

## Requirements Checklist (Course Project)

- User registration & login (Firebase Authentication)
- Share content (text + image) visible to other users
- Edit & delete own posts; profile screen with name & photo editing
- Auto-login on next app open; logout
- External REST API content (Finnhub)
- MVVM, ViewModel, LiveData, Room, Navigation Graph
- No synchronous network calls; loading indicators where appropriate
- Material Design UI
- Local object caching (Room) + image caching (filesystem) — not Firebase as local store

---

## License

Academic / course project.
