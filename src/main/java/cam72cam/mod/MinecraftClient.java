package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/** Static Minecraft Client props, don't touch server side */
public class MinecraftClient {
    /** Minecraft is loaded and has a loaded world */
    public static boolean isReady() {
        return net.minecraft.client.MinecraftClient.getInstance().player != null;
    }

    private static Player playerCache;
    /** Hey, it's you! */
    public static Player getPlayer() {
        ClientPlayerEntity internal = net.minecraft.client.MinecraftClient.getInstance().player;
        if (internal == null) {
            throw new RuntimeException("Called to get the player before minecraft has actually started!");
        }
        if (playerCache == null || internal != playerCache.internal) {
            playerCache = new Player(internal);
        }
        return playerCache;
    }

    /** Hooks into the GUI profiler */
    public static void startProfiler(String section) {
        net.minecraft.client.MinecraftClient.getInstance().getProfiler().push(section);
    }

    /** Hooks into the GUI profiler */
    public static void endProfiler() {
        net.minecraft.client.MinecraftClient.getInstance().getProfiler().pop();
    }

    /** Entity that you are currently looking at (distance limited) */
    public static Entity getEntityMouseOver() {
        net.minecraft.entity.Entity ent = net.minecraft.client.MinecraftClient.getInstance().targetedEntity;
        if (ent != null) {
            return getPlayer().getWorld().getEntity(ent.getUuid(), Entity.class);
        }
        return null;
    }

    /** Block you are currently pointing at (distance limited) */
    public static Vec3i getBlockMouseOver() {
        return net.minecraft.client.MinecraftClient.getInstance().crosshairTarget != null && net.minecraft.client.MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.BLOCK ? new Vec3i(new BlockPos(net.minecraft.client.MinecraftClient.getInstance().crosshairTarget.getPos())) : null;
    }

    /** Offset inside the block you are currently pointing at (distance limited) */
    public static Vec3d getPosMouseOver() {
        return net.minecraft.client.MinecraftClient.getInstance().crosshairTarget != null && net.minecraft.client.MinecraftClient.getInstance().crosshairTarget.getType() == HitResult.Type.BLOCK ? new Vec3d(net.minecraft.client.MinecraftClient.getInstance().crosshairTarget.getPos()) : null;
    }

    /** Is the game in the paused state? */
    public static boolean isPaused() {
        return net.minecraft.client.MinecraftClient.getInstance().isPaused();
    }
}
