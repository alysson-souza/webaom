# WebAOM Agent Instructions

## Project Overview

WebAOM (Web Anime-o-Matic) is a Java Swing desktop application for identifying anime files via AniDB's UDP API.

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

- **Entry Point**: `src/main/java/epox/webaom/WebAOM.java` - Swing launcher
- **Global State**: `AppContext.java` - Static singleton with all subsystems
- **Job System**: `Job.java` with bitmask-based state machine
- **Network Layer**: `AniDBConnection.java` / `AniDBFileClient.java`

### Package Structure

- `epox.webaom/` - Core application logic
- `epox.webaom.data/` - Data models (`AniDBFile`, `Anime`, `Episode`, `Group`)
- `epox.webaom.net/` - AniDB UDP protocol implementation (see `@docs/udp-api-definition.md` for protocol spec)
- `epox.webaom.ui/` - Swing UI components (JPanel*, JTable*, JDialog*)
- `epox.webaom.ui.actions.jobs/` - UI-decoupled job action orchestration (commands, gateway, controller)
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
2. `README.md` - Update JAR filename references
3. `changelog.txt` - Add entry at top: `X.Y.Z DD.MM.YYYY:` (user-facing, non-technical wording)
4. `src/main/resources/info.txt` - Add entry at top: `X.Y.Z YYYY.MM.DD` (user-facing, non-technical wording)

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
