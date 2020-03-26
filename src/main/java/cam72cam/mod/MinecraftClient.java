package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import com.mojang.blaze3d.platform.GLX;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.util.hit.HitResult;

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
        return true;
    }

    public static Entity getEntityMouseOver() {
        net.minecraft.entity.Entity ent = net.minecraft.client.MinecraftClient.getInstance().targetedEntity;
        if (ent != null) {
            return getPlayer().getWorld().getEntity(ent.getUuid(), Entity.class);
        }
        return null;
    }

    public static Vec3i getBlockMouseOver() {
        return net.minecraft.client.MinecraftClient.getInstance().crosshairTarget != null && net.minecraft.client.MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.BLOCK ? new Vec3i(net.minecraft.client.MinecraftClient.getInstance().crosshairTarget.getPos()) : null;
    }

    public static Vec3d getPosMouseOver() {
        return net.minecraft.client.MinecraftClient.getInstance().crosshairTarget != null && net.minecraft.client.MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.BLOCK ? new Vec3d(net.minecraft.client.MinecraftClient.getInstance().crosshairTarget.getPos()) : null;
    }

    public static boolean isPaused() {
        return net.minecraft.client.MinecraftClient.getInstance().isPaused();
    }
}
