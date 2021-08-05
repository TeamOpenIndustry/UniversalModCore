package cam72cam.mod.input;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.Event;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.Function;

/** Only used for MC bugfixes, don't use directly */
public class Mouse {
    @SideOnly(Side.CLIENT)
    public static void registerClientEvents() {
        ClientEvents.CLICK.subscribe(button -> {
            // So it turns out that the client sends mouse click packets to the server regardless of
            // if the entity being clicked is within the requisite distance.
            // We need to override that distance because train centers are further away
            // than 36m.

            if (Minecraft.getMinecraft().objectMouseOver == null) {
                return true;
            }

            Entity entity = MinecraftClient.getEntityMouseOver();
            if (entity != null && entity.internal instanceof ModdedEntity) {
                new MousePressPacket(button, entity).sendToServer();
                return false;
            }
            Entity riding = MinecraftClient.getPlayer().getRiding();
            if (riding != null && riding.internal instanceof ModdedEntity) {
                new MousePressPacket(button, riding).sendToServer();
                return false;
            }
            return true;
        });
    }

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

    public static void registerDragHandler(Function<Player.Hand, Boolean> handler) {
        ClientEvents.DRAG.subscribe(handler);
    }

    public static Vec3d getDrag() {
        return ClientEvents.ClientEventBus.getDragPos();
    }
}
