# PlanEat AI Coding Agent Instructions

## Project Overview

**PlanEat** is a federated meal planning application with three major components:
- **Android App** (Kotlin/Compose) - UI for recipe discovery, meal scheduling, ingredient parsing
- **Rust Server** - Enables account linking and agenda/shopping list synchronization between users
- **Python ML Models** - Recipe classification and ingredient parsing

Focus areas for most tasks: `app/src/main/java/com/planeat/planeat/` (core business logic)

## Architecture Essentials

### Three-Layer App Design (`app/src/main/java/com/planeat/planeat/`)

1. **Data Layer** (`data/`)
   - **Room Databases**: `RecipesDb`, `AgendaDb`, `IngredientsDb` with DAOs and TypeConverters
   - **Models**: `Recipe` (URL-keyed, Serializable), `Agenda`, `IngredientItem`, `Account`
   - No REST layer - all external data flows through Connectors
   - All models use `mutableStateOf`/`mutableStateListOf` for Compose state reactivity

2. **Connector Layer** (`connectors/`)
   - Abstract `Connector` base class with implementations: `Ricardo`, `Marmiton`, `Nytimes`, `ChaCuit`
   - Each connector implements: `handleUrl()`, `search()`, `getRecipe()`, `parsePages()`, `suggest()`
   - Uses JSoup for HTML parsing; handles JSON extraction from React-bridged pages (Ricardo)
   - Always validates recipe titles before emitting to prevent empty entries

3. **UI Layer** (`ui/`)
   - **Compose-first** architecture with Material3 theme
   - **Central state holder**: `AppModel` class manages all app state, recipes lists, selected dates, account info
   - **Navigation**: File-based routing via `PlanEatNavigationActions` and `PlanEatRoute`
   - **Key screens**: `DiscoverScreen`, `AgendaScreen`, `ShoppingScreen`, `RecipeDetailScreen`, `EditRecipeScreen`

### AI Enhancements in App

- **`IngredientClassifier`** (TensorFlow Lite) - Categorizes ingredients on-device
- **`TagClassifier`** (TensorFlow Lite) - Classifies recipes by meal type on-device
- No external API calls for classification - runs fully offline

## Critical Developer Workflows

### Build & Run
```bash
# Android Studio: Import project, sync Gradle, run via IDE
# Or from CLI:
./gradlew build        # Compile debug APK
./gradlew assembleRelease  # Release build with ProGuard
```

### Database Migrations
- Room schemas auto-generated to `app/schemas/` on compile
- Schema files are **versioned in git** - breaking changes require version bump + migration
- Use `@Database(version = N)` and `@Migration` annotations for schema evolution

### Adding New Recipe Connector
1. Create class extending `Connector` in `connectors/`
2. Implement abstract methods: `handleUrl()`, `getRecipe()`, `search()`, `parsePages()`
3. Register in `AppModel.connectors` list (~line 324 in PlanEatApp.kt)
4. Handle timeouts and malformed JSON gracefully (try-catch with logging)

### Testing Structure
- **Shared test code**: `app/src/sharedTest/java/` runs in both unit and instrumented contexts
- **Unit tests** use Robolectric (no device required)
- **Instrumented tests** run on Android device/emulator
- Enable resource loading: `isIncludeAndroidResources = true` in build.gradle.kts

## Project-Specific Patterns

### State Management
- Use `mutableStateOf()` for single values, `mutableStateListOf()` for lists (not mutableListOf)
- AppModel lists: `suggestedRecipesShown`, `recipesSearchedShown`, `recipesInDbShown` (Compose-reactive)
- Filtering/searching triggers `LaunchedEffect` with debounce: search waits 300ms before querying

### Async Operations
- Use `CoroutineScope(Dispatchers.IO).launch {}` for background work (recipe fetching)
- Store Job references in `searchJobs` list to cancel on screen exit
- Main UI updates dispatch back to `Dispatchers.Main` implicitly via state mutation

### Recipe Data Flow
1. **Search/Suggest** → Connector scrapes external site via JSoup
2. **Parse** → Connector extracts Recipe object and runs ML classifiers
3. **Store** → Recipe saved to RecipesDb via RecipeDao
4. **Display** → RecipeListItem component renders with edit/delete/plan actions

### Key Idioms
- **Recipe identity**: Always use `recipe.url` as unique key in LazyColumn/LazyRow `.items()` calls
- **Dividers**: Insert `HorizontalDivider(color = surfaceLight)` after each RecipeListItem
- **Navigation state**: LocalDate selection stored in `AppModel.selectedDate` for agenda views
- **Theming**: Use semantic colors from `com.example.compose` (e.g., `primaryLight`, `surfaceContainerLowestLight`)

## External Dependencies & Boundaries

### Key Libraries
- **Compose UI**: Material3 components, WindowSizeClass for adaptive layouts
- **Room**: SQLite with type converters for List<String> and IngredientItem serialization
- **JSoup**: HTML/XML parsing with configurable timeouts (2000ms standard)
- **TensorFlow Lite**: On-device ML inference (no internet required)
- **Coil**: Image loading from URLs
- **Kotlin Serialization**: JSON serialization/deserialization (kotlinx-serialization)

### Server Communication (Rust Server)
- Runs on port 8080 (configured in docker/Dockerfile)
- Handles account linking via QR codes with ed25519 public keys
- Stores sync messages in `$sha256(public_key)/sync.db`
- Client does not auto-sync - user manually triggers via account linking UI

### Model Generation
- **Recipe tags**: Train classifier in `models/tag_classifier/` then deploy .tflite file
- **Ingredient parsing**: BERT QA model in `models/ingredients_parser/` for natural language extraction
- Models deployed as app assets (no_compress in build.gradle.kts)

## Debugging Tips

- **Search failures**: Check connector logs; compare JSoup HTML structure with website updates
- **Missing ingredients**: Verify IngredientItem parsing in `data/IngredientItem.kt` line ~149
- **Stale UI state**: Ensure state mutations use reactive types (mutableStateOf, mutableStateListOf), not regular lists
- **Room schema conflicts**: Delete app data, rebuild with `./gradlew clean assembleDebug`, check `app/schemas/`
- **ML inference errors**: Verify .tflite files exist in assets folder and are not compressed (`androidResources.noCompress`)

## File Cross-References for Common Tasks

| Task | Key Files |
|------|-----------|
| Add recipe source | `connectors/Ricardo.kt`, `PlanEatApp.kt` line ~100 |
| Modify recipe fields | `data/Recipe.kt`, `data/Converters.kt` |
| New agenda view | `ui/AgendaScreen.kt`, `components/calendar/` |
| Ingredient categorization | `data/IngredientItem.kt`, `ui/utils/IngredientClassifier.kt` |
| Account linking UI | `ui/AccountScreen.kt`, query `AppModel.account` |

## Version Constraints

- **Kotlin**: 2.2.10
- **Compose**: 2024.03.00 (Material3 1.4.0-alpha15)
- **Room**: 2.6.0
- **Target Android**: API 33–36
- **Minimum Android**: API 25

