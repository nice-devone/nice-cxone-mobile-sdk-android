/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.util

import androidx.emoji2.text.EmojiCompat

/**
 * Counts the number of emojis in a given message.
 *
 * @receiver The [EmojiCompat] instance to use for emoji detection.
 * @param message The message to count emojis in.
 * @param limit The maximum number of emojis to count. If the limit is exceeded, -1 is returned.
 * @return The number of emojis in the message, or -1 if the limit is exceeded or if any character is not an emoji.
 */
internal fun EmojiCompat.emojiCount(message: String, limit: Int = Int.MAX_VALUE): Int {
    var emojiCount = 0
    var offset = 0
    while (offset < message.length && emojiCount <= limit) {
        if (getEmojiStart(message, offset) == offset) {
            emojiCount++
            offset = getEmojiEnd(message, offset)
        } else {
            emojiCount = -1
            break
        }
    }
    return if (emojiCount > limit) -1 else emojiCount
}
