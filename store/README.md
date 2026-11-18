# CXone Chat sample app (store)

A demo storefront which integrates the CXone Chat SDK. It is not published — it exists for manual
testing, demos, and the automated test runs in `cxone-mobile-sdk-test-automation`.

## TL;DR

Launch the app with a channel supplied on the intent and it skips the SDK configuration dialog
entirely, shows its own progress spinner, and opens the chat once it is ready:

```bash
adb shell am start \
  -a android.intent.action.RUN -c android.intent.category.TEST \
  -n com.nice.cxonechat.sample/.StoreActivity \
  --es environment CUSTOM \
  --es environmentHost <your-environment-host> \
  --el brandId <your-brand-id> \
  --es channelId <your-channel-id>
```

> **Non-production environment details are not kept in this repository.** Named environments beyond
> `PROD_NA1`/`PROD_EU1` come from the `environment.json` asset, which published copies do not carry;
> supply anything else with `environmentHost`. See [Supplying a channel](#supplying-a-channel).

`-a android.intent.action.RUN -c android.intent.category.TEST` is **required** — see
[Why the action and category are required](#why-the-action-and-category-are-required).

## Supplying a channel

A channel is named by environment, brand and channel id. There is deliberately no way to select one
of the configurations in `environment.json` by name: those names are human-readable labels which are
expected to change, so they make a poor contract for an automated run.

### A named environment

```bash
--es environment PROD_EU1 --el brandId <your-brand-id> --es channelId <your-channel-id>
```

`PROD_NA1` and `PROD_EU1` are always available — they come from the SDK's own `CXoneEnvironment`.
**Any other name comes from the `environment.json` asset**, which is not included in published
copies of this repository, so those names only resolve in a build that bundles it. A name this copy
cannot resolve is rejected and the log lists the ones it can:

```
Ignoring launch intent: unknown 'environment' value 'SOME_ENVIRONMENT',
expected one of PROD_NA1, PROD_EU1 or CUSTOM
```

> `brandId` must be passed with `--el` (long), not `--ei` (int). An int is rejected rather than read
> as a default, so a typo fails visibly instead of connecting to the wrong brand.

### A custom environment

Use this to reach anything not bundled with the app — it is the escape hatch that keeps working in
any copy. There are two ways to describe it; supply **one or the other**, never both.

**By host stem** — every environment's urls follow `https://<service>-<stem>`, so the stem is all
that is actually needed:

```bash
--es environment CUSTOM --es environmentHost de-na1.niceincontact.com \
--el brandId 1234 --es channelId chat_abc
```

which derives:

| Url | Derived as |
|---|---|
| `baseUrl` | `https://channels-<stem>/` |
| `socketUrl` | `wss://chat-gateway-<stem>` |
| `originHeader` | `https://livechat-<stem>` |
| `chatUrl` | `https://channels-<stem>/chat/` |
| `loggerUrl` | `https://app-<stem>/logger-public` |
| `tokenUrl` | `https://digital-oauth-<stem>/` |

The stem is a bare host — letters, digits, dots and dashes only. A scheme, a path, a trailing slash
or a stray space from a mis-quoted command is rejected rather than folded into the derived urls.

**Url by url** — for an environment that does not follow that pattern:

```bash
--es environment CUSTOM --el brandId 1234 --es channelId chat_abc \
--es envName Local --es envLocation Europe \
--es baseUrl "https://…/" --es socketUrl "wss://…" --es originHeader "https://…" \
--es chatUrl "https://…/chat/" --es tokenUrl "https://…/"
```

All of those are required in this form; `--es loggerUrl` is optional.

## All extras

| Extra | Type | Meaning |
|---|---|---|
| `environment` | `--es` | `PROD_NA1`, `PROD_EU1`, `CUSTOM`, or any name from the bundled `environment.json`. |
| `environmentHost` | `--es` | `CUSTOM` only. Host stem to derive all urls from. Mutually exclusive with the individual urls. |
| `brandId` | `--el` | Brand id. Long, not int. |
| `channelId` | `--es` | Channel id. |
| `firstName`, `lastName` | `--es` | Chat user name. Must be supplied together. Default `QA Automation` when a channel is supplied — see [The chat user](#the-chat-user). |
| `customerId` | `--es` | Customer id for the chat user. Works with the default name too. |
| `openChat` | `--ez` | **Defaults to true.** Pass `--ez openChat false` to stop on the store home instead of opening the chat. |
| `envName`, `envLocation`, `baseUrl`, `socketUrl`, `originHeader`, `chatUrl`, `tokenUrl` | `--es` | Required only when `environment=CUSTOM`. |
| `loggerUrl` | `--es` | Optional, `CUSTOM` only. |

`openChat` can also be sent on its own, to open the chat with whatever configuration the app already
has stored. On a fresh install there is nothing to open, so the request is logged and dropped.

## The chat user

A channel without authorization leaves the app on its login dialog until it has a user name, which
would strand an unattended run. So whenever the intent supplies a channel, it also signs a user in:

- `--es firstName … --es lastName …` if you supply them (both, or neither)
- otherwise **`QA Automation`**, which is deliberately recognisable in the agent console

`--es customerId …` applies either way.

An intent that supplies **no** channel — `--ez openChat true` on its own — never invents a user, so
whoever is already signed in is left alone. A `customerId` sent with nothing to attach it to is
rejected rather than dropped.

## What you will see

Instead of the configuration and login dialogs, the app shows a cancellable progress dialog which
steps through:

1. *Setting up chat* — applying the supplied configuration
2. *Loading chat channel configuration* — the SDK is fetching the channel configuration
3. *Preparing chat* — opening the connection

Once the chat reports `Ready` it opens directly on the thread screen.

**The storefront is not built on this path.** The product list and its images are only loaded when
somebody is actually going to look at them — when you come back from the chat, or when the setup
finishes without opening it. Until then the progress dialog sits on an empty background.

**Cancel** stops the setup and drops you on the store home, so a person using this entry point by
hand is never trapped behind the spinner.

## Limitations

- **Authorization-enabled channels are not fully automatable.** They require an interactive browser
  sign-in, which no intent extra can supply. The app gives up on the automated setup and shows the
  usual OAuth sheet; the chat is not opened. Use a channel without authorization for unattended
  runs.
- The request is applied on a cold start and on `onNewIntent`, but not on an activity restore — a
  configuration you changed in the app is not silently overwritten when the activity is recreated.

## Pre-granting permissions

An unattended run otherwise stops on the notification permission dialog the first time the chat
opens. Grant the runtime permissions up front — either at install:

```bash
adb install -g -t store-debug.apk
```

or on an app that is already installed:

```bash
adb shell pm grant com.nice.cxonechat.sample android.permission.POST_NOTIFICATIONS
adb shell pm grant com.nice.cxonechat.sample android.permission.CAMERA
adb shell pm grant com.nice.cxonechat.sample android.permission.RECORD_AUDIO
```

`POST_NOTIFICATIONS` is the one that blocks an automated run; `CAMERA` and `RECORD_AUDIO` only come
up if the test takes a photo or records a voice message.

> `adb shell pm clear` revokes them again, so re-grant after clearing. The grants also survive
> `adb install -r`, but not an uninstall.

## Reconfiguring a running app

`StoreActivity` is `singleTop`, so a second `am start` reaches `onNewIntent` **only while it is the
top activity**. After the chat has been opened it is on top, so a plain relaunch would stack a second
`StoreActivity` above it. `FLAG_ACTIVITY_CLEAR_TOP` finishes the chat and delivers the intent to the
existing `StoreActivity`:

```bash
adb shell am start \
  -a android.intent.action.RUN -c android.intent.category.TEST \
  -f 0x04000000 -n com.nice.cxonechat.sample/.StoreActivity \
  --es environment PROD_EU1 --el brandId 1234 --es channelId chat_other
```

For a completely clean slate, `adb shell pm clear com.nice.cxonechat.sample` before launching.

## Why the action and category are required

`StoreActivity` declares `android:intentMatchingFlags="enforceIntentFilter"`. On Android 16 and
later that means an intent from another app — including one sent by `adb shell am start` — must
match one of the activity's intent filters, and **an intent with no action matches nothing**. So
`am start -n com.nice.cxonechat.sample/.StoreActivity --es …` on its own is blocked, failing with:

```
Error: Activity class {com.nice.cxonechat.sample/...StoreActivity} does not exist.
```

`android.intent.action.RUN` + `android.intent.category.TEST` are the platform's own constants for
"launch this as a test, not as part of the normal user experience". The filter carries no
`LAUNCHER` category, so it adds no second icon to the launcher.

## Troubleshooting

A malformed request is never partially applied. The reason is logged and the app falls back to the
normal configuration dialog:

```bash
adb logcat -s StoreActivity:* AutomatedSetupHandler:* StoreViewModel:*
```

Typical messages include an `environment` value this copy cannot resolve,
a `brandId` passed as an int, a missing `channelId`, `firstName` without `lastName`, an
incomplete `environment=CUSTOM` URL set, and an `environmentHost` that is not a bare hostname.

This holds for a reconfiguring intent too: a refused second `am start` abandons any setup already
running and puts the app back on the storefront, or on the configuration dialog when nothing is
stored — it never leaves the progress spinner up.
