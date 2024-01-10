<a name="unreleased"></a>
## [Unreleased]


<a name="1.2.1"></a>
## [1.2.1]

### Bug Fixes
- Fix minification issues with ChatInstanceProvider


### Features
- Improve Java compatiblity for 1.2.1


<a name="1.2.0"></a>
## [1.2.0] - 2023-09-26


### Bug Fixes
- Remove messages with duplicate id from thread
- reenable tests depending on android.util.Patterns
- Successful thread-archived event will trigger thread list refresh
- Persist deviceToken until the value is updated
- Fix StoreVisitor upload failure message - server message part
- Create/update Visitor using new endpoint
- fix issue with color settings fields not reflecting day/night
- improve handling of notifications
- Allow empty/non-existent TEXT in QuickReply plugins.
- prepopulate configuration menu with last known custom values
- fix thread update flow and remove extraneous refresh requests
- Fix misc errors around state changes
### Dependency Change
- Bump androidx.core:core-ktx from 1.10.0 to 1.10.1
- Bump com.squareup.okhttp3:okhttp from 4.10.0 to 4.11.0
### Features
- Create new chat-sdk-ui library from the prior sample application.
- use events endpoint for analytics events
- BREAKING CHANGE - Convert unnecessary abstract classes to interfaces
- Merge SampleApplication project to the SDK project
- internally mark archived threads as archived pending success/failure from server
- Convert ListPicker message to Compose
- Convert thread list screen to JetPack Compose
- Compose Chat UI
- add support for timeSpentOnPage
- Support updated RecoveredThread event
- Add ChatInstanceProvider
- integrate new Chat UI module with Store application
- Allow colorization of chat sdk screens
- Add support for customizable brand logo
- Android: implement conversion events in sample application

<a name="1.1.0"></a>
## [1.1.0] - 2023-06-26

### Bug Fixes
- Allow lenient parsing of UUID type
- Successful thread-archived event will trigger thread list refresh
- Update thread agent correctly
- Allow application to respond to the websocket session state & reconnect
- Use OkHttp for WebSocket communication
- Add missing consumer ProGuard rule for GSON classes
- Modify Proguard/R8 rules to be compatible with R8 fullMode
- Fix threading issue introduced with postback support
### Dependency Change
- Bump androidx.core:core-ktx from 1.9.0 to 1.10.0
- Bump org.jetbrains.dokka from 1.6.10 to 1.8.10
- Bump org.jetbrains.kotlin.android from 1.6.10 to 1.8.10
### Features
- Increase targetSdk 31 -> 33
- implement/fix LoadThreadMetadata and add loadMetadata entry to ChatThreadEventHandlerActions
- Dynamic pre contact survey
- Documentation update
- create ContentDescriptor.DataSource to facilitate minimizing memory usage
- Modify Custom Fields storing behavior
- implement RichLink message types
- implement ListPicker message type
- add support for QUICK_REPLIES message types
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

[Unreleased]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.2.1...HEAD
[1.2.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.2.1...1.2.0
[1.2.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.1.0...1.2.0
[1.1.0]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.0.1...1.1.0
[1.0.1]: https://github.com/nice-devone/nice-cxone-mobile-sdk-android/compare/1.0.0...1.0.1
