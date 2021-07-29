package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Static Minecraft Client props, don't touch server side */
public class MinecraftClient {
    /** Minecraft is loaded and has a loaded world */
    public static boolean isReady() {
        return Minecraft.getInstance().player != null;
    }

    private static Player playerCache;
    /** Hey, it's you! */
    public static Player getPlayer() {
        LocalPlayer internal = Minecraft.getInstance().player;
        if (internal == null) {
            throw new RuntimeException("Called to get the player before minecraft has actually started!");
        }
        if (playerCache == null || internal != playerCache.internal) {
            playerCache = new Player(Minecraft.getInstance().player);
        }
        return playerCache;
    }

    /** Hooks into the GUI profiler */
    public static void startProfiler(String section) {
        Minecraft.getInstance().getProfiler().push(section);
    }

    /** Hooks into the GUI profiler */
    public static void endProfiler() {
        Minecraft.getInstance().getProfiler().pop();
    }

    /** Entity that you are currently looking at (distance limited) */
    public static Entity getEntityMouseOver() {
        if (Minecraft.getInstance().hitResult instanceof EntityHitResult) {
            net.minecraft.world.entity.Entity ent = ((EntityHitResult) Minecraft.getInstance().hitResult).getEntity();
            return getPlayer().getWorld().getEntity(ent.getUUID(), Entity.class);
        }
        return null;
    }

    /** Block you are currently pointing at (distance limited) */
    public static Vec3i getBlockMouseOver() {
        return Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK ? new Vec3i(new Vec3d(Minecraft.getInstance().hitResult.getLocation())) : null;
    }

    /** Offset inside the block you are currently pointing at (distance limited) */
    public static Vec3d getPosMouseOver() {
        return Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK ? new Vec3d(Minecraft.getInstance().hitResult.getLocation()) : null;
    }

    /** Is the game in the paused state? */
    public static boolean isPaused() {
        return Minecraft.getInstance().isPaused();
    }
}
