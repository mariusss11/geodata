# Android Deployment Guide

This guide covers building and deploying the Geodata Flutter app on Android devices.

## Prerequisites

- Flutter SDK (3.11.5+)
- Android Studio or Android SDK installed
- Java 11+
- A physical Android device or emulator (API 24+)

## Development Build (Testing)

### Run on Android Emulator

```bash
# Start the Android emulator
emulator -avd Pixel_6_API_35

# Run the app in dev mode (uses localhost:8010/20/30)
flutter run -d emulator-5554
```

### Run on Physical Device

1. Enable USB debugging on your Android device
2. Connect device via USB
3. Run:
```bash
flutter devices  # Verify device is listed
flutter run      # App uses 10.0.2.2 for device access to localhost
```

## Production Build

### 1. Update API Endpoints

**Update `lib/core/api_config.dart`:**
```dart
// Change the production host to your actual backend domain
static String _host() {
  switch (_environment) {
    case Environment.prod:
      return 'api.geodata.app'; // Replace with your domain/IP
    case Environment.dev:
      // ...
  }
}
```

### 2. Build APK (Unsigned)

```bash
# Build APK for production (GEODATA_ENV=prod enables HTTPS + prod endpoints)
flutter build apk \
  --dart-define=GEODATA_ENV=prod \
  --release \
  --no-tree-shake-icons

# Output: build/app/outputs/flutter-apk/app-release.apk
```

### 3. Sign APK (Required for Google Play)

**Step 3a: Create a keystore (first time only)**

```bash
keytool -genkey -v -keystore ~/geodata-release.jks \
  -storetype JKS \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias geodata
```

**Step 3b: Sign the APK**

```bash
jarsigner -verbose \
  -sigalg SHA256withRSA \
  -digestalg SHA-256 \
  -keystore ~/geodata-release.jks \
  build/app/outputs/flutter-apk/app-release.apk \
  geodata

# Verify signature
jarsigner -verify -verbose -certs \
  build/app/outputs/flutter-apk/app-release.apk
```

**Step 3c: Zip-align for optimization**

```bash
zipalign -v 4 \
  build/app/outputs/flutter-apk/app-release.apk \
  build/app/outputs/flutter-apk/app-release-aligned.apk
```

### 4. Build Bundle (Google Play)

For publishing to Google Play Store:

```bash
flutter build appbundle \
  --dart-define=GEODATA_ENV=prod \
  --release

# Output: build/app/outputs/bundle/release/app-release.aab
```

## Configuration for Production

### Backend URL

Update your Kubernetes ingress or load balancer to use a consistent domain:

```yaml
# k8s/ingress.yaml
spec:
  rules:
    - host: api.geodata.app
      http:
        paths:
          - path: /api/auth
            backend:
              service:
                name: identity-service
                port: 8010
          - path: /api/maps
            backend:
              service:
                name: map-service
                port: 8020
          - path: /api/borrows
            backend:
              service:
                name: borrow-service
                port: 8030
```

Then update `api_config.dart`:
```dart
case Environment.prod:
  return 'api.geodata.app'; // Your actual domain
```

### SSL/TLS (HTTPS)

1. Enable HTTPS on your Kubernetes ingress:
```yaml
spec:
  tls:
    - hosts:
        - api.geodata.app
      secretName: geodata-tls-cert
```

2. The app automatically uses HTTPS in prod mode (`_protocol()` returns `https`)

## Testing Before Release

### 1. Local Testing with Prod Config

```bash
# Build APK with prod config but test locally
flutter build apk \
  --dart-define=GEODATA_ENV=dev \
  --release

# Install and test
adb install build/app/outputs/flutter-apk/app-release.apk
```

### 2. Firebase Test Lab (Optional)

```bash
firebase test android run \
  --app=build/app/outputs/flutter-apk/app-release.apk \
  --test=test_driver/app.dart \
  --device-ids=Pixel6Pro \
  --os-versions=32
```

## Troubleshooting

### App can't connect to backend

1. Check backend URL in `api_config.dart`:
   ```bash
   # Device will resolve api.geodata.app to your backend IP
   # Ensure DNS or hosts file points to correct IP
   ```

2. Verify certificate (if using HTTPS):
   ```bash
   # Check cert validity
   openssl s_client -connect api.geodata.app:443 -showcerts
   ```

3. Check network permissions in `android/app/src/main/AndroidManifest.xml`:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
   ```

### App crashes on startup

1. Check logs:
   ```bash
   adb logcat | grep "flutter"
   ```

2. Ensure env vars are passed correctly:
   ```bash
   flutter run --dart-define=GEODATA_ENV=prod -v
   ```

## Versioning & Releases

Update version in `pubspec.yaml` before each release:

```yaml
version: 1.0.1+2  # format: major.minor.patch+buildNumber
```

- First number: semantic version (1.0.1)
- Number after `+`: build number (increments for each Android versionCode)

## Continuous Integration (CI)

Example GitHub Actions workflow for auto-building:

```yaml
# .github/workflows/android-build.yml
name: Android Build
on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: subosito/flutter-action@v2
      - run: flutter pub get
      - run: flutter build apk --dart-define=GEODATA_ENV=prod --release
      - run: ./sign-apk.sh  # Custom script using secrets for keystore
      - uses: softprops/action-gh-release@v1
        with:
          files: build/app/outputs/flutter-apk/app-release-aligned.apk
```

## Store Listing Requirements

### Google Play Store

1. **App signing:** Use Play App Signing (Google manages the final cert)
2. **Min SDK:** API 24 (Android 7.0)
3. **Target SDK:** API 35 (as of 2026)
4. **Content rating:** Fill out questionnaire
5. **Privacy policy:** Link required if handling user data
6. **Screenshots:** 2-8 device screenshots (440×660 or 1080×1920)
7. **Description:** ~80 character short description, detailed 4000 char max
8. **Permissions:** Request justification for dangerous permissions (location, etc.)

## Post-Release

1. Monitor crash reports in Google Play Console
2. Check user reviews for issues
3. Use Firebase Crashlytics (optional) for deeper diagnostics
4. Plan updates with 2-week rollout windows

---

For issues or questions, check the Flutter [Android deployment docs](https://docs.flutter.dev/deployment/android).
