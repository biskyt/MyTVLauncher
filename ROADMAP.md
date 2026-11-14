# Roadmap

MyTVLauncher is a light home screen for Android TV and Google TV, with no ads, no tracking and no analytics. The
owner approved the plan and the mockups on 2026-11-14 (https://claude.ai/artifact/MvR1kkXVpH8ZqfS375XNQK, layout B).

## Devices
- **Philips 48OLED806**, Android TV 11 (API 30): the main device.
- **Chromecast with Google TV** (Android 12 or 14): the second device. Google TV gives the Home button to its own
  launcher, so the owner turns that off with adb (see RELEASING.md).

## Releases
1. **0.1.0:** the project, the theme from MyPlexPlayer, Home with every app, the home app set-up, Android settings,
   Settings › Home app, the accent colour, and the Watch Next check with the Home key test.
2. **0.2.0:** the Continue watching row, above the apps.
   - Up to 20 items, newest first, from `TvContractCompat.WatchNextPrograms` (permission `READ_TV_LISTINGS`).
   - A poster card for an item with a 2:3 picture, else a wide 16:9 card, with a progress bar and the app's icon.
   - Hold OK: open the app, or hide the item (kept here only).
   - Settings › Home rows: the row on or off (off reads nothing), and a tick for each app.
   - No effect on start-up time: Home draws from the saved list; reads run in the background after the first frame,
     debounced (500 ms), one at a time; pictures load at card size, only for cards on screen.
3. **0.3.0:** hide, show and move apps (hold OK on an app: Move, Hide from Home, App info), and the All apps screen.
4. **0.4.0:** app channel rows (with their switches), the clock, and the network icon with the VPN badge.
5. **0.5.0:** backup and restore, as MyPlexPlayer's: one plain JSON file with a fixed name, permissions only when
   used, also accepted by Open with / Share.
6. **0.6.0, low priority:** wake the speaker when the TV leaves standby, with about 1 s of silent sound, once for
   each wake, only when nothing plays, after an HDMI or ARC output appears or a delay set in Settings. Off by default.
   Dropped if the TV needs large workarounds.

## Remote shortcuts (after the Home key test)
- Android never gives the Home key to an app. A launcher gets only "go home" for a short press. A held press and a
  double press are set by the TV maker.
- **Press Home twice:** if both presses reach the launcher, it times them and opens its own recent apps panel (from
  `UsageStatsManager`, with usage access that the owner gives once). If the TV opens its own recent apps, keep that.
- **Hold Home for All apps:** if the TV does nothing useful with a held press, the only way is an optional
  accessibility service that reads only the Home key. The owner decides after the test.

## Later
- Posters from MyTVPlayer and MyPlexPlayer: they publish their Watch Next items with poster art and a 2:3 ratio.
