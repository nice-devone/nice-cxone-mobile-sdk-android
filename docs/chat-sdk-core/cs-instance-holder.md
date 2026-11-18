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
    3. Implementing the `ChatInstanceProvider.Listener` interface to handle chat state changes.
    4. Using chat instance.

## Libraries

For this case study we will need following extra libraries, apart from the Chat SDK.

```groovy
dependencies {
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:<latest-version>" // provides lifecycleScope
    implementation "androidx.startup:startup-runtime:<latest-version>"
}
```
## Creating & binding the `ChatInstanceProvider`

Let's create initialize the `ChatInstanceProvider` with the application process,
using the `androidx.startup` library.

### `ChatInitializer.kt`
```kotlin
class ChatInitializer : Initializer<ChatInstanceProvider> {
    override fun create(context: Context) = ChatInstanceProvider.create(
        configuration = SocketFactoryConfiguration(
            environment = BuildConfig.CXoneRegion.let(CXoneEnvironment::valueOf).value,
            brandId = BuildConfig.CXoneBrandId, // Long
            channelId = BuildConfig.CXoneChannelId,
        )
    )

    override fun dependencies() = emptyList()
}
```

> [!NOTE]
> `create()` does not take a `Context`. The provider receives one later, when you call
> `prepare(context)` from the `Initial` state — see `onStart` and `onChatStateChanged` below.

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
this, you should register a `ChatInstanceProvider.Listener`, so your application will
be notified when the network socket connection needs to be reestablished.

`closeSuspending()` closes the web sockets and returns the state to `Prepared`; the next `connect()` transparently
rebuilds the `Chat` instance. Call it from a coroutine — the blocking `close()` variant is annotated `@WorkerThread` and
must not be called from a lifecycle callback on the main thread.

Close only a provider that has a `Chat` to close, and use `cancel()` for one that does not. `closeSuspending()` advances
to `Prepared` whether or not it found a `Chat`, so calling it while `Initial` or `Preparing` cancels the build in flight
and arrives at `Prepared` holding nothing: `connect()` then returns silently and `prepare()` is refused, stranding the
provider for the life of the process. `cancel()` returns `Preparing` to `Initial`, so the next `onStart()` can prepare
again. The window is not narrow — preparation takes a moment (see the note below), and a rotation inside it delivers
`onStop()`.

`SdkNotSupported` needs the same handling for a different reason. It is terminal and it also holds no `Chat`, so closing
it strands no session — it discards the only record that the backend refused this SDK version, replacing it with a
`Prepared` the provider can never act on. `cancel()` leaves it standing, which is what you want: nothing recovers it, so
the state has to survive until the user updates the app.

Wrap the call in `NonCancellable`. `lifecycleScope` is cancelled at `ON_DESTROY`, which can arrive while the close is
still draining pending storage and cookie writes. A close cut short there leaves the provider's state where it was
instead of back at `Prepared`, holding a `Chat` the provider will not rebuild — so no later `connect()` recovers the
session. The close must therefore outlive the scope that started it.

Close from `onStop()`, as the UI module's own `ChatActivity` does. `onPause()` also fires for transient interruptions — a
permission dialog, split-screen focus loss, another activity opened for a result — and a close that cannot be abandoned
would end a session the user is about to come back to. `onDestroy()` is too late: the scope is cancelled by then, so the
coroutine never runs and `NonCancellable` protects nothing.

> [!NOTE]
> `ChatInstanceProvider.Listener` reports the provider's `ChatState` (the recommended way to
> manage the chat lifecycle across process transitions). If you work with a `Chat` instance directly
> — without the provider — observe its coroutine-native `Chat.stateFlow: SharedFlow<ChatStateEvent>`
> instead. See the [Coroutine API migration guide](../migration/MIGRATION_COROUTINE_API.md).

> Note that initialization may take about 2 seconds to fetch and resolve the instance. It's not
> immediately available once the activity starts!

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

    override fun onStop() {
        super.onStop()

        val provider = chatInstanceProvider ?: return
        when (provider.chatState) {
            // Nothing to close: no Chat has been built yet, or the build was refused.
            Initial, Preparing, SdkNotSupported -> provider.cancel()
            else -> lifecycleScope.launch {
                withContext(NonCancellable) { provider.closeSuspending() }
            }
        }
    }
}
```

## Implementing `ChatInstanceProvider.Listener`
We can now expand our previous implementation of `ChatActivity` to include the `ChatInstanceProvider.Listener` interface.

```kotlin
class ChatActivity : AppCompatActivity(R.layout.activity_chat), ChatInstanceProvider.Listener {

    private var chatInstanceProvider: ChatInstanceProvider? = null
    private var chatStateSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatInstanceProvider = ChatInstanceProvider.get()
    }

    override fun onStart() {
        super.onStart()

        val provider = chatInstanceProvider ?: return
        provider.addListener(this)
        // addListener does not replay the current state, so the state on arrival has to be acted on
        // here. On the first start of a process that is still Initial, nothing else will call prepare.
        when (provider.chatState) {
            Initial -> provider.prepare(this)
            Prepared, ConnectionLost -> provider.connect()
            // Terminal, and no further callback announces it, so restore the UI here after a rotation.
            SdkNotSupported -> showSdkNotSupported()
            else -> Unit // a prepare or connect is already in flight; the listener reports the result
        }
    }

    override fun onStop() {
        super.onStop()

        val provider = chatInstanceProvider ?: return
        provider.removeListener(this)
        when (provider.chatState) {
            // Cancelling returns Preparing to Initial, so the onStart above can prepare again, and
            // leaves SdkNotSupported standing so that onStart still sees the refusal.
            Initial, Preparing, SdkNotSupported -> provider.cancel()
            else -> lifecycleScope.launch {
                withContext(NonCancellable) { provider.closeSuspending() }
            }
        }
    }

    override fun onChatStateChanged(chatState: ChatState) {
        when (chatState) {
            Initial -> {
                // ChatInstanceProvider wasn't initialized yet
                chatInstanceProvider?.prepare(this@ChatActivity)
            }
            Preparing -> {
                // ChatInstanceProvider is being configured
                // Show loading spinner or similar UI
            }
            Prepared -> {
                chatInstanceProvider?.connect()
            }
            Connecting -> {
                // ChatInstanceProvider is connecting
                // Show loading spinner or similar UI
            }
            Connected -> chatStateSnackbar = Snackbar.make(
                window.decorView.rootView,
                "Chat SDK connected",
                Snackbar.LENGTH_SHORT
            ).apply(Snackbar::show)
            Ready -> chatStateSnackbar = Snackbar.make(
                window.decorView.rootView,
                "Chat SDK is ready",
                Snackbar.LENGTH_SHORT
            ).apply(Snackbar::show)
            Offline -> { // This state can happen only for the LiveChat mode of the channel.
                // Show UI informing user that the Chat is not available, because the service is offline
            }
            ConnectionLost -> chatStateSnackbar = Snackbar.make(
                window.decorView.rootView,
                "Chat SDK connection lost",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Reconnect") {
                chatInstanceProvider?.connect()
            }.apply(Snackbar::show)
            SdkNotSupported -> showSdkNotSupported()
        }
    }

    // Neither prepare() nor connect() recovers this state, so offer an update rather than a retry.
    private fun showSdkNotSupported() {
        chatStateSnackbar = Snackbar.make(
            window.decorView.rootView,
            "This app version is no longer supported, please update",
            Snackbar.LENGTH_INDEFINITE
        ).apply(Snackbar::show)
    }

    companion object {
        private const val TAG = "ChatActivity"
    }
}
```

> [!NOTE]
> Note that interactions with chat instance (e.g. sending of a message)
> while it is not connected can be lost if the connection is not reestablished.

## Next Steps
After obtaining your chat instance, you should perform the following steps:
    1. Preparing and Connecting
        - This is already outlined in the [Implementing `ChatInstanceProvider.Listener`](#implementing-chatinstanceproviderlistener) section.
        - **Prepare** - This method should be called early, when the app is started. It allows the SDK to fetch configuration and allows usage of the SDK Analytics.
        - **Connect** - This method should be called when the user is ready to start chatting.  It establishes an active connection to the chat server.
        - Waiting for **Ready** state - It is recommended to wait for the Ready state before starting to interact with the chat instance.  This allows the SDK to perform necessary calls to the server and prepare the chat instance for the interaction.
    2. Creating a Thread
        - See either [Case Study Single Thread](cs-single-thread.md), [Case Study Multiple Threads](cs-multi-thread.md) or [Case Study Live Chat](cs-live-chat.md) since implementation of this step greatly depends on your channel configuration.
    3. Handling Messages

- This part is outlined in the [implementation.md](../implementation.md) document.
    4. Push notification support
        - While push notifications are not mandatory, it is a recommended that your application takes advantage of them. For more information, refer to the [Case Study Push Notifications](cs-push-notifications.md) document.

For more detailed information, refer to the [implementation.md](../implementation.md) document which explains the differences between
`ChatInstanceProvider` and `ChatBuilder`.
