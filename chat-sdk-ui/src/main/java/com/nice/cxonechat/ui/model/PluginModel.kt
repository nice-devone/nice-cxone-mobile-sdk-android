/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.ui.model

import android.view.View
import com.nice.cxonechat.ui.model.PluginModel.Gallery

/**
 * Internal version of [com.nice.cxonechat.message.PluginElement],
 * which replaces implementation of [com.nice.cxonechat.message.PluginElement.Button]
 * with a version, which has already converted deeplink/postback to onClickAction.
 * This is recursively for all types which can contain the button or other container types (eg. [Gallery]]).
 *
 * @see com.nice.cxonechat.message.PluginElement
 */
@Suppress(
    "UndocumentedPublicProperty" // Properties are copied
)
internal sealed interface PluginModel {
    /**
     * @see com.nice.cxonechat.message.PluginElement.Menu
     */
    data class Menu(
        val files: Iterable<File>,
        val titles: Iterable<Title>,
        val subtitles: Iterable<Subtitle>,
        val texts: Iterable<Text>,
        val buttons: Iterable<Button>,
    ) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.File
     */
    data class File(
        val url: String,
        val name: String,
        val mimeType: String,
    ) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.Title
     */
    data class Title(val text: String) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.Subtitle
     */
    data class Subtitle(val text: String) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.Text
     */
    data class Text(
        @Suppress(
            "MemberNameEqualsClassName"
        )
        val text: String,
        val isMarkdown: Boolean,
        val isHtml: Boolean,
    ) : PluginModel

    /**
     * Modified version of [com.nice.cxonechat.message.PluginElement.Button]
     * with [com.nice.cxonechat.message.PluginElement.Button.deepLink] and
     * [com.nice.cxonechat.message.PluginElement.Button.postback] already converted to [onClickAction].
     *
     * @property text Same as [com.nice.cxonechat.message.PluginElement.Button.text].
     * @property displayInApp Same as [com.nice.cxonechat.message.PluginElement.Button.displayInApp].
     * @property onClickAction Function which should be called as part of [View.OnClickListener.onClick].
     *
     * @see [com.nice.cxonechat.message.PluginElement.Button]
     */
    data class Button(
        val text: String,
        val displayInApp: Boolean,
        val onClickAction: (View) -> Unit,
    ) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.TextAndButtons
     */
    data class TextAndButtons(
        val text: Text,
        val buttons: Iterable<Button>,
    ) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.QuickReplies
     */
    data class QuickReplies(
        val text: Text?,
        val buttons: Iterable<Button>,
    ) : PluginModel

    /**
     * Currently unsupported.
     */
    object InactivityPopup : PluginModel

    /**
     * Currently unsupported.
     */
    object Countdown : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.Custom
     */
    data class Custom(
        val fallbackText: String?,
        val variables: Map<String, Any?>,
    ) : PluginModel

    /**
     * Modified version of [com.nice.cxonechat.message.PluginElement.Gallery]
     * with [elements] type changed to [PluginModel].
     *
     * @see com.nice.cxonechat.message.PluginElement.Gallery
     */
    data class Gallery(val elements: Iterable<PluginModel>) : PluginModel

    /**
     * @see com.nice.cxonechat.message.PluginElement.SatisfactionSurvey
     */
    data class SatisfactionSurvey(
        val text: Text?,
        val button: Button,
        val postback: String?,
    ) : PluginModel
}
