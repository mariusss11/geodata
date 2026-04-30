# CI/CD Pipeline - Complete Guide

This document explains the complete CI/CD setup for the Geodata project, including the testing phase before merging.

## Overview

The CI/CD pipeline ensures code quality, security, and functionality before merging to `main`.

```
┌─────────────┐
│ Push / PR   │
└──────┬──────┘
       │
       ├─→ [PR Checks] ──────────→ Security, Secrets, File Size
       │       │
       │       └─→ ✅ Pass
       │
       ├─→ [Flutter Tests] ──────→ Unit + Widget Tests
       │       │
       │       └─→ ✅ Pass + Coverage
       │
       ├─→ [Backend Tests] ──────→ Unit + Integration Tests
       │       │
       │       └─→ ✅ Pass + Coverage
       │
       └─→ [Approval] ───────────→ Manual review
               │
               └─→ 🟢 Merge to main
                      │
                      ├─→ [Docker Build] → Push to GHCR
                      ├─→ [Android Build] → APK/AAB Release
                      └─→ [Deploy] → Kubernetes (optional)
```

## Phase 1: Pre-merge Checks (Required)

### 1.1 PR Checks Workflow (`.github/workflows/pr-checks.yml`)

Runs on every pull request:

✅ **Secrets Detection**
- Scans for hardcoded credentials, API keys, passwords
- Uses TruffleHog for deep scanning
- Fails if secrets found

✅ **File Size Check**
- Rejects files >50MB
- Prevents accidentally committing binaries or large assets

✅ **Kubernetes Validation**
- Validates YAML syntax
- Checks for placeholder values (TODO, FIXME)

✅ **Commit Message Validation**
- Suggests conventional commits (feat:, fix:, etc.)
- Improves changelog generation

**Expected Duration:** ~2-3 minutes

**View Results:**
```
GitHub → Pull Requests → [Your PR] → Checks tab
```

### 1.2 Flutter Tests Workflow (`.github/workflows/flutter-test.yml`)

Runs on:
- Every push to `main` or `develop`
- Every PR to `main` or `develop`
- Changes to `mobile/` directory

**Tests Executed:**

```bash
flutter analyze          # Static analysis
flutter test --coverage  # Unit + Widget tests
```

**Coverage:** Targets 80%+ on key modules

**Artifacts:** Coverage report uploaded to Codecov

**Expected Duration:** ~5-8 minutes

**Exit Criteria:**
- ✅ No analyzer warnings
- ✅ All tests pass
- ✅ Code coverage maintained

### 1.3 Backend Tests Workflow (`.github/workflows/backend-test.yml`)

Runs on:
- Every push to `main` or `develop`
- Every PR to `main` or `develop`
- Changes to `backend/` directory

**Tests Executed:**

```bash
mvn clean compile           # Compile check
mvn test                    # Unit tests (H2 in-memory DB)
mvn verify                  # Integration tests
```

**Services Tested:**
- `identity-service`
- `map-service`
- `borrow-service`

**Expected Duration:** ~8-12 minutes

**Exit Criteria:**
- ✅ All 3 services compile
- ✅ Unit tests pass (100%)
- ✅ Integration tests pass
- ✅ No test flakes

### 1.4 Test Status in GitHub

**View test results:**

```
GitHub → [PR/Branch] → Actions tab → Workflow name
```

**Merge Requirements:**

All of these must be green (✅):
- ✅ Flutter Tests
- ✅ Backend Tests
- ✅ PR Checks

**Branch Protection:**

To enforce testing before merge, configure in GitHub:

```
Settings → Branches → [main] → Branch protection rules

Required:
✅ "Require status checks to pass before merging"
   ✅ flutter-test
   ✅ backend-test
   ✅ pr-checks

✅ "Require code review from 2 people"
✅ "Require branches to be up to date before merging"
✅ "Require conversation resolution before merging"
```

## Phase 2: Post-merge Actions

### 2.1 Docker Build Workflow (`.github/workflows/docker-build.yml`)

Triggers automatically after tests pass on `main` branch.

**Builds:**
- `identity-service:latest`
- `map-service:latest`
- `borrow-service:latest`

**Pushes to:** GitHub Container Registry (GHCR)
- `ghcr.io/username/identity-service:latest`
- `ghcr.io/username/identity-service:v1.0.0` (on tag)

**Expected Duration:** ~15-20 minutes per service

**View Results:**

```
GitHub → Actions → Docker Build & Push
```

### 2.2 Android Build Workflow (`.github/workflows/android-build.yml`)

Triggers on `main` branch push or tag creation.

**Builds:**
- APK (dev environment)
- APK (prod environment)
- App Bundle (prod, for Google Play)

**Outputs:**
- `app-release.apk` (dev)
- `app-release-prod.apk` (prod)
- `app-release.aab` (Play Store)

**Expected Duration:** ~10-15 minutes

**Download Artifacts:**

```
GitHub → Actions → Android Build → Artifacts → Download
```

## Testing Locally Before Push

### Run Tests Locally (Recommended)

```bash
# Mobile
cd mobile
flutter test --coverage

# Backend
cd backend
./mvnw clean verify

# Both
cd ..
echo "=== Mobile ===" && flutter test && echo "=== Backend ===" && ./mvnw verify
```

### Pre-commit Hook (Optional)

Create `.git/hooks/pre-commit`:

```bash
#!/bin/bash
set -e

echo "Running pre-commit tests..."

# Flutter tests
cd mobile
flutter test --no-pub || exit 1
cd ..

# Backend tests
cd backend
./mvnw test -q || exit 1
cd ..

echo "✅ All tests passed!"
```

Make executable:
```bash
chmod +x .git/hooks/pre-commit
```

## Test Requirements for Merge

### Flutter Tests Must Pass

```
✅ flutter analyze (0 warnings)
✅ All unit tests pass
✅ All widget tests pass
✅ Coverage maintained (80%+)
```

### Backend Tests Must Pass

```
✅ mvn clean compile (no errors)
✅ All unit tests pass
✅ All integration tests pass
✅ All 3 services compile
```

### PR Checks Must Pass

```
✅ No secrets found
✅ No large files (>50MB)
✅ K8s manifests valid
✅ Commit messages formatted
```

## Troubleshooting Failed Tests

### Flutter Test Fails

**Problem:** `flutter analyze` shows warnings

```bash
# Fix: Run locally and fix issues
cd mobile
flutter analyze
# Fix issues in code
git add .
git commit -m "fix: resolve analyzer warnings"
git push
```

**Problem:** Widget tests timeout

```dart
// Add timeout parameter
testWidgets('...', (tester) async {
  // ...
}, timeout: Timeout(Duration(seconds: 30)));
```

### Backend Test Fails

**Problem:** Integration test needs database

```bash
# Run with PostgreSQL
docker-compose up postgres

./mvnw -pl identity-service verify \
  -Dspring.datasource.url=jdbc:postgresql://localhost:5432/postgres
```

**Problem:** Test is flaky (intermittent failures)

```java
@Test
@Retry(3)  // Retry up to 3 times
public void testUnstableOperation() {
  // ...
}
```

### PR Check Fails

**Problem:** Secrets detected

```bash
# Remove sensitive data
git rm --cached path/to/secret
git commit -m "remove: delete secrets file"

# Add to .gitignore
echo "secrets.json" >> .gitignore
git add .gitignore
git commit -m "chore: add secrets to gitignore"
```

## Monitoring & Metrics

### GitHub Actions Dashboard

```
GitHub → Actions → Workflows
```

Monitor:
- Test pass rate (should be 100%)
- Average execution time
- Failure trends

### Code Coverage

```
GitHub → [Branch] → Codecov badge
```

Targets:
- Mobile: 80%+
- Backend: 75%+

### Test Flakiness

Track tests that intermittently fail:

```bash
# Run tests 10 times
for i in {1..10}; do
  flutter test test/flaky_test.dart || echo "Failed on run $i"
done
```

## Best Practices

### Before Merging

1. ✅ Run tests locally first
2. ✅ Review coverage reports
3. ✅ Check CI results are green
4. ✅ Code review approval
5. ✅ All conversations resolved

### Writing Tests

1. ✅ Write tests **before** or **with** code
2. ✅ Aim for meaningful coverage (not just numbers)
3. ✅ Mock external dependencies
4. ✅ Use descriptive test names
5. ✅ Keep tests independent

### Maintaining Tests

1. ✅ Fix failing tests immediately
2. ✅ Update tests when code changes
3. ✅ Remove skipped/disabled tests (with reason)
4. ✅ Review coverage trends monthly
5. ✅ Refactor test code regularly

## Deployment to Production

### Manual Approval Required

CI/CD stops at merge. To deploy to production:

1. Create release tag: `v1.0.0`
2. Push tag: `git push origin v1.0.0`
3. Docker builds automatically
4. APK/AAB created automatically
5. Manual review of release notes
6. Manual push to Google Play Store

## Emergency Procedures

### Quick Fix (Hotfix)

For critical production issues:

```bash
# Create hotfix branch
git checkout -b hotfix/critical-bug main

# Fix + test locally
git add .
git commit -m "fix: critical issue"

# Push & create PR
git push origin hotfix/critical-bug

# All tests must pass before merge
```

### Rollback Strategy

If production deployment fails:

```bash
# Revert commit
git revert <commit-hash>
git push

# Or rollback Docker image
kubectl set image deployment/identity-service \
  identity-service=ghcr.io/user/identity-service:v1.0.0-previous
```

## Performance Optimization

### Faster Local Testing

```bash
# Run only specific directory
flutter test test/models/

# Run in parallel (backend)
./mvnw test -T 1C
```

### Faster CI Runs

- Cache dependencies (already configured)
- Run tests in parallel (GitHub Actions)
- Skip expensive tests in CI if needed

## Security

### Secrets Management

Never commit secrets. Use GitHub Secrets:

```
Settings → Secrets and variables → Actions → New repository secret
```

Then reference in workflows:

```yaml
- name: Use secret
  run: echo ${{ secrets.MY_SECRET }}
```

### Branch Protection

Enforce testing before merge:

```
Settings → Branches → Add rule
✅ Require status checks to pass
✅ Require code reviews
✅ Dismiss stale reviews
```

---

## Quick Reference

| Workflow | Trigger | Time | Exit Criteria |
|----------|---------|------|---------------|
| PR Checks | Every PR | 2-3m | No secrets, valid K8s |
| Flutter Tests | PR/push | 5-8m | Tests pass, coverage 80%+ |
| Backend Tests | PR/push | 8-12m | Tests pass, all services compile |
| Docker Build | Merge to main | 15-20m | Images pushed to GHCR |
| Android Build | Merge to main | 10-15m | APK/AAB built, artifacts ready |

---

For questions or issues, see:
- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [Flutter Testing](https://docs.flutter.dev/testing)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
