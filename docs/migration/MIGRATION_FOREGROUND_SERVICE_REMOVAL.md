# Migration Guide: Foreground Service Permission Removal

## Summary

Starting with SDK version 3.1.2, Chat SDK UI no longer declares foreground service permissions. Audio pre-caching will continue to work normally.

## Migration Steps

### 1. Update Dependency

Update to the new SDK version in your module's Gradle build file (`build.gradle` or `build.gradle.kts`):

```groovy
implementation("com.nice.cxone:chat-sdk-ui:3.1.2")
```

### 2. Remove Manifest Workarounds (if present)

If you previously added these to your `AndroidManifest.xml`, remove them:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" tools:node="remove" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" tools:node="remove" />
```

### 3. Update Google Play Console (if applicable)

If you declared "Audio message pre-caching" foreground service usage, remove it:
- **App content** → **Foreground services** → Remove "Audio message pre-caching"

### 4. Rebuild and Test

```bash
./gradlew clean assembleRelease
```

Test that audio message playback works normally.

## Verification

- ✅ Audio messages play without delay
- ✅ No manifest merge conflicts
- ✅ No Google Play Console warnings (allow 24-48h after upload)
