# CXone Chat for Android

This repository consists out of three main modules.

## CXOne Chat SDK

This is the only published module, it is released as android multi-flavor library with maven artifact coordinates
`com.nice.cxone:chat-core`.

Visit [documentation][docs] for more information about SDK API.

You can also find a simplified example of possible SDK usage in [case studies](docs/case-studies.md)
documentation.

[docs]: https://help.nice-incontact.com/content/acd/digital/mobilesdk/android/getstartedandroid.htm

## CXOne Chat UI

This is a sample implementation of the UI for CXOne Chat SDK, which allows easier integration of SDK into
the intended target application.

### Chat UI provides these features:

* Display the thread list (for multi-thread SDK configuration)
  * Archive thread
  * Start a new thread
* Display the thread conversation (including implementation for all currently supported message formats)
  * Send message
  * Upload attachment
  * Record & play voice messages
  * Name the thread (for multi-thread SDK configuration)
  * Message pagination support
  * Typing indication & reporting
* Filling out of pre-contact form
* Display notification about the new message when application is in the background
* Sharing of message attachments
* Fullscreen previews of images & videos

## Store application

This is a mock application that tries to imitate e-store with its purchase flow, so we can
also demonstrate an integration of the SDK analytics events like pageView or conversion.
