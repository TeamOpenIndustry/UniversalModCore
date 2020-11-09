package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import com.mojang.blaze3d.platform.GLX;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;

/** Static Minecraft Client props, don't touch server side */
public class MinecraftClient {
    /** Minecraft is loaded and has a loaded world */
    public static boolean isReady() {
        return Minecraft.getInstance().player != null;
    }

    private static Player playerCache;
    /** Hey, it's you! */
    public static Player getPlayer() {
        ClientPlayerEntity internal = Minecraft.getInstance().player;
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
        Minecraft.getInstance().getProfiler().startSection(section);
    }

    /** Hooks into the GUI profiler */
    public static void endProfiler() {
        Minecraft.getInstance().getProfiler().endSection();
    }

    /** Entity that you are currently looking at (distance limited) */
    public static Entity getEntityMouseOver() {
        if (Minecraft.getInstance().objectMouseOver instanceof EntityRayTraceResult) {
            net.minecraft.entity.Entity ent = ((EntityRayTraceResult) Minecraft.getInstance().objectMouseOver).getEntity();
            if (ent != null) {
                return getPlayer().getWorld().getEntity(ent.getUniqueID(), Entity.class);
            }
        }
        return null;
    }

    /** Block you are currently pointing at (distance limited) */
    public static Vec3i getBlockMouseOver() {
        return Minecraft.getInstance().objectMouseOver != null && Minecraft.getInstance().objectMouseOver.getType() == RayTraceResult.Type.BLOCK ? new Vec3i(new Vec3d(Minecraft.getInstance().objectMouseOver.getHitVec())) : null;
    }

    /** Offset inside the block you are currently pointing at (distance limited) */
    public static Vec3d getPosMouseOver() {
        return Minecraft.getInstance().objectMouseOver != null && Minecraft.getInstance().objectMouseOver.getType() == RayTraceResult.Type.BLOCK ? new Vec3d(Minecraft.getInstance().objectMouseOver.getHitVec()) : null;
    }

    /** Is the game in the paused state? */
    public static boolean isPaused() {
        return Minecraft.getInstance().isGamePaused();
    }
}
