# Case Study: Live Chat

The CXone Chat provides two main modes in which it functions, Messaging (Single & Multi-Threaded)
and Live Chat.
Live Chat offers same basic chat functionality as asynchronous Messaging,
but it also provides clear indication when the user was assigned to the agent contact queue and provides
updates on users position in the said queue.
Finally when agent marks the contact Case as closed, user is notified about the event and the
conversation is switched to read only mode. User is then presented with options to review the conversation
transcript, start a new chat conversation/contact Case (which clears the transcript)
or close the chat entirely.
Once the conversation Case is closed and cleared, either through manual restart of the conversation
or Chat SDK reinitialization the transcript history is cleared for the client.

> The Live Chat shares many limitations of the Single Threaded Messaging.
> For example it is not possible to set the thread name or to archive the thread.

As part of the connect operation it will attempt to recover an existing thread, if it's `canAddMoreMessages` 
property is true, indicating the thread is still active, otherwise, a new thread must be created.

## Example

>This case study builds on the information in [CS: Instance Holder][cs-instance-holder], so you should familiarize yourself
>with that before continuing.
> A complete reference how to integrate the Live Chat can be found in the chat-sdk-ui and the store sample app, this
> example omits some details like handling of reconnection for brevity.

With brand & channel which are set as live chat, perform following steps:

1. Obtain an instance of `ChatInstanceProvider` using
   `ChatInstanceProvider.create(SocketFactoryConfiguration(environment, brandId, channelId))`.
2. Register `ChatInstanceProvider.Listener` with the instance of `ChatInstanceProvider`.
3. Call `chatInstanceProvider.prepare(context)`.
4. Once the `ChatInstanceProvider` instance `ChatState` has reached `Prepared` state, you will either receive the
   `Chat` instance via the registered listener or it can be obtained from the instance provider.
    * If the channel configuration is correct, the `chat.chatMode` will return `LiveChat`.
5. Call `chatInstanceProvider.connect()` when user enters flow where he may want to enter the chat.
6. Handle resulting state of the chat
    * If the channel is not available (because of SLA setup, no agent online) the `ChatInstanceProvider` will report that `ChatState`
      is `Offline`,
      in that case inform the user.
    * If the state is `Ready` you can continue with following steps.
7. Create thread if none is retrieved in thread list
    * If the PreChatSurvey is non-null it will have to be used for collection of answers, which are required for creation of thread handler.
8. Handle chat transcript UI with loaded/updated thread data.
9. Handle end of conversation from the customer perspective.

### Code example

#### `ChatConversationViewModel`

> As this ViewModel uses a requirement for chat to exist, visit the screen only when you're certain
> that the ChatInstanceProvider::chat is not null, otherwise the app will crash.

> Example below can be simplified with frameworks, visit [CS: Coroutines][cs-coroutines] for more
info.

Example below is simplified as it doesn't handle callbacks back to the ui for edge-cases.

```kotlin
class ChatConversationViewModel : ViewModel() {
   private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
   private val threadsHandler: ChatThreadsHandler = chat.threads()
   private val chatThreadUpdateFlow: Flow<ChatThread?> = callbackFlow {
      val cancellable = threadsHandler.threads(::trySend)
      awaitClose(cancellable::cancel)
   }
      .map(List<ChatThread>::firstOrNull)

   private val threadHandlerFlow: StateFlow<ChatThreadHandler?> = chatThreadUpdateFlow
      .filterNotNull()
      .map(threadsHandler::thread)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

   val threadFlow: Flow<ChatThread?> = threadHandlerFlow
      .filterNotNull()
      .flatMapLatest { chatThreadHandler ->
         callbackFlow {
            val cancellable = chatThreadHandler.get(::trySend)
            awaitClose(cancellable::cancel)
         }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

   val isConversationClosedFlow: StateFlow<Boolean> = threadHandlerFlow
      .map { threadHandler ->
         !chat.isChatAvailable || threadHandler != null && !threadHandler.get().canAddMoreMessages
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), true)

   val isThreadCreationRequired: Flow<Boolean> = chatThreadUpdateFlow
      .map { chatThread ->
         chat.isChatAvailable && (chatThread == null || !chatThread.canAddMoreMessages)
      }

   val preChatSurvey: PreChatSurvey? = threadsHandler.preChatSurvey

   fun createThread(preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>> = emptySequence()) {
      threadsHandler.create(preChatSurveyResponse)
   }

   fun sendMessage(message: OutboundMessage) {
      viewModelScope.launch {
         threadHandlerFlow.first()?.messages()?.send(message)
      }
   }
}
```

[cs-instance-holder]: cs-instance-holder.md

[cs-coroutines]: cs-coroutines.md
