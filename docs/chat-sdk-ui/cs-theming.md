# Case Study: Customizing UI Module Theming in CXone Mobile SDK Android

## Overview

The CXone Chat SDK UI module provides a default set of color tokens and theming for all UI components, ensuring a consistent look and feel
out-of-the-box.
However, applications integrating the SDK can override these defaults to match their brand identity and accessibility requirements.

This case study demonstrates how to set up custom theming for the UI module, override default color tokens, and ensure sufficient contrast
for accessibility.

---

## 1. Default Colors and Usage

The UI module defines a set of color tokens (primary, secondary, background, surface, error, etc.) using Jetpack Compose's MaterialTheme3 as
a basis with custom extensions.
These tokens are used throughout the UI components for backgrounds, text, icons, and interactive elements.

By default, the UI module supplies its own color palette, which is suitable for most use cases. However, these can be overridden by the host
application.

---

## 2. Overriding Colors in the Application

To override the default colors, the application should set the desired values to the token variables in
[ChatThemeDetails](/com/nice/cxonechat/ui/composable/theme/ChatThemeDetails.kt) before starting `ChatActivity`.
[ThemeColorTokens](/com/nice/cxonechat/ui/composable/theme/ThemeColorTokens.kt) contains
nested data classes for different token groups (background, content, brand, border, status) and it's respective companion
objects offer helper functions to create instances of these interfaces (implementations are not exposed to avoid breaking API changes).

Example:

```kotlin
// Set custom color tokens before launching ChatActivity
ChatThemeDetails.lightTokens.background = ThemeColorTokens.Background(
    default = Color(0xFFF6F6F6),
    inverse = Color(0xFF222222),
    surface = ThemeColorTokens.Background.Surface(
        default = Color.White,
        variant = Color(0xFFE0E0E0),
        container = Color(0xFFFAFAFA),
        subtle = Color(0xFFF0F0F0),
        emphasis = Color(0xFF007AFF)
    )
)
// Repeat for other token groups: content, brand, border, status

// Now start ChatActivity
val intent = Intent(context, ChatActivity::class.java)
context.startActivity(intent)
```

All UI module components will use these tokens for rendering, allowing customization of the Chat UI.

---

## 3. Ensuring Sufficient Contrast

It is the application's responsibility to supply color tokens with sufficient contrast for accessibility. This means:

- Text should be readable against its background.
- Interactive elements should be distinguishable.
- Follow WCAG guidelines for color contrast (minimum 4.5:1 for normal text).

Use tools like [Material Theme Builder](https://material.io/resources/color/)
or [Color Contrast Checker](https://webaim.org/resources/contrastchecker/) to validate your palette.

---

## 4. Usage of Tokens for Overriding

When overriding, always use the standard Material3 color tokens and the invoke operator on companion objects:

- `primary`, `onPrimary`, `secondary`, `onSecondary`, `background`, `onBackground`, `surface`, `onSurface`, `error`, `onError`, etc.

Example:

```kotlin
ChatThemeDetails.lightTokens.background = ThemeColorTokens.Background(
    default = Color(0xFFF6F6F6),
    inverse = Color(0xFF222222),
    surface = ThemeColorTokens.Background.Surface(
        default = Color.White,
        variant = Color(0xFFE0E0E0),
        container = Color(0xFFFAFAFA),
        subtle = Color(0xFFF0F0F0),
        emphasis = Color(0xFF6200EE) // Emphasis is CXone Chat extension over Material3 tokens
    )
)
```

These Material3 color tokens are set for Chat UI internal override of Material3 Theme colors which is applied to elements
like buttons, text fields, etc. from Material3 library used by the Chat UI.

---

## 5. Summary

- The UI module provides default theming, but applications can override color tokens via `ChatThemeDetails` by setting its token variables.
- Always supply color tokens with sufficient contrast for accessibility.
- Use the standard Material color tokens for overriding.
- Set custom tokens in `ChatThemeDetails` before launching `ChatActivity`.

For more details, see the sample implementation in your project and
the [Jetpack Compose MaterialTheme documentation](https://developer.android.com/jetpack/compose/themes/material).
