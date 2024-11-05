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

import android.content.Context
import android.text.Spannable
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlin.math.max

private const val SPACING_FIX = 3f

@Composable
internal fun SpannableTextView(
    spannable: Spannable,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = ChatTheme.typography.bodyLarge,
) {
    AndroidView(
        modifier = modifier,
        update = { it.text = spannable },
        factory = { context ->
            val spacingReady =
                max(textStyle.lineHeight.value - textStyle.fontSize.value - SPACING_FIX, 0f)
            val extraSpacing = spToPx(spacingReady.toInt(), context)
            val gravity = when (textStyle.textAlign) {
                TextAlign.Center -> Gravity.CENTER
                TextAlign.End -> Gravity.END
                else -> Gravity.START
            }

            TextView(context).apply {
                // general style
                textSize = textStyle.fontSize.value
                setLineSpacing(extraSpacing, 1f)
                if (textStyle.color != Color.Unspecified) {
                    setTextColor(textStyle.color.toArgb())
                }
                setGravity(gravity)
            }
        }
    )
}

@Composable
internal fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = ChatTheme.typography.bodyLarge
) {
    SpannableTextView(
        spannable = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT) as Spannable,
        modifier = modifier,
        textStyle = textStyle
    )
}

private fun spToPx(sp: Int, context: Context) =
    TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp.toFloat(),
        context.resources.displayMetrics
    )

@Preview
@Composable
private fun PreviewHtml() {
    val html = "Some <b>bold</b> text."

    ChatTheme {
        HtmlText(html = html)
    }
}
