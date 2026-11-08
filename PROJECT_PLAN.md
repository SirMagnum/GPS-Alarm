# GPS Alarm — Project Plan

A single Android app that does everything the stock Clock/Alarm app does, **plus** location-based ("geofence") alarms: pick a destination on a map, set a trigger radius, and get an alarm when you enter (or approach) that radius.

---

## 1. Goal & Scope

**V1 goal:** A working Android app with two alarm types living side by side in one list:
- **Time alarms** — identical in spirit to the stock Android alarm clock.
- **Location alarms** — pick a point on a map, set a radius, alarm fires on entry.

**Explicitly out of scope for V1** (revisit later):
- iOS version
- Multiple destinations per alarm / routes
- Turn-by-turn navigation
- Cloud sync / account system
- Smart ETA-based alerts ("wake me 10 min before arrival" using traffic data)

---

## 2. Tech Stack

| Piece | Choice | Why |
|---|---|---|
| Language | Kotlin | Modern Android standard, best docs/support |
| UI | Jetpack Compose | Current recommended UI toolkit, less boilerplate than XML views |
| Min SDK | API 26 (Android 8.0) | Covers ~95%+ of active devices, required for reliable notification channels |
| Map | OpenStreetMap via `osmdroid` | No API key, no Google Cloud account, no billing risk ever — free OSM tiles. Traded Maps' polish for zero account/key management (decided in Phase 2) |
| Location/Geofencing | Android Geofencing API (`com.google.android.gms:play-services-location`) | OS-managed, battery-efficient, no need to poll GPS manually |
| Local DB | Room (SQLite wrapper) | Standard for storing alarms locally, works offline |
| Background alarm trigger | `AlarmManager` (time alarms) + `GeofencingClient` broadcast receiver (location alarms) | Native Android mechanisms for exact-time and geofence triggers |
| Alarm ringing/UI-over-lockscreen | Foreground `Service` + full-screen `Notification` | Required pattern for reliable alarm-style apps (same approach stock clock apps use) |
| Dependency injection | Hilt (optional, can skip for V1 and add later) | Keeps code testable as it grows |

---

## 3. Environment Setup (do this once, manually, before touching Claude Code)

1. Install **Android Studio** (latest stable channel).
2. On first launch, let it download the Android SDK + an emulator system image (Android 14 recommended).
3. Create a new project: **Empty Activity** template, Kotlin, min SDK 26.
4. Run it on the emulator — confirm you see a blank "Hello Android" screen. This proves the whole toolchain works.
5. ~~Get a Google Maps API key~~ — not needed. The app uses `osmdroid` (OpenStreetMap tiles), so there's no API key, Google Cloud project, or billing account to set up, ever.
6. (Optional, later) Install the **Claude Code JetBrains plugin** from the marketplace, restart Android Studio, run `claude` in the integrated terminal, then `/ide` to connect it for AI-assisted coding directly in the IDE.

---

## 4. Required Permissions

Declare in `AndroidManifest.xml`, and request at runtime where needed:

- `ACCESS_FINE_LOCATION` — precise GPS for geofencing
- `ACCESS_BACKGROUND_LOCATION` — needed for geofences to fire while app is closed (Android 10+ requires a **separate** runtime request for this, with its own permission dialog)
- `POST_NOTIFICATIONS` — Android 13+ requires explicit opt-in for notifications
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` — for precise time-based alarms (Android 12+ restricts exact alarms)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_LOCATION` — to run the alarm-ringing service reliably
- Request **battery optimization exemption** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — without this, Doze mode can delay or kill your alarm; stock alarm apps always ask for this during onboarding

---

## 5. Data Model (Room entities)

**`Alarm`** (shared base info)
- `id`, `label`, `isEnabled`, `type` (TIME / LOCATION), `soundUri`, `vibrate`, `snoozeEnabled`, `snoozeDurationMin`, `createdAt`

**`TimeAlarmDetails`** (1:1 with Alarm where type == TIME)
- `alarmId`, `hour`, `minute`, `repeatDays` (bitmask or list of weekdays), `isOneTime`

**`LocationAlarmDetails`** (1:1 with Alarm where type == LOCATION)
- `alarmId`, `destinationLat`, `destinationLng`, `destinationAddress` (display string), `radiusMeters`, `triggerOn` (ENTER / EXIT), `oneShot` (auto-disable after firing, like a one-time alarm)

---

## 6. Architecture Overview

```
UI (Compose screens)
  ├─ Alarm List screen        → shows both alarm types in one list, toggle on/off
  ├─ Add/Edit Time Alarm      → time picker, repeat days, sound, label
  ├─ Add/Edit Location Alarm  → map picker, search box, radius slider, label
  └─ Alarm Ringing screen     → full-screen, shows over lock screen, dismiss/snooze

ViewModel layer (one per screen, holds UI state, talks to Repository)

Repository layer
  ├─ AlarmRepository          → Room DB CRUD, single source of truth
  ├─ TimeAlarmScheduler       → wraps AlarmManager, (re)schedules on boot/edit
  └─ GeofenceManager          → wraps GeofencingClient, adds/removes geofences

System callbacks
  ├─ AlarmReceiver (BroadcastReceiver)      → fired by AlarmManager at set time
  ├─ GeofenceBroadcastReceiver              → fired by GeofencingClient on enter/exit
  ├─ BootReceiver                           → re-registers all alarms/geofences after device reboot
  └─ AlarmRingingService (foreground Service) → plays sound/vibration, shows full-screen notification, survives app being swiped away
```

**Key reliability pattern:** Android kills geofences and exact alarms on device reboot — you must register a `BOOT_COMPLETED` receiver that reloads everything from Room and re-registers it with the OS.

---

## 7. Build Phases (recommended order)

### Phase 0 — Scaffolding ✅
- [x] Empty Compose project running on emulator
- [x] Set up Room DB with the three entities above
- [x] Basic navigation shell: Alarm List → Add/Edit screens (empty for now)

### Phase 1 — Time Alarms ✅ (prove the core alarm mechanism works first, no location complexity yet)
- [x] Add/Edit Time Alarm UI (time picker, label, repeat days, sound picker)
- [x] `TimeAlarmScheduler` using `AlarmManager.setExactAndAllowWhileIdle`
- [x] `AlarmReceiver` + `AlarmRingingService` + full-screen ringing UI with snooze/dismiss
- [x] `BootReceiver` to reschedule after reboot
- [x] Test on emulator: set alarm 2 min out, lock screen, confirm it rings over lock screen — verified live, including dismiss and a real emulator reboot re-registering the alarm

### Phase 2 — Location Alarms ✅ (uses `osmdroid`/OpenStreetMap instead of Google Maps — see §2)
- [x] ~~Google Maps integration, API key wired up~~ — replaced with `osmdroid`, no key needed
- [x] Add/Edit Location Alarm UI: map with draggable pin + tap-to-place, search bar (device `Geocoder`), radius slider with a circle overlay on the map
- [x] Runtime permission flow for fine + background location (with the Android 11+ "Allow all the time" Settings redirect)
- [x] `GeofenceManager` registers geofence with `GeofencingClient` — note: the registration `PendingIntent` **must** use `FLAG_MUTABLE`, not `FLAG_IMMUTABLE` (Play Services fills in the `GeofencingEvent` extras when it fires the broadcast; immutable causes `DEVELOPER_ERROR`)
- [x] `GeofenceBroadcastReceiver` → triggers same `AlarmRingingService` as time alarms
- [ ] **Pinned for later**: live-fire confirmation of an actual ENTER/EXIT transition on the emulator. Registration is confirmed working (no more `DEVELOPER_ERROR`), but Play Services' own geofence evaluation is batched by the OS and was taking ~20+ min to resolve even for the initial-trigger check in this emulator session — impractical to sit through interactively. The code path (receiver → `AlarmRingingService`) is already proven out via the time-alarm path, which shares the same service. Revisit by leaving an enabled location alarm running in the background and checking back later, or testing on a real device.

### Phase 3 — Unified List & Polish ✅
- [x] Combined alarm list (time + location) with type icon, swipe to delete, toggle switch — verified live: swipe removes the alarm, cancels its `AlarmManager`/geofence registration, and cascades the delete to its details row
- [x] Battery optimization exemption prompt on first launch — verified live (dialog appears when not exempted, naturally skips once granted)
- [x] Notification permission prompt (Android 13+) — basic request with no custom rationale UI (acceptable for V1; revisit if a rationale dialog is wanted)
- [x] Empty states, basic app icon, app name

### Phase 4 — Reliability Hardening
- [x] Handle device reboot (re-register everything) — `BootReceiver` re-registers both time alarms and geofences; verified live with a real emulator reboot
- [ ] Handle app force-stop edge cases — **documented limitation, not coded**: Android can silently stop background triggers (alarms/geofences) if the OS or user force-stops the app, and no in-app mechanism can fully work around this (even a periodic self-check job would itself be killed by the same force-stop). Users should be told not to force-stop the app from Settings/recent-apps "clear all" on aggressive OEMs.
- [ ] Test across a couple of real-world manufacturers — not possible from this environment (no physical Xiaomi/Samsung devices); needs manual testing on real hardware before relying on this day-to-day.

### Phase 5 — Nice-to-haves (post-V1)
- [ ] Multiple radius zones (e.g., warn at 5km, alarm at 500m)
- [x] Custom alarm sounds per alarm — sound picker + vibrate toggle, shared between time and location alarms (`SoundAndVibrateSection`), verified live on both screens
- [x] Dark mode — already free from Compose `MaterialTheme` following the system setting (plus dynamic color on Android 12+), since Phase 0
- [ ] Widget for quick alarm toggle

---

## 8. Testing Strategy

- **Emulator + fake GPS** for all location-alarm development — Android Studio's Extended Controls lets you set a lat/lng instantly or simulate a moving route, which is far faster than physically traveling to test.
- **Real device** for final checks on: exact alarm reliability while phone is locked overnight, battery optimization behavior, true GPS accuracy outdoors.
- Keep a short manual test checklist (e.g., "set alarm → lock phone → wait → does it ring over lock screen with sound + vibration?") and re-run it after any change to the scheduling/service code, since this is the part most prone to silent regressions.

---

## 9. Cost Notes

- Map tiles via `osmdroid`/OpenStreetMap: **free, no API key, no account** — but be a good citizen of the shared OSM tile server (osmdroid's default user agent/usage policy) if this ever gets heavy real-world use; self-hosting tiles is an option far down the line, not needed for personal use.
- Geofencing via `play-services-location`: **free** (it's a device API, not a billed cloud call).
- Destination search uses the device's built-in geocoder (`Geocoder` class) — free, no network billing, no Places API needed.

---

## 10. Known Android Gotchas (read before Phase 2)

- **Background location permission is a two-step prompt** on Android 10+: you must first get foreground location granted, show the user *why* you need background access, then request it separately — Google Play also requires a privacy policy and justification if you publish an app using this permission.
- **Exact alarms need special permission on Android 12+** (`SCHEDULE_EXACT_ALARM`), and Play Store review scrutinizes apps requesting it — fine for an alarm app (it's a legitimate use case) but be ready to justify it if you publish.
- **Doze mode / battery optimization** can delay geofence callbacks by minutes on some devices unless the user exempts your app — this is why stock alarm apps always ask for that exemption up front.
- **OEM aggressive battery management** (Xiaomi/MIUI, Samsung, OnePlus) can kill background services more aggressively than stock Android — a known pain point for every geofencing app, not something you can fully fix in code, just need to document it for users.

---

## 11. Suggested Next Step

Once Android Studio + emulator are confirmed working (§3), start Phase 0 → Phase 1 in order. Time alarms alone teach you `AlarmManager`, foreground services, and the ringing UI — all of which the location-alarm feature reuses, so building it first makes Phase 2 much faster.
