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

## Import strategy
On first launch:
1. read bundled JSON files
2. map them into entities
3. seed Room database
4. skip reseeding if already imported
