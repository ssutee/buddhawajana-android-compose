package com.watnapp.buddhawajana.core.ui.nav

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable

@Composable
fun BuddhawajanaNavSuite(
    selected: TopDestination,
    onSelect: (TopDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopDestination.entries.forEach { dest ->
                item(
                    selected = dest == selected,
                    onClick = { onSelect(dest) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                )
            }
        },
        content = content,
    )
}
