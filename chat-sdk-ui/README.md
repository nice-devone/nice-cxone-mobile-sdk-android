# Module chat-sdk-ui

The chat-sdk-ui module provides a default implementation of the UI for CXone Chat SDK, which allows easier integration
of the chat SDK into the intended target application. This module is designed to handle all channel configurations
(single, multi-thread and live chat) and provides a ready-to-use UI for the chat experience.

## Requirements:

Compile SDK version: 36
Target SDK version: 35
Min SDK version: 24

* Kotlin 2.2.10
* Jetpack Compose BOM 2025.10.00
* Koin 4.1.1
* Firebase Cloud Messaging 34.4.0 (unless the push notifications are not used)

This module requires that the integrating application uses Koin during its startup and it is also recommended
to provide an instance of `Logger` with `UiQualifier`.

### Runtime permissions:

For proper functioning of the Chat UI, the following runtime permissions are requested from the user, unless they are already granted:

* `android.permission.POST_NOTIFICATIONS` - Required for displaying of notifications regarding new messages in the chat, requested when the
  chat is started.
  The Chat UI will disable the notifications feature if the permission is not granted.
  For single-thread chat / live-chat, the notifications will be used only if the push notifications are configured for the channel, but the
  permission
  is always requested.
* `android.permission.RECORD_AUDIO` - Required for recording&sending of voice messages, requested when the user tries to send a voice
  message
  (via a microphone icon in the chat input). Chat UI will disable the voice message feature if the permission is not granted.
* `android.permission.WRITE_EXTERNAL_STORAGE` - This permission is used only for Android 10 and below, in order to store recorded audio
  messages,
  in a storage accessible by the user. On newer Android versions, the audio messages are stored in using the `MediaStore` API, which does
  not require this permission.

## Limitations:

* The UI module has limited support for landscape mode (this concern devices which have small height while
  in landscape mode). It is recommended to use the chat in portrait mode. This limitations will be addressed in future releases.
* Color customization is limited to only few selected colors. This feature will be fully supported in future releases.
* The UI module does not provide translated strings. It is recommended to provide your own translations for the strings used in the UI.

## Case studies

You can find focused case studies for following topics also described in [case studies.md](../docs/chat-sdk-ui/case-studies.md).

## Getting Started

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

```koin
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

For more focused information about this topic see the [cs-push-notifications.md].

#### Provide required resources

The UI module defines drawable resource id, for which the integrating application must provide the corresponding drawable resources.
The following resources are required:

* `ic_chat_push_service` - used for the push notification icon as a fallback when the notification does not define a custom icon.
  Application
  should provide a drawable resource which follows instructions
  from [NotificationCompat.Builder.setSmallIcon()](https://developer.android.com/reference/androidx/core/app/NotificationCompat.Builder#setSmallIcon(int)).

#### Configuration

#### Technical configuration

You can configure the notification channel for both local & push notifications by overriding the `default_notification_channel_id` string
resource.

#### String resources

`notification_channel_title` - The title of the notification channel used for local and push notifications.
The default value is "Chat notifications".

#### Not using push notifications/FCM?

If you are not using push notifications, you should disable the automatic startup of Firebase initialization in order to avoid unnecessary
overhead.

(see [Take Control of Your Firebase Init on Android]https://firebase.blog/posts/2017/03/take-control-of-your-firebase-init-on/) and disable
the `PushListenerService` in the merged manifest.

[cs-push-notifications.md]: ../docs/cs-push-notifications.md

## Features

### Single thread configuration

The UI modules provides near effortless support of the single thread chat configuration. This configuration allows users to engage in
one-on-one
conversations with agents.

### Multi-thread configuration

The UI module also supports multi-thread chat configuration, which allows users to engage in multiple conversations with agents
simultaneously.
Switch between the conversations via list, review the transcript of an archived conversation and receive notifications about new messages
in background conversations.

### Live chat configuration

The UI module supports live chat configuration, which allows users to engage in real-time conversations with agents, including UX for
queuing for an agent to become available and handling of the end of conversation.

### Voice messages

The UI module supports sending and receiving voice messages, which allows users to record and send audio messages during the conversation.
This feature enhances user experience by allowing more natural communication.

> ⚠️ Warning: The UI module is using AMR format for voice recording. It is necessary to specify "audio/amr" or "audio/\*" mimeType in the
> brand Settings to enable voice recording.

## Customization

The UI module supports

### Color customization

> ⚠️ Warning: Color customization is currently not recommended to ensure accessibility standards and optimal user experience. This feature
> will be fully available in future releases.

### Localization

The UI module does not provide translated strings. It is recommended to provide your own translations for the strings used in the UI, using
standard Android localization practices.
All localized strings used in the UI are defined in the `strings.xml` file of the module, which can be overridden in the integrating
application.
