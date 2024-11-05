# CXone Chat for Android

This repository consists out of three main modules and three tooling modules,
which are described in chapters below.

## CXOne Chat SDK

This is the only published module, it is released as android multi-flavor library with maven artifact coordinates
`com.nice.cxone:chat-core`.

### Adding the dependency

If you want to use published artifact you will have to include our public maven/gradle github repository into your project.
For details
see [Github documentation on using published packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package).

In section where you define repositories used in your project (in our case it is settings.gradle) add this part:

```groovy
    def localProperties = new Properties()
    try {
        localProperties.load(file("local.properties").newReader())
    } catch (ignored) {
        logger.trace("Unable to read local.properties")
    }
    repositories {
        maven {
            name = "github-nice-devone-cxone-mobile"
            url = "https://maven.pkg.github.com/nice-devone/nice-cxone-mobile-sdk-android"
            credentials {
                // Use property key from local.properties for local builds or environment variable for CI builds
                username = localProperties["github.user"] ?: System.getenv("GPR_USERNAME")
                password = localProperties["github.key"] ?: System.getenv("GPR_TOKEN")
            }
        }
    }
```

Then either add properties `github.user` and `github.key` to your local.properties
e.g.:

```
github.user=myuser
github.key=github_my_token
```

or set system variables `GPR_USERNAME` and `GPR_TOKEN`.
You can use any uprivilidged valid token, since the package are public.

Then you can the dependency simply by adding:

```groovy
    implementation "com.nice.cxone:chat-sdk-core:$currentVersion"
```

### Additional information

Visit [NICE documentation][NICE-docs] for more information about CXone Chat and pre-requisites for the SDK.

Current [API][API].

You can also find a simplified example of possible SDK usage in [case studies](docs/case-studies.md)
documentation.

We offer a brief how-to guide for integration [here][implementation].

[NICE-docs]: https://help.nice-incontact.com/content/acd/digital/mobilesdk/cxonemobilesdk.htm
[API]: https://nice-devone.github.io/nice-cxone-mobile-sdk-android/
[API-1.3]: https://help.nice-incontact.com/mobilesdk/Android1.3/dist/index.html
[implementation]: docs/implementation.md

## CXone Chat UI

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

## Tooling modules:

### logger

The logger module is a minimalistic logging framework used by the chat-sdk-core without any platform-specific code.
It is distributed as a java library at the moment.

The maven artifact coordinates are `com.nice.cxone:logger`.

### logger-android

The logger-android module provides the default android-specific implementation of the Logger.
Application can provide this instance to the SDK builder if it wishes the SDK to log to the Android
platform log output.

The maven artifact coordinates are `com.nice.cxone:logger-android`.

### utilities

This is an internal module that attempts to resolve some of the issues reported by the strict mode. 
