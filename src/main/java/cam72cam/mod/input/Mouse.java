package cam72cam.mod.input;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.net.Packet;
import cam72cam.mod.serialization.TagField;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Only used for MC bugfixes, don't use directly */
public class Mouse {
    @OnlyIn(Dist.CLIENT)
    public static void registerClientEvents() {
        ClientEvents.CLICK.subscribe(button -> {
            // So it turns out that the client sends mouse click packets to the server regardless of
            // if the entity being clicked is within the requisite distance.
            // We need to override that distance because train centers are further away
            // than 36m.

            if (Minecraft.getInstance().hitResult == null || !MinecraftClient.isReady() || Minecraft.getInstance().screen != null) {
                return true;
            }

            Entity entity = MinecraftClient.getEntityMouseOver();
            if (entity != null && entity.internal instanceof ModdedEntity) {
                double d0 = 36.0D;
                // Invert MC's built in logic for entity distance (assumes small entities)
                if (MinecraftClient.getPlayer().internal.distanceToSqr(entity.internal) >= d0) {
                    new MousePressPacket(button, entity).sendToServer();
                    return false;
                }
                return true;
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
                        getPlayer().internal.attack(target.internal);
                        break;
                }
            }
        }
    }
}
