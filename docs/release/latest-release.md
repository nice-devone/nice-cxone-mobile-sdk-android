# Version

**4.0.1**

## Status:

**RELEASED**

## Release Notes:

### 🔄 Changed

- **Attachments picked while the chat is unavailable**: `chat-sdk-ui` adds an attachment picked while the chat is `Offline` or its connection is lost only if the chat recovers, and shows no feedback until then. Previously it added the attachment at once, although it could not be sent.

### 🐛 Fixed

- **Thread updates lost before the first collector**: an update such as a new message that arrived before `ChatThreadHandler.threadFlow` was first collected was lost, so neither `threadFlow` nor `ChatThreadHandler.get()` ever showed it. A thread handler now applies updates from shortly after it is created, whether or not it is collected.
- **Thread list kept stale contact data**: `ChatThreadsHandler.threadsFlow` kept a thread's loaded `contactId` and `threadState` after a new-message event changed them, so the thread list disagreed with the open thread. A thread closed by such an event now also shows as closed in the list, until a later thread recovery reopens it or brings a new contact.
- **Duplicate new-message events ignored contact data**: `ChatThread.contactId` and `ChatThread.threadState` were not updated when a new-message event arrived for a message already in the thread.
- **Live chat messages missing from `threadFlow`**: in a live chat conversation started with `ChatThreadsHandler.create()`, the customer's own messages after the first, and agent replies, were added to the thread but often not emitted. A UI rendering `threadFlow` showed them only after the next agent typing indicator, a `refresh()` or a new thread handler.
- **Late first agent reply in a new multi-thread thread**: when a newly created thread held only the customer's first message, the agent's first reply could reach `threadFlow` late or never, so the Chat UI posted no notification for it while another thread was open. Custom fields added to such a thread before its first message now also reach `threadFlow`.
- **Proactive `WelcomeMessage` dropped for a new visitor**: on a new anonymous visitor's first chat open, visitor registration failed until the backend assigned a `customerId`. When the app supplies no `customerId`, the SDK now generates a local one at startup.
- **`chatWindowOpen()` reported before the chat was ready**: `ChatEventHandlerActions.chatWindowOpen()` could be reported before the chat was actually `Ready`, which could cause the backend to drop a proactive `WelcomeMessage` action. The Chat UI now reports it only once `Ready`.

### ⚠️ Known Issues

- **Thread list after a close and a recovery at the same moment**: in a single-thread channel, if the agent closes the conversation at the same moment as a thread recovery, `threadsFlow` can still show the thread as open while `threadFlow` shows it closed, until the thread's state changes again.
- **Thread updates right after a handler is created**: a thread handler applies updates from shortly after it is created, not from the very first moment. An update that arrives within that short window can still be missed until the next update for the thread.
- **Explicit OAuth flow, carried over from 4.0.0**: explicit OAuth flow (`ChatBuilder.setAuthorization(...)`) is not supported by the backend for this SDK version. Use the implicit flow (`setTokenDelegateListener(...)`).
- **Local `customerId` kept for a new OAuth or secured-cookie visitor**: the visitor is registered with the locally generated `customerId` and is not registered again with the real identity after authentication. Before 4.0.1 the registration was retried and used the real identity.