# Integrator's Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Proguard / R8](#proguard--r8)
3. [Setting Up](#setting-up)
4. [Logging](#logging)
5. [Startup](#startup)
6. [Runtime Exceptions](#runtime-exceptions)
7. [Configuration](#configuration)
8. [Integrate UI module](#integrate-ui-module)
9. [Manual Username Update](#manual-username-update)
10. [Custom Fields](#custom-fields)
11. [Global Events](#global-events)

Building your own UI instead of using the UI module? These live in
[Custom UI Implementation](custom-ui-implementation.md):

- [Push Notification Tokens](custom-ui-implementation.md#push-notification-tokens)
- [Conversations (formerly Threads)](custom-ui-implementation.md#conversations-formerly-threads)
- [Thread](custom-ui-implementation.md#thread)
- [Message States](custom-ui-implementation.md#message-states)

## Introduction
This document will guide you through the necessary steps to integrate CXone Chat SDK into your application.

> [!NOTE]
> Please follow the steps diligently to ensure you're using the SDK correctly.
> If you're unsure about any method, consult the documentation provided with the SDK.

All examples are written in Kotlin, though you might use Java with this SDK. The SDK version you've been provided is heavily obfuscated to discourage you from using internal APIs.

> [!IMPORTANT]
> We strongly urge you not to use reflection for any of CXone SDK classes.
> Your code **will** break from release to release.

> [!NOTE]
> Note that in every example, the instance of any given Handler is created at most once.
> Make sure to follow suit.

## Proguard / R8

There are specific Proguard rules needed for this library.
They are bundled with the chat-sdk-code aar and are provided automatically with Maven, alternatively they can be found in a file
[chat-sdk-core/consumer-rules.pro](../chat-sdk-core/consumer-rules.pro) and copied directly to your rule file.

## Setting Up

Ensure you've received your **Region**, **Brand ID** and **Channel ID**.

- **Region**: Referenced in code as "environment". Typically closest to your main deployment region.
  - Examples: `NA1`, `EU1`, `AU1`, `CA1`, `UK1`, `JP1`
- **Brand ID**: Typically a 4-digit integer.
- **Channel ID**: Typically a UUID string prefixed with "chat_".

Once you got all of these data, you may proceed.

> [!NOTE]
> If you're unsure where to get these values, you should consult your CXone representative or local
> managers depending on your company structure.

## Logging

Logging events from the CXone Chat SDK and the Chat UI module are normally passed to the `Logger` instance, which
by default is a no-op logger.
To include logging to console (or any other destination/s), you need to provide an instance of `Logger` to the SDK.
For more information about logging, see the [Logging case study](chat-sdk-core/cs-logging.md).

## Startup

First you need to obtain a `Chat` instance. You can achieve that through our `ChatBuilder` or you can use `ChatInstanceProvider` which is covered [here](chat-sdk-core/cs-instance-holder.md).
We recommend the use of `ChatInstanceProvider` since it provides state tracking for the chat instance, but to cover both cases we will show you how to use the `ChatBuilder` in the following example.

> [!NOTE]
> As of version 4.0 the SDK is **coroutine-first**: `ChatBuilder.build()` and `Chat.connect()` are
> `suspend` functions, and chat state is observed through `Chat.stateFlow` rather than a listener.
> Java consumers should add the `com.nice.cxone:chat-sdk-core-java` dependency and use the
> `ChatBuilderJavaInterop` / `ChatJavaInterop` helpers — see the
> [Coroutine API migration guide](migration/MIGRATION_COROUTINE_API.md).

```kotlin
  val config = SocketFactoryConfiguration(
    CXoneEnvironment.NA1.value, // replace with your region: NA1, EU1, AU1, CA1, UK1 or JP1
    yourBrandId,
    yourChannelId
  )

  // ChatBuilder(...) is a suspend factory and build() is a suspend function — call them from a coroutine.
  scope.launch {
    val chat = ChatBuilder(context, config)
      .setDevelopmentMode(BuildConfig.DEBUG) // Development mode shouldn't be enabled in production
      .setTokenDelegateListener(yourTokenDelegate) // (1) Implicit OAuth flow
    // .setAuthorization(yourAuthorization) // (1) Explicit OAuth flow
      .setUserName("firstName", "lastName") // (2)
      .build()
    // TODO save chat instance
  }
```

- (1) Authorization
  - For third-party OAuth you can choose:
    - **Explicit flow**: call `.setAuthorization(Authorization(code, verifier))`
    - **Implicit flow**: call `.setTokenDelegateListener(tokenDelegate)`
  - If both are set, the SDK uses the **explicit** flow.
  - If you don't use OAuth, don't call either OAuth API.
  - For full setup guidance, see the [OAuth Third-Party Authentication case study](chat-sdk-core/cs-oauth-third-party.md).
- (2) Username
  - Usage depends on the fact if you are using OAuth.
    The OAuth users typically won't need to set username,
    since user details will be retrieved from OAuth backend.
  - If you are using a manual username setup, please follow instructions on updating the username
    (if it can change in your application).

> [!NOTE]
> The `build` method creates an instance of Chat which is ready for analytics usage; for the chat
> use-case it needs to be connected.
> Cancelling the coroutine that calls `build()` (or `connect()`) cancels the operation, replacing the
> `Cancellable` returned by the old callback API.

> [!IMPORTANT]
> If the startup was not successful and `build` threw, be sure to check your configuration as the
> server might have rejected the request. Read the documentation for the `build` method for more
> clarity on the subject.

---

Now you can use the CXone Chat SDK for sending of analytics events (which are used for automation).
If you also need to activate the chat, you will need to connect it to backend and let Chat perform basic preparation of
the instance.

1. Call the `suspend fun chat.connect()` from a coroutine to connect to the backend.
2. Observe `chat.stateFlow` to learn when the connection is established and ready. After
   `ChatStateEvent.Connected`, the `Chat` performs final background tasks to fully prepare the
   instance (retrieval of the thread in single-thread mode or thread list in the multi-thread mode).
3. `Chat` reports it is fully ready by emitting `ChatStateEvent.Ready` on `stateFlow`.

```kotlin
  scope.launch {
    chat.stateFlow.collect { event ->
      when (event) {
        ChatStateEvent.Connecting -> Unit
        ChatStateEvent.Connected -> Unit
        ChatStateEvent.Ready -> { /* chat is ready for interaction */ }
        ChatStateEvent.UnexpectedDisconnect -> { /* offer reconnect */ }
        is ChatStateEvent.RuntimeException -> handle(event.exception)
      }
    }
  }
  scope.launch { chat.connect() } // suspend
```

Great! Now you're ready to use the CXone Chat SDK.

> [!IMPORTANT]
> It is recommended to wait for the `ChatStateEvent.Ready` event before using the chat instance.

> [!IMPORTANT]
> Chat instance maintains open socket connection to backend, until `chat.close()` is called, or the
> application process is terminated.
> It is the responsibility of the integrating application,
> to close the chat when the user leaves the part of application dedicated to chat
> (usually dedicated Activity) as it is outlined in terms of use for the SDK.

## Runtime Exceptions

The SDK communicates critical errors as `ChatStateEvent.RuntimeException` events on `Chat.stateFlow`. Your application should collect this flow and handle the various error conditions appropriately. (Java consumers use `ChatJavaInterop.observeState(...)` — see the [Coroutine API migration guide](migration/MIGRATION_COROUTINE_API.md).)

### Exception Types

#### RuntimeChatException.ConnectionTokenFailed

This exception is raised in the **explicit OAuth flow** when SDK recovery of an expired transaction token
cannot proceed (for example, missing `refresh_token`, or the refresh-token exchange did not return a
usable access token).

**Explicit flow behavior:**
- On token expiry, if the underlying OAuth access token is still valid, the SDK reuses it directly to
  obtain a new transaction token — the `refresh_token` is not spent
- If the access token has also expired, the SDK spends the `refresh_token` to mint a new access token,
  then uses it to obtain a new transaction token
- If either step succeeds, the user is not prompted
- `ConnectionTokenFailed` is reported only when neither recovery path can proceed and re-authentication is required

**Required Actions (explicit flow):**
1. Update UI state to show the OAuth/login dialog to the user
2. Initiate OAuth re-authentication with your OAuth provider
3. Obtain new authorization credentials (authorization code and code verifier)
4. Set the new authorization again using `setAuthorization(...)` (via `ChatBuilder` or the suspending
   `ChatInstanceProvider.configure(context) { authorization = ... }`)
5. Open ChatActivity again so chat will automatically reconnect with the new credentials

**Example Implementation:**

```kotlin
scope.launch {
    chat.stateFlow.collect { event ->
        if (event is ChatStateEvent.RuntimeException) {
            when (val exception = event.exception) {
                is RuntimeChatException.ConnectionTokenFailed,
                is RuntimeChatException.InvalidRefreshTokenException -> {
                    // SDK could not recover the session in the background - re-authentication required
                    handleReAuthenticationRequired()
                }
                else -> {
                    // Handle other exceptions
                    logger.error("Runtime exception: ${exception.message}")
                }
            }
        }
    }
}

    private fun handleReAuthenticationRequired() {
        // SDK could not recover the session: either no credential was usable (ConnectionTokenFailed),
        // or the backend rejected the refresh_token itself (InvalidRefreshTokenException)
        Log.w("Chat", "OAuth fallback re-authentication required")

        // Update UI state to show OAuth/login dialog
        // This triggers the UI to show the login dialog to the user
        // The user then completes OAuth re-authentication
        // Once new authorization is obtained and set via setAuthorization(...),
        // the chat will automatically reconnect with the new credentials

    // TODO: Implement app-specific UI state update (e.g., navigate to login screen or show login dialog)
   }
```

> [!IMPORTANT]
> For explicit OAuth authentication, you **must** handle `ConnectionTokenFailed` and `InvalidRefreshTokenException` by re-initiating the OAuth flow as a fallback path.
> The SDK cannot automatically recover from either error because it does not have direct access to OAuth provider credentials.

For detailed guidance on implementing OAuth authentication and handling token expiration, see the [OAuth Third-Party Authentication case study](chat-sdk-core/cs-oauth-third-party.md).

#### RuntimeChatException.AuthorizationError

This exception indicates that user authorization has failed. This can occur during initial connection or when the backend rejects authorization credentials.

**Common Causes:**
- Invalid authorization code or code verifier
- Expired authorization code (before exchange with backend)
- Backend OAuth configuration issues
- Network errors during token exchange

**Required Actions:**
1. Log the error details for debugging
2. Check your OAuth provider integration
3. Verify the authorization credentials are current and valid
4. Create a new chat instance with correct credentials

#### RuntimeChatException.TokenDelegationFailedException

This exception is reported in the **implicit OAuth flow** when `TokenDelegateListener.onNewTokenRequested(...)` fails (for example, user cancelled sign-in or token fetch failed).

**Recommended Actions:**
1. Show re-login UI or a retry action
2. Re-run your OAuth flow and return a valid `OAuthToken` from the delegate

#### RuntimeChatException.InvalidAccessTokenException

This exception is reported in the **implicit OAuth flow** when the backend rejects the delegated JWT and automatic recovery also fails.

**Recommended Actions:**
1. Prompt the user to sign in again
2. Reconnect with a fresh token delivered by your `TokenDelegateListener`

#### RuntimeChatException.InvalidRefreshTokenException

This exception is reported in the **explicit OAuth flow** when the backend rejects the stored `refresh_token` while the SDK tries to exchange it for a new access token (for example, the refresh token expired or was revoked). Like `InvalidAccessTokenException`, this is terminal — the SDK has no credential left that can recover the session automatically; the two differ only in which flow and credential were rejected.

**Recommended Actions:**
1. Prompt the user to sign in again
2. Re-run your PKCE OAuth flow and reconnect via `ChatBuilder.setAuthorization(...)` with a fresh authorization code and verifier

#### RuntimeChatException.ServerCommunicationError

This exception indicates a problem communicating with the CXone backend.

**Common Causes:**
- Network connectivity issues
- Backend service unavailable
- Invalid request format

**Recommended Actions:**
1. Check network connectivity
2. Implement retry logic with exponential backoff
3. Inform the user of the connectivity issue

### Best Practices

1. **Always Implement Exception Handling**: Every integration should handle `ChatStateEvent.RuntimeException` from `Chat.stateFlow`
2. **Provide User Feedback**: Inform users when action is required (e.g., re-login)
3. **Log for Debugging**: Log exception details to help diagnose integration issues
4. **Test Error Scenarios**: Verify your error handling works correctly, especially token expiration
5. **Graceful Degradation**: Ensure your app remains functional even when chat is unavailable

## Configuration
You can use the chat `configuration` property to support different UI/UX flows in your application and also to
preemptively verify that your assumptions about active chat configuration are correct.

```kotlin
fun readChatConfiguration() {
    val chat = ChatInstanceProvider.get().chat ?: return
    val chatConfiguration = chat.configuration
}
```

## Integrate UI module

> [!NOTE]
> This is excerpt from the [chat-sdk-ui/README.md](../chat-sdk-ui/README.md) file, for the most up-to-date information please refer to the
> original file.

To use the chat-sdk-ui module, you need to add it as a dependency in your project. You can do this by adding the following line to your
`build.gradle.kts` (or `build.gradle`) file:

```kotlin
    implementation("com.nice.cxone:chat-sdk-ui:${chatSdkVersion}")
```

### Before you start Chat UI

#### Initializing the Chat SDK

Before you start using the Chat UI, you need to initialize the CXone Chat SDK in your application using the `ChatInstanceProvider` class.
Specifically the `ChatInstanceProvider` has to be at least in the `Prepared` state, which means that the `ChatInstanceProvider` has to be
initialized with the `SocketFactoryConfiguration`.
This can be done in the `onCreate` method of your `Application` class or in a dedicated `androidx.startup.Initializer` implementation.

#### Initialize the Chat UI Dependency Injection

If you are not using Koin in your application you can just call

```kotlin
   import org.koin.android.ext.koin.androidContext

   // from Application.onCreate()
   startKoin {
      androidContext(applicationContext)
      includes(UiModule.chatUiModule())
   }
```

Otherwise call `includes(UiModule.chatUiModule())` inside your existing `startKoin { }` block, alongside your own
`modules(...)` calls — `chatUiModule()` returns a `KoinConfiguration`, which `includes` accepts, not a `Module`.

Either way the Koin setup has to register `androidContext(...)`. `chatUiModule()` does not register it, and the UI
resolves `Context` and `Application` from the graph, so without it the chat fails when it is opened rather than when
Koin starts.

##### Custom fields provider

If you are using custom fields (applies to both contact and customer) in the chat which are not user editable, you need to provide the
`UiCustomFieldsProvider`
implementation(s) to the UI Koin module. These implementations will be used to append the custom fields to events sent to the server.

##### Chat UI logging

If you want to enable logging in the Chat UI, you can provide an instance of `Logger` to the Koin UI module as parameter `logger`, default
instance is a No-op variant.

### Starting the Chat UI

To start the Chat UI call `ChatActivity.startChat(from: Activity)` method.

### Push notifications

For the push notifications to work, you need to add the Firebase Cloud Messaging dependency to your project.
See the [Firebase Cloud Messaging documentation](https://firebase.google.com/docs/cloud-messaging/android/client) for more instructions
on how to set up FCM in your project.

For more focused information about this topic see the [cs-push-notifications.md](chat-sdk-core/cs-push-notifications.md).

#### Provide required resources

The UI module defines drawable resource id, for which the integrating application must provide the corresponding drawable resources.
The following resources are required:

* `ic_chat_push_service` - used for the push notification icon as a fallback when the notification does not define a custom icon.
  Application
  should provide a drawable resource which follows instructions
  from [NotificationCompat.Builder.setSmallIcon()](https://developer.android.com/reference/androidx/core/app/NotificationCompat.Builder#setSmallIcon(int)).

## Manual username update
If you are not using OAuth user authentication and your application allows to change username in
application, you will have to close current instance of chat and create a new instance of chat using
the `ChatBuilder`.
The updated username can be supplied to the builder before chat instance is created.

## Custom Fields
Custom fields represent metadata about customers / users.
There are two types of these custom fields based on the context of the said data:
1. Customer Custom Fields — These are global for all chat threads and shouldn't contain information about one specific conversation.
2. Contact Custom Fields — These contain metadata relevant to the one specific chat thread / conversation.

Custom fields are defined as part of the channel configuration (on backend), and only defined custom fields can be supplied to the chat instance.

### Custom Field definitions
Custom field definitions reach the SDK as part of the pre-chat survey, and are read from
`chat.threads().preChatSurvey?.fields` — a `FieldDefinitionList`. Each definition provides a `label` field which can hold
a human-readable label (or reference to a string resource) and a `fieldId` which is used for adding of custom field
values. Use `lookup(fieldId)` to find one:

```kotlin
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.state.lookup

val definition = chat.threads().preChatSurvey?.fields?.lookup("referral") as? FieldDefinition.Text
```

> [!NOTE]
> Field definitions are provided only for custom fields which are used for the pre-chat survey.
> It is possible to use other custom fields, even without a definition supplied in the configuration — pass the key
> directly, as shown in [Adding customer custom fields](#adding-customer-custom-fields).
> Backend can reject undefined (meaning the definition is not present in channel configuration on backend) custom fields.
> There is no definition list on `Chat.configuration`; the client-side validation API that exposed one was removed.

#### Custom field types & expected values
| Type      | Required value                         | Validations                                                                                                                    |
|-----------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| Text      | Any `String`                           | Value is validated against pattern `android.utils.Patterns.EMAIL_ADDRESS` is used if definition has flag `isEMail` set to true |
| Selector  | `nodeId` from selected `SelectorNode`  | Value must match id of one of the nodes                                                                                        |
| Hierarchy | `nodeId` from selected `HierarchyNode` | Value must must match id one of the **leaf** nodes                                                                             |

#### Adding custom fields using definitions
To create custom field entry for `Map<String,String>`, which is used to add custom fields via `ChatFieldHandler`,
combine custom field definition `fieldId` with a value matching defined type.

### Accessing current customer custom fields
Immutable collection of customer custom fields is available as field of the Chat instance

```kotlin
fun readCustomerCustomFields() {
    val chat = ChatInstanceProvider.get().chat ?: return
    val customerCustomFields = chat.fields
}
```

> [!NOTE]
> Note that the instance won't be modified if the custom fields get updated.
> It should be considered as a current snapshot.

> [!NOTE]
> Customer custom fields can get updated after the thread list refresh.

> [!NOTE]
> Note that it will always only contain custom fields which have a valid definition; legacy fields are filtered out.

### Adding customer custom fields
You can append customer custom fields through Chat instance ChatFieldHandler handler.
You may be required to supply such values based on automatic flows attached to your chat channel
configuration.

  ```kotlin
  fun addReferralUnknown() {
    val chat = ChatInstanceProvider.get().chat ?: return
    // Custom fields are added by key, whether or not they have a pre-chat definition:
    chat.customFields().add(mapOf("referral" to "referral_unknown"))
  }
  ```

  Look a definition up with `lookup(fieldId)` only when you need its declared type — for example to confirm
  `"referral"` is a `FieldDefinition.Text` before sending free-form text, or to read the allowed `nodeId`s of a
  `FieldDefinition.Selector`. The key you send is the `fieldId` either way.

> Supplying the same key with different value, will overwrite the existing custom field.

> Customer custom fields can be used for creation of an automatic welcome message or other automatic
> events.
> If you're unsure if these values will be required, you should consult your CXone representative
> or local managers depending on your company structure.

## Global Events

### Analytics events

The actions listed below are `suspend` extensions on `ChatEventHandler` declared inside
`object ChatEventHandlerActions`. Import each action you use — importing the object alone does not bring its extension
members into scope — and call it from a coroutine:

```kotlin
import com.nice.cxonechat.ChatEventHandlerActions.pageView

scope.launch { chat.events().pageView(title, uri) }
```

- **ChatWindowOpenEvent**
  - Specific Chat page or screen (conversation) has been opened
  - Correct reporting of this event is required for "Welcome message" automation.
- **ConversionEvent**
  - The user was redirected from other media (link, etc…), made a purchase, read an article.
    Anything your company has internally defined as a conversion.  Generated by `chat.events().conversion(...)`.
- **CustomVisitorEvent**
  - Any event you may want to track.
- **PageViewEvent**
  - User has visited a page in the host application. Generated by `chat.events().pageView(...)`.
- **TimeSpentOnPageEvent**
  - User has left a page in the host application and time spent on the page.  Automatically
    generated by `chat.events().pageViewEnded(...)`.
- **ProactiveActionClickEvent**
  - Action regards to `ChatActionHandler.popupFlow`. Generated by `chat.events().proactiveActionClick(...)`.
- **ProactiveActionDisplayEvent**
  - Action regards to `ChatActionHandler.popupFlow`. Generated by `chat.events().proactiveActionDisplay(...)`.
- **ProactiveActionFailureEvent**
  - Action regards to `ChatActionHandler.popupFlow`. Generated by `chat.events().proactiveActionFailure(...)`.
- **ProactiveActionSuccessEvent**
  - Action regards to `ChatActionHandler.popupFlow`. Generated by `chat.events().proactiveActionSuccess(...)`.
- **TriggerEvent**
  - Trigger an automation event by ID

### Automatic analytics events

> [!NOTE]
> These events are automatically generated by the UI module (if you are using it) and should not be triggered manually.

#### PageView triggered events
Following events are automatically generated by the SDK when the application calls
`chat.events().pageView(...)` and `chat.events().pageViewEnded(...)`.
These should be triggered for every page view by the user in the application.
What is a "page" is defined by the application, it can be a activity, a fragment, or a specific compose view.

- **VisitEvent**
  - Defines the beginning of a visit, which is a sequence of
    PageView events.
  - Automatically generated by `chat.events().pageView(...)` when there is no
    current visit defined or when the last visit has been idle for more
    than 30 minutes.
- **TimeSpentOnPageEvent**
  - Reports time spent on the last page in seconds.
  - Automatically generated by `chat.events().pageViewEnded(...)`.

See analytics case study for information how to implement analytics events [here](chat-sdk-core/cs-analytics.md)

### Authorization events

Token refresh is managed entirely by the SDK — there is no event for it. The implicit OAuth flow asks
your `TokenDelegateListener` for a fresh token; the explicit flow refreshes with the stored
`refresh_token`. See the [OAuth flow migration guide](migration/MIGRATION_OAUTH_FLOWS.md).
