# Case Study: Analytics

CXone backend provides WFA (WorkFlow automation) functionality which relies on Chat SDK reporting of analytic events which serve as triggers for the automation.
More information about WFA can be found in the [CXone documentation](https://help.nice-incontact.com/content/acd/digital/chat/workflowautomation.htm).

## Supported Analytics events

- **ChatWindowOpenEvent**
  - Specific Chat screen (conversation) has been opened
  - Correct reporting of this event is required for "Welcome message" automation.
- **ConversionEvent**
  - The user was redirected from other media (link, etc…), made a purchase, read an article.
    Anything your company has internally defined as a conversion.
- **CustomVisitorEvent**
  - Any event you may want to track
- **PageViewEvent**
  - User has visited a page or screen in the host application.  The precise definition of page or screen is up to the implementer, but should be fine-grained enough for analytics purposes.
    A PageView event must be generated as each page is entered.  
- **PageViewEndedEvent**
  - The user is leaving a page or screen previously recorded with a Page View Event.
    A Page View Ended event should be generated as each page is left.
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

## Automatic analytics events

- **VisitEvent**
  - PageView event was reported, which starts the 'Visit' session. The tracked session ends within 30 minutes of inactivity.
- **TimeSpentOnPageEvent**
  - Event is generated when new PageViewEvent when a PageViewEndedEvent is reported.
  - Reports time spent on the last page in seconds.

To record analytics events use `com.nice.cxonechat.ChatEventHandlerActions` extension functions for `ChatEventHandler`.

Sample:

```kotlin
class ChatViewModel : ViewModel() {

  private val chat = ChatInstanceProvider.get().chat
  private val events = chat.events()

  /**
   * Called when the chat window is opened.
   */
  internal fun reportOnResume() {
    events.chatWindowOpen()
  }
}
```

## Tracking Customer

### Flow

Events `events.pageView()` and `events.pageViewEnded()` can help you with tracking customer visits within your application.
Also, these events are used for automatic reporting of time spent on page by the user.

> [!IMPORTANT]
> Integrator must handle entering background on its own, the SDK does not handle this behavior.
> Implement lifecycle observer which will report `pageView()` event for `Lifecycle.Event.ON_START` and `pageViewEnded()`
> for `Lifecycle.Event.ON_STOP`.

> [!IMPORTANT]
> Thread list, chat transcript, etc. should not be generating page view events. For tracing chatting with the agent,
> the SDK includes the `chatWindowOpen()` method.

### ProActive reactions

Proactive events are used for evaluation of user flow when they are presented with proactive action.
When the proactive action is presented to the user, the integration should report `events.proactiveActionDisplay(action.metadata)`
and when user interacts with it the application should report `events.proactiveActionClick(action.metadata)`.
Reporting of success and failure is left to interpretation of integrators.

In current version the SDK only supports Popup Box which requires the integration to implement `ChatActionHandler.OnPopupActionListener` interface and register it via the `ChatActionHandler.onPopup` method.
Other details can be found in the [Chat SDK documentation](https://help.nice-incontact.com/content/acd/digital/guide/guideactions/mobileapplicationpopupbox.htm?tocpath=CXone%20Guide%7CCXone%20Guide%7CCreate%20Engagement%20Rules%7CLegacy%20Engagement%20Actions%7C_____6#MobileApplicationPopupBox).
