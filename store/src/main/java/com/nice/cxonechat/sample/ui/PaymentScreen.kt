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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.operations.total
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.ContinueButton
import com.nice.cxonechat.sample.ui.theme.ScreenWithScaffold
import com.nice.cxonechat.sample.viewModel.StoreViewModel

/**
 * Payment screen to dummy collect payment information.
 */
object PaymentScreen : Screen {
    private const val routeTo = "payment"
    private const val routeFormat = routeTo
    private const val defaultRoute = routeTo

    override fun navigation(navGraphBuilder: NavGraphBuilder, navHostController: NavHostController, viewModel: StoreViewModel) {
        navGraphBuilder.composable(
            route = routeFormat,
        ) {
            val cart = viewModel.storeRepository.cart.collectAsState().value

            viewModel.analyticsHandler.SendPageView("payment", "/payment")

            Screen(
                onContinue = {
                    viewModel.analyticsHandler.sendConversion("sale", cart.total)
                    viewModel.storeRepository.resetCart()
                    ConfirmationScreen.navigateTo(navHostController)
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
        AppTheme.ScreenWithScaffold(title = stringResource(string.payment)) {
            Form(onContinue)
        }
    }

    @Composable
    private fun Form(onContinue: () -> Unit) {
        var name by remember { mutableStateOf("") }
        var cardNumber by remember { mutableStateOf("") }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Field(name = stringResource(string.name), value = name, onValueChange = { name = it })
            Field(
                name = stringResource(string.card_number),
                value = cardNumber,
                onValueChange = { cardNumber = it },
                placeholder = stringResource(string.card_number_placeholder)
            )
            Spacer(modifier = Modifier.weight(1f))
            AppTheme.ContinueButton(onClick = onContinue)
        }
    }

    @Composable
    private fun Field(name: String, value: String, onValueChange: (String) -> Unit, placeholder: String? = null) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(name) },
            placeholder = { placeholder?.let { Text(it) } }
        )
    }
}

@Preview
@Composable
private fun PaymentScreenPreview() {
    AppTheme {
        PaymentScreen.Screen(onContinue = {})
    }
}
