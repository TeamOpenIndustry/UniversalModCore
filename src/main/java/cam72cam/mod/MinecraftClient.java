package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.client.entity.EntityPlayerSP;

/** Static Minecraft Client props, don't touch server side */
public class MinecraftClient {
    /** Minecraft is loaded and has a loaded world */
    public static boolean isReady() {
        return Minecraft.getMinecraft().thePlayer != null;
    }

    private static Player playerCache;
    /** Hey, it's you! */
    public static Player getPlayer() {
        EntityPlayerSP internal = Minecraft.getMinecraft().thePlayer;
        if (internal == null) {
            throw new RuntimeException("Called to get the player before minecraft has actually started!");
        }
        if (playerCache == null || internal != playerCache.internal) {
            playerCache = new Player(Minecraft.getMinecraft().thePlayer);
        }
        return playerCache;
    }

    /** Hooks into the GUI profiler */
    public static void startProfiler(String section) {
        Minecraft.getMinecraft().mcProfiler.startSection(section);
    }

    /** Hooks into the GUI profiler */
    public static void endProfiler() {
        Minecraft.getMinecraft().mcProfiler.endSection();
    }

    /** Entity that you are currently looking at (distance limited) */
    public static Entity getEntityMouseOver() {
        net.minecraft.entity.Entity ent = Minecraft.getMinecraft().objectMouseOver.entityHit;
        if (ent != null) {
            return getPlayer().getWorld().getEntity(ent.getUniqueID(), Entity.class);
        }
        return null;
    }

    /** Block you are currently pointing at (distance limited) */
    public static Vec3i getBlockMouseOver() {
        return Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ?
                new Vec3i(
                        Minecraft.getMinecraft().objectMouseOver.blockX,
                        Minecraft.getMinecraft().objectMouseOver.blockY,
                        Minecraft.getMinecraft().objectMouseOver.blockZ
                ) : null;
    }

    /** Offset inside the block you are currently pointing at (distance limited) */
    public static Vec3d getPosMouseOver() {
        return Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK ? new Vec3d(Minecraft.getMinecraft().objectMouseOver.hitVec) : null;
    }

    /** Is the game in the paused state? */
    public static boolean isPaused() {
        return Minecraft.getMinecraft().isGamePaused();
    }
}
