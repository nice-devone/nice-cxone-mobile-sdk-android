# Case Study: Single Thread

> If you haven't read it yet, this CS takes classes defined
> in [CS: Instance Holder][cs-instance-holder].

> [!IMPORTANT]
> Usage of this case study is intended for scenarios where you are not using provided UI module artifact.
> If you are using the UI module, then this use-case is already covered by the UI module and the case study
> is only provided for educational purposes.

Whenever you'd like to use only a single thread, regardless of server-side settings, you can take
several shortcuts. Most notably, you can bypass the listing of the threads list and automatically
pick the first conversation or create a new one.

> [!NOTE]
> Note that this example doesn't use any additional libraries for performing asynchronous workflows.
> If you prefer exploring how parts of this app would look with coroutines, you can visit the
> corresponding [Case Study][cs-coroutines]

## Libraries

This example uses AndroidX ViewModel libraries to demonstrate the projected usage.

```groovy
dependencies {
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6"
}
```

### `ChatConversationViewModel.kt`

For the sake of brevity, this example omits some features. For full feature-rich ViewModel
visit [CS: Multi Thread][cs-multi-thread].

> As this ViewModel uses a requirement for chat to exist, visit the screen only when you're certain
> that the ChatInstanceProvider::chat is not null, otherwise the app will crash.

Example below can be simplified with frameworks, visit [CS: Coroutines][cs-coroutines] for more
info.

```kotlin
class ChatConversationViewModel : ViewModel() {

    private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
    private val handlerThreads = chat.threads()
    private lateinit var handlerThread: ChatThreadHandler
    var thread: ChatThread? = null
        private set

    init {
        viewModelScope.launch {
            // (1) Fetch the current threads list (first emission) and pick or create a thread
            val existingThread = handlerThreads.threadsFlow
                .first()
                .firstOrNull()

            handlerThread = when (existingThread) { // (2)
                null -> handlerThreads.create()
                else -> handlerThreads.thread(existingThread)
            }

            // (3) Observe the thread for updates
            handlerThread.threadFlow
                .onStart { handlerThread.refresh() }
                .collect { updated ->
                    thread = updated
                    // notify ui
                }
        }
    }

}
```

- (1) `threadsFlow` triggers `refresh()` automatically on first collection and emits the current
  list; `first()` awaits that initial emission and then cancels collection — equivalent to the
  one-shot callback cancel pattern
- (2) Create or select a Thread
  - When Thread exists, then pick a thread otherwise create a new one.
  - Since the Chat automatically creates thread if there is no pre-chat survey,
    we can assume that survey is required if thread is not present.
- (3) `.onStart { handlerThread.refresh() }` fires once collection is active, asking the server
  for the latest thread state; subsequent emissions arrive via `threadFlow` whenever the server
  sends an update for this thread. Without the `onStart` hook, `refresh()` would run before a
  collector is registered and would be a no-op per `ChatThreadHandler.refresh()`'s contract.

[cs-instance-holder]: cs-instance-holder.md

[cs-coroutines]: cs-coroutines.md

[cs-multi-thread]: cs-multi-thread.md
