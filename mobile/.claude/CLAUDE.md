# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

# RTK Token Optimization

## Unhandled Commands — Manual Optimization

RTK doesn't yet optimize these commands, so use these patterns to reduce token cost:

### Flutter/Dart Commands (28+ flutter analyze calls detected)

**flutter analyze** — Remove stderr redirect unless debugging:
  ```bash         
  # ❌ Avoid: flutter analyze 2>&1                                              
  # ✅ Use: flutter analyze

  flutter devices / flutter doctor — Run only when needed, not in loops:        
  # Batch device checks: run once, reuse output for multiple tasks
  flutter devices                                                               
  flutter doctor                                                                
                
  flutter pub get — Use with quiet flag:                                        
  flutter pub get --quiet                                                       
                         
  dart analyze — Analyze specific files instead of entire lib/:                 
  # Instead of: /snap/bin/dart analyze lib/                                     
  # Use: /snap/bin/dart analyze lib/specific_file.dart
  ```
                                                                                
  Maven Commands (mvnw compile — 9 calls detected)

  ```bash                                             
  # Skip tests to save time/tokens:
  ./mvnw compile -q -DskipTests                                                 
                  
  # Or just run specific modules:                                               
  ./mvnw compile -q -pl module-name
  ```

### Git Commands

# git ls-files: use shorter flag syntax
git ls-files --others --exclude-standard

# git rm: batch operations when possible
git rm -r --cached path/  # Run once, not in loop

# git reset: specify exact commit
git reset --soft HEAD~3  # More explicit than searching

### GitHub CLI

# gh auth: check status only when needed
gh auth status

### Other useful commands

```bash
flutter analyze                        # Lint — must be clean before any task is done
flutter run -d windows                 # Run on Windows (fastest iteration)
flutter run -d emulator-5554           # Run on Android emulator
flutter pub add <package>              # Add a dependency (never edit pubspec.yaml by hand)
flutter test                           # Run all tests
flutter test test/path/to_test.dart    # Run a single test file
```

## Architecture

Services are constructed once in `main.dart` and injected into the widget tree via `AppServices` (`lib/core/app_services.dart`), an `InheritedWidget`. Screens retrieve them with `AppServices.of(context)`. There is no state management library — only `setState` and `InheritedWidget`.

**Layer breakdown:**

| Layer | Location | Responsibility |
|---|---|---|
| Core | `lib/core/` | `ApiClient` (HTTP + auth headers), `ApiConfig` (base URLs per platform), `AuthStore` (JWT persistence via `shared_preferences`), `AppServices` (DI root) |
| Models | `lib/models/` | Plain Dart classes with `fromJson`/`toJson`. No code generation. |
| Services | `lib/services/` | One class per backend microservice; all methods are `async`, throw `ApiException` on HTTP errors |
| Screens | `lib/screens/` | `StatefulWidget` screens; fetch data directly from services obtained via `AppServices.of(context)` |

**Backend microservice ports** (defined in `lib/core/api_config.dart`):

| Service | Port |
|---|---|
| identity-service | 8010 |
| map-service | 8020 |
| borrow-service | 8030 |

Android emulator uses `10.0.2.2` instead of `localhost`; `ApiConfig._host()` handles this automatically.

**Auth flow:** `AuthStore` loads the JWT from `shared_preferences` at startup. `ApiClient` reads `authStore.token` and attaches `Authorization: Bearer <token>` to every request. On login, `AuthService` persists the token and `User` via `AuthStore.save()`. On logout, `AuthStore.clear()` wipes both.

**Pagination pattern:** `MapsService.search()` returns `PagedResponse<T>` with `content`, `hasNext`, and page metadata. Screens implement infinite scroll by tracking `_page` and `_hasNext` locally, loading more on scroll, resetting on search query change.

**Navigation:** flat `Navigator.push` / `Navigator.pushAndRemoveUntil` — no named routes, no go_router. `MapDetailScreen` returns `bool` (true = list needs refresh) via `Navigator.pop(context, true)`.

## Key conventions

- All new dependencies added via `flutter pub add`, not hand-edited into `pubspec.yaml`.
- `ApiException` (from `lib/core/api_client.dart`) is the only typed exception — catch it explicitly in screens.
- Models tolerate both snake_case and camelCase keys from the backend (see `MapItem.fromJson` for examples of defensive `??` chaining).
- Files cap at ~300 lines; split into sub-widgets or helpers before exceeding.

## Workflow Rules                                                                
                  
  1. Never loop analysis commands — Run flutter analyze once at start, reuse    
  output
  2. Batch git operations — Combine related git commands in single session      
  3. Use quiet/short flags — -q, --quiet, --oneline reduce output tokens
  4. Cache tool outputs — If you run a command twice, ask me to reference the   
  previous output instead