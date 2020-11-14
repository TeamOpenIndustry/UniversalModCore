package cam72cam.mod.item;

import net.minecraft.util.ActionResult;

public enum ClickResult {
    /** Handled */
    ACCEPTED(ActionResult.SUCCESS),
    /** Unhandled */
    PASS(ActionResult.PASS),
    /** Handled, but cancelled */
    REJECTED(ActionResult.FAIL),
    ;

    public final ActionResult internal;

    ClickResult(ActionResult internal) {
        this.internal = internal;
    }

    public static ClickResult from(ActionResult ear) {
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
