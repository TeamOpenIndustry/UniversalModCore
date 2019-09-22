package cam72cam.mod.item;

public enum ClickResult {
    ACCEPTED(true),
    PASS(false),
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
