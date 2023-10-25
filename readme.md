# CXone Chat for Android

This repository consists out of three main modules.

## CXOne Chat SDK

This is the only published module, it is released as android multi-flavor library with maven artifact coordinates
`com.nice.cxone:chat-core`.

### Adding the dependency
If you want to use published artifact you will have to include our public maven/gradle github repository into your project.
For details see [Github documentation on using published packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package).

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
                username = localProperties["github.user"] ?: System.getenv("GPR_USERNAME") // Use github.user property key from local.properties for local builds or environment variable GPR_USERNAME for CI builds
                password = localProperties["github.key"] ?: System.getenv("GPR_TOKEN") // Use github.key property key from local.properties for local builds or environment variable GPR_USERNAME for CI builds
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
    implementation "com.nice.cxone:chat-sdk-core:1.2.0"
```
### Additional information
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
