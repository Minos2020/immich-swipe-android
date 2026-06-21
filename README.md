# Immich Swipe

[Version française ici](README.fr.md)

Immich Swipe is an open-source Android application designed to make sorting your photos and videos hosted on your [Immich](https://immich.app/) server easy and fun.

Inspired by the [Sponge](https://get-sponge.com/) app (similar concept for local files), it allows you to quickly sort your media using simple swipe gestures, featuring a smooth and modern interface.

> **Note**: This project started as a personal need and was developed with the help of AI. Although I am not a developer by trade, I place great importance on stability and user experience.
>
> **Disclaimer**: This is an independent project and is not affiliated in any way with the official Immich project.

## 📸 Overview

|                                         Home Screen                                          |                                    Sorting Stack                                     |                                      Review Mode                                      |
|:--------------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/Light_HomeScreen_GridView.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Light_SwipeScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Light_ReviewScreen.png" width="200"> |
|                                     *Browse your albums*                                     |                                  *Swipe to decide*                                   |                                *Check before deleting*                                |

## ✨ Features

- **🚀 Fast Sorting Stack**: Swipe right to keep, left to delete. The scrolling banner at the top gives you direct visual feedback on your choices and upcoming media.
- **🕒 Snooze (SKIP)**: Unsure about a photo? Skip it. It will automatically reappear in your stack after a delay you configure (e.g., 1 day, 1 month, or never).
- **🛡️ Deferred Sync**: Immich Swipe doesn't delete anything without your final approval. Your choices are stored locally, and you trigger the actual deletion on the Immich server when you're ready via **Review Mode**.
- **💾 Session Save**: Leave the app and come back later: your progress in each album is saved.
- **📂 Multi-Album Management**: The app perfectly handles media present in multiple albums simultaneously.
- **🚦 Connection Diagnostic**: A visual indicator (Green/Orange/Red) informs you in real-time of the connection status to your server.
- **🎨 Modern Interface**: Developed with Jetpack Compose and Material Design 3, supporting both Light and Dark themes.

## ⚙️ Configuration

1. Enter your Immich server URL (e.g., `https://immich.your-domain.com`).
2. Enter your Immich API Key.
   - **Required Permissions**: For the app to function correctly, your API key must have the following permissions:
     - `user.read`
     - `album.read`
     - `asset.read`
     - `asset.delete`
3. Select an album and start sorting!

## 🛠️ Build

If you want to compile the application yourself:

- **JDK 17** or higher required.
- **Android Studio** (Ladybug version or newer recommended).
- Clone the repository and import the project into Android Studio.
- Use `./gradlew assembleDebug` to generate a test APK.

## 📄 License

This project is licensed under the GNU GPL v3. See the [LICENSE](LICENSE) file for more details.
