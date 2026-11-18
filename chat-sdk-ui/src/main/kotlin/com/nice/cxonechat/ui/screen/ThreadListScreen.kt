/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.screen

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.AddFab
import com.nice.cxonechat.ui.composable.ThreadListContentView
import com.nice.cxonechat.ui.composable.theme.BackButton
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.Scaffold
import com.nice.cxonechat.ui.composable.theme.TopBar
import com.nice.cxonechat.ui.composable.theme.wideScreenHorizontalPadding
import com.nice.cxonechat.ui.util.koinActivityViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel

/**
 * Displays a list of chat threads in the chat UI.
 * It provides a top bar with a back button and a floating action button to create a new thread.
 *
 * @param snackbarHostState [SnackbarHostState] to show snackbars.
 * @param navController [NavHostController] for navigation actions.
 * @param modifier [Modifier] to be applied to the screen.
 */
@Composable
internal fun ThreadListScreen(snackbarHostState: SnackbarHostState, navController: NavHostController, modifier: Modifier) {
    val chatThreadsViewModel = koinActivityViewModel<ChatThreadsViewModel>()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val onBack: () -> Unit = { backDispatcher?.onBackPressed() }
    // On wide/foldable screens, inset the FAB so its trailing edge aligns with the content column
    // (and each cell's divider), matching the design. The Scaffold already applies a built-in 16dp
    // margin to a FabPosition.End FAB, so we add only the remainder of the content inset on top of
    // it. On compact/portrait widths the content inset is 0dp, leaving the layout unchanged.
    val contentInset = wideScreenHorizontalPadding()
    val fabHorizontalPadding = (contentInset - space.scaffoldFabEndMargin).coerceAtLeast(0.dp)
    ChatTheme.Scaffold(
        modifier = Modifier
            .testTag("thread_list_view")
            .then(modifier),
        snackbarHostState = snackbarHostState,
        topBar = {
            ChatTheme.TopBar(
                title = stringResource(R.string.thread_list_title),
                navigationIcon = { BackButton(onBack) },
            )
        },
        floatingActionButton = {
            AddFab(
                onClick = remember { { chatThreadsViewModel.createThread() } },
                modifier = Modifier.padding(end = fabHorizontalPadding),
            )
        }
    ) { paddingValues ->
        // MultiThreadContent already insets its rows by wideScreenHorizontalPadding() to align them
        // with the title, so the box itself only needs the Scaffold's own padding.
        Box(modifier = Modifier.padding(paddingValues)) {
            ThreadListContentView(chatThreadsViewModel) {
                navController.navigate(Screen.ThreadScreen)
            }
        }
    }
}
