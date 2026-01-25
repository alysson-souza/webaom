package epox.webaom.data;

public final class MylistStates {
    private MylistStates() {}

    // Location states (API state field)
    public static final int LOCATION_UNKNOWN = 0;
    public static final int LOCATION_HDD = 1;
    public static final int LOCATION_CD = 2;
    public static final int LOCATION_DELETED = 3;
    public static final int LOCATION_REMOTE = 4;

    // File condition states (API filestate field)
    public static final int FILE_COND_NORMAL = 0;
    public static final int FILE_COND_CORRUPTED = 1;
    public static final int FILE_COND_SELF_EDITED = 2;
    public static final int FILE_COND_SELF_RIPPED = 10;
    public static final int FILE_COND_DVD = 11;
    public static final int FILE_COND_VHS = 12;
    public static final int FILE_COND_TV = 13;
    public static final int FILE_COND_THEATERS = 14;
    public static final int FILE_COND_STREAMED = 15;
    public static final int FILE_COND_OTHER = 100;

    // Valid ranges
    public static final int LOCATION_MIN = 0;
    public static final int LOCATION_MAX = 4;

    public static boolean isValidLocation(int state) {
        return state >= LOCATION_MIN && state <= LOCATION_MAX;
    }

    public static String toLocationDisplayName(int state) {
        return switch (state) {
            case LOCATION_HDD -> "On HDD";
            case LOCATION_CD -> "On CD";
            case LOCATION_DELETED -> "Deleted";
            case LOCATION_REMOTE -> "Remote";
            default -> "Unknown";
        };
    }
}
