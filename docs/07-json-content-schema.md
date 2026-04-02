# JSON Content Schema

Use JSON files in assets for initial content packs.

## Example structure
- levels.json
- units_b2.json
- units_c1.json
- activities_b2.json
- activities_c1.json

## Suggested fields for an activity
- id
- level
- skill
- unitId
- exerciseType
- title
- prompt
- instructions
- tags
- difficulty
- expectedAnswer
- sampleAnswer
- choices
- explanation
- audioAsset
- transcript
- reviewWeight

## Suggested fields for book-catalog practice prompts
- id
- type
- targetSkill
- prompt
- instructions
- starterText
- modelAnswer
- expectedKeywords
- scoringProfile
- minimumWordCount
- minimumResponseItems

## Writing scoring profiles
- `default`: open response with normal length and target-language checks
- `list`: phrase-list task with item-count checks
- `sentence_drill`: sentence-pair or repeated production drill
- `rewrite`: rewrite/error-correction style task

## Generator workflow
1. create or export a raw book catalog JSON
2. keep prompt-specific metadata in `tools/content_metadata_overrides.json`
3. run `tools/generate_content_repository.py`
4. write the result to `app/src/main/assets/content/content_repository.json`
5. run the app tests to verify the new asset still parses

## Import strategy
On first launch:
1. read bundled JSON files
2. map them into entities
3. seed Room database
4. skip reseeding if already imported
