# Run Checklist (Team)

Use this checklist before demo/submission.

## Setup

- [ ] `local.properties` exists and contains `sdk.dir`
- [ ] `FINNHUB_TOKEN` is set in `local.properties`
- [ ] `app/google-services.json` exists
- [ ] Build succeeds with `./gradlew :app:assembleDebug`

## Remote + Social requirements

- [ ] User A registers and logs in
- [ ] User A publishes a post with text + image
- [ ] User B logs in and sees User A post in Feed
- [ ] User A updates own post (text/image)
- [ ] User A deletes own post

## Auth/Profile requirements

- [ ] Auto-login works after app restart (already authenticated user)
- [ ] Logout works and returns to Login
- [ ] Profile name update works
- [ ] Profile image update works

## API + Cache requirements

- [ ] Articles page loads from REST API when online
- [ ] Feed and Articles still show cached content when offline after one successful online load
- [ ] Offline mode shows the "Offline mode: showing cached data." notice

