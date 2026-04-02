package com.example.englishpractice.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ContentProvenanceBlock(
    sourceLabel: String,
    collectionTitle: String? = null,
    unitTitle: String? = null,
    currentTitle: String? = null,
    modifier: Modifier = Modifier
) {
    val normalizedCollection = collectionTitle?.takeIf { title -> title.isNotBlank() }
    val normalizedCurrentTitle = currentTitle?.takeIf { title -> title.isNotBlank() }
    val normalizedUnit = unitTitle
        ?.takeIf { title -> title.isNotBlank() }
        ?.takeIf { title -> title != normalizedCollection }
        ?.takeIf { title -> title != normalizedCurrentTitle }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        normalizedCollection?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        normalizedUnit?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = "Source: $sourceLabel",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
