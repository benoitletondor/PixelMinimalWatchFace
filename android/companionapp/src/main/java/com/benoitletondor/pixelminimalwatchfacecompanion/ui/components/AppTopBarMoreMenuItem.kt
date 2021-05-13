package com.benoitletondor.pixelminimalwatchfacecompanion.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
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

    Box {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Menu",
            modifier = Modifier
                .padding(6.dp)
                .clip(CircleShape)
                .clickable { showMenu = true },
            tint = MaterialTheme.colors.onBackground,
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            content = content,
        )
    }
}