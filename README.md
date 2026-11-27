# PlayReward Android App (`com.playreward.app`)

Complete User Panel built with Kotlin, Jetpack Compose (Material 3), Firebase Authentication, and Firebase Realtime Database (Spark Free Plan compatible), styled with the soft lavender, indigo-violet, and amber-coral design language.

## Build via GitHub Actions (No Local Phone Gradle Needed)

1. **Extract in Termux** (copy from Download to `~/` first so execute permissions work):
   ```bash
   cp /storage/emulated/0/Download/PlayReward.zip ~/
   cd ~ && unzip PlayReward.zip -d PlayReward && cd PlayReward
   ```
2. **Add Firebase Config (`google-services.json`)**:
   - Download `google-services.json` for package `com.playreward.app` from Firebase Console (Enable **Authentication** -> Email/Password + Google, and **Realtime Database**).
   - Place it at `app/google-services.json` (or base64-encode it into GitHub Secret `GOOGLE_SERVICES_JSON_BASE64`).
   - Update `app/src/main/res/values/strings.xml` -> `default_web_client_id` with your Firebase Web Client ID for Google Sign-In.
3. **Import Firebase Spark RTDB Rules & Seed**:
   - In Firebase Console -> Realtime Database -> Rules: paste `firebase-rtdb-rules.json`.
   - In Realtime Database -> Data -> Import JSON: import `firebase-rtdb-seed.json`.
4. **Add Signing Keystore Secrets (Recommended for Release APKs)**:
   Generate once in Termux (or let GitHub Actions use a temporary CI keystore until you add yours):
   ```bash
   keytool -genkeypair -v -keystore playreward-release.jks -alias playreward -keyalg RSA -keysize 2048 -validity 10000
   base64 -w 0 playreward-release.jks
   ```
   Add GitHub Repository Secrets:
   - `SIGNING_KEYSTORE_BASE64`
   - `KEYSTORE_PASSWORD`
   - `KEY_ALIAS`
   - `KEY_PASSWORD`
5. **Push to GitHub & Build**:
   ```bash
   git init && git add . && git commit -m "Initial PlayReward User Panel"
   gh repo create mdshahiid09-bit/PlayReward --private --source=. --push
   ```
   GitHub Actions (`.github/workflows/build.yml`) will automatically build both **Debug** and **Signed Release** APKs.
6. **Trigger Forced In-App Update**:
   Bump `versionCode` & `versionName` in `app/build.gradle.kts`, push a tag like `git tag v1.0.1 && git push origin v1.0.1` — GitHub Actions publishes a GitHub Release with the APK, and all installed PlayReward apps immediately show the Forced Update screen with `DownloadManager` progress & installer.
