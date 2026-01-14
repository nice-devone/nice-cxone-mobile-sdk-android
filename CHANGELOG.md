<a name="unreleased"></a>

## [Unreleased]

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

- `RemoteLogger` to report errors to the server
- Inactivity Popup Support
- `RemoteLogger` to public API for error reporting
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

- Handle failure & added auto retry mechanism during visitor creation/update
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

- Re-enable tests depending on android.util.Patterns
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
    - Load thread metadada
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

[Unreleased]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.1.1...HEAD

[3.1.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/3.1.0...3.1.1

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
