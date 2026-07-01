package cam72cam.mod.gui_v2.core.actions;

public interface IUpdatable {
    default void preRender() {}
    default void postRender() {}
    default void onTick() {}
}
