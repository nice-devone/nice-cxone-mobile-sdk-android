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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.nice.cxonechat.ui.R

/**
 * Set of images used for decoration and branding.
 */
@Immutable
interface Images {

    /**
     * Image which will be used to display a brand logo.
     */
    val logo: Any?

    companion object {

        /**
         * Creates a set of images which will be used for decoration and branding of the chat.
         *
         * @param logo Branding image which will be used as logo, see [Images.logo].
         * It can be any resource supported by Coil to load an image.
         */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(
            logo: Any?,
        ): Images = ImagesImpl(logo)

        @Immutable
        private data class ImagesImpl(
            override val logo: Any?,
        ) : Images
    }
}

internal val LocalImages = staticCompositionLocalOf {
    Images(DefaultImages.logo)
}

internal object DefaultImages {
    val logo = R.mipmap.ic_launcher
}
