package cam72cam.mod.item;

import net.minecraft.util.EnumActionResult;

public enum ClickResult {
    /** Handled */
    ACCEPTED(EnumActionResult.SUCCESS),
    /** Unhandled */
    PASS(EnumActionResult.PASS),
    /** Handled, but cancelled */
    REJECTED(EnumActionResult.FAIL),
    ;

    public final EnumActionResult internal;

    ClickResult(EnumActionResult internal) {
        this.internal = internal;
    }

    public static ClickResult from(EnumActionResult ear) {
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
