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

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.Product
import com.nice.cxonechat.sample.extensions.asCurrency
import com.nice.cxonechat.sample.extensions.bold
import com.nice.cxonechat.sample.previewproviders.ProductParameterProvider
import com.nice.cxonechat.sample.ui.components.ImageCarousel
import com.nice.cxonechat.sample.ui.components.RatingBar
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.ScreenWithScaffold
import com.nice.cxonechat.sample.viewModel.StoreViewModel

/**
 * The Product screen, allowing a single product to be displayed.
 */
object ProductScreen : Screen {
    private const val productKey = "productId"
    private const val defaultCategory = "1"
    private val NavBackStackEntry.productId: String
        get() = arguments?.getString(productKey) ?: defaultCategory
    private val routeFormat = routeTo("{$productKey}")

    private fun routeTo(category: String = defaultCategory) = "product/$category"

    override fun navigation(navGraphBuilder: NavGraphBuilder, navHostController: NavHostController, viewModel: StoreViewModel) {
        navGraphBuilder.composable(
            route = routeFormat,
            arguments = listOf(
                navArgument(productKey) {
                    type = NavType.StringType
                }
            )
        ) { navBackStackEntry ->
            var product by remember { mutableStateOf<Product?>(null) }
            val productId = navBackStackEntry.productId
            var attempt by remember { mutableIntStateOf(0) }
            var error by rememberSaveable { mutableStateOf<String?>(null) }
            val resetError = { error = null }
            val cart = viewModel.storeRepository.cart.collectAsState().value
            val context = LocalContext.current

            viewModel.analyticsHandler.SendPageView("product?$productId", "/product/$productId")

            LaunchedEffect(productId, attempt) {
                viewModel.storeRepository.getProduct(productId)
                    .onSuccess { product = it }
                    .onFailure { error = it.localizedMessage ?: context.getString(string.unknown_error) }
            }

            Screen(
                product,
                addToCart = viewModel.storeRepository::addToCart,
                showCart = {
                    CartScreen.ActionForCart(cart = cart, navHostController)
                },
            )

            error?.let { message ->
                ErrorAlert(message = message, onDismiss = resetError) {
                    error = null
                    attempt += 1
                }
            }
        }
    }

    /**
     * Navigate to this screen using the passed [NavHostController].
     *
     * @param productId Product to be displayed.
     * @param navHostController controller to be used for navigation.
     */
    fun navigateTo(productId: String, navHostController: NavHostController) {
        navHostController.navigate(routeTo(productId))
    }

    @Composable
    internal fun Screen(
        product: Product?,
        addToCart: (Product) -> Unit,
        @SuppressLint(
            "ComposableLambdaParameterNaming" // This isn't intended to be a re-usable composable
        )
        showCart: @Composable RowScope.() -> Unit,
    ) {
        AppTheme.ScreenWithScaffold(
            title = product?.title ?: stringResource(string.unknown_product),
            actions = showCart,
        ) {
            product?.let { product ->
                ProductView(product = product) {
                    addToCart(product)
                }
            }
        }
    }

    @Composable
    private fun ErrorAlert(message: String, onDismiss: () -> Unit, onRetry: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                OutlinedButton(onClick = onRetry) {
                    Text(stringResource(string.retry))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(string.ok))
                }
            },
            text = { Text(message) },
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    internal fun ProductView(
        product: Product,
        addToCart: (Product) -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ImageCarousel(
                images = product.images,
                modifier = Modifier.fillMaxHeight(0.33f)
            )
            Row {
                Text(product.brand)
                Spacer(Modifier.weight(1f))
                RatingBar(product.rating)
                Spacer(Modifier.width(space.small))
                Text(
                    product.rating.toString(),
                    style = LocalTextStyle.current.bold
                )
            }
            Row {
                Headline(product.title)
                Spacer(modifier = Modifier.weight(1f))
                Headline(product.price.asCurrency)
            }
            Description(product.description)
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { addToCart(product) }) {
                Text(stringResource(string.add_to_cart))
            }
        }
    }

    @Composable
    private fun Headline(text: String) {
        Text(text, style = AppTheme.typography.h5.bold)
    }

    @Composable
    private fun Description(text: String) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth(1f),
            style = AppTheme.typography.body1,
            color = AppTheme.colors.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductPreview(@PreviewParameter(ProductParameterProvider::class) product: Product) {
    AppTheme {
        ProductScreen.ProductView(product = product, addToCart = {})
    }
}
