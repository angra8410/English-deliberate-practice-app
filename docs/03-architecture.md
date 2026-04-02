# Architecture

## Recommended architecture
- Presentation: Compose + ViewModel + StateFlow
- Domain: core models and use cases
- Data: Room entities, DAOs, repositories, asset importers

## Top-level app structure
- app shell with Scaffold
- bottom navigation:
  - Home
  - Practice
  - Review
  - Progress
  - Settings

## Separation of concerns
### Engine
Reusable logic:
- attempts
- review scheduling
- scoring
- streaks
- recommendations
- navigation
- progress calculations

### Content
Replaceable/extensible:
- levels
- units
- activities
- prompts
- answer keys
- skill tags
- explanations
- audio references

## Why this matters
This lets you begin with B2/C1 and later add A1/A2/B1/B2 without redesigning the app.
