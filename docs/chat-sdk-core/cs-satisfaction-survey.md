# Case Study: Satisfaction Survey

> [!IMPORTANT]
> Usage of this case study is intended for scenarios where you are not using the provided UI module artifact.
> If you are using the UI module, the Satisfaction Survey popup is already presented automatically, and this
> case study is only provided for educational purposes.

## About

A Satisfaction Survey is a backend-controlled message that the SDK surfaces to the application as a
`Popup.SatisfactionSurvey`:

```kotlin
interface SatisfactionSurvey : Popup {
    /** The prompt text to display alongside the survey. */
    val text: String

    /** The HTTPS address of the survey page to load in a WebView. */
    val surveyUrl: String
}
```

The survey page itself is generated and controlled by the backend, and it owns response collection and
submission:

- The SDK does not read survey answers.
- The SDK does not add a JavaScript-to-native bridge.
- The SDK cannot detect submission — there is no callback for "the user submitted the survey".
- The SDK does not automatically dismiss the popup. The integrating application must provide an explicit
  close/dismiss control; the survey is otherwise shown until dismissed.
- No analytics event is sent for displaying or interacting with the survey.
- An un-dismissed, on-screen survey keeps showing across a configuration change only if the integrating
  application keeps its own UI state across it (the UI module does, through its `ViewModel`). The SDK
  itself does not persist it: the survey was already marked delivered the moment it was emitted (see
  [Delivery semantics](#delivery-semantics--at-most-once-per-thread)), so after process death it is **not**
  shown again.

Delivery is mode-agnostic: a Satisfaction Survey can be delivered in a `SingleThread`, `MultiThread`, or
`LiveChat` session alike. This is unlike `Popup.InactivityPopup`, which only applies to `LiveChat`.

## Receiving the popup

Both `InactivityPopup` and `SatisfactionSurvey` are delivered through the same
`ChatThreadActionHandler.popupFlow`:

```kotlin
chatThreadHandler.actions().popupFlow
    .onEach { popup ->
        when (popup) {
            is Popup.SatisfactionSurvey -> showSurveyUi(popup.text, popup.surveyUrl)
            is Popup.InactivityPopup -> showInactivityUi(popup)
        }
    }
    .launchIn(viewModelScope)
```

`popupFlow` is a cold flow: collecting it establishes the subscription, and cancelling the collection stops
it. There is no separate close()/release step, and the handler does not implement `AutoCloseable`.

## Delivery semantics — at most once per thread

A Satisfaction Survey is emitted to `popupFlow` in one of two situations:

1. **Live** — a message carrying the survey arrives for the thread while a collector is active.
2. **Recovery** — the survey was sent while no `popupFlow` collector was active for the thread, whether
   because the app was disconnected (closed, or the socket dropped) or because the app stayed connected
   but nothing was collecting `popupFlow` yet. Each new collection of `popupFlow` actively requests a
   fresh recovery for exactly this reason, so an uncollected live survey is not necessarily unrecoverable.
   The recovered survey is carried by the thread-recovery response the SDK requests: Live Chat recovery
   for `LiveChat`, thread recovery for `SingleThread`/`MultiThread`.

   The "newest event" rule differs by mode: in `LiveChat`, a recovered survey is resurfaced only if it is
   the newest message in the thread, so a later plain message supersedes it. In `SingleThread`/
   `MultiThread`, it is resurfaced if it is the newest *popup* message, even when later plain messages
   exist. Unlike a recovered `InactivityPopup` (which is only resurfaced while the thread can still accept
   messages), a recovered survey is **not** gated on the thread's open/closed state: a survey is sent
   precisely when a conversation ends, so by the time it is recovered the thread is normally already
   archived.

> [!WARNING]
> "Delivered" means the SDK emitted the event to a `popupFlow` collector. This is recorded, keyed by thread
> id, at the moment of emission — there is no confirmation call from the application. An integrating
> application that receives the event and does not present the survey loses it: the SDK will not emit it
> again for that thread while its id is still tracked (see "Bounded" below). This is a deliberate, accepted
> limitation.

Delivery tracking:

- Tracked per thread, not per survey message.
- The delivery mark is saved to storage asynchronously, so if the app process is killed right after
  delivery, the survey can be delivered again after restart.
- Local to the device, and cleared along with the rest of SDK storage on sign-out. As a consequence, the
  **same survey can be delivered again on a second device**, or again on the same device after a
  sign-out/sign-in.
- Bounded — the SDK remembers at most the 50 most recently delivered thread ids; the oldest is evicted first
  once that limit is reached. A thread whose id has since been evicted is no longer tracked, so a later live
  or recovery event for it can deliver its survey again.
- Collect `popupFlow` once per thread and route each popup type from that single collector (as the snippet
  above does), rather than collecting it more than once. The delivery mark is set while producing the flow's
  values, before any downstream filtering, so an additional, uncoordinated collector can win the delivery
  check and then discard the survey (e.g. by filtering for only `InactivityPopup`) — losing it for every
  other collector just as surely as not presenting it would. Avoid replaying the collected values (e.g. via
  `shareIn(..., replay = 1)`) to late subscribers for this same reason: a replay happens downstream of the
  delivery mark, so a lifecycle-aware subscriber that resubscribes later would receive an already-dismissed
  survey again.

## Payload and validation

The backend sends the survey as a plugin message containing at least two elements, where the first is a text
element (the prompt) and the second is an iframe-button element whose URL is the survey page; any further
elements are ignored. The button's own label, if present, is not surfaced as part of the public
`SatisfactionSurvey` contract.

`surveyUrl` is validated to be a well-formed HTTPS URL. A malformed survey message — an unexpected element
structure, or a non-HTTPS URL — is dropped (logged, not thrown to the collector) and never terminates the
`popupFlow` collection, nor affects regular messages. In `SingleThread`/`MultiThread` recovery specifically,
a malformed survey can still prevent a valid, newer survey in the same recovered batch from being emitted
(`LiveChat` recovery only ever considers the newest message, so it is unaffected).

## UI module behavior

When using the UI module, a `Popup.SatisfactionSurvey` is presented as a modal bottom sheet containing the
prompt text, the survey page in a WebView, and a single explicit dismiss control — there is no
swipe-to-dismiss and no back-button dismiss, so the control is the only way to leave the survey.

The WebView loading `surveyUrl` applies the following constraints:

- HTTPS only for a GET-style top-level navigation. A POST-based (e.g. form submit) navigation is best-effort
  only — Android provides no reliable pre-flight hook to block a POST navigation before it is sent.
- A top-level navigation to a different origin is blocked only when caused by an actual user gesture
  (a link or button tap). A programmatic cross-origin navigation — e.g. an auth/session redirect — is let
  through, since survey providers routinely bounce through one as part of their normal load.
- Cross-origin iframes are never intercepted, since survey widgets commonly embed their real content this
  way.
- Camera and microphone permission requests made by the loaded page are denied.
- New windows/tabs (`target="_blank"`, `window.open()`) are not supported, and are blocked outright.
- WebView storage (cookies, cache) is used as-is; the SDK does not manage, isolate, or clear it.

Additionally:

- In `LiveChat`, if the thread becomes archived while the survey is on screen, the End Conversation
  experience that would otherwise be shown automatically is held back until the survey is dismissed.
- A survey recovered for a previously selected thread holds back a new thread's pre-chat survey from being
  shown until the recovered survey has been dismissed, so one does not silently cover the other.

## Support and Further Reading

- [Live Chat case study](cs-live-chat.md)
- [Multi Thread Chat case study](cs-multi-thread.md)
- [UI Module Integration case study](../chat-sdk-ui/cs-ui-integration.md)
