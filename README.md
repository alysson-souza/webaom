# WebAOM

WebAOM (Web Anime-o-Matic) is a Java application that automatically identifies anime files, retrieves metadata from AniDB, and manages your anime collection. It hashes media files using multiple algorithms and integrates with AniDB's MyList to organize and track your files.

## Features

- **File Identification**: Hash files using ED2K, MD5, SHA-1, CRC32, and Tiger Tree Hash algorithms to identify them in AniDB's database
- **MyList Integration**: Automatically add identified files to your AniDB MyList with metadata
- **Automated Organization**: Rename and move files based on customizable rules and templates
- **Concurrent Processing**: Hash and process multiple files simultaneously
- **Drag-and-Drop Support**: Visual feedback with border highlighting when dragging files to the jobs table
- **Local Database**: Optional MySQL or PostgreSQL backend for caching metadata and job tracking
- **Alternative Views**: Tree view display for organized file browsing
- **Import/Export**: Manage your collection data

## Requirements

- **Java Development Kit (JDK)**: Java 21 or later (for building) or Java 21+ (for running)
- **Gradle**: Included via Gradle Wrapper (no separate installation needed)
- **Swing Support**: Required for the GUI
- **AniDB Account**: Needed to use MyList functionality
- **Internet Connection**: For communicating with AniDB's UDP API

## Building from Source

### Using Gradle (Recommended)

1. Ensure you have Java 21+ installed
2. Clone or download this repository
3. Build the project:
   ```bash
   ./gradlew build
   ```
4. Run the application:
   ```bash
   ./gradlew run
   ```

Or run the JAR directly:
     ```bash
     java -jar build/libs/webaom-2.0.0.jar
     ```

### Gradle Commands

- **Build**: `./gradlew build` - Compiles and creates the JAR
- **Run**: `./gradlew run` - Runs the application from the built classes
- **Clean**: `./gradlew clean` - Removes build artifacts
- **Check**: `./gradlew check` - Runs build and tests (if any)
- **Format**: `./gradlew spotlessApply` - Applies code formatting (Google Java Style)
- **Style Check**: `./gradlew spotlessCheck` - Checks code formatting without applying changes

### Manual Compilation (Legacy)

If you prefer to compile without Gradle:

```bash
mkdir -p build
find src/main/java -name "*.java" -print > sources.txt
javac -d build @sources.txt
cp -r src/main/resources/* build/
jar cfm webaom.jar Manifest.mf -C build .
java -jar webaom.jar
```

## Installation (Pre-built JAR)

If you have a pre-built JAR file:

```bash
java -jar webaom-2.0.0.jar
```

## Usage

### Basic Workflow

1. **Configure Settings**: Set up your AniDB username, hash algorithms, and file organization rules
2. **Add Files**: Drag and drop files/folders onto the application or use the file browser
3. **Login**: WebAOM will connect to AniDB's UDP API with your credentials
4. **Process**: Files are hashed, identified, and added to your MyList
5. **Organize**: Files can be automatically renamed and moved based on your rules

### File Organization Rules

WebAOM supports rule-based file organization using templates and conditionals:

- Use tags like `%ann` (anime name), `%epn` (episode number), `%grp` (group name)
- Define conditional rules with IF/ELSE IF/RETURN statements
- Automatically move and rename files based on their metadata

## Configuration

WebAOM stores settings in `~/.webaom` (UTF-8 encoded). You can configure:

- **Connection Settings**: Host, port, and timeout for AniDB API
- **Hash Types**: Which algorithms to use for file identification
- **Renaming Rules**: Templates and conditions for organizing files
- **MyList Preferences**: How files are added to your list
- **Database Connection**: Optional MySQL/PostgreSQL for metadata caching

### Optional Database Setup

For persistent metadata caching, configure a MySQL or PostgreSQL database. See the included SQL schemas for setting up the database tables.

## Project Status

**Note on Development History**: WebAOM was created by **epoximator** (RIP). Originally developed from 2005-2010, development ceased around 2010 (v1.19p, March 2010). The project remained dormant for many years until it was revived with v2.0.0 (November 2025) to modernize the build system and improve Java compatibility.

This repository is a maintenance fork extracted from AniDB's official UDP clients repository (git.anidb.net). The goal is to:

- Preserve the application for the community
- Fix long-standing issues and bugs
- Improve compatibility with modern Java versions
- Maintain the codebase for users who depend on it

While this is not an active feature development project, improvements to stability, compatibility, and bug fixes are welcome.

## History

- **Original Repository**: Hosted on AniDB's Subversion (SVN) server
- **Migration**: Moved to AniDB's Git repository (git.anidb.net/anidb/udp-clients/) alongside other UDP clients
- **Current Home**: Extracted to this standalone GitHub repository for improved visibility and community contribution

## Technical Details

WebAOM is written in Java and consists of:

- **Core Logic**: File hashing, metadata retrieval, and MyList synchronization
- **User Interface**: Swing-based GUI with job queue and progress tracking
- **Networking**: AniDB UDP API v3 client with session management
- **Database Abstraction**: Support for local MySQL/PostgreSQL backends
- **File I/O**: Audio/video metadata parsing and file system operations

## License

GNU General Public License v2 (GPLv2)

## Related Links

- [AniDB](https://anidb.net/) - Official AniDB website
- [AniDB WebAOM Wiki](https://wiki.anidb.net/WebAOM) - Original documentation
- [AniDB UDP Clients Repository](https://git.anidb.net/anidb/udp-clients/) - Official source of various UDP clients
- [AniDB API Documentation](https://wiki.anidb.net/UDP_API) - API reference

## Contributing

Contributions are welcome. Please feel free to open issues for bugs or improvements, or submit pull requests with fixes and enhancements.
