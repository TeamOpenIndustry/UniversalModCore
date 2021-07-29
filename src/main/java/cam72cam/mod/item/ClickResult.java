package cam72cam.mod.item;


import net.minecraft.world.InteractionResult;

public enum ClickResult {
    /** Handled */
    ACCEPTED(InteractionResult.SUCCESS),
    /** Unhandled */
    PASS(InteractionResult.PASS),
    /** Handled, but cancelled */
    REJECTED(InteractionResult.FAIL),
    ;

    public final InteractionResult internal;

    ClickResult(InteractionResult internal) {
        this.internal = internal;
    }

    public static ClickResult from(InteractionResult ear) {
        switch (ear) {
            case SUCCESS:
            case CONSUME:
                return ACCEPTED;
            case PASS:
                return PASS;
            case FAIL:
                return REJECTED;
        }
        return null;
    }
}
