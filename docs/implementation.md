# Integrator's guide

This document will guide you through the necessary steps to integrate CXone Chat SDK application of
your own.

> Please follow the steps diligently, that will ensure you're using the SDK in a correct way.
> If you're unsure about any method and what causes it may incur,
> consult the documentation provided with the clone of your public SDK.
> That would typically be a bundled JAR or a link to current HTML documentation.

All examples are written in Kotlin, though you might use Java with this SDK. The SDK version you've
been provided is heavily obfuscated to discourage you from using internal APIs.

> We strongly urge you to not use reflection for any of CXone SDK classes.
> Your code **will** break from release to release.

> Note that in every example, the instance of any given Handler is created at most once.
> Make sure to follow suit.

## Proguard / R8

There are specific Proguard rules needed for this library.
They are bundled with the chat-sdk-code aar and are provided automatically with Maven, alternatively they can be found in a file
[chat-sdk-core/consumer-rules.pro](../chat-sdk-core/consumer-rules.pro) and copied directly to your rule file.

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

Once you got all of these data, you may proceed.

> If you're unsure where to get these values, you should consult your CXone representative or local
> managers depending on your company structure.

### Startup

First you need to obtain `Chat` instance. You can achieve that through our `ChatBuilder`

```kotlin
val config = SocketFactoryConfiguration(
  CXOneEnvironment.YourRegion,
  yourBrandId,
  yourChannelId
)
val myChatStateListener = object : ChatStateListener() {
  override fun onReady() {
    // TODO - Chat instance is ready for usage by the consumer, use Chat instance for chat
  }
}
cancellable = ChatBuilder(context, config)
  .setDevelopmentMode(BuildConfig.DEBUG)
  .setAuthorization(yourAuthorization) // (1)
  .setUserName("firstName", "lastName") // (2)
  .setChatStateListener(myChatStateListener)
  .build { chat ->
    // TODO save chat instance
  }
```

- (1) Authorization
  - Depending on whether you use oAuth, you might be required to use Authorization.
  - If you don't use oAuth, then don't call `.setAuthorization` method.
- (2) Username
  - Usage depends on the fact if you are using oAuth.
    The OAuth users typically won't need to set username,
    since user details will be retrieved from oAuth backend.
  - If you are using a manual username setup, please follow instructions on updating the username
    (if it can change in your application).

> ℹ️
> The `build` method asynchronously creates an instance of Chat which is ready for analytics usage, for chat use-case it
> needs to be connected.
> Chat will start the asynchronous connection attempt once the `Chat.connect()` method is called.
>
> In case of connection error, the application will be notified and it will have to schedule a connection retry attempt.
> Application can cancel both the build and connection process according to its requirements via `Cancellable` instance
> returned from the `build` and `connect` method calls.

> ⚠️ Important note
> In case the startup was not successful for you and `build` method did not return the `Chat`
> instance, be sure to check your configuration as server might have rejected the request. Read the
> documentation for `build` method for more clarity on the subject.

---

Now you can use the CXone Chat SDK for sending of analytics events (which are used for automation).
If you also need to activate the chat, you will need to connect it to backend and let Chat perform basic preparation of
the instance.

First you need to inform `Chat` instance that it should connect to backend by calling `chat.connect`.
Once it is connected the `Chat` instance will call the supplied `ChatStateListener.onConnected` callback.
At this moment the `Chat` has established socket connection with backend and it will start final background
tasks to fully prepare instance for usage (retrieval of the thread in single-thread mode or thread list in the
multi-thread mode). `Chat` will inform that is fully ready by calling the `ChatStateListener.onReady` callback.

Great! Now you're ready to use the CXone Chat SDK.

> ⚠️ Important note
> Chat instance maintains open socket connection to backend, until `chat.close()` is called, or the
> application process is terminated.
> It is the responsibility of the integrating application,
> to close the chat when the user leaves the part of application dedicated to chat
> (usually dedicated Activity) as it is outlined in terms of use for the SDK.

## Configuration
You can use the chat `configuration` property to support different UI/UX flows in your application and also to
preemptively verify that your assumptions about active chat configuration are correct.

```kotlin
val chat = MyChatInstanceProvider.chat ?: return
val chatConfiguration = chat.configuration
```

## Push Notification Tokens

This is obviously not required, but if you want your clients to receive push notifications, you need
to pass us the device push token.

The push token can be, for most applications anyway, requested ad-hoc from firebase services, or is
provided to you via BroadcastReceiver.

Agent console also needs to have your application registered, which might involve using a Firebase
API key.

```kotlin
val chat = MyChatInstanceProvide.chat ?: return
chat.setDeviceToken(yourDeviceToken)
```

## Custom Fields
Custom fields represent metadata about customers / users.
There are two types of these custom fields based on the context of the said data:
1. Customer Custom Fields — These are global for all chat threads and shouldn't contain information about one specific conversation.
2. Contact Custom Fields — These contain metadata relevant to the one specific chat thread / conversation.

Custom fields are defined as part of the channel configuration (on backend), and only defined custom fields can be supplied to the chat instance.

### Custom Field definitions
Custom field definitions can be accessed by using chat configuration instance.
Definition provides a `label` field which can hold human-readable label (or reference to string resource) and `fieldId`
which is used for adding of custom field values.

#### Custom field types & expected values
| Type      | Required value                         | Validations                                                                                                                    |
|-----------|----------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| Text      | Any `String`                           | Value is validated against pattern `android.utils.Patterns.EMAIL_ADDRESS` is used if definition has flag `isEMail` set to true |
| Selector  | `nodeId` from selected `SelectorNode`  | Value must match id of one of the nodes                                                                                        |
| Hierarchy | `nodeId` from selected `HierarchyNode` | Value must must match id one of the **leaf** nodes                                                                             |

#### Adding custom fields using definitions
To create custom field entry for `Map<String,String>`, which is used to add custom fields via `ChatFieldHandler`,
combine custom field definition `fieldId` with a value matching defined type.

### Accessing current customer custom fields
Immutable collection of customer custom fields is available as field of the Chat instance

```kotlin
val chat = MyChatInstanceProvide.chat ?: return
val customerCustomFields = chat.fields
```

> Note that the instance won't be modified if the custom fields get updated.
> It should be considered as a current snapshot.

> Customer custom fields can get updated after the thread list refresh.

> Note that it will always only contain custom fields which have a valid definition; legacy fields are filtered out.

### Adding customer custom fields
You can append customer custom fields through Chat instance ChatFieldHandler handler.
You may be required to supply such values based on automatic flows attached to your chat channel
configuration.

```kotlin
fun addReferralUnknown() {
  val chat = MyChatInstanceProvide.chat ?: return
  val customerCustomFields = chat.configuration.customerCustomFields.lookup("referral") as? FieldDefinition.Text ?: return
  val customFields = mapOf(customerCustomFields.fieldId to "referral_unknown")
  chat.customFields().add(customFields)
}
```

> Supplying the same key with different value, will overwrite the existing custom field.

> Customer custom fields can be used for creation of an automatic welcome message or other automatic
> events. 
> If you're unsure if these values will be required, you should consult your CXone representative
> or local managers depending on your company structure.

## Global Events

### Analytics events

- **ChatWindowOpenEvent**
  - Specific Chat page or screen (conversation) has been opened
  - Correct reporting of this event is required for "Welcome message" automation.
- **ConversionEvent**
  - The user was redirected from other media (link, etc…), made a purchase, read an article.
    Anything your company has internally defined as a conversion.  Generated by `ChatEventHandlerActions.conversion`
- **CustomVisitorEvent**
  - Any event you may want to track.
- **PageViewEvent**
  - User has visited a page in the host application. Generated by `ChatEventHandlerActions.pageView`.
- **TimeSpentOnPageEvent**
  - User has left a page in the host application and time spent on the page.  Automatically
    generated by `ChatEventHandlerActions.pageViewEnded`.
- **ProactiveActionClickEvent**
  - Action regards to `ChatActionHandler::onPopup`. Generated by `ChatEventHandlerActions.proactiveActionClick`.
- **ProactiveActionDisplayEvent**
  - Action regards to `ChatActionHandler::onPopup`. Generated by `ChatEventHandlerActions.proactiveActionDisplay`.
- **ProactiveActionFailureEvent**
  - Action regards to `ChatActionHandler::onPopup`. Generated by `ChatEventHandlerActions.proactiveActionFailure`.
- **ProactiveActionSuccessEvent**
  - Action regards to `ChatActionHandler::onPopup`. Generated by `ChatEventHandlerActions.proactiveActionSuccess`.
- **TriggerEvent**
  - Trigger an automation event by ID

### Automatic analytics events

- **VisitEvent**
  - Defines the beginning of a visit, which is a sequence of
PageView events.
  - Automatically generated by `ChatEventHandlerActions.viewPage` when there is no
current visit defined or when the last visit has been idle for more
than 30 minutes.
- **TimeSpentOnPageEvent**
  - Reports time spent on the last page in seconds.
  - Automatically generated by `ChatEventHandlerActions.viewPageEnded`.

See analytics case study for information how to implement analytics events [here](cs-analytics.md)

### Authorization events

- **RefreshToken**
  - Event notifying backend that authorization token has to be refreshed.

## Threads

Depending on your configuration, you might be able to create either multiple Threads or you will be
stuck with one.
In any case, the flow is the same, the limitations are described in the code documentation as effects
for some threading methods.

First, you're required to fetch a Threads list.
This is a list of all the threads you can access and have been created by this app's instance.

> ⚠️ Note that when retrieving a `Handler` you don't have to keep the instance since it is internally memoized.
> Some methods may have effects directly on the given handler or parent handlers

```kotlin
val threadsHandler = chat.threads() // (1)
threadsHandler.threads {
  // todo save the threads list
  // update ui
}
```

> Note that we do not encourage a specific pattern as every application's code might be different.
> Use your own expertise to determine how to update the UI and save the list of threads.

> ⚠️ Warning!
> Some listener methods return Cancellable effect.
> You, are required to cancel the effect once it's no longer necessary.

Now that you have saved the list of threads, you have options, depending on whether the
configuration is single or multi-threaded.

### Single Thread

Use a thread list to fetch the first instance in the list OR create a new thread.

> Note that a failure to follow these exact steps might cause an exception. Single Threaded
> instances can have at most one thread.
> Thrown exception can be of type `UnsupportedChannelConfigException` in case you are trying
> to create second thread (or archive current one),
> or it can be of type `MissingThreadListFetchException` when you have called `create` before the chat has signaled `ChatStateListener.onReady()`.

```kotlin
val threads: List<ChatThread> // stored somewhere
val thread = threads.firstOrNull()
val threadHandler = when (thread) {
  null -> threadsHandler.create()
  else -> threadsHandler.thread(thread)
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

> ℹ️
> Note that attempts to create thread without supplying responses to required survey questions will
> cause an exception.

> Response sequence to pre-chat surveys is converted to custom field values and are sent with
> a first thread message.
> Pre-chat survey is a subset of contact custom field definitions.
---

Now that your configuration uses multiple or single threads you have obtained the Thread Handler.
You can furthermore explore what can you with these objects and as long you follow the principles
defined by warnings and notes above, you're generally good to go.

> ❓ If you have experience with browsing and using SDKs on your own, you can skip all the following
> documentation. It's well documented in the code itself and can be used as reference.

> ℹ️
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

### Send a Message with a document

Refer to the [Message States](#Message States) for more info how attachment (document) messages
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

> ⚠️ Warning!
> Note that attachments passed as Uri will be entirely read into memory before being uploaded, so be careful your uploaded files are not too large.

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
 
> ⚠️ Warning!
> Note that such attachments will be stored in memory until they are uploaded to the server.
> Be careful how large files you'll upload.

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
import com.nice.cxonechat.ChatThreadEventHandlerActions.archiveThread

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

#### Processed

All text messages are automatically processed as soon as they are sent
through `ChatThreadMessageHandler::send`. Messages with attachments (documents) are being processed
by sending them to a storage server first. Once successfully stored, they are marked processed.

This state might be beneficial for your users to see indeterminate progressbar as the message "is
being sent".

#### Sent

The Message reaches this state once it successfully leaves this device.
If it doesn't leave this device, then the corresponding callback is never triggered.

#### Received

The Received state is implicit. That means that if the `ChatThreadHandler::get` with callback returns
the message in its list of messages, the message was received successfully by the server.

If your new message is not received within a reasonable amount of time through this callback, offer
your users to resend the message.

#### Read

The agent has read the message and/or acted upon it. This indication is now part of the Message
object received through aforementioned `ChatThreadHandler::get`.

## Manual username update
If you are not using OAuth user authentication and your application allows to change username in
application, you will have to close current instance of chat and create a new instance of chat using
the `ChatBuilder`.
The updated username can be supplied to the builder before chat instance is created.
