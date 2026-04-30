# Geodata — Flutter App Setup Plan

This plan covers building the **Windows + Android + iOS** apps with Flutter,
sharing the existing Supabase backend with the React web app.

> **Context:** the web app stays in React. The backend is an existing Supabase
> project (Postgres + PostGIS). This plan only covers the Flutter side.
> I (Marius) have never used Flutter or Dart before, so explain non-obvious
> choices as you go.

---

## Architecture

```
┌─────────────────────────────────────────────┐
│  Supabase (Postgres + PostGIS + Auth + Storage)
│  - Row-Level Security (RLS) for permissions
│  - Spatial queries via Postgres RPC functions
└──────────────┬──────────────────────────────┘
               │ HTTPS
       ┌───────┴────────┬──────────────┐
       │                │              │
   React web        Flutter app     (same Flutter
   (existing)    Windows/Android/    binary, all 3
                       iOS           targets)
```

**Key decisions already made:**
- Flutter for native (Windows + Android + iOS), one codebase
- React stays for web (do not touch the web project)
- Supabase + PostGIS for backend (already exists)
- `flutter_map` + OpenStreetMap tiles (free, no vendor lock-in)
- Spatial queries go through Postgres RPC functions, not direct table queries
- Riverpod for state management — but only when `setState` actually hurts
- iOS builds require a Mac; develop on Windows, build for iOS later

---

## Phase 0 — Toolchain (do this first, no code yet)

Goal: `flutter doctor` shows all green checkmarks for Windows + Android.
iOS will stay red until a Mac is available — that's fine, ignore it.

1. Install Flutter SDK (Stable channel) — extract to `C:\src\flutter`,
   add `C:\src\flutter\bin` to PATH.
2. Install Visual Studio 2022 Community with the
   **"Desktop development with C++"** workload (needed for Windows builds).
3. Install Android Studio. Open it once, accept SDK licenses, install an
   emulator image (any recent Pixel + API 34+).
4. Install VS Code + the **Flutter** and **Dart** extensions.
5. Run `flutter doctor` and fix every issue except iOS-on-Mac.
6. Run `flutter doctor --android-licenses` if prompted.

**Checkpoint:** `flutter doctor` is clean (Windows + Android sections green).

---

## Phase 1 — Hello World on every target

Goal: the default counter app runs on Windows and Android. Don't connect
to Supabase yet. This is purely about confirming the toolchain.

1. From the repo root: `flutter create --platforms=windows,android,ios mobile`
2. `cd mobile`
3. `flutter run -d windows` — confirm the counter app opens.
4. Start the Android emulator. `flutter run -d <emulator-id>` — confirm it
   runs on Android. `flutter devices` lists available targets.
5. Commit: `chore(mobile): scaffold flutter project`.

**Checkpoint:** the unmodified counter app runs on Windows AND Android.
Stop here if either is broken. Do not continue until both work.

---

## Phase 2 — Supabase connection

Goal: fetch one row from one existing table and print it.

1. `flutter pub add supabase_flutter`
2. `flutter pub add flutter_dotenv` (or use `--dart-define`, choose one).
3. Create `mobile/.env` with `SUPABASE_URL=...` and `SUPABASE_ANON_KEY=...`.
4. Add `.env` to `mobile/.gitignore`. **Never commit keys.**
5. Add `assets: [.env]` to `pubspec.yaml`.
6. In `main.dart`:

   ```dart
   import 'package:flutter/material.dart';
   import 'package:flutter_dotenv/flutter_dotenv.dart';
   import 'package:supabase_flutter/supabase_flutter.dart';

   Future<void> main() async {
     WidgetsFlutterBinding.ensureInitialized();
     await dotenv.load(fileName: '.env');
     await Supabase.initialize(
       url: dotenv.env['SUPABASE_URL']!,
       anonKey: dotenv.env['SUPABASE_ANON_KEY']!,
     );
     runApp(const MyApp());
   }

   final supabase = Supabase.instance.client;
   ```

7. On a test screen, call:
   ```dart
   final rows = await supabase.from('your_table').select().limit(1);
   print(rows);
   ```
8. Confirm it prints data from the existing DB. RLS policies must allow the
   anon role to read, OR add Supabase auth in Phase 3.

**Checkpoint:** Flutter app reads from the existing Supabase DB.

---

## Phase 3 — Auth (only if the app needs login)

Skip if the app is read-only and public. Otherwise:

1. Use `supabase.auth.signInWithPassword(...)` or magic link.
2. Wrap the app in a listener for auth state (`onAuthStateChange`).
3. Make sure RLS policies match what the React web app already does — the
   policies live in the database, so both clients automatically share them.

---

## Phase 4 — Map + first feature

Goal: render OSM tiles and plot real data from Supabase as markers.

1. `flutter pub add flutter_map latlong2`
2. Render a `FlutterMap` widget centered on Moldova
   (lat ~47.0, lng ~28.85) with the OSM tile layer.
3. Make sure `userAgentPackageName` is set on the tile layer
   (OSM tile policy requires it).
4. Write a Postgres RPC function for spatial queries — example:

   ```sql
   create or replace function features_in_bbox(
     min_lng float, min_lat float,
     max_lng float, max_lat float
   ) returns setof features as $$
     select * from features
     where geom && ST_MakeEnvelope(min_lng, min_lat, max_lng, max_lat, 4326);
   $$ language sql stable;
   ```
5. From Flutter: `await supabase.rpc('features_in_bbox', params: {...})`.
6. Convert results to `Marker` widgets on the map.

**Checkpoint:** real data from the DB is visible on the map on Windows
and Android.

---

## Phase 5 — Iterate on real features

Now the foundation is done. Add features one at a time:
- Layer toggling
- Drawing/editing geometry
- Offline tile caching (consider switching to `maplibre_gl` if needed)
- Search / filter
- Forms for adding records
- Photos via Supabase Storage

---

## Phase 6 — iOS build (when Mac is available)

1. On a Mac with Xcode, clone the repo and run `flutter build ios`.
2. Set up an Apple Developer account ($99/yr) for TestFlight / App Store.
3. Cloud option if no Mac: Codemagic or GitHub Actions macOS runners.

---

## Conventions

- **Folder structure inside `lib/`:** start flat. Once you have ~10 files,
  split into `lib/features/<feature>/` and `lib/core/` (shared stuff).
- **Naming:** snake_case files, PascalCase classes, camelCase members
  (Dart standard).
- **State:** `setState` until it hurts, then Riverpod.
- **Don't optimize early.** Get the vertical slice working first.

---

## What NOT to do

- Don't commit `.env`, `*.keystore`, or any Supabase keys.
- Don't touch the React web app or the Spring Boot backend folders.
- Don't introduce a state management library before the first feature
  ships — `setState` is fine to start.
- Don't try to share Dart code with the React app. Share the **schema**
  (via Supabase) instead.
- Don't use `google_maps_flutter` unless explicitly asked — costs money,
  needs API keys, vendor lock-in.

---

## Definition of done for the initial setup

- `flutter doctor` clean on Windows + Android
- `mobile/` folder exists, scaffolded by `flutter create`
- App runs on Windows and Android emulator
- App reads at least one row from the existing Supabase DB
- A map renders with at least one real marker from the DB
- README in `mobile/` explains how to run the project locally
