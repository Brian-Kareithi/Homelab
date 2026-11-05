# Homelab

A native Android app (Kotlin + Jetpack Compose) that mirrors my self-hosted
[Homepage](https://gethomepage.dev) dashboard, so I can watch the whole homelab from my phone.

The app talks to **one URL only: the Homepage instance**. Every number comes from Homepage's
JSON API and its server-side widget proxy, which adds the Sonarr/Radarr/Jellyfin/etc. API keys
on the server. The app never stores or asks for any service credentials.

## Features

- **Dashboard**: service groups and cards built from `/api/services` (add a service in Homepage
  and it appears here), Homepage-style stat blocks per widget, Docker status pills, CPU/RAM/Disk
  gauges, collapsible groups, pull-to-refresh and auto-refresh (5/15/30/60 s or off).
- **Activity**: Jellyfin now-playing, the Sonarr queue and active qBittorrent torrents together.
- **Bookmarks**: grouped bookmarks from `/api/bookmarks`, opened in a Custom Tab.
- **Service detail**: every stat plus lists (queue, missing albums, per-indexer Prowlarr table,
  torrents) and Docker CPU/memory/network for services with `server` + `container`.
- **Offline**: the last good snapshot is cached and shown with a "Last updated X ago" banner.
- **Notifications** (optional, every 15 min): container stopped, server down/up, torrent
  finished, new Seerr request.

Supported widgets: Jellyfin (v1 and v2), Sonarr, Radarr, Lidarr, Prowlarr,
Seerr/Overseerr/Jellyseerr and qBittorrent. Other widget types show the card without stats.

## Build and install

Requirements: Android Studio (recent; the project uses AGP 9.5 and Gradle 9.6) with the Android
SDK platform 37 installed. Android Studio's bundled JDK is fine.

**Android Studio:** *File → Open* this folder, wait for Gradle sync, pick your phone, press **Run**.

**Command line:**

```bash
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

Then install `app/build/outputs/apk/debug/app-debug.apk` on the phone. Either:

- `adb install -r app/build/outputs/apk/debug/app-debug.apk` with USB debugging on, or
- copy the APK to the phone and open it (allow "Install unknown apps" for your file manager).

Run the unit tests with `./gradlew testDebugUnitTest`.

## Connecting to Homepage

Settings → **Server**:

| Mode      | URL                                  | When                                    |
|-----------|--------------------------------------|-----------------------------------------|
| LAN       | `http://192.168.100.35:3000`         | On home Wi-Fi                           |
| Tailscale | `http://100.100.173.21:3000`         | Anywhere, with Tailscale on (default)   |
| Auto      | tries LAN for 1.5 s, then Tailscale  | Switches automatically as you move      |
| Custom    | e.g. `http://dreezy0:3000` (MagicDNS) | Any other Homepage URL                  |

Use **Test connection** to check a URL before saving. Homepage is served over plain HTTP on
private addresses, so the app allows cleartext traffic (`res/xml/network_security_config.xml`).

### Tailscale on the phone

1. Install **Tailscale** from the Play Store and sign in with the same account as the server
   (`dreezy0`).
2. Turn the VPN on. `dreezy0` should be listed as a machine with IP `100.100.173.21`.
3. Optional: enable **MagicDNS** in the Tailscale admin console to use `http://dreezy0:3000`.
4. In Homelab, choose **Tailscale** (or **Auto**) and tap **Test connection**.

Long-pressing a card opens the service's own web UI (its Homepage `href`). Those links point at
LAN/Tailscale addresses, so they also need home Wi-Fi or Tailscale.

## Project layout

```
app/src/main/java/dev/kareithi/homelab/
  data/api/HomepageApi.kt          Retrofit interface for Homepage's API
  data/model/                      @Serializable DTOs (only the fields used)
  data/net/BaseUrlResolver.kt      Auto LAN → Tailscale logic, connection test
  data/net/BaseUrlInterceptor.kt   rewrites every request onto the current base URL
  data/repo/                       repository + offline cache
  data/settings/SettingsStore.kt   DataStore preferences
  domain/                          widget mappers, icon resolver, formatters, UI models
  di/AppContainer.kt               manual dependency container
  ui/                              Compose screens, components, theme
  work/                            WorkManager health check + notifications
```

## Notes

- **Dependency injection:** a small manual container is used instead of Hilt. It keeps the
  build free of annotation processing on AGP 9's built-in Kotlin.
- **Radarr:** Homepage's proxy trims `movie.all` to `{title, id}`, so the detail screen lists the
  movies in Radarr. A "missing movies" list appears automatically if Homepage starts including
  `hasFile`/`monitored`.
- **Sonarr queue:** Homepage returns the first page of the queue (10 records), so lists show
  "10 of N".
- **Sizes** use 1024-based units: the ~2.8 TB (decimal) media pool shows as 2.6 TB.
- **Icons** load from jsDelivr (dashboard-icons, Material Design Icons, simple-icons) and are
  cached on disk, so they keep working offline after the first load.
- No accounts, analytics or third-party services beyond those icon CDNs.
