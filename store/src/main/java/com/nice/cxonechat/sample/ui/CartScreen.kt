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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nice.cxonechat.sample.R.string
import com.nice.cxonechat.sample.data.models.Cart
import com.nice.cxonechat.sample.data.models.Cart.Item
import com.nice.cxonechat.sample.data.operations.isEmpty
import com.nice.cxonechat.sample.data.operations.total
import com.nice.cxonechat.sample.extensions.asCurrency
import com.nice.cxonechat.sample.extensions.bold
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.ContinueButton
import com.nice.cxonechat.sample.ui.theme.ScreenWithScaffold
import com.nice.cxonechat.sample.viewModel.StoreViewModel

/**
 * Defines the Cart Screen which displays a list of items in the cart as well
 * as the total amount in the cart and allows the quantity of individual items
 * to be changed.
 */
object CartScreen : Screen {
    private const val routeTo = "cart"
    private const val routeFormat = routeTo
    private const val defaultRoute = routeTo

    /**
     * Define some constant values so columns all line up.
     */
    private object Dimensions {
        /** width of quantity column. */
        val quantityWidth = 60.dp

        /** width of extended price column. */
        val extendedWidth = 90.dp
    }

    override fun navigation(navGraphBuilder: NavGraphBuilder, navHostController: NavHostController, viewModel: StoreViewModel) {
        navGraphBuilder.composable(route = routeFormat) {
            viewModel.analyticsHandler.SendPageView("cart", "/cart")

            Screen(
                cart = viewModel.storeRepository.cart.collectAsState().value,
                onContinue = { PaymentScreen.navigateTo(navHostController) },
                updateItem = { viewModel.storeRepository.updateCartItem(it) }
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

    /**
     * A composable to create a button suitable for navbar usage to navigate to the cart page iff the cart
     * is not empty.
     *
     * @param cart Cart to test.
     * @param navHostController [NavHostController] to be used for navigation.
     */
    @Composable
    fun ActionForCart(cart: Cart?, navHostController: NavHostController) {
        if (cart?.isEmpty == false) {
            IconButton(onClick = { navigateTo(navHostController) }) {
                Icon(Icons.Default.ShoppingCart, stringResource(string.shopping_cart))
            }
        }
    }

    @Composable
    internal fun Screen(
        cart: Cart,
        onContinue: () -> Unit,
        updateItem: (Item) -> Unit,
    ) {
        AppTheme.ScreenWithScaffold(title = stringResource(string.shopping_cart)) {
            CartView(
                cart,
                updateItem = updateItem,
                onContinue = onContinue,
            )
        }
    }

    @Composable
    internal fun CartView(
        cart: Cart,
        modifier: Modifier = Modifier,
        updateItem: (Item) -> Unit,
        onContinue: () -> Unit,
    ) {
        Column(modifier = modifier.fillMaxSize()) {
            CartListView(cart, modifier = Modifier.weight(1f), updateItem = updateItem)
            AppTheme.ContinueButton(onContinue)
        }
    }

    @Composable
    private fun CartListView(
        cart: Cart,
        modifier: Modifier = Modifier,
        updateItem: (Item) -> Unit
    ) {
        // Creating a values and variables to remember
        // focus requester, manager and state
        val focusRequester = remember { FocusRequester() }

        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Header(modifier = Modifier.padding(bottom = 4.dp))
                Divider(thickness = 2.dp, color = AppTheme.colors.onBackground)
            }

            itemsIndexed(items = cart.items, key = { _, item -> item.productId }) { index, item ->
                CartLineItem(
                    item,
                    index != cart.items.lastIndex,
                    modifier = Modifier.padding(vertical = 4.dp),
                    focusRequester,
                    updateItem = updateItem
                )
                if (index != cart.items.lastIndex) {
                    Divider()
                }
            }

            item {
                Divider(thickness = 2.dp, color = AppTheme.colors.onBackground)
                Footer(cart.total, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    @Composable
    private fun Header(modifier: Modifier = Modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(string.quantity_label),
                Modifier.width(Dimensions.quantityWidth),
                textAlign = TextAlign.Center
            )
            Text(stringResource(string.item_label), Modifier.weight(1f), textAlign = TextAlign.Center)
            Text(stringResource(string.price_label))
            Text(
                stringResource(string.total_label),
                Modifier.width(Dimensions.extendedWidth),
                textAlign = TextAlign.Center
            )
        }
    }

    @Composable
    private fun Footer(total: Double, modifier: Modifier = Modifier) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(total.asCurrency, style = AppTheme.typography.body1.bold)
        }
    }

    @Composable
    private fun CartLineItem(
        item: Item,
        more: Boolean,
        modifier: Modifier = Modifier,
        focusRequester: FocusRequester,
        updateItem: (Item) -> Unit
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            var quantity by remember { mutableStateOf(item.quantity.toString()) }
            val focusManager = LocalFocusManager.current

            OutlinedTextField(
                quantity,
                onValueChange = { quantity = it },
                modifier = Modifier
                    .width(Dimensions.quantityWidth)
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        if (!it.hasFocus) {
                            updateItem(item.copy(quantity = quantity.toInt()))
                        }
                    },
                textStyle = AppTheme.typography.body1.copy(textAlign = TextAlign.End),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = if(more) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                )
            )
            Text(
                item.title,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
                softWrap = true
            )
            Text(item.price.asCurrency, textAlign = TextAlign.End, modifier = Modifier.alignByBaseline())
            Text(
                item.extended.asCurrency,
                modifier = Modifier
                    .width(Dimensions.extendedWidth)
                    .alignByBaseline(),
                textAlign = TextAlign.End
            )
        }
    }
}

@Preview
@Composable
private fun CartLineItemPreview() {
    val items = listOf(
        Item(
            "1",
            title = "A longish product title that should wrap",
            price = 599.99,
            quantity = 32
        ),
        Item(
            "2",
            title = "A longish product title that should wrap",
            price = 599.99,
            quantity = 32
        )
    )

    AppTheme {
        CartScreen.CartView(cart = Cart(items), updateItem = {}, onContinue = {})
    }
}
