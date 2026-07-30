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

import static epox.webaom.cli.CliText.firstNonBlank;
import static epox.webaom.cli.CliText.usefulMessage;
import static epox.webaom.cli.CliText.valueOr;

import epox.util.UserPass;
import epox.webaom.AppContext;
import epox.webaom.Options;
import epox.webaom.cli.CliApplication.MylistAction;
import epox.webaom.cli.CliApplication.MylistOverrides;
import epox.webaom.cli.CliOutput.Format;
import epox.webaom.cli.CliStyle.Mode;
import epox.webaom.util.PlatformPaths;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

/** Picocli command tree for the headless WebAOM interface. */
@Command(
        name = "webaom",
        description = "Identify and rename anime files with AniDB.",
        mixinStandardHelpOptions = true,
        versionProvider = Cli.VersionProvider.class,
        subcommands = {
            Cli.IdentifyCommand.class,
            Cli.RenameCommand.class,
            Cli.HashCommand.class,
            Cli.MylistCommand.class,
            Cli.RulesCommand.class,
            Cli.ConfigCommand.class,
            Cli.LoginCommand.class,
            Cli.DoctorCommand.class,
            Cli.DbCommand.class
        })
public final class Cli implements Callable<Integer> {
    @Spec
    private CommandSpec spec;

    @Option(
            names = "--config",
            paramLabel = "FILE",
            description = "Use an explicit WebAOM configuration file.",
            scope = ScopeType.INHERIT)
    private Path configPath;

    @Option(
            names = {"-q", "--quiet"},
            description = "Suppress progress output.",
            scope = ScopeType.INHERIT)
    private boolean quiet;

    @Option(
            names = {"-v", "--verbose"},
            description = "Show additional detail.",
            scope = ScopeType.INHERIT)
    private boolean verbose;

    @Option(
            names = "--color",
            paramLabel = "WHEN",
            description = "Color output: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).",
            defaultValue = "auto",
            scope = ScopeType.INHERIT)
    private Mode colorMode = Mode.AUTO;

    @Option(
            names = "--hyperlinks",
            paramLabel = "WHEN",
            description = "OSC 8 hyperlinks: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).",
            defaultValue = "auto",
            scope = ScopeType.INHERIT)
    private Mode hyperlinkMode = Mode.AUTO;

    @Option(names = "--no-color", description = "Disable colored output.", scope = ScopeType.INHERIT)
    private boolean noColor;

    private boolean interactive;
    private CliPrompt.Factory promptFactory = CliPrompt::open;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    public static int execute(String... args) {
        return execute(
                new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true),
                new PrintWriter(new OutputStreamWriter(System.err, StandardCharsets.UTF_8), true),
                terminalInteractive(),
                args);
    }

    static int execute(PrintWriter out, PrintWriter err, String... args) {
        return execute(out, err, false, CliPrompt::open, args);
    }

    static int execute(PrintWriter out, PrintWriter err, boolean interactive, String... args) {
        return execute(out, err, interactive, CliPrompt::open, args);
    }

    static int execute(
            PrintWriter out, PrintWriter err, boolean interactive, CliPrompt.Factory promptFactory, String... args) {
        Cli root = new Cli();
        root.interactive = interactive;
        root.promptFactory = promptFactory;
        CommandLine commandLine = new CommandLine(root);
        commandLine.setOut(out);
        commandLine.setErr(err);
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        CliStyle handlerStyle = styleFromArguments(interactive, args);
        CommandLine.Help.Ansi helpAnsi =
                handlerStyle.colorsEnabled() ? CommandLine.Help.Ansi.ON : CommandLine.Help.Ansi.OFF;
        commandLine.setColorScheme(CommandLine.Help.defaultColorScheme(helpAnsi));
        commandLine.setExecutionExceptionHandler((exception, cmd, parseResult) -> {
            Throwable cause = exception;
            while (true) {
                if (cause instanceof CliException cliException) {
                    String message = handlerStyle.messageWithPath(cliException.getMessage(), cliException.linkedPath());
                    if (cliException.isError()) {
                        cmd.getErr().println(handlerStyle.error("Error:") + " " + message);
                    } else {
                        cmd.getErr().println(message);
                    }
                    return cliException.exitCode();
                }
                if (cause.getCause() == null || cause == cause.getCause()) {
                    break;
                }
                cause = cause.getCause();
            }
            cmd.getErr().println(handlerStyle.error("Error:") + " " + handlerStyle.text(usefulMessage(cause)));
            return 1;
        });
        commandLine.setParameterExceptionHandler((exception, arguments) -> {
            exception
                    .getCommandLine()
                    .getErr()
                    .println(handlerStyle.error("Error:") + " " + handlerStyle.text(exception.getMessage()));
            exception
                    .getCommandLine()
                    .getErr()
                    .println(handlerStyle.detail(
                            "Try '" + exception.getCommandLine().getCommandName() + " --help' for more information."));
            return 2;
        });
        return commandLine.execute(args);
    }

    private static CliStyle styleFromArguments(boolean interactive, String... args) {
        Mode color = Mode.AUTO;
        Mode hyperlinks = Mode.AUTO;
        boolean noColor = false;
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if ("--".equals(argument)) {
                break;
            } else if ("--no-color".equals(argument)) {
                noColor = true;
            } else if (argument.startsWith("--color=")) {
                color = Mode.parse(argument.substring("--color=".length()));
            } else if ("--color".equals(argument) && index + 1 < args.length) {
                color = Mode.parse(args[++index]);
            } else if (argument.startsWith("--hyperlinks=")) {
                hyperlinks = Mode.parse(argument.substring("--hyperlinks=".length()));
            } else if ("--hyperlinks".equals(argument) && index + 1 < args.length) {
                hyperlinks = Mode.parse(args[++index]);
            }
        }
        String noColorEnvironment = System.getenv("NO_COLOR");
        boolean noColorPresent = noColorEnvironment != null && !noColorEnvironment.isEmpty();
        return CliStyle.resolve(noColor ? Mode.NEVER : color, hyperlinks, interactive, noColorPresent);
    }

    private static boolean terminalInteractive() {
        String terminal = System.getenv("TERM");
        return System.console() != null && (terminal == null || !"dumb".equalsIgnoreCase(terminal));
    }

    private CliStyle style() {
        String noColorEnvironment = System.getenv("NO_COLOR");
        boolean noColorPresent = noColorEnvironment != null && !noColorEnvironment.isEmpty();
        return CliStyle.resolve(noColor ? Mode.NEVER : colorMode, hyperlinkMode, interactive, noColorPresent);
    }

    private CliOutput output(CommandSpec commandSpec) {
        Format format = commandSpec.userObject() instanceof FileCommand fileCommand ? fileCommand.format : Format.TABLE;
        return new CliOutput(
                commandSpec.commandLine().getOut(),
                commandSpec.commandLine().getErr(),
                format,
                quiet,
                verbose,
                style());
    }

    private CliApplication open(boolean database) throws CliException {
        return CliApplication.open(configPath, database);
    }

    private CliInput openPrompt(String command) throws CliException {
        if (!interactive) {
            throw new CliException(command + " requires an interactive terminal.", 2);
        }
        return promptFactory.open(style());
    }

    private String readConfirmation(String prompt) {
        if (!interactive) {
            return null;
        }
        try (CliInput input = openPrompt("Database upgrade")) {
            return input.readLine(prompt);
        } catch (CliPrompt.Cancelled cancelled) {
            throw cancelled;
        } catch (CliException | IOException exception) {
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private static Cli root(CommandSpec spec) {
        return (Cli) spec.root().userObject();
    }

    private static int resultCode(List<CliFileResult> results) {
        return results.stream().anyMatch(CliFileResult::failed) ? 1 : 0;
    }

    public static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {"WebAOM " + AppContext.VERSION};
        }
    }

    private abstract static class FileCommand implements Callable<Integer> {
        @Spec
        protected CommandSpec spec;

        @Option(
                names = "--format",
                paramLabel = "FORMAT",
                description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).",
                defaultValue = "table")
        protected Format format;

        @Parameters(arity = "1..*", paramLabel = "PATH", description = "Files or directories to process.")
        protected List<Path> paths;

        protected Cli root() {
            return Cli.root(spec);
        }
    }

    @Command(name = "identify", description = "Identify files using AniDB.", mixinStandardHelpOptions = true)
    static final class IdentifyCommand extends FileCommand {
        @Option(names = "--refresh", description = "Ignore cached results and query AniDB again.")
        private boolean refresh;

        @Override
        public Integer call() throws Exception {
            Cli root = root();
            CliOutput output = root.output(spec);
            try (CliApplication application = root.open(true)) {
                List<CliFileResult> results = application.identify(
                        paths, output, refresh ? CliApplication.Refresh.FULL : CliApplication.Refresh.NONE);
                output.printResults(results, CliOutput.ResultView.IDENTIFY);
                return resultCode(results);
            }
        }
    }

    @Command(name = "rename", description = "Preview or apply rule-based file names.", mixinStandardHelpOptions = true)
    static final class RenameCommand extends FileCommand {
        @Option(names = "--apply", description = "Apply the displayed filesystem changes.")
        private boolean apply;

        @Option(names = "--move", description = "Also apply the configured move rules.")
        private boolean move;

        @Option(names = "--rules", paramLabel = "FILE", description = "Use a rules document for this run only.")
        private Path rulesFile;

        @Option(names = "--refresh", description = "Ignore cached results and query AniDB again.")
        private boolean refresh;

        @Override
        public Integer call() throws Exception {
            Cli root = root();
            CliOutput output = root.output(spec);
            RuleDocument override = null;
            if (rulesFile != null) {
                override = RuleDocument.parse(Files.readString(rulesFile, StandardCharsets.UTF_8));
            }
            try (CliApplication application = root.open(true)) {
                List<CliFileResult> results = application.rename(
                        paths,
                        move,
                        apply,
                        override,
                        output,
                        refresh ? CliApplication.Refresh.FULL : CliApplication.Refresh.NONE);
                output.printResults(results, CliOutput.ResultView.RENAME_PREVIEW);
                if (!apply && format == Format.TABLE) {
                    long changes = results.stream()
                            .filter(result -> result.target != null && !result.source.equals(result.target))
                            .mapToLong(result -> 1L + result.companionMoves.size())
                            .sum();
                    output.printWarning("Preview only. Run again with --apply to change " + changes + " file"
                            + (changes == 1 ? "." : "s."));
                }
                return resultCode(results);
            }
        }
    }

    @Command(
            name = "hash",
            description = "Calculate file hashes without contacting AniDB.",
            mixinStandardHelpOptions = true)
    static final class HashCommand extends FileCommand {
        @Option(names = "--all", description = "Calculate every supported hash.")
        private boolean all;

        @Option(names = "--crc32", description = "Also calculate CRC32.")
        private boolean crc32;

        @Option(names = "--md5", description = "Also calculate MD5.")
        private boolean md5;

        @Option(names = "--sha1", description = "Also calculate SHA-1.")
        private boolean sha1;

        @Option(names = "--tth", description = "Also calculate Tiger Tree Hash.")
        private boolean tth;

        @Override
        public Integer call() throws Exception {
            EnumSet<FileHasher.Algorithm> algorithms = EnumSet.of(FileHasher.Algorithm.ED2K);
            if (all || crc32) algorithms.add(FileHasher.Algorithm.CRC32);
            if (all || md5) algorithms.add(FileHasher.Algorithm.MD5);
            if (all || sha1) algorithms.add(FileHasher.Algorithm.SHA1);
            if (all || tth) algorithms.add(FileHasher.Algorithm.TTH);

            Cli root = root();
            CliOutput output = root.output(spec);
            try (CliApplication application = root.open(false)) {
                List<CliFileResult> results = application.hash(paths, algorithms, output);
                output.printResults(results, CliOutput.ResultView.STANDARD);
                return resultCode(results);
            }
        }
    }

    @Command(
            name = "mylist",
            description = "Manage AniDB MyList entries.",
            mixinStandardHelpOptions = true,
            subcommands = {MylistAddCommand.class, MylistRemoveCommand.class, MylistStatusCommand.class})
    static final class MylistCommand implements Callable<Integer> {
        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            spec.commandLine().usage(spec.commandLine().getOut());
            return 0;
        }
    }

    private abstract static class MylistFileCommand extends FileCommand {
        protected abstract MylistAction action();

        protected MylistOverrides overrides() {
            return MylistOverrides.none();
        }

        @Override
        public Integer call() throws Exception {
            Cli root = root();
            CliOutput output = root.output(spec);
            try (CliApplication application = root.open(true)) {
                List<CliFileResult> results = application.mylist(paths, action(), overrides(), output);
                output.printResults(results, CliOutput.ResultView.STANDARD);
                return resultCode(results);
            }
        }
    }

    @Command(name = "add", description = "Add files to MyList.", mixinStandardHelpOptions = true)
    static final class MylistAddCommand extends MylistFileCommand {
        @Option(names = "--watched", negatable = true, description = "Override the saved watched state.")
        private Boolean watched;

        @Option(names = "--state", paramLabel = "STATE", description = "Storage state: ${COMPLETION-CANDIDATES}.")
        private MylistState state;

        @Option(names = "--source", paramLabel = "TEXT", description = "Override the saved source.")
        private String source;

        @Option(names = "--storage", paramLabel = "TEXT", description = "Override the saved storage location.")
        private String storage;

        @Option(names = "--other", paramLabel = "TEXT", description = "Override the saved notes.")
        private String other;

        @Override
        protected MylistAction action() {
            return MylistAction.ADD;
        }

        @Override
        protected MylistOverrides overrides() {
            return new MylistOverrides(watched, state == null ? null : state.apiValue, source, storage, other);
        }
    }

    @Command(name = "remove", description = "Remove files from MyList.", mixinStandardHelpOptions = true)
    static final class MylistRemoveCommand extends MylistFileCommand {
        @Override
        protected MylistAction action() {
            return MylistAction.REMOVE;
        }
    }

    @Command(name = "status", description = "Show whether files are in MyList.", mixinStandardHelpOptions = true)
    static final class MylistStatusCommand extends MylistFileCommand {
        @Override
        protected MylistAction action() {
            return MylistAction.STATUS;
        }
    }

    enum MylistState {
        UNKNOWN(0),
        HDD(1),
        CD(2),
        DELETED(3),
        REMOTE(4);

        private final int apiValue;

        MylistState(int apiValue) {
            this.apiValue = apiValue;
        }
    }

    @Command(
            name = "rules",
            description = "Inspect and edit rename and move rules.",
            mixinStandardHelpOptions = true,
            subcommands = {RulesShowCommand.class, RulesEditCommand.class, RulesCheckCommand.class})
    static final class RulesCommand implements Callable<Integer> {
        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            spec.commandLine().usage(spec.commandLine().getOut());
            return 0;
        }
    }

    private abstract static class SimpleCommand implements Callable<Integer> {
        @Spec
        protected CommandSpec spec;

        protected Cli root() {
            return Cli.root(spec);
        }
    }

    @Command(name = "show", description = "Print the active rules.", mixinStandardHelpOptions = true)
    static final class RulesShowCommand extends SimpleCommand {
        @Override
        public Integer call() throws Exception {
            Cli root = root();
            try (CliApplication application = root.open(false)) {
                root.output(spec)
                        .printText(RuleDocument.fromRules(application.rules()).format());
                return 0;
            }
        }
    }

    @Command(name = "check", description = "Validate saved rules or a rules file.", mixinStandardHelpOptions = true)
    static final class RulesCheckCommand extends SimpleCommand {
        @Parameters(arity = "0..1", paramLabel = "FILE", description = "Rules document to validate.")
        private Path rulesFile;

        @Override
        public Integer call() throws Exception {
            Cli root = root();
            CliOutput output = root.output(spec);
            if (rulesFile != null) {
                RuleDocument.parse(Files.readString(rulesFile, StandardCharsets.UTF_8));
            } else {
                try (CliApplication application = root.open(false)) {
                    RuleDocument.parse(
                            RuleDocument.fromRules(application.rules()).format());
                }
            }
            output.printSuccess("Rules are valid.");
            return 0;
        }
    }

    @Command(name = "edit", description = "Edit and validate the active rules.", mixinStandardHelpOptions = true)
    static final class RulesEditCommand extends SimpleCommand {
        @Override
        public Integer call() throws Exception {
            Cli root = root();
            CliOutput output = root.output(spec);
            try (CliApplication application = root.open(false)) {
                Path temporary = Files.createTempFile("webaom-rules-", ".conf");
                editRules(application, output, temporary, RulesEditCommand::runEditor);
            }
            return 0;
        }

        static void editRules(CliApplication application, CliOutput output, Path temporary, RulesEditor rulesEditor)
                throws Exception {
            boolean keepTemporary = false;
            try {
                Files.writeString(
                        temporary, RuleDocument.fromRules(application.rules()).format(), StandardCharsets.UTF_8);
                keepTemporary = true;
                try {
                    rulesEditor.edit(temporary);
                } catch (Exception exception) {
                    if (exception instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    int exitCode = exception instanceof CliException cliException ? cliException.exitCode() : 1;
                    throw CliException.withPath(
                            "Editor failed; your rules file was kept at "
                                    + temporary
                                    + System.lineSeparator()
                                    + exception.getMessage(),
                            exitCode,
                            exception,
                            temporary);
                }
                RuleDocument edited;
                try {
                    edited = RuleDocument.parse(Files.readString(temporary, StandardCharsets.UTF_8));
                } catch (IllegalArgumentException exception) {
                    throw CliException.withPath(
                            "Rules are invalid; your edited file was kept at "
                                    + temporary
                                    + System.lineSeparator()
                                    + exception.getMessage(),
                            1,
                            exception,
                            temporary);
                }
                application.rules().setRenameRules(edited.renameRules());
                application.rules().setMoveRules(edited.moveRules());
                application.rules().saveToOptions(application.options());
                if (!application.options().saveToFileUnchecked()) {
                    throw CliException.withPath(
                            "Could not save the WebAOM configuration; your edited file was kept at " + temporary,
                            1,
                            null,
                            temporary);
                }
                keepTemporary = false;
                output.printSuccess("Rules saved.");
            } finally {
                if (!keepTemporary) {
                    Files.deleteIfExists(temporary);
                }
            }
        }

        @FunctionalInterface
        interface RulesEditor {
            void edit(Path file) throws Exception;
        }

        private static void runEditor(Path file) throws IOException, InterruptedException, CliException {
            String editor = firstNonBlank(System.getenv("VISUAL"), System.getenv("EDITOR"));
            Process process;
            if (editor != null) {
                if (isWindows()) {
                    process = new ProcessBuilder(windowsEditorCommand(editor, file))
                            .inheritIO()
                            .start();
                } else {
                    process = new ProcessBuilder("/bin/sh", "-c", editor + " \"$1\"", "webaom-editor", file.toString())
                            .inheritIO()
                            .start();
                }
            } else if (isWindows()) {
                process = new ProcessBuilder("notepad", file.toString())
                        .inheritIO()
                        .start();
            } else {
                process = new ProcessBuilder("vi", file.toString()).inheritIO().start();
            }
            int exit = process.waitFor();
            if (exit != 0) {
                throw new CliException("Editor exited with status " + exit + ".", 1);
            }
        }

        static List<String> windowsEditorCommand(String editor, Path file) throws CliException {
            List<String> command = new ArrayList<>();
            StringBuilder argument = new StringBuilder();
            boolean quoted = false;
            boolean argumentStarted = false;
            for (int index = 0; index < editor.length(); index++) {
                char current = editor.charAt(index);
                if (current == '"') {
                    quoted = !quoted;
                    argumentStarted = true;
                } else if (Character.isWhitespace(current) && !quoted) {
                    if (argumentStarted) {
                        command.add(argument.toString());
                        argument.setLength(0);
                        argumentStarted = false;
                    }
                } else {
                    argument.append(current);
                    argumentStarted = true;
                }
            }
            if (quoted) {
                throw new CliException("VISUAL or EDITOR contains an unmatched quote.", 2);
            }
            if (argumentStarted) {
                command.add(argument.toString());
            }
            if (command.isEmpty()) {
                throw new CliException("VISUAL or EDITOR does not contain an editor command.", 2);
            }
            command.add(file.toString());
            return command;
        }
    }

    @Command(
            name = "config",
            description = "Inspect the effective configuration.",
            mixinStandardHelpOptions = true,
            subcommands = {ConfigShowCommand.class, ConfigPathCommand.class})
    static final class ConfigCommand implements Callable<Integer> {
        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            spec.commandLine().usage(spec.commandLine().getOut());
            return 0;
        }
    }

    @Command(name = "show", description = "Show non-secret effective settings.", mixinStandardHelpOptions = true)
    static final class ConfigShowCommand extends SimpleCommand {
        @Override
        public Integer call() throws Exception {
            Cli root = root();
            try (CliApplication application = root.open(false)) {
                Options options = application.options();
                CliOutput output = root.output(spec);
                output.printPath("Configuration", Path.of(options.getFilePath()));
                output.printValue("AniDB host", valueOr(options.getString(Options.STR_HOST_URL), "api.anidb.net"));
                output.printValue("Automatic GUI rename", String.valueOf(options.getBoolean(Options.BOOL_AUTO_RENAME)));
                output.printValue(
                        "Automatic GUI MyList add", String.valueOf(options.getBoolean(Options.BOOL_ADD_FILE)));
                Path databasePath = databasePath(options);
                if (databasePath == null) {
                    output.printValue("Database", databaseLocation(options));
                } else {
                    output.printPath("Database", databasePath);
                }
                return 0;
            }
        }
    }

    @Command(name = "path", description = "Print configuration and database paths.", mixinStandardHelpOptions = true)
    static final class ConfigPathCommand extends SimpleCommand {
        @Override
        public Integer call() throws Exception {
            Cli root = root();
            try (CliApplication application = root.open(false)) {
                Options options = application.options();
                CliOutput output = root.output(spec);
                output.printPath("Configuration", Path.of(options.getFilePath()));
                Path databasePath = databasePath(options);
                if (databasePath == null) {
                    output.printValue("Database", databaseLocation(options));
                } else {
                    output.printPath("Database", databasePath);
                }
                return 0;
            }
        }
    }

    private static Path databasePath(Options options) {
        return valueOr(options.getString(Options.STR_DATABASE_URL), null) == null
                ? Path.of(PlatformPaths.getDefaultEmbeddedDatabasePath())
                : null;
    }

    static String databaseLocation(Options options) {
        String configured = valueOr(options.getString(Options.STR_DATABASE_URL), null);
        if (configured == null) {
            return PlatformPaths.getDefaultEmbeddedDatabasePath().toString();
        }
        return configured
                .replaceFirst("(?i)(jdbc:[^:]+://)[^/]+@", "$1<redacted>@")
                .replaceAll(
                        "(?i)([?&;](?:user|[^=&;]*(?:password|passwd|passphrase|secret|token)[^=&;]*)=)[^&;#]*",
                        "$1<redacted>");
    }

    @Command(name = "login", description = "Save AniDB credentials securely.", mixinStandardHelpOptions = true)
    static final class LoginCommand extends SimpleCommand {
        @Override
        public Integer call() throws Exception {
            Cli root = root();
            CliOutput output = root.output(spec);
            try (CliApplication application = root.open(false)) {
                try (CliInput input = root.openPrompt("Login")) {
                    String current = AppContext.userPass.username;
                    String prompt = current == null || current.isBlank()
                            ? "AniDB username: "
                            : "AniDB username [" + current + "]: ";
                    String username = input.readLine(prompt);
                    if (username == null || username.isBlank()) {
                        username = current;
                    }
                    String password = input.readSecret("AniDB password: ");
                    String apiKey = input.readSecret("AniDB API key (optional): ");
                    if (username == null || username.isBlank() || password == null || password.isBlank()) {
                        throw new CliException("Username and password are required.", 2);
                    }
                    UserPass credentials = new UserPass(username, password, apiKey);
                    application.options().setBoolean(Options.BOOL_STORE_PASSWORD, true);
                    application.options().setString(Options.STR_USERNAME, credentials.get(true));
                    if (!application.options().saveToFileUnchecked()) {
                        throw new CliException("Could not save the WebAOM configuration.", 1);
                    }
                    output.printSuccess("Credentials saved for " + username + ".");
                    return 0;
                }
            } catch (CliPrompt.Cancelled cancelled) {
                throw CliException.cancelled("Login cancelled.", cancelled.exitCode());
            }
        }
    }

    @Command(
            name = "doctor",
            description = "Check configuration, database, credentials, and AniDB.",
            mixinStandardHelpOptions = true)
    static final class DoctorCommand extends SimpleCommand {
        @Override
        public Integer call() throws Exception {
            Cli root = root();
            CliOutput output = root.output(spec);
            try (CliApplication application = root.open(false)) {
                return application.doctor(output) ? 0 : 1;
            }
        }
    }

    @Command(
            name = "db",
            description = "Manage the WebAOM database.",
            mixinStandardHelpOptions = true,
            subcommands = {DbUpgradeCommand.class})
    static final class DbCommand implements Callable<Integer> {
        @Spec
        private CommandSpec spec;

        @Override
        public Integer call() {
            spec.commandLine().usage(spec.commandLine().getOut());
            return 0;
        }
    }

    @Command(
            name = "upgrade",
            description = "Upgrade the database schema, with explicit consent.",
            mixinStandardHelpOptions = true)
    static final class DbUpgradeCommand extends SimpleCommand {
        @Option(names = "--yes", description = "Confirm the upgrade without an interactive prompt.")
        private boolean yes;

        @Override
        public Integer call() throws Exception {
            Cli root = root();
            CliOutput output = root.output(spec);
            try (CliApplication application = root.open(false)) {
                application.setConsoleReader(root::readConfirmation);
                return application.upgradeDatabase(output, yes) ? 0 : 1;
            }
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
