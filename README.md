# AtaPotify

**AtaPotify** is a native Android application built with **Kotlin**, **Jetpack Compose**, and **Material Design 3** to manage a shared **Spotify Family Plan** in **LKR** among trusted friends. It removes the monthly hassle of chasing split payments by combining transparent payment tracking, compressed bank-slip verification, automated WhatsApp nudges, and a Spotify listening **Leaderboard** showing who gets the most value per LKR spent.

---

## Purpose

Managing a Spotify Family subscription with friends usually means tracking transfers in spreadsheets, scrolling through chat screenshots, and guessing who still owes what. **AtaPotify** centralizes the entire monthly cycle into a single private app gated by a Google account whitelist:

- **Friends** see their current status (`Pending`, `Verifying`, or `Paid`), upload a compressed transfer receipt in two taps, view past billing months, and connect Spotify to compete on listening minutes and **LKR/hr** value.
- **The Admin** starts new billing months, sets the LKR share per person, verifies or rewinds payment slips, edits billing logs, sends overdue WhatsApp reminders, and distributes APK updates directly via GitHub Releases.

---

## Key Features

### Payment Tracking & Monthly Billing Summary
- **Peer Payment Board:** Real-time Firestore-synced list showing every member's status (`Paid`, `Verifying`, `Pending`) for the active billing month, plus full historical archives in the **History** tab.
- **Monthly Billing Summary Card:** Automatically calculates and displays **Total Collected (LKR)**, **Pending Amounts (LKR)** (combining `Pending` and awaiting-verification entries), **Net Balance (LKR)**, and overall collection progress.
- **Private Slip Upload & Review:** Members attach bank-transfer receipts using the zero-permission Android Photo Picker (`PickVisualMedia`). `ImageCompressor` resizes and encodes the image safely before storing it in a restricted Firestore `private/slip` sub-document readable only by the uploader and Admin.

### WhatsApp Reminders & 7-Day Overdue Queue
- **One-Tap WhatsApp Reminders:** Launch a pre-filled WhatsApp message (`https://wa.me/...`) containing the member's name, billing month, and LKR amount.
- **Automatic Sri Lankan Phone Formatting:** Local `07XXXXXXXX` numbers are normalized to `947XXXXXXXX` (`AtaPotifyConstants.sanitizeWhatsAppDigits`) while saving profiles or whitelist entries.
- **7+ Day Overdue Queue:** Highlights pending members past 7 days with a top-bar badge and opens an **Overdue Auto-Reminder** queue that tracks `reminderCount` and `lastRemindedAt` cooldowns.

### Full Admin Ledger & Whitelist Controls
- **Confirm, Reject, or Rewind:** Verify uploaded slips (`Paid`), return unclear slips to `Pending`, or **Rewind Paid** confirmations back to `Verifying`/`Pending`.
- **Edit Payment Logs:** Update a member's display name, LKR amount, or status, clear an attached receipt, or reset reminder counters.
- **Flexible Month Roster:** Add missing members (including the Admin or whitelist members) to an existing month, remove individual entries, or delete an entire billing month.
- **Whitelist & Rate Config:** Toggle active family members and update the default monthly LKR rate in the **Admin** tab (optionally syncing the active billing month).

### Spotify Listening Leaderboard & Hourly Sync
- **In-App Spotify OAuth 2.0 (PKCE):** Members connect their Spotify account directly via `atapotify://spotify-callback` with no client secret embedded in the APK.
- **Hourly Background Sync (`WorkManager`):** `SpotifyBackgroundSyncWorker` runs periodically every hour on connected networks (plus foreground sync on app resume) to accumulate newly played tracks continuously.
- **4-Week Top Highlights & Value Metric:** Pulls each member's **4-week Top Artist & Top Track** (`short_term`), preserves existing Firestore monthly totals as a baseline across app reinstalls, and ranks everyone (including the Admin) on the **Leaderboard** tab by **Minutes Listened** and **Cost per Hour (LKR/hr)**.

### Direct In-App APK Updates (GitHub Releases)
- Checks the latest release of [`thathsarayasiru97/AtaPotify`](https://github.com/thathsarayasiru97/AtaPotify/releases) via GitHub's API on launch and manually from **My Profile → Check for Updates**.
- Shows release notes plus `.apk` filename/size and opens the download directly with Android's browser/system installer (`Intent.ACTION_VIEW`), avoiding restricted permissions such as `REQUEST_INSTALL_PACKAGES`.

---

## Tech Stack & Project Structure

- **Language & UI:** Kotlin, Jetpack Compose, Material 3 (dark obsidian & emerald palette), Edge-to-Edge, adaptive `NavigationBar` (phones) / `NavigationRail` (tablets & foldables).
- **Architecture:** MVVM + Repository pattern (`ViewModel`, `StateFlow`, Kotlin Coroutines & Flow).
- **Backend:** Firebase Authentication (Google Sign-In via Credential Manager) & Cloud Firestore with zero-trust `firestore.rules`.
- **Background Jobs:** AndroidX `WorkManager` (`PeriodicWorkRequestBuilder`).
- **Spotify Integration:** Spotify Web API + OAuth 2.0 Authorization Code Flow with PKCE.

```text
├── app/
│   ├── build.gradle.kts              # Dependencies, Secrets plugin, signing & SDK config
│   ├── google-services.json          # Firebase client configuration (per project)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Minimal INTERNET permission & Spotify deep link
│       │   ├── res/values/
│       │   │   ├── firebase_applet_config.xml  # Named Firestore database ID
│       │   │   └── strings.xml       # App strings & accessibility labels
│       │   └── java/com/example/
│       │       ├── MainActivity.kt
│       │       ├── data/
│       │       │   ├── model/        # AtaPotifyModels.kt & billing summary math
│       │       │   ├── repository/   # AuthRepository & PaymentRepository
│       │       │   ├── service/      # SpotifyStatsService, AppUpdateService, ImageCompressor
│       │       │   └── worker/       # SpotifyBackgroundSyncWorker (WorkManager)
│       │       └── ui/
│       │           ├── screens/      # AuthScreens, BoardScreen, HistoryScreen, AdminScreen, SheetsAndDialogs
│       │           ├── theme/        # Color, Type, Theme
│       │           └── viewmodel/    # AuthGateViewModel & AtaPotifyViewModel
│       └── test/                     # JUnit & Robolectric unit/rule tests
├── firestore.rules                   # Zero-trust Firestore Security Rules
├── firestore.test.js                 # Node emulator security rule tests
├── gradle/libs.versions.toml         # Version catalog
└── .env.example                      # Required Secrets / environment variable template
```

---

## Setup Instructions

### Prerequisites
- **Android Studio** (supporting AGP `9.1.1`, Kotlin `2.2.10`, `compileSdk = 36`, `minSdk = 24`)
- **JDK 11** or newer
- A **Firebase project** with Authentication and Cloud Firestore enabled
- A **Spotify Developer** application (for the Listening Leaderboard)
- *(Optional)* **Firebase CLI** + **Node.js** if you want to run Firestore Security Rules tests locally

### 1. Clone the Repository
```bash
git clone https://github.com/thathsarayasiru97/AtaPotify.git
cd AtaPotify
```

### 2. Firebase & Cloud Firestore Setup
1. Create or open a project in the [Firebase Console](https://console.firebase.google.com/).
2. Register an Android app with package name **`com.aistudio.spotsplit.kvwmpx`** (or update `applicationId` in `app/build.gradle.kts` to match your own Android app).
3. Download **`google-services.json`** from Firebase Console and place it at:
   ```text
   app/google-services.json
   ```
4. Enable **Cloud Firestore** and verify the database ID in `app/src/main/res/values/firebase_applet_config.xml`:
   ```xml
   <resources>
       <string name="firestore_database_id" translatable="false">YOUR_FIRESTORE_DATABASE_ID</string>
   </resources>
   ```
   *(If you use the default Firestore database instead of a named instance, ensure `firebase_applet_config.xml` and `firebase.json` match your database ID.)*
5. Configure the **Admin email** if deploying your own fork:
   - Update `ADMIN_EMAIL` in `app/src/main/java/com/example/data/model/AtaPotifyModels.kt`.
   - Update `isBootstrappedAdmin()` in `firestore.rules`.
6. Deploy the Firestore Security Rules:
   ```bash
   firebase deploy --only firestore:rules
   ```

### 3. Google Sign-In Setup (Credential Manager)
AtaPotify uses Android **Credential Manager** (`GetGoogleIdOption` for silent auto sign-in and `GetSignInWithGoogleOption` for explicit login) paired with `Firebase.auth.signInWithCredential(...)`:

1. In **Firebase Console → Authentication → Sign-in method**, enable **Google**.
2. In **Firebase Console → Project Settings → Your apps (Android)**, add the **SHA-1** and **SHA-256** fingerprints for both your debug keystore and release upload keystore:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   *(If building with the repo's `debugConfig` signing config, inspect `${rootDir}/debug.keystore` instead.)*
3. Re-download `google-services.json` after adding SHA certificates so the Google Services plugin injects `R.string.default_web_client_id` automatically at build time.
4. Security rules require verified Google emails (`request.auth.token.email_verified == true`), and only the Admin plus users added to the `whitelist` collection are permitted past the access gate.

### 4. Spotify Developer Setup (Listening Stats)
AtaPotify reads `SPOTIFY_CLIENT_ID` via the **Secrets Gradle Plugin** (`.env` locally or the Secrets panel in AI Studio):

1. Open the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) and create/select your app.
2. Under **Redirect URIs**, add:
   ```text
   atapotify://spotify-callback
   ```
3. Under **User Management** (required while the Spotify app is in Development Mode), add the Spotify account emails of every family member.
4. Create a `.env` file in the repository root based on `.env.example` (never commit `.env`):
   ```ini
   SPOTIFY_CLIENT_ID=your_spotify_client_id_here
   ```

---

## Building & Running Tests

### Build the App
Using Gradle installed locally (or `./gradlew` if you generate/use a wrapper locally):
```bash
# Build a debug APK
gradle :app:assembleDebug

# Compile Kotlin & verify resources cleanly
gradle :app:compileDebugKotlin
```
The generated debug APK will be located under `app/build/outputs/apk/debug/`.

### Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```
Includes unit coverage for:
- `MonthlyBillingSummaryTest` — collected, pending, verifying, and net balance LKR calculations.
- `AppUpdateServiceTest` — GitHub release SemVer comparisons and APK size formatting.
- `AtaPotifyRepositoryRuleTest` — Firestore security rule & repository integration tests (requires local Auth/Firestore emulators when running emulator-backed suites).

### Run Firestore Security Rules Tests (Node.js)
With the Firebase Emulator Suite running (`auth` on `9099` and `firestore` on `8085`):
```bash
firebase emulators:exec --only auth,firestore "node --test firestore.test.js"
```

---

## Publishing In-App APK Updates (GitHub Releases)

When shipping a new fix or feature directly to family members:

1. Bump **`versionCode`** and **`versionName`** in `app/build.gradle.kts`:
   ```kotlin
   defaultConfig {
       versionCode = 2
       versionName = "1.1"
   }
   ```
2. Build a signed APK using the **same keystore** as previous releases so Android allows an in-place upgrade without uninstalling first.
3. Publish a **GitHub Release** on [`thathsarayasiru97/AtaPotify`](https://github.com/thathsarayasiru97/AtaPotify/releases):
   - Set the release tag to match `versionName` prefixed with `v` (e.g., `v1.1`).
   - Write user-facing release notes in the description (displayed inside AtaPotify's update dialog).
   - Attach the `.apk` file as a release asset and publish as the latest (non-draft) release.

### Automated APK Releases (GitHub Actions)
The repository includes **`.github/workflows/release-apk.yml`**, which automatically runs `:app:testDebugUnitTest`, builds the APK, and uploads `AtaPotify-v<versionName>.apk` to **GitHub Releases** whenever you push to `main` or push a `v*` tag:

1. In your GitHub repository, open **Settings → Secrets and variables → Actions** and add:
   - `GOOGLE_SERVICES_JSON` — paste the contents of your `app/google-services.json` (or `GOOGLE_SERVICES_JSON_BASE64` if base64-encoded), unless `app/google-services.json` is already committed.
   - `SPOTIFY_CLIENT_ID` — your Spotify Developer Client ID.
   - *(Recommended for production signing)* `UPLOAD_KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` matching `my-upload-key.jks` (`upload` alias). If omitted, the workflow falls back cleanly to `debug.keystore.base64` (or generates a temporary `debug.keystore` via `keytool`).
2. Whenever you want friends to receive an in-app update prompt, bump `versionCode` + `versionName` in `app/build.gradle.kts` and push to `main` (or push tag `v<versionName>`). GitHub Actions creates/updates the release and attaches the latest `.apk` automatically.

---

## Contributing

Contributions, bug fixes, and feature improvements are welcome:

1. **Fork** the repository and create a descriptive feature branch:
   ```bash
   git checkout -b feat/your-feature-name
   ```
2. **Follow project conventions:**
   - Keep UI in Jetpack Compose following Material 3 tokens from `ui/theme/Theme.kt` (no hardcoded color hex values inside composables).
   - Store user-facing copy in `app/src/main/res/values/strings.xml`, provide `contentDescription` for icons/images, keep touch targets at least `48.dp` (`minimumInteractiveComponentSize()`), and add `Modifier.testTag(...)` to primary interactive elements.
   - Never commit `.env`, `google-services.json` changes containing private tokens, or keystores/passwords.
   - If you change Firestore document fields or subcollections, update `firestore.rules` and `firestore.test.js` together.
3. **Verify before opening a PR:**
   - Confirm `gradle :app:assembleDebug` and `gradle :app:testDebugUnitTest` pass.
   - Run `node --test firestore.test.js` against the Firebase emulator if rules changed.
4. **Open a Pull Request** targeting `main` on `thathsarayasiru97/AtaPotify` with a clear summary of changes and screenshots if UI changed.
