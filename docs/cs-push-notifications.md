# Case study: Push notifications

Integration of automatic push notifications, which are delivered in case that user has received
a message in a thread while the CXone Chat SDK is not active, is an optional step.

## Pre-requisite
CXone Chat SDK for Android is leveraging [Firebase Cloud Messaging] to deliver push notifications
to the user's device.
If the application wishes to receive these push notifications, it has to complete the "Enable Push Notifications" part of
CXone Chat SDK [Set Up]

> ⚠️ Important note: The Amazon guide is partially out of date. It fails to mention that the "Server key"
> mentioned in the guide is in subsection of "Cloud Messaging API (Legacy)" and this API has to be
> manually enabled for new Firebase projects.

## Implementation
As outlined in the "Enable Push Notifications" subchapter, the CXone Chat SDK needs a Firebase token
for push notification functionality.
It can be passed to the SDK continuously, whenever it is updated (preferred variant), or if necessary,
only when chat functionality is activated.

### `PushListenerService`
As outlined in [Firebase guide - Set up the SDK] application needs a service that will extend `FirebaseMessagingService`.
In this case study we will call it `PushListenerService`.

For successful integration of CXone Chat SDK driven push notifications, it has to implement the following functionalities.
1. Supply SDK with token updates
2. Display notifications received from the backend

Example implementation:
```kotlin
package com.example.app

import com.example.app.ChatSdkProvider
import com.example.app.R
import com.google.firebase.messaging.FirebaseMessagingService

internal class PushListenerService : FirebaseMessagingService() {
    
    private val chatSdkProvider = ChatSdkProvider // We assume that there is a singleton instance of class which can provide an instance of Chat SDK 

    // 1. Supplying SDK with token updates
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val chat = chatSdkProvider?.chatInstance
        if (chat == null) {
            // There is no existing instance of Chat SDK, no need to update.
            Log.v(TAG, "No chat instance present, token not passed")
            return
        }
        chat.setDeviceToken(token) // Update the instance with current token
        Log.d(TAG, "Registering push notifications token: $token")
    }
    
    // 2. Display the notification 
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        sendNotification(
            remoteMessage.data["pinpoint.notification.title"],
            remoteMessage.data["pinpoint.notification.body"])
    }
    
    private fun sendNotification(title: String?, messageBody: String?) {
        val channelId = getString(R.string.default_notification_channel_id) // Id for chat notifications
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setSmallIcon(R.mipmap.ic_launcher) // Application provided icon for chat notifications
            .setAutoCancel(false) // This option is up to the application, it is included for completeness
            .setSound(defaultSoundUri) // This option is up to the application, it is included for completeness
            .setPriority(2)  // This option is up to the application, it is included for completeness
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Since android Oreo notification channel is needed.
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_title),  // Application should provide title serving as a description for chat notification channel
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = notificationBuilder.build()
        notificationManager.notify(CHAT_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHAT_NOTIFICATION_ID = 0
    }
}
```

### Providing token to Chat instance during initialization
Integrating application **has to** provide Chat SDK with the current value of the Firebase
messaging token, after SDK is initialized in order for proper push message support.

```kotlin
    // context and config are left out for briefness
    val builder = ChatBuilder(context = context, config = socketFactoryConfiguration)
    builder.build { chat ->
        val firebaseToken = Firebase.messaging.token
        firabaseToken.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                chat.setDeviceToken(task.result)
            } else {
                chat.setDeviceToken(null)
            }
        }
        // TODO return chat, resume coroutine or set chat instance to class property
    }

```

## Notes:
It is possible to unregister the device from receiving CXone Chat SDK push messages/notifications
by passing `null` to the `setDeviceToken()` method.

[Firebase Cloud Messaging]: https://firebase.google.com/docs/cloud-messaging
[Set up a Firebase Cloud Messaging client app on Android]: https://firebase.google.com/docs/cloud-messaging/android/client
[Set Up]: https://help.nice-incontact.com/content/acd/digital/mobilesdk/setupadvancedfeatures.htm?tocpath=Digital%20Experience%7CDigital%20Experience%20%7CDigital%20Channels%7CChat%20Channels%7CCXone%20Mobile%20SDK%7C_____1#PushNotifications
[Firebase guide - Set up the SDK]: https://firebase.google.com/docs/cloud-messaging/android/client#set_up_the_sdk
