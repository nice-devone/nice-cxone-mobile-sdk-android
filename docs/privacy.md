# Data Collection & Privacy Disclosure

Android has no OS-level equivalent of Apple's `PrivacyInfo.xcprivacy` manifest, so this document is the source of
truth for what the CXone Chat SDK (`chat-sdk-core`, `chat-sdk-ui`, and their transitive `logger*`/`utilities`
modules) collects, stores on-device, and transmits. It mirrors the intent of the iOS SDKs' privacy manifests
([UI](https://github.com/nice-devone/nice-cxone-mobile-ui-ios/blob/main/PrivacyInfo.xcprivacy),
[Core](https://github.com/nice-devone/nice-cxone-mobile-sdk-ios/blob/main/PrivacyInfo.xcprivacy)).

> [!IMPORTANT]
> This document covers only data handled by SDK **client code**. It does not cover backend-side retention or
> processing (governed by NICE's contractual/DPA terms), and it is not a substitute for your own app's privacy
> policy or [Google Play Data safety](https://support.google.com/googleplay/android-developer/answer/10787469)
> declaration. Several of the categories below (custom fields, pre-chat survey questions, whether the voice
> message or attachment feature is enabled) are configured by the integrating brand or app, so your app's actual
> collection may be a subset — or, if you add your own custom fields containing PII, a superset — of what's
> listed here.

## Data sent to the CXone backend

| Data | Source | Purpose | Notes |
|---|---|---|---|
| Customer ID, first/last name | `ChatBuilder.setCustomerId()` / `setUserName()` | Identify the visitor to agents | Sent in the customer identity payload (`CustomerIdentityModel`) attached to outbound WebSocket requests (send message, thread actions, etc.); the separate visitor-profile registration call (`createOrUpdateVisitor`) sends a redacted copy without first/last name (`Visitor.redacted()`) |
| OAuth authorization code / PKCE verifier | `ChatBuilder.setAuthorization()` | Server-side authentication of the customer | Only present if the integrating app uses OAuth |
| Custom fields (arbitrary key/value) | `ChatFieldHandler.add()` | Brand-defined visitor/contact attributes | May contain PII (e.g. email, phone) if the brand configures such fields |
| Pre-chat survey answers | Pre-chat survey UI | Route/qualify the conversation | Submitted as custom fields; may include an email field (`FieldDefinition.Text.isEMail`) |
| Chat message text, postback values, attachment file name/URL/MIME type | User input in chat | Core chat functionality | Attachments are uploaded to backend-provided storage |
| Custom analytics event payloads | `CustomVisitorEvent` | Brand-defined analytics/WFA triggers | See [Analytics case study](chat-sdk-core/cs-analytics.md) for the full event catalogue (`PageViewEvent`, `ConversionEvent`, etc.) |
| Country, language | `Locale.getDefault()` | Localization/analytics | Part of `DeviceFingerprint` |
| OS version | `Build.VERSION.RELEASE` | Support/diagnostics | Part of `DeviceFingerprint` |
| OS name ("Android"), device type ("mobile"), app type ("native") | SDK constants | Support/diagnostics | Part of `DeviceFingerprint`; not read from the device, hardcoded defaults |
| Push (FCM) token | `PushListenerService.onNewToken` | Deliver chat push notifications | Forwarded via `Chat.setDeviceToken()` |
| Referrer URL, UTM parameters | Integrator-supplied `Journey` | Marketing attribution | Optional, integrator-controlled |
| Page title/URL | `PageViewEvent` | Analytics/WFA triggers | Optional, integrator-controlled |
| User-Agent, `x-sdk-platform`, `x-sdk-version` headers | Every HTTP request | Support/diagnostics | Includes app name/version and device model via a bundled user-agent library |
| SDK diagnostic log lines (level, message text, app version, device fingerprint) | `RemoteLogger` | Remote SDK diagnostics | Only sent for warning/error-level logs, to a brand-specific logging endpoint; message text could incidentally include app-supplied strings |

**IP address** is a nullable field on the visitor payload but is never populated by SDK code — it is inferred
server-side from the network connection, not read or sent by the client. **Location** is likewise a field that
exists on the wire format but is never populated by any SDK code path. **The SDK does not access device location
or the contacts list.** **Avatar URL** (`CustomerIdentityModel.image`) is likewise a field on the identity model,
but the SDK only ever *receives* it from the backend for rendering an agent's or customer's avatar
(`toMessageAuthor()`) — there is no `ChatBuilder` setter for it, and the SDK never sends one.

## Data stored on-device

| Data | Location | Encrypted? |
|---|---|---|
| Auth token, transaction token, customer ID, visitor ID (persistent UUID), visit details, welcome message, push token | `secure_store.preferences_pb` (Jetpack DataStore, `chat-sdk-core`) | Yes — AES-256-GCM via Tink + Android Keystore |
| Tink keyset wrapping the store above | `cxonechat_tink_keyset.xml` (SharedPreferences, `chat-sdk-core`) | N/A — key material itself; wrapped by a non-exportable Android Keystore key |
| Session cookies | `persistent_cookies.pb` (Jetpack DataStore, `chat-sdk-core`) | Yes — AES-256-GCM via Tink + Android Keystore |
| Tink keyset wrapping the cookie store | `cookie_datastore_keyset.xml` (SharedPreferences, `chat-sdk-core`) | N/A — key material itself; wrapped by a non-exportable Android Keystore key |
| Channel/brand configuration cache | In-memory only | N/A — cleared on sign-out, never written to disk |
| Pending camera-capture URI (transient, during camera round-trip) | `cxone_chat_attachment_prefs.xml` (SharedPreferences, `chat-sdk-ui`) | No |
| Captured photo/video temp files | App cache dir (`cacheDir/tmp/`, `cacheDir/capture/`), shared only via `FileProvider` | No |
| UI state: permissions already requested, notifications dismissed, "end contact" prompts already shown per thread | `datastore/com.nice.cxonechat.ui.settings.preferences_pb` (Jetpack DataStore, `chat-sdk-ui`) | No |

There is no Room/SQLite database and no on-device chat history cache — message history is fetched from the
backend on demand.

### Backup & restore

Android Auto Backup is on by default for any app (`android:allowBackup` defaults to `true`); `chat-sdk-core`
declares `android:fullBackupContent="@xml/backup_rules"`, which configures — but does not itself enable — the
scope of what's included. The bundled `backup_rules.xml` excludes only:

- `secure_store.preferences_pb` (file)
- `cxonechat_tink_keyset.xml` (sharedpref)
- `com.nice.cxonechat.secure.xml` (sharedpref)

Everything else under the app's `sharedpref`/`file` domains is included by default. That means
**`persistent_cookies.pb` (session cookies), its `cookie_datastore_keyset.xml` Tink keyset, `cxone_chat_attachment_prefs.xml`,
and the `datastore/com.nice.cxonechat.ui.settings.preferences_pb` DataStore are backup-eligible** and may be uploaded to Google's backup
servers as part of a device backup, even though they are listed as "stored on-device" above. This is not a
confidentiality break: the cookie keyset is wrapped by a non-exportable Android Keystore key, so a restored blob
cannot be decrypted on a different device — the SDK detects the failure and resets to an empty cookie list rather
than crashing.

**Whether these exclusions take effect depends on your app's `targetSdk`, not just the device's OS version.**
Android only switches from `fullBackupContent` to `android:dataExtractionRules` when *both* conditions hold: the
app targets API 31+ **and** the device is running Android 12+. If either doesn't hold — the app targets API 30
or below, or the device is on Android 11 or below — `fullBackupContent` (and `backup_rules.xml`'s exclusions)
still governs backup, regardless of the other condition.

`chat-sdk-core` ships no `dataExtractionRules`. So for an app that targets API 31+ and runs on an Android 12+
device, `backup_rules.xml`'s exclusions are ignored entirely, and unless that app supplies its own
`dataExtractionRules`, the platform default applies: **everything is backed up**, including the encrypted store
(`secure_store.preferences_pb`) and the Tink keyset that decrypts it (`cxonechat_tink_keyset.xml`).

Google Play has a rolling minimum `targetSdk` requirement for new/updated apps that is well past API 31, so
virtually every actively-maintained Play-distributed app meets the `targetSdk` half of that condition — in
practice this comes down to the *device's* OS version. To get equivalent protection regardless
of which device your users are on, supply your own `android:dataExtractionRules` with `<cloud-backup>` and
`<device-transfer>` sections mirroring the exclusions above, in addition to (not instead of) your
`fullBackupContent` rules. See the [Integration](../README.md#integration) section of the README for the full set
of exclusion entries — covering both the auth-token store and the cookie store — to keep in sync across both
files.

## Permissions

| Permission | Declared in | Purpose |
|---|---|---|
| `INTERNET` | `chat-sdk-core`, `chat-sdk-ui` | REST/WebSocket communication with the CXone backend |
| `ACCESS_NETWORK_STATE` | `chat-sdk-ui` | Network-state awareness for the bundled chat UI |
| `POST_NOTIFICATIONS` | `chat-sdk-ui` | Local notifications for incoming chat messages |
| `RECORD_AUDIO` | `chat-sdk-ui` | Voice message recording (only requested if the app uses this feature) |
| `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 29) | `chat-sdk-ui` | Writing the voice-message recording into shared storage (`MediaStore`) on Android ≤10 only — the file is kept for the user, not deleted after sending |
| `CAMERA` | **Not declared in the SDK's own manifest** | The SDK checks — and may request — this permission at runtime only if your app has separately declared it; the capture itself never requires it. See note below. |

In-chat photo/video capture (`AttachmentType.CameraPhoto`/`CameraVideo`) launches the device's default camera app
via `ActivityResultContracts.TakePicture`/`CaptureVideo` — an Intent-based handoff that writes the result to a
shareable `FileProvider` URI (`SelectAttachmentActivityLauncher.getAttachment()`). The SDK never calls the Camera
API directly, and the capture itself never requires the `CAMERA` permission to succeed.

`ChatActivity.withCameraPermission()` does check the runtime `CAMERA` permission state via the **merged** app
manifest (`PackageManager.GET_PERMISSIONS`) before launching capture, and will request the permission
(`requestCameraPermission()`) if it's declared but not yet granted. This exists only for apps that separately
declare `CAMERA` for their own purposes elsewhere: Android requires that permission to be *granted*, not just
declared, before this Intent-based capture will succeed for such apps. If your app doesn't declare `CAMERA` at
all — the common case, since capture itself doesn't require it — the check resolves to `NOT_DECLARED` and capture
proceeds directly through the default system camera app, subject to that app's own permission.

## Third-party SDKs bundled

| SDK | Used for | Independent data collection |
|---|---|---|
| Firebase Cloud Messaging | Push notification delivery | Push token registration flows through Google/Firebase infrastructure — see [Firebase's privacy documentation](https://firebase.google.com/support/privacy) |
| Coil | Image/video loading for attachments | Fetches media over HTTP from backend-provided URLs; no independent telemetry |
| Retrofit, OkHttp, Tink, Koin | Networking, local encryption, dependency injection | No independent data collection |

Firebase Analytics and Crashlytics are **not** bundled by this SDK.

## Keeping this document current

Whenever a change adds, removes, or changes the purpose of collected/stored/transmitted data (new custom field
type, new persisted preference, new bundled SDK, new permission), update this document in the same PR — the same
discipline the iOS SDKs apply to `PrivacyInfo.xcprivacy`.
