# Case Study: Instance Holder

This CS demonstrates how to correctly handle process transitions with your Chat instance.
The first step in using the SDK is to get a chat instance, and the examples below walk you through that.
The `ChatInstanceProvider` operates through a singleton pattern, so the implementor doesn’t have to worry
about managing their chat instances. If your application doesn't use any of these
components, feel free to use whatever tooling you're comfortable with. Just note that this example
follows "Google Suggested" practices.

## Step-by-step:
The usage of `ChatInstanceProvider` consists of following steps:
    1. Creating instance of `ChatInstanceProvider` & binding the `ChatInstanceProvider` creation to the application lifecycle.
    2. Obtaining the chat instance & binding the chat instance to the activity lifecycle.
    3. Implementing the `ChatStateListener` interface to handle chat state changes.
    4. Using chat instance.

## Libraries

For this case study we will need following extra libraries, apart from the Chat SDK.

```groovy
dependencies {
    implementation "androidx.lifecycle:lifecycle-common:2.6.2"
    implementation "androidx.startup:startup-runtime:1.1.1"
}
```
## Creating & binding the `ChatInstanceProvider`

Let's create initialize the `ChatInstanceProvider` with the application process,
using the `androidx.startup` library.

### `ChatInitializer.kt`
```kotlin
class ChatInitializer : Initializer<ChatInstanceProvider> {
    override fun create(context: Context) = ChatInstanceProvider.create(
        context,
        BuildConfig.CXOneRegion.let(CXOneEnvironment::valueOf).value,
        BuildConfig.CXOneBrandId,
        BuildConfig.CXOneChannelId
    )

    override fun dependencies() = emptyList()
}
```

### `AndroidManifest.xml`

```xml

<manifest>
    <application>
        <provider android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup" android:exported="false"
            tools:node="merge">
            <meta-data android:name=".ChatInitializer" android:value="androidx.startup" />
        </provider>
    </application>
</manifest>
```

### Anywhere else in your code

```kotlin
fun interactWithChat() {
    val chat = ChatInstanceProvider.get().chat ?: return
}
```

## Obtain Chat instance and bind it to the activity lifecycle
You are strongly encouraged to bind chat instance to your activity's lifecycle. Chat is always
directly bound to a network socket that listens to messages. Failure to dispose the connection after
leaving the activity, leaks the connection.

The connection can also be lost because of other reasons, like dropped network connection. Because of
this, you should implement ChatStateListener and provide its instance to the Chat, so your application will
be notified when network socket connection needs to be reestablished.

> Note that initialization may take about 2 seconds to fetch and resolve the instance. It's not
> immediately available after resume!

The CXone Chat SDK provides `com.nice.cxonechat.ChatInstanceProvider` to help with this process.

### `ChatActivity.kt`

> Note that before launching this activity (which automatically initializes chat) you should ask for
> necessary permissions regarding EULA, GDPR or similar policies.

```kotlin
class ChatActivity : AppCompatActivity(R.layout.activity_chat) {

    private var chatInstanceProvider: ChatInstanceProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatInstanceProvider = ChatInstanceProvider.get()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        chatInstanceProvider.stop()
    }
}
```

## Implementing `ChatStateListener`
We can now expand our previous implementation of `ChatActivity` to include the `ChatStateListener` interface.

```kotlin
class ChatActivity : AppCompatActivity(R.layout.activity_chat), ChatInstanceProvider.Listener {
 
    private var chatInstanceProvider: ChatInstanceProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatInstanceProvider = ChatInstanceProvider.get()
    }
 
    override fun onResume() {
        super.onResume()

        chatInstanceProvider.addListener(this)
        chatInstanceProvider.connect()
    }

    override fun onPause() {
        super.onPause()

        chatInstanceProvider.stop()
        chatInstanceProvider.removeListener(this)
    }

    override fun onChatStateChanged(chatState: ChatState) {
        when (state) {
            CONNECTED -> chatStateSnackbar = Snackbar.make(
                Window.DecorView.RootView,
                "Chat SDK connected",
                Snackbar.LENGTH_SHORT
            ).apply(Snackbar::show)
            READY -> chatStateSnackbar = Snackbar.make(
                Window.DecorView.RootView,
                "Chat SDK is ready",
                Snackbar.LENGTH_SHORT
            ).apply(Snackbar::show)
            OFFLINE -> { // This state can happen only for the LiveChat mode of the channel.
                // Show UI informing user that the Chat is not available, because the service is offline
            }
            CONNECTION_LOST -> chatStateSnackbar = Snackbar.make(
                Window.DecorView.RootView,
                "Chat SDK connection lost",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Reconnect") {
                reconnectJob?.cancel()
                reconnectJob = chatInstanceProvider.chat.reconnect()
            }.apply(Snackbar::show)
            else -> Log.v(TAG, "ChatState: $state")
        }
    }
    
    companion object {
        private const val TAG = "ChatActivity"
    }
}
```

> ℹ️
> Note that interactions with chat instance (e.g. sending of a message)
> while it is not connected can be lost if the connection is not reestablished.

## Next Steps
After obtaining your chat instance, you should perform the following steps:
    1. Preparing and Connecting
        - This is already outlined in the [Implementing `ChatStateListener`](#implementing-chatstatelistener) section.
        - **Prepare** - This method should be called early, when the app is started. It allows the SDK to fetch configuration and allows usage of the SDK Analytics.
        - **Connect** - This method should be called when the user is ready to start chatting.  It establishes an active connection to the chat server.
        - Waiting for **Ready** state - It is recommended to wait for the Ready state before starting to interact with the chat instance.  This allows the SDK to perform necessary calls to the server and prepare the chat instance for the interaction.
    2. Creating a Thread
        - See either [Case Study Single Thread](cs-single-thread.md), [Case Study Multiple Threads](cs-multi-thread.md) or [Case Study Live Chat](cs-live-chat.md) since implementation of this step greatly depends on your channel configuration.
    3. Handling Messages
        - This part is outlined in the [implementation.md](implementation.md) document.
    4. Push notification support
        - While push notifications are not mandatory, it is a recommended that your application takes advantage of them. For more information, refer to the [Case Study Push Notifications](cs-push-notifications.md) document.

For more detailed information, refer to the [implementation.md](implementation.md) document which explains the differences between `ChatInstanceProvider` and `ChatBuilder`.
