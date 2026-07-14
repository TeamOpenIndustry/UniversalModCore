package cam72cam.mod.input;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.entity.custom.IClickable;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.item.ClickResult;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class Mouse {
    /**
     * Checks if the left mouse button is currently pressed.
     */
    public static boolean isLMBDown() {
        return GLFW.glfwGetKey(Minecraft.getInstance().mainWindow.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == 1;
    }

    /**
     * Checks if the right mouse button is currently pressed.
     */
    public static boolean isRMBDown() {
        return GLFW.glfwGetKey(Minecraft.getInstance().mainWindow.getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT) == 1;
    }

    /**
     * Checks if the middle mouse button exists and is currently pressed.
     */
    public static boolean isMMBDown() {
        return GLFW.glfwGetKey(Minecraft.getInstance().mainWindow.getHandle(), GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == 1;
    }

    /**
     * Registers a 3D space dragging handler.
     *
     * @param handler receives the pressing key ({@link cam72cam.mod.entity.Player.Hand#PRIMARY} as LMB, {@link cam72cam.mod.entity.Player.Hand#SECONDARY} for others)
     *                and returns a boolean indicating whether the event should be passed down or not
     */
    public static void registerDragHandler(Function<Player.Hand, Boolean> handler) {
        ClientEvents.DRAG.subscribe(handler);
    }

    /**
     * Get the screen space mouse position.
     *
     * @return  the screen space mouse position from the start point of dragging, or {@code null} if the mouse is not pressed.
     */
    public static Vec3d getDrag() {
        return ClientEvents.ClientEventBusForge.getDragPos();
    }

    /**
     * Internal, don't use
     */
    public static void registerClientEvents() {
        ClientEvents.CLICK.subscribe(button -> {
            // So it turns out that the client sends mouse click packets to the server regardless of
            // if the entity being clicked is within the requisite distance.
            // We need to override that distance because train centers are further away
            // than 36m.

            if (Minecraft.getInstance().objectMouseOver == null || !MinecraftClient.isReady() || Minecraft.getInstance().currentScreen != null) {
                return true;
            }

            Entity entity = MinecraftClient.getEntityMouseOver();
            if (entity != null && entity.internal instanceof ModdedEntity && entity instanceof IClickable) {
                boolean flag = MinecraftClient.getPlayer().internal.canEntityBeSeen(entity.internal);
                double d0 = 36.0D;
                if (!flag) {
                    d0 = 9.0D;
                }

                // Invert MC's built in logic for entity distance (assumes small entities)
                if (MinecraftClient.getPlayer().internal.getDistanceSq(entity.internal) >= d0) {
                    if (((IClickable)entity).onClick(MinecraftClient.getPlayer(), button) == ClickResult.ACCEPTED) {
                        new MousePressPacket(button, entity).sendToServer();
                        return false;
                    }
                }
                return true;
            }
            /*
            Entity riding = MinecraftClient.getPlayer().getRiding();
            if (riding != null && riding.internal instanceof ModdedEntity && entity instanceof IClickable) {
                if (((IClickable)riding).onClick(MinecraftClient.getPlayer(), button) == ClickResult.ACCEPTED) {
                    new MousePressPacket(button, riding).sendToServer();
                    return false;
                }
            }*/
            return true;
        });
    }

    /**
     * Internal, don't use
     */
    public static class MousePressPacket extends Packet {
        @TagField
        private Player.Hand hand;
        @TagField
        private Entity target;

        public MousePressPacket() {}

        MousePressPacket(Player.Hand hand, Entity target) {
            this.hand = hand;
            this.target = target;
        }

        @Override
        public void handle() {
            if (target != null && getPlayer() != null) {
                switch (hand) {
                    case PRIMARY:
                        getPlayer().internal.interactOn(target.internal, hand.internal);
                        break;
                    case SECONDARY:
                        getPlayer().internal.attackTargetEntityWithCurrentItem(target.internal);
                        break;
                }
            }
        }
    }
}
