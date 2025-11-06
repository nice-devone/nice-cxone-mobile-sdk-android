# Case Study: UI Configuration

## Overview

The chat-sdk-ui module offers a basic option to configure internals of the Chat SDK UI.
This case study demonstrates how to set up and customize the UI configuration.

Configuration consists of two parts:

* compile-time configuration using the AndroidManifest and resource files
* runtime configuration using the Koin module

UI configuration is mostly independent from the core SDK configuration, except for custom field definitions.
If your application uses custom fields that are not part of the pre-chat survey, you must provide their definitions to the UI.
These will be passed to the Core SDK when needed.

### Configurable options:

- Translations (only default English strings are provided in the SDK)
- Push notifications (no defaults - can be disabled if unused)
    - Firebase (application has to provide its own google-services.json)
    - Notification icon (application has to provide its own icon for predefined drawable resource id)
- Logger (default is no logging - LoggerNoop)
- Customer custom field definitions (default is empty map - NoExtraCustomFields)
- Contact custom field definitions (default is empty map - NoExtraCustomFields)

## Prerequisites

- You have a working CXone Chat SDK integration in your Android application (it has to be initialized before the UI).
- Your application can use Android Compose with BOM 2025.10.00 or newer.

## Step 1: Add the SDK repository

   ```groovy
   // In your root build.gradle or settings.gradle
maven {
  url = uri("https://maven.pkg.github.com/nice-digital/cxone-mobile-sdk-android")
  credentials {
    username = project.findProperty("gpr.user") ?: System.getenv("GPR_USERNAME")
    password = project.findProperty("gpr.key") ?: System.getenv("GPR_TOKEN")
  }
}
   ```

## Step 2: Add Dependency

Add the chat-sdk-ui dependency to your build.gradle file:

```groovy
    implementation 'com.nice.cxone:chat-sdk-ui:<latest-version>'
```

## Step 3: Initialize the Chat SDK

Before using the Chat UI, initialize the CXone Chat SDK in your application with the `ChatInstanceProvider` class.
Specifically the `ChatInstanceProvider` has to be at least in the `Prepared` state, which means that the `ChatInstanceProvider` has to be
initialized with the `ChatSdkConfig`.
This can be done in the `onCreate` method of your `Application` class or in a dedicated `androidx.startup.Initializer` implementation.

## Step 4: Initialize the Chat UI Dependency Injection

### No existing Koin setup

If your application does not use Koin, add the Koin core dependency to your project:

```groovy
    implementation "io.insert-koin:koin-core:$koin_version"
```

Then you can initialize Koin with the Chat UI module in your application class like this:

```kotlin
   fun setupChatUi() {
    startKoin {
        UiModule.chatUiModule()
    }
}
```

as part of your application initialization code for the Chat UI.

### Existing Koin setup

Just add the `UiModule.chatUiModule()` to your Koin modules, which will provide the necessary dependencies for the Chat UI.

> [!NOTE]
> It is important that the module is set up before you start using any of the Chat UI `ChatActivity`.

## Step 4: Customize the UI Configuration

### Custom field definitions

If you are using custom fields (applies to both contact and customer) in the chat which are not user editable, you need to provide the
[UiCustomFieldsProvider](/chat-sdk-ui/src/main/java/com/nice/cxonechat/ui/api/UiCustomFieldsProvider.kt)
implementation(s) to the UI Koin module. These implementations will be used to append the custom fields to events sent to the server.

Example of providing custom field definitions to the Koin module:

```kotlin
class MyCustomerCustomFieldsProvider : UiCustomFieldsProvider {
    override val customFieldDefinitions: Map<String, String>
        get() = mapOf(
            "loyaltyLevel" to getLoyaltyLevel(),
            "referralCode" to getReferralCode(),
        )
}

class MyContactCustomFieldsProvider : UiCustomFieldsProvider {
    override val customFieldDefinitions: Map<String, String>
        get() = mapOf(
            "issueSource" to getIssueSource(),
        )
}

fun setupChatUi() {
    startKoin {
        UiModule.chatUiModule(
            customerCustomFieldsProvider = MyCustomerCustomFieldsProvider(),
            contactCustomFieldsProvider = MyContactCustomFieldsProvider(),
        )
    }
}
```

### Logging

If you want to enable logging in the Chat UI, you can provide an instance of `Logger` to the Koin UI module as parameter `logger`, default
instance is a No-op variant.

Detailed case study about logging is available [here](../chat-sdk-core/cs-logging.md) and can be also applied to the Chat UI.

## Step 5: Configure Push Notifications

### Firebase configuration

To enable push notifications, add the Firebase Cloud Messaging dependency to your project.
See the [Firebase Cloud Messaging documentation](https://firebase.google.com/docs/cloud-messaging/android/client) for more instructions
on how to set up FCM in your project.

The FCM dependency is included transitively with the chat-sdk-ui module, so you do not need to add it explicitly(, unless
you are also using it in your application).
What is required is to add the `google-services.json` file to your project, as described in the Firebase documentation.

For more focused information about this topic see the [cs-push-notifications.md](../chat-sdk-core/cs-push-notifications.md).

> [!NOTE]
> It is important that the application will also provide FCM token as a device token to the Chat SDK as outlined in
> the [cs-push-notifications.md](../chat-sdk-core/cs-push-notifications.md).

### Notification icon drawable

The UI module defines drawable resource id, for which the integrating application must provide the corresponding drawable resources.
The following resources are required:

* `ic_chat_push_service` - used for the push notification icon as a fallback when the notification does not define a custom icon.
  Application
  should provide a drawable resource which follows instructions
  from [NotificationCompat.Builder.setSmallIcon()](https://developer.android.com/reference/androidx/core/app/NotificationCompat.Builder#setSmallIcon(int)).

### Setup deeplink data scheme

To allow the Chat SDK UI to open the chat from push notification, you need to define an intent filter.
Add following entry to your `<application>` tag in you app's manifest:

```xml
    <!-- This setup is required only to register correct intent filter for the push notifications. -->
<activity android:name="com.nice.cxonechat.ui.screen.ChatActivity" android:exported="true">
    <!-- The intent filter is required by the deeplinks in the push notifications. -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <!-- The intent filter data scheme has to match the scheme set in the channel settings. -->
        <data android:scheme="com.nice.cxonechat.sample" />
    </intent-filter>
</activity>
```

> [!IMPORTANT]
> Update the `data` tag with **android:scheme** attribute to match the scheme configured in your CXone channel settings.

### Override Push Notification Channel IDs (optional)

By default the Chat SDK UI uses the string resource `default_notification_channel_id` as the notification channel id for push notifications.
If you want to override this value, you can provide your own string resource with the same name in your application resources.
Chat SDK UI is also setting this channel id as the default channel id for FCM notifications.
If this is undesired, you will have to set the manifest metadata `com.google.firebase.messaging.default_notification_channel_id` to
your desired channel id.

### Disable Push Notification Service (optional)

If your application does not use push notifications for chat (or you don't use Firebase), you can disable the push notification service.
Add following entry to your `<application>` tag in you app's manifest:

```xml

<service android:name="com.nice.cxonechat.ui.services.PushListenerService" android:enabled="false" />
```

## Step 6: Provide Translations (optional)

The Chat SDK UI comes with default English translations.
If your default application language is not English, override the default strings in your application's resources.
Also if you need to support additional languages, you will need to provide the translations in your application resources.

All strings are defined in the [res/values/strings.xml](/chat-sdk-ui/src/main/res/values/strings.xml) file.

## Step 7: Enable Backported PhotoPicker (optional)

If your application targets Android versions below 11 and you want to allow users to use the PhotoPicker functionality
so they can attach photos / recorded videos as attachments to chat messages, you will need to enable the backported PhotoPicker.
Add following entry to your `<application>` tag in you app's manifest:

```xml
    <!-- Trigger Google Play services to install the backported photo picker module. -->
<!--suppress AndroidDomInspection -->
<service android:name="com.google.android.gms.metadata.ModuleDependencies" android:enabled="false" android:exported="false"
    tools:ignore="MissingClass">
    <intent-filter>
        <action android:name="com.google.android.gms.metadata.MODULE_DEPENDENCIES" />
    </intent-filter>
    <meta-data android:name="photopicker_activity:0:required" android:value="" />
</service>
```

Original Android Developers [article](https://developer.android.com/training/data-storage/shared/photo-picker#device-availability) for this
topic.

## Additional configuration option

There are two additional configuration options which can be set by overriding resources in your application.

- `loading_close_button_delay_ms` - delay in milliseconds before the close button is shown on the loading screen.
  Default value is `20000` (20 seconds).