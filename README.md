# NekoMusic

A modern, cross-platform music player client for Netease Cloud Music, built with **Compose Multiplatform**.

![Banner](https://socialify.git.ci/xiaojiang233/NekoMusic/image?description=1&descriptionEditable=A%20beautiful%20Compose%20Multiplatform%20Music%20Client&font=Inter&language=1&name=1&owner=1&pattern=Solid&theme=Auto)

## ✨ Features

-   **Cross-Platform**: Runs natively on **Android**, **Windows**, **Linux**, and **macOS**.
-   **Modern UI**: Beautiful Material Design 3 interface with dynamic theming support.
-   **Music Playback**:
    -   Support for multiple audio qualities (Standard, High, Lossless, Hi-Res, Dolby Atmos*, etc.).
    -   Lyric display with karaoke-style effects and translation.
    -   Background playback and media notification support.
-   **Content Discovery**:
    -   Daily Recommendations.
    -   Personalized Playlists.
    -   Top/Hot Song Lists.
-   **Search & Library**:
    -   Comprehensive search (Songs, Artists, Albums, Playlists).
    -   User Profile integration (Created/Subscribed playlists).
    -   Artist and Album detail pages.
-   **Customization**:
    -   Theme color customization (Dynamic or Custom Hex).
    -   Dark/Light mode toggle.
    -   Lyric font size and blur styles.

## 🛠️ Tech Stack

-   **Language**: [Kotlin](https://kotlinlang.org/)
-   **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (Jetpack Compose)
-   **Network**: [Ktor](https://ktor.io/) & [Ktor Client](https://ktor.io/docs/client.html)
-   **Image Loading**: [Coil3](https://coil-kt.github.io/coil/)
-   **Audio Engine**:
    -   **Android**: androidx.media3 (ExoPlayer)
    -   **Desktop**: JavaFX Media / Java Sound
-   **State Management**: ViewModel & Kotlin Flow
-   **API Source**: Netease Cloud Music (via direct API implementation)

## 🚀 Getting Started

### Prerequisites

-   JDK 17 or higher.
-   Android Studio (for Android development) or IntelliJ IDEA (for Desktop development).

### Installation

#### Android
Download the latest `.apk` from the [Releases](https://github.com/xiaojiang233/NekoMusic/releases) page.

#### Desktop
Download the installer for your OS (`.msi`, `.deb`, `.rpm`, or `.dmg`) from the [Releases](https://github.com/xiaojiang233/NekoMusic/releases) page.

## 🔨 Building from Source

1.  Clone the repository:
    ```bash
    git clone https://github.com/xiaojiang233/NekoMusic.git
    cd NekoMusic
    ```

2.  Run the Desktop application:
    ```bash
    ./gradlew :composeApp:run
    ```

3.  Build for Android:
    ```bash
    ./gradlew :composeApp:assembleDebug
    ```

4.  Package for Desktop (Distribution):
    ```bash
    ./gradlew :composeApp:packageReleaseDistributionForCurrentOS
    ```

## 🔑 Login & Configuration

NekoMusic supports logging in via Netease Cloud Music account.

1.  **WebView Login**: Use the built-in browser login option in Settings.
2.  **Cookie Login**: If WebView fails (common on some desktop environments), you can manually input your cookie.
    -   Detailed instructions for retrieving cookies are available in the App Settings > Account section.

## 📝 License

This project is licensed under the [MIT License](LICENSE).

## 🙏 Acknowledgements

-   [NeteaseCloudMusicApi](https://github.com/Binaryify/NeteaseCloudMusicApi) for API reference.
-   [JetBrains](https://www.jetbrains.com/) for Kotlin and Compose Multiplatform.
-   [Coil](https://coil-kt.github.io/coil/) for image loading.
-   [Ktor](https://ktor.io/) for networking.
-   [NekoPlayer](https://github.com/xiaojiang233/NekoPlayer)
---

*Note: This is a third-party client and is not affiliated with NetEase Inc.*
