# Case Study: Instance Holder

This CS demonstrates how to correctly handle process transitions with your Chat instance. There is
obviously some leeway on how it can be implemented. If your application doesn't use any of these
components, feel free to use whatever tooling you're comfortable with. Just note that this example
follows "Google Suggested" practices.

## Libraries

```groovy
dependencies {
    implementation "androidx.lifecycle:lifecycle-common:2.6.2"
    implementation "androidx.startup:startup-runtime:1.1.1"
}
```

### `ChatInstanceProvider`

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

### `ChatInitializer.kt`

Let the chat initialize with a process, using the `androidx.startup` library.

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
