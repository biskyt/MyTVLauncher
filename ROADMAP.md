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
3. **0.3.0 (done):** hide, show and move apps (hold OK on an app: Move, Hide from Home, App info), the All apps screen (a
   top bar button and a tile at the end of the apps row), and the recent apps panel on a double Home press.
4. **0.4.0 (done):** app channel rows (with their switches), the clock, and the network icon with the VPN badge.
   - **0.4.1 (done):** the owner's points on 0.4.0: Home's row order in Settings, every app's channels (grouped by
     app), half-size channel cards, rows that do not jump on Up and Down, Back to a row's first card, a moved app kept
     in view, and Move in All apps.
5. **0.5.0 (done):** backup and restore, as MyPlexPlayer's: one plain JSON file with a fixed name, permissions only when
   used, also accepted by Open with / Share.
6. **0.6.0 (done), low priority:** wake the speaker when the TV leaves standby, with about 1 s of silent sound, once for
   each wake, only when nothing plays, after a delay set in Settings (0 to 10 s, 2 s by default). Off by default.
   The wait for an HDMI or ARC output to appear was left out: the delay is simpler and the owner can set it.
7. **0.7.0 (done):** the owner's points on 0.6.0 (2026-11-15): pictures over plain HTTP (Plex, Plezy), a focus glow,
   a TV guide button and the Guide key (System, MyTVPlayer or MyPlexPlayer), channel logos shown whole, larger posters
   in channel rows, Open in Play Store, a Left and Right speaker delay, and no Watch Next check.
8. **0.8.0 (done): system notifications** (the owner, 2026-11-15).
   - A setting, off by default. Turning it on asks for notification access (Android's page for it, or the adb command
     where the TV has none: `adb shell cmd notification allow_listener <launcher>/<listener service>`).
   - A bell in the top bar with the number of notifications, shown only when there are some.
   - OK on the bell opens a panel of the notifications (app, title, text). OK opens one; holding OK or Menu
     dismisses it.
   - The launcher follows notifications only while the setting is on and access is given.

## Test results and decisions (the owner, 2026-11-14)
- On the Philips TV, 0.1.0 read 38 Watch Next items and 390 rows of the channels table. That table also holds the TV's
  own broadcast channels, so 0.2.0's check shows app channels and TV channels apart.
- **A held Home press does not reach the launcher** on the Philips TV. No accessibility service: a held Home keeps
  what the TV does.
- **A double Home press does reach the launcher**, with 150 to 350 ms between the presses. In 0.3.0, two presses within
  about 500 ms open the launcher's own recent apps panel (from `UsageStatsManager`, with usage access that the owner
  gives once).
- **All apps** opens from a button in the top bar and from an "All apps" tile at the end of the apps row (0.3.0).

## Later
- Posters from MyTVPlayer and MyPlexPlayer: they publish their Watch Next items with poster art and a 2:3 ratio.
