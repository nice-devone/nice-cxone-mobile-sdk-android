# Integrator's guide

This document will guide you through the necessary steps to integrate CXOne Chat SDK application of
your own.

> Please follow the steps diligently, that will ensure you're using the SDK in a correct way. If
> you're unsure about any method and what causes it may incur, consult the documentation provided
> with
> the clone of your public SDK. That would typically be a bundled JAR or a link to current HTML
> documentation.

All examples are written in Kotlin, though you might use Java with this SDK. The SDK version you've
been provided is heavily obfuscated to discourage you from using internal APIs.

> We strongly urge you to not use reflection for any of CXOne SDK classes. You code **will** break
> from release to release.

> Note that in every example the instance of any given Handler is created at most once. Make sure to
> follow suit.

## Proguard

There are no specific Proguard rules needed for this library. If there will be in the future, they
will be bundled with your aar and provided automatically with Maven.

## Setting Up

Ensure you've received your **Region**, **Brand ID** and **Channel ID**.

- Region
  - Is referenced in code as "environment"
  - Is typically closest to your main deployment region
  - Is one of (but not limited to):
    - `NA1`, `EU1`, `AU1`, `CA1`, `UK1`, `JP1`
    - List may grow and may not be documented here, consult code reference for more clarity.
- Brand ID
  - Is typically 4 digit integer
- Channel ID
  - Is typically a UUID string prefixed with "chat_"

Once you got all of this data, you may proceed.

> If you're unsure where to get these values, you should consult your CXOne representative or local
> managers depending on your company structure.

### Startup

First you need to obtain `Chat` instance. You can achieve that through our `ChatBuilder`

```kotlin
val config = SocketFactoryConfiguration(
  CXOneEnvironment.YourRegion,
  yourBrandId,
  yourChannelId
)
cancellable = ChatBuilder(context, config)
  .setDevelopmentMode(BuildConfig.DEBUG)
  .setAuthorization(yourAuthorization) // (1)
  .build { chat ->
    // TODO save chat instance
  }
```

- (1) Authorization
  - Depending on whether you use oAuth, you might be required to use Authorization.
  - If you don't use oAuth, then don't call `.setAuthorization` method

---

Great! Now you're ready to use the CXOne Chat SDK.

> In case the startup was not successful for you and `build` method did not return the `Chat`
> instance, be sure to check your configuration as server might have rejected the request. Read the
> documentation for `build` method for more clarity on the subject.

## Push Notification Tokens

This is obviously not required, but if you want your clients to receive push notifications you need
to pass us the device push token.

The push token can be, for most applications anyway, requested ad-hoc from firebase services, or is
provided to you via BroadcastReceiver.

Agent console also needs to have your application registered which might involve using Firebase API
key.

```kotlin
val chat = MyChatInstanceProvide.chat ?: return
chat.setDeviceToken(yourDeviceToken)
```

## Global Events

### Available events

- **ChatWindowOpenEvent**
  - Specific Chat screen (conversation) has been opened
- **ConversionEvent**
  - User was redirected from other media (link, etc…), made a purchase, read an article. Anything
    your company internally defines as a conversion
- **CustomVisitorEvent**
  - Any event you may want to track
- **PageViewEvent**
  - User has visited URL that was linked to them, or viewed some other screen within "chat" flow.
- **ProactiveActionClickEvent**
  - Action regards to `ChatActionHandler::onPopup`
- **ProactiveActionDisplayEvent**
  - Action regards to `ChatActionHandler::onPopup`
- **ProactiveActionFailureEvent**
  - Action regards to `ChatActionHandler::onPopup`
- **ProactiveActionSuccessEvent**
  - Action regards to `ChatActionHandler::onPopup`
- **TriggerEvent**
  - Trigger an automation event by ID
- **VisitEvent**
  - Landing page for chat has been opened, the tracked session ends within 30 minutes of inactivity.

## Threads

Depending on your configuration you might be able to create either multiple Threads or you will be
stuck with one. In any case the flow is the same, the limitations are described in the code
documentation as effects for some threading methods.

First you're required to fetch a Threads list. This is a list of all the threads you can access and
have been created by this app's instance.

> ⚠️ Note that when retrieving a `Handler` you should keep the instance as long as you project
> needing it. Some methods may have effects directly on the given handler or parent handlers

```kotlin
val threadsHandler = chat.threads() // (1)
threadsHandler.threads {
  // todo save the threads list
  // update ui
}
threadsHandler.refresh()
```

> Note that we do not encourage specific pattern as every application's code might be different. Use
> your own expertise to determine how to update the UI and save the list of threads.

> ⚠️ Warning! Some listener methods return Cancellable effect. You, are required to cancel the
> effect once it's no longer necessary.

Now that you have saved the list of threads, you have options, depending on whether the
configuration is single or multi-threaded.

### Single Thread

Use threads list to fetch the first instance in the list OR create a new thread.

> Note that a failure to follow these exact steps might cause an exception. Single Threaded
> instances can have at most one thread.

```kotlin
val threads: List<ChatThread> // stored somewhere
val thread = threads.firstOrNull()
val threadHandler = when (thread) {
  null -> threadsHandler.create()
  else -> threadsHandler.thread(thread)
}
```

### Multiple Threads

There's virtually no limitation on how many threads the user can create and in this case, yes, user
is creating the threads. Not the application by itself.

```kotlin
fun onThreadClick(thread: ChatThread) {
  val threadHandler = threadsHandler.thread(thread)
}

fun onThreadCreateClick() {
  val threadHandler = threadsHandler.create()
}
```

---

Now that your configuration uses multiple or single threads you obtained Thread Handler. You can
furthermore explore what can you with these objects and as long you follow the principles defined by
warnings and notes above, you're generally good to go.

> ❓ If you have experience with browsing and using SDKs on your own, you can skip all the following
> documentation. It's well documented in the code itself and can be used as reference.

---

## Thread

With thread you're permitted to do all sorts of things, we'll cover the most here, but always
consult the in-code documentation. It gives more in-depth info than can be found here.

### Fetching Thread

You have several options here really. One is that you're permitted to listen to the Thread changes -
which includes agent changes, messages and other updates. Another is to fetch the _current_ state (
…of the Thread) that the library holds.

#### Listen to Thread changes

Thread changes typically include Metadata refresh, Agent swaps or new sent/received Messages. Might
be extended in the future with more events and/or more reactivity.

> ⚠️ If you don't use this form of listening to Thread changes, effect-inducing
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

### Send a Message with document

Refer to the [Message States](#Message States) for more info how attachment (document) messages
differ from regular text Messages.

If you want to compress images, clip videos, it's a good time to do so before passing it
to `ContentDescriptor`.

> Note that attachments are stored in memory until they are uploaded to the server. Be careful how
> large files you'll upload.

> Repeated requests with the same attachment will not be uploaded. Identical reference is used to
> save bandwidth.

```kotlin
val descriptor = ContentDescriptor(
  content = myPdfFile.toBase64(),
  mimeType = "application/pdf",
  fileName = "my-awesome-pdf.pdf"
)
messageHandler.send(listOf(descriptor))
```

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
the `actionHandler`) as soon as possible. The SDK cannot guarantee that this callback will be called
multiple times, nor it can guarantee that will be called at least once.

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
Events section below or browse package `com.nice.cxonechat.event.thread` for more info.

```kotlin
val eventHandler = threadHandler.events()
eventHandler.trigger(ArchiveThreadEvent)
```

#### Available Events

- **TypingStartEvent**
  - User has started typing, typically has keyboard opened and has issued any form of input to the
    text field within reasonable time frame (say 5 seconds)
- **TypingEndEvent**
  - User has stopped typing, typically has no keyboard visible or stopped issuing any form of text
    within reasonable time frame
- **ArchiveThreadEvent**
  - Archive current thread, may disallow users to interact with it
- **MarkThreadReadEvent**
  - Notifies backend that messages up to >this< point have been read by the user

---

## Message States

All messages sent from the mobile device go through these specific steps (or states). You're free to
use only some of those indications, or all of them. It's completely up to you. Though you might find
helpful description on how this state machine works.

#### Processed

All text messages are automatically processed as soon as they are sent
through `ChatThreadMessageHandler::send`. Messages with attachments (documents) are being processed
by sending them to a storage server first. Once successfully stored, they are marked processed.

This state might be beneficial for your users to see indeterminate progressbar as the message "is
being sent".

#### Sent

Message reaches this state once it successfully leaves this device. If it doesn't leave this device
then the corresponding callback is never triggered.

#### Received

Received state is implicit. That means that if the `ChatThreadHandler::get` with callback returns
the message in its list of messages, the message was received successfully by the server.

If your new message is not received within reasonable amount of time through this callback, offer
your users to resend the message.

#### Read

Agent has read the message and/or acted upon it. This indication is now part of the Message object
received through aforementioned `ChatThreadHandler::get`.
