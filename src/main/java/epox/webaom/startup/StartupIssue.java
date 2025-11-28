package epox.webaom.startup;

/**
 * Represents an issue encountered during application startup. Includes severity level and
 * user-friendly messaging.
 */
public class StartupIssue {

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

    private final Severity severity;
    private final String title;
    private final String message;
    private final String suggestion;
    private final Exception cause;

    /**
     * Create a startup issue with detailed information.
     *
     * @param severity the severity level
     * @param title a short title (e.g., "Logging Failed")
     * @param message the detailed error message
     * @param suggestion a suggestion for fixing the issue
     * @param cause the underlying exception (can be null)
     */
    public StartupIssue(
            Severity severity, String title, String message, String suggestion, Exception cause) {
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.suggestion = suggestion;
        this.cause = cause;
    }

    /**
     * Create a startup issue with message and suggestion only.
     *
     * @param severity the severity level
     * @param title a short title
     * @param message the detailed error message
     * @param suggestion a suggestion for fixing the issue
     */
    public StartupIssue(Severity severity, String title, String message, String suggestion) {
        this(severity, title, message, suggestion, null);
    }

    /**
     * Create a startup issue with message only.
     *
     * @param severity the severity level
     * @param title a short title
     * @param message the detailed error message
     */
    public StartupIssue(Severity severity, String title, String message) {
        this(severity, title, message, null, null);
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public Exception getCause() {
        return cause;
    }

    /**
     * Get a formatted string representation of this issue suitable for display.
     *
     * @return formatted issue string
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(severity.getDisplayName()).append("] ");
        sb.append(title).append("\n");
        sb.append(message);
        if (suggestion != null && !suggestion.isEmpty()) {
            sb.append("\n\nSuggestion: ").append(suggestion);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
