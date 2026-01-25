package epox.webaom.data;

import epox.util.StringUtilities;

public final class MylistResponseHandler {
    private MylistResponseHandler() {}

    private static final int FIELD_LID = 0;
    private static final int FIELD_FID = 1;
    private static final int FIELD_EID = 2;
    private static final int FIELD_AID = 3;
    private static final int FIELD_GID = 4;
    private static final int FIELD_DATE = 5;
    private static final int FIELD_STATE = 6;
    private static final int FIELD_VIEWDATE = 7;
    private static final int FIELD_STORAGE = 8;
    private static final int FIELD_SOURCE = 9;
    private static final int FIELD_OTHER = 10;
    private static final int FIELD_FILESTATE = 11;

    public static Mylist parse(String response) {
        String[] fields = StringUtilities.split(response, '|');
        Mylist mylist = new Mylist();

        if (fields.length > FIELD_STATE) {
            mylist.state = parseIntSafe(fields[FIELD_STATE]);
        }
        if (fields.length > FIELD_FILESTATE) {
            mylist.filestate = parseIntSafe(fields[FIELD_FILESTATE]);
        }

        return mylist;
    }

    public static String format(String response) {
        String[] fields = StringUtilities.split(response, '|');
        if (fields.length < 6) {
            return response;
        }

        if (isNumeric(fields[0])) {
            return formatSingleEntry(fields);
        }
        return formatMultipleEntries(fields);
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String formatSingleEntry(String[] fields) {
        String state = fields.length > FIELD_STATE && !fields[FIELD_STATE].isEmpty()
                ? MylistStates.toLocationDisplayName(parseIntSafe(fields[FIELD_STATE]))
                : "Unknown";

        StringBuilder result = new StringBuilder();
        result.append("lid=")
                .append(fields[FIELD_LID])
                .append(" (")
                .append(state)
                .append(")");
        return result.toString();
    }

    private static String formatMultipleEntries(String[] fields) {
        String animeName = fields[0];
        String totalEps = fields[1].isEmpty() ? "?" : fields[1];
        String epsInList = fields[5].isEmpty() ? "0" : fields[5];
        String watchedEps = fields.length > 6 && !fields[6].isEmpty() ? fields[6] : "";

        StringBuilder result = new StringBuilder();
        result.append(animeName)
                .append(" - ")
                .append(epsInList)
                .append("/")
                .append(totalEps)
                .append(" eps");

        if (!watchedEps.isEmpty()) {
            result.append(", watched: ").append(watchedEps);
        }

        return result.toString();
    }

    private static int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
