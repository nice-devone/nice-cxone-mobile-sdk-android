# Case Study: Coroutines

Coroutines are widely accepted framework in the Android space, therefore we would like to show you
how to implement some extensions atop of CXone Chat SDK.

The following examples are based on source code in the Chat SDK UI module, which can be found [here](../chat-sdk-ui/src/main/java/com/nice/cxonechat/ui/main).

## Libraries

Additional dependencies required to run these samples. All samples are validated for version
described in the `dependencies` block, Major update revisions may vary in syntax.

```groovy
dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0"
    runtimeOnly "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"
}
```

### `ChatThreadsHandlerExt.kt`

Threads callback can be consumed as callback flow, thereafter you can attach effects or morph the
data to your liking. Once the flow is disposed, the callback will automatically cancel.

```kotlin
val ChatThreadsHandler.flow
    get() = callbackFlow {
        val cancellable = threads(::trySend)
        awaitClose {
            cancellable.cancel()
        }
    }
```

### `ChatThreadHandlerExt.kt`

Thread callback can be consumed as callback flow, thereafter you can attach effects or morph the
data to your liking. Once the flow is disposed, the callback will automatically cancel.

```kotlin
val ChatThreadHandler.flow
    get() = callbackFlow {
        val cancellable = get(::trySend)
        awaitClose {
            cancellable.cancel()
        }
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

### `ChatConversationsViewModel.kt`

Simplified example for implementing threads list with coroutines.

```kotlin
class ChatConversationsViewModel : ViewModel() {

    private val handler = ChatInstanceProvider.get().flowAlt
        .map { it.threads() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val threads = handler.filterNotNull()
        .flatMapLatest { it.flow }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

}
```

### `ChatConversationViewModel.kt`

Simplified example for implementing thread metadata and messages with coroutines.

```kotlin
class ChatConversationViewModel(
    thread: ChatThread,
) : ViewModel() {

    val handler = ChatInstanceProvider.get().flowAlt
        .map { it.threads().thread(thread) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val thread = handler
        .filterNotNull()
        .flatMapLatest { it.flow }
        .stateIn(viewModelScope, Companion.WhileSubscribed(), null)

    val messages
        get() = thread
            .filterNotNull()
            .map { it.messages }

}
```

[cs-instance-holder]: cs-instance-holder.md
