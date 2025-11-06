# Case Study: UI Module Integration

The CXone Chat UI module provides a ready-to-use chat interface for Android applications using Jetpack Compose. It is designed for use with
the CXone Chat SDK core module, which must be initialized before using the UI module. This guide assumes the core module is already
integrated and configured in your application.

The UI module handles all UI aspects of the chat experience, including:

* Thread management (in multi-thread messaging channels)
* Pre-chat surveys
* Live chat flow (in live-chat channels)
* Rich content messages (for supported message types in the SDK)
* Permission requests (notifications, audio recording for audio messages)
* File attachment handling (filtering, validation, direct photo/video capture, sharing)
* Audio messages
* Push notifications (remote and local)

> [!NOTE]
> Not all features are available in every channel type. Refer to the official documentation for channel-specific capabilities.

## Prerequisites

- Properly set up your CXone brand and channel configuration for Mobile SDK usage.
- Configured and initialized the CXone Chat SDK core module in your application.
- Jetpack Compose BOM 2025.10.00 (included as transitive dependency with the UI module).
- Added the CXOne Chat UI package to your project.
- Familiarity with Gradle and Android project structure

The UI module is implemented as a Jetpack Compose-based library. Integration involves adding the module as a dependency, configuring
required parameters via dependency injection, and starting the chat UI by starting the ChatActivity.
The module is designed to work with minimal configuration, but supports advanced customization as needed.

- Starting the chat UI by launching the `ChatActivity` (recommended entry point)

Advanced users may use the provided Composables directly for custom navigation or embedding in custom flows.

1. **Setup & configure UI module**
    - Setup the UI module according the instructions in the [UI Configuration case study](./cs-ui-configuration.md).
2. **Override strings**
    - Provide your own translations by overriding string resources in your app module or modify existing ones.
3. **Configure UI module**
    - The UI module uses Koin for dependency injection. Refer to the Koin documentation or the SDK sample for setup instructions.
    - Provide your own translations by overriding string resources in your app module. Use Android Studio’s translation editor for
      convenience.
    - Override default themes, colors.
    - Refer to the theming case study for details.
4. **Present the chat interface**
    - Start the ChatActivity from your application to display the chat UI.

## Present the chat interface

To display the chat interface, start the chat activity from your activity:

```kotlin
    // From your activity (or fragment)
    ChatActivity.startChat(from = this)
```

## Complete Integration Example

A complete working example is available in the `store/` module within this repository. Review both the code and configuration in this sample
for a full integration reference.

## Advanced Configuration

- **Additional Custom Fields**: If your application uses custom fields beyond the pre-chat survey, refer to
  the [Configuration case study](./cs-ui-configuration.md#custom-field-definitions).
- **Theming**: [See the theming case study](./cs-theming.md)
- **Localization**: (To be documented)
- **Logging**: (To be documented)

## Best Practices

- **SingleTop ChatActivity**: Use `launchMode="singleTop"` for ChatActivity to ensure correct handling of notifications and deep links. This
  avoids unnecessary data fetching if the chat thread is already displayed.
- **Avoid usage of internal API**: Only use APIs annotated with `@com.nice.cxonechat.Public` or documented in case studies. Internal APIs
  may change without notice.
- **Production logging**: Disable Logcat logging in production builds to avoid performance overhead and leaking sensitive information. Use a
  remote logging solution for error reporting if needed.

## Troubleshooting

- **Chat does not appear**:
    - Ensure the CXone Chat SDK core module is properly initialized before presenting the chat
    - Verify your channel configuration
    - Check the logs for any error messages
- **Push notifications do not work**:
    - Confirm that `google-services.json` and Firebase dependencies are correctly added and configured
    - Confirm that the device token is provided to the Chat SDK as outlined in
      the [Push Notifications case study](../chat-sdk-core/cs-push-notifications.md)
    - Verify that the testing device with your application installed is able to receive FCM test messages
    - Check FCM quotas and device network connectivity
    - Try to create a new key for your FCM service account and upload it to your CXone channel settings
- **UI customization not applied**:
    - Ensure that you apply the customizations before starting the ChatActivity
- **Unable to capture photos**:
    - Verify that the device is compatible with prerequisites for the backported PhotoPicker
    - Ensure that your channel attachment settings allow uploads for mimetype `image/jpeg`
- **Unable to capture videos**:
    - Verify that the device is compatible with prerequisites for the backported PhotoPicker
    - Ensure that your channel attachment settings allow uploads for mimetype `video/mp4`
- **Unable to record audio messages**:
    - Ensure that your channel attachment settings allow uploads for mimetype `audio/amr`
    - Verify that the application has the required audio recording permission granted
    - Ensure that the device supports the feature `android.hardware.microphone`
- **Further assistance**:
    - Refer to the sample application, official documentation, or support channels if the problem persists.

## Support and Further Reading

- [CXone Mobile SDK Documentation](../implementation.md)
- [UI Configuration Case Study](./cs-ui-configuration.md)
- [Theming Case Study](./cs-theming.md)
- [Push Notifications Case Study](../chat-sdk-core/cs-push-notifications.md)
- For additional support, contact your CXone representative or open an issue in the repository.
