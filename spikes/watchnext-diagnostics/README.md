# Watch Next diagnostics

A small app that shows why some apps' pictures do not show in MyTVLauncher (the owner, 2026-11-15: Plex's
"Recommendations from Plex" row had no pictures).

It reads the TV provider's Watch Next items, channels and channel items, as MyTVLauncher does. For each picture it:
- tries to open it, and shows the result (the picture's type and size, or the error);
- for a `content://` picture, shows the provider's app and settings, and whether each home app on the TV (and
  MyTVLauncher) has permission to read it.

The report shows on screen (Up and Down scroll it a page at a time) and is saved to
`Download/MyTVLauncher-diagnostics.txt`. Every query value in an address is hidden, so no server token is in it.

Build it from the repository's root:

```
./gradlew -p spikes/watchnext-diagnostics assembleDebug
```

The APK is `spikes/watchnext-diagnostics/app/build/outputs/apk/debug/app-debug.apk`. It installs beside MyTVLauncher
(its own app id, `com.biskyt.mytvlauncher.diag`).
