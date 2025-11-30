package epox.webaom.startup;

/**
 * Represents an issue encountered during application startup. Includes severity level and
 * user-friendly messaging.
 */
public record StartupIssue(Severity severity, String title, String message, String suggestion, Exception cause) {

    /**
     * Create a startup issue with detailed information.
     *
     * @param severity
     *            the severity level
     * @param title
     *            a short title (e.g., "Logging Failed")
     * @param message
     *            the detailed error message
     * @param suggestion
     *            a suggestion for fixing the issue
     * @param cause
     *            the underlying exception (can be null)
     */
    public StartupIssue {}

    /**
     * Create a startup issue with message and suggestion only.
     *
     * @param severity
     *            the severity level
     * @param title
     *            a short title
     * @param message
     *            the detailed error message
     * @param suggestion
     *            a suggestion for fixing the issue
     */
    public StartupIssue(Severity severity, String title, String message, String suggestion) {
        this(severity, title, message, suggestion, null);
    }

    /**
     * Create a startup issue with message only.
     *
     * @param severity
     *            the severity level
     * @param title
     *            a short title
     * @param message
     *            the detailed error message
     */
    public StartupIssue(Severity severity, String title, String message) {
        this(severity, title, message, null, null);
    }

    /**
     * Get a formatted string representation of this issue suitable for display.
     *
     * @return formatted issue string
     */
    public String getFormattedMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append("[").append(severity.getDisplayName()).append("] ");
        builder.append(title).append("\n");
        builder.append(message);
        if (suggestion != null && !suggestion.isEmpty()) {
            builder.append("\n\nSuggestion: ").append(suggestion);
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }

    /** Severity levels for startup issues */
    public enum Severity {
        INFO("Info"),
        WARN("Warning"),
        ERROR("Error");

        private final String displayName;

        Severity(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
