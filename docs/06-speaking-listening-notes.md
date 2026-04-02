# Speaking and Listening Notes

## Speaking
Recommended Android components:
- SpeechRecognizer for speech-to-text
- runtime microphone permission
- optional TextToSpeech for model prompts

### Safe v1 speaking flow
1. Show prompt
2. Start listening
3. Capture transcript
4. Save transcript as user answer
5. Generate rule-based feedback
6. Push weak tags into review queue

### Feedback examples
- too short
- missing connector
- tense inconsistency
- basic vocabulary repeated too often
- answer not fully on topic

## Listening
Recommended Android components:
- Media3 / ExoPlayer for audio playback

### Listening flow
1. Play audio
2. User answers questions or summary
3. Save result
4. Add weak tags to review queue
