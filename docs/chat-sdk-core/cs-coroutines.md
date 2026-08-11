# Case Study: Coroutines

> [!IMPORTANT]
> Usage of this case study is intended for scenarios where you are not using provided UI module artifact.
> If you are using the UI module, then this use-case is already covered by the UI module and the case study
> is only provided for educational purposes.

Coroutines are widely accepted framework in the Android space, therefore we would like to show you
how to implement some extensions atop of CXone Chat SDK.

The following examples are based on source code in the Chat SDK UI module, which can be
found [here](../../chat-sdk-ui/src/main/kotlin/com/nice/cxonechat/ui/viewmodel).

## Libraries

Additional dependencies required to run these samples. All samples are validated for version
described in the `dependencies` block, Major update revisions may vary in syntax.

```groovy
dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
    runtimeOnly "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2"
}
```

### `ChatInstanceProviderExt.kt`

This is an extension for `ChatInstanceProvider` described [here][cs-instance-holder].

> [!WARNING]
>  We do not necessarily believe that this is the "go-to" implementation for indicating that the
> chat is ready. Implement callbacks to your `ChatInstanceProvider`, if necessary. Though this is
> useful for demonstration or PoC purposes.

You should prefer `flowAlt` for production code and check whether the chat is initialized before
accessing the SDK functionality.

```kotlin
val ChatInstanceProvider.flow
    get() = flow {
        while (currentCoroutineContext().isActive) {
            emit(chat)
            delay(1.seconds)
        }
    }
        .filterNotNull()
        .distinctUntilChanged()

val ChatInstanceProvider.flowAlt
    get() = flowOf(chat)
        .filterNotNull()
```

> The ViewModels below use `ChatInstanceProvider.get().chat.let(::requireNotNull)` for brevity —
> they assume the chat instance is already connected. Wire them behind `flowAlt` (or your own
> readiness gate) if your navigation may reach these screens before the SDK finishes connecting.

### `ChatConversationsViewModel.kt`

Simplified example for implementing threads list with coroutines. `threadsFlow` emits the current
thread list whenever `refresh()` is called or the server pushes an update. The SDK triggers
`refresh()` automatically the first time `threadsFlow` is collected, so no explicit refresh is needed
here. (Call `handler.refresh()` yourself only when you want to force a re-fetch, e.g. on a manual
pull-to-refresh.)

```kotlin
class ChatConversationsViewModel : ViewModel() {

    private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
    private val handler = chat.threads()

    val threads = handler.threadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

}
```

### `ChatConversationViewModel.kt`

Simplified example for implementing thread metadata and messages with coroutines. The `threadFlow`
property emits the current thread state whenever the server sends an update.

```kotlin
class ChatConversationViewModel(
    thread: ChatThread,
) : ViewModel() {

    private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
    private val handler = chat.threads().thread(thread)

    val thread: StateFlow<ChatThread> = handler.threadFlow
        .onStart { handler.refresh() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), handler.get())

    val messages
        get() = thread.map { it.messages }

}
```

[cs-instance-holder]: cs-instance-holder.md
