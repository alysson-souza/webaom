# WebAOM

WebAOM (Web Anime-O-Matic) is a Java Swing application for identifying anime files via AniDB. It hashes media files, retrieves metadata, and integrates with AniDB's MyList to organize and track your collection.

## Features

- Multi-algorithm file hashing (ED2K, MD5, SHA-1, CRC32, Tiger Tree Hash)
- AniDB MyList integration with automatic metadata retrieval
- Rule-based file renaming using templates (`%ann`, `%epn`, `%grp`, etc.)
- Concurrent file processing with drag-and-drop support
- Optional database backend (H2 embedded, PostgreSQL, or MySQL)

## Installation

### Native Packages (Recommended)

Download pre-built packages from the [Releases](https://github.com/alyssonfm/webaom/releases) page:

| Platform | Package                  |
| -------- | ------------------------ |
| macOS    | `WebAOM-2.0.0.dmg`       |
| Windows  | `WebAOM-2.0.0.msi`       |
| Linux    | `webaom_2.0.0_amd64.deb` |

> **macOS users**: The app is not notarized. Right-click → Open, or run `xattr -cr /Applications/WebAOM.app`

### JAR Files

Requires Java 21+:

```bash
# Lite (H2 only, ~4MB) - recommended for most users
java -jar webaom-2.0.0-lite.jar

# Full (H2 + PostgreSQL + MySQL, ~10MB)
java -jar webaom-2.0.0-full.jar
```

## Building from Source

Requires Java 21+. Gradle is included via wrapper.

```bash
./gradlew build   # Compile and create JARs
./gradlew run     # Run the application
```

JARs are output to `build/libs/`.

### Other Commands

| Command                   | Description                                  |
| ------------------------- | -------------------------------------------- |
| `./gradlew clean`         | Remove build artifacts                       |
| `./gradlew spotlessApply` | Format code                                  |
| `./gradlew jpackage`      | Create native installer for current platform |

## Usage

1. Add files via drag-and-drop or the file browser
2. WebAOM hashes and identifies files against AniDB
3. Files are added to your MyList with metadata
4. Optionally rename/move files using your configured rules

## Configuration

Settings are stored in `~/.webaom` (UTF-8). Configure:

- AniDB credentials and connection settings
- Hash algorithms to use
- File renaming templates and rules
- Database connection (optional)

See the [AniDB WebAOM Wiki](https://wiki.anidb.net/WebAOM) for detailed documentation.

## Project History

Created by **epoximator** (RIP), developed 2005–2010. Revived in 2025 as a maintenance fork from [AniDB's UDP clients repository](https://git.anidb.net/anidb/udp-clients/).

Goals: preserve the application, fix bugs, and maintain compatibility with modern Java.

## License

GNU General Public License v2 (GPLv2)

## Links

- [AniDB](https://anidb.net/)
- [WebAOM Wiki](https://wiki.anidb.net/WebAOM)
- [UDP API Documentation](https://wiki.anidb.net/UDP_API)
