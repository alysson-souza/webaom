package epox.webaom.startup;

import epox.webaom.Options;
import epox.webaom.util.PlatformPaths;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates application startup conditions and provides graceful degradation when optional features
 * (logging, database) are unavailable.
 */
public final class StartupValidator {

    /** Private constructor to prevent instantiation of utility class. */
    private StartupValidator() {
        // Utility class
    }

    /**
     * Validate logging configuration and handle directory creation.
     *
     * @param autoLogEnabled
     *            whether auto-logging is enabled
     * @param logFilePath
     *            the configured log file path (can be null/empty for defaults)
     * @return the validated/corrected log file path, or null if logging should be disabled
     */
    public static String validateLogging(boolean autoLogEnabled, String logFilePath) {
        if (!autoLogEnabled) {
            return null; // Logging not enabled
        }

        // Use default if not specified
        if (logFilePath == null || logFilePath.trim().isEmpty()) {
            logFilePath = PlatformPaths.getDefaultLogFilePath();
        }

        // Ensure parent directory exists
        if (!PlatformPaths.ensureParentDirectoryExists(logFilePath)) {
            // Cannot create log directory - logging will be disabled
            return null;
        }

        return logFilePath;
    }

    /**
     * Validate database configuration and provide helpful information. Only reports actual problems
     * - H2 fallback is silent and expected behavior.
     *
     * @param autoLoadDBEnabled
     *            whether auto-loading DB is enabled
     * @param dbUrl
     *            the configured database URL
     * @return list of startup issues found (empty if all is well)
     */
    public static List<StartupIssue> validateDatabase(boolean autoLoadDBEnabled, String dbUrl) {
        List<StartupIssue> issues = new ArrayList<>();

        if (!autoLoadDBEnabled) {
            return issues; // Database not enabled
        }

        // Empty URL is fine - will use H2 fallback silently
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            return issues;
        }

        // Only warn if URL is provided but looks invalid
        String dbUrlLower = dbUrl.toLowerCase();
        boolean hasPostgresql = dbUrlLower.contains("postgresql");
        boolean hasMysql = dbUrlLower.contains("mysql");
        boolean hasH2 = dbUrlLower.contains("h2");
        boolean hasJdbc = dbUrlLower.contains("jdbc:");
        boolean looksLikeJdbcUrl = hasPostgresql || hasMysql || hasH2 || hasJdbc;
        if (!looksLikeJdbcUrl) {
            String message = "Database URL doesn't look like a valid JDBC URL: " + dbUrl;
            String formatHint = "Expected format: jdbc:postgresql://host:port/db";
            String altFormat = " or jdbc:mysql://host:port/db";
            String fallbackNote = "\nThe application will use embedded H2 database as fallback.";
            String suggestion = formatHint + altFormat + fallbackNote;
            String title = "Invalid Database URL";
            StartupIssue issue = new StartupIssue(StartupIssue.Severity.WARN, title, message, suggestion);
            issues.add(issue);
        }

        return issues;
    }

    /**
     * Validate overall startup configuration. Returns a list of issues found during startup.
     *
     * @param options
     *            the Options instance to validate
     * @return list of startup issues (empty if everything is okay)
     */
    public static List<StartupIssue> validateStartup(Options options) {
        List<StartupIssue> issues = new ArrayList<>();

        if (options == null) {
            return issues; // No config to validate
        }

        // Validate logging
        boolean autoLogEnabled = options.getBoolean(Options.BOOL_AUTO_LOGIN);
        String logFilePath = options.getString(Options.STR_LOG_FILE);
        String validatedLogPath = validateLogging(autoLogEnabled, logFilePath);

        // If logging validation returned null, add the WARN issue
        boolean logPathWasProvided = logFilePath != null && !logFilePath.trim().isEmpty();
        if (autoLogEnabled && validatedLogPath == null && logPathWasProvided) {
            issues.add(new StartupIssue(
                    StartupIssue.Severity.WARN,
                    "Logging Disabled",
                    "Failed to create or access log directory: " + logFilePath,
                    "Automatic logging has been disabled. You can enable it again in"
                            + " Options when the issue is resolved."));
        }

        // Validate database
        boolean autoLoadDatabaseEnabled = options.getBoolean(Options.BOOL_AUTO_LOAD_DATABASE);
        String databaseUrl = options.getString(Options.STR_DATABASE_URL);
        List<StartupIssue> databaseIssues = validateDatabase(autoLoadDatabaseEnabled, databaseUrl);
        issues.addAll(databaseIssues);

        return issues;
    }

    /**
     * Format startup issues for display to user.
     *
     * @param issues
     *            list of startup issues
     * @return formatted string for display (multiple issues separated by blank lines)
     */
    public static String formatIssuesForDisplay(List<StartupIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "";
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int issueIndex = 0; issueIndex < issues.size(); issueIndex++) {
            if (issueIndex > 0) {
                messageBuilder.append("\n\n---\n\n");
            }
            messageBuilder.append(issues.get(issueIndex).getFormattedMessage());
        }
        return messageBuilder.toString();
    }

    /**
     * Check if there are any error-level issues in the list.
     *
     * @param issues
     *            list of startup issues
     * @return true if any ERROR severity issues exist
     */
    public static boolean hasErrors(List<StartupIssue> issues) {
        if (issues == null) {
            return false;
        }
        for (StartupIssue issue : issues) {
            if (issue.severity() == StartupIssue.Severity.ERROR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there are any non-error issues (info or warnings).
     *
     * @param issues
     *            list of startup issues
     * @return true if any INFO or WARN severity issues exist
     */
    public static boolean hasWarningsOrInfo(List<StartupIssue> issues) {
        if (issues == null) {
            return false;
        }
        for (StartupIssue issue : issues) {
            if (issue.severity() != StartupIssue.Severity.ERROR) {
                return true;
            }
        }
        return false;
    }
}
