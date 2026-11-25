package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=ModCore.MODID, bus=Mod.EventBusSubscriber.Bus.FORGE, value=Dist.CLIENT)
public final class PassengerInputClient {
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        if (mc.player==null || mc.getConnection()==null || !(mc.player.getVehicle() instanceof SeatEntity seat)) return;
        int mask=0;
        if (mc.screen==null && mc.isWindowActive()) {
            var k=mc.options;
            if (k.keyLeft.isDown()) mask|=PassengerInputState.LEFT;
            if (k.keyRight.isDown()) mask|=PassengerInputState.RIGHT;
            if (k.keyUp.isDown()) mask|=PassengerInputState.FORWARD;
            if (k.keyDown.isDown()) mask|=PassengerInputState.BACK;
            if (k.keySprint.isDown()) mask|=PassengerInputState.SPRINT;
        }
        PassengerInputNetwork.send(seat.getId(),mask);
    }
}
