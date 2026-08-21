# WebAOM

WebAOM (Web Anime-O-Matic) is a Java desktop application for AniDB. It hashes anime files, adds them to your [MyList](https://wiki.anidb.net/MyList), and gets metadata through AniDB's [UDP API](https://wiki.anidb.net/UDP_API). User-defined rules can rename and move matched files.

## Features

- Multi-algorithm file hashing (ED2K, MD5, SHA-1, CRC32, Tiger Tree Hash)
- AniDB MyList integration with automatic metadata retrieval
- Rule-based file renaming and moving using customizable templates
- Drag-and-drop file processing with concurrent hashing
- Optional database backend (SQLite embedded, PostgreSQL, or MySQL)
- Encrypted and compressed UDP communication

## Requirements

- [AniDB account](https://anidb.net/user/register) (required)
- Java 21 or newer when running a JAR file

## Installation

### Native packages

Download pre-built packages from the [Releases](https://github.com/alysson-souza/webaom/releases) page:

| Platform | Package                           |
| -------- | --------------------------------- |
| macOS    | `WebAOM-2.5.0.dmg`                |
| Windows  | `WebAOM-2.5.0.msi`                |
| Linux    | `WebAOM-2.5.0-x86_64.AppImage`    |
| Linux    | `webaom_2.5.0_amd64.deb` (Debian) |

> The macOS app is not notarized. Right-click the app and select "Open", or run `xattr -cr /Applications/WebAOM.app`.

### JAR files

With Java 21 or newer installed:

```bash
# Lite, SQLite only, about 18 MB. Recommended for most users.
java -jar webaom-2.5.0-lite.jar

# Full, with SQLite, PostgreSQL, and MySQL, about 22 MB.
java -jar webaom-2.5.0-full.jar
```

## Quick start

1. Add files. Drag them into the window, click "Files..." or "Folders...", or set default directories in Options.
2. Log in. Click "Login" and enter your AniDB credentials.
3. Start processing. Click "Start". WebAOM hashes each file, gets its AniDB metadata, adds it to your MyList, and applies any rename or move rules.

## Command line

The CLI uses the desktop application's configuration, database, AniDB credentials, file extensions, and rules.

### Running the CLI

Both JARs include the CLI. Use the lite JAR with SQLite or the full JAR with PostgreSQL or MySQL:

```bash
java -jar webaom-2.6.0-lite.jar --help
java -jar webaom-2.6.0-full.jar --help
```

## Configuration

WebAOM stores its UTF-8 settings in the platform app-data directory:

- Linux: `~/.config/webaom`
- macOS: `~/Library/Application Support/webaom`
- Windows: `%APPDATA%\\webaom`

The main options are:

- **Connection.** Set the AniDB host, ports, keep-alive interval, and delay between requests. The minimum delay is two seconds, and three seconds is recommended.
- **File Options.** Set the MyList state, source, storage, and watched status.
- **Hash Functions.** Choose which hashes to calculate. ED2K is required.
- **Wanted Extensions.** Choose which file types WebAOM processes. The default list includes AVI, OGM, MKV, and MP4.

### File renaming rules

Rules in the **Rules** tab rename and move files based on AniDB metadata.

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

#### Common tags

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

#### Rule tests

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

### Converting to TVDB names with FileBot

Plex, Jellyfin, and Emby expect TVDB season and episode numbers such as S01E01. AniDB uses absolute episode numbers. Use [FileBot](https://www.filebot.net/) to convert files after WebAOM renames them.

The expression below expects files renamed with WebAOM's default template:
`%ann (%yea) - %enr - %epn [%src-%res][%aud][%dub][%vid]-%grp`

```bash
filebot -rename "FILES" --db AniDB --format @format.txt -non-strict
```

Put this expression in **format.txt** to convert AniDB matches to TVDB season and episode numbers:

```groovy
{def grp=any{group}{fn.tokenize("-")[-1].tokenize(".")[0]}; def lang=any{audioLanguages.join("+")}{""}; def src=any{vs}{source}{""}; def res=any{vf}{resolution}{""}; def aud=any{ac}{""}; def vid=any{vc}{""}; db.TheTVDB.n+" ("+any{db.TheTVDB.y}{""}+") - "+db.TheTVDB.s00e00+" - "+db.TheTVDB.t+" ["+src+"-"+res+"]["+aud+"]["+lang+"]["+vid+"]"+(grp?"-"+grp:"")}
```

FileBot's `db.TheTVDB` binding gets AniDB-to-TVDB mappings from the [Anime-Lists](https://github.com/Anime-Lists/anime-lists) project.

For example:

```
From: Gintama (2006) - 083 - Rank Has Nothing to Do With Luck [DVD-760x576][FLAC][japanese][H264]-CBT.mkv
To:   Gintama (2006) - S02E34 - Rank Has Nothing to Do with Luck [DVDRip-576p][FLAC][jpn][x264]-CBT.mkv
```

## Keyboard shortcuts

| Key         | Action                       |
| ----------- | ---------------------------- |
| `?`         | Show keyboard shortcuts help |
| Enter/Space | Show job info                |
| A           | Open anime page in browser   |
| M           | Open MyList page in browser  |
| W           | Watch file (Windows/macOS)   |
| X           | Open containing folder       |
| P           | Pause/unpause job            |
| S           | Apply rules / update status  |
| I           | Re-identify file             |
| Left        | Collapse row (tree view)     |
| Right       | Expand row (tree view)       |
| F5/⌘R       | Refresh alternate view       |
| F9          | Reset application            |
| Delete/BackSpace | Delete selected jobs     |
| ESC         | Stop worker thread           |

## External database

WebAOM uses an embedded SQLite database by default. To use PostgreSQL or MySQL:

1. Create a database with Unicode support.
2. Enter its JDBC URL in Options under "My Database".
   ```
   jdbc:postgresql://localhost/webaom?user=myuser&password=mypass
   jdbc:mysql://localhost/webaom?user=myuser&password=mypass
   ```
3. Press Enter to connect.

## Building from source

Java 21 or newer is required. The repository includes the Gradle wrapper.

```bash
./gradlew build   # Compile and create JARs
./gradlew run     # Run the application
```

| Command                   | Description                                  |
| ------------------------- | -------------------------------------------- |
| `./gradlew clean`         | Remove build artifacts                       |
| `./gradlew spotlessApply` | Format code                                  |
| `./gradlew jpackage`      | Create native installer for current platform |

Running any Gradle task installs or refreshes the `.git/hooks/pre-commit` and `.git/hooks/pre-push` shims. The hooks run `spotlessCheck` on commits with staged Java or Gradle changes and on every push.

## Project history

epoximator created WebAOM and developed it from 2005 to 2010. RIP. This maintenance fork revived the project in 2025 from [AniDB's UDP clients repository](https://git.anidb.net/anidb/udp-clients/).

The fork preserves WebAOM, fixes bugs, and keeps it working with current Java releases.

## License

GNU General Public License v2 (GPLv2)

## Links

- [AniDB](https://anidb.net/)
- [WebAOM Wiki](https://wiki.anidb.net/WebAOM)
- [UDP API Documentation](https://wiki.anidb.net/UDP_API)
- [Report Issues](https://github.com/alysson-souza/webaom/issues)
