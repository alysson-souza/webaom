# WebAOM Copilot Instructions

## Project Overview
WebAOM (Web Anime-o-Matic) is a legacy Java Swing desktop application for identifying anime files via AniDB's UDP API. Originally developed 2005-2010, revived in 2025 for maintenance.

## Architecture

### Core Components
- **Entry Point**: [WebAOM.java](../src/main/java/epox/webaom/WebAOM.java) - Swing JApplet/JFrame launcher
- **Global State**: [A.java](../src/main/java/epox/webaom/A.java) - Static singleton holding all subsystems (`A.db`, `A.nio`, `A.dio`, `A.jobs`, `A.gui`, etc.)
- **Job System**: [Job.java](../src/main/java/epox/webaom/Job.java) with bitmask-based state machine (status + health flags)
- **Network Layer**: [ACon.java](../src/main/java/epox/webaom/net/ACon.java) - AniDB UDP API v3 client with encryption/compression

### Package Structure
```
epox.webaom/        # Core application logic
epox.webaom.data/   # Data models (AFile, Anime, Ep, Group)
epox.webaom.net/    # AniDB UDP protocol implementation
epox.webaom.ui/     # Swing UI components (JPanel*, JTable*, JDialog*)
epox.util/          # Utilities and hashing
epox.av/            # Audio/video file info parsing
epox.swing/         # Reusable Swing components
```

### Vendored Code (DO NOT MODIFY)
Third-party code in `com/`, `gnu/`, `jonelo/` packages is excluded from formatting/linting:
- `com.bitzi.util` - Base32 encoding
- `com.twmacinta` - Fast MD5 implementation
- `gnu.crypto.hash` - Tiger hash
- `jonelo.jacksum` - Checksum algorithms

## Build & Run

```bash
./gradlew build          # Compile + create fat JAR with dependencies
./gradlew run            # Run application
./gradlew spotlessApply  # Format code (Google Java Style, AOSP variant)
./gradlew checkstyleMain # Check code style (warnings only, non-blocking)
```

The JAR bundles PostgreSQL and MySQL drivers for optional database caching.

## Code Style & Conventions

### Formatting
- **Spotless** with Google Java Format (AOSP variant) - only applies to `epox/**/*.java`
- Line length: 120 chars (info-level)
- Run `./gradlew spotlessApply` before committing

### Legacy Patterns (Preserve These)
- Single-letter class names: `A` (globals), `U` (utilities), `DB` (database)
- Hungarian-ish prefixes: `m_` (member), `mI` (int), `mS` (string), `mB` (boolean)
- Bitmask constants for Job states: `S_DONE`, `H_PAUSED`, `D_DIO`, etc.
- Static field access via `A.` class (e.g., `A.jobs`, `A.opt`, `A.gui`)

### Job State Machine
Jobs flow through states using bitmask combinations:
```
HASHWAIT → HASHING → HASHED → IDENTWAIT → IDENTIFYING → IDENTIFIED → ADDWAIT → ADDING → FINISHED
```
Health flags: `H_NORMAL`, `H_PAUSED`, `H_MISSING`, `H_DELETED`

## Key Implementation Details

### Configuration
- User settings stored in `~/.webaom` (UTF-8 encoded)
- HTML template override: `~/.webaom.htm`
- Options managed via [Options.java](../src/main/java/epox/webaom/Options.java) with indexed arrays (`mBa[]`, `mIa[]`, `mSa[]`)

### AniDB UDP Protocol
- Default server: `api.anidb.net:9000`
- Session-based auth with optional AES encryption
- Rate limiting handled internally (configurable delay between packets)
- Compression support (zlib)

### File Processing
- Multi-algorithm hashing: ED2K, MD5, SHA-1, CRC32, Tiger Tree Hash
- Template-based file renaming with tags (`%ann`, `%epn`, `%grp`, etc.)
- Conditional rules defined in [Rules.java](../src/main/java/epox/webaom/Rules.java)

## Testing
No automated test suite exists. Test manually by running the application.

## Dependencies
- Java 21 (toolchain)
- PostgreSQL/MySQL drivers (runtime only, optional)
- No external compile dependencies - uses bundled/vendored crypto and hash libraries

## Git Conventions

### Commit Style
Use **conventional commits** with optional scope and body:
```
type(scope): subject line

Optional body explaining the change in detail.
```

Examples from this repo:
- `fix(ci): remove Azul vendor requirement from toolchain`
- `chore: switch to Google Java Format`
- `style: fix checkstyle warnings`

### Git Commands
- If you need to see the git log history, use `git log --format=full` or similar
