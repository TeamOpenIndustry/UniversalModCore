package cam72cam.mod.gui_v2.rendering;

@FunctionalInterface
public interface GuiRenderFunc<T> {
    void draw(GuiRenderer renderer, T widget);
}
