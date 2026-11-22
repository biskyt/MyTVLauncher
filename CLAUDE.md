# CLAUDE.md: working on MyTVLauncher

MyTVLauncher is a light home screen (launcher) for Android TV and Google TV, with no ads, no tracking and no
analytics. It is written in Kotlin with Compose for TV and Room.
- Modules: `app` (Android) and `core` (pure Kotlin: app order, Watch Next and channel rules, recent apps, the Home press
  log, the accent palette).
- The repo is `biskyt/MyTVLauncher` on GitHub.
- **Its look comes from MyPlexPlayer** (`biskyt/MyPlexPlayer`, the owner, 2026-11-14), which has the newest style.
  The theme, `TvCard` and the other parts of `ui/common/Components.kt`, `Focus.kt`, `Touch.kt`, `Keys.kt`,
  the icons' style and the test helpers in `testutil/` are copies of MyPlexPlayer's, with the package renamed.
  - Keep them as MyPlexPlayer has them, so a later style change ports across by hand. Put launcher-only parts in
    files of their own.
  - Tighter links with MyTVPlayer and MyPlexPlayer come later (see ROADMAP.md): for example, their Watch Next items
    with posters.

See also:
- `ROADMAP.md`: what's planned, release by release.
- `RELEASING.md`: signing, releases, and how to make MyTVLauncher the TV's home app.
- `CHANGELOG.md`: what changed in each version.
- `store/README.md`: the Google Play listing's text, graphics and form answers. The privacy policy is
  `docs/privacy.html` (GitHub Pages, https://biskyt.github.io/MyTVLauncher/privacy.html): change it, with a new date,
  when the app starts to use a new kind of information, permission or connection.

## Standing instructions from the owner
- **Write in plain English.** Use the `plain-english` skill (`.claude/skills/plain-english/SKILL.md`) for all
  documentation, code comments that explain behaviour, commit messages, PR descriptions and release notes.
- **Run `/simplify` and `/code-review` before every commit.**
  - Apply their findings, or note why one is skipped.
  - Never commit first and review afterwards.
- **No test APKs unless asked.** CI builds no APK on pushes; APKs are built only for release tags. Build a debug APK
  only when the owner asks for one.
- **Steer clear of any reference to, or hint of, piracy.** This applies to code, docs, store text, changelog and commit messages.
- **Keep overhead and risk low.** Prefer small, well-tested changes, and don't widen a release on your own initiative.
- **Flaky tests must be root-caused and fixed.**
  - Don't re-run CI hoping for green.
  - Never skip, disable or quarantine a test.
  - Don't just raise timeouts.
- **Long waits (CI, test loops, agents) go in the background**, so the conversation isn't blocked.
- **Do not ask the owner to approve the plan again for each step** (the owner, 2026-11-14). Carry on with the
  approved plan. Ask only for a significant change: a new or dropped feature, or a different approach.
- **Answer the owner's questions straight away** (the owner, 2026-10-25): when they ask something, pause the work,
  answer first, then carry on. Leaving a question waiting behind other work reads as ignoring them.
- **Release every merged app change** (as in the sister apps): once an app PR is merged, start `release.yml` (step 8)
  and give the owner the release link.
- **When the owner says "don't release until X", don't merge or release** until they confirm.
- **Only the app's own text is translated**: see **Languages** below.
- **Release notes are English only**, on GitHub and in Play's "What's new" (translated Play notes: not worth the work).
- **Release notes are short** (the owner, 2026-10-27, again 2026-11-14): a bullet list, one brief sentence per change
  (about 12 words at most), no bold lead-ins, explanations or "so that" clauses.

## Process for each change or release
1. Make the change with tests. Check that a new test fails without the fix.
2. Run the CI checks locally:
   - `./gradlew ktlintCheck detekt lintDebug`
   - `./gradlew :core:test :app:testDebugUnitTest`
   - `./gradlew ktlintFormat` fixes most style issues. Rerun `ktlintCheck` afterwards, because the format task itself exits non-zero when it changed something.
3. Run `/simplify`, then `/code-review`, then apply their fixes.
4. Bump `versionCode` (+1) and `versionName` in `app/build.gradle.kts`, for every PR that changes the app, released or
   not: the patch number for fixes and small changes, the minor for a notable feature (1.1.0: languages).
5. Add a `## x.y.z` section at the top of `CHANGELOG.md`, written for people using the app.
   - A release's notes are its section plus those of any unreleased versions since the last release
     (`.github/scripts/release-notes.sh`). For Play's "What's new" (500 characters), `play-notes.py` keeps the whole
     bullets that fit in about 420 and adds a link to the full notes, so put the change users notice most first.
   - Fix a published GitHub release's notes with the **Release notes** workflow (`release-notes.yml`, input `tag`),
     which rewrites them from CHANGELOG.md without rebuilding. It doesn't touch Play: change Play's notes in Play
     Console.
6. Commit with the session's attribution trailers, then push to the working branch and open a PR to `main`.
7. When CI and the emulator runs (if the PR has them; see below) are green, merge the PR (merge commit).
   - CI runs on pull requests only (or by hand), not on `main` after a merge (the owner, 2026-11-14). A push to a
     branch without a PR runs nothing, so open the PR to get CI. The Release run after each merge keeps the caches.
   - CI runs `:core:test :app:recordRoborazziDebug` (the unit tests, recording screenshots to `app/screenshots/`; don't
     commit those unless meant to). The emulator runs are on PRs (or started by hand), and skipped when every
     changed file is one that can't reach the app: `.md`, `docs/`, `store/`, JVM tests and screenshots, or the other
     workflows and their scripts (the list is `paths-ignore` in `emulator.yml`).
8. Releases are built only for a tag, never for a plain merge to `main`. After each merged app change (see the
   standing instructions), start `release.yml` for `v<versionName>` on `main`. It re-tests, signs and publishes the GitHub release with the APK and SHA256SUMS.
   - **From a Claude session, run the workflow by hand:** tag pushes from a cloud session have failed with HTTP 403
     (with either repo name as the remote), while pushes to the working branch work. Use the GitHub MCP tool
     `actions_run_trigger` with method `run_workflow`, `workflow_id` `release.yml` and ref `main`. It tags
     `v<versionName>` itself.
   - **Check first** that the version bump is merged to `main`. The workflow itself refuses a run from another branch,
     or one for a version that already has a release, or (started by hand) a tag: a tag left by a failed run must be
     deleted first.
   - From a normal checkout, pushing the tag also works: `git tag v1.0.2 && git push origin v1.0.2`.
   - A change with no app changes (CI, tests, docs) is merged without a version bump, changelog entry or release.
   - The signed Play bundle (`.aab`) goes to Play's closed testing track, and is a run artifact called
     `MyTVLauncher-<version>-play-bundle`, kept 90 days.
   - If only the Play upload failed (the GitHub release is out), run `play-upload.yml` on `main` with input `tag`
     (for example `v0.1.0`): it uploads that release's bundle again. Don't re-run the release.
   - Versions `0.x` (or dev-key signed builds) are pre-releases. `1.x` and later are full releases.
9. Confirm the release exists (`get_release_by_tag`), then give the owner the release link and the `.aab` artifact link
   (`actions_list` with method `list_workflow_run_artifacts` and the release run's id). For when testers see it, see
   RELEASING.md › Google Play.

## Working from a Claude Code cloud session
- **GitHub tools:** use owner `biskyt`, repo `MyTVLauncher`.
- **CI results:** for each PR, call `subscribe_pr_activity` (repo `MyTVLauncher`) so its CI results wake the session.
  - Each run (CI, the emulators) reports on its own: check them all with `pull_request_read` (`get_check_runs`).
  - Keep one `send_later` check as a safety net (CI and the emulator runs take about 5 minutes). Use it alone for runs
    outside a PR (a release, about 6 minutes; release notes) or if another agent already watches the PR.
- `merge_pull_request`'s `expectedHeadSha` needs the full 40-character SHA.
- Work on the session's designated branch. Once its PR is merged, restart the branch from `origin/main` for any
  follow-up (`git checkout -B <branch> origin/main`); never stack new work on merged history.
- The local `main` can be stale: compare with `origin/main` after `git fetch origin main`.

## Languages
- UK English is the default in `values/`, with es, pt-BR and fr. The launcher follows the device's language (no
  language setting).
  - Every new string needs es, pt-BR and fr translations (`StringsTest` checks this). If the US wording differs, it
    also needs entries in `values-en-rUS` and `values-en-rGB` (see the comment there).
  - A new language needs a `values-<qualifier>` folder and an entry in `androidResources.localeFilters` in
    `app/build.gradle.kts`.
- Text from other apps (app names, Watch Next titles) is shown as it is.

## Gotchas learned the hard way
- **Android never gives the Home key to an app.** A launcher only gets the HOME intent for a short press
  (`MainActivity.onNewIntent`). A held press and a double press are set by the TV maker. The Home key test in
  Settings › Watch Next check showed what the Philips TV sends; the screen went in 0.7.0 (the owner, 2026-11-15).
- **Android 11 package visibility:** the launcher sees other apps only through the `<queries>` block in the manifest.
- **Which apps show** (`core/apps/LaunchEntries`): every app is in All apps. A system app with only a phone entry
  (`LAUNCHER`, no `LEANBACK_LAUNCHER`) is hidden from Home when first found, and once for places saved before 0.3.0
  (the setting `system_phone_apps_hidden`). The Philips TV has system set-up screens with phone entries that its own
  launcher does not show (the owner, 2026-11-14).
- **Recent apps** (a double Home press) need usage access (`PACKAGE_USAGE_STATS`, an app op). TVs often have no screen
  for it: `adb shell appops set com.biskyt.mytvlauncher GET_USAGE_STATS allow`.
- **The TV provider:** query `WatchNextPrograms` and `Channels` with no selection and no sort order. Android 9 refuses
  both from an app without `ACCESS_ALL_EPG_DATA` (system apps only). Filter and sort in core.
- **TV provider rows** (Continue watching, app channels) follow `data/ProviderFollower.kt`: the row's setting, the
  permission, change notices, and reads only when Home is in front after a change. Its `Refresher` work must not take
  the `Refresher`'s own lock again (`exclusive`): the lock is not re-entrant, and a 0.4.0 draft hung every read.
- **App channels:** an app can have many preview channels, and the TV marks as browsable only those that its own
  launcher shows (one for each app at first). The launcher reads them all: a browsable channel is on until the user
  turns it off, another is off until the user turns it on (`HiddenKind.CHANNEL` and `CHANNEL_ON`, `Channels.shown`).
- **Scrolling:** on a TV, Compose keeps the item with focus about a third of the way across a list. `KeepInViewSpec`
  (ui/home/RowScroll.kt, set in `AppRoot`) scrolls only as far as needed, so Up and Down between rows do not make
  them jump (the owner, 2026-11-14). Move mode keeps the moved item in view itself (`AppMove`, `keepInView`).
- **Home's row order** is a list of keys in the settings (`RowOrder`, Settings › Home rows › Row order). A row not
  in the list keeps its usual place after the saved ones.
- **Backup** (0.5.0, as MyPlexPlayer's): `data/BackupFile.kt` is MyPlexPlayer's file code (Download/MyTVLauncher,
  MediaStore, or a direct write with the storage permission on Android 9 only). The JSON is Android's own `org.json`
  (`Backup.toJson` and `Backup.parse`), not a new library. It holds the settings, the app places and the `hidden` rows,
  not other apps' items. A file opened with the launcher (Open with, Share) goes to `AppContainer.openedFile`, and
  `AppRoot` opens Backup & restore for it.
- **Speaker wake** (0.6.0, `data/SpeakerWake.kt`): a receiver for `ACTION_SCREEN_ON`, registered when the app starts
  (that broadcast goes only to receivers registered at run time). It is exported: only the system can send it, and
  "not exported" needs a permission of its own before Android 13. Tests use `FakeSpeaker` (testutil/TestApp.kt).
- **Pictures over plain HTTP** (0.7.0): `usesCleartextTraffic` is on, because Plex and Plezy give Watch Next and
  channel pictures from a server on the home network. Pictures are the launcher's only network use.
- **Pictures of another shape** (`fitsWhole`) are scaled to fit the card, not cropped.
- **TV channel logos** (0.8.1, `ui/home/Logo.kt`): an item of `TYPE_CHANNEL` is a `CardShape.LOGO` card. Its logo's
  transparent border is cut off, every logo gets the same area inside a margin (`LogoFit`), and a dark logo on a
  transparent background goes on a light card (`LogoLook`, in core).
- **The focus glow** (`ui/home/FocusGlow.kt`, 0.7.0; 0.8.1: larger, with a glowing edge) grows the card with focus by
  a tenth and draws a blurred accent edge under it, in one scale of the card's drawing (no layout). In 0.8.1 the card
  grew in a layer and the glow by its own transform, and on the TV the card did not fill the glow. Home's cards turn off `TvCard`'s
  ring (`ring = false`); `TvCard` itself stays as MyPlexPlayer has it. `raiseOnFocus` on a row's item draws it over its
  neighbours, so the semantics tree lists that item last: a test of the order on screen sorts by position.
- **Home's background** (1.1.0, `ui/home/Ambient.kt`): it follows the card with focus (`ambientSource` on each card,
  through `LocalAmbience`, only on Home). A card with a picture gives a backdrop: the picture decoded 48 px wide and
  stretched (a cheap blur: Android 9 to 11 have no blur effect) under a dark overlay. An app's icon, or a card with no
  picture, gives soft radial glows in its colours (`tonesOf`). It changes only after the focus rests for 250 ms and
  fades over 600 ms, so it does no work while the remote moves along a row. A test of its change needs
  `waitAdvancingClock(stepMs > 0)` for the settle delay. Settings › Background brightness (1.1.2, `BackdropLevel`, 0
  to 40%, default 20%, 0 is off) sets the backdrop's share and the glows' strength; the card glow is not part of it.
  `ambientSource` also updates the background when a card's source changes while it has focus: a card shown from
  saved data at start-up gets its picture later (a flaky `AmbientUiTest` found this).
- **The TV guide** (`data/GuideApp.kt`, 0.7.0): the sister apps open their guide with `<package>.action.TV_GUIDE`
  (MyTVPlayer 1.19.0, and MyPlexPlayer's release of 2026-11-15). The Guide key is handled in `MainActivity.dispatchKeyEvent`, so a test
  sends it to the activity, not to the Compose root.
- **Home rows nudge** (1.1.3, `data/HomeRowsNudge.kt`): when the TV wakes (through Speaker wake's `ACTION_SCREEN_ON`
  receiver) and when Home comes to the front, the launcher sends MyTVPlayer (1.22.0) and MyPlexPlayer (1.1.4) a direct
  broadcast, `<package>.action.REFRESH_HOME_ROWS` (with `FLAG_INCLUDE_STOPPED_PACKAGES`), 8 s later (for the network
  after a wake, and for Home's first frame), at most once in 5 minutes and only while Continue watching or the channel
  rows are on. Each app then syncs (MyTVPlayer from the user's other TVs) and writes its Watch Next items and channels,
  and the rows follow. MyTVPlayer's receiver is on only while its sync or its home screen rows are on. Android has no such request of its own. MyTVPlayer runs no scheduled sync for it (the owner,
  2026-11-15: it is used only with this launcher).
- **Home on Home** (1.1.5, the owner, 2026-11-17): one Home press while Home is in front resets it
  (`Navigator.resetHome`, a time: `homeReset`). The rows are not made again: `ResetHome` scrolls the list to its top and
  gives the first app focus, and each row's `rememberHomeRowState` scrolls back to its first card when the reset comes
  or when it next comes into view (`LocalHomeReset`). A move in progress ends, saved. `MainActivity.takeIntent` waits
  `DOUBLE_PRESS_MS` first (so a double press opens the recent apps), and resets only if Home is still in front.
- **Row spacing** (1.1.5): Home's rows use `ROW_ROOM` (8 dp above the cards, 4 dp below), not `GLOW_ROOM`. A row
  draws up to 15 dp past its own top and bottom (Compose's clip for a scrolling list), so the glow still has room.
- **Notifications** (0.8.0, `data/Notifications.kt`): `LauncherNotificationListener` is off in the manifest and turned
  on only while Settings › Show notifications is on (`SystemNotifications.follow`), so Android binds it only then.
  Access is checked again each time Home comes to the front. Tests use `FakeNotifications` (testutil/TestApp.kt).
  Since 1.1.7 each notification is a card with Open and Dismiss buttons. The panel is a window of its own, so a test
  reads its focus as the last part of `focusedText()`.
- **Removing an app from Home** (1.1.7): `RemovedFocus` (ui/home/RowScroll.kt) gives the focus to the card that takes
  the removed app's place. Without it, the focus went to Continue watching.
- **Start-up:** Home draws its first frame from saved data only: Room (`AppContainer.allApps` and `shownApps`, the saved Watch Next
  items) and the settings DataStore, which the accent colour needs first anyway. Reads of
  the installed apps and the TV provider run in the background, one at a time (`data/Refresher.kt`).
  `HomeStartUiTest` holds both reads to check this. A row that comes in on top before the user presses a key takes
  the first focus (`FocusNewTop`): without this, a test that expects focus on Continue watching failed now and then,
  when the Watch Next read ended after Home's first frame.
- **Back:** the activity gives the remote's Back to `BackHandler` in `AppRoot`. In a Robolectric test, use the
  activity's `onBackPressedDispatcher`, not a key event to the Compose root.
- **Robolectric UI tests:**
  - **Compose rules:** use `createUiComposeRule()` / `createUiComposeRule<Activity>()` from `testutil/ComposeRules.kt`, not `createComposeRule()`.
    - The plain rule runs effects on an unconfined dispatcher. Room flows collected with `collectAsState` then resume on Room's `arch_disk_io` threads and race the main thread's recomposition, so updates get lost now and then.
    - That caused long-running flakes in MyPlexPlayer. The real app is unaffected.
  - `TestApp.onTerminate` runs the main looper down before Robolectric resets it: Compose's `AndroidUiDispatcher.Main`
    is a process-wide lazy, and if Robolectric drops its posted run it never posts again (in MyPlexPlayer, every later
    test's paged lists stayed empty).
  - `createUiComposeRule` reads kotlinx-coroutines-test's private `dispatchEvents` by reflection (no public hook in
    1.10.2; an IdlingResource isn't consulted by performScrollToNode, which then loops until out of memory). A
    coroutines upgrade that renames it fails every UI test at once.
  - **Waits:** go through `waitAdvancingClock(stepMs)` in `testutil/UiWait.kt`, because the main looper is paused. Use step 0 to run due work, and a step greater than 0 when delayed work (debounces, timeouts) must happen.
  - **Settings start from defaults:** the settings DataStore is process-wide, so `TestApp` resets it (and `FakeApps`)
    when each test ends. A test sets only what differs from the defaults.
  - **The installed apps in tests** come from `FakeApps`, the Watch Next items from `FakeWatchNext` and the recent apps
    from `FakeRecents`, and the channels from `FakeChannels` (all in testutil/TestApp.kt; its `extra` channel is one
    the TV does not show, so Settings › Home rows lists "Music" twice). `hold()` on `FakeApps` or `FakeWatchNext` makes a read wait. `FakeApps` includes an app named
    "Settings", as on a real TV.
  - **A read that a test calls straight after the app starts** must not depend on a flag that a coroutine sets later:
    the Watch Next read checks the saved setting itself (a flaky `WatchNextSafetyTest`, fixed in 0.2.0).
  - **Do not send a system broadcast such as `ACTION_SCREEN_ON` in a test.** Robolectric delivered it to a receiver
    left by an earlier test's app, and while `SpeakerWakeTest` sent one, UI tests later in the same JVM failed now and
    then (Back did not reach AppRoot, a channel row was missing). Call the receiver's method (`SpeakerWake.woke`) and
    check the registration with `shadowOf(app).registeredReceivers`.
  - **Thread dumps on timeout:** `otherThreads()` in UiWait.kt dumps the other threads when a wait times out.
  - **Images on two threads at once abort the test JVM** (exit 134, "JniConstants … Class not found: java/nio/…"):
    Robolectric's native graphics set themselves up unsafely. `TestApp` calls `lookUpNioClasses()`
    (testutil/NativeGraphics.kt) first, so a new test application class must call it too.
- **Never run `pkill -f <pattern>`** where the pattern also appears in your own command line, because it kills your own shell. Kill background loops by PID.

## The owner's setup
- **Main device:** a Philips 48OLED806, Android TV 11 (API 30). **Second device:** a Chromecast with Google TV.
- The emulator smoke tests use Android TV 11 and Google TV 14 to match them (`.github/workflows/emulator.yml`).

## Product decisions from the owner (2026-11-14)
- **No ads, no tracking, no analytics.** The only network use is pictures that other apps give to Watch Next.
- **Layout B** of the mockups: Continue watching first, then the apps as square icons with names, then channel rows.
  No hero area.
- **Watch Next:** up to 20 items. A poster card for a 2:3 picture, else a wide 16:9 card. Settings turn the row off
  (then nothing is read) and choose its apps. The same for app channels.
- **No effect on start-up time** from Watch Next or channels.
- **Always a way out:** every app and Android settings open from MyTVLauncher, so the owner can change back to another
  launcher. Settings › Home app opens Android's choice of home app.
- **Accent colour:** MyPlexPlayer's presets, mint by default. No large text setting.
- **All apps:** a grid of every app, hidden ones too, with no counts and no button to Android's app list.
- **Remote shortcuts:** the Home key test on the Philips TV showed that a double Home press reaches the launcher and a
  held one does not. Two presses within about 500 ms open the launcher's own recent apps panel. All apps opens from a
  top bar button and a tile at the end of the apps row. No accessibility service (see ROADMAP.md).
- **Backup** (0.5.0) as MyPlexPlayer's. **Speaker wake** (0.6.0) is low priority.
- **TV only on Google Play** (the owner, 2026-11-18, 1.1.6): the manifest requires `android.software.leanback`, so
  Play offers the app only to TVs. The touch support stays for sideloaded use.
