# OAuth Third-Party Authentication

This case study explains how to integrate third-party OAuth authentication (such as Amazon Login) with the CXone Chat SDK, including
handling token expiration and re-authentication flows.

## Table of Contents

1. [Overview](#overview)
2. [Authentication Flow](#authentication-flow)
3. [Initial Setup](#initial-setup)
4. [Token Expiration and Re-Authentication](#token-expiration-and-re-authentication)
5. [Implementation Requirements](#implementation-requirements)
6. [Example: Amazon Login Integration](#example-amazon-login-integration)
7. [Error Handling](#error-handling)

## Overview

The CXone Chat SDK supports third-party OAuth authentication through the `AuthenticationType.ThirdPartyOAuth` mode. This authentication type
is designed for scenarios where user identity is managed by an external OAuth provider (e.g., Amazon Login, Google Sign-In).

### Key Concepts

- **Authorization Code**: A temporary code obtained from the OAuth provider after user authorization
- **Code Verifier**: A PKCE (Proof Key for Code Exchange) component used to secure the authorization flow
- **Transaction Token**: A token issued by the CXone backend after successful OAuth authentication
- **Access Token**: The OAuth provider's access token, managed by the SDK for session validity
- **Refresh Token**: Used to obtain new access tokens when the current one expires

## Authentication Flow

### High-Level Flow Diagram

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐          ┌──────────────┐
│   Client    │         │    OAuth     │         │   CXone     │          │   CXone      │
│     App     │         │   Provider   │         │   Backend   │          │  Chat SDK    │
└──────┬──────┘         └──────┬───────┘         └──────┬──────┘          └──────┬───────┘
       │                       │                        │                        │
       │ 1. Generate Code      │                        │                        │
       │    Verifier &         │                        │                        │
       │    Request Login      │                        │                        │
       ├──────────────────────>│                        │                        │
       │                       │                        │                        │
       │ 2. Auth Code          │                        │                        │
       │<──────────────────────┤                        │                        │
       │                       │                        │                        │
       │ 3. Set Authorization  │                        │                        │
       ├───────────────────────┼────────────────────────┼───────────────────────>│
       │                       │                        │                        │
       │ 4. Connect Chat       │                        │                        │
       ├───────────────────────┼────────────────────────┼───────────────────────>│
       │                       │                        │                        │
       │                       │  5. Exchange Auth Code │                        │
       │                       │     for Transaction    │                        │
       │                       │     Token              │                        │
       │                       │<───────────────────────┤                        │
       │                       │                        │                        │
       │                       │  6. Transaction Token  │                        │
       │                       │     (with Access &     │                        │
       │                       │     Refresh Tokens)    │                        │
       │                       ├───────────────────────>│                        │
       │                       │                        │                        │
       │ 7. WebSocket Connected│                        │                        │
       │<──────────────────────┼────────────────────────┼────────────────────────┤
       │                       │                        │                        │
```

### Detailed Steps

1. **Generate PKCE Parameters**: Application generates code verifier and derives code challenge before initiating OAuth flow
2. **User Initiates Login**: Application triggers OAuth provider login flow with code challenge (not the verifier)
3. **OAuth Provider Returns Authorization Code**: Provider returns authorization code after user authorization
4. **Application Provides Authorization to SDK**: Use `ChatBuilder.setAuthorization()` with authorization code and code verifier
5. **SDK Connects to CXone Backend**: On `chat.connect()`, SDK exchanges authorization code for transaction token
6. **Backend Validates with OAuth Provider**: CXone backend validates the authorization code with the OAuth provider, which verifies the
   code challenge and code verifier match
7. **Transaction Token Issued**: Backend returns transaction token containing:
    - Access token from OAuth provider
    - Refresh token from OAuth provider
    - Token expiration information
    - Customer identity details
8. **WebSocket Connection Established**: SDK uses transaction token to establish secure WebSocket connection

## Initial Setup

### Step 1: Configure ChatBuilder with OAuth

```kotlin
val chatBuilder = ChatBuilder(context, socketFactoryConfiguration)
    .setDevelopmentMode(BuildConfig.DEBUG)
    .setAuthorization(authorization) // OAuth credentials from provider
    .setChatStateListener(chatStateListener)
```

### Step 2: Obtain OAuth Credentials

Your application must implement the OAuth provider's authentication flow to obtain:

- Authorization code
- Code verifier (PKCE)

Example with Amazon Login:

```kotlin
// Import required Amazon Login SDK classes:
// - com.amazon.identity.auth.device.AuthError
// - com.amazon.identity.auth.device.api.authorization.AuthorizationManager
// - com.amazon.identity.auth.device.api.authorization.AuthorizeListener
// - com.amazon.identity.auth.device.api.authorization.ProfileScope
// - com.amazon.identity.auth.device.api.workflow.RequestContext

// Generate PKCE challenge
val (codeVerifier, codeChallenge) = PKCE.generateCodeVerifier()

// Request authorization from Amazon
val requestContext = RequestContext.create(context)
requestContext.registerListener(object : AuthorizeListener {
    override fun onSuccess(result: AuthorizeResult) {
        val authorizationCode = result.authorizationCode

        val authorization = ChatAuthorization(
            code = authorizationCode,
            verifier = codeVerifier
        )

        // Set authorization before connecting
        chatSettingsHandler.setAuthorization(authorization)
    }

    override fun onError(error: AuthError?) {
        // Handle error
    }

    override fun onCancel(cancellation: AuthCancellation?) {
        // Handle cancellation
    }
})

AuthorizationManager.authorize(
    AuthorizeRequest.Builder(requestContext)
        .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
        .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
        .withProofKeyParameters(codeChallenge, "S256")
        .build()
)
```

### Step 3: Build and Connect Chat

```kotlin
chatBuilder.build { result ->
    result
        .onSuccess { chat ->
            // Chat instance created, now connect
            chat.connect()
        }
        .onFailure { throwable ->
            // Handle chat build failure (e.g., log or show error to the user)
        }
}
```

## Token Expiration and Re-Authentication

### Understanding Token Expiration

The transaction token issued by CXone backend has an expiration time. When the token expires or becomes invalid:

**For ThirdPartyOAuth authentication:**

1. When connection is attempted with an expired transaction token stored locally, the SDK detects the failure during connection
   establishment
2. The SDK throws `RuntimeChatException.ConnectionTokenFailed` exception
3. The exception is reported via `ChatStateListener.onChatRuntimeException()`

**For other authentication types:**

1. During connect, the SDK requests a new transaction token from the CXone backend using the current credentials
2. If the transaction token request succeeds, connection proceeds normally
3. If the transaction token request fails, `RuntimeChatException.AuthorizationError` is thrown

The key difference for ThirdPartyOAuth is that the SDK cannot automatically refresh because the authorization code and verifier are not
available to the SDK—they must come from the integrating application.

### Handling ConnectionTokenFailed Exception

When the `ConnectionTokenFailed` exception is raised, the transaction token stored locally has expired and cannot be used. The SDK **does
not have the authorization code and verifier** needed to request a new token from the backend, so re-authentication is required.

The integrating application typically handles this by:

1. Detecting the `ConnectionTokenFailed` exception in `ChatStateListener.onChatRuntimeException()`
2. Updating the UI state to show the OAuth/login dialog to the user
3. Once the user completes OAuth re-authentication:
    - A new authorization code is obtained from the OAuth provider
    - The new authorization is set via `ChatSettingsHandler.setAuthorization()`
    - Chat is reconnected automatically or on user action

**In the sample app** (StoreViewModel):

- When `ConnectionTokenFailed` is caught, the UI state is changed to `OAuth` (if auth is enabled) or `Login` (if user-based auth)
- This triggers the UI to show the login/OAuth dialog
- User re-authenticates through the OAuth provider
- On successful authentication, new authorization is persisted and chat reconnects automatically

### Implementation Example (ViewModel Pattern)

The sample app demonstrates the recommended pattern using a ViewModel with `ChatInstanceProvider.Listener`:

```kotlin
class StoreViewModel(
    application: Application,
    private val chatProvider: ChatInstanceProvider,
    // ...other dependencies
) : AndroidViewModel(application) {

    private val listener = Listener().also(chatProvider::addListener)

    private inner class Listener : ChatInstanceProvider.Listener {

        override fun onChatStateChanged(chatState: ChatState) {
            // Handle state transitions
        }

        override fun onChatRuntimeException(exception: RuntimeChatException) = scope("onChatRuntimeException") {
            if (exception is RuntimeChatException.ConnectionTokenFailed) {
                // Token expired - update UI to show login dialog
                val isAuthorizationEnabled = chatProvider.chat?.configuration?.isAuthorizationEnabled
                val state = currentUiState(this, null, isAuthorizationEnabled)
                if (state != null) {
                    setUiState(state)
                }
            } else {
                error("Chat SDK reported exception.", exception)
            }
        }
    }

    private fun currentUiState(
        loggerScope: LoggerScope,
        settings: ChatSettings?,
        isAuthorizationEnabled: Boolean?,
    ) = when (isAuthorizationEnabled) {
        true -> if (settings?.authorization != null) {
            Prepared  // Chat ready
        } else {
            OAuth()   // Show OAuth login dialog
        }
        false -> if (settings?.userName != null) {
            Prepared  // Chat ready
        } else {
            Login     // Show login dialog
        }
        null -> {
            loggerScope.error("No chat configuration available")
            null
        }
    }

    // User clicks login button
    fun loginWithAmazon(isForced: Boolean = false) {
        // Trigger OAuth flow via Activity/UI
        uiStateStore.value = showLoginDialog(isForced)
    }
}
```

In this pattern:

- When `ConnectionTokenFailed` occurs, the UI state changes to show the login dialog
- User interacts with the OAuth provider through the Activity
- New authorization is obtained and persisted
- Chat automatically reconnects on next `connect()` call

### Re-Authentication Flow

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐
│   Client    │         │    OAuth     │         │  Chat SDK    │
│     App     │         │   Provider   │         │              │
└──────┬──────┘         └──────┬───────┘         └──────┬───────┘
       │                       │                        │
       │ Token Expired         │                        │
       │ Exception Received    │                        │
       │<──────────────────────┼────────────────────────┤
       │                       │                        │
       │ 1. Update UI State    │                        │
       │    (Show Login Dialog)│                        │
       │                       │                        │
       │ 2. Request Re-Login   │                        │
       ├──────────────────────>│                        │
       │                       │                        │
       │ 3. New Auth Code +    │                        │
       │    Code Verifier      │                        │
       │<──────────────────────┤                        │
       │                       │                        │
       │ 4. Set New Authorization                       │
       ├───────────────────────┼───────────────────────>│
       │                       │                        │
       │ 5. Connect Chat       │                        │
       ├───────────────────────┼───────────────────────>│
       │                       │                        │
       │ 6. New Session Established                     │
       │<──────────────────────┼────────────────────────┤
       │                       │                        │
```

## Implementation Requirements

### For Integrating Applications

The integrating application **must** implement the following:

#### 1. OAuth Provider Integration

- Implement the OAuth provider's SDK (e.g., Amazon Login SDK)
- Handle authorization request and response
- Generate PKCE code verifier and challenge
- Extract authorization code from OAuth response

#### 2. Token Expiration Handling

- Implement `ChatInstanceProvider.Listener.onChatRuntimeException()`
- Detect `RuntimeChatException.ConnectionTokenFailed`
- Update UI state to show login/OAuth dialog
- Re-initiate OAuth flow
- Obtain new authorization credentials
- Persist new authorization via `ChatSettingsHandler.setAuthorization()`
- After the new token is persisted, call `ChatActivity.startChat(activity)` to reconnect the chat session **(sample app implementation; if not using `ChatActivity`, reconnect your chat session as appropriate for your integration)**

#### 3. State Management

- Persist OAuth provider state across app lifecycle (if needed)
- Handle re-authentication during Activity/Fragment lifecycle events
- Re-register OAuth listeners when returning to the activity

#### 4. User Experience

- Provide clear messaging when re-authentication is required
- Handle user cancellation of re-authentication
- Gracefully degrade functionality if user declines to re-authenticate

### For CXone Backend

The backend must be configured to:

- Support the specific OAuth provider
- Validate authorization codes
- Exchange codes for access/refresh tokens
- Issue transaction tokens with appropriate expiration

## Example: Amazon Login Integration

This example demonstrates a complete integration with Amazon Login, including re-authentication handling.

### Activity Implementation

```kotlin
class StoreActivity : AppCompatActivity(), UiStateContext {

    private lateinit var requestContext: RequestContext
    private val storeViewModel: StoreViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Amazon request context
        requestContext = RequestContext.create(this as Context)
    }

    override fun onResume() {
        super.onResume()
        requestContext.onResume()
    }

    override fun onPause() {
        super.onPause()
        requestContext.onPause()
    }

    override fun loginWithAmazon(isForced: Boolean) = logger.scope("loginWithAmazon") {
        val (codeVerifier, codeChallenge) = PKCE.generateCodeVerifier()

        requestContext.registerListener(LoggingAuthorizeListener(codeVerifier, storeViewModel, isForced, logger))

        AuthorizationManager.authorize(
            AuthorizeRequest.Builder(requestContext)
                .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
                .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                .withProofKeyParameters(codeChallenge, "S256")
                .build()
        )
    }

    private inner class LoggingAuthorizeListener(
        private val codeVerifier: String,
        private val storeViewModel: StoreViewModel,
        private val isForced: Boolean,
        logger: Logger,
    ) : AuthorizeListener(), LoggerScope by LoggerScope<AuthorizeListener>(logger) {

        override fun onSuccess(result: AuthorizeResult?) = scope("onSuccess") {
            result?.authorizationCode?.let { authorizationCode ->
                storeViewModel.chatSettingsHandler.setAuthorization(
                    authorization = ChatAuthorization(code = authorizationCode, verifier = codeVerifier),
                    onPersisted = {
                        if (isForced) ChatActivity.startChat(this@StoreActivity)
                    }
                )
            } ?: error("loginWithAmazon success with no result")
        }

        override fun onError(authError: AuthError?) = scope("onError") {
            info("LoginWithAmazon: ${authError?.message ?: getString(string.unknown_error)}")
        }

        override fun onCancel(authCancellation: AuthCancellation?) = scope("onCancel") {
            info("LoginWithAmazon: ${authCancellation?.description}")
        }
    }
}
```

### PKCE Helper

```kotlin
object PKCE {
    /**
     * Generate a code verifier and code challenge for PKCE flow.
     *
     * @return Pair of (codeVerifier, codeChallenge)
     */
    fun generateCodeVerifier(): Pair<String, String> {
        val secureRandom = SecureRandom()
        val codeVerifier = ByteArray(32).also { secureRandom.nextBytes(it) }
            .let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }

        val codeChallenge = MessageDigest.getInstance("SHA-256")
            .digest(codeVerifier.toByteArray())
            .let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) }

        return Pair(codeVerifier, codeChallenge)
    }
}
```

## Error Handling

### Exception Types

The SDK may raise the following exceptions during OAuth authentication:

#### RuntimeChatException.ConnectionTokenFailed

- **Cause**: Transaction token is expired (locally) and cannot be automatically refreshed (ThirdPartyOAuth only)
- **Action Required**: Re-authenticate with OAuth provider
- **Recovery**:
    1. Update UI state to show login/OAuth dialog
    2. Initiate OAuth flow to obtain new credentials
    3. Set new authorization via `ChatSettingsHandler.setAuthorization()`
    4. Chat reconnects automatically with new credentials

#### RuntimeChatException.AuthorizationError

- **Cause**: Authorization failed
- **Action Required**: Check OAuth configuration and credentials
- **Recovery**:
    1. Verify OAuth provider setup
    2. Check authorization code validity
    3. Ensure backend is configured correctly
    4. Retry authentication

### Best Practices

1. **Always Handle ConnectionTokenFailed**: This exception is specific to token expiration and requires user interaction
2. **Use ViewModel Pattern**: Implement token expiration handling in a ViewModel with `ChatInstanceProvider.Listener` for proper lifecycle
   management
3. **Update UI State**: When token expires, update UI state to show login/OAuth dialog rather than imperatively calling methods
4. **Provide User Feedback**: Clearly communicate to users when re-authentication is required
5. **Log OAuth Errors**: Log detailed error information for debugging
6. **Test Token Expiration**: Verify your re-authentication flow works correctly

### Error Handling Flow Chart

```
┌─────────────────────────────────┐
│      Exception Received         │
└───────────────┬─────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│    Is ConnectionTokenFailed?    │
└───┬─────────────────────────┬───┘
    │ Yes                     │ No
    ▼                         ▼
┌───────────────────────┐   ┌──────────────────┐
│  Update UI State      │   │  Check Exception │
│  Show Login Dialog    │   │  Type & Handle   │
└───────────────────────┘   └──────────────────┘
```

## Summary

### Key Takeaways

1. **Third-Party OAuth** requires the integrating application to manage the OAuth flow
2. **Transaction tokens expire** and cannot be automatically refreshed for ThirdPartyOAuth
3. **ConnectionTokenFailed exception** signals that re-authentication is required
4. **Handle via UI state**: Update UI to show login/OAuth dialog
5. **Integrators must implement** re-authentication flow, the SDK cannot do this automatically
6. **State management** across Activity/Fragment lifecycle is important

### Responsibilities

| Component           | Responsibility                                                      |
|---------------------|---------------------------------------------------------------------|
| **Integrating App** | OAuth provider integration, re-authentication flow, user experience |
| **Chat SDK**        | Token exchange, WebSocket management, exception signaling           |
| **CXone Backend**   | OAuth validation, transaction token issuance, session management    |
| **OAuth Provider**  | User authentication, authorization code issuance, token validation  |

### Next Steps

- Review the [Amazon Login sample implementation](../../store/src/main/java/com/nice/cxonechat/sample/StoreActivity.kt)
- Implement `ChatInstanceProvider.Listener.onChatRuntimeException()` in your ViewModel
- Test re-authentication flow with expired tokens
- Ensure proper error handling and user messaging
