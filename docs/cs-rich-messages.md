# Rich messages

## About
Rich messaging is an optional part of Chat SDK. They are used to deliver rich message content to the user,
beyond the capabilities of a simple message or message with an attachment.

### Postback
Rich messages may contain a call-to-action (or action for short), which may contain
an optional `postback` value.
In such cases, interacting with the action should produce a reply message on behalf of the user.
The response must be a message which contains the label and post back value from the activated action.
The reason for this is that the postback is used by automation (triggers/bots) to recognize the specific choice from the rich message from
an ordinary customer message with content matching the label of possible action in a rich message.

> [!WARNING]
> If you don't provide TORM `postback` value, the chat-bot integration may not work correctly!

## TORM - Truly Omnichannel Rich Messaging
A set of message components shared in DTO across multiple channels (Facebook, WhatsApp, Chat).

### Supported TORM types:

* Quick Reply
  - Message with a list of actions. Only one action can be selected from provided options, and once selected it should be made inactive. It is the responsibility of integrating application to enforce this constraint.
  - Contains:
    - title
    - list of actions
* List Picker
  - Message with title, body and list of actions (possibly with images). Any action from the list can be selected multiple times.
  - Contains:
    - title
    - text
    - list of actions
* Rich Link
  - Message with title, optional image and Uri link. The link can be a deeplink or an ordinary url.
  - Contains:
    - title
    - Media object
    - url link

All of the TORM message contain fields common for all message types (text messages and attachment messages):
- id
- threadId
- createdAt
- direction
- _optional_ author
- _optional_ list of attachments
- _optional_ fallbackText

```kotlin
/**
 * Actionable item to display as part of a message.
 */
@Public
interface Action {
  /**
   * Display a button which *may* have an associated icon.
   * If the button is selected integrating application should send [OutboundMessage]
   * with the supplied [text] and [postback].
   */
  @Public
  interface ReplyButton : Action {
    /** Postback to be sent as part of the [OutboundMessage] if the button is selected. */
    val postback: String?
    // ...
  }
}
```

## Sending postback
Postback should be sent as part of standard message from the user, using optional parameter of the `OutboundMessage` object.

```kotlin
class ChatAllConversationViewModel(
  thread: ChatThread,
) : ViewModel() {
  private val chat = ChatInstanceProvider.get().chat.let(::requireNotNull)
  private val handlerThreads = chat.threads()
  private val handlerThread = handlerThreads.thread(thread)
  private val handlerMessage = handlerThread.messages()

  // ...

  fun send(text: String, postback: String) {
    handlerMessage.send(OutboundMessage(text, postback), sentListener)
  }
}
```
