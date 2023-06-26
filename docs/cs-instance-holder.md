# Case Study: Instance Holder

This CS demonstrates how to correctly handle process transitions with your Chat instance. There is
obviously some leeway on how it can be implemented. If your application doesn't use any of these
components, feel free to use whatever tooling you're comfortable with. Just note that this example
follows "Google Suggested" practices.

## Libraries

```groovy
dependencies {
    implementation "androidx.lifecycle:lifecycle-common:2.5.1"
    implementation "androidx.startup:startup-runtime:1.1.1"
}
```

### `ChatInstanceProvider.kt`

You are strongly encouraged to bind chat instance to your activity's lifecycle. Chat is always
directly bound to a network socket that listens to messages. Failure to dispose the connection after
leaving the application, leaks the connection.
The connection can also be lost because of other reasons, like dropped network connection. Because of
this, you should implement ChatStateListener and provide its instance to the Chat, so your application will
be notified when network socket connection needs to be reestablished.

> Note that initialization may take about 2 seconds to fetch and resolve the instance. It's not
> immediately available after resume!

```kotlin
class ChatInstanceProvider private constructor(
    private val context: Context
) : DefaultLifecycleObserver {

    private var cancellable: Cancellable? = null
    private val chatStateListener by lazy { ChatStateListenerImpl() }
    var chat: Chat? = null
        private set
    
    val chatStateFlow : Flow<ChatState>
        get() = chatStateListener.state

    override fun onResume(owner: LifecycleOwner) {
        val config = SocketFactoryConfiguration(
            BuildConfig.CXOneRegion.let(CXOneEnvironment::valueOf).value,
            BuildConfig.CXOneBrandId,
            BuildConfig.CXOneChannelId
        )
        cancellable = ChatBuilder(context, config)
            .setDevelopmentMode(BuildConfig.DEBUG)
            .setChatOnStateListener(chatStateListener)
            .build {
                chat = it
            }
    }

    override fun onPause(owner: LifecycleOwner) {
        cancellable?.cancel()
        chat?.close()
    }

    companion object {

        private lateinit var provider: ChatInstanceProvider

        fun create(context: Context) = ChatInstanceProvider(context).also {
            provider = it
        }

        fun get() = provider

    }

}

class ChatStateListenerImpl : ChatStateListener {
    private val mutableState: MutableStateFlow<ChatState?> = MutableStateFlow(null)
    val state: Flow<ChatState>
        get() = mutableState.filterNotNull()

    override fun onConnected() {
        mutableState.value = CONNECTED
    }

    override fun onUnexpectedDisconnect() {
        mutableState.value = CONNECTION_LOST
    }
}

enum class ChatState {
        CONNECTED,
        CONNECTION_LOST,
}
```

### `ChatActivity.kt`

> Note that before launching this activity (which automatically initializes chat) you should ask for
> necessary permissions regarding EULA, GDPR or similar policies.

```kotlin
class ChatActivity : AppCompatActivity(R.layout.activity_chat) {
 
    private var chatStateSnackbar: Snackbar? = null
    private var reconnectJob: Cancellable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chatInstanceProvider = ChatInstanceProvider.get()
        lifecycle.addObserver(chatInstanceProvider)
        registerChatStateSnackbar()
    }
 
    /**
     * Simplified way to react to the Chat State changes.
     * An alternative approach would be to use several automatic
     * reconnection attempts paired with detection of network state.
     */
    private fun registerChatStateSnackbar() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatInstanceProvider.chatStateFlow.collect { state: ChatState ->
                 when (state) {
                    CONNECTED -> chatStateSnackbar = Snackbar.make(
                        Window.DecorView.RootView,
                        "Chat SDK connected",
                        Snackbar.LENGTH_SHORT
                    ).apply(Snackbar::show)
                    CONNECTION_LOST -> chatStateSnackbar = Snackbar.make(
                        Window.DecorView.RootView,
                        "Chat SDK connection lost",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Reconnect") {
                        reconnectJob?.cancel()
                        reconnectJob = chatInstanceProvider.chat.reconnect() 
                    }.apply(Snackbar::show)
                 }
                }
            }
        }
    }
 
    override fun onDestroy() {
        reconnectJob?.cancel()
        reconnectJob = null
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

    override fun create(context: Context) = ChatInstanceProvider.create(context)
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
