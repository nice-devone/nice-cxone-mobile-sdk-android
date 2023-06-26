package com.nice.cxonechat.message

import com.nice.cxonechat.Public
import com.nice.cxonechat.event.CustomVisitorEvent
import com.nice.cxonechat.message.PluginElement.Custom
import com.nice.cxonechat.message.PluginElement.InactivityPopup
import com.nice.cxonechat.message.PluginElement.Menu
import com.nice.cxonechat.message.PluginElement.QuickReplies
import com.nice.cxonechat.message.PluginElement.TextAndButtons
import java.util.Date

/**
 * Elements provided with Messages with a [Plugin][Message.Plugin]. Plugins are
 * enclosed and highly variable, though SDK redefines them in a few aggregating
 * structures. These structures are:
 *
 * - [Menu] (also referenced as Gallery)
 * - [TextAndButtons]
 * - [QuickReplies]
 * - [InactivityPopup]
 * - [Custom]
 *
 * All of these are parsed ahead of time for integrator's convenience. Whenever
 * none of these use-cases fits your needs, use [Custom] type from which you
 * can extract properties defined by your company or representative.
 *
 * @see Message.Plugin
 * */
@Public
sealed class PluginElement {

    /**
     * Menu plugin also references as `Gallery` in some reference documents.
     * These elements are highly variable and there's no guarantee that all
     * of the elements will be present at any given time. Though at least one
     * element should be present at any given time.
     * */
    @Public
    abstract class Menu : PluginElement() {
        /**
         * Files associated with this menu. Drain at most once, if possible.
         * Content will not change from once call of this method to another.
         *
         * If the associated elements contain no appropriately typed items,
         * then iterable returns no elements.
         *
         * @see File
         * */
        abstract val files: Iterable<File>

        /**
         * Titles associated with this menu. Drain at most once, if possible.
         * Content will not change from once call of this method to another.
         *
         * If the associated elements contain no appropriately typed items,
         * then iterable returns no elements.
         *
         * @see Title
         * */
        abstract val titles: Iterable<Title>

        /**
         * Subtitle (as a title of a second category) associated with this
         * menu. Drain at most once, if possible. Content will not change
         * from once call of this method to another.
         *
         * If the associated elements contain no appropriately typed items,
         * then iterable returns no elements.
         *
         * @see Subtitle
         * */
        abstract val subtitles: Iterable<Subtitle>

        /**
         * Texts associated with this menu. Drain at most once, if possible.
         * Content will not change from once call of this method to another.
         *
         * If the associated elements contain no appropriately typed items,
         * then iterable returns no elements.
         *
         * @see Text
         * */
        abstract val texts: Iterable<Text>

        /**
         * Buttons associated with this menu. Drain at most once, if
         * possible. Content will not change from once call of this method
         * to another.
         *
         * If the associated elements contain no appropriately typed items,
         * then iterable returns no elements.
         *
         * @see Button
         * */
        abstract val buttons: Iterable<Button>
    }

    /**
     * File of any type defined by [mimeType]. This class doesn't carry any
     * guarantees of the file existing on the remote server, if you need to
     * know whether it exists use HEAD request on the url.
     * */
    @Public
    abstract class File : PluginElement() {
        /**
         * Url for the file on a remote server. It can required authorization
         * though there's no information or guarantee that it does. Use your
         * representative's expertise.
         * */
        abstract val url: String

        /**
         * Name of the original file which was used to upload to a remote
         * server. This should be something either human-readable or user
         * defined from when the user requested to upload this file.
         * */
        abstract val name: String

        /**
         * Mime type of the file hosted on [url]. Don't assume all files are
         * equal in type! Make sure to correctly categorize files based on
         * their mime types while integrating them to your application.
         * */
        abstract val mimeType: String
    }

    /**
     * Title text component. Title is usually provided with large aggregating
     * components.
     *
     * @see Menu
     * @see InactivityPopup
     * */
    @Public
    abstract class Title : PluginElement() {
        /**
         * Text ought to be displayed in a title styleable component. It is
         * not localized to your app's language though. Uses language defined
         * in your agent console.
         * */
        abstract val text: String
    }

    /**
     * Subtitle text component. Subtitle is usually provided with large
     * aggregating components.
     *
     * @see Menu
     * @see InactivityPopup
     * */
    @Public
    abstract class Subtitle : PluginElement() {
        /**
         * Text ought to be displayed in a subtitle styleable component. It is
         * not localized to your app's language though. Uses language defined
         * in your agent console.
         * */
        abstract val text: String
    }

    /**
     * Regular text component.
     * Contextually can be of a different form, such as html or markdown.
     * It's up to the integrator to format or strip the text's format
     * if they do not wish to use formatted text.
     * */
    @Public
    abstract class Text : PluginElement() {
        /**
         * Text with formatting. This is determined by additional properties
         * [isMarkdown] or [isHtml].
         * */
        @Suppress(
            "MemberNameEqualsClassName" // Part of shared API.
        )
        abstract val text: String

        /**
         * Determines whether is this [text] markdown formatted.
         * */
        abstract val isMarkdown: Boolean

        /**
         * Determines whether is this [text] html formatted.
         * */
        abstract val isHtml: Boolean
    }

    /**
     * Button component. Buttons should report [postback]s when clicking the
     * button. Postback might contain a [deepLink] which is extracted for
     * convenience.
     */
    @Public
    abstract class Button : PluginElement() {
        /**
         * Text to display in place of the button.
         * Text is unformatted and localized, according as per agent console settings.
         * */
        abstract val text: String

        /**
         * Metadata associated with the button which should be returned to the server
         * once the button is pressed.
         * Send an [OutboundMessage] message with button [text] and the [postback] values.
         */
        abstract val postback: String?

        /**
         * Deeplink extracted from postback if applicable.
         * Not all buttons have deepLinks.
         * DeepLink is associated with a specific app.
         * For an example, you might want to use it to redirect the user directly
         * to a Facebook profile or perform another similar action.
         * Or it can be just a plain URL.
         * */
        abstract val deepLink: String?

        /**
         * Flag indicating that deeplink contents should be displayed in the context of application.
         * This will be true only for URL deeplink.
         */
        abstract val displayInApp: Boolean
    }

    /**
     * Component containing [Text] and [Button] elements. You should always
     * expect at least one button, but more often might encounter multiple
     * buttons as choices for the user.
     *
     * @see Text
     * @see Button
     * */
    @Public
    abstract class TextAndButtons : PluginElement() {
        /**
         * Text associated with a choice which is presented by [buttons].
         *
         * @see Text
         * */
        abstract val text: Text

        /**
         * Buttons, or choices, associated with this component. Do not
         * display buttons without displaying text component. Buttons might
         * be as simple as "Yes", which needs the contextual information
         * from [text].
         * */
        abstract val buttons: Iterable<Button>
    }

    /**
     * Quick replies component. Might be presented with optional [text].
     * This component might be provided in cases where users are prompted
     * with a satisfaction prompt or similar.
     *
     * @see Text
     * @see Button
     * */
    @Public
    abstract class QuickReplies : PluginElement() {
        /**
         * Optional text component which shouldn't provide any contextual
         * information and is only informative to the user.
         *
         * @see Text
         * */
        abstract val text: Text?

        /**
         * Buttons associated with this component. Buttons might send
         * messages on behalf of the user using appropriate event. It's
         * guaranteed that there will be at least one button, maximum is
         * undefined.
         *
         * @see Button
         * */
        abstract val buttons: Iterable<Button>
    }

    /**
     * Inactivity popups are presented to the user when agent or system
     * on behalf of agent detects inactivity of the user. They are
     * supported by a [countdown] which might be shown to the user and
     * [buttons] to take action to stop the countdown or something else.
     *
     * Note that even though it's named "popup" it doesn't need to be
     * implemented on the UI side to reflect the name. It can be also an
     * interactive message in chat or a banner of sorts.
     *
     * @see Title
     * @see Subtitle
     * @see Text
     * @see Button
     * @see Countdown
     * */
    @Public
    abstract class InactivityPopup : PluginElement() {
        /**
         * Title providing contextual information about the user's
         * inactivity.
         *
         * @see Title
         * */
        abstract val title: Title

        /**
         * Optional subtitle providing additional information about the
         * popup.
         *
         * @see Subtitle
         * */
        abstract val subtitle: Subtitle?

        /**
         * Texts representing different sections of the popup, every text
         * should have its own widget (or view).
         *
         * @see Text
         * */
        abstract val texts: Iterable<Text>

        /**
         * Buttons associated with choices presented to the user. It can
         * be empty, in which case the countdown can be cancelled by
         * sending a message to the given thread.
         *
         * @see Button
         * */
        abstract val buttons: Iterable<Button>

        /**
         * Countdown associated with this component. Countdowns that end
         * in the past are obviously expired. Buttons should be disabled
         * if countdown is expired.
         *
         * @see Countdown
         * */
        abstract val countdown: Countdown
    }

    /**
     * Countdown component. Countdowns should affect whether are
     * interactive components active. Interactive components might be
     * [Custom] or [Button].
     * */
    @Public
    abstract class Countdown : PluginElement() {
        /**
         * Date in the future (or in the past for when [isExpired] is
         * `true`) that holds the information about an action that should
         * be taken before the countdown expires.
         *
         * @see isExpired
         * */
        abstract val endsAt: Date

        /**
         * Method that checks current time against [endsAt]. It can be
         * polled periodically to check whether actions should be active.
         * */
        abstract val isExpired: Boolean
    }

    /**
     * Custom component defined by the integrator. All variables supplied
     * to the custom action is deserialized as [Map]. If the [variables]
     * are invalid, from integrator's point of view, then they should use
     * [fallbackText].
     * */
    @Public
    abstract class Custom : PluginElement() {
        /**
         * Default text that should be used only in cases where [variables]
         * is deemed invalid.
         * */
        abstract val fallbackText: String?

        /**
         * Variables deserialized from objects supplied from backend. These
         * are never modified or injected by the SDK.
         * */
        abstract val variables: Map<String, Any?>
    }

    /**
     * Gallery component which bundles together other components, which should be displayed together in one context.
     * Gallery is the only component which can contain a collection of all other [PluginElement]s.
     */
    @Public
    abstract class Gallery : PluginElement() {

        /**
         * Collection of other [PluginElement]s. If the elements **are empty**,
         * then you have received a message with an element that's not supported by
         * this version. Kindly update the SDK in order to gain support.
         */
        abstract val elements: Iterable<PluginElement>
    }

    /**
     * Satisfaction survey component which can be presented to user
     * as a call to action to fill out a survey (via deeplink in the [button]).
     *
     * @see Text
     * @see Button
     */
    @Public
    abstract class SatisfactionSurvey : PluginElement() {

        /**
         * Optional [Text] with a message to the user, which should ask user to fill out the survey.
         */
        abstract val text: Text?

        /**
         * [Button] with deeplink leading to the survey.
         */
        abstract val button: Button

        /**
         * Optional metadata associated with the survey, which should be reported to backend for analytics tracking.
         * Use a [CustomVisitorEvent] to report that component was displayed if the value is present.
         */
        abstract val postback: String?
    }
}
