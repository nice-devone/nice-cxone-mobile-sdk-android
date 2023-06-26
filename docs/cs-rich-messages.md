# Rich messages

## About
Rich messaging is an optional part of Chat SDK. They are used to deliver rich message content to the user,
beyond the capabilities of a simple message or message with an attachment.

### Message types
The Chat SDK supports two general types of rich messages
1. TORM messages — a set of message components shared in DTO across multiple channels (Facebook, WhatsApp, Chat)
2. Plugin messages — a legacy rich message type which is built using common components

### Postback
Rich messages may contain a call-to-action (or action for short), which may contain
an optional `postback` value.
In such cases, interacting with the action should produce a reply message on behalf of the user.
The response must be a message which contains the label and post back value from the activated action.
The reason for this is that the postback is used by automation (triggers/bots) to recognize the specific choice from the rich message from
an ordinary customer message with content matching the label of possible action in a rich message.

> Warning: If you don't provide plugin/TORM `postback` value, the chat-bot integration may not work correctly!

## TORM - Truly Omnichannel Rich Messaging
* Quick Reply - Message with a list of actions. Only one action can be selected from provided options, and once selected it should be made inactive. It is the responsibility of integrating application to enforce this constraint.
* List Picker - Message with title, body and list of actions (possibly with images). Any action from the list can be selected multiple times.
* Rich Link - Message with title, optional image and Uri link. The link can be a deeplink or an ordinary url.

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
    ...
  }
}
```

## Plugin
Plugin message is created from one of the elements which can contain one or more sub-elements. For complete details
see class `PluginElement`.

* Elements
  * Gallery
  * Menu
  * Text and Buttons
  * Quick Replies 
  * Inactivity Popup 
  * Satisfaction Survey 
  * Custom

* Sub Elements
    * Text
    * Button
    * File
    * Title
    * Subtitle

Only the `Button` sub-element (ignoring the `Custom` element) contains information for user interaction, so the postback
should be reported only for this element.

```kotlin
/**
 * Quick Reply messages have a [title] to present to the user along with
 * a selection of quick response [Button].  The buttons should also be
 * presented to the user and if the user taps a button, the associated
 * postback should be sent via [OutboundMessage].
 * Future interaction with the Quick Reply message must be disabled upon action.
 */
@Public
abstract class QuickReplies : Message() {
  /** list of actions to display along with [title]. */
  abstract val actions: Iterable<Action>
  ...
}

/**
 * Button component. Buttons should report [postback]s when clicking the
 * button. Postback might contain a [deepLink] which is extracted for
 * convenience.
 *
 * @see CustomVisitorEvent
 * */
@Public
abstract class Button : PluginElement() {
  /** Text to display on a button.  */
  val text: String
  /**
   * Metadata associated with the button which should be returned to the server
   * once the button is pressed.
   * Send a [OutboundMessage] message with button [text] and the [postback] values.
   */
  abstract val postback: String?
  ...
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

  ...

  fun send(text: String, postback: String) {
    handlerMessage.send(OutboundMessage(text, postback), sentListener)
  }
}
```
