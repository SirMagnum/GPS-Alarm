# Alarm App — UI/UX Design Specification

**Platform:** Android (Gradle build)
**Recommended stack:** Jetpack Compose + Material 3 (heavily customized) — chosen because container-morph transitions, shared-element animation, and physics-based motion are native to Compose's animation APIs and painful to retrofit onto classic View/XML + Fragment navigation.

---

## 1. Design Philosophy

Five rules govern every screen in this app:

1. **One surface, not many pages.** The app behaves like a single living surface. Instead of navigating *to* a new screen, components expand, morph, or float above the current context and then collapse back into it. There is no `Activity`-to-`Activity` or full-screen `Fragment` transaction for anything that can instead be a container transform.
2. **Nothing overlaps unintentionally.** Every component owns a defined region of the screen at every point in its animation lifecycle. Overlays (sheets, dialogs, expanded cards) use scrim + elevation + motion to make it obvious they are temporarily occupying space above the base layer — never clipping or colliding with another interactive element.
3. **Motion explains state change.** Every transition answers "where did this come from and where is it going?" via continuous motion (position, scale, corner-radius interpolation) rather than a cut or a cross-fade between two disconnected layouts.
4. **Tactile, not flat.** Every pressable element behaves like a physical arcade-cabinet control — it sits up on a hard shadow and visibly sinks when touched, confirmed by a matching haptic. See §3A.
5. **Say less.** Screens communicate through color, shape, position, and motion first. Words are a last resort, and when used, they're a single word or numeral, never a sentence.

---

## 2. Design Tokens

### 2.1 Color System

Two **independent** palettes — dark mode is not just an inverted light mode. Both avoid purple/violet and navy/indigo entirely. The feel is a lit-up arcade marquee: a warm neutral base, punchy saturated accents, and one true "glow" color reserved for the alarm-ringing state.

**Palette A — Light ("Marquee Day")** — warm paper base, ink text, candy-bright accents.

| Token | Hex | Usage |
|---|---|---|
| `color.bg.base` | `#FAF4E8` | App background (warm cream, not white) |
| `color.bg.surface` | `#FFFFFF` | Cards, containers at rest |
| `color.bg.surfaceRaised` | `#FFFFFF` | Expanded containers, sheets (lifted via hard-shadow, see §4) |
| `color.accent.primary` | `#FF4B2B` | Tomato-red — primary actions, selected time, FAB |
| `color.accent.secondary` | `#1FAE6E` | Arcade green — toggles ON, confirm states |
| `color.accent.glow` | `#FFC53D` | Marigold — alarm-ringing glow, snooze |
| `color.text.primary` | `#1C1710` | Headlines, time display (warm near-black, never pure black) |
| `color.text.secondary` | `#8A7F6D` | Labels, metadata |
| `color.text.disabled` | `#C9BFA9` | Disabled alarm rows |
| `color.danger` | `#D62839` | Delete action (distinct from primary red) |
| `color.outline` | `#1C1710` | Hard outline used on tactile components, see §4 |

**Palette B — Dark ("Marquee Night")**

| Token | Hex | Usage |
|---|---|---|
| `color.bg.base` | `#14100C` | App background (warm near-black, brown-tinted) |
| `color.bg.surface` | `#1F1A14` | Cards, containers at rest |
| `color.bg.surfaceRaised` | `#28221A` | Expanded containers, sheets |
| `color.accent.primary` | `#FF6A3D` | Neon coral-orange — primary actions, selected time, FAB |
| `color.accent.secondary` | `#3EE68C` | Neon green — toggles ON, confirm states |
| `color.accent.glow` | `#FFD23F` | Neon amber — alarm-ringing glow, snooze |
| `color.text.primary` | `#F6EFE2` | Headlines, time display |
| `color.text.secondary` | `#A89A84` | Labels, metadata |
| `color.text.disabled` | `#564C3E` | Disabled alarm rows |
| `color.danger` | `#FF5C5C` | Delete action |
| `color.outline` | `#0A0806` | Hard outline on tactile components |

Rules: no pure black/white backgrounds (both bases carry a warm tint); no blue, indigo, purple, or navy anywhere in either palette; every screen uses at most 3 accent colors at once (primary, secondary, glow) to keep the arcade-marquee feel punchy rather than noisy.

### 2.2 Typography

No Helvetica/Poppins/Inter/Roboto-generic sans. Two-family pairing, chosen for character:

- **Display family — "Cabinet Grotesk"** (or equivalent chunky, slightly-condensed geometric grotesk with real personality in its numerals — alternates: "Clash Display", "Bricolage Grotesque"). Used for the clock, time display, and any numeral a user reads at a glance. Its distinctive single-story numerals and slightly blunt terminals are what give the app its "cabinet marquee" identity — this is the one typeface a screenshot of the app should be recognizable by.
- **Ticker family — "Departure Mono"** (or "Space Mono"/"JetBrains Mono" as fallback). A monospaced face used *only* for small live-updating digits: the snooze countdown, the seconds tick, repeat-day abbreviations. Reads like an arcade score ticker — reserved for things that are literally counting, never for body text.
- **UI family — "General Sans"** (or "Switzer"). A warmer, slightly humanist grotesk for everything else — labels, the rare sentence, buttons. Distinct enough from the display face to not compete with the big numerals, plain enough to stay legible at small sizes.

| Role | Family | Size / Line height | Weight | Usage |
|---|---|---|---|---|
| `type.displayTime` | Display (Cabinet Grotesk) | 76sp / 80sp | 700 | Big clock, time wheel in editor |
| `type.alarmTime` | Display (Cabinet Grotesk) | 34sp / 38sp | 700 | Time shown on each alarm card |
| `type.ticker` | Ticker (Departure Mono) | 14sp / 18sp | 500 | Snooze countdown, seconds, day-letter chips |
| `type.title` | UI (General Sans) | 20sp / 26sp | 600 | Section titles — used sparingly, see §5 |
| `type.body` | UI (General Sans) | 16sp / 22sp | 400 | Labels, the occasional short line |
| `type.button` | UI (General Sans), all-caps, +2% letterspacing | 14sp / 18sp | 700 | Buttons, tactile component labels (arcade cabinet caps) |

### 2.3 Spacing & Shape

- Base spacing unit: **4dp**. Standard paddings: 8 / 12 / 16 / 24 / 32.
- Corner radius scale: `radius.sm = 12dp` (chips, small buttons), `radius.md = 20dp` (alarm cards), `radius.lg = 28dp` (bottom sheets, expanded containers), `radius.full` (FAB, toggle pill, time scrubber knob).
- Minimum touch target: 48×48dp, minimum spacing between adjacent interactive elements: **12dp** — this is a hard rule to guarantee the "no overlapping components" requirement holds even at the largest system font scale.

### 2.4 Elevation (container depth system)

Not Material's soft blurred shadow — a **hard offset shadow** (neo-arcade style: a flat duplicate of the shape, offset down-right, in `color.outline` or a darker tint of the surface, 0 blur). It reads as physically stacked, not just "lit from above," and it's what makes buttons look pressable in §4.

| Layer | Z | Visual treatment |
|---|---|---|
| `layer.base` | 0 | App background, alarm list — no shadow |
| `layer.raised` | 1 | Alarm card, buttons at rest — 4dp hard offset shadow (down-right), 1.5dp `color.outline` border |
| `layer.floating` | 2 | Expanded/opened container (edit sheet, time picker) — 8dp hard offset shadow + scrim behind it at 45% of `color.outline` |
| `layer.critical` | 3 | Alarm-ringing full-screen state — no shadow (full-bleed), communicates urgency via the glow pulse instead |

---

## 3A. Arcade Tactility

Every interactive element behaves like a physical cabinet button, not a flat tap target:

- **Pressed state = shadow collapses.** At rest, a button/toggle/card sits on its 4dp hard-offset shadow (`layer.raised`). On press, the element translates down-and-right by the exact shadow offset while the shadow itself shrinks to 0 — the button visually "sinks" into the surface. Release reverses it with a short overshoot spring (`motion.spring`), never a linear ease. Total press-to-rest round trip: ~140ms down, ~180ms spring back up.
- **Chunky outlines, not hairlines.** Every primary tappable surface (buttons, toggle track, slider track, FAB, cards) carries a 1.5–2dp solid `color.outline` border. This is what reads as "cabinet hardware" rather than "flat material card."
- **Sliders are physical.** The snooze-duration and time-adjust sliders use a thick grooved track (`color.bg.surface` inset channel) with a large, outlined, hard-shadowed knob — dragging gives a continuous haptic "tick" every discrete step (same cadence as a real slide-potentiometer), and the knob itself compresses 2dp vertically while dragging.
- **Toggle = flip switch, not iOS pill.** The alarm on/off toggle is a short rectangular track with hard-cut corners (`radius.sm`, not `radius.full`) and a thick knob that snaps between two outlined states with a sharp 90ms flip plus a single haptic click — closer to a physical rocker switch than a smooth sliding pill.
- **Haptics are mandatory, not decorative.** Every tactile interaction (button press, toggle flip, slider tick, day-chip select, time-wheel minute tick) fires a distinct short haptic (`HapticFeedbackConstants` equivalents: light tick for continuous drag, medium click for discrete toggles, a slightly longer "confirm" buzz for Save/Dismiss).
- **Color does the work color does; shape does the work shape does.** `color.accent.primary` / `secondary` / `glow` are reserved for things the user can press or that are currently active — never used as decoration on static text or backgrounds, so a glance at the screen tells you exactly what's actionable.

---

## 3. Motion & Animation Principles

### 3.1 Motion tokens

| Token | Duration | Easing |
|---|---|---|
| `motion.micro` | 120ms | `FastOutSlowIn` — toggles, checkbox, ripple |
| `motion.standard` | 280ms | `EmphasizedEasing` (cubic-bezier 0.2, 0.0, 0, 1.0) — card expand/collapse |
| `motion.container` | 350–450ms (size-dependent) | `EmphasizedEasing` — full container transform |
| `motion.spring` | — | Spring: `stiffness=400, dampingRatio=0.8` — drag-release, snooze slider, FAB morph |

### 3.2 The Container Transform pattern (core interaction)

This is the single most important pattern in the app and replaces page navigation everywhere it's plausible:

- **Trigger element** (e.g. an alarm card, the FAB) and **destination container** (e.g. the alarm editor) are treated as *the same element* animating through a shape/size/position change — not two separate screens cross-fading.
- Implementation: Compose's `SharedTransitionLayout` + `AnimatedContent`, animating `bounds`, `corner radius`, and `background color` simultaneously. (If using classic Views: `MaterialContainerTransform` from Material Components Android — supported out of the box via Gradle dependency `com.google.android.material:material`.)
- Sequence for "tap an alarm card to edit it":
  1. Card is at its list position/size, `radius.md`, `layer.raised`.
  2. On tap: background scrim fades in behind (`200ms`), the card's bounds interpolate to fill ~92% of the screen width, anchored where it started, corner radius eases to `radius.lg`, elevation steps to `layer.floating`.
  3. Content inside cross-fades/slides in only *after* the container reaches ~70% of its final size (staggered reveal — prevents the illegible "squished content" look).
  4. Collapse is the exact reverse, driven by a drag-down gesture or close button, interruptible at any point (user can flick it closed mid-animation).
- The **same transform** is reused for: alarm card → editor, FAB → new-alarm editor, settings row → settings detail, label chip → label picker.

### 3.3 Supporting motion rules

- **No instant appearance/disappearance.** Every container that overlays content enters via scale(0.94→1)+fade+slight upward translate (12dp), never a flat fade or a hard cut.
- **Shared element continuity:** when a value changes location (e.g., the alarm time moves from the big display into a compact card after saving), that value itself animates across the position change rather than disappearing in place A and appearing in place B.
- **Physical dismissal:** sheets and the ringing screen respond to drag with 1:1 finger tracking and a velocity-based spring release (snap back vs. complete dismissal), never a binary swipe threshold with no visual feedback.
- **Interruptibility:** every enter animation can be reversed mid-flight if the user taps away or drags back; never block input during a transition longer than `motion.micro`.
- **Reduced motion:** honor `Settings > Accessibility > Remove animations` (and Android's system "remove animations" setting) by collapsing all `motion.container`/`motion.standard` transitions to simple 100ms cross-fades.

---

## 4. Navigation Model

- **Single-activity architecture.** One `Activity` hosts a single root composable. There is no multi-screen back-stack navigation for primary flows.
- Three *states* layered on the home surface, never separate "pages":
  1. **Base layer** — Alarm List (always present underneath everything).
  2. **Floating containers** — Add/Edit Alarm, Alarm Details, Settings Panel, Snooze-duration picker. These open *on top of* the base layer via container transform and always show enough of the dimmed base layer behind them to maintain spatial context.
  3. **Critical overlay** — Alarm Ringing screen. This is the one state allowed to take over the full screen edge-to-edge (system requirement: must show over lock screen), but it still animates in via scale+fade from the status bar, not a hard screen swap, and the clock face it shows is the same shared element as the list's header clock.
- Settings, Sounds picker, and Snooze config are **nested containers**: Settings opens as a floating container from a row tap; tapping a sub-row (e.g. "Default snooze length") morphs *that row* into a smaller floating sub-container above it — a container can open another container, each dismissing independently and in reverse order (stack of transforms, not a stack of pages).
- Back gesture / back button always reverses the most recently opened container transform rather than navigating "back" to a different page.

---

## 5. Screens & Components

### 5.1 Home — Alarm List

- Header: large digital clock (`type.displayTime`, tabular numerals, updates live) + date, top-aligned, generous 32dp top padding below status bar.
- Below: vertical list of **Alarm Cards**, 16dp horizontal margin, 12dp vertical gap (never touching, never overlapping even during list reordering/drag).
- **Alarm Card** (`layer.raised`, `radius.md`, `color.bg.surface`):
  - Left: time (`type.alarmTime`) + label below in `type.caption`/`color.text.secondary`.
  - Repeat-day chips (Mon–Sun, single-letter, `radius.full`, 24dp) render inline beneath the time, selected days filled with `color.accent.primary`, others outlined — truncated to a single row with a "+2" overflow chip rather than wrapping, to guarantee no vertical overlap with the toggle.
  - Right: a pill-shaped toggle switch (`radius.full`), minimum 12dp clearance from the card's edge and from the time text.
  - Disabled alarms: entire card drops to 55% opacity and `color.text.disabled`, toggle track desaturates — no modal needed to communicate state.
  - Swipe-left reveals a **Delete** action as a `color.danger` panel that slides out from under the card (the card itself shifts left, revealing the action behind it — again, a container reveal, not an overlapping floating button).
- **FAB** (bottom-right, 56dp circle, `color.accent.primary`, hard outline + offset shadow per §3A, `+` icon only — no label): this *is* the trigger element for the container-transform into the Add Alarm editor (see 5.2). It is the sole way to reach "add alarm"; no secondary toolbar button, to avoid redundant competing CTAs.
- Empty state (no alarms): a single icon, no caption. The FAB is the only affordance; it needs no sentence pointing at it.

**Copy rule for this whole app:** no instructional sentences, no subtitles that restate the screen's title, no "Tap + to add an alarm" style hints. Labels are single words or numerals wherever possible (e.g. toggle has no "On/Off" text at all — state is shown by color + knob position only). If a label can be inferred from an icon or position, the label is removed.

### 5.2 Add / Edit Alarm (floating container)

- Opens via container transform from either the FAB (new alarm, expands from bottom-right corner) or an Alarm Card (edit, expands from the card's exact list position).
- Layout inside the expanded container, top to bottom, each section separated by 24dp (never by hairlines unless density is very high):
  1. **Time wheel** — large scrollable hour/minute drum picker, `type.displayTime` scale, haptic tick per minute. Centered, generous side padding so it never visually competes with the close button.
  2. **Repeat** — 7 day-chips in one row (`radius.full`, 40dp), multi-select, tap toggles fill color with a `motion.micro` scale-bounce (1.0→1.15→1.0).
  3. **Label** — single-line text field, `radius.sm`, expands height by 1 line max before scrolling internally (never pushes other sections down unexpectedly — fixed max-height with internal scroll).
  4. **Sound & Vibration** — a row that itself is a mini container-transform trigger: tapping it morphs into a **sub-container** (sound picker list) that slides up from that row's position, dims the editor behind it, and collapses back into the row on selection. Icon + sound name only, no "Choose a sound" heading.
  5. **Snooze duration** — a physical stepper (tactile `−` / numeral / `+`, per §3A), numeral only, no "minutes" unit spelled out beyond a trailing "m" in `type.ticker`.
- Bottom-anchored **Save** button (full-width minus 16dp margins, `radius.sm`, `color.accent.primary`, hard outline + offset shadow, single word "SAVE" in `type.button`) — always visible, never requires scrolling to reach (pin to bottom of the container, content scrolls independently above it). Press collapses the shadow per §3A before the container-collapse transition starts, so the tap itself feels like it physically triggers the close.
- Close affordance: drag-handle bar at top of the container (standard bottom-sheet convention) *and* tap-outside-to-dismiss-with-save-prompt-if-dirty. Dismissal reverses the exact entry transform back into the originating card/FAB.

### 5.3 Alarm Ringing (critical overlay)

- Full-bleed `color.bg.base` (dark regardless of theme, for glare reduction), large shared-element clock continues from the list header. Label shown only if one was set — no placeholder text if it wasn't.
- Two tactile buttons (§3A: hard outline, offset shadow, press-to-sink feedback) stacked with 16dp gap, each full 64dp height — **SNOOZE** (`color.accent.glow` fill) above **DISMISS** (outlined, transparent fill) — single all-caps words, no icons needed alongside them. For one-handed use, Dismiss can instead be a short physical slide-track (same grooved-channel treatment as the slider in §3A) with Snooze as the button above it.
- A looping ambient glow (radial pulse behind the clock in `color.accent.glow`, 2000ms ease-in-out loop) communicates "live/ringing" state — this is the one place the glow accent is used purely atmospherically, since the whole screen's job is to signal "ringing."
- Dismiss transition: the whole screen scale-fades down and tucks back toward the status bar/clock origin (reverse of how it entered), landing back on the Alarm List with that alarm's card doing a brief highlight pulse.

### 5.4 Settings (floating container)

- Opens from a persistent icon in the Home header (top-right), using the same container-transform language — expands from the icon's position, not a slide-in drawer.
- Grouped rows (Theme, Snooze, Sound, Motion, About — single words as headers, no descriptive subtext under any of them). Each row that leads to more options opens a nested sub-container exactly as in 5.2.5, never a push-navigation to a new full page.

---

## 6. Component Specs Summary

| Component | Shape/Radius | Elevation layer | Tactile press behavior | Entry motion |
|---|---|---|---|---|
| Alarm Card | `radius.md`, 1.5dp outline | `layer.raised` | shadow collapses + 2dp sink on tap | n/a (resident) |
| Toggle switch | `radius.sm` track (flip-switch, not pill) | flat on card, outlined | 90ms hard flip + haptic click | n/a (resident) |
| FAB | `radius.full` (circle), 2dp outline | `layer.floating` while morphing | sinks on press before morph begins | morphs into editor via container transform |
| Button (Save/Snooze/Dismiss) | `radius.sm`, 2dp outline | `layer.raised` → shadow 0 on press | full §3A press-to-sink + spring release | — |
| Slider (snooze/time) | grooved track, outlined round knob | `layer.raised` | knob compresses 2dp while dragging, haptic tick per step | — |
| Add/Edit container | `radius.lg` | `layer.floating` | n/a | `motion.container`, origin = trigger element bounds |
| Sub-pickers (sound, snooze) | `radius.lg` top corners only (bottom-sheet-like) | `layer.floating` (+1 over parent) | n/a | slide-up + fade, 280ms |
| Ringing overlay | none (full-bleed) | `layer.critical` | n/a | scale+fade from clock origin, 350ms |
| Scrim | — | behind `layer.floating`/`critical` | n/a | fade 0→45% outline-tint, 200ms, always under the container it supports |

---

## 7. Accessibility

- Minimum contrast 4.5:1 for all text tokens against their intended background in both themes.
- All touch targets ≥48dp with ≥12dp clearance, verified above for every list/card layout (prevents accidental overlap taps on dense screens).
- `Settings > Reduce Motion` collapses every container-transform to a 100ms cross-fade; no information is conveyed by motion alone (state is always also reflected in a static label/color).
- TalkBack: container transforms announce the destination's title on completion ("Editing alarm, 7:00 AM"); scrims are marked non-focusable so focus moves directly into the opened container.
- Snooze/Dismiss buttons on the ringing screen remain the two largest touch targets on screen and are never time-gated or require precision gestures.

---

## 8. Technical Notes (Gradle / Android)

- **Build:** standard Android Gradle Plugin project; module split suggested as `:app`, `:design-system` (tokens, theme, shared composables), `:feature-alarm-list`, `:feature-alarm-editor`, `:feature-ringing`.
- **Key dependencies** (via `build.gradle.kts`):
  - `androidx.compose.*` (BOM-managed) for UI + `androidx.compose.animation:animation` for `SharedTransitionLayout`/`AnimatedContent`.
  - `com.google.android.material:material` only if any classic Views are retained (provides `MaterialContainerTransform` as a fallback/reference implementation).
  - `androidx.core:core-splashscreen` for a themed, non-jarring cold-start.
  - `androidx.work:work-runtime-ktx` + `AlarmManager`/`setExactAndAllowWhileIdle` for actual alarm scheduling (UI concern only insofar as the ringing screen must launch as a full-screen intent over the lock screen — requires `USE_FULL_SCREEN_INTENT` handling and a dedicated trampoline activity that still participates in the shared clock element where possible).
- **Theming:** define all Section 2 tokens as a Compose `CompositionLocal`/`MaterialTheme` extension (`AlarmAppTheme`) rather than hardcoding values in composables, so Palette A/B and "reduce motion" propagate app-wide from one source of truth.
- **Fonts:** Cabinet Grotesk, Departure Mono, and General Sans are not system fonts — bundle them as variable/static `.ttf`/`.otf` files under `:design-system/src/main/res/font/`, declare a font family XML or `FontFamily(Font(...))` in Compose, and confirm each family's license permits app bundling (all three have free-for-commercial-use licenses as of writing, but verify before shipping). Register them once in `AlarmAppTheme.Typography` — never reference a font file path from a feature module directly.
- **Testing motion:** keep all durations/easings as named tokens (Section 3.1) in one `Motion.kt` object so QA and design can audit timing without hunting through composables.

---

## 9. Summary of the "No Page-Switch" Rule in Practice

Every navigation-like action in this app maps to a container behavior, not a route:

| User intends to... | Old pattern (avoided) | This app's pattern |
|---|---|---|
| Add an alarm | Navigate to "Add Alarm" screen | FAB morphs into the editor container |
| Edit an alarm | Push a new screen | Tapped card expands in place into the editor |
| Pick a sound | Navigate to "Sound" screen | Sound row morphs into a sub-container list |
| Open settings | Navigate to "Settings" screen | Icon morphs into the settings container |
| Alarm fires | Launch a separate full-screen Activity with a cut transition | Full-screen state fades/scales in from the shared clock element |
| Go back | Back-stack pop | Reverse the active container transform |

This keeps the user's mental model to a single continuous surface at all times, which is the core of the "modern, minimalistic, no overlapping, smooth, container-based" brief.
