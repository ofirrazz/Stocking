# Run Checklist (Team)

Use this checklist before demo/submission.

## Setup

- [ ] `local.properties` exists and contains `sdk.dir`
- [ ] `FINNHUB_TOKEN` is set in `local.properties`
- [ ] `app/google-services.json` exists
- [ ] Build succeeds with `./gradlew :app:assembleDebug`

## Remote + Social requirements

- [ ] User A registers and logs in
- [ ] User A publishes a post with text + image (mention `$AAPL` to link the ticker)
- [ ] User B logs in and sees User A's post in Feed
- [ ] **User B likes User A's post and the count updates for both users**
- [ ] **User B unlikes the same post and the count goes back down**
- [ ] **User B comments on User A's post and the comment counter updates for both**
- [ ] User A updates own post (text/image)
- [ ] User A deletes own post
- [ ] User B follows User A from the user profile screen, then unfollows

## Auth/Profile requirements

- [ ] Auto-login works after app restart (already authenticated user)
- [ ] Logout works and returns to Login
- [ ] **"Forgot password?" sends a reset email via Firebase Auth**
- [ ] Profile display-name update works (and does NOT change `@username`)
- [ ] Profile image update works
- [ ] Registration blocks a username that is already taken; surfaces a clear error
      when the uniqueness check fails (offline / rules error)

## API + Cache requirements

- [ ] **Articles tab is reachable from the bottom navigation** and loads from REST API
- [ ] Feed and Articles still show cached content when offline after one successful online load
- [ ] Offline mode shows the "Offline mode: showing cached data." notice
- [ ] Stock Details → **"Add to Portfolio"** opens a dialog, saves shares + buy price,
      and the holding appears in the Portfolio tab

## Tests

- [ ] `./gradlew test` runs and all unit tests pass

