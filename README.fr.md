# Immich Swipe

[English version here](README.md)

Immich Swipe est une application Android open-source conçue pour faciliter le tri de vos photos et vidéos hébergées sur votre serveur [Immich](https://immich.app/).

Inspirée par l'application [Sponge](https://get-sponge.com/) (concept similaire pour les fichiers locaux), elle vous permet de trier rapidement vos médias par de simples gestes de balayage (swipe), avec une interface fluide et moderne.

> **Note** : Ce projet est né d'un besoin personnel et a été développé avec l'aide de l'IA. Bien que je ne sois pas développeur de métier, j'accorde une grande importance à la stabilité et à l'expérience utilisateur.
>
> **Avertissement** : Ce projet est indépendant et n'est affilié d'aucune façon avec le projet officiel Immich.

## 📸 Aperçu

|                                       Écran d'accueil                                        |                                    La pile de tri                                    |                                      Mode Revue                                       |
|:--------------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------:|:-------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/Light_HomeScreen_GridView.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Light_SwipeScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Light_ReviewScreen.png" width="200"> |
|                                    *Parcourez vos albums*                                    |                                *Swipez pour décider*                                 |                             *Vérifiez avant de supprimer*                             |

<details>
<summary>🌙 <b>Voir la galerie en Mode Sombre</b></summary>

|                                       Écran d'accueil                                       |                                   La pile de tri                                    |                                      Mode Revue                                      |                                       Paramètres                                       |
|:-------------------------------------------------------------------------------------------:|:-----------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------:|:--------------------------------------------------------------------------------------:|
| <img src="metadata/en-US/images/phoneScreenshots/Dark_HomeScreen_GridView.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Dark_SwipeScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Dark_ReviewScreen.png" width="200"> | <img src="metadata/en-US/images/phoneScreenshots/Dark_SettingsScreen.png" width="200"> |

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
2. Entrez votre clé API Immich.
   - **Permissions nécessaires** : Pour le bon fonctionnement de l'application, votre clé API doit avoir les permissions suivantes :
     - `user.read`
     - `album.read`
     - `asset.read`
     - `asset.delete`
3. Sélectionnez un album et commencez à trier !

## 🛠️ Build

Si vous souhaitez compiler l'application vous-même :

- **JDK 17** ou supérieur requis.
- **Android Studio** (Version Ladybug ou plus récente recommandée).
- Clonez le dépôt et importez le projet dans Android Studio.
- Utilisez `./gradlew assembleDebug` pour générer un APK de test.

## 📄 Licence

Ce projet est sous licence GNU GPL v3. Voir le fichier [LICENSE](LICENSE) pour plus de détails.
