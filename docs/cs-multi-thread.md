# Case Study: Multi Thread

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

    var threads = emptyList<ChatThread>()

    private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
    private val handlerThreads = chat.threads()
    private val handlerFields = chat.fields()
    private val handlerEvents = chat.events()
    private val cancellable = handlerThreads.threads {
        threads = it
        // notify ui
    }.also { handlerThreads.refresh() }

    init {
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            chat.setDeviceToken(it)
        }
        val configuration = chat.configuration
        val deviceInfo = configuration.customerCustomFields
            .filterIsInstance(FieldDefinition.Text::class.java)
            .mapNotNull {
                when (val id: String = it.fieldId) {
                    "device-oem" -> id to Build.MANUFACTURER
                    "device-model" -> id to Build.MODEL
                    "device-os" -> id to "Android"
                    "device-version" -> id to Build.VERSION.SDK_INT.toString()
                    else -> null
                }
            }.toMap()
        handlerFields.add(deviceInfo)
    }

    fun onConversion(type: String, value: Number) {
        handlerEvents.trigger(ConversionEvent(type, value))
    }

    fun onOpened() {
        handlerEvents.trigger(VisitEvent())
    }

    fun onClickThread(thread: ChatThread) {
        navigator.toDetail(thread)
    }

    override fun onCleared() {
        cancellable.cancel()
    }

}
```

### `ChatAllConversationViewModel.kt`

This ViewModel also demonstrates how to detect which messages are sent. Note that you might want to
use WeakReferences to your ViewModel to avoid immediate memory leaks.

> All listeners are cleared after calling `Chat::close`, thereafter are ViewModels withheld from GC
> cleared. This only regards the cases where you'd accidentally use hard references on dead objects.

```kotlin
class ChatAllConversationViewModel(
    thread: ChatThread,
) : ViewModel() {

    private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
    private val handlerThreads = chat.threads()
    private val handlerThread = handlerThreads.thread(thread)
    private val handlerMessage = handlerThread.messages()
    private val handlerAction = handlerThread.actions()
    private val handlerEvents = handlerThread.events()
    private val handlerFields = handlerThread.fields()
    private val cancellable = handlerThread.get {
        if (isInForeground) handlerEvents.trigger(MarkThreadReadEvent)
        // notify ui that ::thread has changed
    }.also { handlerThread.refresh() }
    private val sentListener = MessageListener(WeakReference(this))

    var isInForeground = true
    val thread get() = handlerThread.get()
    val messagesSent = mutableSetOf<UUID>()

    init {
        val customFields = chat.configuration.contactCustomFields
            .filterIsInstance(FieldDefinition.Text::class.java)
            .mapNotNull {
                when (val id: String = it.fieldId) {
                    "last-contact" -> id to "2005-08-25T11:45:89.187Z"
                    "flag-disrespectful" -> id to "true"
                    else -> null
                }
            }.toMap()
        handlerFields.add(customFields)
    }

    fun setName(name: String) {
        handlerThread.setName(name)
    }

    fun setOnPopupListener(listener: ChatActionHandler.OnPopupActionListener) {
        handlerAction.onPopup(listener)
    }

    fun onTypingStart() {
        handlerEvents.trigger(TypingStartEvent)
    }

    fun onTypingStop() {
        handlerEvents.trigger(TypingEndEvent)
    }

    fun onClickArchive() {
        handlerEvents.trigger(ArchiveThreadEvent)
    }

    fun onEndReached() {
        handlerMessage.loadMore()
    }

    fun send(text: String) {
        handlerMessage.send(OutboundMessage(text), sentListener)
    }
    
    fun send(text: String, postback: String) {
        handlerMessage.send(OutboundMessage(text, postback), sentListener)
    }

    fun send(file: File) {
        val descriptor =
            ContentDescriptor(uri = Uri.fromFile(file), context = appli, mimeType = "application/pdf", fileName = file.name)
        handlerMessage.send(OutboundMessage(listOf(descriptor)), listener = sentListener)
    }

    override fun onCleared() {
        cancellable.cancel()
        handlerAction.close()
    }

    private class MessageListener(
        private val reference: WeakReference<ChatAllConversationViewModel>,
    ) : OnMessageTransferListener {
        override fun onSent(id: UUID) {
            reference.get()?.messagesSent?.add(id)
        }
    }

}
```
