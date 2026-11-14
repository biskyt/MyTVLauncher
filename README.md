# MyTVLauncher

A light home screen for Android TV and Google TV, with no ads, no tracking and no analytics. It has the same look as
[MyTVPlayer](https://github.com/biskyt/MyTVPlayer) and [MyPlexPlayer](https://github.com/biskyt/MyPlexPlayer).

## What it does
- Shows the TV's apps on Home. OK opens an app.
- Opens Android settings, and Android's choice of home app.
- Lets you choose the accent colour.
- Soon: other apps' "Continue watching" (Watch Next) items with posters, app channels, and hidden and moved apps.
  See [ROADMAP.md](ROADMAP.md).

## Install
Download the APK from [Releases](https://github.com/biskyt/MyTVLauncher/releases) and sideload it. To make it the TV's
home app, see [RELEASING.md › Make MyTVLauncher the home app](RELEASING.md#make-mytvlauncher-the-home-app).

## Privacy
MyTVLauncher sends nothing about you anywhere. Its only network use is to load the pictures that other apps give to
Watch Next and their channels. It reads other apps' Watch Next items only with your permission.

## Build
- `./gradlew :app:assembleDebug` builds a debug APK.
- `./gradlew ktlintCheck detekt lintDebug` and `./gradlew :core:test :app:testDebugUnitTest` run the CI checks.

## Licence
GPL-3.0. See [LICENSE](LICENSE).
