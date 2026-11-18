# Case Study: Setting Customer ID
> [!CAUTION]
> Incorrect implementation/usage of this feature may be abused to extract sensitive
> information about the user. Make sure to follow the best practices and guidelines if
> it's usage is required.

## About:
CXone in default mode is generating random UUID as customer id (and persisting it
until logout or until backend overwrites it).
However, in some cases, you might want to set your own customer id 
(e.g. to allow switching between multiple accounts without implementing oAuth).

## How to use:
To set your own customer id, you just need to specify it either in the `ChatBuilder`
e.g:
```kotlin
// ChatBuilder(...) and build() are both suspend functions — call them from a coroutine.
scope.launch {
    val chat = ChatBuilder(context, config)
        // other setup steps
        .setCustomerId("myCustomerId")
        .build()
}
```
or during the configuration step
e.g.:
```kotlin
// configure() is a suspend function — call it from a coroutine.
scope.launch {
    ChatInstanceProvider.get().configure(context) {
        // other setup steps
        customerId = "myCustomerId"
    }
}
```

`ChatInstanceProvider.customerId` is read-only; it can only be set inside the `configure` lambda, which receives a
`ConfigurationScope`.

From Java, use the wrappers in `com.nice.cxonechat.api.ChatInstanceProviderJavaInterop` — `configureAsync(...)` for a
callback-based call, or `configureBlocking(...)` when blocking the calling thread is acceptable.

`ChatInstanceProvider` will deliver a new instance of `Chat` to the registered `ChatInstanceProvider.Listener`s,
or it will be available via `ChatInstanceProvider.get().chat` field once it is prepared.

If the customer id is set to `null` the SDK will generate random customer id as usual.

## Best Practices:
 - Customer ID setting can be used only together with `Authorization.None`
 - Customer ID should not be predictable or easily guessable.
 - Customer ID should be stored securely
 - If possible prefer OAuth or other secure authentication methods over setting customer id.
