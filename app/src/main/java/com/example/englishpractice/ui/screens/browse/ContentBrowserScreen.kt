package com.example.englishpractice.ui.screens.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.ContentBrowserItem

private const val ALL_SOURCES = "All sources"
private const val ALL_SKILLS = "All skills"

@Composable
fun ContentBrowserScreen(
    state: AppUiState,
    onActivitySelected: (String) -> Unit
) {
    var selectedSource by rememberSaveable { mutableStateOf(ALL_SOURCES) }
    var selectedSkill by rememberSaveable { mutableStateOf(ALL_SKILLS) }

    val sourceCounts = state.contentBrowserItems
        .groupingBy(ContentBrowserItem::sourceLabel)
        .eachCount()
    val sourceOptions = listOf(ALL_SOURCES) + sourceCounts.keys.sorted()

    val sourceFilteredItems = state.contentBrowserItems.filter { item ->
        selectedSource == ALL_SOURCES || item.sourceLabel == selectedSource
    }
    val skillCounts = sourceFilteredItems
        .groupingBy(ContentBrowserItem::skill)
        .eachCount()
    val skillOptions = listOf(ALL_SKILLS) + SkillType.entries
        .filter(skillCounts::containsKey)
        .map(SkillType::name)

    val normalizedSelectedSkill = selectedSkill.takeIf { skill ->
        skill == ALL_SKILLS || skillOptions.contains(skill)
    } ?: ALL_SKILLS
    if (normalizedSelectedSkill != selectedSkill) {
        selectedSkill = normalizedSelectedSkill
    }

    val filteredItems = sourceFilteredItems.filter { item ->
        normalizedSelectedSkill == ALL_SKILLS || item.skill.name == normalizedSelectedSkill
    }
    val groupedItems = filteredItems.groupBy(ContentBrowserItem::skill)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Browse content", style = MaterialTheme.typography.headlineMedium)
        Text("Explore all ${state.currentLevel} activities and open a specific prompt directly.")

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Catalog overview", style = MaterialTheme.typography.titleMedium)
                Text("Visible activities: ${filteredItems.size}")
                Text("Available sources: ${state.contentBrowserItems.map { it.sourceLabel }.distinct().size}")
                Text("Current level: ${state.currentLevel}")
            }
        }

        Text("Filter by source", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sourceOptions.forEach { source ->
                val sourceCount = if (source == ALL_SOURCES) {
                    state.contentBrowserItems.size
                } else {
                    sourceCounts[source] ?: 0
                }
                FilterChip(
                    selected = selectedSource == source,
                    onClick = { selectedSource = source },
                    label = { Text("$source ($sourceCount)") }
                )
            }
        }

        Text("Filter by skill", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            skillOptions.forEach { skill ->
                val skillCount = if (skill == ALL_SKILLS) {
                    sourceFilteredItems.size
                } else {
                    skillCounts[SkillType.valueOf(skill)] ?: 0
                }
                FilterChip(
                    selected = normalizedSelectedSkill == skill,
                    onClick = { selectedSkill = skill },
                    label = {
                        Text("${skillLabel(skill)} ($skillCount)")
                    }
                )
            }
        }

        if (filteredItems.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("No content matches the current filters.")
                }
            }
        } else {
            SkillType.entries.forEach { skill ->
                val skillItems = groupedItems[skill].orEmpty()
                if (skillItems.isEmpty()) return@forEach

                Text(skill.label, style = MaterialTheme.typography.titleMedium)
                skillItems.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onActivitySelected(item.activityId) }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            if (item.unitTitle != item.title) {
                                Text(
                                    text = item.unitTitle,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(item.focus, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = "${item.exerciseType}  |  Source: ${item.sourceLabel}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = abbreviate(item.promptPreview),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun abbreviate(text: String, maxLength: Int = 180): String {
    val normalizedText = text.replace(Regex("\\s+"), " ").trim()
    return if (normalizedText.length <= maxLength) {
        normalizedText
    } else {
        normalizedText.take(maxLength - 1).trimEnd() + "..."
    }
}

private fun skillLabel(skill: String): String {
    return if (skill == ALL_SKILLS) {
        skill
    } else {
        SkillType.valueOf(skill).label
    }
}
