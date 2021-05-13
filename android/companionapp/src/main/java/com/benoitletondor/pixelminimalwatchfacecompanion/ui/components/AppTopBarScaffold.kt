package com.benoitletondor.pixelminimalwatchfacecompanion.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.benoitletondor.pixelminimalwatchfacecompanion.ui.productSansFontFamily

@Composable
fun AppTopBarScaffold(
    navController: NavController,
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    @Composable
    fun NavigationIcon(navController: NavController): @Composable (() -> Unit)? {
        return navController.previousBackStackEntry?.let {
            {
                IconButton(onClick = {
                    navController.popBackStack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Up button")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontFamily = productSansFontFamily,
                    )
                },
                actions = actions,
                navigationIcon = NavigationIcon(navController),
                elevation = 0.dp,
            )
        },
        content = content
    )
}