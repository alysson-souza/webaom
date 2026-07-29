/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2025 Alysson Souza
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.webaom.cli;

import static epox.webaom.cli.CliText.blankToNull;
import static epox.webaom.cli.CliText.firstNonBlank;
import static epox.webaom.cli.CliText.usefulMessage;
import static epox.webaom.cli.CliText.valueOr;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.Job;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.data.Mylist;
import epox.webaom.data.MylistStates;
import epox.webaom.db.DatabaseManager;
import epox.webaom.db.DatabaseManagerFactory;
import epox.webaom.net.AniDBConnection;
import epox.webaom.net.AniDBConnectionSettings;
import epox.webaom.net.AniDBException;
import epox.webaom.net.AniDBFileClient;
import epox.webaom.util.PlatformPaths;
import java.io.Console;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Headless, one-shot application service used by the CLI commands. */
public final class CliApplication implements AutoCloseable {
    private final Options options;
    private final Rules rules;
    private final FileDiscovery discovery = new FileDiscovery();
    private final FileHasher hasher = new FileHasher();
    private final Set<String> extensions;
    private final ConnectionFactory connectionFactory;
    private AniDBFileClient connection;
    private Thread shutdownHook;

    private CliApplication(Options options, Rules rules, Set<String> extensions) {
        this(options, rules, extensions, settings -> new AniDBFileClient(null, settings));
    }

    CliApplication(Options options, Rules rules, Set<String> extensions, ConnectionFactory connectionFactory) {
        this.options = options;
        this.rules = rules;
        this.extensions = extensions;
        this.connectionFactory = connectionFactory;
    }

    public static CliApplication open(Path configPath, boolean requireDatabase) throws CliException {
        Options options = configPath == null
                ? Options.forCommandLine()
                : new Options(configPath.toAbsolutePath().normalize());
        AppContext.initCore(options);
        boolean configExists = options.existsOnDisk();
        boolean loaded = options.loadFromFileQuietly();
        if (configExists && !loaded) {
            throw new CliException("Could not load WebAOM configuration: " + options.getFilePath(), 2);
        }
        AppContext.userPass.username = null;
        AppContext.userPass.password = null;
        AppContext.userPass.apiKey = null;

        if (loaded) {
            AppContext.fileHandler.loadOptions(options);
            AppContext.rules.loadFromOptions(options);
            String credentials = options.getString(Options.STR_USERNAME);
            if (credentials != null && !credentials.isBlank()) {
                AppContext.userPass.set(credentials);
            }
        } else {
            AppContext.fileHandler.loadDefaultExtensions();
            hydrateFreshOptions(options);
        }

        if (requireDatabase) {
            initializeDatabase(options);
        }

        Set<String> extensions =
                FileDiscovery.normalizeExtensions(AppContext.fileHandler.allowedExtensions.getStrings());
        CliApplication application = new CliApplication(options, AppContext.rules, extensions);
        application.registerShutdownHook();
        return application;
    }

    private void registerShutdownHook() {
        shutdownHook = new Thread(this::close, "webaom-cli-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private static void hydrateFreshOptions(Options options) {
        options.setBoolean(Options.BOOL_ADD_FILE, true);
        options.setInteger(Options.INT_REMOTE_PORT, AniDBConnection.DEFAULT_REMOTE_PORT);
        options.setInteger(Options.INT_LOCAL_PORT, AniDBConnection.DEFAULT_LOCAL_PORT);
        options.setInteger(Options.INT_FILE_STATE, MylistStates.LOCATION_HDD);
        options.setInteger(Options.INT_TIMEOUT, 20);
        options.setInteger(Options.INT_DATAGRAM_DELAY, 4);
        options.setString(Options.STR_HOST_URL, AniDBConnection.DEFAULT_HOST);
        AppContext.fileHandler.saveOptions(options);
        AppContext.rules.saveToOptions(options);
    }

    private static void initializeDatabase(Options options) throws CliException {
        String failure = openDatabase(options);
        if (failure != null) {
            throw new CliException(failure, 2);
        }
    }

    /** Opens the database, returning null on success or a user-facing reason on failure. */
    static String openDatabase(Options options) {
        String connectionString = resolveConnectionString(options);
        if (!isSupportedDatabaseUrl(connectionString)) {
            return "Unsupported database type in WebAOM configuration.";
        }
        AppContext.databaseManager = DatabaseManagerFactory.create(connectionString);
        AppContext.databaseManager.setLoadAllJobs(false);
        if (!AppContext.databaseManager.initialize(connectionString)) {
            String detail = databaseFailureDetail(AppContext.databaseManager);
            AppContext.databaseManager.shutdown();
            AppContext.databaseManager = null;
            return detail;
        }
        AppContext.databaseManager.setDebug(false);
        return null;
    }

    private static String resolveConnectionString(Options options) {
        String connectionString = blankToNull(options.getString(Options.STR_DATABASE_URL));
        return connectionString == null ? DatabaseManagerFactory.getEmbeddedConnectionString() : connectionString;
    }

    /**
     * Upgrades the database schema with explicit consent from the {@code webaom db upgrade} command. Consent
     * is only ever sought (and only ever takes effect) for the pre-v4 legacy-schema case: a database that is
     * too old to migrate, or from a newer WebAOM version, is refused unconditionally regardless of consent.
     */
    public boolean upgradeDatabase(CliOutput output, boolean assumeYes) {
        String connectionString = resolveConnectionString(options);
        if (!isSupportedDatabaseUrl(connectionString)) {
            output.printText("Unsupported database type in WebAOM configuration.");
            return false;
        }
        DatabaseManager manager = DatabaseManagerFactory.create(connectionString);
        manager.setLoadAllJobs(false);
        manager.setLegacySchemaUpgradeConfirmationSource(() -> confirmUpgrade(output, assumeYes));
        if (!manager.initialize(connectionString)) {
            String detail = manager.isLegacySchemaUpgradeDeclined()
                    ? "Database schema upgrade was not confirmed; nothing was changed."
                    : databaseFailureDetail(manager);
            manager.shutdown();
            output.printText(detail);
            return false;
        }
        manager.setDebug(false);
        AppContext.databaseManager = manager;
        output.printText("Database schema is up to date.");
        return true;
    }

    private static boolean confirmUpgrade(CliOutput output, boolean assumeYes) {
        output.printText("The database schema must be upgraded. Back up the database now if you have not "
                + "already: this will make it incompatible with older WebAOM versions.");
        if (assumeYes) {
            output.printText("Proceeding because --yes was given.");
            return true;
        }
        Console console = System.console();
        if (console == null) {
            output.printText("Re-run with --yes to confirm without an interactive prompt.");
            return false;
        }
        String answer = console.readLine("Continue with the upgrade? [y/N]: ");
        return answer != null && (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes"));
    }

    private static String databaseFailureDetail(DatabaseManager manager) {
        if (manager.hasUnsupportedFutureSchema()) {
            return "The database was created by a newer WebAOM version.";
        }
        if (manager.hasUnsupportedLegacySchema()) {
            return "The database uses a schema that predates WebAOM's migrations and cannot be upgraded. "
                    + "Point the database URL in your WebAOM configuration at a new database, or keep "
                    + "using the old one with a matching WebAOM release.";
        }
        if (manager.isLegacySchemaUpgradeDeclined()) {
            return "The database schema must be upgraded before it can be used. Back up the database, then "
                    + "run `webaom db upgrade` to grant consent (or open the database once in the WebAOM "
                    + "desktop app instead).";
        }
        return "Could not open the WebAOM database.";
    }

    private static boolean isSupportedDatabaseUrl(String connectionString) {
        String normalized = connectionString.toLowerCase(java.util.Locale.ROOT);
        return normalized.startsWith("jdbc:sqlite:")
                || normalized.startsWith("jdbc:postgresql:")
                || normalized.startsWith("jdbc:mysql:");
    }

    public Options options() {
        return options;
    }

    public Rules rules() {
        return rules;
    }

    public List<Path> discover(List<Path> inputs) throws CliException {
        try {
            List<Path> files = discovery.discover(inputs, extensions);
            if (files.isEmpty()) {
                throw new CliException("No supported files found.", 1);
            }
            return files;
        } catch (IOException exception) {
            throw new CliException(exception.getMessage(), 2, exception);
        }
    }

    public List<CliFileResult> hash(List<Path> inputs, EnumSet<FileHasher.Algorithm> algorithms, CliOutput output)
            throws CliException {
        List<CliFileResult> results = new ArrayList<>();
        for (Path file : discover(inputs)) {
            CliFileResult result = new CliFileResult(file);
            try {
                output.progress("Hashing " + file);
                result.hashes = hasher.hash(file, algorithms);
                result.status = CliFileResult.Status.HASHED;
            } catch (IOException exception) {
                result.status = CliFileResult.Status.FAILED;
                result.message = exception.getMessage();
            }
            results.add(result);
        }
        return results;
    }

    /** How much of a cached identification to discard before identifying again. */
    public enum Refresh {
        /** Reuse a complete cached identification. */
        NONE,
        /** Re-query AniDB using the cached hash, to pick up server-side changes such as MyList state. */
        REMOTE,
        /** Re-hash the file and resolve its identity from scratch. */
        FULL
    }

    public List<CliFileResult> identify(List<Path> inputs, CliOutput output) throws CliException {
        return identify(inputs, output, Refresh.NONE);
    }

    /**
     * Identifies files, discarding as much of any cached identification as {@code refresh} asks for.
     *
     * <p>A cached job is only reused when its stored size matches the file currently on disk, so a
     * replaced file is normally re-hashed automatically. {@link Refresh#FULL} covers the residual
     * case of a replacement that happens to have exactly the same size.
     */
    public List<CliFileResult> identify(List<Path> inputs, CliOutput output, Refresh refresh) throws CliException {
        List<CliFileResult> results = new ArrayList<>();
        for (Path file : discover(inputs)) {
            CliFileResult result = new CliFileResult(file);
            results.add(result);
            try {
                output.progress("Identifying " + file);
                identify(result, refresh);
            } catch (CliException exception) {
                throw exception;
            } catch (AniDBException exception) {
                // Connection-level failures (ban, dead session, timeouts) are fatal for the whole
                // batch; keeping on querying AniDB per file would violate its flood policy.
                throw new CliException(usefulMessage(exception), 2, exception);
            } catch (Exception exception) {
                result.status = CliFileResult.Status.FAILED;
                result.message = usefulMessage(exception);
            }
        }
        return results;
    }

    private void identify(CliFileResult result, Refresh refresh) throws IOException, AniDBException, CliException {
        Job job = AppContext.jobs.add(result.source.toFile());
        if (job == null) {
            throw new IOException("File is already queued.");
        }
        result.job = job;
        if (refresh == Refresh.NONE && job.anidbFile != null && !job.incompl()) {
            result.status = CliFileResult.Status.IDENTIFIED;
            result.hashes = jobHashes(job);
            return;
        }

        Map<FileHasher.Algorithm, String> hashes = jobHashes(job);
        if (refresh == Refresh.FULL || !hashes.containsKey(FileHasher.Algorithm.ED2K)) {
            if (Files.size(result.source) < 1) {
                throw new IOException("File size is less than 1 byte.");
            }
            // Re-hash on a full refresh: reusing the cached hash would re-confirm the very identity
            // being re-checked.
            hashes = hasher.hash(result.source, configuredAlgorithms());
            applyHashes(job, hashes);
            job.fileSize = Files.size(result.source);
        }
        result.hashes = hashes;

        AniDBFileClient client = connection();
        // A full refresh must resolve by hash; the cached file id is part of the stale identity.
        String[] fields = refresh != Refresh.FULL && job.anidbFile != null && job.anidbFile.getFileId() > 0
                ? client.retrieveFileData(job.anidbFile.getFileId(), job.currentFile.getName())
                : client.retrieveFileData(job.fileSize, job.ed2kHash, job.currentFile.getName());
        Cache.FileParseResult parseResult =
                fields == null ? null : AppContext.cache.parseFileWithPersistence(fields, job);
        if (parseResult == null || parseResult.file() == null) {
            result.status = CliFileResult.Status.UNKNOWN;
            result.message = client.getLastError();
            job.setStatus(Job.UNKNOWN, false);
            recordPersistenceResult(result, AppContext.databaseManager.update(0, job, DatabaseManager.INDEX_JOB));
            return;
        }

        job.mylistId = job.anidbFile.getMylistEntryId();
        job.anidbFile.setJob(job);
        job.setStatus(Job.FINISHED, false);
        result.status = CliFileResult.Status.IDENTIFIED;
        boolean jobPersisted = AppContext.databaseManager.update(0, job, DatabaseManager.INDEX_JOB);
        recordPersistenceResult(result, parseResult.persisted() && jobPersisted);
    }

    public List<CliFileResult> rename(
            List<Path> inputs, boolean includeMoveRules, boolean apply, RuleDocument overrideRules, CliOutput output)
            throws CliException {
        return rename(inputs, includeMoveRules, apply, overrideRules, output, Refresh.NONE);
    }

    public List<CliFileResult> rename(
            List<Path> inputs,
            boolean includeMoveRules,
            boolean apply,
            RuleDocument overrideRules,
            CliOutput output,
            Refresh refresh)
            throws CliException {
        List<CliFileResult> results = identify(inputs, output, refresh);
        Rules effectiveRules = new Rules();
        if (options.getString(Options.STR_RENAME_RULES) != null || options.getString(Options.STR_MOVE_RULES) != null) {
            effectiveRules.loadFromOptions(options);
        } else {
            effectiveRules.setRenameRules(rules.getRenameRules());
            effectiveRules.setMoveRules(rules.getMoveRules());
        }
        if (overrideRules != null) {
            effectiveRules.setRenameRules(overrideRules.renameRules());
            effectiveRules.setMoveRules(overrideRules.moveRules());
        }
        if (!includeMoveRules) {
            effectiveRules.setMoveRules("# Move rules disabled by CLI");
        }

        for (CliFileResult result : results) {
            if (!canContinueAfterIdentify(result, false)) {
                continue;
            }
            try {
                java.io.File destination = effectiveRules.apply(result.job);
                if (destination == null) {
                    throw new IOException("Rules did not produce a destination.");
                }
                Path target = expandHome(destination.toPath()).toAbsolutePath().normalize();
                String filename = target.getFileName().toString();
                if (Rules.isFilenameTooLong(filename)) {
                    target = target.resolveSibling(Rules.smartTruncateFilename(filename, Rules.MAX_FILENAME_BYTES));
                }
                result.target = target;
                if (result.source.equals(target)) {
                    result.status = CliFileResult.Status.UNCHANGED;
                    result.message = "Rules leave the path unchanged.";
                } else {
                    boolean moved = !result.source.getParent().equals(target.getParent());
                    result.status = moved ? CliFileResult.Status.MOVE_PLANNED : CliFileResult.Status.RENAME_PLANNED;
                    result.message = apply ? null : "Preview only.";
                }
            } catch (Exception exception) {
                result.status = CliFileResult.Status.FAILED;
                result.message = usefulMessage(exception);
            }
        }

        planCompanionMoves(results, extensions);
        validateDestinations(results);
        if (apply) {
            applyMoves(results, output);
        }
        return results;
    }

    static void planCompanionMoves(List<CliFileResult> results, Set<String> mediaExtensions) throws CliException {
        Set<Path> primarySources = new HashSet<>();
        for (CliFileResult result : results) {
            primarySources.add(result.source.toAbsolutePath().normalize());
        }

        Map<Path, CliFileResult.CompanionMove> plannedBySource = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (CliFileResult result : results) {
            if (result.target == null
                    || result.source.equals(result.target)
                    || (result.status != CliFileResult.Status.RENAME_PLANNED
                            && result.status != CliFileResult.Status.MOVE_PLANNED)) {
                continue;
            }

            String sourceBase = baseName(result.source.getFileName().toString());
            String targetBase = baseName(result.target.getFileName().toString());
            List<Path> entries = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(result.source.getParent())) {
                stream.forEach(entries::add);
            } catch (IOException exception) {
                errors.add("Cannot inspect companion files for " + result.source + ": " + usefulMessage(exception));
                continue;
            }
            entries.sort(Comparator.comparing(path -> path.getFileName().toString()));

            for (Path entry : entries) {
                Path companionSource = entry.toAbsolutePath().normalize();
                String companionName = entry.getFileName().toString();
                if (primarySources.contains(companionSource)
                        || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)
                        || FileDiscovery.accepts(entry, mediaExtensions)
                        || !companionName.startsWith(sourceBase + ".")) {
                    continue;
                }

                String suffix = companionName.substring(sourceBase.length());
                String companionTargetName =
                        Rules.smartTruncateFilenamePreservingSuffix(targetBase, suffix, Rules.MAX_FILENAME_BYTES);
                Path companionTarget = result.target
                        .resolveSibling(companionTargetName)
                        .toAbsolutePath()
                        .normalize();
                if (companionSource.equals(companionTarget)) {
                    continue;
                }

                CliFileResult.CompanionMove companionMove =
                        new CliFileResult.CompanionMove(companionSource, companionTarget);
                CliFileResult.CompanionMove previous = plannedBySource.putIfAbsent(companionSource, companionMove);
                if (previous != null) {
                    errors.add("Companion file is associated with multiple inputs: " + companionSource);
                    continue;
                }
                result.companionMoves.add(companionMove);
            }
        }
        if (!errors.isEmpty()) {
            throw new CliException(
                    "Rename preflight failed:" + System.lineSeparator() + String.join(System.lineSeparator(), errors),
                    2);
        }
    }

    static void validateDestinations(List<CliFileResult> results) throws CliException {
        validateDestinations(results, CliApplication::isCaseInsensitiveTarget);
    }

    static void validateDestinations(List<CliFileResult> results, Predicate<Path> caseInsensitiveTarget)
            throws CliException {
        Map<String, Path> sourcesByTarget = new HashMap<>();
        Map<Path, Boolean> caseSensitivityByParent = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (CliFileResult result : results) {
            if (result.target != null) {
                validateDestination(
                        result.source,
                        result.target,
                        caseInsensitiveTarget,
                        sourcesByTarget,
                        caseSensitivityByParent,
                        errors);
            }
            for (CliFileResult.CompanionMove companionMove : result.companionMoves) {
                validateDestination(
                        companionMove.source,
                        companionMove.target,
                        caseInsensitiveTarget,
                        sourcesByTarget,
                        caseSensitivityByParent,
                        errors);
            }
        }
        if (!errors.isEmpty()) {
            throw new CliException(
                    "Rename preflight failed:" + System.lineSeparator() + String.join(System.lineSeparator(), errors),
                    2);
        }
    }

    private static void validateDestination(
            Path source,
            Path target,
            Predicate<Path> caseInsensitiveTarget,
            Map<String, Path> sourcesByTarget,
            Map<Path, Boolean> caseSensitivityByParent,
            List<String> errors) {
        if (source.equals(target)) {
            return;
        }

        Path parentKey = target.getParent().toAbsolutePath().normalize();
        boolean caseInsensitive =
                caseSensitivityByParent.computeIfAbsent(parentKey, ignored -> caseInsensitiveTarget.test(target));
        String targetKey = collisionKey(target, caseInsensitive);
        Path previous = sourcesByTarget.putIfAbsent(targetKey, source);
        if (previous != null && !previous.equals(source)) {
            errors.add("Multiple files would become " + target);
        }
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(target) || !isCaseOnlyRename(source, target, caseInsensitive)) {
            errors.add("Destination already exists: " + target);
            return;
        }
        try {
            if (!Files.isSameFile(source, target)) {
                errors.add("Destination already exists: " + target);
            }
        } catch (IOException exception) {
            errors.add("Cannot inspect destination: " + target);
        }
    }

    private static boolean isCaseOnlyRename(Path source, Path target, boolean caseInsensitive) {
        String sourcePath = source.toAbsolutePath().normalize().toString();
        String targetPath = target.toAbsolutePath().normalize().toString();
        return caseInsensitive && !sourcePath.equals(targetPath) && sourcePath.equalsIgnoreCase(targetPath);
    }

    private static String collisionKey(Path target, boolean caseInsensitive) {
        String key = target.toAbsolutePath().normalize().toString();
        return caseInsensitive ? key.toLowerCase(Locale.ROOT) : key;
    }

    private static boolean isCaseInsensitiveTarget(Path target) {
        Path directory = target.getParent();
        while (directory != null && !Files.isDirectory(directory)) {
            directory = directory.getParent();
        }
        if (directory != null) {
            try {
                FileStore targetStore = Files.getFileStore(directory);
                Path current = directory;
                while (current != null && Files.getFileStore(current).equals(targetStore)) {
                    Boolean detected = detectCaseInsensitiveDirectory(current);
                    if (detected != null) {
                        return detected;
                    }
                    current = current.getParent();
                }
            } catch (IOException | SecurityException ignored) {
                // Fall through to the platform default.
            }
        }
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static Boolean detectCaseInsensitiveDirectory(Path directory) {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                String alternateName = toggleAsciiCase(entry.getFileName().toString());
                if (alternateName == null) {
                    continue;
                }
                if (hasExactEntry(directory, alternateName)) {
                    return false;
                }
                return Files.exists(directory.resolve(alternateName), LinkOption.NOFOLLOW_LINKS);
            }
        } catch (IOException | SecurityException ignored) {
            // Try an ancestor on the same file store.
        }
        return null;
    }

    private static boolean hasExactEntry(Path directory, String name) throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
            for (Path entry : entries) {
                if (entry.getFileName().toString().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String toggleAsciiCase(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            char replacement;
            if (character >= 'a' && character <= 'z') {
                replacement = Character.toUpperCase(character);
            } else if (character >= 'A' && character <= 'Z') {
                replacement = Character.toLowerCase(character);
            } else {
                continue;
            }
            return value.substring(0, index) + replacement + value.substring(index + 1);
        }
        return null;
    }

    private void applyMoves(List<CliFileResult> results, CliOutput output) {
        for (CliFileResult result : results) {
            if (!canApplyMove(result)) {
                continue;
            }
            try {
                output.progress("Applying " + result.source + " → " + result.target);
                boolean moved = !result.source.getParent().equals(result.target.getParent());
                moveWithCompanions(result);
                result.applied = true;
                result.status = moved ? CliFileResult.Status.MOVED : CliFileResult.Status.RENAMED;
                updateJobPathAfterMove(result.job, result.target);
                recordPersistenceResult(
                        result, AppContext.databaseManager.update(0, result.job, DatabaseManager.INDEX_JOB));
            } catch (Exception exception) {
                result.status = CliFileResult.Status.FAILED;
                result.message = usefulMessage(exception);
            }
        }
    }

    void moveWithCompanions(CliFileResult result) throws IOException {
        Map<CliFileResult.CompanionMove, String> movedCompanions = new LinkedHashMap<>();
        try {
            for (CliFileResult.CompanionMove companionMove : result.companionMoves) {
                String expectedEd2k = hasher.hash(companionMove.source, EnumSet.of(FileHasher.Algorithm.ED2K))
                        .get(FileHasher.Algorithm.ED2K);
                moveVerified(companionMove.source, companionMove.target, expectedEd2k);
                companionMove.applied = true;
                movedCompanions.put(companionMove, expectedEd2k);
            }
            moveVerified(result.source, result.target, result.hashes.get(FileHasher.Algorithm.ED2K));
        } catch (IOException moveFailure) {
            boolean rollbackFailed = false;
            List<Map.Entry<CliFileResult.CompanionMove, String>> movedEntries =
                    new ArrayList<>(movedCompanions.entrySet());
            for (int index = movedEntries.size() - 1; index >= 0; index--) {
                Map.Entry<CliFileResult.CompanionMove, String> entry = movedEntries.get(index);
                CliFileResult.CompanionMove companionMove = entry.getKey();
                try {
                    moveVerified(companionMove.target, companionMove.source, entry.getValue());
                    companionMove.applied = false;
                } catch (IOException rollbackFailure) {
                    moveFailure.addSuppressed(rollbackFailure);
                    rollbackFailed = true;
                }
            }
            if (rollbackFailed) {
                throw new IOException(
                        usefulMessage(moveFailure) + " Companion rollback also failed; inspect the reported paths.",
                        moveFailure);
            }
            throw moveFailure;
        }
    }

    void moveVerified(Path source, Path target, String expectedEd2k) throws IOException {
        Files.createDirectories(target.getParent());
        FileStore sourceStore = Files.getFileStore(source);
        FileStore targetStore = Files.getFileStore(target.getParent());
        if (sourceStore.equals(targetStore)) {
            Files.move(source, target);
            return;
        }

        // Keep the temporary name short and fixed-length: the target name may already be at the
        // filesystem's 255-byte limit, so deriving the temp name from it could overflow that cap.
        Path temporary = target.resolveSibling(".webaom-" + UUID.randomUUID() + ".part");
        try {
            copyPreservingAttributes(source, temporary);
            String copiedEd2k = hasher.hash(temporary, EnumSet.of(FileHasher.Algorithm.ED2K))
                    .get(FileHasher.Algorithm.ED2K);
            if (expectedEd2k != null && !expectedEd2k.equals(copiedEd2k)) {
                throw new IOException("Checksum verification failed for copied file.");
            }
            publishWithoutReplace(temporary, target);
            try {
                Files.delete(source);
            } catch (IOException deleteFailure) {
                if (Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
                    try {
                        Files.deleteIfExists(target);
                    } catch (IOException cleanupFailure) {
                        throw publishedCopyCleanupFailure(source, target, deleteFailure, cleanupFailure);
                    }
                }
                throw deleteFailure;
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void publishWithoutReplace(Path source, Path target) throws IOException {
        Files.move(source, target);
    }

    static void copyPreservingAttributes(Path source, Path target) throws IOException {
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
    }

    static IOException publishedCopyCleanupFailure(
            Path source, Path target, IOException deleteFailure, IOException cleanupFailure) {
        IOException combinedFailure = new IOException(
                "Could not delete source " + source + " or remove copied target " + target + "; both files may remain.",
                deleteFailure);
        combinedFailure.addSuppressed(cleanupFailure);
        return combinedFailure;
    }

    public List<CliFileResult> mylist(
            List<Path> inputs, MylistAction action, MylistOverrides overrides, CliOutput output) throws CliException {
        // STATUS answers from the local database so it works offline; ADD and REMOVE re-query
        // AniDB first so they act on the current MyList entry. The cached hash stays valid, so
        // this is a remote refresh rather than a full re-hash.
        List<CliFileResult> results =
                identify(inputs, output, action == MylistAction.STATUS ? Refresh.NONE : Refresh.REMOTE);
        if (action != MylistAction.STATUS) {
            connection();
        }
        Mylist defaults = mylistDefaults(overrides);
        for (CliFileResult result : results) {
            if (!canContinueAfterIdentify(result, action == MylistAction.STATUS)) {
                continue;
            }
            try {
                switch (action) {
                    case STATUS ->
                        result.status = result.job.mylistId > 0
                                ? CliFileResult.Status.MYLIST_PRESENT
                                : CliFileResult.Status.MYLIST_ABSENT;
                    case ADD -> {
                        AniDBFileClient.MylistAddOutcome outcome =
                                connection.addOrEditMylistEntry(result.job, defaults);
                        if (!outcome.succeeded()) {
                            throw new IOException(connection.getLastError());
                        }
                        result.job.mylistId = outcome.listEntryId();
                        if (outcome.created()) {
                            result.status = CliFileResult.Status.MYLIST_ADDED;
                        } else if (outcome.edited()) {
                            result.status = CliFileResult.Status.MYLIST_ADDED;
                            result.message = "Already in MyList; entry updated.";
                        } else {
                            // The entry exists but AniDB rejected the edit, so the requested state
                            // was not applied; reporting success here would be a lie.
                            result.status = CliFileResult.Status.FAILED;
                            result.message = valueOr(
                                    connection.getLastError(), "Already in MyList; entry could not be updated.");
                        }
                        recordPersistenceResult(
                                result, AppContext.databaseManager.update(0, result.job, DatabaseManager.INDEX_JOB));
                    }
                    case REMOVE -> {
                        if (result.job.mylistId <= 0) {
                            result.status = CliFileResult.Status.MYLIST_ABSENT;
                            result.message = "File is not in MyList.";
                        } else if (connection.removeFromMylist(
                                result.job.mylistId, result.source.getFileName().toString())) {
                            result.job.mylistId = 0;
                            result.status = CliFileResult.Status.MYLIST_REMOVED;
                            recordPersistenceResult(
                                    result,
                                    AppContext.databaseManager.update(0, result.job, DatabaseManager.INDEX_JOB));
                        } else {
                            throw new IOException(connection.getLastError());
                        }
                    }
                }
            } catch (Exception exception) {
                result.status = CliFileResult.Status.FAILED;
                result.message = usefulMessage(exception);
            }
        }
        return results;
    }

    static boolean canContinueAfterIdentify(CliFileResult result, boolean readOnly) {
        return result.status == CliFileResult.Status.IDENTIFIED && (readOnly || !result.failed());
    }

    static boolean canApplyMove(CliFileResult result) {
        return !result.failed()
                && (result.status == CliFileResult.Status.RENAME_PLANNED
                        || result.status == CliFileResult.Status.MOVE_PLANNED);
    }

    static void updateJobPathAfterMove(Job job, Path target) {
        job.currentFile = target.toFile();
        job.targetFile = null;
        job.directoryId = -1;
        job.setStatus(Job.FINISHED, false);
    }

    static void recordPersistenceResult(CliFileResult result, boolean persisted) {
        if (persisted || result.persistenceFailed) {
            return;
        }
        result.persistenceFailed = true;
        String detail = "WebAOM could not update its database; this result was not saved locally.";
        result.message = result.message == null || result.message.isBlank() ? detail : result.message + " " + detail;
    }

    public enum MylistAction {
        ADD,
        REMOVE,
        STATUS
    }

    public record MylistOverrides(Boolean watched, Integer state, String source, String storage, String other) {
        public static MylistOverrides none() {
            return new MylistOverrides(null, null, null, null, null);
        }
    }

    public boolean doctor(CliOutput output) {
        boolean healthy = true;
        output.printText("Configuration: " + options.getFilePath());
        // doctor opens without requiring the database so that database failures become a reported
        // check rather than an abort; diagnosing them is the point of the command.
        if (AppContext.databaseManager != null && AppContext.databaseManager.isConnected()) {
            output.printText("Database: ok");
        } else {
            String failure = openDatabase(options);
            if (failure == null) {
                output.printText("Database: ok");
            } else {
                output.printText("Database: " + failure);
                healthy = false;
            }
        }
        String username = resolvedUsername();
        if (username == null || resolvedPassword() == null) {
            output.printText("Credentials: missing (run `webaom login` or set WEBAOM_USERNAME/WEBAOM_PASSWORD)");
            healthy = false;
        } else {
            output.printText("Credentials: configured for " + username);
            try {
                connection();
                output.printText("AniDB: authenticated");
            } catch (Exception exception) {
                output.printText("AniDB: " + usefulMessage(exception));
                healthy = false;
            }
        }
        return healthy;
    }

    AniDBFileClient connection() throws CliException {
        if (connection != null) {
            return connection;
        }
        String username = resolvedUsername();
        String password = resolvedPassword();
        if (username == null || password == null) {
            throw new CliException(
                    "AniDB credentials are missing. Run `webaom login` or set WEBAOM_USERNAME and WEBAOM_PASSWORD.", 2);
        }

        String host = valueOr(options.getString(Options.STR_HOST_URL), AniDBConnection.DEFAULT_HOST);
        int remotePort = positiveOr(options.getInteger(Options.INT_REMOTE_PORT), AniDBConnection.DEFAULT_REMOTE_PORT);
        int localPort = options.getInteger(Options.INT_LOCAL_PORT);
        if (localPort < 0 || localPort > 65_535) {
            localPort = AniDBConnection.DEFAULT_LOCAL_PORT;
        }
        int timeout = positiveOr(options.getInteger(Options.INT_TIMEOUT), 20);
        int delaySeconds = positiveOr(options.getInteger(Options.INT_DATAGRAM_DELAY), 4);
        AniDBConnectionSettings settings = new AniDBConnectionSettings(
                host,
                remotePort,
                localPort,
                timeout,
                delaySeconds * 1000,
                3,
                options.getBoolean(Options.BOOL_NAT_KEEP_ALIVE));
        AniDBFileClient candidate = connectionFactory.create(settings);
        candidate.setLoginPromptEnabled(false);
        String apiKey = resolvedApiKey();
        candidate.set(username, password, apiKey);
        try {
            if (!candidate.connect()) {
                throw new CliException(valueOr(candidate.getLastError(), "Could not connect to AniDB."), 2);
            }
            if (candidate.encrypt() < 0) {
                throw new CliException("AniDB encryption setup failed.", 2);
            }
            if (!candidate.login()) {
                throw new CliException("AniDB authentication failed.", 2);
            }
            connection = candidate;
        } catch (IOException | AniDBException exception) {
            candidate.disconnect();
            throw new CliException(usefulMessage(exception), 2, exception);
        } catch (CliException exception) {
            candidate.disconnect();
            throw exception;
        }
        return connection;
    }

    private EnumSet<FileHasher.Algorithm> configuredAlgorithms() {
        EnumSet<FileHasher.Algorithm> result = EnumSet.of(FileHasher.Algorithm.ED2K);
        if (options.getBoolean(Options.BOOL_HASH_CRC)) result.add(FileHasher.Algorithm.CRC32);
        if (options.getBoolean(Options.BOOL_HASH_MD5)) result.add(FileHasher.Algorithm.MD5);
        if (options.getBoolean(Options.BOOL_HASH_SHA)) result.add(FileHasher.Algorithm.SHA1);
        if (options.getBoolean(Options.BOOL_HASH_TTH)) result.add(FileHasher.Algorithm.TTH);
        return result;
    }

    private static Map<FileHasher.Algorithm, String> jobHashes(Job job) {
        Map<FileHasher.Algorithm, String> hashes = new HashMap<>();
        putIfPresent(hashes, FileHasher.Algorithm.ED2K, job.ed2kHash);
        putIfPresent(hashes, FileHasher.Algorithm.CRC32, job.crc32Hash);
        putIfPresent(hashes, FileHasher.Algorithm.MD5, job.md5Hash);
        putIfPresent(hashes, FileHasher.Algorithm.SHA1, job.sha1Hash);
        putIfPresent(hashes, FileHasher.Algorithm.TTH, job.tthHash);
        return hashes;
    }

    private static void applyHashes(Job job, Map<FileHasher.Algorithm, String> hashes) {
        job.fileSize = job.currentFile.length();
        job.ed2kHash = hashes.get(FileHasher.Algorithm.ED2K);
        job.crc32Hash = hashes.get(FileHasher.Algorithm.CRC32);
        job.md5Hash = hashes.get(FileHasher.Algorithm.MD5);
        job.sha1Hash = hashes.get(FileHasher.Algorithm.SHA1);
        job.tthHash = hashes.get(FileHasher.Algorithm.TTH);
    }

    private Mylist mylistDefaults(MylistOverrides overrides) {
        Mylist mylist = new Mylist();
        mylist.state = overrides.state() == null ? options.getInteger(Options.INT_FILE_STATE) : overrides.state();
        mylist.filestate = 0;
        boolean watched = overrides.watched() == null ? options.getBoolean(Options.BOOL_WATCHED) : overrides.watched();
        mylist.viewed = watched ? 1 : 0;
        mylist.storage =
                overrides.storage() == null ? valueOr(options.getString(Options.STR_STORAGE), "") : overrides.storage();
        mylist.source = overrides.source() == null
                ? valueOr(options.getString(Options.STR_SOURCE_FOLDER), "")
                : overrides.source();
        mylist.other =
                overrides.other() == null ? valueOr(options.getString(Options.STR_OTHER_INFO), "") : overrides.other();
        return mylist;
    }

    private String resolvedUsername() {
        return firstNonBlank(System.getenv("WEBAOM_USERNAME"), AppContext.userPass.username);
    }

    private String resolvedPassword() {
        return firstNonBlank(System.getenv("WEBAOM_PASSWORD"), AppContext.userPass.password);
    }

    private String resolvedApiKey() {
        return firstNonBlank(System.getenv("WEBAOM_API_KEY"), AppContext.userPass.apiKey);
    }

    @Override
    public synchronized void close() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException ignored) {
                // The JVM is already shutting down; the hook itself is running close().
            }
            shutdownHook = null;
        }
        if (connection != null) {
            try {
                connection.logout();
            } catch (Exception ignored) {
                // Best-effort logout.
            } finally {
                connection.disconnect();
                connection = null;
            }
        }
        if (AppContext.databaseManager != null) {
            AppContext.databaseManager.shutdown();
            AppContext.databaseManager = null;
        }
    }

    private static Path expandHome(Path path) {
        String value = path.toString();
        if (value.equals("~")) {
            return Path.of(System.getProperty("user.home"));
        }
        if (value.startsWith("~" + java.io.File.separator)) {
            return Path.of(System.getProperty("user.home")).resolve(value.substring(2));
        }
        return path;
    }

    private static void putIfPresent(Map<FileHasher.Algorithm, String> map, FileHasher.Algorithm key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private static String baseName(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(0, dotIndex) : filename;
    }

    private static int positiveOr(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    public static Path defaultConfigPath() {
        return Path.of(PlatformPaths.getConfigFilePath());
    }

    @FunctionalInterface
    interface ConnectionFactory {
        AniDBFileClient create(AniDBConnectionSettings settings);
    }
}
