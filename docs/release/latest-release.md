# Version

**4.1.0**

## Status:

**RELEASED**

## Release Notes:

### ⬆️ Upgrading from 4.0.1

Check each item that applies to your app; the sections below have the details.

- **`RuntimeChatException` in a Kotlin `when`**: the class is sealed, so a `when` without an `else` branch needs a case
  for `InvalidRefreshTokenException`.
- **Backup rules**: an app that targets API 31 and newer adds `android:dataExtractionRules` with the exclusions in
  [Integration](../../README.md#integration), because the SDK's bundled `fullBackupContent` rules do not apply to it on
  Android 12 and newer. An app with its own `fullBackupContent` or `android:dataExtractionRules` rules re-checks them
  against that list, which now includes `datastore/com.nice.cxonechat.ui.secure_settings.preferences_pb`.
- **`close_chat` override**: also override `loading_close_button_text`, which the preparing overlay's close button now
  uses.
- **`ic_share` drawable**: removed; drop any reference to it.
- **`FeatureUnavailableException`**: deprecated and no longer thrown; code that waits for it never runs.
- **Koin**: a Koin dependency added only to make `chat-sdk-ui` resolve can be removed; `startKoin` must still register
  `androidContext(...)`.
- **`threadsFlow` and closed conversations**: a closed Live Chat conversation with an undelivered survey stays in the
  list; collect `ChatThreadActionHandler.popupFlow` to show the survey.

### ✨ Added

- **Cognigy AI bot support.** While a Cognigy bot handles the contact, the Chat UI hides the position-in-queue
  overlay. `ChatThread.contactHandler` tells who is handling it; treat `ContactHandler.Unknown` the same as `Cognigy`.
- `docs/privacy.md` — data collection & privacy disclosure, to support Google Play "Data safety" declarations
- `RuntimeChatException.InvalidRefreshTokenException` — reported when the explicit OAuth flow's `refresh_token` is rejected by the backend while the SDK tries to exchange it for a new access token (e.g. expired or revoked). Previously surfaced as the generic `AuthorizationError`; integrators can now distinguish this case to trigger full re-authentication. `RuntimeChatException` is sealed, so a Kotlin `when` over it without an `else` branch needs a case for it
- **Long-pressing a conversation in the thread list opens a menu with Archive and Rename.** Archive is offered
  only for conversations that can still receive messages. The labels are the new string resources
  `archive_thread_menu_item` and `rename_thread_menu_item`.
- **Landscape and large-screen layouts.** The Chat UI now adapts to the window it is drawn into rather than
  to the device, so it stays correct in split screen, in a freeform window and on foldables. A window counts as
  wide when it reaches the Material 3 expanded width class (840dp) or whenever it is in landscape, which covers
  rotated phones, landscape-native tablets and side-by-side multi-window panes.
  - **Wide screen and landscape support for navigation bars.** Navigation bars now inset their content on
    wide and foldable screens rather than stretching it edge to edge, while the bar background still spans
    the full window width.
  - **The conversation navigation bar stays on a single row in landscape.** It previously used the taller
    two-row Material 3 layout in every window, and that layout only collapses once the conversation is
    scrolled, so a short or empty conversation gave up 48dp of height where there was least to spare, or
    72dp when the AI badge is shown. The two-row layout is kept for taller windows. The "AI Assistant"
    badge remains directly below the title in both layouts.
  - **The status bar is hidden while the chat is shown on a wide screen**, reclaiming its space for
    content, and is restored when the window is no longer wide or the chat is closed. It can always be
    revealed by swiping from the edge. Only the status bar is hidden — the navigation bar stays visible so
    devices using three-button navigation keep their back affordance.
  - **Messages sit on the conversation's wide-screen column.** The design insets the conversation's content column
    by 16dp, and by a wider margin once the window is wide; the wider margin replaces the 16dp rather than adding to
    it, putting the column 64dp from the window edge. Agent messages there reserve room for the avatar drawn half
    outside their bubble's corner, so the avatar sits on the column's edge with the bubble inside it. That room is
    taken for every agent message rather than only the one carrying the avatar, so a group of them stays in one
    column. Windows too narrow for the wide-screen margin are untouched, avatar overhang included.
  - **Message bubbles are now at most 60% of the window width** (excluding the wide-screen side padding). A rich
    link message takes the full width of its bubble instead of a narrower share of it.
  - **A quick reply's options no longer spread across the full window width in a landscape window.** They
    previously had no width cap of their own, so on a landscape phone or a foldable they could spread across
    the entire window, past the message bubble above them. They now share the message bubble's own maximum
    width there, matching the Figma design. Unchanged in portrait, including on a wide portrait tablet.
  - **The audio player fills its message bubble.** It takes the full width of its bubble in every window, so
    its width follows the same 60% limit as every other message. It is never narrower than its playback
    controls need: on a window too narrow for that limit to fit them, the bubble grows to the controls' width
    rather than cutting one off.
  - **The message input bar lines its controls up with the conversation on wide and foldable screens.** The
    wide-window margin replaces the row's own margin rather than adding to it, so the text field, the microphone
    and the voice message recording controls sit on the same column as the messages above them. Compact windows
    are unchanged.
  - **The bar of attachments waiting to be sent has been redesigned.** It now keeps a fixed height instead
    of growing with its contents, shows a scrollbar in a strip below the attachments once there are more
    than fit on screen, and fades the attachments out against the edge they scroll past so it is obvious
    there are more. The space held clear at the far end appears only once the bar is scrolled to its end,
    where it keeps the last attachment's remove button from being clipped. On wide and foldable screens the
    attachments line up with the rest of the conversation rather than starting at the window edge, using
    the same wide-window rule as the navigation bars; the divider and the scrollbar still span the full
    width.
  - **The attachment bar shrinks while the keyboard is open on a short window.** On a window less than 480dp tall, such
    as a phone in landscape, the keyboard left too little height for the full bar and the message input together,
    pushing the input out of view. The bar now draws its
    attachments at an eighth of the window's shorter edge and drops their file names, which the attachments' accessibility
    labels still carry, animating to and from its full size as the keyboard opens and closes; taller windows, such as a
    tablet in landscape, keep the full bar.
  - **Bottom sheets use the whole window in landscape.** Material 3 caps a modal bottom sheet at 640dp
    and centres it, which suits a form on a desktop-sized window but left a phone in landscape showing a
    narrow card with unusable space either side. Every sheet in the chat — the attachment selection sheet,
    the list and time pickers, send transcript and the pre-chat survey
    — now spans the window whenever the window is wider than it is tall, and is built from one
    shared sheet so they cannot drift apart. The attachment picker, the end conversation sheet and the
    inactivity prompt are exceptions, because their own design draws them as a centred card in landscape as
    well; each opts out through the shared sheet's width parameter rather than by reaching past it. Any
    window taller than it is wide keeps the centred sheet it
    had, including a large tablet held upright: there the window has the height to show a centred sheet as
    designed, and the cap is what keeps its content readable. Note this is deliberately a narrower rule
    than the one the navigation bars and the conversation column use, which also treats a portrait window
    at least 840dp wide as wide. Lining sheet content up with the conversation's own content column is
    left to each sheet's design; the entries for the individual sheets say which ones do.
  - **The attachment selection sheet adapts its grid to the window.** It laid its attachments out in two
    fixed columns in every window, which stretched each one to roughly 390dp on a landscape phone, so a
    sheet meant to show a gallery showed two enormous tiles and no indication of how many more there were.
    The grid now takes four columns when the window is wider than it is tall and keeps two when it is
    taller, and carries a scrollbar in a strip reserved along its trailing edge. The count follows the
    window's own proportions rather than its width alone, and is read from the window rather than the
    device, so it tracks a rotation, a resize, or a split-screen change. On wide and foldable screens the
    title, the grid and the action bar all line up with the rest of the conversation using the same
    wide-window rule as the navigation bars; the action bar's divider still spans the full width.
  - **The attachment picker sheet stays centred in a landscape window on a device using three-button
    navigation.** As a centred card it stops well short of the side navigation bar, so it reserves room for
    that bar vertically only; the options keep their own inset, which is wider than any such bar.
  - **The list picker sheet now shares the same modal bottom sheet chrome as the pre-chat survey and
    inactivity sheets**, instead of building its own header and footer. Its title, options and Cancel/Submit
    buttons keep the design's 16dp portrait margin and widen to the shared 64dp inset in a landscape window,
    instead of staying at 16dp in every window. The subtitle now scrolls with the options rather than sitting
    in the fixed header, matching the design.
  - **The time picker sheet now shares the same modal bottom sheet as the list picker and pre-chat survey**,
    instead of a bespoke header and footer. Its title, date groups and slot grid kept a fixed margin in
    every window, including landscape, where the design widens it to match the conversation's own content
    column. The slot grid also gains a scrollbar in a strip reserved along its trailing edge once there are
    more slots than fit on screen, mirroring the list picker sheet's own scrollbar.
  - **The send transcript sheet follows the pre-chat survey's layout on wide and foldable screens.** Its
    title, email fields and Cancel/Submit buttons line up on the shared 64dp inset instead of sitting 16dp
    from the window edge. On a wide screen the sheet also hides the status bar, as the rest of the chat does,
    and in landscape it keeps clear of a navigation bar on the side of the window. In every window its title
    now sits in a fixed header on the sheet's own background, separated from the body by a divider and lined
    up with the email fields instead of 16dp further in, and the subtitle scrolls with the fields.
  - **The send transcript sheet scrolls in a short window.** It laid its header and both email fields out
    without scrolling and drew its footer over them, so in landscape the confirmation field could not be
    reached and the keyboard covered whichever field had focus. The content now scrolls, respects the
    keyboard inset, and reserves room for the footer.
  - **The full screen image and video viewers' headers now use the native Material 3 top app bar** instead
    of a hand-rolled row. Their back and share buttons sat 8dp from the window edge in every window, so in
    landscape they drifted well outside the content column the rest of the chat observes; they now inherit
    the same wide-window inset as the navigation bars. In portrait the header shifts by a few dp as it
    adopts the native bar's own padding and fixed height. A full-width divider separates the header from
    the media below it, matching the attachment selection sheet. Both viewers draw the same header, so the
    two cannot drift apart again.
  - **The video in the full screen viewer is capped at 640dp wide and centred.** It previously filled the
    window, which on a landscape tablet stretched it far past a watchable width. The cap is the width the
    inset header bar already uses, and it does nothing on a window narrower than that, so no compact layout
    changes.
  - **Attachment previews in a message are sized from the window the chat is drawn into** rather than from
    the device screen. Their size was a fraction of the display's shorter edge, which does not change when
    the window does, so a window smaller than the display — one side of a split screen, a freeform window,
    or a foldable folded shut — was given previews scaled for the whole device. A single preview, the grid
    of them and a video's thumbnail now all measure the window instead, and follow it across a resize or a
    split-screen change. Previews are unchanged in a window which fills the display, in either orientation.
- **`Popup.SatisfactionSurvey`** — a survey delivered through `ChatThreadActionHandler.popupFlow`. The survey page
  collects the answers; nothing needs to be reported back to the SDK. In Live Chat,
  `ChatThreadsHandler.threadsFlow` reports a closed conversation whose survey is still undelivered, instead of a new
  one, until the survey has been delivered through `popupFlow` and the list is refreshed; an integrator using only
  `chat-sdk-core` should collect `popupFlow` for it, or create a new thread.
- Chat UI now posts a local notification titled "Satisfaction Survey" when a survey arrives for a background thread in a multi-thread channel, showing the backend-provided prompt text. Opening that thread afterward still shows the in-app survey dialog. The survey is stashed (AES-256-GCM, Android Keystore) until then; integrators with their own backup rules should add `datastore/com.nice.cxonechat.ui.secure_settings.preferences_pb` to their exclusions (see the [Integration](../../README.md#integration) section)
- **The Chat UI shows a satisfaction survey as a blocking in-app dialog.** The dialog loads the survey page from the
  `Popup.SatisfactionSurvey` URL in a WebView, and is shown once per thread.
  In Live Chat, a survey sent while the app was closed or in the background is still shown: the closed
  conversation is kept and its survey displayed, followed by the usual "close chat" options once the survey is
  dismissed.

### 🔄 Changed

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
- **The time picker sheet header now uses the native Material 3 top app bar**, instead of a hand-rolled row.
- Bump `androidx.appcompat:appcompat` from 1.7.1 to 1.8.0. AppCompat 1.8.0 annotates its API with
  [JSpecify nullness annotations](https://jspecify.dev/), which are type-use. The SDK's own UI modules already compile with
  `-Xjspecify-annotations=strict`, so nothing changes for SDK consumers. Kotlin integrators who subclass or call AppCompat types directly
  can enforce the annotations in their own modules by adding `-Xjspecify-annotations=strict` to `freeCompilerArgs`.
- Explicit OAuth flow token-exchange errors that previously triggered a silent `ChatActivity` exit now show the standard blocking auth-error dialog, matching how other hard authorization failures are already presented. See **Deprecated** below.
- **Tapping a disabled or already-selected time picker message now shows a tooltip anchored to it**,
  explaining why its options are unavailable, instead of a snackbar at the bottom of the screen.
- **Tapping a disabled quick reply message now shows a tooltip anchored to it**, instead of a snackbar
  at the bottom of the screen, reusing the time picker message's own tooltip treatment so the two rich
  content types stay consistent.
- **A quick reply option whose text does not fit on one line at the chip's normal width now fills its
  row instead.** It previously stayed pinned to that width and wrapped its text across several lines
  inside a narrow chip; the decision is made by measuring the text's own single-line width rather than
  guessing from its character count, so it follows the actual rendered width for any font or locale.
- **The attachment selection sheet is now banded.** Its title and action bar sit on the sheet's own
  background with the attachments on a subtler one between them, each pair separated by a divider that
  spans the full width while the content inside each band stays in the conversation's column. Previously
  the whole sheet was one flat surface, so the title ran straight into the grid. In a short landscape
  window the attachment names can now fall below the fold; the grid scrolls to reach them.
- **The attachment picker sheet now matches its design.** The sheet which offers the camera, the gallery
  and a file browser no longer draws a drag handle; its title takes over the clearance the handle used to
  provide, sitting 32dp below the sheet's top edge and 16dp above the first option. The options no longer
  carry a trailing arrow either, which the design does not draw. Its title now reads "Select Attachment"
  rather than "Select attachment", and its options "Camera - Photo", "Camera - Video" and "Media Gallery"
  rather than sentence case, matching the capitalisation the design uses; the resource names are
  unchanged, so an integrator already overriding them keeps their own wording. In a landscape window the
  sheet stays the centred 640dp card its design draws instead of spanning the window as the chat's other
  sheets now do, and its options and the dividers between them are inset to sit in the design's content
  column. Its title is inset 16dp so that it lines up with the options it heads, as the design draws it,
  rather than the 24dp that every other sheet title shares; in a landscape window it keeps that 16dp
  inside the content column. No other sheet's title moves. The sheet is still dismissed by swiping it
  down or tapping outside it.
- **Share buttons on attachments now use the standard Material share icon** in place of the SDK's own, so the
  same action no longer carries two different icons. On an attachment in the conversation the glyph is 16dp
  inside its 34dp button, the size its design draws, rather than an icon's default 24dp, which crowded the
  outline. The selection sheet's button has the room and keeps 24dp.
- **Audio attachment messages match their current design more closely.** The progress bar is 6dp tall rather
  than 8dp and the gap that sat above and below it has moved beside it, between the bar and the elapsed and
  remaining times, which is both where the design puts it and what makes the cell shorter — the top and bottom
  insets grow a little against that. The tighter side padding gives the contents 8dp more room on each side
  rather than narrowing the cell. The seek icons are slightly smaller, the playback controls sit 68dp apart
  centre to centre, and the play and pause icon is 48dp rather than 40dp so it reads as the main control, which
  needed its Material 3 button container sized along with it, a 40dp container cropping anything larger.
- **Bump `androidx.compose.material3` to 1.5.0-beta01.** This is required for the single-row app bar
  with a subtitle, which is not public in the version resolved by the Compose BOM. Note this is a
  pre-release version of Material 3, chosen deliberately because the components this release needs are
  not available in a stable one.
- **Messages whose format cannot be displayed now explain themselves with a tooltip.** Tapping the fallback
  cell that stands in for a message the SDK cannot render previously raised a snackbar at the bottom of the
  window; it now shows a tooltip anchored to the message itself. The tooltip stays open until the reader
  dismisses it - by tapping outside it or pressing Back - rather than hiding itself on a timer, so a long
  explanation can be read at any pace and a screen reader can finish announcing it. While it is shown its
  window is touch-modal, so the dismissing tap is consumed and the conversation cannot be scrolled until it
  is closed - the cost of the focusable popup that also moves accessibility focus into the tooltip and lets
  Back dismiss it.
- **The offline screen no longer has a top app bar.** Its title and back button are gone. Its notice and the
  position-in-queue header are now a card inset from the window edges, with 12dp rounded corners and a 640dp
  maximum width. The `position_in_queue_supporting_text` string no longer contains a line break.
- **The archived-conversation notice above the input is redesigned.** It is now a divider and a 52dp bar on the
  default background, with an outlined archive icon and tertiary-colour text.
- **The preparing overlay's close button uses the string resource `loading_close_button_text`** instead of
  `close_chat`. If you override `close_chat` to change this button, override `loading_close_button_text` too.
- A PDF you send is previewed from your local file after it is sent, instead of being downloaded again. The file is copied once when the message is sent and reused as the attachment's cached preview
- The pre-chat survey's title and fields now sit 16dp from the sheet edge in portrait instead of 24dp, matching the list picker and time picker sheets
- The time picker's slots now share each row equally and fill the sheet's width, as many to a row as fit (for example three on a phone in portrait and six on a wide phone in landscape), with no gap at the row's end
- The list picker's options now follow the design's row layout: 12dp inside the row's left and right edges instead of 16dp, a 64dp image instead of 32dp, supporting text directly under the label, a 12dp gap before the selection check, and a minimum height of 48dp instead of 55dp

### ⚠️ Deprecated

- `RuntimeChatException.FeatureUnavailableException` is deprecated — the backend issue that caused explicit OAuth flow token-exchange failures is fixed, so the SDK no longer throws this exception. These errors now surface as `RuntimeChatException.AuthorizationError`, same as any other flow's token-exchange failure. The class is retained for source/binary compatibility and will be removed in a future major version.

### 🗑️ Removed

- **The `ic_share` drawable.** Nothing in the SDK references it now that the share buttons draw the
  standard Material share icon. It was never listed in `provided_resources.xml`, so it was not part of the
  set of resources the SDK offers for overriding, but an integrator who referenced it directly will need
  to supply their own copy.

### 🐛 Fixed

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
- Fix the remove button on attachments waiting to be sent having a touch target no larger than its 32dp circle,
  sitting over the attachment's corner rather than around the circle drawn beside it. It now has a 48dp tall target
  that covers the circle up to the gap before the next attachment, whose own taps are unaffected, and reaches over its
  own attachment to just short of its centre, so a tap there still opens the attachment rather than removing it.
- Fix the full screen video viewer drawing each clip in the proportions of the area it reserves rather
  than the clip's own. The player surface was handed the whole of that area, and a surface scales its
  buffer to its own bounds, so a clip shaped differently from it lost its edges to a crop — or, on devices
  where the crop the player asked for does not apply to a rotated recording, was distorted instead. The
  clip is now fitted inside that area, leaving the space it does not fill blank, and the playback controls
  span the clip rather than the area around it so they stay with the frame being watched.
- **Fix the send button's icon pointing downwards in right-to-left layouts.** The paper plane glyph is mirrored
  automatically for right-to-left reading, but the tilt that lifts its nose was a fixed rotation applied on top of that
  mirroring, and mirroring a rotation reverses it — so the two composed into a half turn, leaving the plane pointing
  down and backwards rather than up and toward the edge a message travels to. The tilt now follows the layout
  direction, which corrects the message input bar's send button and the voice message recording bar's together, because
  both draw the same control; left-to-right layouts are unchanged.
- Fix "load more messages" pagination sending the newest message's timestamp as the cursor instead of the oldest, which could prevent older messages from loading; the request now uses the oldest message in the thread as the pagination cursor
- Correct the backup-exclusion guidance in the README integration notes: the encrypted preferences file path was listed incorrectly and would not have matched anything, the `com.nice.cxonechat.secure.xml` and cookie-store files were missing from the exclusion list entirely, and no guidance existed for `android:dataExtractionRules`, which apps targeting API 31+ need instead of `fullBackupContent`. Integrators who applied the previous guidance should update their backup rules
- Correct the integration guides and case studies against the 4.0 API. Examples that call `suspend` functions now show the surrounding coroutine — including `ChatBuilder(...)` and `ChatInstanceProvider.configure(...)`, which are themselves `suspend` and previously appeared at top level; Koin setup uses `includes(UiModule.chatUiModule())` with the current `customerFieldsProvider` and `contactFieldsProvider` parameter names; `closeSuspending()` replaces the removed `stop()` and is shown from `onStop()` in a coroutine that outlives the lifecycle scope starting it, rather than blocking a lifecycle callback — closing in `onPause()` also ended sessions the user was about to return to, and a close cut short at `ON_DESTROY` leaves the provider holding a `Chat` that no later `connect()` restores; that close is now guarded on the provider's state, because `closeSuspending()` arrives at `Prepared` whether or not it found a `Chat` to close, so closing while the provider is `Initial` or `Preparing` cancels the build in flight and strands it where both `connect()` and `prepare()` do nothing, and closing on `SdkNotSupported` discards the record that the backend refused this SDK version — `cancel()` is the call for all three, and the listener example now handles `SdkNotSupported` as well, without which its `when` over `ChatState` did not compile; the provider listener is registered in `onStart()` to pair with that, because `addListener` does not de-duplicate and a resumed activity would otherwise register twice; that same `onStart` now calls `prepare()` when it finds the provider `Initial`, because `addListener` does not replay the current state and a freshly started process would otherwise sit in `Initial` with no callback ever arriving; custom-field definitions are read from `chat.threads().preChatSurvey?.fields`; and the logging case study names `timedScope` and `logger.warning`
- Publish a resolvable Koin version to consumers of `chat-sdk-ui`. The module exposes `koin-android` and `koin-annotations` as `api` dependencies, but the Koin BOM that versions them was declared as `implementation`, so it was published into the runtime variant only. The compile variant listed both artifacts with no version at all, and an integrator who did not declare Koin themselves could not build — resolution failed with `Could not find io.insert-koin:koin-android:`. The BOM is now an `api` dependency, so the compile variant carries the version. Anyone who worked around this by adding a Koin dependency of their own can drop it
- Document that the Chat UI Koin setup has to register `androidContext(...)`. `UiModule.chatUiModule()` does not register it and the UI resolves `Context` and `Application` from the graph, so every previous `startKoin` example — in the guides and in the published `chatUiModule()` reference — produced a setup that started cleanly and then failed on the first injection inside `ChatActivity`, which reads as an SDK fault rather than a missing setup line. The UI configuration guide also no longer asks for a `koin-core` dependency: `chat-sdk-ui` exposes `koin-android` as an `api` dependency, so Koin needs no declaration of your own, and `androidContext(...)` ships in `koin-android` rather than `koin-core`
- Replace documented names that are absent from the public API: `ChatSdkConfig` becomes `SocketFactoryConfiguration`, `PreChatSurveyType` becomes `FieldDefinition`, the removed `RefreshToken` event is documented as SDK-managed token refresh, `ArchiveThreadEvent` becomes `ChatThreadHandler.archive()`, and the thread event list names the public `ChatThreadEventHandlerActions` functions rather than internal event classes
- Document the import that analytics and thread-event actions require: they are extension members of `ChatEventHandlerActions` and `ChatThreadEventHandlerActions`, so the actions must be imported from the object, as in `import com.nice.cxonechat.ChatEventHandlerActions.pageView`. Importing the object itself leaves the call unresolved, and the previous examples showed no import at all
- Correct the `chat-sdk-ui` integration requirements, which stated the wrong minimum supported Android version and pinned library versions the module no longer ships. The `minSdk`, `compileSdk` and `targetSdk` levels are stated in the root `README.md` under *Android SDK levels*, and the module requirements link there
- Dependency snippets presented as `build.gradle.kts` now use valid Kotlin DSL rather than Groovy-only syntax, and the published API reference for `ChatBuilder.build()` no longer shows a literal escape sequence in place of a dash
- Fix source links in the localization, theming and UI-configuration guides that led nowhere. A markdown target beginning with `/` is resolved from the host root rather than the repository, so links written that way pointed at pages that do not exist; they are now relative paths that open the file they name
- Correct the `SocketFactoryConfiguration` example, which selected the environment through a `CXoneEnvironment.YourRegion` constant that does not exist. It now uses a real region and names the ones available
- Fix the repository setup step, which declared the Maven repository at the top level of `settings.gradle.kts`. A settings script has no top-level `repositories`, so the snippet did not evaluate; it is now nested in `dependencyResolutionManagement { repositories { … } }`
- Fix explicit OAuth flow reconnecting with the refresh token even when the underlying access token was still valid; it now reuses the still-valid access token to obtain a new transaction token, only falling back to the refresh token once the access token itself has expired
- Detect OAuth token rejection via the backend's actual HTTP 400 response instead of HTTP 401. The SDK looked for a
  401 the backend never sends, so the implicit flow treated a rejected token like any other error. It now calls
  `TokenDelegateListener.onNewTokenRequested(TOKEN_INVALID)` to get a fresh token, and reports
  `InvalidAccessTokenException` when that one is rejected too
- Fix the drag handle of the list picker and send transcript sheets being announced in English
  regardless of locale — it used a hardcoded string instead of a localizable resource.
- Fix the share button beside an attachment message sitting closer to the bubble than its design draws it.
  The gap the message frame leaves between a bubble and the action beside it was 8dp where the design calls
  for 12dp. Only a message which carries an action moves: a message without one has nothing on that edge to
  shift, and a bubble is capped well short of the window, so no message re-wraps.
- Fix the tile for an attachment with no preview of its own — a document or any other file — being drawn
  without the outline its design gives it and painting no background of its own. Its icon, drawn in the tone
  meant for the brand colour, vanished wherever that colour was not behind it: on a message carrying a single
  attachment, whose bubble is drawn transparent, on an agent's pale bubble, and in the attachment picker sheet,
  where it was too faint against the sheet to make out. The tile now draws the outline in the icon's tone. The tile
  also paints the background its design gives it — the brand colour for the customer, a neutral surface for
  the agent — so on the customer's bubble the icon has full contrast, and on the agent's surface it is the
  outline and the extension label which mark the tile out, the icon being close to its backdrop in tone by
  design. The larger icon the picker sheet uses takes a neutral tone instead, that sheet having no message
  behind it to take a colour from. The extension label is unchanged. Both themes and every orientation are
  covered.
- Fix a PDF attachment in a message not being drawn inside its bubble while its first page is still
  rendering. Until the page arrived the message stood in with the tile the attachment picker draws, which
  takes a fixed size and paints no background of its own. On a message carrying a single attachment, whose
  bubble is drawn transparent, that left nothing behind the tile and the attachment looked as though it had
  no bubble at all; on a message carrying several, the tiles overflowed the cells they sit in. A PDF still
  rendering now shows the same tile as any other file: it fills the space the page will take and carries the
  same outline and backdrop — the brand colour for the customer, a neutral surface for the agent — so when
  the page arrives it replaces the tile without the attachment changing size or position. The picker sheet's
  own tile keeps the size and neutral tone it had.
- Fix a PDF attachment leaking its renderer and the open file it reads from when the attachment leaves the
  screen while its document is still being opened — scrolling a conversation quickly past a large document, or
  leaving the chat while one opens. Opening runs off the main thread, and the thumbnail took ownership of the
  document only once that work handed its result back. Work cancelled after it had already opened the document
  handed nothing back, so the renderer and the file it reads from were left with no owner at all and stayed open
  until the garbage collector eventually reclaimed them, however long that took; the larger the document, the
  longer the window in which this could happen, opening being slowest exactly when a reader is most likely to
  scroll on. The thumbnail now takes ownership as the document is opened rather than after, and a file whose open
  finishes after the work asking for it was cancelled is closed rather than dropped. That covers a file already on
  the device too: it was handed back through a path which discarded it in exactly that window, so its descriptor
  stayed open on the same terms.
- Fix PDF attachments that share a file name showing the same preview. The preview was cached under the file name, so a second PDF with that name showed the first one's page. Each attachment's preview is now cached separately
- Fix a destroyed `ChatActivity` being kept in memory after a device rotation until the chat was closed; the voice-message recorder held a reference to it
- Fix closing the chat while a voice message is being recorded leaving the microphone recorder and its file open; they are now released when the chat closes
- Fix the options of a list field in the pre-chat survey and in the "Edit pre-chat survey custom fields" sheet opening under the keyboard. With the keyboard shown, the list now opens above the field when there is no room below it

### 🔒 Security

- Enforce minimum `io.netty` 4.1.138.Final in buildscript resolutionStrategy to address AIKIDO-2026-592216 | GHSA-cg2g-fxr4-mg8m (build-time
  only, no SDK artifact impact)
- Enforce minimum `org.jsoup` 1.23.1 via Gradle resolutionStrategy to address CVE-2026-71497 / GHSA-pmhh-3w7g-xqp8 (build-time only, no SDK
  artifact impact)
- Enforce minimum `org.freemarker:freemarker` 2.3.35 via Gradle resolutionStrategy to address GHSA-27j2-h3m2-8237 (documentation generation
  only, no SDK artifact impact)
- Enforce minimum `com.fasterxml.jackson` 2.18.10 via Gradle resolutionStrategy to address CVE-2026-68497 / GHSA-q4xh-88c3-wmh7 (build-time
  only, no SDK artifact impact)
- Enforce minimum `org.bouncycastle` 1.86 in buildscript resolutionStrategy to address AIKIDO-2026-322957 (build-time only,
  no SDK artifact impact)
- Enforce minimum `org.apache.httpcomponents:httpclient` 4.5.13 via Gradle resolutionStrategy to address CVE-2020-13956.
  The vulnerable version reaches only the Android Gradle Plugin's build tools (build-time only, no SDK artifact impact)
- Enforce minimum `org.jetbrains.kotlin:kotlin-stdlib` 2.1.0 via Gradle resolutionStrategy to address AIKIDO-2026-550292.
  The SDK itself already ships a newer Kotlin; the floor lifts the older copy used by build tooling (build-time only, no
  SDK artifact impact)

### ⚠️ Known Issues

- **Thread list after a close and a recovery at the same moment**: In a single-thread channel, if the agent closes the conversation at the same moment as a thread recovery (for example, right after reconnecting), `threadsFlow` can still show the thread as open while `threadFlow` shows it closed, until the thread's state changes again. `threadFlow` and `ChatThreadHandler.get()` are not affected. 4.0.0 never showed the close in the list at all.
- **Thread updates right after a handler is created**: A thread handler applies updates from shortly after it is created, not from the very first moment. An update that arrives within that short window can still be missed until the next update for the thread. Closing the window would move the handler's work onto the socket thread, so it was not done in this release.
- **Local `customerId` kept for a new OAuth or secured-cookie visitor**: the visitor is registered with the locally generated `customerId` and is not registered again with the real identity after authentication. Before 4.0.1 the registration was retried and used the real identity.
