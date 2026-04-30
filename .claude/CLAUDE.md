# Instructions for Claude Code

You are helping Marius build the **Flutter** part of the Geodata project.
Read `SETUP_PLAN.md` (in the repo root) for the full plan and execute it
step by step. This file is the standing instructions; the plan is the
roadmap.

## Project at a glance

- **Goal:** cross-platform engineering maps app (Windows + Android + iOS).
- **This folder (`mobile/`):** Flutter app, all 3 native targets.
- **Sibling folders:** existing React web app + Supabase backend.
  **Do not modify them.**
- **Backend:** existing Supabase project (Postgres + PostGIS).
- **Map library:** `flutter_map` + OpenStreetMap.
- **Marius's background:** strong Java/Spring Boot, React/TypeScript, C#.
  **New to Flutter and Dart** — explain non-obvious Dart/Flutter choices.

## How to work with Marius

- **One step at a time.** Don't dump 5 phases of changes in one go.
  Finish a checkpoint, confirm it works, then move on.
- **Run `flutter doctor` early** and report what's missing before writing
  code. The toolchain has to be green first.
- **Prefer the terminal** for `flutter` commands. Show the command,
  run it, show the output. Don't just describe what would happen.
- **Use `--platforms=windows,android,ios`** when scaffolding. Web is
  intentionally excluded (handled by the separate React app).
- **Test on Windows first** — fastest iteration, no emulator boot time.
  Then verify on Android emulator. iOS comes later (needs a Mac).
- **When you hit something Dart-specific** that differs from
  Java/TS/C# (null safety, `late`, `Future` vs Promises, named
  parameters, mixins), call it out briefly so Marius learns as you go.

## Hard rules

1. **Never commit secrets.** `.env`, Supabase keys, signing keys, keystores
   — all gitignored. If you see one being added, stop and warn.
2. **Don't touch `web/` or `backend/`** (or whatever the React/Spring
   folders are called). This Flutter project is isolated.
3. **No `google_maps_flutter`** unless Marius explicitly asks. Use
   `flutter_map` (free, OSM, no API key).
4. **No state management library** (Riverpod, Bloc, Provider) until
   `setState` actually causes pain. Start simple.
5. **Spatial queries go through Postgres RPC functions**, not raw
   `.from('table').select()` with manual lat/lng filtering. PostGIS
   does the spatial work; Flutter just calls the RPC.
6. **Don't reformat or refactor existing code** that's not part of the
   current task. Stay focused on the step at hand.
7. **Don't write giant files.** If a Dart file passes ~300 lines, split it.

## Conventions

- **Files:** `snake_case.dart`. **Classes:** `PascalCase`.
  **Members:** `camelCase`.
- **Folder layout in `lib/`:** start flat. Once there are ~10 files,
  split into `lib/features/<name>/` and `lib/core/`.
- **Async:** prefer `async`/`await` over `.then(...)` chains.
- **Null safety:** lean on it. Avoid `!` (bang operator) except when
  truly unavoidable; prefer `?.`, `??`, and `if (x != null)` checks.
- **Imports:** package imports before relative imports, alphabetized.

## Definition of done for any task

- Code compiles (`flutter analyze` clean — zero warnings).
- App still runs on Windows.
- Any new dependency is added via `flutter pub add`, not edited into
  `pubspec.yaml` by hand.
- Secrets are not in the diff.
- A short note in the chat explains what changed and why.

## When unsure

Ask Marius. Don't guess about:
- The existing DB schema (read it from Supabase or ask).
- Which table or column maps to a feature.
- Whether to add auth now or later.
- Whether a feature should be Windows-first or mobile-first.

## Starting point

If this is the first session: read `SETUP_PLAN.md`, then run
`flutter doctor` and report the output. Do nothing else until that's
reviewed.
