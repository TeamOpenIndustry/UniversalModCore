package cam72cam.mod.event;

import cam72cam.mod.entity.EntityRegistry;
import cam72cam.mod.entity.Player;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.render.EntityRenderer;
import cam72cam.mod.render.GlobalRender;
import cam72cam.mod.render.opengl.CustomTexture;
import cam72cam.mod.render.opengl.VBO;
import cam72cam.mod.world.World;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

/** Registry of events that are client side only. */
public class ClientEvents {
    /** Fired at client world ticking */
    public static final Event<Runnable> TICK = new Event<>();
    /**
     * Fired at mouse scrolling<br>
     * The double inputted indicates the scrolling direction, where 1.0 is scrolling upwards<br>
     * The event can be canceled by returning {@code false} in the handler
     */
    public static final Event<Function<Double, Boolean>> SCROLL = new Event<>();
    /**
     * Fired at mouse clicking<br>
     * The Player.Hand inputted indicates the clicking button, where {@link Player.Hand#PRIMARY} is main button
     * and {@link Player.Hand#SECONDARY} is others<br>
     * It's recommended to use with helpers in {@link Mouse}, like {@link Mouse#isLMBDown()}, to check the actual button<br>
     * The event can be canceled by returning {@code false} in the handler
     */
    public static final Event<Function<Player.Hand, Boolean>> CLICK = new Event<>();
    /**
     * Fired at mouse events in GUIs<br>
     * The event can be canceled by returning {@code false} in the handler
     */
    public static final Event<Function<MouseGuiEvent, Boolean>> MOUSE_GUI = new Event<>();
    /** Fired at client resource reloads */
    public static final Event<Runnable> RELOAD = new Event<>();

    public enum MouseAction {
        CLICK,
        RELEASE,
        MOVE,
        SCROLL,
    }

    public static class MouseGuiEvent {
        public final MouseAction action;
        public final int x;
        public final int y;
        public final int button;
        public final double scroll;

        public MouseGuiEvent(MouseAction action, int x, int y, int button, double scroll) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.button = button;
            this.scroll = scroll;
        }
    }

    @ApiStatus.Internal
    public static void registerClientEvents() {
        EntityRegistry.registerClientEvents();
        EntityRenderer.registerClientEvents();
        Mouse.registerClientEvents();
        GlobalRender.registerClientEvents();
        World.registerClientEvnets();

        VBO.registerClientEvents();
        CustomTexture.registerClientEvents();
    }

    /** Manually fires off a fake client resource reload event for UMC mods. */
    @ApiStatus.Internal
    public static void fireReload() {
        RELOAD.execute(Runnable::run);
    }
}
