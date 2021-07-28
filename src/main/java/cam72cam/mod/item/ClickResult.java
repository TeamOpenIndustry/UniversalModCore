package cam72cam.mod.item;

public enum ClickResult {
    /** Handled */
    ACCEPTED(true),
    /** Unhandled */
    PASS(false),
    /** Handled, but cancelled */
    REJECTED(false),
    ;

    public final boolean internal;

    ClickResult(boolean internal) {
        this.internal = internal;
    }

    public static ClickResult from(boolean ear) {
        return ear ? ACCEPTED : PASS;
    }
}
