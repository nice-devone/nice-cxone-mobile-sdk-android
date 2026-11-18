<a name="unreleased"></a>

## [Unreleased]

<a name="4.1.0"></a>

## [4.1.0] - 2026-11-17

### Added

- Cognigy AI bot support: while a Cognigy bot handles the contact, the Chat UI hides the position-in-queue overlay. `ChatThread.contactHandler` tells who is handling it; treat `ContactHandler.Unknown` the same as `Cognigy`
- `docs/privacy.md` — data collection & privacy disclosure, to support Google Play "Data safety" declarations
- `RuntimeChatException.InvalidRefreshTokenException` — reported when the explicit OAuth flow's `refresh_token` is rejected by the backend while the SDK tries to exchange it for a new access token (e.g. expired or revoked). Previously surfaced as the generic `AuthorizationError`; integrators can now distinguish this case to trigger full re-authentication. `RuntimeChatException` is sealed, so a Kotlin `when` over it without an `else` branch needs a case for it
- Long-pressing a conversation in the thread list opens a menu with Archive and Rename. Archive is offered only for conversations that can still receive messages. The labels are the new string resources `archive_thread_menu_item` and `rename_thread_menu_item`
- Landscape and large-screen layouts. The Chat UI now adapts to the window it is drawn into rather than to the device, so it stays correct in split screen, in a freeform window and on foldables. A window counts as wide when it reaches the Material 3 expanded width class (840dp) or whenever it is in landscape, which covers rotated phones, landscape-native tablets and side-by-side multi-window panes
  - Navigation bars now inset their content on wide and foldable screens instead of stretching it edge to edge
  - The conversation navigation bar keeps its title on a single row in landscape, instead of using the taller two-row layout which only collapsed once the conversation was scrolled
  - The status bar is hidden while the chat is shown on a wide screen, and can be revealed by swiping from the edge
  - Messages sit on the conversation's wide-screen column: the wider margin replaces the message list's own 16dp inset
    rather than adding to it, and agent messages reserve room for the avatar that overlaps their bubble's corner, so it
    sits on the edge of the column. Narrower windows are unchanged
  - Message bubbles are now at most 60% of the window width (excluding the wide-screen side padding). A rich link message takes the full width of its bubble instead of a narrower share of it.
  - A quick reply's options no longer spread across the full window width in landscape; they are now capped at the same maximum width as the message bubble above them there, instead of stretching past it. Unchanged in portrait
  - The audio player now fills its message bubble in every window. It is never narrower than its playback controls need, so on a very narrow window its bubble grows past the usual width limit rather than cutting a control off
  - The message input bar lines its controls up with the conversation on wide and foldable screens: the text field, the microphone and the voice message recording controls sit on the same column as the messages above them
  - The bar of attachments waiting to be sent has been redesigned: it keeps a fixed height, shows a scrollbar under the attachments once there are more than fit, and fades them out at the edge they scroll past. On wide and foldable screens its attachments line up with the rest of the conversation instead of starting at the window edge
  - While the keyboard is open on a window less than 480dp tall, such as a phone in landscape, the bar of attachments waiting to be sent shrinks so the message input stays in view: its attachments are drawn smaller and without their file names, and it returns to its full size when the keyboard closes. Taller windows, including tablets in landscape, are unchanged
  - Bottom sheets now span the window when it is wider than it is tall, instead of stopping at the Material 3 640dp width. Any window taller than it is wide keeps the centred sheet it had, including a large tablet held upright, since there the cap is what keeps sheet content readable. The attachment picker, the end conversation sheet and the inactivity prompt are exceptions, keeping the centred card in landscape too, because that is what their own design draws
  - The attachment selection sheet now lays its attachments out in four columns when the window is wider than it is tall, keeping two columns when it is taller, and shows a scrollbar beside them. Its title, grid and action bar line up with the rest of the conversation on wide and foldable screens instead of starting at the window edge
  - The attachment picker sheet stays centred in a landscape window on a device using three-button navigation, without reserving space for a side navigation bar its centred card never reaches
  - The list picker sheet now shares the same modal bottom sheet chrome as the pre-chat survey and inactivity sheets instead of building its own header and footer. Its title, options and Cancel/Submit buttons keep the design's 16dp portrait margin and widen to the shared 64dp inset in a landscape window, instead of staying at 16dp in every window. The subtitle now scrolls with the options rather than sitting in the fixed header, matching the design
  - The time picker sheet now shares the same modal bottom sheet as the list picker and pre-chat survey: its title, date groups and slot grid line up with the rest of the conversation on wide and foldable screens instead of starting at the window edge, and the slot grid shows a scrollbar once there are more slots than fit on screen
  - The send transcript sheet follows the pre-chat survey's layout on wide and foldable screens: its title, email fields and Cancel/Submit buttons line up on the shared 64dp inset instead of sitting 16dp from the window edge. On a wide screen the sheet also hides the status bar, and in landscape it keeps clear of a navigation bar on the side of the window. In every window its title now sits in a fixed header on the sheet's own background, separated from the body by a divider and lined up with the email fields instead of 16dp further in, and the subtitle scrolls with the fields
  - The send transcript sheet scrolls its content in a short window, such as a phone in landscape, so the confirmation field stays reachable and the keyboard no longer covers the focused field
  - The full screen image and video viewers' headers now use the native Material 3 top app bar and are separated from the media by a divider, so their back and share buttons line up with the rest of the conversation on wide and foldable screens instead of sitting against the window edge
  - The video in the full screen viewer is capped at 640dp wide and centred, instead of being stretched across a window wider than that
  - Attachment previews in a message take their size from the window the chat is drawn into rather than from the device screen, so they shrink with the window in split screen, in a freeform window or on a foldable folded shut, and follow it across a resize. They are unchanged in a window which fills the display
- `Popup.SatisfactionSurvey` — a survey delivered through `ChatThreadActionHandler.popupFlow`. The survey page collects the answers; nothing needs to be reported back to the SDK. The Chat UI shows it as a blocking in-app dialog with a WebView, once per thread. In Live Chat, a survey sent while the app was closed or in the background is still shown: the closed conversation is kept and its survey displayed, followed by the usual "close chat" options once the survey is dismissed. In Live Chat, `ChatThreadsHandler.threadsFlow` now reports such a closed conversation, instead of a new one, until its survey has been delivered through `popupFlow` and the list is refreshed; an integrator using only `chat-sdk-core` should collect `popupFlow` for it, or create a new thread
- Chat UI now posts a local notification titled "Satisfaction Survey" when a survey arrives for a background thread in a multi-thread channel, showing the backend-provided prompt text. Opening that thread afterward still shows the in-app survey dialog. The survey is stashed (AES-256-GCM, Android Keystore) until then; integrators with their own backup rules should add `datastore/com.nice.cxonechat.ui.secure_settings.preferences_pb` to their exclusions (see the [Integration](README.md#integration) section)

### Changed
- Tapping a message whose format the SDK cannot display now shows the explanation in a tooltip anchored to that message, instead of a snackbar at the bottom of the screen. The tooltip stays open until it is dismissed — by tapping outside it or pressing Back — rather than hiding itself on a timer; while it is shown it takes over touch, so that dismissing tap is consumed rather than reaching what is under it, and the conversation cannot be scrolled until the tooltip is closed.
- Building or re-preparing chat no longer always calls the backend to fetch the channel configuration: the result is now cached in memory for up to 5 minutes, so repeated builds/prepares within that window reuse it instead of making a new network call. The cache is cleared on sign-out, including for a fetch that was still in progress when sign-out happened
- In Live Chat, `ChatThreadsHandler.threadsFlow` no longer always replaces a recovered closed conversation with a new one: a closed conversation whose Satisfaction Survey has not been delivered yet is reported until the survey is delivered through `ChatThreadActionHandler.popupFlow` and the list is refreshed. An integrator using only `chat-sdk-core` should collect `popupFlow` for that conversation, or create a new thread
- Bump `com.google.crypto.tink:tink-android` from 1.22.0 to 1.23.0
- Bump `androidx.navigation` from 2.9.8 to 2.10.1
- Bump Kotlin from 2.4.0 to 2.4.10
- Bump `com.squareup.okhttp3:okhttp` from 5.4.0 to 5.5.0
- Bump `androidx.core:core-ktx` from 1.19.0 to 1.19.1
- Bump `androidx.annotation:annotation` from 1.9.1 to 1.11.0
- Bump `androidx.compose:compose-bom` from 2026.06.00 to 2026.09.00
- Bump `androidx.constraintlayout:constraintlayout-compose` from 1.1.1 to 1.1.2
- Bump `io.coil-kt.coil3:coil-bom` from 3.4.0 to 3.6.3
- Bump `androidx.emoji2` from 1.6.0 to 1.7.0
- Bump `com.google.firebase:firebase-bom` from 34.15.0 to 34.19.0
- Bump `androidx.appcompat:appcompat` from 1.7.1 to 1.8.0. AppCompat now ships JSpecify nullness annotations; Kotlin integrators can enforce them by adding `-Xjspecify-annotations=strict` to their own module's `freeCompilerArgs`.
- Explicit OAuth flow token-exchange errors that previously triggered a silent `ChatActivity` exit now show the standard blocking auth-error dialog, matching how other hard authorization failures are already presented. See **Deprecated** below.
- The time picker sheet header now uses the native Material 3 top app bar
- Bump androidx.compose.material3 from 1.4.0 to 1.5.0-beta01. This is a pre-release version, required because the app bar components used here are not public in a stable one
- The attachment selection sheet's title and action bar now sit on the sheet's own background with the attachments on a subtler one between them, each separated by a full-width divider
- The attachment picker sheet no longer draws a drag handle, and its title now has clearance above and below it instead. The title reads "Select Attachment" and the options "Camera - Photo", "Camera - Video" and "Media Gallery", correcting their capitalisation; the resource names are unchanged, so an integrator already overriding them keeps their own wording. Its options no longer carry a trailing arrow, and its title lines up with them instead of sitting further in. In a landscape window the sheet stays a centred card rather than spanning the window, with its options and their dividers inset to match its design. The sheet is still dismissed by swiping it down or tapping outside it
- Share buttons on attachments now use the standard Material share icon in place of the SDK's own, so they match the icon used elsewhere in the chat. On an attachment in the conversation the glyph is 16dp inside its 34dp button, the size its design draws, rather than the 24dp an icon takes by default, which crowded the button's outline
- Audio attachment messages match their current design more closely: a thinner progress bar with its spacing beside it rather than above and below, playback controls spaced further apart, slightly smaller seek icons, and a larger play/pause icon so it stands out as the main control. The player's side padding is tighter, while its top and bottom insets grow a little. The cell is shorter overall, because dropping the gap above and below the bar takes off more than those insets add back
- Tapping a disabled or already-selected time picker message now shows a tooltip anchored to it, explaining why its options are unavailable, instead of a snackbar at the bottom of the screen
- Tapping a disabled quick reply message now shows a tooltip anchored to it, matching the time picker message's own treatment, instead of a snackbar at the bottom of the screen
- A quick reply option whose text does not fit on one line at the normal chip width now fills its row instead of wrapping across multiple lines inside a narrow chip, matching the Figma design
- The offline screen no longer has a top app bar with a title and back button. Its notice and the position-in-queue header are now a card inset from the window edges, with 12dp rounded corners and a 640dp maximum width. The `position_in_queue_supporting_text` string no longer contains a line break
- The archived-conversation notice above the input is redesigned: a divider and a 52dp bar on the default background, with an outlined archive icon and tertiary-colour text
- The preparing overlay's close button uses the string resource `loading_close_button_text` instead of `close_chat`. If you override `close_chat` to change this button, override `loading_close_button_text` too
- A PDF you send is previewed from your local file after it is sent, instead of being downloaded again
- The pre-chat survey's title and fields now sit 16dp from the sheet edge in portrait instead of 24dp, matching the list picker and time picker sheets
- The time picker's slots now share each row equally and fill the sheet's width, as many to a row as fit (for example three on a phone in portrait and six on a wide phone in landscape), with no gap at the row's end
- The list picker's options now follow the design's row layout: 12dp inside the row's left and right edges instead of 16dp, a 64dp image instead of 32dp, supporting text directly under the label, a 12dp gap before the selection check, and a minimum height of 48dp instead of 55dp

### Deprecated

- `RuntimeChatException.FeatureUnavailableException` is deprecated — the backend issue that caused explicit OAuth flow token-exchange failures is fixed, so the SDK no longer throws this exception. These errors now surface as `RuntimeChatException.AuthorizationError`, same as any other flow's token-exchange failure. The class is retained for source/binary compatibility and will be removed in a future major version.

### Removed

- The `ic_share` drawable, now that the SDK's share buttons draw the standard Material share icon instead

### Fixed

- Fix `persistent_cookies.pb` (the encrypted cookie DataStore) and its Tink keyset not being excluded from Android Auto Backup. `backup_rules.xml` now excludes `persistent_cookies.pb` and the keyset's actual SharedPreferences file, `cookie_master_key.xml`
- Fix the auth-token store's Tink keyset also not being excluded from Android Auto Backup. `backup_rules.xml` excluded `cxonechat_tink_keyset.xml`, a path that matched no file; the keyset's actual SharedPreferences file, `cxonechat_master_key.xml`, is now excluded instead
- Fix an open thread in multi-thread mode staying open after its case was closed: its `ChatThreadHandler` now reports `ChatThreadState.Closed` and no longer allows new messages, as the thread list already did, and a thread reopened by a recovery or by a message on a new case accepts messages again and shows as open again in `ChatThreadsHandler.threadsFlow`, also after the list is refreshed
- Fix an open Live Chat conversation staying open after a thread recovery reported it closed, for example when it was closed while the app was in the background: its `ChatThreadHandler` now reports `ChatThreadState.Closed` and no longer allows new messages, so the "close chat" options are shown for it
- Fix a thread archived with `ChatThreadHandler.archive()` not becoming `ChatThreadState.Closed` when its case was closed afterwards
- Fix a closed conversation being treated as open again when a late case status change for its already-closed case arrived, which let new messages be sent into it, or when a late message, or in Live Chat a late agent assignment, for that case arrived, which reported it as `ChatThreadState.Ready`. A closed thread now keeps its closed state for such events for its own case, or for an event or thread without a case id, though a late message is still added to it; it is reopened by a message for a new case or by a recovery that reports it open
- Fix `chat-sdk-ui` adding the customer custom fields only after reporting `chatWindowOpen()`, so a message sent while that report was in flight could go out without them; the fields are now added as soon as the chat is `Ready`, before the report
- Fix `chat-sdk-ui` crashing the app when the customer `UiCustomFieldsProvider` throws as the chat screen opens; the error is now logged and `chatWindowOpen()` is still reported
- Fix an access-token refresh that was still in flight during `signOut()` writing the signed-out user's credentials back to storage, or reporting its failure to the next session; with a `TokenDelegateListener`, a token the delegate returns after `signOut()` is no longer sent for the signed-out visitor
- Fix `chat-sdk-ui` adding an attachment picked before the chat was prepared to the composer of a different conversation, when the chat screen stayed open across a `signOut()` and the next session, or the user moved to another thread meanwhile; such an attachment is now dropped, even when the same user signs in again, and its error dialog is no longer shown in the next session. An attachment picked while the chat is still being prepared is also dropped when that preparation fails or is cancelled, as is one picked after a failed preparation once the chat is prepared again; such an attachment has to be picked again
- Fix `ChatActivity` repeatedly retrying to reconnect with an already-rejected credential while its blocking authorization-error dialog is shown, wasting network/battery and risking backend rate limiting until the user dismisses the dialog. Auto-reconnect now pauses for as long as that dialog stays on screen
- Fix the remove button on attachments waiting to be sent having a touch target no larger than its 32dp circle. It now has a 48dp tall target that covers the circle up to the gap before the next attachment and reaches over its own attachment to just short of its centre, where a tap still opens the attachment.
- Fix the full screen video viewer drawing each clip in the proportions of the area it reserves rather than the clip's own. A clip shaped differently from that area — a recording made with the phone held upright, watched in landscape — had its edges cropped away, and on some devices was distorted instead. The clip is now fitted inside that area, and the playback controls span the clip rather than the area around it
- Fix the send button's icon pointing downwards in right-to-left layouts. The paper plane is mirrored for right-to-left
  reading, but the tilt drawn on top of it was not, which composed the two into a half turn and left the plane pointing
  down and away from the edge a message travels to. Both the message input bar's send button and the voice message
  recording bar's are corrected, since they draw the same control
- Fix "load more messages" pagination sending the newest message's timestamp as the cursor instead of the oldest, which could prevent older messages from loading; the request now uses the oldest message in the thread as the pagination cursor
- Correct the backup-exclusion guidance in the README integration notes: the encrypted preferences file path was listed incorrectly and would not have matched anything, the `com.nice.cxonechat.secure.xml` and cookie-store files were missing from the exclusion list entirely, and no guidance existed for `android:dataExtractionRules`, which apps targeting API 31+ need instead of `fullBackupContent`. Integrators who applied the previous guidance should update their backup rules
- Correct the integration guides and case studies against the 4.0 API. Examples that call `suspend` functions now show the surrounding coroutine — including `ChatBuilder(...)` and `ChatInstanceProvider.configure(...)`, which are themselves `suspend` and previously appeared at top level; Koin setup uses `includes(UiModule.chatUiModule())` with the current `customerFieldsProvider` and `contactFieldsProvider` parameter names; `closeSuspending()` replaces the removed `stop()` and is shown from `onStop()` in a coroutine that outlives the lifecycle scope starting it, rather than blocking a lifecycle callback — closing in `onPause()` also ended sessions the user was about to return to, and a close cut short at `ON_DESTROY` leaves the provider holding a `Chat` that no later `connect()` restores; that close is now guarded on the provider's state, because `closeSuspending()` arrives at `Prepared` whether or not it found a `Chat` to close, so closing while the provider is `Initial` or `Preparing` cancels the build in flight and strands it where both `connect()` and `prepare()` do nothing, and closing on `SdkNotSupported` discards the record that the backend refused this SDK version — `cancel()` is the call for all three, and the listener example now handles `SdkNotSupported` as well, without which its `when` over `ChatState` did not compile; the provider listener is registered in `onStart()` to pair with that, because `addListener` does not de-duplicate and a resumed activity would otherwise register twice; that same `onStart` now calls `prepare()` when it finds the provider `Initial`, because `addListener` does not replay the current state and a freshly started process would otherwise sit in `Initial` with no callback ever arriving; custom-field definitions are read from `chat.threads().preChatSurvey?.fields`; and the logging case study names `timedScope` and `logger.warning`
- Publish a resolvable Koin version to consumers of `chat-sdk-ui`. The module exposes `koin-android` and
  `koin-annotations` as `api` dependencies, but the Koin BOM that versions them was declared as `implementation`, so it
  was published into the runtime variant only. The compile variant listed both artifacts with no version at all, and an
  integrator who did not declare Koin themselves could not build — resolution failed with
  `Could not find io.insert-koin:koin-android:`. The BOM is now an `api` dependency, so the compile variant carries the
  version. Anyone who worked around this by adding a Koin dependency of their own can drop it
- Document that the Chat UI Koin setup has to register `androidContext(...)`. `UiModule.chatUiModule()` does not register
  it and the UI resolves `Context` and `Application` from the graph, so every previous `startKoin` example — in the
  guides and in the published `chatUiModule()` reference — produced a setup that started cleanly and then failed on the
  first injection inside `ChatActivity`, which reads as an SDK fault rather than a missing setup line. The UI
  configuration guide also no longer asks for a `koin-core` dependency: `chat-sdk-ui` exposes `koin-android` as an `api`
  dependency, so Koin needs no declaration of your own, and `androidContext(...)` ships in `koin-android` rather than
  `koin-core`
- Replace documented names that are absent from the public API: `ChatSdkConfig` becomes `SocketFactoryConfiguration`, `PreChatSurveyType` becomes `FieldDefinition`, the removed `RefreshToken` event is documented as SDK-managed token refresh, `ArchiveThreadEvent` becomes `ChatThreadHandler.archive()`, and the thread event list names the public `ChatThreadEventHandlerActions` functions rather than internal event classes
- Document the import that analytics and thread-event actions require: they are extension members of `ChatEventHandlerActions` and `ChatThreadEventHandlerActions`, so the actions must be imported from the object, as in `import com.nice.cxonechat.ChatEventHandlerActions.pageView`. Importing the object itself leaves the call unresolved, and the previous examples showed no import at all
- Correct the `chat-sdk-ui` integration requirements, which stated the wrong minimum supported Android version and pinned library versions the module no longer ships. The `minSdk`, `compileSdk` and `targetSdk` levels are stated in the root `README.md` under *Android SDK levels*, and the module requirements link there
- Dependency snippets presented as `build.gradle.kts` now use valid Kotlin DSL rather than Groovy-only syntax, and the published API reference for `ChatBuilder.build()` no longer shows a literal escape sequence in place of a dash
- Fix source links in the localization, theming and UI-configuration guides that led nowhere. A markdown target beginning with `/` is resolved from the host root rather than the repository, so links written that way pointed at pages that do not exist; they are now relative paths that open the file they name
- Correct the `SocketFactoryConfiguration` example, which selected the environment through a `CXoneEnvironment.YourRegion` constant that does not exist. It now uses a real region and names the ones available
- Fix the repository setup step, which declared the Maven repository at the top level of `settings.gradle.kts`. A settings script has no top-level `repositories`, so the snippet did not evaluate; it is now nested in `dependencyResolutionManagement { repositories { … } }`
- Fix explicit OAuth flow reconnecting with the refresh token even when the underlying access token was still valid; it now reuses the still-valid access token to obtain a new transaction token, only falling back to the refresh token once the access token itself has expired
- Detect OAuth token rejection via the backend's actual HTTP 400 response instead of HTTP 401, so the implicit flow now calls `TokenDelegateListener.onNewTokenRequested(TOKEN_INVALID)` and reports `InvalidAccessTokenException` when the backend rejects its token
- Fix drag handle of the list picker and send transcript sheets being announced in English regardless of locale
- Fix the share button beside an attachment message sitting closer to the bubble than its design draws it: the gap was 8dp where the design calls for 12dp. Only a message which carries an action moves
- Fix the tile for an attachment with no preview of its own — a document or other file — being drawn without the outline and background its design gives it, which left its icon hard to see wherever the customer's brand-coloured bubble was not behind it. The tile now paints the brand colour for the customer and a neutral surface for the agent, with an outline in the icon's tone. The larger icon the picker sheet draws takes a neutral tone against the sheet instead
- Fix a PDF attachment in a message not being drawn inside its bubble while its first page is still rendering. Until the page arrived the message stood in with the tile the attachment picker draws, which takes a fixed size and paints no background of its own: on a message carrying a single attachment, whose bubble is drawn transparent, that left the attachment looking as though it had no bubble at all, and on a message carrying several the tiles overflowed the cells they sit in. A PDF still rendering now shows the same tile as any other file, filling the space the page will take and carrying the same outline and backdrop, so the arriving page replaces it without the attachment changing size or position. The picker sheet's own tile is unchanged
- Fix a PDF attachment leaking its renderer and the open file it reads from when the attachment leaves the screen while its document is still being opened, such as scrolling a conversation quickly past a large document or leaving the chat while one opens. Opening runs off the main thread, and the document was taken ownership of only once that work handed its result back, so work cancelled after it had already opened the document handed nothing back and left the renderer and its file with no owner, held open until the garbage collector eventually reclaimed them. The thumbnail now takes ownership as the document is opened, and a file whose open finishes after the work asking for it was cancelled is closed rather than dropped — including a file already on the device, which was handed back through a path that discarded it in that same window
- Fix PDF attachments that share a file name showing the same preview. Each attachment's preview is now cached separately, so every PDF shows its own first page
- Fix a destroyed `ChatActivity` being kept in memory after a device rotation until the chat was closed; the voice-message recorder held a reference to it
- Fix closing the chat while a voice message is being recorded leaving the microphone recorder and its file open; they are now released when the chat closes
- Fix the options of a list field in the pre-chat survey and in the "Edit pre-chat survey custom fields" sheet opening under the keyboard. With the keyboard shown, the list now opens above the field when there is no room below it

### Security

- Enforce minimum `io.netty` 4.1.138.Final in buildscript resolutionStrategy to address AIKIDO-2026-592216 | GHSA-cg2g-fxr4-mg8m (build-time
  only, no SDK artifact impact)
- Enforce minimum `org.jsoup` 1.23.1 via Gradle resolutionStrategy to address CVE-2026-71497 / GHSA-pmhh-3w7g-xqp8 (build-time only, no SDK artifact impact)
- Enforce minimum `org.freemarker:freemarker` 2.3.35 via Gradle resolutionStrategy to address GHSA-27j2-h3m2-8237 (documentation generation
  only, no SDK artifact impact)
- Enforce minimum `com.fasterxml.jackson` 2.18.10 via Gradle resolutionStrategy to address CVE-2026-68497 / GHSA-q4xh-88c3-wmh7 (build-time
  only, no SDK artifact impact)
- Enforce minimum `org.bouncycastle` 1.86 in buildscript resolutionStrategy to address AIKIDO-2026-322957 (build-time only,
  no SDK artifact impact)
- Enforce minimum `org.apache.httpcomponents:httpclient` 4.5.13 via Gradle resolutionStrategy to address CVE-2020-13956
  (build-time only, no SDK artifact impact)
- Enforce minimum `org.jetbrains.kotlin:kotlin-stdlib` 2.1.0 via Gradle resolutionStrategy to address AIKIDO-2026-550292
  (build-time only, no SDK artifact impact)

<a name="4.0.1"></a>

## [4.0.1] - 2026-10-30

### Changed

- `chat-sdk-ui` adds an attachment picked while the chat is `Offline` or its connection is lost only if the chat recovers

### Fixed

- Fix thread updates such as a new message being lost when they arrived before `ChatThreadHandler.threadFlow` was first collected, so neither `threadFlow` nor `ChatThreadHandler.get()` ever showed them. A thread handler now applies updates from shortly after it is created, whether or not it is collected
- Fix `ChatThreadsHandler.threadsFlow` keeping a thread's loaded `contactId` and `threadState` after a new-message event changed them, so the thread list disagreed with the open thread; a thread closed by such an event now shows as closed in the list until a later thread recovery reopens it or brings a new contact
- Fix `ChatThread.contactId` and `ChatThread.threadState` not being updated when a new-message event arrives for a message that is already in the thread
- Fix new messages not reaching `threadFlow` in a live chat conversation started with `ChatThreadsHandler.create()`. The customer's own messages after the first, and agent replies, were added to the thread but often not emitted, so a UI rendering `threadFlow` showed them only after the next agent typing indicator, a `refresh()` or a new thread handler
- Fix a missing new-message update for a newly created thread in a multi-thread channel. When such a thread held only the customer's first message, the agent's first reply could reach `threadFlow` late or never, so the Chat UI posted no notification for it while another thread was open. Custom fields added to such a thread before its first message now also reach `threadFlow`
- Fix the proactive `WelcomeMessage` action being dropped on a new anonymous visitor's first chat open. Visitor registration failed until the backend assigned a `customerId`; when the app supplies no `customerId`, the SDK now generates a local one at startup
- Fix `ChatEventHandlerActions.chatWindowOpen()` being reported before the chat was actually `Ready`, which could cause the backend to drop a proactive `WelcomeMessage` action; `chat-sdk-ui` now reports it only once `Ready`

<a name="4.0.0"></a>

## [4.0.0] - 2026-08-04

### Added

- Enable deobfuscated Compose function names in stack traces for minified release builds
- AI authorship indicator: when AI-generated messages are present in a conversation, an "AI Assistant" badge appears in the top bar and AI authorship is announced to screen readers
- Enrich HTTP requests with a custom `User-Agent` header identifying the SDK (`cxone-chat-sdk/version`), app metadata (name, version, package, build), and device context (OS, model)
- First-class coroutine support — the SDK now exposes `Flow`-based observation APIs alongside the existing listener callbacks, and Java callers can opt into the new async surface via the provided interop wrappers:
    - `ChatThreadHandler.threadFlow: Flow<ChatThread>` — observe thread state updates reactively without registering a listener
    - `ChatThreadsHandler.threadsFlow: Flow<List<ChatThread>>` — observe thread list updates reactively without registering a listener
    - `ChatThreadActionHandler.popupFlow` and `ChatActionHandler.popupFlow` — observe popup action events as a `Flow`
    - Java interop helpers `ChatEventHandlerCoroutineWrapper`, `ChatThreadEventHandlerCoroutineWrapper`, and `ChatThreadMessageHandlerCoroutineWrapper` provide `*Async()` callback-based overloads for Java callers
- New `chat-sdk-core-java` artifact (Maven coordinate `com.nice.cxone:chat-sdk-core-java`) bundles all Java-facing async helpers — Java consumers should add it as a dependency to use callback-style APIs:
    - `ChatBuilderJavaInterop.getDefaultBlocking(...)` replaces the removed `ChatBuilder.getDefaultBlocking(...)` from `chat-sdk-core`
    - `ChatBuilderJavaInterop.getDefaultAsync(...)` and `buildAsync(...)` provide non-blocking factories with `Runnable`/`Consumer<Chat>`/`ErrorCallback` callbacks
    - `ChatJavaInterop.connectAsync(...)`, `getChannelAvailabilityAsync(...)`, `observeState(...)` wrap the suspend/Flow surface of `Chat`
    - `ChatThreadHandlerJavaInterop.archiveAsync(...)`, `observeThread(...)` wrap the suspend/Flow surface of `ChatThreadHandler`
    - `ChatInstanceProviderJavaInterop.closeSuspendingAsync(...)`, `signOutAsync(...)`/`signOutBlocking(...)`, and `configureAsync(...)`/`configureBlocking(...)` wrap the suspend surface of `ChatInstanceProvider` (`closeSuspending()`, `signOut()`, `configure(...)`) for Java callers
    - `ChatThreadsHandlerJavaInterop.observeThreads(...)` adapts `ChatThreadsHandler.threadsFlow` to a callback
    - `ChatActionHandlerJavaInterop.onPopup(...)` and `ChatThreadActionHandlerJavaInterop.onPopup(...)` adapt popup Flows to callbacks
    - All helpers return `Cancellable` for parity with the existing Java interop and never expose Kotlin coroutine types in their public signatures
- `chat-sdk-core-java` helpers and `*CoroutineWrapper` classes now accept an optional `java.util.concurrent.Executor callbackExecutor` parameter — when supplied, success and error callbacks are dispatched via the executor (e.g. `ContextCompat.getMainExecutor(context)` for Android UI updates). When omitted, callbacks fire on the SDK's internal IO thread.
- `chat-sdk-core-java` observer helpers (`observeState`, `observeThread`, `observeThreads`, `ChatActionHandlerJavaInterop.onPopup`, `ChatThreadActionHandlerJavaInterop.onPopup`) now accept an optional `ErrorCallback onError` parameter. When the underlying Flow terminates with an exception, the error is delivered to `onError` (CXoneException pass-through, other exceptions wrapped in InternalError) instead of escaping to the thread's uncaught-exception handler. When `onError` is null, upstream errors still escape so they remain visible to `Thread.UncaughtExceptionHandler` / logging infrastructure.
- `*CoroutineWrapper` async methods called after `close()` now synthesize an `InternalError("Wrapper has been closed; cannot perform async operation")` through `onError` instead of silently dropping the call. The `onError` is invoked synchronously on the calling thread for this case (no scope to dispatch on).

### Changed

- `ChatInstanceProvider` now rebuilds the `Chat` instance for each connected session: after `close()` (e.g. when the app goes to background), the next `connect()` transparently creates a fresh `Chat` and notifies `Listener.onChatChanged`. Do not cache the `Chat` instance across `close()` — always read `ChatInstanceProvider.chat` or observe `onChatChanged`. The channel configuration fetched during `prepare()` is reused for these rebuilds, so no additional network requests are made on reconnect.
- **BREAKING**: Project now uses Java 17 (up from 11 due to the Java 11 EOL) for source and target compatibility.
- **BREAKING**: Increase minSdk version 24 -> 26
- Increase targetSdk version 35 -> 36
- Increase compileSdk 36 -> 37
   - Required by the latest stable versions of androidx libraries (e.g., androidx.core:core-ktx:1.19.0)
- **BREAKING**:
  - **`Agent.fullName`**: return type changed from `String` to `String?`; it now returns `null` when neither name component was provided by the backend, replacing the previous empty-string result.
  - **`Agent.firstName`**, **`Agent.lastName`**, and **`Agent.imageUrl`**: return type changed from `String` to `String?`; `null` now indicates the field was not provided by the backend, as opposed to `""` which is an explicitly empty value.
  - **`Agent.isBotUser`** and **`Agent.isSurveyUser`**: return type changed from `Boolean` to `Boolean?`; `null` now indicates the agent's bot/survey status is unknown (not provided by the backend), which is distinct from `false` (definitively not a bot/survey agent).
- **BREAKING**:
  - **Time API**: All public time fields and parameters now use `kotlin.time.Instant` instead of `java.util.Date`. Affected API surface: `Message.createdAt`, `TimeSlot.startTime`, `MessageMetadata.seenAt`/`readAt`/`seenByCustomerAt`, `CustomField.updatedAt`, `Popup.InactivityPopup.Countdown.startedAt`, and the `instant` parameter on all `ChatEventHandlerActions` extension functions (renamed from `date`). Replace `Date()` with `kotlin.time.Clock.System.now()` and `Date(epochMs)` with `kotlin.time.Instant.fromEpochMilliseconds(epochMs)`.
- **BREAKING** Coroutine API migration — several public APIs have been migrated to `suspend` functions; callers must invoke them from a coroutine context. See the [Coroutine API migration guide](docs/migration/MIGRATION_COROUTINE_API.md) for the full callback → suspend/Flow mapping and Java interop. Java callers should add the new `com.nice.cxone:chat-sdk-core-java` dependency and use the helpers documented under **Added**:
    - `ChatBuilder.build()` is now a `suspend` function; call it from a coroutine or use `ChatBuilderJavaInterop.getDefaultBlocking()` for Java/blocking contexts
    - `Chat.connect()` is now a `suspend` function; call it from a coroutine
    - `ChatThreadMessageHandler.send()` is now a `suspend` function returning a `String` message ID; the previous callback-based overload and the `MessageSendResult` sealed interface have been removed
    - `ChatEventHandler.trigger()`, `ChatEventHandler.chatWindowOpen()`, and related event extensions are now `suspend` functions
    - `Chat.stateFlow` now returns `SharedFlow<ChatStateEvent>`, enforcing the replay-1 guarantee by the type system
    - `Chat.signOut()` is now a `suspend` function; call it from a coroutine
    - `ChatInstanceProvider.signOut()` and `ChatInstanceProvider.configure()` are now `suspend` functions; call them from a coroutine
    - `Chat.closeSuspending()` and `ChatInstanceProvider.closeSuspending()` added as the coroutine-native equivalent of `close()` — suspends until the same cleanup (storage/cookie persistence, socket teardown) completes instead of blocking the calling thread; prefer these over `close()` when a coroutine context is available
- **BREAKING** Java interop helpers extracted from `chat-sdk-core` to the new `com.nice.cxone:chat-sdk-core-java` artifact. Java consumers that use the `*CoroutineWrapper` classes or `ErrorCallback` must add `chat-sdk-core-java` as a dependency. Package names are unchanged (`com.nice.cxonechat.api`), so source imports do not change. Kotlin-only consumers can use the new suspend/Flow APIs directly and do not need the new dependency. As part of this extraction, the wrapper classes' constructors no longer accept a custom `CoroutineScope` parameter — they now accept an optional `java.util.concurrent.Executor callbackExecutor` instead, and otherwise manage their own internal coroutine scope.
- **BREAKING** `ChatBuilder.getDefaultBlocking(...)` removed from `chat-sdk-core`. Java consumers should replace calls with `ChatBuilderJavaInterop.getDefaultBlocking(...)` from `chat-sdk-core-java`. Kotlin consumers should call the suspend `ChatBuilder.getDefault(...)` directly.
- `chat-sdk-core` contains no internal `runBlocking` calls outside the `AutoCloseable` `close()` bridges — every `close()` implementation (`Chat`, its internal decorators, and `ChatInstanceProvider`) delegates to a suspend `closeSuspending()` via `runBlocking` so it can still support non-coroutine callers. Coroutine callers should call `closeSuspending()` directly instead. All other suspending work happens on caller-controlled coroutine contexts.
- Widen `com.nice.cxonechat.exceptions.InternalError` constructor visibility from `internal` to `public` so the moved `chat-sdk-core-java` interop helpers can wrap unexpected exceptions when forwarding to `ErrorCallback`. SDK consumers should not construct this exception type directly — its contract remains "report to CXone support".
- **BREAKING**  **`Environment` interface**: `authUrl` has been removed and replaced by `tokenUrl`. Both implementers of `Environment` (who must rename their `authUrl` override to `tokenUrl`) and callers that read `authUrl` on an `Environment` instance must migrate to `tokenUrl`.
- **BREAKING**  **`ChatThread.contactId`**: Exposed as a public abstract property; existing `ChatThread` subclasses must add `override val contactId: String?`.
- **BREAKING** Add implicit OAuth token delegation flow: integrators implement `TokenDelegateListener` and register it via `ChatBuilder.setTokenDelegateListener()`; the SDK calls `onNewTokenRequested()` with `UNSPECIFIED` on initial connect, `TOKEN_INVALID` when the backend rejects the stored token (HTTP 401), and `TOKEN_EXPIRED` when the token expires proactively during a session. The integrator's `onNewTokenRequested` implementation is expected to perform a **silent background refresh** (e.g. AppAuth `AuthState.performActionWithFreshTokens` using a cached refresh token) for `TOKEN_EXPIRED` / `TOKEN_INVALID` reasons — no browser UI should be shown to the user.
- **New exception types** — three new `RuntimeChatException` subclasses are now thrown and must be handled in your `ChatInstanceProvider.Listener.onChatRuntimeException` (or `ChatStateListener`) implementation:
  - `FeatureUnavailableException` — thrown when the explicit OAuth flow (`ChatBuilder.setAuthorization(...)`) is used but the backend returns an error during the transaction token exchange. Indicates the explicit flow is not functioning on the current backend version. Switch to the implicit flow via `setTokenDelegateListener(...)`.
  - `InvalidAccessTokenException` — thrown when the backend rejects the JWT used in the implicit flow and the SDK's automatic recovery also fails. Treat as a hard re-authentication signal.
  - `TokenDelegationFailedException` — thrown when `TokenDelegateListener.onNewTokenRequested` throws. The original cause is available via `Throwable.cause`.
- **Known issue**: Explicit OAuth flow (`ChatBuilder.setAuthorization(...)`) is currently not supported by the backend in the latest SDK version. Use implicit flow (`setTokenDelegateListener(...)`) for upgrades, or stay on your previous SDK version until explicit flow is supported.
- Bump `org.jetbrains.kotlin` from 2.2.21 to 2.4.0
- Bump `org.jetbrains.kotlinx:kotlinx-serialization-json` from 1.9.0 to 1.11.0
- Bump `org.jetbrains.kotlinx:kotlinx-coroutines-android` from 1.10.2 to 1.11.0
- Bump `io.insert-koin:koin-bom` from 4.1.1 to 4.2.2
- Bump `com.google.crypto.tink:tink-android` from 1.20.0 to 1.22.0
- Bump `androidx.core:core-ktx` from 1.17.0 to 1.19.0 (also bumps `androidx.core:core` and `androidx.annotation:annotation(-jvm)` transitively)
- Bump `androidx.compose:compose-bom` from 2026.02.01 to 2026.06.00
- Bump `androidx.activity:activity-compose` from 1.12.4 to 1.13.0
- Bump `androidx.media3` from 1.8.0 to 1.10.1
- Bump `androidx-lifecycle` from 2.10.0 to 2.11.0
- Bump `androidx.navigation` and `androidx.navigation.safeargs` from 2.9.7 to 2.9.8
- Bump `androidx.datastore:datastore-preferences` from 1.2.0 to 1.2.1
- Bump `io.coil-kt.coil3:coil-bom` from 3.3.0 to 3.4.0
- Bump `com.google.firebase:firebase-bom` from 34.10.0 to 34.15.0
- Bump `com.google.firebase.appdistribution` from 5.2.1 to 5.3.0
- Bump `com.google.gms.google-services` from 4.4.4 to 4.5.0
- Bump `com.squareup.okhttp3:okhttp` from 5.3.2 to 5.4.0
- Default SocketFactoryConfiguration implementation is now a data class with value equality,
    which may cause issues if users rely on previous reference only equality
- Published javadoc artifacts now use a single `-javadoc.jar` classifier per module instead of separate `-debug-javadoc.jar`/`-release-javadoc.jar` variants, and are no longer exposed as a Gradle Module Metadata `docstype=javadoc` variant; classifier-based javadoc resolution used by Maven, Gradle, and IDEs is unaffected

### Deprecated

- `Configuration.securedSessions` and `Configuration.Feature.SecuredSessions` — secured sessions are becoming mandatory for all channels. The flag will be removed in a future SDK release; integrators should stop reading it. The SDK will treat secured sessions as always enabled regardless of the channel configuration.

### Removed

- **BREAKING** `ChatThreadHandler.get(OnThreadUpdatedListener): Cancellable` removed — use `threadFlow` to observe thread state updates reactively
- **BREAKING** `ChatThreadHandler.OnThreadUpdatedListener` nested interface removed (deprecated since coroutine migration) — use `threadFlow` instead
- **BREAKING** `ChatThreadsHandler.threads(OnThreadsUpdatedListener): Cancellable` and `OnThreadsUpdatedListener` removed — use `threadsFlow` to observe thread list updates reactively
- **BREAKING** `ChatInstanceProvider.stateFlow` extension property removed — use `Chat.stateFlow` to observe chat state changes

### Fixed

- Chat header, thread list, and end-session screen now show a localizable "Agent" label when an agent is assigned but personal information is hidden by the backend
- Message avatars fall back correctly to initials (when name is visible) or the generic agent icon (when both name and image are hidden)
- Fix position-in-queue overlay remaining visible after Live Chat thread recovery when the assigned agent is only present in the `ownerAssignee` field
- Fix `ChatInstanceProvider` remaining stuck in `Connecting` state when `connect()` fails immediately (race between coroutine start and state transition)
- Fix `threadsFlow` in single-thread and live-chat modes not replaying the correct thread state to late subscribers after a recovery failure
- Fix `chat-sdk-ui` showing no feedback when the backend rejects the SDK version (`SdkNotSupported` state now shows a non-dismissible error dialog instead of silently leaving the user on a blank screen)
- Fix LiveChat getting stuck on the loading screen with no automatic reconnect after the initial connection attempt fails while the device is offline; the SDK now retries automatically with exponential backoff once connectivity is available again

### Security

- Enforce minimum `org.bouncycastle` 1.84 in buildscript resolutionStrategy to address CVE-2026-5598 / GHSA-p93r-85wp-75v3 (build-time only, no SDK artifact impact)
- Enforce minimum `io.netty` 4.1.135.Final in buildscript resolutionStrategy to address GHSA-3qp7-7mw8-wx86 (build-time only, no SDK artifact impact)
- Harden exported `ChatActivity` with `android:intentMatchingFlags="enforceIntentFilter"` for Android 16 (API 36) intent-matching compliance
- Enforce minimum `com.fasterxml.jackson` 2.18.9 (including `jackson-bom` and `jackson-databind`) in resolutionStrategy to address CVE-2026-54515 / GHSA-5jmj-h7xm-6q6v (build-time only, no SDK artifact impact)

<a name="3.3.1"></a>

## [3.3.1] - 2026-05-06

### Fixed

- Fix queue position indicator not updating during agent handover; visibility now relies solely on the reported position value

<a name="3.3.0"></a>

## [3.3.0] - 2026-04-30

### Added

- Prefer agent nickname in chat thread titles
- Add `TimePicker` message type support with UI, enabling time slot selection in chat interactions
- Add contextual loading indicators for attachments throughout the chat UI
- Add option to reset chat theme to default colors in UI settings
- Add End Conversation confirmation dialog requiring explicit user confirmation before closing the chat session

### Changed

- Update navigation bar menu icons and action labels
- Bump `androidx.activity:activity-compose` from 1.12.3 to 1.12.4
- Bump `androidx.compose:compose-bom` from 2026.01.01 to 2026.02.01
- Bump `com.google.firebase:firebase-bom` from 34.8.0 to 34.10.0
- Bump `org.jetbrains:annotations` from 26.0.2-1 to 26.1.0

### Fixed

- Fix crash in `RemoteLogger` when logging exceptions with fewer than 3 stack trace frames
- Fix thread safety issue in `DownloadUtil` causing race conditions during concurrent media downloads
- Fix `Send Transcript` action always visible in End Conversation dialog regardless of channel configuration
- Fix video preview loading spinner being skipped when Compose composition slot is reused
- Fix menu action box padding in navigation bar
- Fix loading overlay not shown and chat not reconnecting after network restore mid-session (regression introduced after 3.2.0 by the WebSocket exponential reconnect feature)
- Fix message read status dropped when `ReadChanged` event arrives before `MessageCreated` has populated the thread; read confirmation is now applied regardless of event ordering and cannot be downgraded by a subsequent `MessageCreated` event
- Fix double-tick read indicator disappearing when an incoming agent message shifts the customer message's position in the chat list

### Security

- Enforce minimum `jose4j` 0.9.6 in buildscript resolutionStrategy to address CVE-2024-29371 (build-time only, no SDK artifact impact)
- Enforce minimum `io.netty` 4.1.129.Final in buildscript resolutionStrategy to address CVE-2025-67735 (build-time only, no SDK artifact impact)
- Enforce minimum `org.jdom:jdom2` 2.0.6.1 and `org.apache.commons:commons-lang3` 3.18.0 in buildscript resolutionStrategy to address build-time CVEs (build-time only, no SDK artifact impact)
- Upgrade `org.jetbrains.dokka` from 2.0.0 to 2.2.0 to resolve CVE-2025-52999, CVE-2025-49128, and CVE-2022-40152 (build-time only, no SDK artifact impact)

<a name="3.2.3"></a>

## [3.2.3] - 2026-05-06

### Fixed

- Fix queue position indicator not updating during agent handover; visibility now relies solely on the reported position value

<a name="3.2.2"></a>

## [3.2.2] - 2026-04-23

### Fixed

- Respect the `liveChatAllowTranscript` channel configuration flag when showing the "Send Transcript" option in the End Conversation sheet

<a name="3.2.1"></a>

## [3.2.1] - 2026-03-31

### Fixed

- Prevent EndContact dialog from reappearing after dismissal in LiveChat mode

<a name="3.2.0"></a>

## [3.2.0] - 2026-02-26

### Added

- Integrate send transcript feature into the SDK & UI
- Implement Transaction Token Exchange for OAuth authentication flows

### Fixed

- Fix loading overlay not appearing and chat not reconnecting after network is restored
- Fix threading issue during captured media retrieval
- Fix audio permission request flow on Android 15
- Fix `RemoteLogger` not reporting upload failures to secondary logger instance
- Prevent loading of metadata for pending thread
- Align confirm button string to use "Submit" consistently across pre-chat survey, send transcript and list picker

### Changed

- Migrate secure storage from deprecated `EncryptedSharedPreferences` to Google Tink
- Update Coil 3.1.0 -> 3.3.0
- Add com.google.crypto.tink:tink-android 1.20.0 dependency for secure storage
- Bump androidx-lifecycle from 2.9.4 to 2.10.0
- Bump androidx-navigation and androidx.navigation.safeargs from 2.9.5 to 2.9.7
- Bump androidx-window from 1.5.0 to 1.5.1
- Bump androidx.activity:activity-compose from 1.11.0 to 1.12.3
- Bump androidx.compose:compose-bom from 2025.10.00 to 2026.01.01
- Bump androidx.datastore:datastore-preferences from 1.1.7 to 1.2.0
- Bump androidx.security:security-crypto from 1.0.0 to 1.1.0
- Bump com.airbnb.android:lottie-compose from 6.6.10 to 6.7.1
- Bump com.google.firebase:firebase-bom from 34.4.0 to 34.8.0
- Bump koin-annotations from 2.1.0 to 2.3.1
- Bump the kotlin-and-ksp group 2.2.10 -> 2.2.21
    - Bump com.google.devtools.ksp from 2.3.1 to 2.3.3 in the kotlin-and-ksp group
- Bump okhttp from 5.1.0 to 5.3.2
- Bump net.engawapg.lib:zoomable from 2.7.0 to 2.9.0

<a name="3.1.2"></a>

## [3.1.2] - 2026-02-18

### Fixed

- Fix FileProvider authority conflict when multiple apps using the SDK are published on Google Play by making the authority dynamic based on
  application package name instead of hardcoded value
- Fix PdfRender thread-safety issue causing crashes during concurrent access
- Fix PdfRenderer resource leak during rapid scrolling and constructor failures by properly closing resources in all error paths and
  lifecycle events
- Fix opening of non-video/image attachments by adding URI read permission flag
- Fix issue introduced in version 3.0 where user entered text which is auto-linked is invisible on background
- Remove remnants of unused Foreground Service implementation to prevent clash with other integrations

<a name="3.1.1"></a>

## [3.1.1] - 2026-01-14

### Fixed

- Fix `OnThreadsUpdatedListener` listener is notified when a `ChatThreadState.Pending` thread is created locally
- Add missing subtitle to the Pre-chat survey screen & Edit Custom Values Screen
- Fix chat transcript layout when the position in queue counter is displayed
- Documentation error regarding UI module Theme customization
- Add missing Java helper methods for UI module `ThemeColorTokens` and subclasses
- Improve handling of slow SDK start when attachments are added
- Improve handling of slow SDK start when UI is resumed
- Hide "Add attachment" button based on channel configuration
- Fix missing permission request for camera attachment iff CAMERA permission is declared in the application manifest

<a name="3.1.0"></a>

## [3.1.0] - 2025-11-06

### Added

- `RemoteLogger` to report errors to the server and exposed in the public API
- Inactivity Popup Support
- `ChatThreadActionHandler` which can be obtained from `ChatThreadHandler`
- `Popup` which is provided to `OnPopup` listeners registered in the `ChatThreadActionHandler`
- `InactivityPopup`
- Utility extension method `ChatThreadEventHandler.triggerAction` to allow easy triggering of `Action` events (for Quick Replies, List
  Picker, Popups)
- Daily Perfecto build workflow for automated testing
- Concurrency controls to PR workflows to cancel previous runs and save CI resources
- UI tests support
- ChatInstanceProvider unit test coverage
- Presurvey field validation check dynamically
- Video and Image caching to improve loading performance
- Indicator for attachment preparing
- WebSocket exponential reconnect with backoff strategy
- androidx.material3.adaptive dependency for adaptive UI layouts
- androidx.window and androidx.window-testing dependencies for window size class support

### Changed

- **BREAKING CHANGE**: Bump com.squareup.okhttp3:okhttp from 4.12.0 to 5.1.0
- Updated the thread list cell design for unread state
- Updated UI for List Picker
- Updated UI for Quick Replies
- Updated UI for voice messages
- Updated UI for attachments
- Updated UI for typing indicator
- Updated UI for Position in Queue
- Updated UI for offline mode
- Updated UI for Rich-Link
- Updated UI for Basic Conversation
- Updated UI for Accessibility
- QuickReply message interaction behavior - options only visible for last message
- UI module error handling with grouped error types
- Increased compileSdkVersion to 36
- Update Android Gradle Plugin 8.11.1 -> 8.13.0
- Bump androidx.compose:compose-bom from 2025.06.01 to 2025.10.00
- Bump androidx-navigation from 2.9.3 to 2.9.5
- Bump androidx.navigation.safeargs from 2.9.3 to 2.9.5
- Bump androidx.activity:activity-compose from 1.10.1 to 1.11.0
- Bump androidx.emoji2:emoji2 from 1.5.0 to 1.6.0
- Bump androidx-lifecycle from 2.9.3 to 2.9.4
- Bump com.google.firebase:firebase-bom from 34.3.0 to 34.4.0
- Bump com.github.gmazzo.buildconfig from 5.6.8 to 5.7.0
- Bump com.google.gms.google-services from 4.4.3 to 4.4.4

### Fixed

- Handle failure & added auto retry mechanism during visitor creation/updation
- Filter unsupported message answers
- Presurvey invalid email and field validation
- Server error reporting in single thread mode - ThreadRecoveryFailure errors now suppressed
- Single attachment preview - fixed size and design according to new specifications
- Deeplink handling - delay Chat access until it is ready or in terminal state
- Custom Fields dropdown - menu now opens properly and expands on clear
- Memory leak in TemporaryFileStorage - using Application context instead of Activity
- ExplicitGcViolation for Android 16 compatibility

### Security

- Audio message recording - validate audio file Uri returned by the ContentResolver before deleting it

<a name="3.0.0"></a>

## [3.0.0] - 2025-07-28

### Added

- Chat SDK UI module release
- androidx.compose:compose-bom
- androidx-navigation and androidx.navigation.safeargs
- androidx.activity:activity-compose
- androidx.constraintlayout:constraintlayout-compose dependency for constraint-based Compose layouts
- androidx.emoji2:emoji2
- androidx.media3 dependencies (datasource-okhttp, exoplayer, exoplayer-hls, ui) for media playback
- androidx-lifecycle
- com.google.firebase:firebase-bom
- com.google.gms.google-services
- net.engawapg.lib:zoomable dependency for image zooming functionality
- io.coil-kt.coil3 dependencies (coil-compose, coil-network-okhttp, coil-video) for image loading
- io.insert-koin dependencies (koin-bom, koin-android, koin-androidx-compose, koin-annotations, koin-ksp-compiler) for dependency injection
- org.jetbrains.kotlinx.binary-compatibility-validator plugin for API compatibility checking

### Changed

- **BREAKING CHANGE**: Renamed all classes with name like `CXOne` to `CXone`
- **BREAKING CHANGE**: Bump retrofit from 2.11.0 to 3.0.0
- Redesigned pre-contact survey
- Allow to name pending thread
- Bump androidx.datastore:datastore-preferences from 1.1.1 to 1.1.7
- Bump com.github.gmazzo.buildconfig from 5.5.4 to 5.6.6
- Bump androidx.core:core-ktx from 1.15.0 to 1.16.0
- Bump org.jetbrains:annotations from 26.0.1 to 26.0.2
- Bump org.jetbrains.kotlinx:kotlinx-serialization-json from 1.7.3 to 1.8.1
- Update Kotlin 2.0.21 -> 2.1.21

### Removed

- All deprecated methods and classes

### Fixed

- ChatInstanceProvider - fix concurrency for state updates
- Block multiple archival requests

<a name="2.3.0"></a>

## [2.3.0] - 2025-03-03

### Added

- Progress dialog for initialization in single thread mode
- com.github.gmazzo.buildconfig plugin for build configuration generation

### Changed

- Raise targetSdk 34 -> 35
- Update query parameters for web socket request
- Redesign message group header
- Redesign pre-contact survey
- Bump androidx.core:core-ktx from 1.13.1 to 1.15.0
- Bump kotlin from 2.0.10 to 2.0.21
- Bump org.jetbrains:annotations from 24.1.0 to 25.0.0
- Bump org.jetbrains.kotlinx:kotlinx-serialization-json from 1.7.2 to 1.7.3

### Fixed

- Welcome message improvements
    - In Messaging mode Welcome message is initially provided as placeholder
    - Welcome Message in LiveChat mode is using different logic
    - Improved support for late handling of a WelcomeMessage ProActive event
- Custom fields handling
    - Remove check which has allowed only the pre-chat survey fields to be supplied during thread creation to align with iOS platform
    - Don't send event about contact custom field change if the thread is in the pending or closed state
- Reconnect issues
    - Events are properly delayed on reconnect
- Allow SDK to reconnect in Offline state
- The `ChatWindowOpen` event is delayed pending authorization
- ProGuard/R8 fixes
    - Split out create method from all invoke operators
    - Added rules to the internal SDK minification

<a name="2.2.2"></a>

## [2.2.2]

### Fixed

- Livechat mode won't recover messages from closed case
- In livechat mode the queue position will remain `null` as long as the agent is assigned to the case
- SDK will supply Message object `createdAt` date with millisecond precision if possible
- Fix missing `ChatThreadState.Closed` enum entry in api.txt

<a name="2.2.1"></a>

## [2.2.1]

### Fixed

- Default `invoke()` operators in public API don't use `@JvmName` annotation to avoid minification issues in ProGuard/R8
    - The API was extended with methods which have the same signature as was previously covered by `invoke()` operators.
      This should prevent binary compatibility issues.
- Fix missing contactId for live chat thread
    - This fixes issue where it wasn't possible to end the live chat session in certain scenarios

<a name="2.2.0"></a>

## [2.2.0]

### Added

- Enable application LargeHeap for attachment upload
    - This allows upload of larger attachments on devices with sufficient RAM
- Enhance AuthorizeCustomer event
- Mobile SDK sends new headers for internal analytics
- Update agent model
    - Added nickName field
    - imageUrl field now provides the public image url

### Changed

- **BREAKING CHANGE**: Update kotlin from 1.9.24 to 2.0.10
- Replace GSON with kotlinx.serialization

### Deprecated

- inContactId and emailAddress from Agent
    - Values for these fields are now always empty

### Removed

- Unused dependencies from the utilities module

### Fixed

- Allow reconnect in offline state
- ProGuard/R8 issues
    - Updated consumer-rules.pro to prevent minification of several problematic methods
    - Added rules to the internal SDK minification
- SDK awaits for authorization
    - ℹ️ Event sending may be delayed until server confirms user authorization to use Chat service, sending of events prior to this could
      lead to loss of such events.

<a name="2.1.1"></a>

## [2.1.1]

### Fixed

- CaseStatusChanged with status closed doesn't archive thread in SingleThread mode

<a name="2.1.0"></a>

## [2.1.0] - 2024-07-18

### Added

- Support for autolinking in text messages
- Support ISO 8601 time zone in datetime values
- Allow to set customerId
- Prevent duplicate welcome message
- coordinate ArchiveThread and ThreadArchived events

### Changed

- **BREAKING CHANGE**: Increased minSDK to 24
- Live chat documentation update
- Bump Kotlin 1.9.22 -> 1.9.24
- Bump retrofit from 2.10.0 to 2.11.0
- Bump com.google.code.gson:gson from 2.10.1 to 2.11.0

### Removed

- Custom field validation
- sarif conversion as it isn't supported without special permission

<a name="2.0.0"></a>

## [2.0.0] - 2024-06-18

### Added

- Missing support for wildcard filetype restrictions
- LiveChat support
- LiveChat creates thread on connect if needed
- Support for autolinking in text messages
- Parsing and publication of file upload restrictions
- SDK enforces file restrictions
- Handle EventInS3 meta-event

### Changed

- **BREAKING CHANGE**: Enum case name consistency
- Align Single-threaded Channel Behavior
- Update DeviceFingerprint
- Sdk doesn't allow messaging to archived thread
- Bump androidx.core:core-ktx from 1.13.0 to 1.13.1
- Update AGP 8.2.2 to 8.3.1
- Update com.squareup.retrofit2 2.9.0 -> 2.10.0

### Deprecated

- Legacy plugins

### Removed

- [@Deprecated](https://github.com/Deprecated) Plugin support

### Fixed

- Fix looping now that caseContactFields is removed
- Deliver thread updates to all listeners
- Fix LiveChat restart
- Validate allowed file mimetype specification

<a name="1.3.1"></a>

## [1.3.1] - 2024-05-16

### Fixed

- Fixes for 1.3.1

<a name="1.3.0"></a>

## [1.3.0] - 2024-02-28

### Added

- ProxyLogger constructor with vararg param
- seenAt and inferred state to message metadata
- logger-android module
- Extract logging library module
- Option to specify Logger for the SDK
- Implement CaseStatusChanged event

### Changed

- **BREAKING CHANGE**: Replace dagger/hilt with Koin in UI and sample application components
- **BREAKING CHANGE**: If uploaded filename has no extension, get one using MimeTypeMap
- Set CustomerId type to String
- Update Chat to separate prepare and connect actions
- Display sender name and read/received status
- Update Agent.isTyping only when agent is typing
- Sockets created by SDK are tagged for TrafficStats
- Delay SharedPreferences initialization
- Improve Java compatibility
- Raise project compileSdk 33 -> 34
- Improve logging of outgoing events
- Pass server reported errors to integration
- Process events on background thread
- Allow to change users name
- Message is updated when read by agent
- Correct welcome message handling
- Update Kotlin 1.9.20 -> 1.9.21
- Bump com.squareup.okhttp3:okhttp from 4.11.0 to 4.12.0
- Bump androidx.core:core-ktx from 1.10.1 to 1.12.0
- Update Kotlin 1.8.21 -> 1.9.10

### Fixed

- Consolidate differing kotlin tool versions on 1.9.21
- Make date formatting thread safe
- Use referential equality for enum comparison
- Cancel start job in case of re-configuration
- TreeField/CVHierarchicalField to not skip every other level
- Remove messages with duplicate id from thread
- Crash when restore suspended login dialog
- Unreliable unit tests depending on makeMessageModel being sequential
- Display of new agent messages
- Disable sending empty messages
- Return error when receiving "invalid" server response on image upload
- Crashes on some Qualcomm/Samsung devices

<a name="1.2.1"></a>

## [1.2.1] - 2024-01-10

### Added

- Improve Java compatibility for 1.2.1

### Fixed

- Fix minification issues with ChatInstanceProvider

<a name="1.2.0"></a>

## [1.2.0] - 2023-09-26

### Added

- New chat-sdk-ui library from the prior sample application
- Support for timeSpentOnPage
- Support updated RecoveredThread event
- ChatInstanceProvider
- Support for customizable brand logo
- Conversion events in sample application

### Changed

- **BREAKING CHANGE**: Convert unnecessary abstract classes to interfaces
- Use events endpoint for analytics events
- Merge SampleApplication project to the SDK project
- Internally mark archived threads as archived pending success/failure from server
- Convert ListPicker message to Compose
- Convert thread list screen to JetPack Compose
- Compose Chat UI
- Integrate new Chat UI module with Store application
- Allow colorization of chat sdk screens
- Bump androidx.core:core-ktx from 1.10.0 to 1.10.1
- Bump com.squareup.okhttp3:okhttp from 4.10.0 to 4.11.0

### Fixed

- Reenable tests depending on android.util.Patterns
- Successful thread-archived event will trigger thread list refresh
- Persist deviceToken until the value is updated
- StoreVisitor upload failure message - server message part
- Create/update Visitor using new endpoint
- Issue with color settings fields not reflecting day/night
- Improve handling of notifications
- Allow empty/non-existent TEXT in QuickReply plugins
- Prepopulate configuration menu with last known custom values
- Thread update flow and remove extraneous refresh requests
- Misc errors around state changes
- Remove messages with duplicate id from thread

<a name="1.1.0"></a>

## [1.1.0] - 2023-06-26

### Bug Fixes

- Update thread agent correctly
- Allow application to respond to the websocket session state & reconnect
- Use OkHttp for WebSocket communication
- Add missing consumer ProGuard rule for GSON classes
- Modify Proguard/R8 rules to be compatible with R8 fullMode
- Successful thread-archived event will trigger thread list refresh
- Fix threading issue introduced with postback support
- Allow lenient parsing of UUID type

### Dependency Change

- Bump androidx.core:core-ktx from 1.9.0 to 1.10.0
- Bump org.jetbrains.dokka from 1.6.10 to 1.8.10
- Bump org.jetbrains.kotlin.android from 1.6.10 to 1.8.10

### Features

- implement/fix LoadThreadMetadata and add loadMetadata entry to ChatThreadEventHandlerActions
- Dynamic pre contact survey
- Documentation update
- create ContentDescriptor.DataSource to facilitate minimizing memory usage
- Modify Custom Fields storing behavior
- Increase targetSdk 31 -> 33
- add support for QUICK_REPLIES message types
- implement RichLink message types
- implement ListPicker message type
- Add an option to send postback value in a thread message
- Parse and utilize custom field definitions from the server.
- Fresh Naming in Websocket events
- Fresh Naming in Websocket events

### Reverts

- [chore] Bump androidx.core:core-ktx from 1.9.0 to 1.10.0

<a name="1.0.1"></a>

## [1.0.1] - 2023-03-07

### Bug Fixes

- Use OkHttp for WebSocket communication
- Add missing consumer ProGuard rule for GSON classes

<a name="1.0.0"></a>

## 1.0.0 - 2023-02-14

### Features

- Logging PoC
- Error handling
- Connection
    - Ping to ensure connection state
    - Execute trigger manually
    - Handle unexpected disconnect
- Customer
    - Save customer credentials
    - Customer authorisation
    - Customer reconnect
    - OAuth
- Customer Custom Fields
    - Save customer custom fields
- Threads
    - Update thread name
    - „Read“ flag
    - „Delivered“ flag
    - Threads load
    - Contact inbox assignee change
    - Recover thread
    - Typing indicator
    - Archive thread
    - Load thread metadata
    - Handle proactive action
        - Welcome message
        - Custom popup box
- Contact Custom Fields
    - Save contact custom fields
- Messages
    - Send/Receive attachments
        - Image
        - Video
        - Documents
    - Handle a message
        - Text
        - Plugin
            - Gallery
            - Menu
            - Text and Buttons
            - Quick Replies
            - Satisfaction Survey
            - Custom
            - Sub Elements
                - Text
                - Title
                - File
                - Button/iFrame Button
    - Previous message load
- Analytics
    - Page view
    - Chat window open
    - App visit
    - Conversion
    - Custom visitor event
    - Proactive action
        - display
        - success
        - failure
    - typing start/end

[Unreleased]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/4.1.0...HEAD

[4.1.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/4.0.1...4.1.0

[4.0.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/4.0.0...4.0.1

[4.0.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.3.1...4.0.0

[3.3.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.3.0...3.3.1

[3.3.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.2.2...3.3.0

[3.2.3]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.2.2...3.2.3

[3.2.2]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.2.1...3.2.2

[3.2.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.2.0...3.2.1

[3.2.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.1.2...3.2.0

[3.1.2]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.1.1...3.1.2

[3.1.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.1.0...3.1.1

[3.1.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.0.0...3.1.0

[3.0.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/2.2.2...3.0.0

[2.2.2]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/2.2.1...2.2.2

[2.2.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/2.2.0...2.2.1

[2.2.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/2.1.1...2.2.0

[2.1.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/2.1.0...2.1.1

[2.1.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/2.0.0...2.1.0

[2.0.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.3.1...2.0.0

[1.3.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.3.0...1.3.1

[1.3.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.2.1...1.3.0

[1.2.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.2.0...1.2.1

[1.2.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.1.0...1.2.0

[1.1.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.0.1...1.1.0

[1.0.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.0.0...1.0.1
