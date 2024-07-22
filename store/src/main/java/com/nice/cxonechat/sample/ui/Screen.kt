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

package com.nice.cxonechat.sample.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.nice.cxonechat.sample.viewModel.StoreViewModel

/**
 * Generic definition of a screen to be included in a NavGraphBuilder/NavHostController pair.
 */
interface Screen {
    /**
     * Define the navigation routes to access this screen.
     *
     * @param navGraphBuilder NavGraphBuilder to populate.
     * @param navHostController NavHostController to use for navigation.
     * @param viewModel StoreViewModel associated with attach activity.
     */
    fun navigation(
        navGraphBuilder: NavGraphBuilder,
        navHostController: NavHostController,
        viewModel: StoreViewModel,
    )
}
