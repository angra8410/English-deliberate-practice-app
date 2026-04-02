# Data Model

## Core conceptual models

### Level
Represents CEFR stage.
Examples:
- A1
- A2
- B1
- B2
- C1

### Skill
- READING
- WRITING
- LISTENING
- SPEAKING

### ExerciseType
- MULTIPLE_CHOICE
- FILL_IN_BLANK
- OPEN_TEXT
- SPEAK_RESPONSE
- LISTEN_AND_SUMMARIZE
- READ_AND_SUMMARIZE
- ERROR_CORRECTION
- SENTENCE_TRANSFORMATION

### Unit
A curriculum grouping.
Examples:
- B2 / Writing / Opinion Paragraphs
- C1 / Vocabulary / Formal Register
- A2 / Grammar / Past Simple Basics

### Activity
A single task shown to the user.

### Attempt
A user response to one activity.

### Mistake
A tagged weakness extracted from an attempt.

### ReviewItem
A scheduled retry item for deliberate practice.

## Key scalability rule
Never hardcode logic for only one level or one book.
Store level, skill, tags, and activity metadata explicitly.
