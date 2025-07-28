# Integrator's Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Proguard / R8](#proguard--r8)
3. [Setting Up](#setting-up)
4. [Startup](#startup)
5. [Configuration](#configuration)
6. [Push Notification Tokens](#push-notification-tokens)
7. [Threads](#threads)
8. [Thread](#thread)
9. [Message States](#message-states)
10. [Manual Username Update](#manual-username-update)
11. [Custom Fields](#custom-fields)
12. [Global Events](#global-events)

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
For more information about logging, see the [Logging case study](cs-logging.md).

## Startup

First you need to obtain a `Chat` instance. You can achieve that through our `ChatBuilder` or you can use `ChatInstanceProvider` which is covered [here](cs-instance-holder.md).
We recommend the use of `ChatInstanceProvider` since it provides state tracking for the chat instance, but to cover both cases we will show you how to use the `ChatBuilder` in the following example.

```kotlin
  val config = SocketFactoryConfiguration(
  CXoneEnvironment.YourRegion,
    yourBrandId,
    yourChannelId
  )
  val myChatStateListener = object : ChatStateListener() {
    override fun onReady() {
      // TODO - Chat instance is ready for usage by the consumer, use Chat instance for chat
    }
  }
  val cancellable = ChatBuilder(context, config)
    .setDevelopmentMode(BuildConfig.DEBUG) // Development mode shouldn't be enabled in production 
    .setAuthorization(yourAuthorization) // (1)
    .setUserName("firstName", "lastName") // (2)
    .setChatStateListener(myChatStateListener)
    .build { chat ->
      // TODO save chat instance
    }
```

- (1) Authorization
  - Depending on whether you use oAuth, you might be required to use Authorization.
  - If you don't use oAuth, then don't call `.setAuthorization` method.
- (2) Username
  - Usage depends on the fact if you are using oAuth.
    The OAuth users typically won't need to set username,
    since user details will be retrieved from oAuth backend.
  - If you are using a manual username setup, please follow instructions on updating the username
    (if it can change in your application).

> [!NOTE]
> The `build` method asynchronously creates an instance of Chat which is ready for analytics usage, for chat use-case it
> needs to be connected.
> Chat will start the asynchronous connection attempt once the `Chat.connect()` method is called.
>
> In case of connection error, the application will be notified, and it will have to schedule a connection retry attempt.
> Application can cancel both the build and connection process according to its requirements via `Cancellable` instance
> returned from the `build` and `connect` method calls.

> [!IMPORTANT]
> In case the startup was not successful for you and `build` method did not return the `Chat`
> instance, be sure to check your configuration as server might have rejected the request. Read the
> documentation for `build` method for more clarity on the subject.

---

Now you can use the CXone Chat SDK for sending of analytics events (which are used for automation).
If you also need to activate the chat, you will need to connect it to backend and let Chat perform basic preparation of
the instance.

1. First you need to inform `Chat` instance that it should connect to backend by calling `chat.connect`.
2. Once it is connected the `Chat` instance will call the supplied `ChatStateListener.onConnected` callback.
At this moment the `Chat` has established socket connection with backend, and it will start final background
tasks to fully prepare instance for usage (retrieval of the thread in single-thread mode or thread list in the
multi-thread mode). 
3. `Chat` will inform that is fully ready by calling the `ChatStateListener.onReady` callback.

Great! Now you're ready to use the CXone Chat SDK.

> [!IMPORTANT]
> It is recommended to wait for the `ChatStateListener.onReady` callback before using the chat instance.

> [!IMPORTANT]
> Chat instance maintains open socket connection to backend, until `chat.close()` is called, or the
> application process is terminated.
> It is the responsibility of the integrating application,
> to close the chat when the user leaves the part of application dedicated to chat
> (usually dedicated Activity) as it is outlined in terms of use for the SDK.

## Configuration
You can use the chat `configuration` property to support different UI/UX flows in your application and also to
preemptively verify that your assumptions about active chat configuration are correct.

```kotlin
  val chat = MyChatInstanceProvider.chat ?: return
  val chatConfiguration = chat.configuration
```

## Integrate UI module

> [!NOTE]
> This is excerpt from the [chat-sdk-ui/README.md](../chat-sdk-ui/README.md) file, for the most up-to-date information please refer to the
> original file.

To use the chat-sdk-ui module, you need to add it as a dependency in your project. You can do this by adding the following line to your
`build.gradle` file:

```groovy
    implementation("com.nice.cxone:chat-sdk-ui:${chatSdkVersion}")
```

### Before you start Chat UI

#### Initializing the Chat SDK

Before you start using the Chat UI, you need to initialize the CXone Chat SDK in your application using the `ChatInstanceProvider` class.
Specifically the `ChatInstanceProvider` has to be at least in the `Prepared` state, which means that the `ChatInstanceProvider` has to be
initialized with the `ChatSdkConfig`.
This can be done in the `onCreate` method of your `Application` class or in a dedicated `androidx.startup.Initializer` implementation.

#### Initialize the Chat UI Dependency Injection

If you are not using Koin in your application you can just call

```kotlin
   startKoin {
      UiModule.chatUiModule()
   }
```

Otherwise add the `UiModule.chatUiModule()` to your Koin modules, which will provide the necessary dependencies for the Chat UI.

##### Custom fields

If you are using custom fields (applies to both contact and customer) in the chat which are not user editable, you need to provide the
`UiCustomFieldsProvider`
implementation(s) to the UI Koin module. These implementations will be used to append the custom fields to events sent to the server.

##### Logging

If you want to enable logging in the Chat UI, you can provide an instance of `Logger` to the Koin UI module as parameter `logger`, default
instance is a No-op variant.

### Starting the Chat UI

To start the Chat UI call `ChatActivity.startChat(from: Activity)` method.

### Push notifications

For the push notifications to work, you need to add the Firebase Cloud Messaging dependency to your project.
See the [Firebase Cloud Messaging documentation](https://firebase.google.com/docs/cloud-messaging/android/client) for more instructions
on how to set up FCM in your project.

For more focused information about this topic see the [cs-push-notifications.md](cs-push-notifications.md).

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
Custom field definitions can be accessed by using chat configuration instance.
Definition provides a `label` field which can hold human-readable label (or reference to string resource) and `fieldId`
which is used for adding of custom field values.

> [!NOTE]
> Field definitions are provided only for custom fields, which are used for the pre-chat survey.
> It is possible to use other custom fields, even without definition supplied in the configuration.
> Backend can reject undefined (meaning the definition is not present in channel configuration on backend) custom fields.

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
  val chat = MyChatInstanceProvide.chat ?: return
  val customerCustomFields = chat.fields
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
    val chat = MyChatInstanceProvide.chat ?: return
    val customerCustomFields = chat.configuration.customerCustomFields.lookup("referral") as? FieldDefinition.Text ?: return
    val customFields = mapOf(customerCustomFields.fieldId to "referral_unknown")
    chat.customFields().add(customFields)
  }
  ```

> Supplying the same key with different value, will overwrite the existing custom field.

> Customer custom fields can be used for creation of an automatic welcome message or other automatic
> events.
> If you're unsure if these values will be required, you should consult your CXone representative
> or local managers depending on your company structure.

## Global Events

### Analytics events

- **ChatWindowOpenEvent**
  - Specific Chat page or screen (conversation) has been opened
  - Correct reporting of this event is required for "Welcome message" automation.
- **ConversionEvent**
  - The user was redirected from other media (link, etc…), made a purchase, read an article.
    Anything your company has internally defined as a conversion.  Generated by `ChatEventHandlerActions.conversion`
- **CustomVisitorEvent**
  - Any event you may want to track.
- **PageViewEvent**
  - User has visited a page in the host application. Generated by `ChatEventHandlerActions.pageView`.
- **TimeSpentOnPageEvent**
  - User has left a page in the host application and time spent on the page.  Automatically
    generated by `ChatEventHandlerActions.pageViewEnded`.
- **ProactiveActionClickEvent**
  - Action regards to `ChatActionHandler::onPopup`. Generated by `ChatEventHandlerActions.proactiveActionClick`.
- **ProactiveActionDisplayEvent**
  - Action regards to `ChatActionHandler::onPopup`. Generated by `ChatEventHandlerActions.proactiveActionDisplay`.
- **ProactiveActionFailureEvent**
  - Action regards to `ChatActionHandler::onPopup`. Generated by `ChatEventHandlerActions.proactiveActionFailure`.
- **ProactiveActionSuccessEvent**
  - Action regards to `ChatActionHandler::onPopup`. Generated by `ChatEventHandlerActions.proactiveActionSuccess`.
- **TriggerEvent**
  - Trigger an automation event by ID

### Automatic analytics events

> [!NOTE]
> These events are automatically generated by the UI module (if you are using it) and should not be triggered manually.

#### PageView triggered events
Following events are automatically generated by the SDK when the application triggers
` ChatEventHandler.pageView` and ` ChatEventHandler.pageViewEnded` methods.
These should be triggered for every page view by the user in the application.
What is a "page" is defined by the application, it can be a activity, a fragment, or a specific compose view.

- **VisitEvent**
  - Defines the beginning of a visit, which is a sequence of
    PageView events.
  - Automatically generated by ` ChatEventHandlerActions.pageView` method when there is no
    current visit defined or when the last visit has been idle for more
    than 30 minutes.
- **TimeSpentOnPageEvent**
  - Reports time spent on the last page in seconds.
  - Automatically generated by `ChatEventHandlerActions.pageViewEnded`.

See analytics case study for information how to implement analytics events [here](cs-analytics.md)

### Authorization events

- **RefreshToken**
  - Event notifying backend that authorization token has to be refreshed.
