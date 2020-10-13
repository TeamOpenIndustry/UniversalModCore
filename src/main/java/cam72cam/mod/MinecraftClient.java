package cam72cam.mod;

import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.math.Vec3i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.math.RayTraceResult;

/** Static Minecraft Client props, don't touch server side */
public class MinecraftClient {
    /** Minecraft is loaded and has a loaded world */
    public static boolean isReady() {
        return Minecraft.getMinecraft().player != null;
    }

    /** Hey, it's you! */
    public static Player getPlayer() {
        if (Minecraft.getMinecraft().player == null) {
            throw new RuntimeException("Called to get the player before minecraft has actually started!");
        }
        return new Player(Minecraft.getMinecraft().player);
    }

    /** Hooks into the GUI profiler */
    public static void startProfiler(String section) {
        Minecraft.getMinecraft().profiler.startSection(section);
    }

    /** Hooks into the GUI profiler */
    public static void endProfiler() {
        Minecraft.getMinecraft().profiler.endSection();
    }

    /** Assume true... */
    @Deprecated
    public static boolean useVBO() {
        return OpenGlHelper.useVbo();
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
        return Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK ? new Vec3i(Minecraft.getMinecraft().objectMouseOver.getBlockPos()) : null;
    }

    /** Offset inside the block you are currently pointing at (distance limited) */
    public static Vec3d getPosMouseOver() {
        return Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK ? new Vec3d(Minecraft.getMinecraft().objectMouseOver.hitVec) : null;
    }

    /** Is the game in the paused state? */
    public static boolean isPaused() {
        return Minecraft.getMinecraft().isGamePaused();
    }
}
