# Case Study: Localization of UI Module in CXone Mobile SDK Android

## Overview

The CXone Chat SDK UI module provides a default set of strings for all UI components, ensuring a consistent user experience out-of-the-box.
However, applications integrating the SDK can override these defaults to support multiple languages and regional preferences using standard
[Android localization practices](https://developer.android.com/guide/topics/resources/localization#using-framework).

This case study shows how the integrating application can add localizations by example.

## Prerequisites

- You have added the CXone Chat SDK UI module to your applications as is outlined in [cs-ui-configuration](cs-ui-configuration.md)

## Add & Override the default strings

This step execution may be dependent on what solution you are using to facilitate translation process and it may be skipped if you are not
using
AndroidStudio IDE to manage strings.

Generally you can start by copying default Chat SDK UI [strings](/chat-sdk-ui/src/main/res/values/strings.xml) file to your
resource values folder under different name e.g.: [chat-ui-strings](/store/src/main/res/values/chat_ui_strings.xml)

This will effectively override all chat strings with this copy and will provide you with opportunity to translate the default strings if
your
default locale isn't english.

In case you will skip this step and you will supply just translations you will need to suppress Lint check `ExtraTranslation`.

## Supply additional resources

Now you can provide translations for strings by defining resources with locale qualifier e.g.:
[values-cs/chat-ui-string.xml](/store/src/main/res/values-cs/chat_ui_strings.xml)

