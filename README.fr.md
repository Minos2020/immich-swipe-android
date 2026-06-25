# Immich Swipe

![GitHub release (latest by date)](https://img.shields.io/github/v/release/minos2020/immich-swipe)
![GitHub License](https://img.shields.io/github/license/minos2020/immich-swipe)
![Kotlin](https://img.shields.io/badge/langage-Kotlin-purple)
![Android](https://img.shields.io/badge/plateforme-Android-green)

[English version here](README.md)

Immich Swipe est une application Android open-source conçue pour faciliter le tri de vos photos et vidéos hébergées sur votre serveur [Immich](https://immich.app/).

Inspirée par l'application [Sponge](https://play.google.com/store/apps/details?id=com.prismtree.sponge&pcampaignid=web_share) (concept similaire pour les fichiers locaux), elle vous permet de trier rapidement vos médias par de simples gestes de balayage (swipe), avec une interface fluide et moderne.

> **Note** : Ce projet est né d'un besoin personnel et a été développé avec l'aide de l'IA. Bien que je ne sois pas développeur de métier, j'accorde une grande importance à la stabilité et à l'expérience utilisateur.
>
> **Avertissement** : Ce projet est indépendant et n'est affilié d'aucune façon avec le projet officiel Immich.

## 📸 Aperçu

|                                         Écran d'accueil                                         |                                     La pile de tri                                      |                                        Mode Revue                                        |
|:-----------------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|:----------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/01_Light_HomeScreen_GridView.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/02_Light_SwipeScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/03_Light_ReviewScreen.png" width="200"> |
|                                     *Parcourez vos albums*                                      |                                  *Swipez pour décider*                                  |                              *Vérifiez avant de supprimer*                               |

<details>
<summary>🌙 <b>Voir la galerie en Mode Sombre</b></summary>

|                                        Écran d'accueil                                         |                                     La pile de tri                                     |                                       Mode Revue                                        |                                       Paramètres                                       |
|:----------------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|:---------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/05_Dark_HomeScreen_GridView.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/07_Dark_SwipeScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/08_Dark_ReviewScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Dark_SettingsScreen.png" width="200"> |

</details>

## ✨ Fonctionnalités

- **🚀 Pile de tri Rapide** : Swipez à droite pour garder, à gauche pour supprimer. Le bandeau déroulant en haut vous donne un retour visuel direct sur vos choix et les médias à venir.
- **🕒 Snooze (SKIP)** : Un média vous fait hésiter ? Passez-le (SKIP). Il réapparaîtra automatiquement dans votre pile après un délai que vous configurez (ex: 1 jour, 1 mois, ou jamais).
- **🛡️ Synchronisation différée** : Immich Swipe ne supprime rien sans votre accord final. Vos choix sont stockés localement, et vous déclenchez la véritable suppression sur le serveur Immich quand vous êtes prêt via le **Mode Revue**.
- **💾 Sauvegarde des sessions** : Quittez l'appli et revenez plus tard : votre progression dans chaque album est sauvegardée.
- **📂 Gestion Multi-Albums** : L'application gère parfaitement les médias présents dans plusieurs albums à la fois.
- **🚦 Diagnostic de Connexion** : Un indicateur visuel (Vert/Orange/Rouge) vous informe en temps réel de l'état de la connexion à votre serveur.
- **🎨 Interface Moderne** : Développée avec Jetpack Compose et Material Design 3, supportant les thèmes Clair et Sombre.

## ⚙️ Configuration

1. Entrez l'URL de votre serveur Immich (ex: `https://immich.votre-domaine.fr`).
2. Entrez votre clé API Immich (si vous n'en avez pas, [créez en une](https://my.immich.app/user-settings?isOpen=api-keys))
   - **Permissions nécessaires** : Pour le bon fonctionnement de l'application, votre clé API doit avoir les permissions suivantes :
     - `user.read`
     - `album.read`
     - `asset.read`
     - `asset.view`
     - `asset.delete`
     - `userProfileImage.read` (Optionnel)
3. Sélectionnez un album et commencez à trier !

## 📦 Installation

  |                                                                                                                       **Obtainium**                                                                                                                        |   **Téléchargement Direct**   |  **IzzyOnDroid / F-Droid**    |
  |:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|:-------------------------:|:----------------------------:|
  | [<img src="https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png"  alt="Get it on Obtainium" height="50">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22%3A%22com.minos2020.immichswipe%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2FMinos2020%2Fimmich-swipe%22%2C%22author%22%3A%22Minos2020%22%2C%22name%22%3A%22Immich%20Swipe%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Atrue%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22github-creds%5C%22%3A%5C%22%5C%22%2C%5C%22GHReqPrefix%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3A%22GitHub%22%7D) | Récupérez le dernier APK dans la section [Releases](https://github.com/minos2020/immich-swipe/releases)    |  Prochainement disponible


## 🛠️ Build

Si vous souhaitez compiler l'application vous-même :

- **JDK 17** ou supérieur requis.
- **Android Studio** (Version Ladybug ou plus récente recommandée).
- Clonez le dépôt et importez le projet dans Android Studio.
- Synchroniser Gradle
- Utilisez `./gradlew assembleDebug` pour générer un APK de test.

## 📄 Licence

Ce projet est sous licence GNU GPL v3. Voir le fichier [LICENSE](LICENSE) pour plus de détails.
