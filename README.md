<div align="center">

# GPS Alarm

**An alarm clock that also knows where you're going.**

Set a time, or set a destination — GPS Alarm rings when you arrive, not just when the clock strikes.

[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](https://github.com/SirMagnum/GPS-Alarm)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Latest Release](https://img.shields.io/github/v/release/SirMagnum/GPS-Alarm?label=release)](https://github.com/SirMagnum/GPS-Alarm/releases/latest)

[Download the latest APK](https://github.com/SirMagnum/GPS-Alarm/releases/latest) · [Report an issue](https://github.com/SirMagnum/GPS-Alarm/issues)

</div>

---

## What it does

- **Time alarms** — the usual: a time, an optional repeat schedule, your own sound and vibration.
- **Location alarms** — pick a destination on the map (or search for one), set a radius, and the alarm fires the moment you arrive. A two-ring geofence (a wide outer ring plus the real destination) keeps battery use low while still catching arrival within seconds, not the many minutes Android's lazy geofencing alone would take.
- **Reliability checks** — Settings surfaces exactly what could stop an alarm from firing on time: blocked notifications, missing exact-alarm permission, battery optimization, or location access that isn't set to "all the time."
- **Diagnostics** — a running event log plus (debug builds) a mock-location tool, so a geofence crossing can be tested without actually driving there.

## Design

Every screen is built around one idea: **nothing is a separate page**. Tapping an alarm card expands it in place to edit it; Settings and "add alarm" slide up as trays over a dimmed home screen instead of navigating away. The whole app uses a warm, high-contrast "arcade cabinet" look — hard-edged shadows instead of soft elevation, bundled display/mono/UI type, and a light and dark palette that are each tuned on their own rather than one being an inverted copy of the other.

## Built with

| | |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 (heavily re-themed) |
| **Persistence** | Room |
| **Location & geofencing** | Google Play Services Location |
| **Map** | osmdroid, on OpenStreetMap tiles |
| **Place search & reverse geocoding** | [Photon](https://photon.komoot.io) and [Nominatim](https://nominatim.openstreetmap.org) (free, keyless OpenStreetMap services) |
| **Navigation** | Navigation Compose, for the one real destination (Diagnostics) that still earns a separate screen |

Minimum SDK 26 (Android 8.0), target/compile SDK 37.

## Getting the app

Grab the APK from the [latest release](https://github.com/SirMagnum/GPS-Alarm/releases/latest) and install it directly — no Play Store listing. It's signed with a debug key, so Android will ask you to confirm installing from an unknown source; that's expected for a side-loaded APK.

On first launch, grant notifications, exact-alarm scheduling, and battery-optimization exemption when prompted. For location alarms, location access needs to be "Allow all the time," not just "while using the app" — Settings will flag it if it isn't.

## Building it yourself

```bash
git clone https://github.com/SirMagnum/GPS-Alarm.git
cd GPS-Alarm
./gradlew assembleDebug    # debug build
./gradlew assembleRelease  # release build (minified, still debug-signed)
```

Open the project in Android Studio as usual, or install straight from the command line with `./gradlew installDebug` on a connected device or emulator.

## Project layout

```
app/src/main/java/com/gpsalarm/app/
├─ alarm/        # AlarmManager scheduling + the ringing foreground service
├─ geofence/     # Geofence registration and the close-range approach tracker
├─ data/         # Room entities, DAO, repository
├─ diagnostics/  # In-app event log + debug mock-location tool
├─ settings/     # Theme, map style, and default snooze preferences
└─ ui/           # Compose screens, the shared design system, and the map
```
