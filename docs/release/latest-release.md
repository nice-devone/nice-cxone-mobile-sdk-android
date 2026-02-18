# Version

**3.1.2**

## Status:

**RELEASED**

## Release Notes:

### Fixed

- Fix FileProvider authority conflict when multiple apps using the SDK are published on Google Play by making the authority dynamic based on
  application package name instead of hardcoded value
- Fix PdfRender thread-safety issue causing crashes during concurrent access
- Fix PdfRenderer resource leak during rapid scrolling and constructor failures by properly closing resources in all error paths and
  lifecycle events
- Fix opening of non-video/image attachments by adding URI read permission flag
- Fix issue introduced in version 3.0 where user entered text which is auto-linked is invisible on background
- Remove remnants of unused Foreground Service implementation to prevent clash with other integrations
