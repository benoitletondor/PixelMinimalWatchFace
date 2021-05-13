package com.benoitletondor.pixelminimalwatchfacecompanion.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun AppTopBarMoreMenuItem(content: @Composable ColumnScope.() -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { showMenu = true }
            .padding(all = 8.dp),
    ) {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Menu",
            tint = MaterialTheme.colors.onBackground,
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            content = content,
        )
    }
}