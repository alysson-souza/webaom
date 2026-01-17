# WebAOM Agent Instructions

## Project Overview

WebAOM (Web Anime-o-Matic) is a Java Swing desktop application for identifying anime files via AniDB's UDP API.
Originally developed 2005-2010, revived in 2025 for modernization.

## Build Commands

```bash
./gradlew build          # Compile + create fat JAR with dependencies
./gradlew run            # Run application
./gradlew spotlessApply  # Format code (Palantir Java Format)
./gradlew spotlessCheck  # Check formatting without applying changes
./gradlew jpackage       # Create native installer (.dmg/.msi/.deb) for current platform
./gradlew jpackageAppImage  # Create portable app bundle for current platform
./gradlew jlink          # Create minimal custom JRE
```

## Architecture

### Core Components

- **Entry Point**: `src/main/java/epox/webaom/WebAOM.java` - Swing JApplet/JFrame launcher
- **Global State**: `AppContext.java` - Static singleton holding all subsystems (`AppContext.databaseManager`,
  `AppContext.nio`, `AppContext.dio`, `AppContext.jobs`, `AppContext.gui`, etc.)
- **Job System**: `Job.java` with bitmask-based state machine (status + health flags)
- **Network Layer**: `AniDBConnection.java` / `AniDBFileClient.java` - AniDB UDP API client and high-level file/MyList
  operations (encryption/compression supported)

### Package Structure

- `epox.webaom/` - Core application logic
- `epox.webaom.data/` - Data models (`AniDBFile`, `Anime`, `Episode`, `Group`)
- `epox.webaom.net/` - AniDB UDP protocol implementation
- `epox.webaom.ui/` - Swing UI components (JPanel*, JTable*, JDialog\*)
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

- Palantir Java Format via Spotless - only applies to `epox/**/*.java`
- 120 character line length, lambda-friendly formatting
- 4-space indentation
- UTF-8 encoding for all Java files
- Java 21 toolchain
- No star imports, unused imports removed automatically
- Run `./gradlew spotlessApply` before committing
- IntelliJ IDEA: install the [Palantir Java Format](https://plugins.jetbrains.com/plugin/13180-palantir-java-format)
  plugin for consistent formatting
- VS Code:
  install [Spotless Gradle](https://marketplace.visualstudio.com/items?itemName=richardwillis.vscode-spotless-gradle)
  extension for format-on-save via Spotless

### Linting

- SonarLint for IDE-based code quality analysis (VS Code and IntelliJ IDEA)
- No CI-based linting - rely on IDE integration

## Key Implementation Details

### Configuration

User settings follow XDG Base Directory specification (with automatic migration from legacy paths):

| Platform | Config Directory                                          | Options File | Template File  |
| -------- | --------------------------------------------------------- | ------------ | -------------- |
| Linux    | `$XDG_CONFIG_HOME/webaom/` (default: `~/.config/webaom/`) | `config`     | `template.htm` |
| macOS    | `~/Library/Application Support/webaom/`                   | `config`     | `template.htm` |
| Windows  | `%APPDATA%\webaom\`                                       | `config`     | `template.htm` |

Legacy paths (`~/.webaom`, `~/.webaom.htm`) are automatically migrated on first run.

- Options managed via `Options.java` with arrays (`booleanOptions[]`, `integerOptions[]`, `stringOptions[]`)
- Platform paths handled by `PlatformPaths.java`

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

## Native Packaging

Native app packages are built using `jpackage` (Java 21+) with a minimal JRE created by `jlink`.

### Output Locations

- `build/jlink/` - Minimal custom JRE (~51MB vs ~166MB full JRE)
- `build/installer/` - Native packages and app bundles

### macOS Notarization

The app is **not notarized** with Apple. Users must bypass Gatekeeper:

```bash
xattr -cr /Applications/WebAOM.app
```

### CI/CD

GitHub Actions workflow (`.github/workflows/build.yml`) builds native packages for all platforms on tagged releases.

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
- `chore(spotless): migrate to Palantir Java Format`
- `style: reformat code with Palantir formatter`

### Git Commands

- If you need to see the git log history, use `git log --format=full` or similar
