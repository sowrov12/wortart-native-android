# WortArt Native Android — starter

This is a separate Kotlin + Jetpack Compose Android project based on the visual direction of the WortArt web app.

## Important scope
This is a starter prototype, not yet a full migration of the private/current web app. It currently contains:
- Native Compose navigation (Practice, Explore, Stats, Premium)
- Sample vocabulary cards
- `der / die / das` answer buttons and simple local counters
- GitHub Actions workflow to build a debug APK in the cloud

Not yet migrated: the full Google Sheet vocabulary feed, category artwork, Firebase sign-in/admin/premium logic, custom decks, saved progress, audio playback, reminders, and production-grade offline database.

## Build APK using GitHub from an Android phone
1. Create a **new** GitHub repository, e.g. `wortart-native-android`. Do not overwrite the existing `wortart` web repository.
2. Upload all files/folders in this project to the new repository, preserving their paths.
3. Open the repository's **Actions** tab and enable Actions if prompted.
4. Run **Build WortArt Native APK** using **Run workflow**, or push a commit to `main`.
5. Open the completed workflow run, go to **Artifacts**, and download `WortArt-Native-debug-apk`.
6. Extract the downloaded artifact ZIP on your phone and install `app-debug.apk`. Android may ask you to allow installs from that browser/file manager.

## Notes
- Package name retained from the web app note: `com.sowrov.germanarticlebangla`.
- The app is independent of the current Vercel website.
- Debug APK is for testing/sideloading. A release APK/AAB should be signed securely before public distribution.
- A GitHub Actions build may need dependency/plugin version adjustments if upstream Android Gradle Plugin versions change.
