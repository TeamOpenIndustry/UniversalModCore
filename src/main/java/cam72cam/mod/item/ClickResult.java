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
        return switch (ear) {
            case InteractionResult.Success success -> ACCEPTED;
            case InteractionResult.Pass pass -> PASS;
            case InteractionResult.TryEmptyHandInteraction empty -> PASS;
            case InteractionResult.Fail fail -> REJECTED;
        };
    }
}
