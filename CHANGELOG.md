
<a name="1.1.0-alpha04"></a>
## [1.1.0-alpha04] - 2023-05-31

### Bug Fixes
- Allow lenient parsing of UUID type

<a name="1.1.0-alpha03"></a>
## [1.1.0-alpha03] - 2023-05-26

### Bug Fixes
- Successful thread-archived event will trigger thread list refresh

<a name="1.1.0-alpha02"></a>
## [1.1.0-alpha02] - 2023-05-22

### Features
- Increase targetSdk 31 -> 33

<a name="1.1.0-alpha01"></a>
## [1.1.0-alpha01] - 2023-05-15

### Bug Fixes
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


<a name="1.0.1-alpha01"></a>
## [1.0.1-alpha01] - 2023-03-06

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

[Unreleased]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.1.0-alpha04...HEAD
[1.1.0-alpha04]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.1.0-alpha03...1.1.0-alpha04
[1.1.0-alpha03]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.1.0-alpha02...1.1.0-alpha03
[1.1.0-alpha02]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.1.0-alpha01...1.1.0-alpha02
[1.1.0-alpha01]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.0.1...1.1.0-alpha01
[1.0.1]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.0.1-alpha01...1.0.1
[1.0.1-alpha01]: https://github.com/BrandEmbassy/cxone-mobile-sdk-android/compare/1.0.0...1.0.1-alpha01
