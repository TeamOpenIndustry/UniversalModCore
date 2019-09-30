package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import com.mojang.blaze3d.platform.GLX;

public class MinecraftClient {
    public static Player getPlayer() {
        if (net.minecraft.client.MinecraftClient.getInstance().player == null) {
            return null;
        }
        return new Player(net.minecraft.client.MinecraftClient.getInstance().player);
    }

    public static void startProfiler(String section) {
        net.minecraft.client.MinecraftClient.getInstance().getProfiler().push(section);
    }

    public static void endProfiler() {
        net.minecraft.client.MinecraftClient.getInstance().getProfiler().pop();
    }

    public static boolean useVBO() {
        return GLX.useVbo();
    }

    public static Entity getEntityMouseOver() {
        net.minecraft.entity.Entity ent = net.minecraft.client.MinecraftClient.getInstance().targetedEntity;
        if (ent != null) {
            return getPlayer().getWorld().getEntity(ent.getUuid(), Entity.class);
        }
        return null;
    }

    public static boolean isPaused() {
        return net.minecraft.client.MinecraftClient.getInstance().isPaused();
    }
}
