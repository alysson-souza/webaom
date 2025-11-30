# WebAOM Agent Instructions

## Project Overview
WebAOM (Web Anime-o-Matic) is a legacy Java Swing desktop application for identifying anime files via AniDB's UDP API. Originally developed 2005-2010, revived in 2025 for maintenance.

## Build Commands
```bash
./gradlew build          # Compile + create fat JAR with dependencies
./gradlew run            # Run application
./gradlew spotlessApply  # Format code (Eclipse formatter)
./gradlew spotlessCheck  # Check formatting without applying changes
./gradlew checkstyleMain # Check code style
```

## Architecture

### Core Components
- **Entry Point**: `src/main/java/epox/webaom/WebAOM.java` - Swing JApplet/JFrame launcher
- **Global State**: `AppContext.java` - Static singleton holding all subsystems (`AppContext.databaseManager`, `AppContext.nio`, `AppContext.dio`, `AppContext.jobs`, `AppContext.gui`, etc.)
- **Job System**: `Job.java` with bitmask-based state machine (status + health flags)
- **Network Layer**: `AniDBConnection.java` / `AniDBFileClient.java` - AniDB UDP API client and high-level file/MyList operations (encryption/compression supported)

### Package Structure
- `epox.webaom/` - Core application logic
- `epox.webaom.data/` - Data models (`AniDBFile`, `Anime`, `Episode`, `Group`)
- `epox.webaom.net/` - AniDB UDP protocol implementation
- `epox.webaom.ui/` - Swing UI components (JPanel*, JTable*, JDialog*)
- `epox.util/` - Utilities and hashing
- `epox.av/` - Audio/video file info parsing
- `epox.swing/` - Reusable Swing components

### Vendored Code (DO NOT MODIFY)
Third-party code in `com/`, `gnu/`, `jonelo/` packages is excluded from formatting/linting:
- `com.bitzi.util` - Base32 encoding
- `com.twmacinta` - Fast MD5 implementation
- `gnu.crypto.hash` - Tiger hash
- `jonelo.jacksum` - Checksum algorithms

## Code Style

### Formatting
- Eclipse formatter via Spotless - only applies to `epox/**/*.java`
- Configuration: `config/eclipse-formatter.xml`
- Tabs for indentation (size 4), line length 120 chars
- UTF-8 encoding for all Java files
- Java 21 toolchain
- No star imports, unused imports removed automatically
- Run `./gradlew spotlessApply` before committing

### Legacy Patterns (To be improved as you go)
- Global state via `AppContext` static fields (historically `A`)
- Some legacy files may use Hungarian-ish prefixes: `m_` (member), `mI` (int), `mS` (string), `mB` (boolean)
- Bitmask constants for Job states: `S_DONE`, `H_PAUSED`, `D_DIO`, etc.
- Static field access via `AppContext.` class (e.g., `AppContext.jobs`, `AppContext.opt`, `AppContext.gui`)

## Key Implementation Details

### Configuration
- User settings stored in `~/.webaom` (UTF-8 encoded)
- HTML template override: `~/.webaom.htm`
- Options managed via `Options.java` with arrays (`booleanOptions[]`, `integerOptions[]`, `stringOptions[]`)

### AniDB UDP Protocol
- Default server: `api.anidb.net:9000`
- Session-based auth with optional AES encryption
- Rate limiting handled internally (configurable delay between packets)

### File Processing
- Multi-algorithm hashing: ED2K, MD5, SHA-1, CRC32, Tiger Tree Hash
- Template-based file renaming with tags (`%ann`, `%epn`, `%grp`, etc.)
- Conditional rules defined in `Rules.java`

## Testing
No automated test suite exists. Test manually by running the application.

## Version Bumping

To bump the project version, update these files:

1. **build.gradle** - Update `version = 'X.Y.Z'`
2. **README.md** - Update JAR filename references (e.g., `webaom-X.Y.Z.jar`)
3. **changelog.txt** - Add entry at top: `X.Y.Z DD.MM.YYYY:` followed by changes
4. **src/main/resources/info.txt** - Add entry at top: `X.Y.Z YYYY.MM.DD` followed by changes

Note: `version.properties` is auto-generated during build from `build.gradle`.

## Git Conventions

### Commit Style
Use **conventional commits** with optional scope and body:
```
type(scope): subject line

Optional body explaining the change in detail.
```

Examples from this repo:
- `fix(ci): remove Azul vendor requirement from toolchain`
- `chore(spotless): update Eclipse formatter configuration`
- `style: fix checkstyle warnings`

### Git Commands
- If you need to see the git log history, use `git log --format=full` or similar
