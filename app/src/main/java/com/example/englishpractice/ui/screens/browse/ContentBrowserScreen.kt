package com.example.englishpractice.ui.screens.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.englishpractice.domain.model.SkillType
import com.example.englishpractice.ui.app.AppUiState
import com.example.englishpractice.ui.app.ContentBrowserItem
import com.example.englishpractice.ui.components.ContentProvenanceBlock
import com.example.englishpractice.ui.components.skillTone

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
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        BrowserHero(state = state, visibleCount = filteredItems.size)
        FilterPanel(
            title = "Source",
            options = sourceOptions,
            selectedOption = selectedSource,
            countForOption = { option ->
                if (option == ALL_SOURCES) state.contentBrowserItems.size else sourceCounts[option] ?: 0
            },
            labelForOption = { it },
            onOptionSelected = { selectedSource = it }
        )
        FilterPanel(
            title = "Skill",
            options = skillOptions,
            selectedOption = normalizedSelectedSkill,
            countForOption = { option ->
                if (option == ALL_SKILLS) sourceFilteredItems.size else skillCounts[SkillType.valueOf(option)] ?: 0
            },
            labelForOption = ::skillLabel,
            onOptionSelected = { selectedSkill = it }
        )

        if (filteredItems.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("No content matches the current filters.")
                }
            }
        } else {
            SkillType.entries.forEach { skill ->
                val skillItems = groupedItems[skill].orEmpty()
                if (skillItems.isEmpty()) return@forEach

                SectionHeading(
                    eyebrow = skill.label,
                    title = "${skillItems.size} visible activities",
                    description = skill.deliberatePracticeFocus
                )
                skillItems.forEach { item ->
                    BrowserItemCard(
                        item = item,
                        onActivitySelected = onActivitySelected
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowserHero(
    state: AppUiState,
    visibleCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Content browser",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Browse the catalog by source, skill, and effort without losing the context behind each prompt.",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Current level ${state.currentLevel}  |  Visible activities $visibleCount  |  Sources ${state.contentBrowserItems.map { it.sourceLabel }.distinct().size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilterPanel(
    title: String,
    options: List<String>,
    selectedOption: String,
    countForOption: (String) -> Int,
    labelForOption: (String) -> String,
    onOptionSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selectedOption == option,
                    onClick = { onOptionSelected(option) },
                    label = { Text("${labelForOption(option)} (${countForOption(option)})") }
                )
            }
        }
    }
}

@Composable
private fun BrowserItemCard(
    item: ContentBrowserItem,
    onActivitySelected: (String) -> Unit
) {
    val tone = skillTone(item.skill)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onActivitySelected(item.activityId) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(tone.gradient)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = tone.accent
                    )
                    Text(
                        text = item.focus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ContentProvenanceBlock(
                sourceLabel = item.sourceLabel,
                collectionTitle = item.collectionTitle,
                unitTitle = item.unitTitle,
                currentTitle = item.title
            )

            Text(
                text = "${item.exerciseType}  |  ${item.effortLabel}  |  Target ${item.responseTargetLabel}",
                style = MaterialTheme.typography.labelLarge
            )

            if (item.tags.isNotEmpty() || item.difficulty != null) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item.difficulty?.let { difficulty ->
                        AssistChip(
                            onClick = {},
                            label = { Text("Difficulty ${difficultyLabel(difficulty)}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = tone.soft,
                                labelColor = tone.accent
                            )
                        )
                    }
                    item.tags.take(4).forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(tagLabel(tag)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Text(
                text = abbreviate(item.promptPreview),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SectionHeading(
    eyebrow: String,
    title: String,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = eyebrow,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

private fun tagLabel(tag: String): String {
    return tag.split('-', '_')
        .filter(String::isNotBlank)
        .joinToString(" ") { part ->
            part.replaceFirstChar { char -> char.uppercase() }
        }
}

private fun difficultyLabel(difficulty: Int): String = "$difficulty/4"
