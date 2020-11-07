package cam72cam.mod.item;

import net.minecraft.util.ActionResultType;

public enum ClickResult {
    /** Handled */
    ACCEPTED(ActionResultType.SUCCESS),
    /** Unhandled */
    PASS(ActionResultType.PASS),
    /** Handled, but cancelled */
    REJECTED(ActionResultType.FAIL),
    ;

    public final ActionResultType internal;

    ClickResult(ActionResultType internal) {
        this.internal = internal;
    }

    public static ClickResult from(ActionResultType ear) {
        switch (ear) {
            case SUCCESS:
                return ACCEPTED;
            case PASS:
                return PASS;
            case FAIL:
                return REJECTED;
        }
        return null;
    }
}
