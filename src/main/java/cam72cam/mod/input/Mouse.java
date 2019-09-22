package cam72cam.mod.input;

import cam72cam.mod.MinecraftClient;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.util.Hand;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;

public class Mouse {
    @SubscribeEvent
    public void onClick(MouseEvent event) {
        // So it turns out that the client sends mouse click packets to the server regardless of
        // if the entity being clicked is within the requisite distance.
        // We need to override that distance because train centers are further away
        // than 36m.

        int attackID = Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode() + 100;
        int useID = Minecraft.getMinecraft().gameSettings.keyBindUseItem.getKeyCode() + 100;

        if ((event.button == attackID || event.button == useID) && event.buttonstate) {
            if (Minecraft.getMinecraft().objectMouseOver == null) {
                return;
            }

            Hand button = attackID == event.button ? Hand.SECONDARY : Hand.PRIMARY;

            Entity entity = MinecraftClient.getEntityMouseOver();
            if (entity != null && entity.internal instanceof ModdedEntity) {
                new MousePressPacket(button, entity).sendToServer();
                event.setCanceled(true);
                return;
            }
            Entity riding = MinecraftClient.getPlayer().getRiding();
            if (riding != null && riding.internal instanceof ModdedEntity) {
                new MousePressPacket(button, riding).sendToServer();
                event.setCanceled(true);
                return;
            }
        }
    }

}
