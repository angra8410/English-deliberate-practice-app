package com.example.englishpractice.feature.listening

data class ListeningCapability(
    val playbackEngine: String,
    val supportsBundledAudio: Boolean,
    val supportsPromptPlayback: Boolean,
    val workflowSteps: List<String>
)

class ListeningPlayer {
    fun capability(): ListeningCapability {
        return ListeningCapability(
            playbackEngine = "Media3 ExoPlayer",
            supportsBundledAudio = true,
            supportsPromptPlayback = true,
            workflowSteps = listOf(
                "play audio",
                "capture comprehension response",
                "save summary or answers",
                "tag weak details for review"
            )
        )
    }
}
