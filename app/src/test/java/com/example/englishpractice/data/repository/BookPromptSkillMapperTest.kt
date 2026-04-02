package com.example.englishpractice.data.repository

import com.example.englishpractice.domain.model.ExerciseType
import com.example.englishpractice.domain.model.SkillType
import kotlin.test.Test
import kotlin.test.assertEquals

class BookPromptSkillMapperTest {
    @Test
    fun `maps vocabulary open text prompts to writing`() {
        val appSkill = BookPromptSkillMapper.toAppSkill(
            sourceTargetSkill = SourceTargetSkill.VOCABULARY,
            promptType = ExerciseType.OPEN_TEXT
        )

        assertEquals(SkillType.WRITING, appSkill)
    }

    @Test
    fun `maps vocabulary recognition prompts to reading`() {
        val appSkill = BookPromptSkillMapper.toAppSkill(
            sourceTargetSkill = SourceTargetSkill.VOCABULARY,
            promptType = ExerciseType.FILL_IN_BLANK
        )

        assertEquals(SkillType.READING, appSkill)
    }

    @Test
    fun `keeps direct source target skills unchanged`() {
        assertEquals(
            SkillType.LISTENING,
            BookPromptSkillMapper.toAppSkill(
                sourceTargetSkill = SourceTargetSkill.LISTENING,
                promptType = ExerciseType.LISTEN_AND_SUMMARIZE
            )
        )
        assertEquals(
            SkillType.SPEAKING,
            BookPromptSkillMapper.toAppSkill(
                sourceTargetSkill = SourceTargetSkill.SPEAKING,
                promptType = ExerciseType.SPEAK_RESPONSE
            )
        )
    }
}
