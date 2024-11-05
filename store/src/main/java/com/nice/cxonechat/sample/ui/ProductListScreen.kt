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
import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import coil.compose.AsyncImage
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.Product
import com.nice.cxonechat.sample.extensions.asCurrency
import com.nice.cxonechat.sample.extensions.bold
import com.nice.cxonechat.sample.previewproviders.ProductsParameterProvider
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.AppTheme.space
import com.nice.cxonechat.sample.ui.theme.ScreenWithScaffold
import com.nice.cxonechat.sample.viewModel.AnalyticsHandler.PageInfo
import com.nice.cxonechat.sample.viewModel.StoreViewModel
import com.nice.cxonechat.sample.viewModel.UiState

/**
 * The Product List Screen displaying a list of available products.
 */
object ProductListScreen : Screen {
    private const val categoryKey = "category"
    private const val defaultCategory = "smartphones"
    private val NavBackStackEntry.category: String
        get() = arguments?.getString(categoryKey) ?: defaultCategory
    private val routeFormat = routeTo("{$categoryKey}")

    /** route to get back to this screen. */
    val defaultRoute = routeTo()

    private fun routeTo(category: String = defaultCategory) = "products/$category"

    override fun navigation(
        navGraphBuilder: NavGraphBuilder,
        navHostController: NavHostController,
        viewModel: StoreViewModel,
    ) {
        navGraphBuilder.composable(
            route = routeFormat,
            arguments = listOf(
                navArgument(categoryKey) {
                    type = NavType.StringType
                    defaultValue = defaultCategory
                }
            ),
        ) { navBackStackEntry ->
            val context = LocalContext.current
            val category = navBackStackEntry.category
            var products by remember { mutableStateOf<List<Product>>(listOf()) }
            var attempt by remember { mutableIntStateOf(0) }
            var error by rememberSaveable { mutableStateOf<String?>(null) }
            val cart = viewModel.storeRepository.cart.collectAsState().value
            val uiState = viewModel.uiState.collectAsState().value

            // setup to generate page views, but only if no dialog is displayed.
            viewModel.analyticsHandler.SendPageView(pageInfoForState(uiState, category), uiState, error)

            LoadCategories(
                viewModel = viewModel,
                category = category,
                attempt = attempt,
                onError = {
                    error = it
                },
                onSuccess = {
                    products = it
                }
            )

            Screen(
                products,
                onProductSelected = {
                    ProductScreen.navigateTo(it.id, navHostController)
                },
                onUiSettings = viewModel::presentUiSettings,
                onSdkSettings = viewModel::presentConfigurationDialog,
                onLogout = viewModel::logout,
                showCart = {
                    CartScreen.ActionForCart(cart = cart, navHostController)
                },
            )

            error?.let { message ->
                ErrorAlert(
                    message = message,
                    onDismiss = { (context as? Activity)?.finishAffinity() }
                ) {
                    error = null
                    attempt += 1
                }
            }
        }
    }

    private fun pageInfoForState(state: UiState, category: String) = if (state.isInDialog) {
        null
    } else {
        PageInfo("products?$category", "/products/$category")
    }

    @Composable
    private fun LoadCategories(
        viewModel: StoreViewModel,
        category: String,
        attempt: Int,
        onError: (String?) -> Unit,
        onSuccess: (List<Product>) -> Unit
    ) {
        LaunchedEffect(category, attempt) {
            viewModel
                .storeRepository
                .getProducts(category)
                .onSuccess(onSuccess)
                .onFailure {
                    onError(it.localizedMessage)
                }
        }
    }

    /**
     * Navigate to this screen using the passed [NavHostController].
     *
     * @param category category to be displayed.
     * @param navHostController controller to be used for navigation.
     */
    fun navigateTo(category: String, navHostController: NavHostController) {
        navHostController.navigate(routeTo(category))
    }

    @Composable
    internal fun Screen(
        products: List<Product>,
        onProductSelected: (Product) -> Unit,
        onUiSettings: () -> Unit,
        onSdkSettings: () -> Unit,
        onLogout: () -> Unit,
        @SuppressLint(
            "ComposableLambdaParameterNaming" // This isn't intended to be a re-usable composable
        )
        showCart: @Composable RowScope.() -> Unit,
    ) {
        AppTheme.ScreenWithScaffold(
            title = stringResource(string.app_name),
            actions = showCart,
            drawerContent = { close ->
                Drawer(
                    onUiSettings = {
                        close()
                        onUiSettings()
                    },
                    onSdkSettings = {
                        close()
                        onSdkSettings()
                    },
                    onLogout = {
                        close()
                        onLogout()
                    }
                )
            }
        ) {
            ProductListView(
                products,
                onProductSelected = onProductSelected,
            )
        }
    }

    @Composable
    private fun ProductListView(
        products: List<Product>,
        modifier: Modifier = Modifier,
        onProductSelected: (Product) -> Unit,
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(160.dp),
            modifier = modifier.fillMaxSize(),
            verticalItemSpacing = space.medium,
            horizontalArrangement = Arrangement.spacedBy(space.medium),
            content = {
                items(items = products, key = Product::id) { product ->
                    ProductCard(
                        product,
                        modifier.clickable(onClick = { onProductSelected(product) })
                    )
                }
            }
        )
    }

    @Composable
    private fun ProductCard(product: Product, modifier: Modifier = Modifier) {
        val screenWidth = LocalConfiguration.current.screenWidthDp * 3 / 4 / 2
        var size by remember { mutableIntStateOf(screenWidth) }

        Card(
            modifier = modifier.fillMaxWidth().onSizeChanged { size = it.width * 3 / 4 },
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        ) {
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AsyncImage(
                    model = product.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .then(
                            with(LocalDensity.current) {
                                Modifier.height(size.toDp())
                            }
                        ),
                    placeholder = rememberVectorPainter(Icons.Default.Photo),
                    error = rememberVectorPainter(Icons.Default.Error),
                )
                Text(product.title, modifier = Modifier.padding(horizontal = space.medium), maxLines = 1)
                Text(
                    product.price.asCurrency,
                    modifier = Modifier.padding(horizontal = space.medium),
                    style = LocalTextStyle.current.bold
                )
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
}

@Preview
@Composable
private fun ProductListPreview(@PreviewParameter(ProductsParameterProvider::class) products: List<Product>) {
    AppTheme {
        ProductListScreen.Screen(products, {}, {}, {}, {}, {})
    }
}
