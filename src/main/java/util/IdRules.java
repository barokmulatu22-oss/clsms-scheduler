package util;

/**
 * Shared rule for user-typed IDs (room ID, course code, instructor ID).
 * DESIGN.md section 4: letters, digits and "-" only, so IDs are safe inside URLs.
 */
public final class IdRules {

    private IdRules() {
        // Utility class
    }

    /** True if the ID (after trim + uppercase) matches [A-Z0-9-]+. */
    public static boolean isValid(String id) {
        return id != null && id.trim().toUpperCase().matches("[A-Z0-9-]+");
    }

    /** Error text, e.g. "Room ID may only contain letters, digits and '-' (no spaces or symbols)." */
    public static String message(String label) {
        return label + " may only contain letters, digits and '-' (no spaces or symbols).";
    }
}
