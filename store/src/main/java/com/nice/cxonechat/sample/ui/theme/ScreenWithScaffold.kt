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

package com.nice.cxonechat.sample.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.sample.ui.theme.Strings.content
import com.nice.cxonechat.sample.ui.theme.Strings.title
import com.nice.cxonechat.ui.ChatActivity
import kotlinx.coroutines.launch

/**
 * Displays a screen in an AppTheme Scaffold.
 *
 * Displays a screen specified by [content] in an AppTheme Scaffold, along with
 * a Fab to open Chat, a slide out drawer, and defined back button and action menu.
 *
 * @param title Title to be displayed in TopAppBar
 * @param actions Actions to be displayed in TopAppBar
 * @param drawerContent Composable to be displayed in the slide out drawer.
 * @param content Content to be displayed in the body of the scaffold.
 */
@Composable
fun AppTheme.ScreenWithScaffold(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    drawerContent: @Composable ((() -> Unit) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val openDrawer: () -> Unit = {
        coroutineScope.launch {
            drawerState.open()
        }
    }

    val closeDrawer: () -> Unit = {
        coroutineScope.launch {
            drawerState.close()
        }
    }

    val navigationIcon: @Composable (() -> Unit) = when {
        drawerContent != null -> {
            {
                IconButton(onClick = openDrawer) {
                    Icon(Icons.Default.Menu, null)
                }
            }
        }
        else -> { {} }
    }

    val onOpenChat: (() -> Unit) = {
        (context as? Activity)?.run(ChatActivity::startChat)
    }

    val scaffoldWithContent: @Composable () -> Unit = {
        ScaffoldWithContent(title, navigationIcon, actions, onOpenChat, content)
    }

    if (drawerContent == null) {
        scaffoldWithContent()
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    drawerContent(closeDrawer)
                }
            },
            content = scaffoldWithContent
        )
    }
}

@Composable
private fun AppTheme.ScaffoldWithContent(
    title: String,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable (RowScope.() -> Unit),
    onOpenChat: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                title,
                navigationIcon = navigationIcon,
                actions = actions
            )
        },
        floatingActionButton = { ChatFab(onClick = onOpenChat) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(space.defaultPadding)
                .padding(paddingValues)
        ) {
            content()
        }
    }
}

private object Strings {
    const val title = "Some Title"
    const val content = "Content"
}

@Preview
@Composable
private fun PreviewScreenDrawer() {
    AppTheme {
        AppTheme.ScreenWithScaffold(
            title = title,
            drawerContent = {
                Text("Drawer")
            },
        ) {
            Text(content)
        }
    }
}

@Preview
@Composable
private fun PreviewScreenBack() {
    AppTheme {
        AppTheme.ScreenWithScaffold(
            title = title,
            drawerContent = {
                Text("Drawer")
            },
        ) {
            Text(content)
        }
    }
}

@Preview
@Composable
private fun PreviewScreenNothing() {
    AppTheme {
        AppTheme.ScreenWithScaffold(
            title = title,
        ) {
            Text(content)
        }
    }
}
