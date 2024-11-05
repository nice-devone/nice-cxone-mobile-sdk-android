# Case Study: Logging

This CS describes how to set up logging for the Chat SDK and details about the logging library
in case the integrating application would want to use it itself.

## Quick Android console logging

> [!WARNING]
> The Chat SDK at the moment performs only minimal redaction of sensitive data in the logs.
> Application should always assume that the log messages may contain sensitive data and that those messages shouldn't be
> shared in publicly accessible log channels (e.g. Android console aka logcat) or that they need to be redacted before sharing!
> 
> For this reason the Chat SDK is using the `LoggerNoop` logger by default, which doesn't log anything.

The Chat SDK already depends on the API artifact of the Logging library (com.nice.cxone:logger),
so only the thing which has to be done is to add a dependency on the Android implementation:

```groovy
dependencies {
    implementation "com.nice.cxone:logger-android:latest"
}
```

and pass the instance of the `LoggerAndroid` to the `ChatSdk`:

```kotlin
    fun createChatInstance() : ChatInstanceProvider {
        return ChatInstanceProvider.create(
            configuration = getSdkConfiguration(),
            authorization = getSdkAuthorization(),
            userName = getUserName(),
            developmentMode = false, // or true during development when you need to see internal SDK logs
            logger = LoggerAndroid("CXoneChat")
        )
    }
```
### Development mode
The `developmentMode` parameter is used to enable or disable method logging in the SDK.
If the mode is disabled only the websocket messages are logged.

## Logging to multiple destinations
For this purpose you can use `ProxyLogger` class which allows you to log messages to multiple loggers.
```kotlin
    fun createChatInstance() : ChatInstanceProvider {
        return ChatInstanceProvider.create(
            ..., // Other required parameters are omitted for brevity
            logger = ProxyLogger(
                LoggerAndroid("CXoneChat"),
                FileLogger("CXoneChat-log.txt")
            )
        )
    }
```
This can be useful for example when you want to log error messages from the SDK to your reporting system.

## Detailed description  of the Logging library

### Logger 

This very simple library provides a simple logging interface that is used by the Chat SDK
to log messages.
It doesn't depend on Android and can be used in any Kotlin (or Java) project.

It can be added to your project by adding the following dependency:

```groovy
dependencies {
    implementation "com.nice.cxone:logger:latest"
}
```

#### Logger interface

Core of this library is the `Logger` interface:

```kotlin
interface Logger {
    fun log(level: Level, message: String, throwable: Throwable? = null)
}
```
The library provides default no-op singleton (**object**) implementation of this interface called `LoggerNoop`. 

#### LoggerScope

Atop of this interface, there is a `LoggerScope` which provides scope metadata for the logged messages,
with it's default (private) implementation `NamedScope` which prefixes all messages with the scope name
e.g.: `NamedScope` with name `ChatSdk` would log message `connect()` as `[ChatSDK] connect()`

The `LoggerScope` also provides two extension functions for creating sub-scopes:
* `LoggerScope.scope(name: String, body: LoggerScope.() -> T)` - creates a new scope which combines the sub-scope name with parent e.g.: `parentScope/subScope`
* `LoggerScope.timesScope(name: String, body: () -> T): T` - does the same as `scope` but also measures the time spent executing the `body` lambda


#### Extension methods
These methods can be used to conveniently log messages with different levels:

```kotlin
class MyClass(val logger: Logger) {

    fun exampleMethod() {
        logger.verbose("This is a verbose level message")
        logger.debug("This is a debug level message")
        logger.info("This is an info level message")
        logger.warn("This is a warning level message")
        logger.error("This is an error level message")
    }
}
```

#### Level
The `Level` is a sealed class with 6 predefined levels for logging and a custom level which
can be used for a fine-tuned logging.
The `Level` implements `Comparable<Level>` so it can be used for easy filtering of log messages even
with the custom levels.

#### ProxyLogger
The `ProxyLogger` is a wrapping logger implementation which allows you to log messages to multiple loggers,
it doesn't produce any output on its own.

### LoggerAndroid
This library provides a simple implementation `Logger` interface which logs messages to the Android console (logcat)
with appropriate log level and automatically chunks large messages to avoid the 4k limit of the logcat.
It also logs any exceptions passed to the `log` method.
