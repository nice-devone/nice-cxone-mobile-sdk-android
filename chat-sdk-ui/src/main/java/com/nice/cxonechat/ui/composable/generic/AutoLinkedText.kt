/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable.generic

import android.text.Spannable
import android.text.Spannable.Factory
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.text.util.LinkifyCompat
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun AutoLinkedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
) {
    Text(
        text = autoLinkedText(text),
        modifier = modifier,
        style = style,
    )
}

internal fun autoLinkedText(
    text: String,
): AnnotatedString = spannableToAnnotated(
    Factory.getInstance()
        .newSpannable(text)
        .also {
            LinkifyCompat.addLinks(it, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS)
        }
)

private fun spannableToAnnotated(spannable: Spannable): AnnotatedString = buildAnnotatedString {
    var lastEnd = 0
    for (span in spannable.getSpans(0, spannable.length, Any::class.java)) {
        val start = spannable.getSpanStart(span)
        val end = spannable.getSpanEnd(span)
        append(spannable.subSequence(lastEnd, start))
        if (span is URLSpan) {
            withLink(LinkAnnotation.Url(url = span.url)) {
                append(spannable.subSequence(start, end))
            }
        } else {
            append(spannable.subSequence(start, end))
        }
        lastEnd = end
    }
    append(spannable.subSequence(lastEnd, spannable.length))
}

@Composable
@Preview
private fun PreviewAutoLinkedText() {
    ChatTheme {
        AutoLinkedText("A text containing a link https://example.com")
    }
}
