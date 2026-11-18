# Store listing

Everything for MyTVLauncher's Google Play listing: the graphics, the screenshots, the text to paste in, and the answers
for Play Console's forms.

## Graphics

| File | Play Console field | Size |
|---|---|---|
| `graphics/icon-512.png` | App icon | 512 × 512, 32-bit PNG |
| `graphics/feature-graphic-1024x500.png` | Feature graphic | 1024 × 500, 24-bit PNG |
| `graphics/tv-banner-1280x720.png` | Android TV banner | 1280 × 720, 24-bit PNG |
| `screenshots/*.png` | Android TV screenshots | 1920 × 1080, 24-bit PNG |

Upload the screenshots in this order:
1. `1-home.png`: Home, with Continue watching first
2. `2-channels.png`: Home's app channels, with TV channel logos
3. `3-all-apps.png`: All apps, with a heart on the favourite apps
4. `4-recent-apps.png`: Recent apps (Home pressed twice)
5. `5-settings.png`: Settings

The app draws all of them itself (`app/src/test/.../ui/StoreAssetsTest.kt`). The apps, posters and channel logos are
stand-ins drawn by the test, so no real app, title or broadcaster shows. To make them again after the app's look changes:

```
./gradlew :app:testDebugUnitTest --tests '*StoreAssetsTest*' -PstoreAssets
```

## Privacy policy

`docs/privacy.html`, published with GitHub Pages at **https://biskyt.github.io/MyTVLauncher/privacy.html**. The site's
home page is `docs/index.html`, at https://biskyt.github.io/MyTVLauncher/.

Change the policy when the app starts to use a new kind of information, a new permission or a new network connection,
and give it a new date.

## Listing text

Write it in English (United Kingdom), the listing's default language. Keep the feature list in step with README.md.

**App name** (30 characters max): `MyTVLauncher`

**Short description** (80 characters max):

> A light home screen for Android TV and Google TV. No ads and no tracking.

**Full description** (4000 characters max):

> MyTVLauncher is a light, fast home screen for Android TV and Google TV. It has no ads, no sponsored rows, no
> tracking and no account.
>
> • Continue watching: the items your apps put on the home screen, with a progress bar, the episode and the time left
> • Favourite apps: round app icons on Home, in the order you choose
> • App channels: the rows that your apps give the TV, with TV channel logos all the same size
> • Your rows, your order: turn each row on or off, choose its apps, and move the rows up and down
> • All apps: every app on the TV, with a heart on your favourites
> • Recent apps: press Home twice to see the apps you used last
> • A background that follows the card you are on, with a brightness setting
> • The TV guide button opens the TV's own guide, MyTVPlayer's or MyPlexPlayer's
> • Optional: the TV's notifications in the top bar
> • Optional: wake a soundbar or receiver that sleeps when the TV wakes
> • Backup and restore of your Home and your choices
> • Accent colours, a clock, the network status and a VPN badge
> • In English, Spanish, Portuguese (Brazil) and French
>
> Light and quick: Home shows straight away from what it saved last time, and it reads your apps' rows in the
> background.
>
> Always a way out: every app and Android's settings open from MyTVLauncher, so you can change back to another home
> screen at any time.
>
> Some TVs, Google TV among them, do not let you choose a new home screen in their settings. On those TVs, you need a
> computer (adb) to make MyTVLauncher the home screen. The steps are at https://github.com/biskyt/MyTVLauncher.
>
> MyTVLauncher is open source (GPL-3.0): https://github.com/biskyt/MyTVLauncher

**What's new:** each release sends its own notes from CHANGELOG.md (see RELEASING.md).

**Category:** *Personalization*. **Tags** (if asked): launcher, home screen, Android TV.

**Contact email:** the developer account's email. **Website:** https://github.com/biskyt/MyTVLauncher

## Answers for Play Console

- **Price:** free. **In-app purchases:** none.
- **Ads:** no ads.
- **Data safety:** *No data collected* and *No data shared*.
  - Nothing goes to the developer. The app has no server, no analytics and no crash reporting.
  - The app reads the installed apps, the TV's home screen items, the recent apps (with usage access) and the
    notifications (with notification access) only to show them on the TV. It keeps them on the device, and the
    notifications only in memory. Play doesn't count data that is used only on the device as "collected".
  - The only network use is to load the pictures that other apps give to the home screen, from the servers that those
    apps name. The app sends no information about the user with these requests.
  - The developer has no data to delete. Uninstalling the app, or clearing its storage, removes everything from the
    device.
- **Content rating (IARC questionnaire):** a utility (a home screen).
  - It has no content of its own. It shows the names, icons and home screen items of the apps on the TV.
  - It has no user-to-user features, no chat, no shared location and no purchases.
  - The usual result is PEGI 3 / Everyone.
- **Target audience:** 18 and over. Not directed at children.
- **App access:** all functionality is available without special access (no sign-in).
- **Government, financial, health, news or COVID-19 app:** no.
- **Sensitive permissions:**
  - The app does not use `QUERY_ALL_PACKAGES`. It finds apps through a `<queries>` block for home screen entries, so
    Play's declaration form for it does not apply.
  - Usage access (`PACKAGE_USAGE_STATS`) and notification access are special access that the user gives in Android
    settings. They are optional, and the privacy policy describes them.
- **Android TV:**
  - The app declares `LEANBACK_LAUNCHER`, a TV banner and a home screen activity (`HOME`), and it doesn't need a
    touchscreen.
  - Opt in to Android TV under *Test and release › Setup › Advanced settings › Form factors*.
  - Add the TV screenshots and the banner under the store listing's *Android TV* section. Play reviews a TV app for
    TV quality before it shows it on TVs.
