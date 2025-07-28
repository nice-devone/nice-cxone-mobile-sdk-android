# Custom UI Implementation

> [!IMPORTANT]
> We strongly recommend that you use UI module artifact to handle & display the chat as it handles all common
> use-cases.
> Sections from this point are for advanced use-cases which are usually driven by your specific application requirements.

This document describes a previously recommended way how to implement your own custom UI for the CXone Chat SDK.

## Pre-requisites

Before you start implementing your own custom UI, you need to follow the steps described in the [Implementation Guide](implementation.md)
to set up the CXone Chat SDK in your application up-to (excluding) the chapter [Integrate UI module](implementation.md#integrate-ui-module).

## Push Notification Tokens

This part of configuration is obviously not required, but if you want your clients to receive push notifications, you need
to pass us the device push token.

The push token can be, for most applications anyway, requested ad-hoc from firebase services, or is
provided to you via BroadcastReceiver.

Agent console also needs to have your application registered, which might involve using a Firebase
credentials file.

```kotlin
  val chat = MyChatInstanceProvide.chat ?: return
  chat.setDeviceToken(yourDeviceToken)
```

[Case Study Push Notifications](cs-push-notifications.md) offers more detail about this topic.

## Conversations (formerly Threads)

In this section, "Threads" and "Conversations" are used interchangeably. The term "Conversations" is a newer term that may appear in some
parts of the documentation or UI, but it refers to the same concept as "Threads."

Depending on your configuration, you might be able to create either multiple Threads or you will be
stuck with one.
In any case, the flow is the same, the limitations are described in the code documentation as effects
for some threading methods.

First, you're required to fetch a Threads list.
This is a list of all the threads you can access and have been created by this app's instance.

> [!WARNING]
> Note that when retrieving a `Handler` you don't have to keep the instance since it is internally memoized.
> Some methods may have effects directly on the given handler or parent handlers

  ```kotlin
  val threadsHandler = chat.threads() // (1)
  threadsHandler.threads {
    // todo save the threads list
    // update ui
  }
  ```

> [!NOTE]
> Note that we do not encourage a specific pattern as every application's code might be different.
> Use your own expertise to determine how to update the UI and save the list of threads.

> [!WARNING]
> Some listener methods return Cancellable effect.
> You, are required to cancel the effect once it's no longer necessary.

Now that you have saved the list of threads, you have options, depending on whether the
configuration is single or multi-threaded.

### Single Thread

Use a thread list to fetch the first instance in the list OR create a new thread.

> [!NOTE]
> Note that a failure to follow these exact steps might cause an exception. Single Threaded
> instances can have at most one thread.
> Thrown exception can be of type `UnsupportedChannelConfigException` in case you are trying
> to create second thread (or archive current one),
> or it can be of type `MissingThreadListFetchException` when you have called `create` before the chat has signaled
`ChatStateListener.onReady()`.
> If the channel configuration doesn't include pre-chat survey, the Chat will prepare an empty thread with no messages automatically.

```kotlin
    val threadsHandler: ChatThreadsHandler = chat.threads()

    fun onThreadListUpdate(threads: List<ChatThread>) {
      val thread = threads.firstOrNull()
      val threadHandler = when (thread) {
        null -> displayPreChatSurvey(::threadsHandler.create)
        else -> threadsHandler.thread(thread)
      }
      // Use the `threadHandler` to interact with the thread
    }
    
    fun displayPreChatSurvey(
        onSurveySubmit: (responses: List<PreChatSurveyResponse>) -> Unit
    ) {
        // Display a dialog window with survey questions
        // See [Pre-chat dynamic surveys](#pre-chat-dynamic-surveys) for more information
    }
```

### Multiple Threads

There's virtually no limitation on how many threads the user can create,
and in this case, yes, the user is creating the threads.
Not the application by itself.

```kotlin
  fun onThreadClick(thread: ChatThread) {
    val threadHandler = threadsHandler.thread(thread)
  }
  
  fun onThreadCreateClick() {
    val threadHandler = threadsHandler.create()
  }
```

### Pre-chat dynamic surveys

Chat channel configuration defined on backend can optionally contain a pre-chat survey.
Pre-chat survey if present will contain label string which should describe the survey to the user,
and it can contain multiple pre-chat survey questions (but always at least one).
Each question has defined label and type, which also defines a possible type of the answer,
all of which are captured in the following table:

| Survey type  | Response type   | Response notes                                                                                                                      |
|--------------|-----------------|-------------------------------------------------------------------------------------------------------------------------------------|
| Text         | `String`        | Free form text supplied by the user                                                                                                 |
| Email        | `String`        | Text which was validated by the application, to adhere to its requirements for valid email address.                                 |
| Selector     | `SelectorNode`  | Response has to be one of provided Selection instances from survey type `values`.                                                   |
| Hierarchical | `HierarchyNode` | Node instance which is also a leaf node. Instance comes from `values` present in the question. Non-leaf node answers are discarded. |

Survey questions should be presented to the user when he attempts to create a new thread and all
questions which are flagged as required has to be answered before a new thread can be created.

```kotlin
  // Naive sample assuming single survey question of type Text
  fun onThreadCreateClick(question: PreChatSurveyType.Text, answer: String) {
    val responses = listOf(
      PreChatSurveyResponse.Text(question, answer)
    )
    val threadHandler = threadsHandler.create(responses)
  }
```

> [!NOTE]
> Note that attempts to create thread without supplying responses to required survey questions will
> cause an exception.
>
> Response sequence to pre-chat surveys is converted to custom field values and are sent with
> a first thread message.
---

Now that your configuration uses multiple or single threads you have obtained the Thread Handler.
You can furthermore explore what can you with these objects and as long you follow the principles
defined by warnings and notes above, you're generally good to go.

> [!TIP]
> If you have experience with browsing and using SDKs on your own, you can skip all the following
> documentation. It's well documented in the code itself and can be used as reference.

> [!NOTE]
> SDK is in-memory caching thread information for loaded threads (thread is considered loaded once
> it's ChatThreadHandler has refreshed its information or user has sent at least one message).
> The cache update is also propagated as thread list update.

---

## Thread

With thread, you're permitted to do all sorts of things, we'll cover the most here, but always
consult the in-code documentation. It gives more in-depth info than can be found here.

### Fetching Thread

You have several options here. One is that you're permitted to listen to the Thread changes —
which include agent changes, messages and other updates. Another is to fetch the _current_ state
(…of the Thread) that the library holds.

#### Listen to Thread changes

Thread changes typically include Metadata refresh, Agent swaps or new sent/received Messages. Might
be extended in the future with more events and/or more reactivity.

> [!WARNING]
> If you don't use this form of listening to Thread changes, effect-inducing
> methods (`ChatThreadMessageHandler::loadMore` or similar) will have no effect when invoked.

```kotlin
  threadHandler.get {
    // TODO save current state and/or
    // update ui
  }
  threadHandler.refresh()
```

#### Get current Thread state

At any point you might request the handler to return the current Thread state. This will be updated
every time `get {}` is called. Even if cancelled the **instance** will remember the latest value.

```kotlin
  val thread = threadHandler.get()
```

### Update Thread name

Changes the name for this particular thread. Can be user generated or even automatically generated
by your application.

```kotlin
  threadHandler.setName("New Thread Name")
```

### Send a Message

There are multiple messages you can send at this point. Notably Text or Attachment messages. Both
can be listened to, so UI reflects true state of any given message. Listeners are obviously
optional.

```kotlin
  val messageHandler = threadHandler.messages()
  messageHandler.send("Hello world!")
```

… or the same with a listener:

  ```kotlin
  messageHandler.send(
    "Hello world!",
    OnMessageTransferListener(
      onProcessed = { /* notify UI */ },
      onSent = { /* notify UI */ }
    )
  )
  ```

### Send a Message with a document

Refer to the [Message States](#message-states) for more info how attachment (document) messages
differ from regular text Messages.

> Repeated requests with the same attachment will not be uploaded.
> Identical reference is used to save bandwidth.

```kotlin
  val descriptor = ContentDescriptor(
    content = myPdfFileUri,
    context = context, // any Android context for URI resolution
    mimeType = "application/pdf",
    fileName = "${UUID.randomUUID()}.pdf",
    friendlyName = "my-awesome-pdf.pdf"
  )
  messageHandler.send(listOf(descriptor))
```

> [!WARNING]
> Note that attachments passed as Uri will be entirely read into memory before being uploaded, so be careful your uploaded files are not too
> large.

If you want to compress images, clip videos, it's a good time to do so before
passing it to `ContentDescriptor`.
If you are compressing the images or videos before processing, it may be
convenient to use the alternate constructor for `ContentDescriptor`:

```kotlin
  val descriptor = ContentDescriptor(
    content = myByteArray,
    mimeType = "image/jpeg",
    fileName = "${UUID.randomUUID()}.jpg",
    friendlyName = "imageName.jpg"
  )
  messageHandler.send(listOf(descriptor))
```

> [!WARNING]
> Note that such attachments will be stored in memory until they are uploaded to the server.
> Be careful how large files you'll upload as they also have to conform to the file size limits enforced by the channel configuration.
> Channel configuration file size restrictions (in MB) can be retrieved from the `Chat` instance via
`chat.configuration.fileRestrictions.allowedFileSize`.
> SDK enables `largeHeap` in its Android Manifest to allow loading of large files to memory for this purpose.

In case of an issue during attachment upload, the application will be notified via `ChatStateListener.onChatRuntimeException`, if the
optional `ChatStateListener` instance was supplied to the SDK. The `onChatRuntimeException` will be invoked with an
instance of `RuntimeChatException.AttachmentUploadError` which will contain information about the cause and the
attachment filename.

### Load more Messages

Loading more messages requires `threadHandler.get {}` to be active. Updates are delivered through
that callback.

  ```kotlin
  messageHandler.loadMore()
  ```

### Update Thread-specific Custom Fields

Add any key-value pairs to this Thread. They will be locally stored until next action is performed
on the given thread. (ie. Sending a Message, …)

  ```kotlin
  val fieldHandler = threadHandler.fields()
  fieldHandler.add(mapOf("pet-preference" to "dog"))
  ```

### Listen to Actions

If you want to support popups, you should register this callback (or at least create
the `actionHandler`) as soon as possible.
The SDK cannot guarantee that this callback will be called multiple times,
nor it can guarantee that it will be called at least once.

  ```kotlin
  val actionHandler = threadHandler.actions()
  actionHandler.onPopup { variables, metadata ->
    // save metadata for analytic events
    // show popup with variables (should be )
  }
  // when done with actions (typically after receiving the first one)
  actionHandler.close()
  ```

### Send an Event

You are permitted to send various types of events that are Thread specific. The API is designed to
be flexible, so we can add more events in the future relatively painlessly. Check the Available
Events section below or browse object `com.nice.cxonechat.ChatThreadEventHandlerActions` for more info.

  ```kotlin
  fun archiveThread(threadHandler: ChatThreadsHandler) {
    val eventHandler = threadHandler.events()
    eventHandler.archiveThread()
  }
  ```

#### Available Events

- **TypingStartEvent**
    - The user has started typing,
      typically has a keyboard opened and has issued any form of input to the
      text field within a reasonable time frame (say 5 seconds)
- **TypingEndEvent**
    - The user has stopped typing, typically has no keyboard visible or stopped issuing any form
      of text within a reasonable time frame.
- **ArchiveThreadEvent**
    - Archive current thread, may disallow users to interact with it
- **MarkThreadReadEvent**
    - Notifies backend that messages up to >this< point have been read by the user
- **LoadThreadMetadataEvent**
    - Make a request to backend for additional information about the thread. E.g.: last message in thread.

---

## Message States

All messages sent from the mobile device go through these specific steps (or states).
You're free to use only some of those indications, or all of them.
It's completely up to you.
Though you might find a helpful description of how this state machine works.

### Sending

All text messages are automatically processed as soon as they are sent
through `ChatThreadMessageHandler::send`. Messages with attachments (documents) are processed
by sending them to a storage server first. Once successfully stored, they are reported as processed through listener.

The state is never directly reported by the SDK for a message, since only successfully sent messages, are reported.
UI implementation are encouraged to use this state to show the user that the message is being sent.

### Sent

The Message reaches this state once it successfully leaves this device.
If it doesn't leave this device, then the corresponding callback is never triggered.

### Received

The Received state is implicit. That means that if the `ChatThreadHandler::get` with callback returns
the message in its list of messages, the message was received successfully by the server.

If your new message is not received within a reasonable amount of time through this callback, offer
your users to resend the message.

### Read

The agent has read the message and/or acted upon it. This indication is now part of the Message
object received through aforementioned `ChatThreadHandler::get`.

### FailedToDeliver

This state is not directly reported by the SDK, but can be inferred,
when listener callback `ChatStateListener.onChatRuntimeException` receives an instance of
`ServerCommunicationError` with message `SendingMessageFailed`.

UI implementation are encouraged to use this state to show the user that the message failed to be sent.

## Additional steps

For additional steps, such as handling of manual user name updates, please refer to
the [implementation guide](implementation.md#manual-username-update)
and further sections.
