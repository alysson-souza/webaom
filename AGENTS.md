# WebAOM Agent Instructions

## Build Commands
```bash
./gradlew build          # Compile + create fat JAR with dependencies
./gradlew run            # Run application
./gradlew spotlessApply  # Format code (Google Java Style, AOSP variant)
./gradlew spotlessCheck  # Check formatting without applying changes
./gradlew checkstyleMain # Check code style
```

## Code Style Guidelines
- **Formatting**: Google Java Format (AOSP variant) via Spotless - only applies to `epox/**/*.java`
- **Line length**: 120 chars (info-level)
- **Encoding**: UTF-8 for all Java files
- **Target**: Java 21 toolchain
- **Imports**: No star imports, unused imports removed automatically
- **Naming**: Follow standard Java conventions (camelCase for methods/variables, PascalCase for classes)

## Package Structure
- `epox.webaom/` - Core application logic
- `epox.webaom.data/` - Data models (AFile, Anime, Ep, Group)
- `epox.webaom.net/` - AniDB UDP protocol implementation
- `epox.webaom.ui/` - Swing UI components
- `epox.util/` - Utilities and hashing
- `epox.av/` - Audio/video file info parsing
- `epox.swing/` - Reusable Swing components

## Important Notes
- DO NOT modify vendored code in `com/`, `gnu/`, `jonelo/` packages (excluded from formatting)
- No automated test suite - test manually by running the application
- Run `./gradlew spotlessApply` before committing