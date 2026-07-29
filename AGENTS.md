# WebAOM Agent Instructions

## Project Overview

WebAOM (Web Anime-o-Matic) is a Java application for identifying anime files via AniDB's UDP API. It ships a Swing
desktop app and a command-line interface that share the same configuration, database, credentials, and rules.

## Build Commands

```bash
./gradlew build              # Compile + create fat JARs (lite and full)
./gradlew run                # Run application
./gradlew jarLite            # Create lite JAR (SQLite only)
./gradlew jarFull            # Create full JAR (all DB drivers)
./gradlew test               # Run non-UI tests (excludes @Tag("ui"))
./gradlew testUi             # Run UI-tagged tests (@Tag("ui"), headless)
./gradlew test --tests "ClassName"   # Run single test class
./gradlew test --tests "ClassName.methodName"  # Run single test method
./gradlew spotlessApply      # Format code (Palantir Java Format)
./gradlew spotlessCheck      # Check formatting without applying changes
./gradlew jpackage           # Create native installer (.dmg/.msi/.deb)
./gradlew jpackageAppImage   # Create portable app bundle
./gradlew jlink              # Create minimal custom JRE (~51MB)
./gradlew appimage           # Create AppImage for Linux (requires appimagetool)
```

## Architecture

### Core Components

- **Entry Point**: `src/main/java/epox/webaom/WebAOM.java` - Swing launcher; dispatches to the CLI when given arguments
- **CLI Entry Point**: `src/main/java/epox/webaom/cli/Cli.java` - picocli command tree
- **Global State**: `AppContext.java` - Static singleton with all subsystems
- **Job System**: `Job.java` with bitmask-based state machine
- **Network Layer**: `AniDBConnection.java` / `AniDBFileClient.java`

### Package Structure

- `epox.webaom/` - Core application logic
- `epox.webaom.data/` - Data models (`AniDBFile`, `Anime`, `Episode`, `Group`)
- `epox.webaom.net/` - AniDB UDP protocol implementation (see `@docs/udp-api-definition.md` for protocol spec)
- `epox.webaom.ui/` - Swing UI components (JPanel*, JTable*, JDialog*)
- `epox.webaom.ui.actions.jobs/` - UI-decoupled job action orchestration (commands, gateway, controller)
- `epox.webaom.cli/` - Command-line interface (`Cli` commands, `CliApplication` headless orchestration, `RuleDocument`)
- `epox.webaom.db/` - Database management
- `epox.webaom.job/` - Job processing system
- `epox.util/` - Utilities and hashing
- `epox.av/` - Audio/video file info parsing
- `epox.swing/` - Reusable Swing components

### Vendored Code (DO NOT MODIFY)

Third-party code in `com/`, `gnu/`, `jonelo/` packages excluded from formatting:
- `com.bitzi.util` - Base32 encoding
- `com.twmacinta` - Fast MD5 implementation
- `gnu.crypto.hash` - Tiger hash
- `jonelo.jacksum` - Checksum algorithms

## Code Style

### Formatting

- **Formatter**: Palantir Java Format via Spotless (only `epox/**/*.java`)
- **Line length**: 120 characters
- **Indentation**: 4 spaces
- **Encoding**: UTF-8
- **Java version**: 21


Run `./gradlew spotlessApply` before committing.

### Naming Conventions

- **Classes**: PascalCase (`JobProcessor`, `AniDBConnection`)
- **Interfaces**: PascalCase with descriptive names
- **Methods**: camelCase (`processFile()`, `getConnection()`)
- **Variables**: camelCase (`fileId`, `jobCount`)
- **Constants**: UPPER_SNAKE_CASE with bitwise flags for state machines
- **Static final fields**: UPPER_SNAKE_CASE
- **Packages**: lowercase (`epox.webaom.data`)
- **UI Classes**: Prefix with type (`JPanel*`, `JTable*`, `JDialog*`)

### Error Handling

- Use checked exceptions for recoverable errors
- Log errors via `Logger.getLogger(Class.class.getName())`
- UI errors: `AppContext.dialog("Title", message)` for user-facing messages
- Print stack traces only in development: `e.printStackTrace()`

### Comments

- GPL license header required on all files
- Javadoc for public APIs and data classes
- Use `//` for inline comments, not `/* */`

## Key Implementation Details

### Configuration

User settings follow XDG Base Directory (auto-migrated from legacy paths):

| Platform | Config Directory                          | Files                  |
|----------|-------------------------------------------|------------------------|
| Linux    | `$XDG_CONFIG_HOME/webaom/` (~/.config/)   | `config`, `template.htm` |
| macOS    | `~/Library/Application Support/webaom/`   | `config`, `template.htm` |
| Windows  | `%APPDATA%\webaom\`                       | `config`, `template.htm` |

### State Machine Constants (Job.java)

Status flags use hexadecimal bitmasks:
- `S_DONE = 0x00000001`, `S_DO = 0x00000002`, `S_DOING = 0x00000014`
- `H_NORMAL = 0x00000100`, `H_PAUSED = 0x00000200`
- `D_DIO = 0x00010000` (disk I/O), `D_NIO = 0x00020000` (network I/O)

### Database Migrations (DatabaseManager.java)

Schema SQL lives in `src/main/resources/`. Each dialect manager lists its files in `SQL_FILES`: index 0
creates a current-version schema, index N upgrades version N-1 to N. Suffixes select the dialect
(`a` PostgreSQL, `b` MySQL, `c` SQLite; unsuffixed files are shared).

To add schema version N:

1. Add the upgrade file for each dialect and append it to every `SQL_FILES`.
2. End each upgrade file with `UPDATE vtb SET ver=N;`.
3. Bump the `INSERT INTO vtb VALUES (...)` at the end of `db00.sql` and `db00c.sql` to N.

`getSupportedSchemaVersion()` derives from `SQL_FILES.length`, so a database whose `vtb.ver` exceeds it
is rejected as coming from a newer build.

Rules for the code:

- Run schema SQL only through `executeSchemaBatch`, which throws on the first failing statement.
  Continuing past a failure reaches the file's own `UPDATE vtb SET ver=N` and records the database as
  migrated without the columns `SQL_JOB_QUERY` selects, which no later run can detect or repair.
- Wrap schema work in `applySchemaChanges`. It opens a transaction where
  `supportsTransactionalSchemaChanges()` is true (SQLite, PostgreSQL) and runs statements directly
  where it is false (MySQL, which commits implicitly on DDL).
- Express dialect differences by overriding `prepareSchemaSql` or
  `supportsTransactionalSchemaChanges`. Do not override `updateSchema`.
- Headless callers must call `setUiPromptsAvailable(false)`, otherwise `AppContext.confirm` and
  `AppContext.dialog` print dialog text to stderr. Such callers report outcomes themselves from
  `hasUnsupportedFutureSchema()`, `hasUnsupportedLegacySchema()`, `isLegacySchemaUpgradeDeclined()`
  and `getSchemaFailureDetail()`.
- Grant legacy-schema consent by installing a `setLegacySchemaUpgradeConfirmationSource(...)`; with
  none set, a pre-v4 schema falls back to the desktop dialog.

### Rules Grammar (Rules.java / RuleDocument.java)

`Rules` executes rename and move scripts; `cli/RuleDocument` validates them for `rules check` and `rules edit`. The
two hold the grammar independently, so a change to one needs a matching change to the other.

- The validator must not reject what the engine accepts. `Rules.buildSections` skips unrecognized lines, and its
  tokenizers drop empty condition groups (`;`) and values (`,`), so `RuleDocument` accepts all of these.
- Both find the `DO` separator via `Rules.indexOfConditionSeparator`, which ignores `DO` inside parentheses so
  condition values and regexes may contain the word "do".
- `processRulesScript` catches everything and returns null, which silently disables the whole script. Parsing
  helpers reached from `Section` must therefore tolerate legacy clauses rather than throw.

### Testing

- Automated tests exist under `src/test/java`.
- `./gradlew test` runs non-UI tests (default CI-safe path).
- `./gradlew testUi` runs UI-tagged tests in headless mode.
- For focused runs, use `./gradlew test --tests "ClassName"` or `./gradlew test --tests "ClassName.methodName"`.

## Native Packaging

Output locations:
- `build/jlink/` - Minimal custom JRE
- `build/installer/` - Native packages
- `build/libs/` - JAR files (webaom-{version}-lite.jar, webaom-{version}-full.jar)

### macOS Notarization

App is **not notarized**. Users must bypass Gatekeeper:
```bash
xattr -cr /Applications/WebAOM.app
```

## Version Bumping

Update these files when bumping version:
1. `build.gradle` - Update `version = 'X.Y.Z'`
2. `README.md` - Update release artifact filename references (native packages and JARs)
3. `changelog.txt` - Add entry at top: `X.Y.Z DD.MM.YYYY:` (user-facing, non-technical wording)
4. `src/main/resources/info.txt` - Add entry at top: `X.Y.Z YYYY.MM.DD` (user-facing, non-technical wording)

## Release Candidates

RC (release candidate) builds follow different rules than final releases:

- **Version format**: Use GitHub release tags like `v2.5.0-rc.1`, `v2.5.0-rc.2` (not `v2.5.0-rc2`)
- **DO NOT edit version files** for RC releases: `build.gradle`, `changelog.txt`, and `info.txt` should stay on the main target version (`X.Y.Z`) and use the unreleased top entry until the final release. These are only finalized for the stable release.
- **Changelog source**: Compare `changelog.txt` at previous RC tag vs HEAD to find new entries (e.g., `git show v2.5.0-rc.1:changelog.txt` vs current `changelog.txt`).
- **Cumulative changelogs**: When creating RC2, RC3, etc., include ALL previous RC changelog entries, not just new ones. Look at the previous release's changelog and carry it forward.
- **Release creation only**: When asked to "create a release" or "release an RC", create the GitHub release with `gh release create`. Do NOT manually create git tags - the release creation handles the tag automatically.

Example RC2 release:
```bash
gh release create v2.5.0-rc.2 --title "v2.5.0-rc.2" --notes "## Changelog

**2.5.0-rc2 (pre-release):**
* [ALL previous RC1 entries here]
* [New RC2 entries here]

**Full Changelog**: https://github.com/alysson-souza/webaom/compare/v2.5.0-rc.1...v2.5.0-rc.2" --prerelease
```

## Git Conventions

Use **conventional commits**:
```
type(scope): subject line

Optional body explaining the change in detail.
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
- `fix(ci): remove Azul vendor requirement from toolchain`
- `chore(spotless): migrate to Palantir Java Format`
- `style: reformat code with Palantir formatter`
