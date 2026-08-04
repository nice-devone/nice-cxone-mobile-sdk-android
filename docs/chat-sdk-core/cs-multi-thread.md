# Case Study: Multi Thread

> [!IMPORTANT]
> Usage of this case study is intended for scenarios where you are not using provided UI module artifact.
> If you are using the UI module, then this use-case is already covered by the UI module and the case study
> is only provided for educational purposes.

This example is the virtually most complete CS, it shows - simplified, of course - usage of this
library in a real-world use-case.

### `MyNavigator.kt`

Navigator's implementation depends on which framework or in-house solution you use. We just use an
interface to demonstrate the basic usage.

```kotlin
interface MyNavigator {
    fun toDetail(thread: ChatThread)
}
```

### `ChatAllConversationsViewModel.kt`

We're demonstrating (nearly) all possible features that conversations screen can do. Notice
especially that all handlers are pulled out and saved to instance properties. Do not unnecessarily
recreate the handlers!

```kotlin
class ChatAllConversationsViewModel(
    private val navigator: MyNavigator,
) : ViewModel() {

    private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
    private val handlerThreads = chat.threads()

    val threads = handlerThreads.threadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun onClickThread(thread: ChatThread) {
        navigator.toDetail(thread)
    }

}
```

### `ChatConversationDetailViewModel.kt`

This ViewModel also demonstrates how to send messages. The sample uses `Flow`/coroutines rather
than listener callbacks, so lifecycle is tied to `viewModelScope` — there is no manual listener
bookkeeping to do.

> Keep long-lived handlers on the ViewModel and avoid recreating them unnecessarily. Collect
> `Flow`s with `viewModelScope` so collection is cancelled automatically when the ViewModel is
> cleared, and close any handler that implements `AutoCloseable` from `onCleared()` (shown below
> with `handlerAction.close()`).

Extending `AndroidViewModel` gives us a safe `Application` context for building
`ContentDescriptor`s without leaking an `Activity`/`Fragment`.

```kotlin
class ChatConversationDetailViewModel(
    thread: ChatThread,
    application: Application,
) : AndroidViewModel(application) {

    private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
    private val handlerThreads = chat.threads()
    private val handlerThread = handlerThreads.thread(thread)
    private val handlerMessage = handlerThread.messages()
    private val handlerAction = handlerThread.actions()
    private val handlerEvents = handlerThread.events()

    val thread: StateFlow<ChatThread> = handlerThread.threadFlow
        .onEach { if (isInForeground) handlerEvents.markThreadRead() }
        .onStart { handlerThread.refresh() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), handlerThread.get())

    val popups: SharedFlow<Popup> = handlerAction.popupFlow
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)

    val messagesSent = mutableSetOf<String>()
    var isInForeground = true

    fun setName(name: String) {
        handlerThread.setName(name)
    }

    fun onClickArchive() {
        viewModelScope.launch {
            handlerThread.archive()
        }
    }

    fun onEndReached() {
        handlerMessage.loadMore()
    }

    fun send(text: String) {
        viewModelScope.launch {
            val id = handlerMessage.send(OutboundMessage(text))
            messagesSent.add(id)
        }
    }

    fun send(text: String, postback: String) {
        viewModelScope.launch {
            val id = handlerMessage.send(OutboundMessage(text, postback))
            messagesSent.add(id)
        }
    }

    fun send(file: File) {
        viewModelScope.launch {
            val descriptor = ContentDescriptor(
                content = Uri.fromFile(file),
                context = getApplication(),
                mimeType = "application/pdf",
                fileName = file.name,
            )
            val id = handlerMessage.send(OutboundMessage(listOf(descriptor)))
            messagesSent.add(id)
        }
    }

    override fun onCleared() {
        handlerAction.close()
    }

}
```
