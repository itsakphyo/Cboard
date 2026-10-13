# Cboard

Cboard is a lightweight Android input method for Burmese Unicode, English QWERTY, symbols, and emoji. The app uses native Java and Android views. It does not require the LatinIME source tree or any file outside this repository.

The package ID remains `com.burmese.keyboard`. A build from this repository can update an installed development build that uses the same signing key.

## Install the included APK

The repository includes [`Cboard.apk`](Cboard.apk). This file is a debug-signed build of version `0.5.0` with version code `13`.

1. Copy `Cboard.apk` to the Android device.
2. Install the APK.
3. Open **Settings > System > Keyboard > On-screen keyboard**.
4. Enable **Burmese Unicode Keyboard**.
5. Select the keyboard from the Android keyboard switcher.

Android accepts an update only when the installed app and the new APK use the same signing key. Uninstall the old development build first if Android reports a signing conflict.

## Build the app

Install these tools:

- Android Studio or the Android command-line tools
- Android SDK Platform 34
- JDK 17 through JDK 22

Set `ANDROID_HOME` or create `local.properties` with the Android SDK path:

```properties
sdk.dir=/absolute/path/to/Android/sdk
```

On macOS or Linux, run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows, run:

```bat
gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Gradle writes the APK to `app/build/outputs/apk/debug/app-debug.apk`.

If the default `java` command is too new for Gradle, use Android Studio's bundled JDK. On macOS:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest lintDebug assembleDebug
```

## Install a local build

Connect a device with USB debugging enabled, then run:

```bash
./gradlew installDebug
```

You can also install the generated APK with Android Debug Bridge:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```text
Cboard/
├── app/
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── assets/emojilist.txt
│       │   ├── java/com/burmese/keyboard/
│       │   ├── res/
│       │   └── AndroidManifest.xml
│       └── test/java/com/burmese/keyboard/
├── gradle/wrapper/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── Cboard.apk
```

The `app/src/main` directory contains all runtime source, resources, and emoji data. The `app/src/test` directory contains the local unit tests. The Gradle wrapper pins the build to Gradle 8.7.
