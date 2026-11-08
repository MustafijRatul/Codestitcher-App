# CodeStitcher Official Android App 📱⚡

[![Release](https://img.shields.io/badge/Release-v1.0.0-black?style=for-the-badge&logo=android)](https://github.com/MustafijRatul/Codestitcher-App/releases/tag/v1.0.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/API-24%2B%20(Android%207.0%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Download APK](https://img.shields.io/badge/Download-APK%20(v1.0.0)-000000?style=for-the-badge&logo=googleplay&logoColor=white)](https://github.com/MustafijRatul/Codestitcher-App/releases/download/v1.0.0/CodeStitcher-v1.0.0.apk)

A native Android application written in **Kotlin** for [CodeStitcher](https://codestitcher-web.github.io/), the modern software suite featuring Calculate, FocusGuard, LambdaScript, and next-generation productivity tools.

---

## ✨ Features

- **Native High-Performance Web Engine**:
  - Full hardware acceleration enabled
  - DOM storage, IndexedDB, and modern web standards enabled
  - Third-party cookies and secure network handling
  - Dynamic JavaScript interface bridge (`window.CodeStitcherNative`) for bi-directional native integration
- **Cybernetic Motion Design & Assets**:
  - **Official CodeStitcher Logo**: Embedded across all density buckets (`mdpi` up to `xxxhdpi`) for standard, rounded, and adaptive squircle launcher icons.
  - **Lottie Cyber Loader**: Real-time rotating and stitching neon particle animation (`lottie_cyber_loader.json`) with smooth fade-out on startup.
  - **Lottie Hologram Radar**: Disconnected pulse animation (`lottie_offline_radar.json`) for the offline state.
  - **Glassmorphic Cards**: Frosted glass dialogs with clean borders (`bg_neon_card.xml`, `bg_splash_gradient.xml`).
  - **Tactile Haptic Feedback**: Sensory micro-vibrations on taps, navigation, fullscreen toggles, and refreshes.
- **Edge-to-Edge & Modern Material 3 UI**:
  - Android 12+ / 14+ SplashScreen API support with branded vector launch animation
  - Support for Day & Night themes matching CodeStitcher's developer aesthetic
  - Dynamic system bar window insets
- **Developer Productivity Controls**:
  - **Fullscreen Mode**: Tap the fullscreen icon to hide system bars and toolbar, maximizing your viewing and working canvas
  - **Desktop Site Toggle**: Switch between mobile viewport and full desktop layout on large phones, foldables, and tablets
  - **Swipe-to-Refresh**: Smooth pull-to-refresh with smart scroll-detection
  - **Horizontal Loading Bar**: Sleek progress bar styled with CodeStitcher brand colors
  - **Native File Chooser**: Upload files, datasets, or media directly from Android device storage
  - **Native File Downloader**: Download exported files, calculations, and documents straight to device `Downloads` folder with system notification
- **Resilience & Connectivity**:
  - Real-time network listener (`ConnectivityManager.NetworkCallback` + Kotlin `StateFlow`)
  - Beautiful offline error view with a retry action
  - Integrated offline Chrome Dino runner game when connection drops
  - Automatic reconnection: when connection is regained, the app auto-restores the session
- **Safe Navigation & Accidental Exit Prevention**:
  - Predictive back navigation handling
  - Exit protection: double-tap back within 2 seconds required on root screen to avoid accidental app closure
  - External links (GitHub, external docs, tutorials) open securely in Chrome Custom Tabs

---

## 🛠️ Project Structure

```
android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/codestitcher/app/
│       │   ├── CodeStitcherApp.kt                  # Application initialization
│       │   ├── MainActivity.kt                     # Main Activity, toolbar, and lifecycle management
│       │   ├── ui/
│       │   │   └── AboutBottomSheetDialog.kt      # Material 3 About Dialog
│       │   ├── utils/
│       │   │   ├── DownloadHelper.kt               # Native DownloadManager integration
│       │   │   └── NetworkHelper.kt                # Real-time network state monitoring
│       │   └── webview/
│       │       ├── CodeStitcherWebChromeClient.kt  # Custom views, file chooser, progress & alerts
│       │       ├── CodeStitcherWebViewClient.kt    # Link routing, SSL handling & error screens
│       │       └── WebAppInterface.kt              # Native JavaScript bridge
│       └── res/
│           ├── drawable/                           # Vector icons & branding graphics
│           ├── layout/                             # Main, offline, and dialog layouts
│           ├── menu/                               # Action bar & overflow menu
│           ├── mipmap-anydpi-v26/                  # Adaptive app launcher icons
│           ├── values/                             # Colors, strings, themes (Light)
│           ├── values-night/                       # Colors, themes (Dark)
│           └── xml/                                # Network security & file provider configs
├── gradle/
│   ├── libs.versions.toml                          # Version catalog
│   └── wrapper/                                    # Gradle wrapper configs
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── local.properties
```

---

## 🚀 How to Build & Run

### Method 1: Android Studio (Recommended)
1. Launch **Android Studio**.
2. Select **Open** and choose this directory: `c:\Users\Ratul\Downloads\android`.
3. Wait for Gradle sync to complete.
4. Select your connected device or Android Emulator.
5. Click **Run ▶** (or press `Shift + F10`).

### Method 2: Command Line (Gradle)
```bash
# Build Debug APK
./gradlew assembleDebug

# Install onto connected device
./gradlew installDebug
```

Output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Permissions & Security
- `INTERNET`: Required for loading CodeStitcher web apps and cloud libraries.
- `ACCESS_NETWORK_STATE`: Used to detect network drops and automatically reconnect.
- `Scoped Storage / DownloadManager`: Safe sandboxed storage for exporting user code files.
- `NetworkSecurityConfig`: Enforces secure HTTPS transport for production endpoints.
