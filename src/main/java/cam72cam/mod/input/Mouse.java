package cam72cam.mod.input;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.net.Packet;
import cam72cam.mod.util.Hand;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

public class Mouse {
    @SideOnly(Side.CLIENT)
    public static void registerClientEvents() {
        ClientEvents.CLICK.subscribe(button -> {
            // So it turns out that the client sends mouse click packets to the server regardless of
            // if the entity being clicked is within the requisite distance.
            // We need to override that distance because train centers are further away
            // than 36m.

            if (Minecraft.getMinecraft().objectMouseOver == null || Minecraft.getMinecraft().currentScreen != null) {
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
        static {
        }

        public MousePressPacket() {
            // Forge Reflection
        }

        MousePressPacket(Hand hand, Entity target) {
            super();
            data.setEnum("hand", hand);
            data.setEntity("target", target);
        }

        @Override
        public void handle() {
            Hand hand = data.getEnum("hand", Hand.class);
            Entity target = data.getEntity("target", getWorld());
            if (target != null) {
                switch (hand) {
                    case PRIMARY:
                        getPlayer().internal.interactWith(target.internal);
                        break;
                    case SECONDARY:
                        getPlayer().internal.attackTargetEntityWithCurrentItem(target.internal);
                        break;
                }
            }
        }
    }
}
