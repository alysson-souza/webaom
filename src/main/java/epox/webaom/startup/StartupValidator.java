package epox.webaom.startup;

import epox.webaom.Options;
import epox.webaom.util.PlatformPaths;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates application startup conditions and provides graceful degradation when optional features
 * (logging, database) are unavailable.
 */
public class StartupValidator {

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
		List<StartupIssue> issues = new ArrayList<>();

		if (!autoLogEnabled) {
			return null; // Logging not enabled
		}

		// Use default if not specified
		if (logFilePath == null || logFilePath.trim().isEmpty()) {
			logFilePath = PlatformPaths.getDefaultLogFilePath();
		}

		// Ensure parent directory exists
		if (!PlatformPaths.ensureParentDirectoryExists(logFilePath)) {
			issues.add(new StartupIssue(StartupIssue.Severity.WARN, "Logging Disabled",
					"Failed to create or access log directory: " + logFilePath,
					"Check filesystem permissions and disk space"));
			return null; // Return null to disable logging
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
		if (!dbUrlLower.contains("postgresql") && !dbUrlLower.contains("mysql") && !dbUrlLower.contains("h2")
				&& !dbUrlLower.contains("jdbc:")) {
			issues.add(new StartupIssue(StartupIssue.Severity.WARN, "Invalid Database URL",
					"Database URL doesn't look like a valid JDBC URL: " + dbUrl,
					"Expected format: jdbc:postgresql://host:port/db or" + " jdbc:mysql://host:port/db\n"
							+ "The application will use embedded H2 database as fallback."));
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
		boolean autoLog = options.getB(Options.B_AUTOLOG);
		String logPath = options.getS(Options.S_LOGFILE);
		String validatedLogPath = validateLogging(autoLog, logPath);

		// If logging validation returned null, add the WARN issue
		if (autoLog && validatedLogPath == null && logPath != null && !logPath.trim().isEmpty()) {
			issues.add(new StartupIssue(StartupIssue.Severity.WARN, "Logging Disabled",
					"Failed to create or access log directory: " + logPath,
					"Automatic logging has been disabled. You can enable it again in"
							+ " Options when the issue is resolved."));
		}

		// Validate database
		boolean autoLoadDB = options.getB(Options.B_ALOADDB);
		String dbUrl = options.getS(Options.S_MYDBURL);
		List<StartupIssue> dbIssues = validateDatabase(autoLoadDB, dbUrl);
		issues.addAll(dbIssues);

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

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < issues.size(); i++) {
			if (i > 0) {
				sb.append("\n\n---\n\n");
			}
			sb.append(issues.get(i).getFormattedMessage());
		}
		return sb.toString();
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
