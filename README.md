# WebAOM

WebAOM (Web Anime-O-Matic) is a Java desktop application that hashes anime files and adds them to your [MyList](https://wiki.anidb.net/MyList) on AniDB. It retrieves metadata via the [UDP API](https://wiki.anidb.net/UDP_API) and can automatically rename and organize your collection.

## Features

- Multi-algorithm file hashing (ED2K, MD5, SHA-1, CRC32, Tiger Tree Hash)
- AniDB MyList integration with automatic metadata retrieval
- Rule-based file renaming and moving using customizable templates
- Drag-and-drop file processing with concurrent hashing
- Optional database backend (SQLite embedded, PostgreSQL, or MySQL)
- Encrypted and compressed UDP communication

## Requirements

- [AniDB account](https://anidb.net/user/register) (required)
- Java 21+ (for JAR files) or use native packages

## Installation

### Native Packages (Recommended)

Download pre-built packages from the [Releases](https://github.com/alysson-souza/webaom/releases) page:

| Platform | Package                            |
| -------- | ---------------------------------- |
| macOS    | `WebAOM-2.0.1.dmg`                 |
| Windows  | `WebAOM-2.0.1.msi`                 |
| Linux    | `WebAOM-2.0.1-x86_64.AppImage`     |
| Linux    | `webaom_2.0.1_amd64.deb` (Debian)  |

> **macOS users**: The app is not notarized. Right-click → Open, or run `xattr -cr /Applications/WebAOM.app`

### JAR Files

Requires Java 21+:

```bash
# Lite (SQLite only, ~4MB) - recommended for most users
java -jar webaom-2.0.2-lite.jar

# Full (SQLite + PostgreSQL + MySQL, ~10MB)
java -jar webaom-2.0.2-full.jar
```

## Quick Start

1. **Add files** — Drag-and-drop, click "Files..." or "Folders...", or set default directories in Options
2. **Log in** — Click "Login" and enter your AniDB credentials
3. **Start** — Click "Start" and WebAOM will hash each file, fetch metadata from AniDB, add it to your MyList, and optionally rename/move it based on your rules

## Configuration

Settings are stored in `~/.webaom` (UTF-8). Key options include:

- **Connection**: AniDB host, ports, keep-alive, delay between requests (minimum 2 sec, 3 recommended)
- **File Options**: MyList state, source, storage, watched status
- **Hash Functions**: Select which algorithms to compute (ED2K is required)
- **Wanted Extensions**: File types to process (default: avi, ogm, mkv, mp4, and many more)

### File Renaming Rules

WebAOM uses a scripting system to rename and move files based on metadata. Rules are defined in the **Rules** tab.

Example rename script:
```
IF A(Naruto) DO FAIL                     // Skip Naruto files
DO ADD '%eng (%ann) - %enr - %epn '      // Base format
IF D(japanese);S(english) DO ADD '(SUB)' // Add (SUB) for English subs
IF G(!unknown) DO ADD '[%grp]'           // Add group if known
DO ADD '(%CRC)'                          // Always add CRC
```

Example move script:
```
IF R(DVD,HKDVD) DO ADD 'M:\dvd\'
ELSE DO ADD 'N:\tv\'
DO ADD '%yea\%ann [%eps]\'
```

#### Common Tags

| Tag             | Description             |
| --------------- | ----------------------- |
| `%ann`          | Anime name (romaji)     |
| `%eng`          | Anime name (english)    |
| `%kan`          | Anime name (kanji)      |
| `%epn`          | Episode name            |
| `%enr`          | Episode number          |
| `%grp`          | Group short name        |
| `%crc` / `%CRC` | CRC32 (lower/upper)     |
| `%ed2` / `%ED2` | ED2K hash (lower/upper) |
| `%src`          | Source (TV, DVD, etc.)  |
| `%res`          | Resolution              |
| `%yea`          | Year                    |

#### Rule Tests

| Test        | Description                          |
| ----------- | ------------------------------------ |
| `A(name)`   | Anime name/ID (supports regex)       |
| `G(name)`   | Group name/ID                        |
| `R(source)` | Source: TV, DVD, HKDVD, www, etc.    |
| `T(type)`   | Type: TV, OVA, Movie, Other, web     |
| `D(lang)`   | Dub language                         |
| `S(lang)`   | Sub language                         |
| `Y(year)`   | Year or range (e.g., `Y(2000-2010)`) |
| `I(tag)`    | Tag is defined                       |

Use `;` for AND, `,` for OR, `!` for NOT.

## Keyboard Shortcuts

| Key         | Action                       |
| ----------- | ---------------------------- |
| Enter/Space | Open file info               |
| A           | Open anime page in browser   |
| F           | Open file page in browser    |
| E           | Open episode page in browser |
| G           | Open group page in browser   |
| M           | Open MyList page in browser  |
| W           | Watch file (Windows/macOS)   |
| X           | Open containing folder       |
| P           | Pause/unpause job            |
| S           | Apply rules                  |
| I           | Re-identify file             |
| F5/⌘R       | Refresh view                 |
| F9          | Reset application            |
| DEL         | Clear log                    |
| ESC         | Stop worker thread           |

## External Database

WebAOM uses an embedded H2 database by default. For PostgreSQL or MySQL:

1. Create the database with Unicode support
2. Enter the JDBC URL in Options → "My Database":
   ```
   jdbc:postgresql://localhost/webaom?user=myuser&password=mypass
   jdbc:mysql://localhost/webaom?user=myuser&password=mypass
   ```
3. Press Enter to connect

## Building from Source

Requires Java 21+. Gradle is included via wrapper.

```bash
./gradlew build   # Compile and create JARs
./gradlew run     # Run the application
```

| Command                   | Description                                  |
| ------------------------- | -------------------------------------------- |
| `./gradlew clean`         | Remove build artifacts                       |
| `./gradlew spotlessApply` | Format code                                  |
| `./gradlew jpackage`      | Create native installer for current platform |

## Project History

Created by **epoximator**, developed 2005–2010. Revived in 2025 as a maintenance fork from [AniDB's UDP clients repository](https://git.anidb.net/anidb/udp-clients/).

Goals: preserve the application, fix bugs, and maintain compatibility with modern Java.

## License

GNU General Public License v2 (GPLv2)

## Links

- [AniDB](https://anidb.net/)
- [WebAOM Wiki](https://wiki.anidb.net/WebAOM)
- [UDP API Documentation](https://wiki.anidb.net/UDP_API)
- [Report Issues](https://github.com/alysson-souza/webaom/issues)
