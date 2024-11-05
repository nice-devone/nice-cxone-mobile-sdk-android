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

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.ContinueButton
import com.nice.cxonechat.sample.ui.theme.ScreenWithScaffold
import com.nice.cxonechat.sample.viewModel.StoreViewModel

/**
 * Define the ConfirmationScreen that appears after the Payment screen, confirming that
 * the transaction is complete, and, perhaps more importantly, issuing a transaction complete
 * event.
 */
object ConfirmationScreen : Screen {
    private const val routeTo = "confirmation"
    private const val routeFormat = routeTo
    private const val defaultRoute = routeTo

    override fun navigation(navGraphBuilder: NavGraphBuilder, navHostController: NavHostController, viewModel: StoreViewModel) {
        navGraphBuilder.composable(
            route = routeFormat,
        ) {
            viewModel.analyticsHandler.SendPageView("confirmation", "/confirmation")

            Screen(
                onContinue = {
                    navHostController.popBackStack(
                        route = ProductListScreen.defaultRoute,
                        inclusive = false
                    )
                }
            )
        }
    }

    /**
     * Navigate to this screen using the passed [NavHostController].
     *
     * @param navHostController controller to be used for navigation.
     */
    fun navigateTo(navHostController: NavHostController) {
        navHostController.navigate(defaultRoute)
    }

    @Composable
    internal fun Screen(onContinue: () -> Unit) {
        AppTheme.ScreenWithScaffold(
            title = stringResource(string.payment),
        ) {
            BackHandler(onBack = onContinue)
            Column(Modifier.fillMaxSize()) {
                Text(stringResource(id = string.confirmation))
                Spacer(Modifier.weight(1f))
                AppTheme.ContinueButton(onClick = onContinue)
            }
        }
    }
}

@Preview
@Composable
private fun ConfirmationPreview() {
    AppTheme {
        ConfirmationScreen.Screen(onContinue = {})
    }
}
