# StockSocial - Setup Notes

This project uses Firebase and Finnhub.

## 1) Local machine setup

Create a local `local.properties` file in the project root:

```properties
sdk.dir=/Users/<your-user>/Library/Android/sdk
FINNHUB_TOKEN=your_finnhub_token
```

`local.properties` is intentionally git-ignored.

## 2) Firebase setup

1. Open Firebase Console -> Project Settings -> Android app (`com.stocksocial`).
2. Download `google-services.json`.
3. Place it at:

`app/google-services.json`

The file is git-ignored by design.

## 3) Build

```bash
./gradlew :app:assembleDebug
```

If `google-services.json` is missing, the app still builds, but Firebase-backed flows are not expected to work.

